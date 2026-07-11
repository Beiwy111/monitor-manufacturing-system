package com.upc.computer.service.impl;

import com.upc.computer.service.PurchaseService;
import com.upc.computer.common.BusinessException;
import com.upc.computer.dto.GeneratePurchaseRequest;
import com.upc.computer.entity.*;
import com.upc.computer.mapper.*;
import com.upc.computer.vo.PurchaseByOrderVO;
import com.upc.computer.vo.PurchaseRequirementDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PurchaseServiceImpl implements PurchaseService {

    @Autowired
    private PurchaseOrderMapper purchaseOrderMapper;

    @Autowired
    private PurchaseOrderItemMapper purchaseOrderItemMapper;

    @Autowired
    private PurchaseRequirementMapper requirementMapper;

    @Autowired
    private PurchaseRequirementSourceMapper requirementSourceMapper;

    @Autowired
    private CustomerOrderMapper customerOrderMapper;

    @Autowired
    private CustomerOrderItemMapper customerOrderItemMapper;

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Autowired
    private BomMapper bomMapper;

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private SupplierMapper supplierMapper;

    // 状态常量
    private static final String REQ_PENDING = "PENDING";
    private static final String REQ_SELECTED = "SELECTED";
    private static final String REQ_PURCHASED = "PURCHASED";
    private static final String REQ_ARRIVED = "ARRIVED";
    private static final String REQ_CANCELLED = "CANCELLED";

    private static final String PO_DRAFT = "DRAFT";
    private static final String PO_CANCELLED = "CANCELLED";
    // DB constraint: DRAFT/SUBMITTED/APPROVED/PART_RECEIVED/RECEIVED/CANCELLED
    private static final String PO_RECEIVED = "RECEIVED";
    // DB constraint for item_status: PENDING/PART_RECEIVED/RECEIVED/CANCELLED
    private static final String ITEM_PENDING = "PENDING";
    private static final String ITEM_RECEIVED = "RECEIVED";

    // ============ 原有采购订单 / 明细 CRUD（保留） ============

    @Override
    public ArrayList<PurchaseOrder> purchaseOrderList() {
        return purchaseOrderMapper.purchaseOrderList();
    }

    @Override
    public PurchaseOrder getPurchaseOrderById(Long purchaseOrderId) {
        return purchaseOrderMapper.getPurchaseOrderById(purchaseOrderId);
    }

    @Override
    public void insertPurchaseOrder(PurchaseOrder purchaseOrder) {
        purchaseOrderMapper.insertPurchaseOrder(purchaseOrder);
    }

    @Override
    public void updatePurchaseOrder(PurchaseOrder purchaseOrder) {
        purchaseOrderMapper.updatePurchaseOrder(purchaseOrder);
    }

    @Override
    public void deletePurchaseOrder(Long purchaseOrderId) {
        purchaseOrderMapper.deletePurchaseOrder(purchaseOrderId);
    }

    @Override
    public ArrayList<PurchaseOrderItem> purchaseOrderItemList() {
        return purchaseOrderItemMapper.purchaseOrderItemList();
    }

    @Override
    public PurchaseOrderItem getPurchaseOrderItemById(Long purchaseOrderItemId) {
        return purchaseOrderItemMapper.getPurchaseOrderItemById(purchaseOrderItemId);
    }

    @Override
    public void insertPurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) {
        purchaseOrderItemMapper.insertPurchaseOrderItem(purchaseOrderItem);
    }

    @Override
    public void updatePurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) {
        purchaseOrderItemMapper.updatePurchaseOrderItem(purchaseOrderItem);
    }

    @Override
    public void deletePurchaseOrderItem(Long purchaseOrderItemId) {
        purchaseOrderItemMapper.deletePurchaseOrderItem(purchaseOrderItemId);
    }

    // ============ 缺料需求计算 ============

    /**
     * 缺料计算主流程：
     * 销售订单/生产工单 -> 展开产品BOM -> 汇总物料总需求
     * -> 扣减可用库存 -> 扣减在途采购 -> 得到净缺料 -> 写入工作台
     */
    @Override
    @Transactional
    public List<PurchaseRequirement> calculateRequirements() {
        // 幂等：清空全部未绑定采购单的开放工作台需求，重新计算当前真实缺料
        requirementSourceMapper.deleteOpenWorkbenchRows();
        requirementMapper.deleteOpenWorkbenchRows();

        // 预加载 BOM，按父物料分组，供多级展开
        Map<Long, List<Bom>> bomByParent = new HashMap<>();
        for (Bom bom : bomMapper.bomList()) {
            if (bom.getStatus() != null && bom.getStatus() == 0) {
                continue;
            }
            bomByParent.computeIfAbsent(bom.getParentMaterialId(), k -> new ArrayList<>()).add(bom);
        }

        // 预加载物料信息：类型 + 默认供应商
        Map<Long, String> materialTypeMap = new HashMap<>();
        Map<Long, Long> materialSupplierIdMap = new HashMap<>();
        for (Material m : materialMapper.materialList()) {
            materialTypeMap.put(m.getMaterialId(), m.getMaterialType());
            if (m.getSupplierId() != null) {
                materialSupplierIdMap.put(m.getMaterialId(), m.getSupplierId());
            }
        }

        // 预加载供应商名称
        Map<Long, String> supplierNameMap = new HashMap<>();
        for (Supplier s : supplierMapper.listAll()) {
            supplierNameMap.put(s.getSupplierId(), s.getSupplierName());
        }

        // 物料维度需求累加器
        Map<Long, MaterialDemand> demandMap = new LinkedHashMap<>();
        // 1) 销售订单来源（仅统计已审核通过订单）
        List<CustomerOrderItem> allOrderItems = customerOrderItemMapper.orderItemList();
        for (CustomerOrder order : customerOrderMapper.customerOrderList()) {
            if (!isApprovedOrder(order.getAuditStatus())) {
                continue;
            }
            SourceRef ref = SourceRef.fromOrder(order);
            for (CustomerOrderItem item : allOrderItems) {
                if (!Objects.equals(item.getOrderId(), order.getOrderId())) {
                    continue;
                }
                if (item.getMaterialId() == null || item.getQuantity() == null) {
                    continue;
                }
                expandDemand(item.getMaterialId(), item.getQuantity(), ref, demandMap, bomByParent, materialTypeMap, new HashSet<>());
            }
        }

        // 2) 生产工单来源（在制/已下达）
        for (WorkOrder wo : workOrderMapper.workOrderList()) {
            if (wo.getMaterialId() == null || wo.getPlannedQuantity() == null) {
                continue;
            }
            if (!isActiveWorkOrder(wo.getStatus())) {
                continue;
            }
            SourceRef ref = SourceRef.fromWorkOrder(wo);
            expandDemand(wo.getMaterialId(), wo.getPlannedQuantity(), ref, demandMap, bomByParent, materialTypeMap, new HashSet<>());
        }

        // 3) 扣减库存与在途，计算净缺料并落库
        Map<Long, BigDecimal> availableStock = loadAvailableStock();
        Map<Long, BigDecimal> onPurchase = loadOnPurchaseQuantity();

        List<PurchaseRequirement> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<Long, MaterialDemand> entry : demandMap.entrySet()) {
            Long materialId = entry.getKey();
            MaterialDemand demand = entry.getValue();

            BigDecimal required = demand.totalRequired.setScale(0, java.math.RoundingMode.CEILING);
            BigDecimal stock = availableStock.getOrDefault(materialId, BigDecimal.ZERO);
            BigDecimal inTransit = onPurchase.getOrDefault(materialId, BigDecimal.ZERO);
            BigDecimal shortage = required.subtract(stock).subtract(inTransit);

            if (shortage.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            shortage = shortage.setScale(0, java.math.RoundingMode.CEILING);

            Material material = materialMapper.getMaterialById(materialId);
            Long supplierId = materialSupplierIdMap.get(materialId);
            String supplierName = supplierId != null ? supplierNameMap.get(supplierId) : null;

            PurchaseRequirement req = new PurchaseRequirement();
            req.setMaterialId(materialId);
            req.setMaterialCode(material != null ? material.getMaterialCode() : null);
            req.setMaterialName(material != null ? material.getMaterialName() : null);
            req.setRequiredQuantity(required);
            req.setStockQuantity(stock);
            req.setOnPurchaseQuantity(inTransit);
            req.setShortageQuantity(shortage);
            req.setSuggestedPurchaseQuantity(shortage);
            req.setStatus(REQ_PENDING);
            req.setPriority(demand.priority);
            req.setExpectedArrivalDate(demand.earliestDeliveryDate);
            req.setSupplierId(supplierId);
            req.setSupplierName(supplierName);
            req.setCreatedAt(now);
            req.setUpdatedAt(now);
            requirementMapper.insertRequirement(req);

            // 写入来源追溯
            for (SourceDemand sd : demand.sources) {
                PurchaseRequirementSource src = new PurchaseRequirementSource();
                src.setRequirementId(req.getRequirementId());
                src.setSourceType(sd.ref.sourceType);
                src.setCustomerOrderId(sd.ref.customerOrderId);
                src.setCustomerOrderNo(sd.ref.customerOrderNo);
                src.setWorkOrderId(sd.ref.workOrderId);
                src.setWorkOrderNo(sd.ref.workOrderNo);
                src.setMaterialId(materialId);
                src.setRequiredQuantity(sd.quantity);
                src.setShortageQuantity(sd.quantity);
                src.setCreatedAt(now);
                requirementSourceMapper.insertSource(src);
            }

            result.add(req);
        }

        return result;
    }

    /**
     * 递归展开 BOM：
     * - 若物料存在子件，继续向下展开
     * - 若物料是叶子件，累加为可采购需求
     */
    private void expandDemand(Long materialId, BigDecimal quantity, SourceRef ref,
                              Map<Long, MaterialDemand> demandMap,
                              Map<Long, List<Bom>> bomByParent,
                              Map<Long, String> materialTypeMap,
                              Set<Long> visiting) {
        List<Bom> children = bomByParent.get(materialId);
        if (children == null || children.isEmpty()) {
            // 叶子节点：过滤掉成品和半成品，只采购原材料/外购件
            String mtype = materialTypeMap.getOrDefault(materialId, "");
            if ("FINISHED".equalsIgnoreCase(mtype) || "SEMI".equalsIgnoreCase(mtype)) {
                return;
            }
            MaterialDemand demand = demandMap.computeIfAbsent(materialId, k -> new MaterialDemand());
            demand.totalRequired = demand.totalRequired.add(quantity);
            demand.addSource(ref, quantity);
            demand.mergePriority(ref.priority);
            demand.mergeDeliveryDate(ref.deliveryDate);
            return;
        }
        if (!visiting.add(materialId)) {
            return;
        }
        for (Bom bom : children) {
            BigDecimal lossRate = bom.getLossRate() == null ? BigDecimal.ZERO : bom.getLossRate();
            BigDecimal childQty = bom.getQuantity() == null ? BigDecimal.ZERO : bom.getQuantity();
            BigDecimal need = quantity.multiply(childQty).multiply(BigDecimal.ONE.add(lossRate));
            expandDemand(bom.getChildMaterialId(), need, ref, demandMap, bomByParent, materialTypeMap, visiting);
        }
        visiting.remove(materialId);
    }

    /** 可用库存（quantity_available 汇总到物料维度） */
    private Map<Long, BigDecimal> loadAvailableStock() {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Inventory inv : inventoryMapper.inventoryList()) {
            if (inv.getMaterialId() == null) {
                continue;
            }
            BigDecimal avail = inv.getQuantityAvailable();
            if (avail == null) {
                avail = inv.getQuantityOnHand() == null ? BigDecimal.ZERO : inv.getQuantityOnHand();
            }
            map.merge(inv.getMaterialId(), avail, BigDecimal::add);
        }
        return map;
    }

    /**
     * 在途采购数量：未到货采购单明细的 (quantity - received_quantity) 汇总到物料维度。
     */
    private Map<Long, BigDecimal> loadOnPurchaseQuantity() {
        Set<Long> closedOrderIds = new HashSet<>();
        for (PurchaseOrder po : purchaseOrderMapper.purchaseOrderList()) {
            if (PO_RECEIVED.equals(po.getStatus()) || PO_CANCELLED.equals(po.getStatus())) {
                closedOrderIds.add(po.getPurchaseOrderId());
            }
        }
        Map<Long, BigDecimal> map = new HashMap<>();
        for (PurchaseOrderItem item : purchaseOrderItemMapper.purchaseOrderItemList()) {
            if (item.getMaterialId() == null || item.getQuantity() == null) {
                continue;
            }
            if (closedOrderIds.contains(item.getPurchaseOrderId())) {
                continue;
            }
            BigDecimal received = item.getReceivedQuantity() == null ? BigDecimal.ZERO : item.getReceivedQuantity();
            BigDecimal remaining = item.getQuantity().subtract(received);
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                map.merge(item.getMaterialId(), remaining, BigDecimal::add);
            }
        }
        return map;
    }

    private boolean isApprovedOrder(String auditStatus) {
        return "APPROVED".equalsIgnoreCase(auditStatus) || "PASS".equalsIgnoreCase(auditStatus)
                || "PASSED".equalsIgnoreCase(auditStatus);
    }

    private boolean isActiveWorkOrder(String status) {
        if (status == null) {
            return false;
        }
        return Arrays.asList("CREATED", "ISSUED", "RELEASED", "DISPATCHED", "IN_PROGRESS", "RUNNING", "PRODUCING")
                .contains(status.toUpperCase());
    }

    @Override
    public List<Map<String, String>> getSupplierList() {
        return purchaseOrderMapper.selectDistinctSuppliers();
    }

    // ============ 需求累加器内部类 ============

    private static class MaterialDemand {
        BigDecimal totalRequired = BigDecimal.ZERO;
        Integer priority = 5;
        LocalDate earliestDeliveryDate;
        List<SourceDemand> sources = new ArrayList<>();

        void addSource(SourceRef ref, BigDecimal quantity) {
            sources.add(new SourceDemand(ref, quantity));
        }

        void mergePriority(Integer p) {
            if (p != null && (priority == null || p < priority)) {
                priority = p;
            }
        }

        void mergeDeliveryDate(LocalDate d) {
            if (d != null && (earliestDeliveryDate == null || d.isBefore(earliestDeliveryDate))) {
                earliestDeliveryDate = d;
            }
        }
    }

    private static class SourceDemand {
        SourceRef ref;
        BigDecimal quantity;

        SourceDemand(SourceRef ref, BigDecimal quantity) {
            this.ref = ref;
            this.quantity = quantity;
        }
    }

    private static class SourceRef {
        String sourceType;
        Long customerOrderId;
        String customerOrderNo;
        Long workOrderId;
        String workOrderNo;
        Integer priority = 5;
        LocalDate deliveryDate;

        static SourceRef fromOrder(CustomerOrder order) {
            SourceRef ref = new SourceRef();
            ref.sourceType = "ORDER";
            ref.customerOrderId = order.getOrderId();
            ref.customerOrderNo = order.getOrderNo();
            ref.deliveryDate = order.getRequiredDeliveryDate();
            if (order.getRequiredDeliveryDate() != null
                    && !order.getRequiredDeliveryDate().isAfter(LocalDate.now().plusDays(7))) {
                ref.priority = 1;
            }
            return ref;
        }

        static SourceRef fromWorkOrder(WorkOrder wo) {
            SourceRef ref = new SourceRef();
            ref.sourceType = "WORK_ORDER";
            ref.workOrderId = wo.getWorkOrderId();
            ref.workOrderNo = wo.getWorkOrderNo();
            if (wo.getPlannedStartTime() != null) {
                ref.deliveryDate = wo.getPlannedStartTime().toLocalDate();
            }
            ref.priority = 2;
            return ref;
        }
    }

    // ============ 采购工作台查询 ============

    @Override
    public List<PurchaseRequirement> workbenchList(String materialName, String status, Integer priority) {
        List<PurchaseRequirement> all = requirementMapper.requirementList();
        String targetStatus = (status == null || status.isEmpty()) ? REQ_PENDING : status;
        Set<Long> excludeMaterialIds = new HashSet<>();
        for (Material m : materialMapper.materialList()) {
            if ("FINISHED".equalsIgnoreCase(m.getMaterialType()) || "SEMI".equalsIgnoreCase(m.getMaterialType())) {
                excludeMaterialIds.add(m.getMaterialId());
            }
        }
        Map<Long, PurchaseRequirement> latestByMaterial = new LinkedHashMap<>();
        for (PurchaseRequirement req : all) {
            if (excludeMaterialIds.contains(req.getMaterialId())) {
                continue;
            }
            if (!targetStatus.equalsIgnoreCase(req.getStatus())) {
                continue;
            }
            if (materialName != null && !materialName.isEmpty()) {
                String name = req.getMaterialName() == null ? "" : req.getMaterialName();
                if (!name.contains(materialName)) {
                    continue;
                }
            }
            if (priority != null && !priority.equals(req.getPriority())) {
                continue;
            }
            Long key = req.getMaterialId();
            if (key == null) {
                continue;
            }
            PurchaseRequirement existing = latestByMaterial.get(key);
            if (existing == null || isBetterWorkbenchRequirement(req, existing)) {
                latestByMaterial.put(key, req);
            }
        }
        return new ArrayList<>(latestByMaterial.values());
    }

    private boolean isBetterWorkbenchRequirement(PurchaseRequirement candidate, PurchaseRequirement existing) {
        Integer candidatePriority = candidate.getPriority() == null ? Integer.MAX_VALUE : candidate.getPriority();
        Integer existingPriority = existing.getPriority() == null ? Integer.MAX_VALUE : existing.getPriority();
        int priorityCompare = candidatePriority.compareTo(existingPriority);
        if (priorityCompare != 0) {
            return priorityCompare < 0;
        }
        BigDecimal candidateShortage = candidate.getShortageQuantity() == null ? BigDecimal.ZERO : candidate.getShortageQuantity();
        BigDecimal existingShortage = existing.getShortageQuantity() == null ? BigDecimal.ZERO : existing.getShortageQuantity();
        int shortageCompare = candidateShortage.compareTo(existingShortage);
        if (shortageCompare != 0) {
            return shortageCompare > 0;
        }
        Long candidateId = candidate.getRequirementId() == null ? Long.MIN_VALUE : candidate.getRequirementId();
        Long existingId = existing.getRequirementId() == null ? Long.MIN_VALUE : existing.getRequirementId();
        return candidateId > existingId;
    }

    @Override
    public PurchaseRequirementDetailVO workbenchDetail(Long requirementId) {
        PurchaseRequirement req = requirementMapper.getRequirementById(requirementId);
        if (req == null) {
            throw new BusinessException("采购需求不存在: " + requirementId);
        }
        PurchaseRequirementDetailVO vo = new PurchaseRequirementDetailVO();
        vo.setRequirement(req);
        vo.setSources(new ArrayList<>(requirementSourceMapper.listByRequirementId(requirementId)));
        return vo;
    }

    @Override
    public List<PurchaseByOrderVO> workbenchByOrder() {
        Map<Long, PurchaseRequirement> pendingRequirementMap = new HashMap<>();
        for (PurchaseRequirement req : requirementMapper.requirementList()) {
            if (REQ_PENDING.equals(req.getStatus()) && req.getRequirementId() != null
                    && req.getMaterialCode() != null && !req.getMaterialCode().isBlank()
                    && req.getMaterialName() != null && !req.getMaterialName().isBlank()) {
                pendingRequirementMap.put(req.getRequirementId(), req);
            }
        }
        // 汇总所有来源，按 (sourceType + 订单/工单) 分组
        Map<String, PurchaseByOrderVO> grouped = new LinkedHashMap<>();
        for (PurchaseRequirementSource src : requirementSourceMapper.sourceList()) {
            PurchaseRequirement req = pendingRequirementMap.get(src.getRequirementId());
            if (req == null) {
                continue;
            }
            String key;
            Long sourceId;
            String sourceNo;
            if ("WORK_ORDER".equals(src.getSourceType())) {
                sourceId = src.getWorkOrderId();
                sourceNo = src.getWorkOrderNo();
                key = "WORK_ORDER:" + sourceId;
            } else {
                sourceId = src.getCustomerOrderId();
                sourceNo = src.getCustomerOrderNo();
                key = "ORDER:" + sourceId;
            }
            if (sourceId == null) {
                continue;
            }
            PurchaseByOrderVO vo = grouped.computeIfAbsent(key, k -> {
                PurchaseByOrderVO v = new PurchaseByOrderVO();
                v.setSourceType(src.getSourceType());
                v.setSourceId(sourceId);
                v.setSourceNo(sourceNo);
                v.setLines(new ArrayList<>());
                return v;
            });
            PurchaseByOrderVO.Line line = new PurchaseByOrderVO.Line();
            line.setMaterialId(req.getMaterialId());
            line.setMaterialCode(req.getMaterialCode());
            line.setMaterialName(req.getMaterialName());
            line.setRequiredQuantity(src.getRequiredQuantity());
            line.setShortageQuantity(src.getShortageQuantity());
            vo.getLines().add(line);
        }
        return new ArrayList<>(grouped.values());
    }

    // ============ 一键生成采购单（按供应商自动拆单） ============

    /**
     * 按物料绑定的默认供应商分组，每个供应商生成一张独立采购单。
     * 无供应商绑定的需求归入"未分配"组，仍合并生成一张采购单。
     */
    @Override
    @Transactional
    public List<PurchaseOrder> generatePurchaseOrder(GeneratePurchaseRequest request) {
        if (request == null || request.getRequirementIds() == null || request.getRequirementIds().isEmpty()) {
            throw new BusinessException("请至少勾选一条采购需求");
        }

        // 去重并校验需求有效性
        List<PurchaseRequirement> requirements = new ArrayList<>();
        for (Long reqId : new LinkedHashSet<>(request.getRequirementIds())) {
            PurchaseRequirement req = requirementMapper.getRequirementById(reqId);
            if (req == null) {
                throw new BusinessException("采购需求不存在: " + reqId);
            }
            if (REQ_PURCHASED.equals(req.getStatus()) || REQ_CANCELLED.equals(req.getStatus())
                    || REQ_ARRIVED.equals(req.getStatus())) {
                throw new BusinessException("采购需求[" + reqId + "]当前状态[" + req.getStatus() + "]不可重复生成采购单");
            }
            if (!REQ_PENDING.equals(req.getStatus())) {
                throw new BusinessException("采购需求[" + reqId + "]不是待采购状态，不能生成采购单");
            }
            requirements.add(req);
        }

        // 预加载供应商信息
        Map<Long, Supplier> supplierMap = new HashMap<>();
        for (Supplier s : supplierMapper.listAll()) {
            supplierMap.put(s.getSupplierId(), s);
        }

        // 按 supplierId 分组（null → key=0L 表示"无供应商"）
        Map<Long, List<PurchaseRequirement>> groups = new LinkedHashMap<>();
        for (PurchaseRequirement req : requirements) {
            Long key = req.getSupplierId() != null ? req.getSupplierId() : 0L;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(req);
        }

        Map<String, GeneratePurchaseRequest.SupplierOverride> overrides =
                request.getSupplierOverrides() != null ? request.getSupplierOverrides() : Collections.emptyMap();

        LocalDateTime now = LocalDateTime.now();
        List<PurchaseOrder> createdOrders = new ArrayList<>();

        for (Map.Entry<Long, List<PurchaseRequirement>> entry : groups.entrySet()) {
            Long supplierId = entry.getKey();
            List<PurchaseRequirement> group = entry.getValue();

            Supplier supplier = supplierId != 0L ? supplierMap.get(supplierId) : null;
            GeneratePurchaseRequest.SupplierOverride override = overrides.get(String.valueOf(supplierId));

            // 决定供应商信息：优先 override，其次 supplier 表，最后留空
            String supplierName = override != null && override.getSupplierName() != null
                    ? override.getSupplierName()
                    : (supplier != null ? supplier.getSupplierName() : null);
            String supplierContact = override != null && override.getSupplierContact() != null
                    ? override.getSupplierContact()
                    : (supplier != null ? supplier.getContactPerson() : null);
            String supplierPhone = override != null && override.getSupplierPhone() != null
                    ? override.getSupplierPhone()
                    : (supplier != null ? supplier.getContactPhone() : null);
            LocalDate expectedArrivalDate = override != null ? override.getExpectedArrivalDate() : null;

            PurchaseOrder order = new PurchaseOrder();
            order.setPurchaseOrderNo(generatePurchaseOrderNo());
            order.setSupplierName(supplierName);
            order.setSupplierContact(supplierContact);
            order.setSupplierPhone(supplierPhone);
            order.setPurchaseDate(LocalDate.now());
            order.setExpectedArrivalDate(expectedArrivalDate);
            order.setStatus(PO_DRAFT);
            order.setPurchaserId(request.getPurchaserId());
            order.setRemark(request.getRemark());
            order.setTotalAmount(BigDecimal.ZERO);
            order.setCreatedAt(now);
            order.setUpdatedAt(now);
            purchaseOrderMapper.insertPurchaseOrder(order);

            BigDecimal totalAmount = BigDecimal.ZERO;
            for (PurchaseRequirement req : group) {
                Material material = materialMapper.getMaterialById(req.getMaterialId());
                BigDecimal qty = req.getSuggestedPurchaseQuantity() != null
                        ? req.getSuggestedPurchaseQuantity() : req.getShortageQuantity();
                BigDecimal unitPrice = material != null && material.getStandardCost() != null
                        ? material.getStandardCost() : BigDecimal.ZERO;

                PurchaseOrderItem item = new PurchaseOrderItem();
                item.setPurchaseOrderId(order.getPurchaseOrderId());
                item.setMaterialId(req.getMaterialId());
                item.setQuantity(qty);
                item.setReceivedQuantity(BigDecimal.ZERO);
                item.setUnit(material != null && material.getUnit() != null ? material.getUnit() : "件");
                item.setUnitPrice(unitPrice);
                item.setLineAmount(qty.multiply(unitPrice));
                item.setItemStatus(ITEM_PENDING);
                item.setCreatedAt(now);
                item.setUpdatedAt(now);
                purchaseOrderItemMapper.insertPurchaseOrderItem(item);

                totalAmount = totalAmount.add(item.getLineAmount());
                requirementMapper.bindPurchaseOrder(req.getRequirementId(), order.getPurchaseOrderId(), REQ_PURCHASED);
            }

            order.setTotalAmount(totalAmount);
            order.setUpdatedAt(LocalDateTime.now());
            purchaseOrderMapper.updatePurchaseOrder(order);
            createdOrders.add(order);
        }

        return createdOrders;
    }

    private String generatePurchaseOrderNo() {
        return "PO" + System.currentTimeMillis();
    }

    @Override
    public void selectRequirement(Long requirementId) {
        PurchaseRequirement req = requirementMapper.getRequirementById(requirementId);
        if (req == null) {
            throw new BusinessException("采购需求不存在: " + requirementId);
        }
        if (!REQ_PENDING.equals(req.getStatus())) {
            throw new BusinessException("仅待采购(PENDING)状态可选中，当前: " + req.getStatus());
        }
        requirementMapper.updateStatus(requirementId, REQ_SELECTED);
    }

    @Override
    public void cancelRequirement(Long requirementId) {
        PurchaseRequirement req = requirementMapper.getRequirementById(requirementId);
        if (req == null) {
            throw new BusinessException("采购需求不存在: " + requirementId);
        }
        if (REQ_PURCHASED.equals(req.getStatus()) || REQ_ARRIVED.equals(req.getStatus())) {
            throw new BusinessException("已生成采购单或已到货的需求不可取消，当前: " + req.getStatus());
        }
        requirementMapper.updateStatus(requirementId, REQ_CANCELLED);
    }

    // ============ 到货确认 + 入库预留联动 ============

    /**
     * 采购到货确认：
     * 1. 校验采购订单存在且状态允许到货
     * 2. 更新采购订单状态为 ARRIVED、明细收货数量
     * 3. 更新关联采购需求状态为 ARRIVED
     * 4. 预留库存入库联动点（对接人员A的 MaterialService.inboundByPurchase）
     */
    @Override
    @Transactional
    public void confirmArrival(Long purchaseOrderId) {
        PurchaseOrder order = purchaseOrderMapper.getPurchaseOrderById(purchaseOrderId);
        if (order == null) {
            throw new BusinessException("采购订单不存在: " + purchaseOrderId);
        }
        if (PO_RECEIVED.equals(order.getStatus())) {
            throw new BusinessException("采购订单已到货，请勿重复确认");
        }
        if (PO_CANCELLED.equals(order.getStatus())) {
            throw new BusinessException("已取消的采购订单不可到货确认");
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. 更新采购单状态
        order.setStatus(PO_RECEIVED);
        order.setUpdatedAt(now);
        purchaseOrderMapper.updatePurchaseOrder(order);

        // 2. 更新明细收货数量 + 入库联动
        for (PurchaseOrderItem item : purchaseOrderItemMapper.listByOrderIdWithMaterial(purchaseOrderId)) {
            item.setReceivedQuantity(item.getQuantity());
            item.setItemStatus(ITEM_RECEIVED);
            item.setUpdatedAt(now);
            purchaseOrderItemMapper.updatePurchaseOrderItem(item);

            // 3. 库存入库：找到该物料的第一个库存记录并增加数量；不存在则新建
            if (item.getMaterialId() != null && item.getQuantity() != null
                    && item.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                List<Inventory> stocks = inventoryMapper.listByMaterialId(item.getMaterialId());
                if (!stocks.isEmpty()) {
                    inventoryMapper.increaseQuantity(stocks.get(0).getInventoryId(), item.getQuantity(), now);
                } else {
                    Inventory inv = new Inventory();
                    inv.setMaterialId(item.getMaterialId());
                    inv.setWarehouseCode("WH01");
                    inv.setWarehouseName("主仓库");
                    inv.setQuantityOnHand(item.getQuantity());
                    inv.setQuantityReserved(BigDecimal.ZERO);
                    inv.setQuantityAvailable(item.getQuantity());
                    inv.setInventoryStatus("NORMAL");
                    inv.setLastTransactionAt(now);
                    inv.setCreatedAt(now);
                    inv.setUpdatedAt(now);
                    inventoryMapper.insertInventory(inv);
                }
            }
        }

        // 4. 更新关联采购需求状态
        for (PurchaseRequirement req : requirementMapper.listByPurchaseOrderId(purchaseOrderId)) {
            requirementMapper.updateStatus(req.getRequirementId(), REQ_ARRIVED);
        }
    }

    // ============ 采购单撤销 ============

    @Override
    @Transactional
    public void revokePurchaseOrder(Long purchaseOrderId) {
        PurchaseOrder order = purchaseOrderMapper.getPurchaseOrderById(purchaseOrderId);
        if (order == null) {
            throw new BusinessException("采购订单不存在: " + purchaseOrderId);
        }
        if (PO_RECEIVED.equals(order.getStatus())) {
            throw new BusinessException("已到货的采购订单不可撤销");
        }
        if (PO_CANCELLED.equals(order.getStatus())) {
            throw new BusinessException("采购订单已撤销，请勿重复操作");
        }

        LocalDateTime now = LocalDateTime.now();

        order.setStatus(PO_CANCELLED);
        order.setUpdatedAt(now);
        purchaseOrderMapper.updatePurchaseOrder(order);

        for (PurchaseOrderItem item : purchaseOrderItemMapper.listByOrderIdWithMaterial(purchaseOrderId)) {
            item.setItemStatus("CANCELLED");
            item.setUpdatedAt(now);
            purchaseOrderItemMapper.updatePurchaseOrderItem(item);
        }

        for (PurchaseRequirement req : requirementMapper.listByPurchaseOrderId(purchaseOrderId)) {
            requirementMapper.updateStatus(req.getRequirementId(), REQ_PENDING);
            requirementMapper.unbindPurchaseOrder(req.getRequirementId());
        }
    }

}
