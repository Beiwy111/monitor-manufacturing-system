package com.upc.computer.service;

import cn.hutool.crypto.digest.BCrypt;
import com.upc.computer.common.BusinessException;
import com.upc.computer.dto.MesActionRequest;
import com.upc.computer.entity.*;
import com.upc.computer.mapper.*;
import com.upc.computer.service.MesRuntimeStore.MesRuntimeState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * MES 工作流：执行前端 mes store 全部动作
 */
@Service
public class MesWorkflowService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    @Autowired
    private CustomerOrderMapper customerOrderMapper;
    @Autowired
    private CustomerOrderItemMapper customerOrderItemMapper;
    @Autowired
    private ProductionPlanMapper productionPlanMapper;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private DispatchTaskMapper dispatchTaskMapper;
    @Autowired
    private WorkReportMapper workReportMapper;
    @Autowired
    private QualityInspectionMapper qualityInspectionMapper;
    @Autowired
    private NonconformingProductMapper nonconformingProductMapper;
    @Autowired
    private MaterialMapper materialMapper;
    @Autowired
    private InventoryMapper inventoryMapper;
    @Autowired
    private InventoryTransactionMapper inventoryTransactionMapper;
    @Autowired
    private PurchaseOrderMapper purchaseOrderMapper;
    @Autowired
    private PurchaseOrderItemMapper purchaseOrderItemMapper;
    @Autowired
    private DeliveryOrderMapper deliveryOrderMapper;
    @Autowired
    private EquipmentMapper equipmentMapper;
    @Autowired
    private AndonAlarmMapper andonAlarmMapper;
    @Autowired
    private AfterSalesCaseMapper afterSalesCaseMapper;
    @Autowired
    private CostSettlementMapper costSettlementMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private ProcessStepMapper processStepMapper;
    @Autowired
    private ProcessRouteMapper processRouteMapper;
    @Autowired
    private ProductionPlanItemMapper productionPlanItemMapper;
    @Autowired
    private BomMapper bomMapper;
    @Autowired
    private EquipmentMaintenanceRecordMapper equipmentMaintenanceRecordMapper;
    @Autowired
    private WorkProgressMapper workProgressMapper;
    @Autowired
    private MesRuntimeStore mesRuntimeStore;
    @Autowired
    private MesPlannerAgentService plannerAgentService;
    @Autowired
    private MesDispatchRecommendService dispatchRecommendService;
    @Autowired
    private QualityReportAiService qualityReportAiService;

    @Transactional
    public Object execute(MesActionRequest req) {
        if (req == null || req.getAction() == null || req.getAction().isBlank()) {
            throw new BusinessException("动作不能为空");
        }
        Map<String, Object> payload = req.getPayload() != null ? req.getPayload() : Map.of();
        String operator = req.getOperator() != null ? req.getOperator() : "system";
        String roleKey = req.getRoleKey() != null ? req.getRoleKey() : "system";

        return switch (req.getAction()) {
            case "createOrder" -> createOrder(payload, operator, roleKey);
            case "auditOrder" -> auditOrder(payload, operator, roleKey);
            case "submitOrder" -> submitOrder(payload, operator, roleKey);
            case "submitOrderToPlanner" -> submitOrderToPlanner(payload, operator, roleKey);
            case "createPlan" -> createPlan(payload, operator, roleKey);
            case "updatePlan" -> updatePlan(payload, operator, roleKey);
            case "previewPlanAgent" -> previewPlanAgent(payload, operator, roleKey);
            case "agentCreatePlan" -> agentCreatePlan(payload, operator, roleKey);
            case "previewSmartPlans" -> previewSmartPlans(payload, operator, roleKey);
            case "generateSmartPlans" -> generateSmartPlans(payload, operator, roleKey);
            case "agentBatchDispatch" -> agentBatchDispatch(payload, operator, roleKey);
            case "previewSmartDispatch" -> previewSmartDispatch(payload, operator, roleKey);
            case "confirmSmartDispatch" -> confirmSmartDispatch(payload, operator, roleKey);
            case "publishPlan" -> publishPlan(payload, operator, roleKey);
            case "submitPlanToManager" -> submitPlanToManager(payload, operator, roleKey);
            case "createWorkOrder" -> createWorkOrder(payload, operator, roleKey);
            case "releaseWorkOrder" -> releaseWorkOrder(payload, operator, roleKey);
            case "createDispatch" -> createDispatch(payload, operator, roleKey);
            case "acceptDispatch" -> acceptDispatch(payload, operator, roleKey);
            case "startDispatch" -> startDispatch(payload, operator, roleKey);
            case "submitReport" -> submitReport(payload, operator, roleKey);
            case "submitToInspection" -> submitToInspection(payload, operator, roleKey);
            case "confirmReport" -> confirmReport(payload, operator, roleKey);
            case "submitInspection" -> submitInspection(payload, operator, roleKey);
            case "generateQualityReport" -> generateQualityReport(payload, operator, roleKey);
            case "qualityReportDetail" -> qualityReportDetail(payload);
            case "scrapDefect" -> scrapDefect(payload, operator, roleKey);
            case "reworkDefect" -> reworkDefect(payload, operator, roleKey);
            case "confirmInbound" -> confirmInbound(payload, operator, roleKey);
            case "issueMaterial" -> issueMaterial(payload, operator, roleKey);
            case "shipDelivery" -> shipDelivery(payload, operator, roleKey);
            case "createPurchaseOrder" -> createPurchaseOrder(payload, operator, roleKey);
            case "receivePurchase" -> receivePurchase(payload, operator, roleKey);
            case "createAlarm" -> createAlarm(payload, operator, roleKey);
            case "handleAlarm" -> handleAlarm(payload, operator, roleKey);
            case "updateEquipment" -> updateEquipment(payload, operator, roleKey);
            case "createAftersale" -> createAftersale(payload, operator, roleKey);
            case "processAftersale" -> processAftersale(payload, operator, roleKey);
            case "confirmCostSettlement" -> confirmCostSettlement(payload, operator, roleKey);
            case "exportCostSettlement" -> exportCostSettlement(payload, operator, roleKey);
            case "saveUser" -> saveUser(payload, operator, roleKey);
            case "toggleUserStatus" -> toggleUserStatus(payload, operator, roleKey);
            case "resetUserPassword" -> resetUserPassword(payload, operator, roleKey);
            case "deleteOrder" -> deleteOrder(payload, operator, roleKey);
            case "deletePlan" -> deletePlanRecord(payload, operator, roleKey);
            case "deleteWorkOrder" -> deleteWorkOrderRecord(payload, operator, roleKey);
            case "deleteDispatch" -> deleteDispatchRecord(payload, operator, roleKey);
            case "deleteReport" -> deleteReportRecord(payload, operator, roleKey);
            case "deleteInspection" -> deleteInspectionRecord(payload, operator, roleKey);
            case "deleteDefect" -> deleteDefectRecord(payload, operator, roleKey);
            case "deletePurchaseOrder" -> deletePurchaseOrderRecord(payload, operator, roleKey);
            case "deleteDelivery" -> deleteDeliveryRecord(payload, operator, roleKey);
            case "deleteAlarm" -> deleteAlarmRecord(payload, operator, roleKey);
            case "deleteAftersale" -> deleteAftersaleRecord(payload, operator, roleKey);
            case "deleteCostSettlement" -> deleteCostSettlementRecord(payload, operator, roleKey);
            case "deleteInboundTask" -> deleteInboundTask(payload, operator, roleKey);
            case "deleteIssueTask" -> deleteIssueTask(payload, operator, roleKey);
            default -> throw new BusinessException("未知动作: " + req.getAction());
        };
    }

    // —— 订单 ——

    private Map<String, Object> createOrder(Map<String, Object> p, String operator, String roleKey) {
        LocalDateTime now = LocalDateTime.now();
        User user = findUserByUsername(operator);
        String orderNo = nextNo("CO", customerOrderMapper.customerOrderList(), CustomerOrder::getOrderNo);

        String customerName = str(p, "customerName");
        if (customerName.isBlank()) {
            customerName = resolveCustomerName(p.get("customerId"));
        }
        if (customerName.isBlank()) {
            throw new BusinessException("请选择客户");
        }

        String productModel = str(p, "productModel");
        Material material = resolveFinishedMaterial(productModel);
        if (material == null) {
            throw new BusinessException("未找到产品型号对应的成品物料：" + productModel);
        }

        BigDecimal quantity = decimal(p.get("quantity"));
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("订购数量必须大于 0");
        }
        BigDecimal unitPrice = material.getStandardCost() != null ? material.getStandardCost() : BigDecimal.ZERO;
        BigDecimal lineAmount = decimal(p.get("amount"));
        if (lineAmount.compareTo(BigDecimal.ZERO) <= 0) {
            lineAmount = unitPrice.multiply(quantity);
        }

        CustomerOrder order = new CustomerOrder();
        order.setOrderNo(orderNo);
        order.setCustomerName(customerName);
        order.setCustomerContact("");
        order.setCustomerPhone("");
        order.setOrderDate(LocalDate.now());
        order.setRequiredDeliveryDate(parseDate(str(p, "deliveryDate")));
        order.setOrderAmount(lineAmount);
        order.setAuditStatus("PENDING");
        order.setCreatedBy(user != null ? user.getUserId() : null);
        order.setRemark(str(p, "remark"));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        customerOrderMapper.insertCustomerOrder(order);

        CustomerOrderItem item = new CustomerOrderItem();
        item.setOrderId(order.getOrderId());
        item.setMaterialId(material.getMaterialId());
        item.setProductName(productModel.isBlank() ? material.getMaterialName() : productModel);
        item.setSpecification(str(p, "panelType"));
        item.setQuantity(quantity);
        item.setUnit("台");
        item.setUnitPrice(unitPrice);
        item.setLineAmount(lineAmount);
        item.setDeliveryDate(order.getRequiredDeliveryDate());
        item.setItemStatus("PENDING");
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        customerOrderItemMapper.insertOrderItem(item);

        MesRuntimeState runtime = mesRuntimeStore.load();
        Map<String, Object> extra = mesRuntimeStore.getExtra(runtime, "order:" + orderNo);
        extra.put("customerId", p.get("customerId"));
        extra.put("productModel", str(p, "productModel"));
        extra.put("panelType", str(p, "panelType", "LCD"));
        extra.put("quantity", intVal(p.get("quantity")));
        appendLog(runtime, "订单管理", "创建订单", orderNo, operator, roleKey);
        mesRuntimeStore.save(runtime);

        return Map.of("id", orderNo, "status", "待审核");
    }

    private boolean auditOrder(Map<String, Object> p, String operator, String roleKey) {
        String orderNo = str(p, "orderId");
        boolean pass = bool(p, "pass");
        CustomerOrder order = findOrderByNo(orderNo);
        if (order == null || !"PENDING".equals(order.getAuditStatus())) {
            throw new BusinessException("订单状态不允许审核");
        }
        LocalDateTime now = LocalDateTime.now();
        // 审核通过后直接进入计划员待办（PLAN_PENDING），形成订单→计划员闭环
        order.setAuditStatus(pass ? "PLAN_PENDING" : "REJECTED");
        order.setAuditAt(now);
        order.setUpdatedAt(now);
        User user = findUserByUsername(operator);
        if (user != null) {
            order.setAuditUserId(user.getUserId());
        }
        customerOrderMapper.updateCustomerOrder(order);
        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "订单管理", pass ? "审核通过并提交计划员" : "审核驳回", orderNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean submitOrder(Map<String, Object> p, String operator, String roleKey) {
        String orderNo = str(p, "orderId");
        CustomerOrder order = findOrderByNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setAuditStatus("PENDING");
        order.setUpdatedAt(LocalDateTime.now());
        customerOrderMapper.updateCustomerOrder(order);
        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "订单管理", "提交审核", orderNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean submitOrderToPlanner(Map<String, Object> p, String operator, String roleKey) {
        String orderNo = str(p, "orderId");
        CustomerOrder order = findOrderByNo(orderNo);
        if (order == null || !"APPROVED".equals(order.getAuditStatus())) {
            throw new BusinessException("订单状态不允许提交计划员");
        }
        order.setAuditStatus("PLAN_PENDING");
        order.setUpdatedAt(LocalDateTime.now());
        customerOrderMapper.updateCustomerOrder(order);
        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "订单管理", "提交计划员", orderNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    // —— 计划 ——

    private Map<String, Object> createPlan(Map<String, Object> p, String operator, String roleKey) {
        String orderNo = str(p, "orderId");
        CustomerOrder order = findOrderByNo(orderNo);
        if (order == null || !List.of("PLAN_PENDING", "APPROVED").contains(order.getAuditStatus())) {
            throw new BusinessException("订单状态不允许创建计划，请确认订单已审核通过并处于待计划状态");
        }
        LocalDateTime now = LocalDateTime.now();
        User planner = findUserByUsername(operator);
        String planNo = nextNo("PP", productionPlanMapper.planList(), ProductionPlan::getPlanNo);

        ProductionPlan plan = new ProductionPlan();
        plan.setPlanNo(planNo);
        plan.setPlanName("计划-" + orderNo);
        plan.setSourceOrderId(order.getOrderId());
        plan.setPlannedStartDate(parseDate(str(p, "planStart")));
        plan.setPlannedEndDate(parseDate(str(p, "planEnd")));
        plan.setPriority(str(p, "priority", "NORMAL"));
        plan.setPlanStatus("DRAFT");
        plan.setPlannerId(planner != null ? planner.getUserId() : null);
        plan.setRemark(str(p, "remark"));
        plan.setCreatedAt(now);
        plan.setUpdatedAt(now);
        productionPlanMapper.insertPlan(plan);

        CustomerOrderItem orderItem = firstOrderItem(order.getOrderId());
        if (orderItem != null) {
            int plannedQty = p.containsKey("plannedQty")
                    ? intVal(p.get("plannedQty"))
                    : orderItem.getQuantity().intValue();
            ProductionPlanItem planItem = new ProductionPlanItem();
            planItem.setPlanId(plan.getPlanId());
            planItem.setOrderItemId(orderItem.getOrderItemId());
            planItem.setMaterialId(orderItem.getMaterialId());
            planItem.setPlannedQuantity(BigDecimal.valueOf(plannedQty));
            planItem.setCompletedQuantity(BigDecimal.ZERO);
            planItem.setPlannedStartDate(plan.getPlannedStartDate());
            planItem.setPlannedEndDate(plan.getPlannedEndDate());
            planItem.setItemStatus("PENDING");
            planItem.setCreatedAt(now);
            planItem.setUpdatedAt(now);
            productionPlanItemMapper.insertPlanItem(planItem);
        }

        order.setAuditStatus("PLANNED");
        order.setUpdatedAt(now);
        customerOrderMapper.updateCustomerOrder(order);

        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "计划管理", "创建生产计划", planNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return Map.of("id", planNo);
    }

    private Map<String, Object> previewPlanAgent(Map<String, Object> p, String operator, String roleKey) {
        String orderNo = str(p, "orderId");
        LocalDate planStart = parseDate(str(p, "planStart"));
        LocalDate planEnd = parseDate(str(p, "planEnd"));
        Map<String, Object> analysis = plannerAgentService.analyze(orderNo, planStart, planEnd);
        analysis.put("agentRole", "planner");
        analysis.put("operator", operator);
        return analysis;
    }

    private Map<String, Object> agentCreatePlan(Map<String, Object> p, String operator, String roleKey) {
        String orderNo = str(p, "orderId");
        LocalDate planStart = parseDate(str(p, "planStart"));
        LocalDate planEnd = parseDate(str(p, "planEnd"));
        boolean autoSubmit = !"false".equalsIgnoreCase(str(p, "autoSubmit", "true"));

        Map<String, Object> analysis = plannerAgentService.analyze(orderNo, planStart, planEnd);
        int recommendedPlanQty = analysis.get("recommendedPlanQty") instanceof Number n ? n.intValue() : 0;
        if (p.containsKey("plannedQty")) {
            int overrideQty = intVal(p.get("plannedQty"));
            if (overrideQty > 0) {
                recommendedPlanQty = overrideQty;
            }
        }
        if (recommendedPlanQty <= 0) {
            throw new BusinessException(String.valueOf(analysis.getOrDefault("recommendation",
                    "库存充足，无需排产，请安排成品仓直接发货")));
        }

        Map<String, Object> createPayload = new LinkedHashMap<>(p);
        createPayload.put("orderId", orderNo);
        createPayload.put("planStart", planStart.toString());
        createPayload.put("planEnd", planEnd.toString());
        createPayload.put("plannedQty", recommendedPlanQty);
        createPayload.put("remark", truncateRemark("Agent智能排产：" + analysis.get("summary"), 500));
        Map<String, Object> created = createPlan(createPayload, operator, roleKey);
        String planNo = String.valueOf(created.get("id"));

        publishPlan(Map.of("planId", planNo), operator, roleKey);
        if (autoSubmit) {
            submitPlanToManager(Map.of("planId", planNo), operator, roleKey);
        }

        MesRuntimeState runtime = mesRuntimeStore.load();
        Map<String, Object> extra = mesRuntimeStore.getExtra(runtime, "plan:" + planNo);
        extra.put("agentRecommendation", analysis);
        extra.put("agentGenerated", true);
        extra.put("dispatchSuggestions", analysis.get("dispatchSuggestions"));
        appendLog(runtime, "计划Agent", "智能排产并提交主管", planNo, operator, roleKey);
        mesRuntimeStore.save(runtime);

        Map<String, Object> result = new LinkedHashMap<>(created);
        result.put("agentRecommendation", analysis);
        result.put("status", autoSubmit ? "已提交" : "已发布");
        result.put("message", autoSubmit
                ? "Agent 已创建生产计划并提交生产主管，主管可直接按建议派工"
                : "Agent 已创建并发布生产计划");
        return result;
    }

    private Map<String, Object> previewSmartPlans(Map<String, Object> p, String operator, String roleKey) {
        Map<String, Object> result = plannerAgentService.generateSmartPlanProposals();
        result.put("agentRole", "planner");
        result.put("operator", operator);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> generateSmartPlans(Map<String, Object> p, String operator, String roleKey) {
        List<String> orderIds = p.get("orderIds") instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();
        boolean autoPublish = !"false".equalsIgnoreCase(str(p, "autoPublish", "true"));

        Map<String, Map<String, Object>> overrideByOrder = new LinkedHashMap<>();
        if (p.get("proposals") instanceof List<?> overrideList) {
            for (Object item : overrideList) {
                if (item instanceof Map<?, ?> raw) {
                    Map<String, Object> ov = (Map<String, Object>) raw;
                    String orderNo = String.valueOf(ov.get("orderId"));
                    if (!orderNo.isBlank() && !"null".equals(orderNo)) {
                        overrideByOrder.put(orderNo, ov);
                    }
                }
            }
        }

        Map<String, Object> preview = plannerAgentService.generateSmartPlanProposals();
        List<Map<String, Object>> proposals = (List<Map<String, Object>>) preview.get("proposals");

        List<Map<String, Object>> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (Map<String, Object> proposal : proposals) {
            String orderNo = String.valueOf(proposal.get("orderId"));
            if (!orderIds.isEmpty() && !orderIds.contains(orderNo)) {
                continue;
            }
            Map<String, Object> override = overrideByOrder.get(orderNo);
            if (override != null) {
                mergeProposalOverride(proposal, override);
            }
            if (!Boolean.TRUE.equals(proposal.get("feasible"))) {
                skipped.add(orderNo + "（" + proposal.get("planStatus") + "）");
                continue;
            }
            int planQty = intVal(proposal.get("planQuantity"));
            if (planQty <= 0) {
                skipped.add(orderNo + "（计划数量为0）");
                continue;
            }

            Map<String, Object> createPayload = new LinkedHashMap<>();
            createPayload.put("orderId", orderNo);
            createPayload.put("planStart", proposal.get("planStart"));
            createPayload.put("planEnd", proposal.get("planEnd"));
            createPayload.put("plannedQty", planQty);
            createPayload.put("priority", priorityToDb(String.valueOf(proposal.get("priorityLevel"))));
            createPayload.put("remark", truncateRemark("智能生成：" + proposal.get("recommendation"), 500));

            Map<String, Object> planCreated = createPlan(createPayload, operator, roleKey);
            String planNo = String.valueOf(planCreated.get("id"));

            if (autoPublish) {
                publishPlan(Map.of("planId", planNo), operator, roleKey);
            }

            MesRuntimeState runtime = mesRuntimeStore.load();
            Map<String, Object> extra = mesRuntimeStore.getExtra(runtime, "plan:" + planNo);
            extra.put("smartPlanProposal", proposal);
            extra.put("agentGenerated", true);
            extra.put("priorityScore", proposal.get("priorityScore"));
            extra.put("riskWarnings", proposal.get("riskWarnings"));
            appendLog(runtime, "计划管理", "智能生成生产计划", planNo, operator, roleKey);
            mesRuntimeStore.save(runtime);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("planId", planNo);
            row.put("orderId", orderNo);
            row.put("productModel", proposal.get("productModel"));
            row.put("planQuantity", planQty);
            row.put("planStatus", autoPublish ? "已发布" : "草稿");
            created.add(row);
        }

        if (created.isEmpty() && skipped.isEmpty()) {
            throw new BusinessException("没有可生成的生产计划，请先执行智能预览");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("createdCount", created.size());
        result.put("message", String.format("已智能生成 %d 份生产计划", created.size()));
        return result;
    }

    private String priorityToDb(String level) {
        return switch (level) {
            case "高" -> "HIGH";
            case "低" -> "LOW";
            default -> "NORMAL";
        };
    }

    private void mergeProposalOverride(Map<String, Object> proposal, Map<String, Object> override) {
        if (override.containsKey("planQuantity")) {
            proposal.put("planQuantity", intVal(override.get("planQuantity")));
        }
        if (override.containsKey("planStart")) {
            proposal.put("planStart", String.valueOf(override.get("planStart")));
        }
        if (override.containsKey("planEnd")) {
            proposal.put("planEnd", String.valueOf(override.get("planEnd")));
        }
        if (override.containsKey("priorityLevel")) {
            proposal.put("priorityLevel", String.valueOf(override.get("priorityLevel")));
        }
        if (override.containsKey("remark")) {
            proposal.put("recommendation", String.valueOf(override.get("remark")));
        }
        if (override.containsKey("feasible")) {
            proposal.put("feasible", Boolean.TRUE.equals(override.get("feasible")));
            if (Boolean.TRUE.equals(override.get("feasible"))) {
                proposal.put("planStatus", "可生成");
            }
        }
    }

    private boolean updatePlan(Map<String, Object> p, String operator, String roleKey) {
        String planNo = str(p, "planId");
        ProductionPlan plan = findPlanByNo(planNo);
        if (plan == null || !List.of("DRAFT", "PUBLISHED").contains(plan.getPlanStatus())) {
            throw new BusinessException("仅草稿或已发布状态的计划可修改");
        }
        LocalDateTime now = LocalDateTime.now();
        if (p.containsKey("planStart")) {
            plan.setPlannedStartDate(parseDate(str(p, "planStart")));
        }
        if (p.containsKey("planEnd")) {
            plan.setPlannedEndDate(parseDate(str(p, "planEnd")));
        }
        if (p.containsKey("priority")) {
            plan.setPriority(str(p, "priority"));
        }
        if (p.containsKey("remark")) {
            plan.setRemark(str(p, "remark"));
        }
        plan.setUpdatedAt(now);
        productionPlanMapper.updatePlan(plan);

        if (p.containsKey("plannedQty")) {
            int qty = intVal(p.get("plannedQty"));
            if (qty > 0) {
                ProductionPlanItem item = productionPlanItemMapper.planItemList().stream()
                        .filter(i -> plan.getPlanId().equals(i.getPlanId()))
                        .findFirst().orElse(null);
                if (item != null) {
                    item.setPlannedQuantity(BigDecimal.valueOf(qty));
                    item.setPlannedStartDate(plan.getPlannedStartDate());
                    item.setPlannedEndDate(plan.getPlannedEndDate());
                    item.setUpdatedAt(now);
                    productionPlanItemMapper.updatePlanItem(item);
                }
            }
        }

        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "计划管理", "修改生产计划", planNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private Map<String, Object> agentBatchDispatch(Map<String, Object> p, String operator, String roleKey) {
        String planNo = str(p, "planId");
        ProductionPlan plan = findPlanByNo(planNo);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }
        MesRuntimeState runtime = mesRuntimeStore.load();
        Map<String, Object> extra = runtime.getExtras().getOrDefault("plan:" + planNo, Map.of());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> suggestions = (List<Map<String, Object>>) extra.get("dispatchSuggestions");
        if (suggestions == null || suggestions.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> agent = (Map<String, Object>) extra.get("agentRecommendation");
            if (agent != null) {
                suggestions = (List<Map<String, Object>>) agent.get("dispatchSuggestions");
            }
        }
        if (suggestions == null || suggestions.isEmpty()) {
            throw new BusinessException("该计划没有 Agent 派工建议，请手动派工");
        }

        WorkOrder wo = workOrderMapper.workOrderList().stream()
                .filter(w -> plan.getPlanId().equals(w.getPlanId()))
                .findFirst()
                .orElse(null);
        if (wo == null) {
            Map<String, Object> woCreated = createWorkOrder(Map.of("planId", planNo), operator, roleKey);
            String woNo = String.valueOf(woCreated.get("id"));
            wo = findWorkOrderByNo(woNo);
        }
        if (wo == null) {
            throw new BusinessException("无法生成工单");
        }

        List<Map<String, Object>> created = new ArrayList<>();
        for (Map<String, Object> sug : suggestions) {
            Map<String, Object> dispatchPayload = new LinkedHashMap<>();
            dispatchPayload.put("workOrderId", wo.getWorkOrderNo());
            dispatchPayload.put("processStep", sug.get("processStep"));
            dispatchPayload.put("equipment", sug.get("equipment"));
            dispatchPayload.put("planQty", sug.getOrDefault("planQty", wo.getPlannedQuantity()));
            dispatchPayload.put("operator", str(p, "operator", "wang_operator"));
            dispatchPayload.put("operatorName", str(p, "operatorName", "王操作"));
            created.add(createDispatch(dispatchPayload, operator, roleKey));
        }

        appendLog(runtime, "生产Agent", "按 Agent 建议批量派工", planNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return Map.of("planId", planNo, "workOrderId", wo.getWorkOrderNo(), "dispatches", created, "count", created.size());
    }

    private Map<String, Object> previewSmartDispatch(Map<String, Object> p, String operator, String roleKey) {
        String planNo = str(p, "planId");
        Map<String, Object> result;
        if (planNo == null || planNo.isBlank()) {
            result = dispatchRecommendService.generateAllRecommendations();
        } else {
            result = dispatchRecommendService.generateRecommendations(planNo);
        }
        result.put("operator", operator);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> confirmSmartDispatch(Map<String, Object> p, String operator, String roleKey) {
        String planNo = str(p, "planId");
        if (planNo == null || planNo.isBlank()) {
            throw new BusinessException("请指定计划编号");
        }

        Map<String, Object> preview = dispatchRecommendService.generateRecommendations(planNo);
        List<Map<String, Object>> recommendations = (List<Map<String, Object>>) preview.get("recommendations");
        if (recommendations == null || recommendations.isEmpty()) {
            throw new BusinessException("没有可确认的派工推荐");
        }

        WorkOrder wo = workOrderMapper.workOrderList().stream()
                .filter(w -> {
                    ProductionPlan plan = findPlanByNo(planNo);
                    return plan != null && plan.getPlanId().equals(w.getPlanId());
                })
                .findFirst().orElse(null);

        String workOrderNo;
        if (wo == null) {
            Map<String, Object> woCreated = createWorkOrder(Map.of("planId", planNo), operator, roleKey);
            workOrderNo = String.valueOf(woCreated.get("id"));
            wo = findWorkOrderByNo(workOrderNo);
        } else {
            workOrderNo = wo.getWorkOrderNo();
        }
        if (wo == null) {
            throw new BusinessException("无法生成正式工单");
        }

        List<Map<String, Object>> created = new ArrayList<>();
        for (Map<String, Object> rec : recommendations) {
            Map<String, Object> dispatchPayload = new LinkedHashMap<>();
            dispatchPayload.put("workOrderId", workOrderNo);
            dispatchPayload.put("processStep", rec.get("processStep"));
            dispatchPayload.put("equipment", rec.get("equipmentName"));
            dispatchPayload.put("planQty", rec.getOrDefault("planQty", wo.getPlannedQuantity()));
            dispatchPayload.put("operator", rec.get("recommendedOperator"));
            dispatchPayload.put("operatorName", rec.get("recommendedOperatorName"));
            created.add(createDispatch(dispatchPayload, operator, roleKey));
        }

        MesRuntimeState runtime = mesRuntimeStore.load();
        Map<String, Object> extra = mesRuntimeStore.getExtra(runtime, "plan:" + planNo);
        extra.put("smartDispatchRecommendations", recommendations);
        extra.put("smartDispatchConfirmed", true);
        appendLog(runtime, "生产管理", "智能派工确认并生成工单", workOrderNo, operator, roleKey);
        mesRuntimeStore.save(runtime);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("planId", planNo);
        result.put("workOrderId", workOrderNo);
        result.put("dispatches", created);
        result.put("count", created.size());
        result.put("message", String.format("已生成工单 %s 并确认 %d 条派工", workOrderNo, created.size()));
        return result;
    }

    private boolean publishPlan(Map<String, Object> p, String operator, String roleKey) {
        String planNo = str(p, "planId");
        ProductionPlan plan = findPlanByNo(planNo);
        if (plan == null || !"DRAFT".equals(plan.getPlanStatus())) {
            throw new BusinessException("计划状态不允许发布");
        }
        plan.setPlanStatus("PUBLISHED");
        plan.setUpdatedAt(LocalDateTime.now());
        productionPlanMapper.updatePlan(plan);
        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "计划管理", "发布生产计划", planNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean submitPlanToManager(Map<String, Object> p, String operator, String roleKey) {
        String planNo = str(p, "planId");
        ProductionPlan plan = findPlanByNo(planNo);
        if (plan == null || !"PUBLISHED".equals(plan.getPlanStatus())) {
            throw new BusinessException("计划状态不允许提交主管");
        }
        LocalDateTime now = LocalDateTime.now();
        plan.setPlanStatus("SUBMITTED");
        plan.setApprovedAt(now);
        plan.setUpdatedAt(now);
        productionPlanMapper.updatePlan(plan);
        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "计划管理", "提交生产主管", planNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    // —— 工单 ——

    private Map<String, Object> createWorkOrder(Map<String, Object> p, String operator, String roleKey) {
        String planNo = str(p, "planId");
        ProductionPlan plan = findPlanByNo(planNo);
        if (plan == null || !"SUBMITTED".equals(plan.getPlanStatus())) {
            throw new BusinessException("计划状态不允许创建工单");
        }
        LocalDateTime now = LocalDateTime.now();
        User manager = findUserByUsername(operator);
        String woNo = nextNo("WO", workOrderMapper.workOrderList(), WorkOrder::getWorkOrderNo);

        CustomerOrderItem item = firstOrderItem(plan.getSourceOrderId());
        Long materialId = item != null ? item.getMaterialId() : null;
        if (materialId == null) {
            Material material = resolveFinishedMaterial(orderProductModel(plan.getSourceOrderId()));
            if (material != null) {
                materialId = material.getMaterialId();
            }
        }
        if (materialId == null) {
            throw new BusinessException("未找到订单对应的成品物料，无法生成工单");
        }
        Long routeId = resolveRouteId(materialId);
        ProductionPlanItem planItem = findPlanItem(plan.getPlanId(), materialId);

        WorkOrder wo = new WorkOrder();
        wo.setWorkOrderNo(woNo);
        wo.setPlanId(plan.getPlanId());
        wo.setPlanItemId(planItem != null ? planItem.getPlanItemId() : null);
        wo.setMaterialId(materialId);
        wo.setRouteId(routeId);
        wo.setPlannedQuantity(planItem != null ? planItem.getPlannedQuantity()
                : (item != null ? item.getQuantity() : BigDecimal.ONE));
        wo.setCompletedQuantity(BigDecimal.ZERO);
        wo.setQualifiedQuantity(BigDecimal.ZERO);
        wo.setUnqualifiedQuantity(BigDecimal.ZERO);
        if (plan.getPlannedStartDate() != null) {
            wo.setPlannedStartTime(plan.getPlannedStartDate().atStartOfDay());
        }
        if (plan.getPlannedEndDate() != null) {
            wo.setPlannedEndTime(plan.getPlannedEndDate().atTime(23, 59, 59));
        }
        wo.setStatus("DRAFT");
        wo.setCreatedBy(manager != null ? manager.getUserId() : null);
        wo.setCreatedAt(now);
        wo.setUpdatedAt(now);
        workOrderMapper.insertWorkOrder(wo);

        plan.setPlanStatus("EXECUTING");
        plan.setUpdatedAt(now);
        productionPlanMapper.updatePlan(plan);

        CustomerOrder order = customerOrderMapper.getCustomerOrderById(plan.getSourceOrderId());
        if (order != null) {
            order.setAuditStatus("PRODUCING");
            order.setUpdatedAt(now);
            customerOrderMapper.updateCustomerOrder(order);
        }

        MesRuntimeState runtime = mesRuntimeStore.load();
        applyWorkOrderRelease(wo, manager, now, runtime, woNo);
        appendLog(runtime, "生产管理", "创建并下达生产工单", woNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return Map.of("id", woNo);
    }

    private boolean releaseWorkOrder(Map<String, Object> p, String operator, String roleKey) {
        String woNo = str(p, "woId");
        WorkOrder wo = findWorkOrderByNo(woNo);
        if (wo == null || !"DRAFT".equals(wo.getStatus())) {
            throw new BusinessException("工单状态不允许下达");
        }
        LocalDateTime now = LocalDateTime.now();
        User user = findUserByUsername(operator);

        MesRuntimeState runtime = mesRuntimeStore.load();
        applyWorkOrderRelease(wo, user, now, runtime, woNo);
        appendLog(runtime, "生产管理", "下达工单", woNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private void applyWorkOrderRelease(WorkOrder wo, User user, LocalDateTime now,
                                       MesRuntimeState runtime, String woNo) {
        wo.setStatus("RELEASED");
        wo.setReleasedBy(user != null ? user.getUserId() : null);
        wo.setReleasedAt(now);
        wo.setUpdatedAt(now);
        workOrderMapper.updateWorkOrder(wo);

        createIssueTasksFromBom(wo, runtime, woNo, now);
    }

    private void ensureWorkOrderReadyForDispatch(WorkOrder wo, String woNo, String operator, String roleKey) {
        if ("DRAFT".equals(wo.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            User user = findUserByUsername(operator);
            MesRuntimeState runtime = mesRuntimeStore.load();
            applyWorkOrderRelease(wo, user, now, runtime, woNo);
            appendLog(runtime, "生产管理", "下达工单", woNo, operator, roleKey);
            mesRuntimeStore.save(runtime);
        } else if ("RUNNING".equals(wo.getStatus())) {
            wo.setStatus("PRODUCING");
            wo.setUpdatedAt(LocalDateTime.now());
            workOrderMapper.updateWorkOrder(wo);
        }
    }

    // —— 派工 ——

    private Map<String, Object> createDispatch(Map<String, Object> p, String operator, String roleKey) {
        String woNo = str(p, "workOrderId");
        WorkOrder wo = findWorkOrderByNo(woNo);
        if (wo == null) {
            throw new BusinessException("未找到生产工单：" + woNo);
        }
        ensureWorkOrderReadyForDispatch(wo, woNo, operator, roleKey);
        if (!List.of("RELEASED", "DISPATCHED", "PRODUCING").contains(wo.getStatus())) {
            throw new BusinessException("工单状态不允许派工（当前：" + MesStatusMapper.toWorkOrderCn(wo.getStatus()) + "）");
        }
        LocalDateTime now = LocalDateTime.now();
        User opUser = resolveOperator(p);
        if (opUser == null) {
            throw new BusinessException("未找到操作员：" + str(p, "operator"));
        }
        User assigner = findUserByUsername(operator);
        ProcessStep step = findStepByName(str(p, "processStep"));
        Equipment eq = findEquipmentByName(str(p, "equipment"));
        String dispatchNo = nextNo("DT", dispatchTaskMapper.dispatchList(), DispatchTask::getDispatchNo);

        DispatchTask d = new DispatchTask();
        d.setDispatchNo(dispatchNo);
        d.setWorkOrderId(wo.getWorkOrderId());
        d.setStepId(step != null ? step.getStepId() : 1L);
        d.setOperatorId(opUser != null ? opUser.getUserId() : null);
        d.setEquipmentId(eq != null ? eq.getEquipmentId() : null);
        d.setAssignedQuantity(decimal(p.get("planQty")));
        d.setAcceptedQuantity(BigDecimal.ZERO);
        d.setCompletedQuantity(BigDecimal.ZERO);
        d.setAssignedBy(assigner != null ? assigner.getUserId() : null);
        d.setAssignedAt(now);
        d.setStatus("ASSIGNED");
        d.setCreatedAt(now);
        d.setUpdatedAt(now);
        dispatchTaskMapper.insertDispatch(d);

        if ("RELEASED".equals(wo.getStatus())) {
            wo.setStatus("DISPATCHED");
        }
        wo.setUpdatedAt(now);
        workOrderMapper.updateWorkOrder(wo);

        MesRuntimeState runtime = mesRuntimeStore.load();
        Map<String, Object> extra = mesRuntimeStore.getExtra(runtime, "dispatch:" + dispatchNo);
        extra.put("processStep", str(p, "processStep"));
        extra.put("equipment", str(p, "equipment"));
        extra.put("operator", opUser != null ? opUser.getUsername() : str(p, "operator"));
        extra.put("operatorName", str(p, "operatorName", opUser != null ? opUser.getRealName() : ""));
        extra.put("planStart", str(p, "planStart", fmt(now)));
        extra.put("planEnd", str(p, "planEnd", ""));
        appendLog(runtime, "生产管理", "派工给 " + extra.get("operatorName"), dispatchNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return Map.of("id", dispatchNo);
    }

    private boolean acceptDispatch(Map<String, Object> p, String operator, String roleKey) {
        String dispatchNo = str(p, "dispatchId");
        DispatchTask d = findDispatchByNo(dispatchNo);
        if (d == null || !"ASSIGNED".equals(d.getStatus())) {
            throw new BusinessException("派工状态不允许接收");
        }
        User op = findUserByUsername(operator);
        if (op == null || !op.getUserId().equals(d.getOperatorId())) {
            throw new BusinessException("无权接收该派工");
        }
        LocalDateTime now = LocalDateTime.now();
        d.setStatus("ACCEPTED");
        d.setAcceptedAt(now);
        d.setUpdatedAt(now);
        dispatchTaskMapper.updateDispatch(d);
        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "现场作业", "接收派工", dispatchNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean startDispatch(Map<String, Object> p, String operator, String roleKey) {
        String dispatchNo = str(p, "dispatchId");
        DispatchTask d = findDispatchByNo(dispatchNo);
        if (d == null || !"ACCEPTED".equals(d.getStatus())) {
            throw new BusinessException("派工状态不允许开始生产");
        }
        User op = findUserByUsername(operator);
        if (op == null || !op.getUserId().equals(d.getOperatorId())) {
            throw new BusinessException("无权操作该派工");
        }
        LocalDateTime now = LocalDateTime.now();
        d.setStatus("PRODUCING");
        d.setUpdatedAt(now);
        dispatchTaskMapper.updateDispatch(d);

        WorkOrder wo = workOrderMapper.getWorkOrderById(d.getWorkOrderId());
        if (wo != null) {
            wo.setStatus("PRODUCING");
            wo.setActualStartTime(now);
            wo.setUpdatedAt(now);
            workOrderMapper.updateWorkOrder(wo);
        }
        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "现场作业", "开始生产", dispatchNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    // —— 报工 ——

    private Map<String, Object> submitReport(Map<String, Object> p, String operator, String roleKey) {
        String dispatchNo = str(p, "dispatchId");
        DispatchTask d = findDispatchByNo(dispatchNo);
        if (d == null || !List.of("ACCEPTED", "PRODUCING", "RUNNING").contains(d.getStatus())) {
            throw new BusinessException("派工状态不允许报工");
        }
        LocalDateTime now = LocalDateTime.now();
        User op = findUserByUsername(operator);
        String reportNo = nextNo("WR", workReportMapper.reportList(), WorkReport::getReportNo);

        WorkReport r = new WorkReport();
        r.setReportNo(reportNo);
        r.setWorkOrderId(d.getWorkOrderId());
        r.setDispatchId(d.getDispatchId());
        r.setStepId(d.getStepId());
        r.setOperatorId(op != null ? op.getUserId() : d.getOperatorId());
        r.setReportDate(LocalDate.now());
        r.setCompletedQuantity(decimal(p.get("reportQty")));
        r.setQualifiedQuantity(decimal(p.get("qualifiedQty")));
        r.setUnqualifiedQuantity(decimal(p.get("unqualifiedQty")));
        r.setWorkHours(decimal(p.get("workHours")).compareTo(BigDecimal.ZERO) > 0
                ? decimal(p.get("workHours")) : BigDecimal.ONE);
        r.setReportStatus("SUBMITTED");
        r.setRemark(str(p, "remark"));
        r.setCreatedAt(now);
        r.setUpdatedAt(now);
        workReportMapper.insertReport(r);

        BigDecimal reportQty = decimal(p.get("reportQty"));
        d.setCompletedQuantity(d.getCompletedQuantity().add(reportQty));
        if ("ACCEPTED".equals(d.getStatus()) || "RUNNING".equals(d.getStatus())) {
            d.setStatus("PRODUCING");
        }
        d.setUpdatedAt(now);
        dispatchTaskMapper.updateDispatch(d);

        WorkOrder wo = workOrderMapper.getWorkOrderById(d.getWorkOrderId());
        if (wo != null) {
            wo.setCompletedQuantity(wo.getCompletedQuantity().add(reportQty));
            if (!"QC_PENDING".equals(wo.getStatus())) {
                wo.setStatus("PRODUCING");
            }
            wo.setUpdatedAt(now);
            workOrderMapper.updateWorkOrder(wo);
        }

        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "现场作业", "提交报工", reportNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return Map.of("id", reportNo);
    }

    private String submitToInspection(Map<String, Object> p, String operator, String roleKey) {
        String dispatchNo = str(p, "dispatchId");
        DispatchTask d = findDispatchByNo(dispatchNo);
        if (d == null || !List.of("PRODUCING", "RUNNING").contains(d.getStatus())) {
            throw new BusinessException("派工状态不允许提交质检");
        }
        if (d.getCompletedQuantity().compareTo(d.getAssignedQuantity()) < 0) {
            throw new BusinessException("报工数量未达标");
        }
        int qualifiedQty = reportQualifiedQty(d.getDispatchId());
        if (qualifiedQty <= 0) {
            throw new BusinessException("无合格数量可提交质检");
        }
        for (QualityInspection existing : qualityInspectionMapper.inspectionList()) {
            Map<String, Object> extra = mesRuntimeStore.load().getExtras()
                    .getOrDefault("inspection:" + existing.getInspectionNo(), Map.of());
            if (dispatchNo.equals(extra.get("dispatchId"))
                    && "PENDING".equals(existing.getInspectionResult())) {
                throw new BusinessException("已有待检任务");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        WorkOrder wo = workOrderMapper.getWorkOrderById(d.getWorkOrderId());
        String qcNo = nextNo("QI", qualityInspectionMapper.inspectionList(), QualityInspection::getInspectionNo);
        boolean isRework = "返修".equals(getDispatchExtra(d.getDispatchNo(), "processStep"));

        QualityInspection qi = new QualityInspection();
        qi.setInspectionNo(qcNo);
        qi.setWorkOrderId(d.getWorkOrderId());
        if (wo != null) {
            qi.setMaterialId(wo.getMaterialId());
        }
        if (qi.getMaterialId() == null) {
            throw new BusinessException("工单缺少成品物料，无法提交质检");
        }
        qi.setWorkReportId(findLatestReportId(d.getDispatchId()));
        qi.setBatchNo("BATCH-" + (wo != null ? wo.getWorkOrderNo() : "") + "-" + System.currentTimeMillis() % 10000);
        qi.setInspectionType(inspectionTypeToDb(isRework ? "复检" : "终检"));
        qi.setSampleQuantity(BigDecimal.valueOf(Math.min(10, qualifiedQty)));
        qi.setQualifiedQuantity(BigDecimal.ZERO);
        qi.setUnqualifiedQuantity(BigDecimal.ZERO);
        qi.setInspectionResult("PENDING");
        User qcUser = findDefaultQcUser();
        if (qcUser == null) {
            throw new BusinessException("未配置质检员账号");
        }
        qi.setInspectorId(qcUser.getUserId());
        qi.setInspectedAt(now);
        qi.setCreatedAt(now);
        qi.setUpdatedAt(now);
        qualityInspectionMapper.insertInspection(qi);

        d.setStatus("QC_PENDING");
        d.setUpdatedAt(now);
        dispatchTaskMapper.updateDispatch(d);
        if (wo != null) {
            wo.setStatus("QC_PENDING");
            wo.setUpdatedAt(now);
            workOrderMapper.updateWorkOrder(wo);
        }

        MesRuntimeState runtime = mesRuntimeStore.load();
        Map<String, Object> extra = mesRuntimeStore.getExtra(runtime, "inspection:" + qcNo);
        extra.put("dispatchId", dispatchNo);
        extra.put("submitQty", qualifiedQty);
        extra.put("qcItems", List.of("外观检查", "点亮测试", "坏点检测"));
        extra.put("operatorName", getDispatchExtra(dispatchNo, "operatorName"));
        if (getDispatchExtra(d.getDispatchNo(), "defectId") != null) {
            extra.put("defectId", getDispatchExtra(d.getDispatchNo(), "defectId"));
        }
        appendLog(runtime, "现场作业", "提交质检 " + qualifiedQty + " 台", dispatchNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return qcNo;
    }

    private boolean confirmReport(Map<String, Object> p, String operator, String roleKey) {
        String reportNo = str(p, "reportId");
        WorkReport r = findReportByNo(reportNo);
        if (r == null || !"SUBMITTED".equals(r.getReportStatus())) {
            throw new BusinessException("报工状态不允许确认");
        }
        boolean pass = bool(p, "pass");
        r.setReportStatus(pass ? "CONFIRMED" : "REJECTED");
        r.setUpdatedAt(LocalDateTime.now());
        workReportMapper.updateReport(r);
        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "生产管理", pass ? "确认报工" : "驳回报工", reportNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    // —— 质检 ——

    @SuppressWarnings("unchecked")
    private boolean submitInspection(Map<String, Object> p, String operator, String roleKey) {
        String qcNo = str(p, "qcId");
        Map<String, Object> payload = (Map<String, Object>) p.getOrDefault("payload", p);
        QualityInspection qi = findInspectionByNo(qcNo);
        if (qi == null || !"PENDING".equals(qi.getInspectionResult())) {
            throw new BusinessException("质检状态不允许提交");
        }
        LocalDateTime now = LocalDateTime.now();
        User inspector = findUserByUsername(operator);
        String result = str(payload, "result");
        qi.setInspectionType(inspectionTypeToDb(str(payload, "qcType", qi.getInspectionType())));
        qi.setSampleQuantity(decimal(payload.get("sampleQty")));
        qi.setQualifiedQuantity(decimal(payload.get("qualifiedQty")));
        qi.setUnqualifiedQuantity(decimal(payload.get("unqualifiedQty")));
        qi.setInspectionResult(MesStatusMapper.inspectionResultToDb(result));
        qi.setInspectorId(inspector != null ? inspector.getUserId() : null);
        qi.setInspectedAt(now);
        qi.setRemark(str(payload, "remark"));
        qi.setUpdatedAt(now);
        qualityInspectionMapper.updateInspection(qi);

        WorkOrder wo = workOrderMapper.getWorkOrderById(qi.getWorkOrderId());
        MesRuntimeState runtime = mesRuntimeStore.load();
        Map<String, Object> inspExtra = runtime.getExtras().getOrDefault("inspection:" + qcNo, Map.of());
        String dispatchNo = String.valueOf(inspExtra.getOrDefault("dispatchId", ""));
        DispatchTask dispatch = findDispatchByNo(dispatchNo);

        if ("合格".equals(result) || "让步接收".equals(result)) {
            int qualQty = intVal(payload.get("qualifiedQty"));
            addInboundTask(runtime, result, qcNo, wo, qi, qualQty);
            if (dispatch != null) {
                dispatch.setStatus("COMPLETED");
                dispatch.setUpdatedAt(now);
                dispatchTaskMapper.updateDispatch(dispatch);
            }
            if (wo != null) {
                wo.setQualifiedQuantity(wo.getQualifiedQuantity().add(decimal(payload.get("qualifiedQty"))));
                syncWorkOrderStatus(wo);
                syncPlanProgress(wo, qualQty);
                workOrderMapper.updateWorkOrder(wo);
            }
            String defectId = String.valueOf(inspExtra.getOrDefault("defectId", ""));
            if (!defectId.isBlank()) {
                NonconformingProduct defect = findDefectByNo(defectId);
                if (defect != null) {
                    defect.setHandleStatus("REWORKED");
                    defect.setHandleMethod("已返修");
                    defect.setUpdatedAt(now);
                    nonconformingProductMapper.updateNonconforming(defect);
                }
            }
        } else if ("不合格".equals(result)) {
            int qualQty = intVal(payload.get("qualifiedQty"));
            if (qualQty > 0) {
                addInboundTask(runtime, "质检合格", qcNo, wo, qi, qualQty);
                if (wo != null) {
                    wo.setQualifiedQuantity(wo.getQualifiedQuantity().add(BigDecimal.valueOf(qualQty)));
                    syncPlanProgress(wo, qualQty);
                }
            }
            int defectQty = intVal(payload.get("unqualifiedQty"));
            if (defectQty > 0) {
                createDefectRecord(runtime, qi, dispatch, wo, payload, defectQty, operator, now);
            }
            if (dispatch != null) {
                dispatch.setStatus("COMPLETED");
                dispatch.setUpdatedAt(now);
                dispatchTaskMapper.updateDispatch(dispatch);
            }
            if (wo != null) {
                syncWorkOrderStatus(wo);
                workOrderMapper.updateWorkOrder(wo);
            }
        }

        Map<String, Object> extra = mesRuntimeStore.getExtra(runtime, "inspection:" + qcNo);
        extra.put("qcItems", payload.get("qcItems"));
        extra.put("inspectorName", str(payload, "inspectorName"));
        Map<String, Object> report = createQualityReport(qcNo, runtime, operator, payload);
        extra.put("qualityReportId", report.get("id"));
        appendLog(runtime, "质量管理", "质检" + result, qcNo, operator, roleKey);
        appendLog(runtime, "质量报告", "自动生成质量报告", String.valueOf(report.get("id")), operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private Map<String, Object> generateQualityReport(Map<String, Object> p, String operator, String roleKey) {
        String qcNo = str(p, "qcId");
        MesRuntimeState runtime = mesRuntimeStore.load();
        QualityInspection qi = findInspectionByNo(qcNo);
        if (qi == null || "PENDING".equals(qi.getInspectionResult())) {
            throw new BusinessException("质检单不存在或尚未完成");
        }
        Map<String, Object> report = createQualityReport(qcNo, runtime, operator, p);
        report.put("updatedAt", fmt(LocalDateTime.now()));
        appendLog(runtime, "质量报告", "重新生成质量报告", String.valueOf(report.get("id")), operator, roleKey);
        mesRuntimeStore.save(runtime);
        return report;
    }

    private Map<String, Object> qualityReportDetail(Map<String, Object> p) {
        String reportId = str(p, "reportId");
        String qcNo = str(p, "qcId");
        MesRuntimeState runtime = mesRuntimeStore.load();
        return runtime.getExtras().entrySet().stream()
                .filter(e -> e.getKey().startsWith("qualityReport:"))
                .map(Map.Entry::getValue)
                .filter(r -> reportId.isBlank() || reportId.equals(String.valueOf(r.get("id"))))
                .filter(r -> qcNo.isBlank() || qcNo.equals(String.valueOf(r.get("qcId"))))
                .findFirst()
                .orElseThrow(() -> new BusinessException("质量报告不存在"));
    }

    private Map<String, Object> createQualityReport(String qcNo, MesRuntimeState runtime,
                                                    String operator, Map<String, Object> payload) {
        QualityInspection qi = findInspectionByNo(qcNo);
        if (qi == null) {
            throw new BusinessException("质检单不存在");
        }
        WorkOrder wo = workOrderMapper.getWorkOrderById(qi.getWorkOrderId());
        ProductionPlan plan = wo != null ? productionPlanMapper.getPlanById(wo.getPlanId()) : null;
        CustomerOrder order = plan != null ? customerOrderMapper.getCustomerOrderById(plan.getSourceOrderId()) : null;
        CustomerOrderItem item = order != null ? firstOrderItem(order.getOrderId()) : null;
        User inspector = qi.getInspectorId() != null ? userMapper.getUserById(qi.getInspectorId()) : findUserByUsername(operator);
        Map<String, Object> inspExtra = mesRuntimeStore.getExtra(runtime, "inspection:" + qcNo);

        List<NonconformingProduct> relatedDefects = nonconformingProductMapper.nonconformingList().stream()
                .filter(d -> qi.getInspectionId().equals(d.getInspectionId()))
                .toList();
        Map<String, Integer> defectDistribution = new LinkedHashMap<>();
        int defectQty = 0;
        int reworkQty = 0;
        int scrapQty = 0;
        for (NonconformingProduct defect : relatedDefects) {
            int qty = intVal(defect.getQuantity());
            defectQty += qty;
            String type = defect.getDefectType() != null && !defect.getDefectType().isBlank()
                    ? defect.getDefectType() : "未分类";
            defectDistribution.merge(type, qty, Integer::sum);
            if (List.of("REWORKING", "REWORKED").contains(defect.getHandleStatus())) {
                reworkQty += qty;
            }
            if ("SCRAPPED".equals(defect.getHandleStatus())) {
                scrapQty += qty;
            }
        }

        int sampleQty = intVal(qi.getSampleQuantity());
        int qualifiedQty = intVal(qi.getQualifiedQuantity());
        int unqualifiedQty = intVal(qi.getUnqualifiedQuantity());
        double yieldRate = sampleQty > 0 ? qualifiedQty * 100.0 / sampleQty : 100.0;
        String result = MesStatusMapper.inspectionResultToCn(qi.getInspectionResult());
        String topDefect = defectDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("无明显不良");

        List<String> suggestions = new ArrayList<>();
        if (unqualifiedQty <= 0) {
            suggestions.add("本批次质量稳定，可按标准流程入库。");
        } else {
            suggestions.add("重点复核" + topDefect + "相关工序，抽查操作记录与设备点检记录。");
            suggestions.add("对同批次未检产品提高抽检比例，必要时追加老化测试。");
        }
        if (yieldRate < 95) {
            suggestions.add("合格率低于 95%，建议生产主管组织工艺、设备、质量联合复盘。");
        }

        String reportId = String.valueOf(inspExtra.getOrDefault("qualityReportId", ""));
        if (reportId.isBlank()) {
            reportId = nextRuntimeId("QR", qualityReports(runtime));
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", reportId);
        report.put("qcId", qcNo);
        report.put("orderId", order != null ? order.getOrderNo() : "");
        report.put("workOrderId", wo != null ? wo.getWorkOrderNo() : "");
        report.put("productModel", item != null ? item.getProductName() : getInspectionProductModel(qi));
        report.put("batchNo", qi.getBatchNo());
        report.put("qcType", qi.getInspectionType());
        report.put("result", result);
        report.put("sampleQty", sampleQty);
        report.put("qualifiedQty", qualifiedQty);
        report.put("unqualifiedQty", unqualifiedQty);
        report.put("yieldRate", BigDecimal.valueOf(yieldRate).setScale(1, java.math.RoundingMode.HALF_UP).doubleValue());
        report.put("defectQty", defectQty > 0 ? defectQty : unqualifiedQty);
        report.put("defectDistribution", defectDistribution);
        report.put("topDefect", topDefect);
        report.put("reworkQty", reworkQty);
        report.put("scrapQty", scrapQty);
        report.put("concessionQty", "让步接收".equals(result) ? qualifiedQty : 0);
        report.put("operatorName", inspExtra.getOrDefault("operatorName", ""));
        report.put("inspectorName", inspExtra.getOrDefault("inspectorName",
                inspector != null ? inspector.getRealName() : operator));
        report.put("conclusion", buildQualityConclusion(result, yieldRate, topDefect, unqualifiedQty));
        report.put("suggestions", suggestions);

        Map<String, Object> batchStats = new LinkedHashMap<>();
        batchStats.put("qcId", qcNo);
        batchStats.put("workOrderId", wo != null ? wo.getWorkOrderNo() : "");
        batchStats.put("productModel", item != null ? item.getProductName() : getInspectionProductModel(qi));
        batchStats.put("batchNo", qi.getBatchNo());
        batchStats.put("result", result);
        batchStats.put("sampleQty", sampleQty);
        batchStats.put("qualifiedQty", qualifiedQty);
        batchStats.put("unqualifiedQty", unqualifiedQty);
        batchStats.put("yieldRate", BigDecimal.valueOf(yieldRate).setScale(1, java.math.RoundingMode.HALF_UP).doubleValue());
        batchStats.put("topDefect", topDefect);
        batchStats.put("defectDistribution", defectDistribution);

        LocalDate statDate = qi.getInspectedAt() != null
                ? qi.getInspectedAt().toLocalDate() : LocalDate.now();
        Map<String, Object> dailyStats = qualityReportAiService.buildDailyStats(statDate);
        report.put("dailyStats", dailyStats);
        report.put("dailyInspectionCount", dailyStats.get("inspectionCount"));
        report.put("dailyTotalSample", dailyStats.get("totalSample"));
        report.put("dailyTotalQualified", dailyStats.get("totalQualified"));
        report.put("dailyTotalUnqualified", dailyStats.get("totalUnqualified"));
        report.put("dailyYieldRate", dailyStats.get("yieldRate"));
        report.put("dailyTopDefect", dailyStats.get("topDefect"));
        report.put("relatedWorkOrders", dailyStats.get("relatedWorkOrders"));

        QualityReportAiService.AiReportResult aiResult =
                qualityReportAiService.generateAnalysis(batchStats, dailyStats);
        report.put("aiAnalysis", aiResult.fullText());
        report.put("analysisSections", aiResult.sections());
        report.put("reportSource", aiResult.source());
        report.put("aiGenerated", aiResult.aiGenerated());

        report.put("createdAt", fmt(LocalDateTime.now()));
        report.put("updatedAt", fmt(LocalDateTime.now()));

        runtime.getExtras().put("qualityReport:" + reportId, report);
        inspExtra.put("qualityReportId", reportId);
        return report;
    }

    private String buildQualityConclusion(String result, double yieldRate, String topDefect, int unqualifiedQty) {
        if (unqualifiedQty <= 0) {
            return String.format("本批次质检结果为%s，抽检合格率 %.1f%%，未发现明显质量风险。", result, yieldRate);
        }
        return String.format("本批次质检结果为%s，抽检合格率 %.1f%%，不良主要集中在%s，建议优先排查对应工序与设备状态。",
                result, yieldRate, topDefect);
    }

    private List<Map<String, Object>> qualityReports(MesRuntimeState runtime) {
        return runtime.getExtras().entrySet().stream()
                .filter(e -> e.getKey().startsWith("qualityReport:"))
                .map(Map.Entry::getValue)
                .sorted((a, b) -> String.valueOf(b.getOrDefault("createdAt", ""))
                        .compareTo(String.valueOf(a.getOrDefault("createdAt", ""))))
                .toList();
    }

    private boolean scrapDefect(Map<String, Object> p, String operator, String roleKey) {
        String defectNo = str(p, "defectId");
        NonconformingProduct defect = findDefectByNo(defectNo);
        if (defect == null || !"PENDING".equals(defect.getHandleStatus())) {
            throw new BusinessException("不合格品状态不允许报废");
        }
        LocalDateTime now = LocalDateTime.now();
        User handler = findUserByUsername(operator);
        defect.setHandleStatus("SCRAPPED");
        defect.setHandleMethod("已报废");
        defect.setHandledBy(handler != null ? handler.getUserId() : null);
        defect.setHandledAt(now);
        defect.setRemark(str(p, "remark", defect.getDefectDescription()));
        defect.setUpdatedAt(now);
        nonconformingProductMapper.updateNonconforming(defect);

        WorkOrder wo = workOrderMapper.getWorkOrderById(defect.getWorkOrderId());
        if (wo != null) {
            syncWorkOrderStatus(wo);
            workOrderMapper.updateWorkOrder(wo);
        }
        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "质量管理", "报废不合格品 " + intVal(defect.getQuantity()) + " 台", defectNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private String reworkDefect(Map<String, Object> p, String operator, String roleKey) {
        String defectNo = str(p, "defectId");
        NonconformingProduct defect = findDefectByNo(defectNo);
        if (defect == null || !"PENDING".equals(defect.getHandleStatus())) {
            throw new BusinessException("不合格品状态不允许返修");
        }
        LocalDateTime now = LocalDateTime.now();
        User handler = findUserByUsername(operator);
        WorkOrder wo = workOrderMapper.getWorkOrderById(defect.getWorkOrderId());

        MesRuntimeState runtime = mesRuntimeStore.load();
        Map<String, Object> defectExtra = runtime.getExtras().getOrDefault("defect:" + defectNo, Map.of());
        String opUsername = String.valueOf(defectExtra.getOrDefault("operator", "operator"));
        User opUser = findUserByUsername(opUsername);
        String dispatchNo = nextNo("DT", dispatchTaskMapper.dispatchList(), DispatchTask::getDispatchNo);

        DispatchTask d = new DispatchTask();
        d.setDispatchNo(dispatchNo);
        d.setWorkOrderId(defect.getWorkOrderId());
        d.setStepId(1L);
        d.setOperatorId(opUser != null ? opUser.getUserId() : null);
        d.setAssignedQuantity(defect.getQuantity());
        d.setAcceptedQuantity(BigDecimal.ZERO);
        d.setCompletedQuantity(BigDecimal.ZERO);
        d.setAssignedAt(now);
        d.setStatus("ASSIGNED");
        d.setCreatedAt(now);
        d.setUpdatedAt(now);
        dispatchTaskMapper.insertDispatch(d);

        Map<String, Object> dispExtra = mesRuntimeStore.getExtra(runtime, "dispatch:" + dispatchNo);
        dispExtra.put("processStep", "返修");
        dispExtra.put("equipment", "返修工位");
        dispExtra.put("operator", opUsername);
        dispExtra.put("operatorName", defectExtra.getOrDefault("operatorName", "王操作"));
        dispExtra.put("defectId", defectNo);
        dispExtra.put("planStart", fmt(now));
        dispExtra.put("planEnd", fmt(now));

        defect.setHandleStatus("REWORKING");
        defect.setHandleMethod("返修");
        defect.setHandledBy(handler != null ? handler.getUserId() : null);
        defect.setHandledAt(now);
        defect.setUpdatedAt(now);
        nonconformingProductMapper.updateNonconforming(defect);

        if (wo != null) {
            wo.setStatus("PRODUCING");
            wo.setUpdatedAt(now);
            workOrderMapper.updateWorkOrder(wo);
        }
        appendLog(runtime, "质量管理", "派返修 " + intVal(defect.getQuantity()) + " 台", defectNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return dispatchNo;
    }

    // —— 仓储 ——

    private boolean confirmInbound(Map<String, Object> p, String operator, String roleKey) {
        String taskId = str(p, "taskId");
        MesRuntimeState runtime = mesRuntimeStore.load();
        Map<String, Object> task = findRuntimeTask(runtime.getInboundTasks(), taskId);
        if (task == null || !"待入库".equals(task.get("status"))) {
            throw new BusinessException("入库任务状态不允许确认");
        }
        LocalDateTime now = LocalDateTime.now();
        User handler = findUserByUsername(operator);
        task.put("status", "已入库");

        int qty = intVal(task.get("quantity"));
        String woNo = String.valueOf(task.getOrDefault("workOrderId", ""));
        WorkOrder wo = findWorkOrderByNo(woNo);
        Material mat = resolveFinishedMaterialForTask(task, wo);
        Inventory inv = mat != null ? findInventoryByMaterial(mat.getMaterialId()) : null;
        if (inv != null) {
            inv.setQuantityOnHand(inv.getQuantityOnHand().add(BigDecimal.valueOf(qty)));
            inv.setUpdatedAt(now);
            inventoryMapper.updateInventory(inv);
            recordInventoryTransaction(inv, mat, "PRODUCT_IN", BigDecimal.valueOf(qty),
                    wo != null ? wo.getWorkOrderId() : null, handler, now,
                    "成品入库 " + taskId);
        }

        String productModel = mat != null ? mat.getMaterialName()
                : String.valueOf(task.getOrDefault("productModel", ""));
        String materialCode = mat != null ? mat.getMaterialCode() : productModel;

        Map<String, Object> flow = new LinkedHashMap<>();
        flow.put("id", nextRuntimeId("SF", runtime.getStockFlows()));
        flow.put("flowType", "成品入库");
        flow.put("materialName", productModel);
        flow.put("materialCode", materialCode);
        flow.put("quantity", qty);
        flow.put("direction", "入");
        flow.put("refNo", taskId);
        flow.put("operator", operator);
        flow.put("createdAt", fmt(now));
        runtime.getStockFlows().add(0, flow);

        String orderNo = String.valueOf(task.getOrDefault("orderId", ""));
        if (!orderNo.isBlank()) {
            boolean hasPending = runtime.getInboundTasks().stream()
                    .noneMatch(t -> orderNo.equals(t.get("orderId")) && "待入库".equals(t.get("status")));
            boolean deliveryExists = deliveryOrderMapper.deliveryList().stream()
                    .anyMatch(d -> {
                        CustomerOrder o = findOrderByNo(orderNo);
                        return o != null && o.getOrderId().equals(d.getOrderId()) && !"SHIPPED".equals(d.getDeliveryStatus());
                    });
            if (hasPending && !deliveryExists) {
                CustomerOrder order = findOrderByNo(orderNo);
                if (order != null) {
                    DeliveryOrder dlv = new DeliveryOrder();
                    dlv.setDeliveryNo(nextNo("DO", deliveryOrderMapper.deliveryList(), DeliveryOrder::getDeliveryNo));
                    dlv.setOrderId(order.getOrderId());
                    dlv.setWorkOrderId(wo != null ? wo.getWorkOrderId() : null);
                    dlv.setCustomerName(order.getCustomerName());
                    dlv.setMaterialId(resolveDeliveryMaterialId(mat, wo, order.getOrderId()));
                    dlv.setBatchNo(String.valueOf(task.getOrDefault("batchNo", "")));
                    dlv.setDeliveryQuantity(BigDecimal.valueOf(qty));
                    dlv.setDeliveryDate(LocalDate.now().plusDays(3));
                    dlv.setDeliveryStatus("PREPARED");
                    dlv.setCreatedAt(now);
                    dlv.setUpdatedAt(now);
                    deliveryOrderMapper.insertDelivery(dlv);
                }
            }
        }

        appendLog(runtime, "仓储管理", "成品入库", taskId, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean issueMaterial(Map<String, Object> p, String operator, String roleKey) {
        String taskId = str(p, "taskId");
        int qty = intVal(p.get("qty"));
        MesRuntimeState runtime = mesRuntimeStore.load();
        Map<String, Object> task = findRuntimeTask(runtime.getIssueTasks(), taskId);
        if (task == null) {
            throw new BusinessException("领料任务不存在");
        }
        String materialCode = normalizeIssueMaterialCode(String.valueOf(task.get("materialCode")));
        task.put("materialCode", materialCode);
        Material mat = findMaterialByCode(materialCode);
        Inventory inv = mat != null ? findInventoryByMaterial(mat.getMaterialId()) : null;
        if (inv == null || inv.getQuantityOnHand().compareTo(BigDecimal.valueOf(qty)) < 0) {
            throw new BusinessException("库存不足");
        }
        LocalDateTime now = LocalDateTime.now();
        User handler = findUserByUsername(operator);
        inv.setQuantityOnHand(inv.getQuantityOnHand().subtract(BigDecimal.valueOf(qty)));
        inv.setUpdatedAt(now);
        inventoryMapper.updateInventory(inv);

        WorkOrder wo = findWorkOrderByNo(String.valueOf(task.get("workOrderId")));
        recordInventoryTransaction(inv, mat, "MATERIAL_OUT", BigDecimal.valueOf(qty),
                wo != null ? wo.getWorkOrderId() : null, handler, now,
                "工单" + task.get("workOrderId") + "领料");

        int issued = intVal(task.get("issuedQty")) + qty;
        int required = intVal(task.get("requiredQty"));
        task.put("issuedQty", issued);
        task.put("status", issued >= required ? "已完成" : "部分领料");

        Map<String, Object> flow = new LinkedHashMap<>();
        flow.put("id", nextRuntimeId("SF", runtime.getStockFlows()));
        flow.put("flowType", "生产领料");
        flow.put("materialCode", materialCode);
        flow.put("materialName", task.get("materialName"));
        flow.put("quantity", qty);
        flow.put("direction", "出");
        flow.put("refNo", task.get("workOrderId"));
        flow.put("operator", operator);
        flow.put("createdAt", fmt(now));
        runtime.getStockFlows().add(0, flow);

        appendLog(runtime, "仓储管理", "生产领料", taskId, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean shipDelivery(Map<String, Object> p, String operator, String roleKey) {
        String dlvNo = str(p, "dlvId");
        DeliveryOrder d = findDeliveryByNo(dlvNo);
        if (d == null || !List.of("PENDING", "PREPARED").contains(d.getDeliveryStatus())) {
            throw new BusinessException("发货单状态不允许出库");
        }
        LocalDateTime now = LocalDateTime.now();
        User handler = findUserByUsername(operator);
        d.setDeliveryStatus("SHIPPED");
        d.setDeliveryDate(LocalDate.now());
        d.setLogisticsNo("SF" + System.currentTimeMillis());
        d.setUpdatedAt(now);
        deliveryOrderMapper.updateDelivery(d);

        Material mat = materialMapper.materialList().stream()
                .filter(m -> d.getMaterialId().equals(m.getMaterialId())).findFirst().orElse(null);
        Inventory inv = d.getMaterialId() != null ? findInventoryByMaterial(d.getMaterialId()) : null;
        if (inv != null && mat != null) {
            int shipQty = intVal(d.getDeliveryQuantity());
            if (inv.getQuantityOnHand().compareTo(BigDecimal.valueOf(shipQty)) < 0) {
                throw new BusinessException("成品库存不足，无法发货");
            }
            inv.setQuantityOnHand(inv.getQuantityOnHand().subtract(BigDecimal.valueOf(shipQty)));
            inv.setUpdatedAt(now);
            inventoryMapper.updateInventory(inv);
            recordInventoryTransaction(inv, mat, "SALE_OUT", BigDecimal.valueOf(shipQty),
                    d.getWorkOrderId(), handler, now, "发货单" + dlvNo + "出库");
        }

        CustomerOrder order = customerOrderMapper.getCustomerOrderById(d.getOrderId());
        if (order != null) {
            order.setAuditStatus("SHIPPED");
            order.setUpdatedAt(now);
            customerOrderMapper.updateCustomerOrder(order);
        }

        MesRuntimeState runtime = mesRuntimeStore.load();
        Map<String, Object> flow = new LinkedHashMap<>();
        flow.put("id", nextRuntimeId("SF", runtime.getStockFlows()));
        flow.put("flowType", "发货出库");
        flow.put("materialName", mat != null ? mat.getMaterialName() : "");
        flow.put("materialCode", mat != null ? mat.getMaterialCode() : "");
        flow.put("quantity", intVal(d.getDeliveryQuantity()));
        flow.put("direction", "出");
        flow.put("refNo", dlvNo);
        flow.put("operator", operator);
        flow.put("createdAt", fmt(now));
        runtime.getStockFlows().add(0, flow);

        appendLog(runtime, "仓储管理", "发货出库", dlvNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    // —— 采购 ——

    private Map<String, Object> createPurchaseOrder(Map<String, Object> p, String operator, String roleKey) {
        LocalDateTime now = LocalDateTime.now();
        User buyer = findUserByUsername(operator);
        String poNo = nextNo("PO", purchaseOrderMapper.purchaseOrderList(), PurchaseOrder::getPurchaseOrderNo);
        Material mat = findMaterialByCode(str(p, "materialCode"));
        BigDecimal qty = decimal(p.get("quantity"));
        BigDecimal unitPrice = decimal(p.get("unitPrice"));

        PurchaseOrder po = new PurchaseOrder();
        po.setPurchaseOrderNo(poNo);
        po.setSupplierName(str(p, "supplier"));
        po.setPurchaseDate(LocalDate.now());
        po.setExpectedArrivalDate(parseDate(str(p, "expectedDate")));
        po.setTotalAmount(qty.multiply(unitPrice));
        po.setStatus("RELEASED");
        po.setPurchaserId(buyer != null ? buyer.getUserId() : null);
        po.setCreatedAt(now);
        po.setUpdatedAt(now);
        purchaseOrderMapper.insertPurchaseOrder(po);

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPurchaseOrderId(po.getPurchaseOrderId());
        item.setMaterialId(mat != null ? mat.getMaterialId() : null);
        item.setQuantity(qty);
        item.setReceivedQuantity(BigDecimal.ZERO);
        item.setUnitPrice(unitPrice);
        item.setLineAmount(po.getTotalAmount());
        item.setItemStatus("RELEASED");
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        purchaseOrderItemMapper.insertPurchaseOrderItem(item);

        MesRuntimeState runtime = mesRuntimeStore.load();
        Map<String, Object> extra = mesRuntimeStore.getExtra(runtime, "purchase:" + poNo);
        extra.put("materialCode", str(p, "materialCode"));
        extra.put("materialName", str(p, "materialName"));
        appendLog(runtime, "采购管理", "创建采购订单", poNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return Map.of("id", poNo);
    }

    private boolean receivePurchase(Map<String, Object> p, String operator, String roleKey) {
        String poNo = str(p, "poId");
        int qty = intVal(p.get("qty"));
        PurchaseOrder po = findPurchaseOrderByNo(poNo);
        if (po == null) {
            throw new BusinessException("采购订单不存在");
        }
        PurchaseOrderItem item = findPurchaseItem(po.getPurchaseOrderId());
        LocalDateTime now = LocalDateTime.now();
        BigDecimal arrived = (item != null ? item.getReceivedQuantity() : BigDecimal.ZERO).add(BigDecimal.valueOf(qty));
        if (item != null) {
            item.setReceivedQuantity(arrived);
            item.setUpdatedAt(now);
            purchaseOrderItemMapper.updatePurchaseOrderItem(item);
            Inventory inv = findInventoryByMaterial(item.getMaterialId());
            if (inv != null) {
                inv.setQuantityOnHand(inv.getQuantityOnHand().add(BigDecimal.valueOf(qty)));
                inv.setUpdatedAt(now);
                inventoryMapper.updateInventory(inv);
            }
        }
        po.setStatus(arrived.compareTo(item != null ? item.getQuantity() : BigDecimal.valueOf(qty)) >= 0
                ? "ARRIVED" : "PARTIAL_ARRIVED");
        po.setUpdatedAt(now);
        purchaseOrderMapper.updatePurchaseOrder(po);

        MesRuntimeState runtime = mesRuntimeStore.load();
        Map<String, Object> flow = new LinkedHashMap<>();
        flow.put("id", nextRuntimeId("SF", runtime.getStockFlows()));
        flow.put("flowType", "采购入库");
        Material mat = item != null ? materialMapper.getMaterialById(item.getMaterialId()) : null;
        flow.put("materialCode", mat != null ? mat.getMaterialCode() : "");
        flow.put("materialName", mat != null ? mat.getMaterialName() : "");
        flow.put("quantity", qty);
        flow.put("direction", "入");
        flow.put("refNo", poNo);
        flow.put("operator", operator);
        flow.put("createdAt", fmt(now));
        runtime.getStockFlows().add(0, flow);

        appendLog(runtime, "采购管理", "采购到货", poNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    // —— 安灯 ——

    private Map<String, Object> createAlarm(Map<String, Object> p, String operator, String roleKey) {
        LocalDateTime now = LocalDateTime.now();
        User reporter = findUserByUsername(operator);
        String alarmNo = nextNo("AL", andonAlarmMapper.alarmList(), AndonAlarm::getAlarmNo);
        WorkOrder wo = findWorkOrderByNo(str(p, "workOrderId"));

        AndonAlarm alarm = new AndonAlarm();
        alarm.setAlarmNo(alarmNo);
        alarm.setWorkOrderId(wo != null ? wo.getWorkOrderId() : null);
        alarm.setAlarmType(str(p, "type"));
        alarm.setAlarmLevel(alarmLevelDb(str(p, "level", "中")));
        alarm.setAlarmDescription(str(p, "description"));
        alarm.setAlarmStatus("REPORTED");
        alarm.setReportedBy(reporter != null ? reporter.getUserId() : null);
        alarm.setReportedAt(now);
        alarm.setCreatedAt(now);
        alarm.setUpdatedAt(now);
        andonAlarmMapper.insertAlarm(alarm);

        MesRuntimeState runtime = mesRuntimeStore.load();
        Map<String, Object> extra = mesRuntimeStore.getExtra(runtime, "alarm:" + alarmNo);
        extra.put("source", str(p, "source"));
        extra.put("reporterName", str(p, "reporterName", reporter != null ? reporter.getRealName() : ""));
        appendLog(runtime, "安灯报警", "触发安灯", alarmNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return Map.of("id", alarmNo);
    }

    private boolean handleAlarm(Map<String, Object> p, String operator, String roleKey) {
        String alarmNo = str(p, "alarmId");
        String action = str(p, "action");
        AndonAlarm alarm = findAlarmByNo(alarmNo);
        if (alarm == null) {
            throw new BusinessException("报警不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        User user = findUserByUsername(operator);
        MesRuntimeState runtime = mesRuntimeStore.load();
        Map<String, Object> extra = mesRuntimeStore.getExtra(runtime, "alarm:" + alarmNo);

        switch (action) {
            case "receive" -> {
                alarm.setAlarmStatus("RECEIVED");
                alarm.setReceivedBy(user != null ? user.getUserId() : null);
                alarm.setReceivedAt(now);
                extra.put("assigneeName", str(p, "assigneeName"));
            }
            case "processing" -> alarm.setAlarmStatus("PROCESSING");
            case "close" -> {
                alarm.setAlarmStatus("CLOSED");
                alarm.setClosedBy(user != null ? user.getUserId() : null);
                alarm.setClosedAt(now);
            }
            default -> throw new BusinessException("未知报警处理动作");
        }
        alarm.setUpdatedAt(now);
        andonAlarmMapper.updateAlarm(alarm);
        appendLog(runtime, "安灯报警", "close".equals(action) ? "关闭报警" : "处理报警", alarmNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    // —— 设备 ——

    private boolean updateEquipment(Map<String, Object> p, String operator, String roleKey) {
        String eqId = str(p, "eqId");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) p.getOrDefault("payload", p);
        Equipment eq = findEquipmentByCode(eqId);
        if (eq == null) {
            throw new BusinessException("设备不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (payload.containsKey("status")) {
            eq.setStatus(MesStatusMapper.toEquipmentDb(String.valueOf(payload.get("status"))));
        }
        eq.setUpdatedAt(now);
        equipmentMapper.updateEquipment(eq);

        MesRuntimeState runtime = mesRuntimeStore.load();
        Map<String, Object> extra = mesRuntimeStore.getExtra(runtime, "equipment:" + eqId);
        if (payload.containsKey("downtimeHours")) {
            extra.put("downtimeHours", payload.get("downtimeHours"));
        }
        if (payload.containsKey("repairNote")) {
            EquipmentMaintenanceRecord mr = new EquipmentMaintenanceRecord();
            mr.setMaintenanceNo(nextNo("MR", equipmentMaintenanceRecordMapper.maintenanceList(), EquipmentMaintenanceRecord::getMaintenanceNo));
            mr.setEquipmentId(eq.getEquipmentId());
            mr.setMaintenanceContent(String.valueOf(payload.get("repairNote")));
            mr.setDowntimeMinutes((int) (dbl(payload.get("downtimeHours")) * 60));
            User op = findUserByUsername(operator);
            mr.setMaintainerId(op != null ? op.getUserId() : null);
            mr.setCreatedAt(now);
            mr.setUpdatedAt(now);
            equipmentMaintenanceRecordMapper.insertMaintenance(mr);
        }
        appendLog(runtime, "设备管理", "更新设备", eqId, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    // —— 售后 ——

    private Map<String, Object> createAftersale(Map<String, Object> p, String operator, String roleKey) {
        LocalDateTime now = LocalDateTime.now();
        String caseNo = nextNo("AS", afterSalesCaseMapper.afterSalesCaseList(), AfterSalesCase::getCaseNo);
        CustomerOrder order = findOrderByNo(str(p, "orderId"));

        AfterSalesCase c = new AfterSalesCase();
        c.setCaseNo(caseNo);
        c.setOrderId(order != null ? order.getOrderId() : null);
        c.setBatchNo(str(p, "batchNo"));
        c.setCustomerName(str(p, "customerName"));
        c.setProblemDescription(str(p, "feedback"));
        c.setCaseStatus("CREATED");
        c.setOpenedAt(now);
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        afterSalesCaseMapper.insertAfterSalesCase(c);

        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "售后管理", "登记售后", caseNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return Map.of("id", caseNo);
    }

    private boolean processAftersale(Map<String, Object> p, String operator, String roleKey) {
        String caseNo = str(p, "caseId");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) p.getOrDefault("payload", p);
        AfterSalesCase c = afterSalesCaseMapper.getAfterSalesCaseById(caseNo);
        if (c == null) {
            throw new BusinessException("售后案例不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        User handler = findUserByUsername(operator);
        c.setCaseStatus(MesStatusMapper.toAftersaleDb(str(payload, "status", "处理中")));
        c.setServiceUserId(handler != null ? handler.getUserId() : null);
        c.setHandleResult(str(payload, "result"));
        c.setUpdatedAt(now);
        afterSalesCaseMapper.updateAfterSalesCase(c);

        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "售后管理", "处理售后", caseNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    // —— 成本 ——

    private boolean confirmCostSettlement(Map<String, Object> p, String operator, String roleKey) {
        String csNo = str(p, "csId");
        CostSettlement cs = findSettlementByNo(csNo);
        if (cs == null || !"DRAFT".equals(cs.getSettlementStatus())) {
            throw new BusinessException("成本结算状态不允许确认");
        }
        LocalDateTime now = LocalDateTime.now();
        User user = findUserByUsername(operator);
        cs.setSettlementStatus("CONFIRMED");
        cs.setConfirmedBy(user != null ? user.getUserId() : null);
        cs.setConfirmedAt(now);
        cs.setUpdatedAt(now);
        costSettlementMapper.updateSettlement(cs);

        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "成本管理", "确认结算", csNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean exportCostSettlement(Map<String, Object> p, String operator, String roleKey) {
        String csNo = str(p, "csId");
        CostSettlement cs = findSettlementByNo(csNo);
        if (cs == null || !"CONFIRMED".equals(cs.getSettlementStatus())) {
            throw new BusinessException("成本结算状态不允许导出");
        }
        LocalDateTime now = LocalDateTime.now();
        cs.setSettlementStatus("EXPORTED");
        cs.setExportedAt(now);
        cs.setUpdatedAt(now);
        costSettlementMapper.updateSettlement(cs);

        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "成本管理", "导出结算", csNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    // —— 用户 ——

    private boolean saveUser(Map<String, Object> p, String operator, String roleKey) {
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) p.getOrDefault("user", p);
        LocalDateTime now = LocalDateTime.now();
        Long userId = longVal(user.get("id"));
        User entity;
        if (userId != null && userId > 0) {
            entity = userMapper.getUserById(userId);
            if (entity == null) {
                throw new BusinessException("用户不存在");
            }
        } else {
            entity = new User();
            entity.setCreatedAt(now);
            String pwd = str(user, "password");
            if (pwd == null || pwd.isBlank()) {
                throw new BusinessException("请设置用户密码");
            }
            entity.setPasswordHash(BCrypt.hashpw(pwd));
        }
        entity.setUsername(str(user, "username"));
        entity.setRealName(str(user, "realName"));
        entity.setPhone(str(user, "phone"));
        entity.setDepartment(str(user, "department"));
        entity.setEmail(str(user, "email"));
        entity.setStatus("启用".equals(str(user, "status", "启用")) ? 1 : 0);
        entity.setUpdatedAt(now);

        String newPassword = str(user, "password");
        if (userId != null && userId > 0 && newPassword != null && !newPassword.isBlank()) {
            entity.setPasswordHash(BCrypt.hashpw(newPassword));
        }

        String roleKeyVal = str(user, "roleKey");
        Role role = findRoleByKey(roleKeyVal);
        if (role != null) {
            entity.setRoleId(role.getRoleId());
        }

        if (userId != null && userId > 0) {
            userMapper.updateUser(entity);
        } else {
            userMapper.insertUser(entity);
        }

        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "系统管理", userId != null ? "编辑用户" : "新增用户",
                str(user, "username"), operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean toggleUserStatus(Map<String, Object> p, String operator, String roleKey) {
        Long userId = longVal(p.get("userId"));
        User u = userMapper.getUserById(userId);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        u.setStatus(u.getStatus() != null && u.getStatus() == 1 ? 0 : 1);
        u.setUpdatedAt(LocalDateTime.now());
        userMapper.updateUser(u);

        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "系统管理", u.getStatus() == 1 ? "启用用户" : "禁用用户",
                u.getUsername(), operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean resetUserPassword(Map<String, Object> p, String operator, String roleKey) {
        Long userId = longVal(p.get("userId"));
        String pwd = str(p, "password");
        if (pwd == null || pwd.isBlank()) {
            throw new BusinessException("请设置新密码");
        }
        User u = userMapper.getUserById(userId);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        u.setPasswordHash(BCrypt.hashpw(pwd));
        u.setUpdatedAt(LocalDateTime.now());
        userMapper.updateUser(u);

        MesRuntimeState runtime = mesRuntimeStore.load();
        appendLog(runtime, "系统管理", "重置密码", u.getUsername(), operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    // —— helpers ——

    private void addInboundTask(MesRuntimeState runtime, String sourceType, String refNo,
                                WorkOrder wo, QualityInspection qi, int qty) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("id", nextRuntimeId("IN", runtime.getInboundTasks()));
        task.put("sourceType", "让步接收".equals(sourceType) ? "让步接收" : "质检合格");
        task.put("refNo", refNo);
        task.put("productModel", getInspectionProductModel(qi));
        task.put("quantity", qty);
        task.put("status", "待入库");
        task.put("batchNo", qi.getBatchNo());
        task.put("workOrderId", wo != null ? wo.getWorkOrderNo() : "");
        if (wo != null) {
            ProductionPlan plan = productionPlanMapper.getPlanById(wo.getPlanId());
            if (plan != null) {
                CustomerOrder order = customerOrderMapper.getCustomerOrderById(plan.getSourceOrderId());
                if (order != null) {
                    task.put("orderId", order.getOrderNo());
                }
            }
        }
        task.put("createdAt", fmt(LocalDateTime.now()));
        runtime.getInboundTasks().add(0, task);
    }

    private void createDefectRecord(MesRuntimeState runtime, QualityInspection qi, DispatchTask dispatch,
                                    WorkOrder wo, Map<String, Object> payload, int defectQty,
                                    String operator, LocalDateTime now) {
        String defectNo = nextNo("NC", nonconformingProductMapper.nonconformingList(), NonconformingProduct::getNonconformingNo);
        NonconformingProduct defect = new NonconformingProduct();
        defect.setNonconformingNo(defectNo);
        defect.setInspectionId(qi.getInspectionId());
        defect.setWorkOrderId(qi.getWorkOrderId());
        defect.setBatchNo(qi.getBatchNo());
        defect.setDefectType(str(payload, "defectLocation"));
        defect.setDefectDescription(str(payload, "description", str(payload, "remark", "质检不合格")));
        defect.setQuantity(BigDecimal.valueOf(defectQty));
        defect.setSeverity(str(payload, "severity", "轻微"));
        defect.setHandleMethod("严重".equals(str(payload, "severity")) ? "建议报废" : "建议返修");
        defect.setHandleStatus("PENDING");
        defect.setRegisteredAt(now);
        defect.setCreatedAt(now);
        defect.setUpdatedAt(now);
        nonconformingProductMapper.insertNonconforming(defect);

        Map<String, Object> extra = mesRuntimeStore.getExtra(runtime, "defect:" + defectNo);
        extra.put("dispatchId", dispatch != null ? dispatch.getDispatchNo() : "");
        extra.put("productModel", getInspectionProductModel(qi));
        extra.put("defectLocation", str(payload, "defectLocation"));
        extra.put("failedItems", payload.get("failedItems"));
        if (dispatch != null) {
            extra.put("operator", getDispatchExtra(dispatch.getDispatchNo(), "operator"));
            extra.put("operatorName", getDispatchExtra(dispatch.getDispatchNo(), "operatorName"));
        }
    }

    private void syncWorkOrderStatus(WorkOrder wo) {
        List<DispatchTask> related = dispatchTaskMapper.dispatchList().stream()
                .filter(d -> wo.getWorkOrderId().equals(d.getWorkOrderId()))
                .toList();
        if (related.isEmpty()) {
            return;
        }
        boolean allDone = related.stream().allMatch(d -> "COMPLETED".equals(d.getStatus()));
        boolean anyPendingQc = related.stream().anyMatch(d -> "QC_PENDING".equals(d.getStatus()));
        boolean anyActive = related.stream().anyMatch(d -> List.of("ASSIGNED", "ACCEPTED", "PRODUCING").contains(d.getStatus()));
        if (allDone) {
            wo.setStatus("COMPLETED");
        } else if (anyPendingQc) {
            wo.setStatus("QC_PENDING");
        } else if (anyActive) {
            wo.setStatus("PRODUCING");
        }
        wo.setUpdatedAt(LocalDateTime.now());
    }

    private int reportQualifiedQty(Long dispatchId) {
        return workReportMapper.reportList().stream()
                .filter(r -> dispatchId.equals(r.getDispatchId()) && !"REJECTED".equals(r.getReportStatus()))
                .mapToInt(r -> intVal(r.getQualifiedQuantity()))
                .sum();
    }

    private void appendLog(MesRuntimeState runtime, String module, String action, String target,
                         String operator, String roleKey) {
        Map<String, Object> log = new LinkedHashMap<>();
        long seq = runtime.getLogSeq() + 1;
        runtime.setLogSeq(seq);
        log.put("id", seq);
        log.put("module", module);
        log.put("action", action);
        log.put("target", target);
        log.put("operator", operator);
        log.put("roleKey", roleKey);
        log.put("createdAt", fmt(LocalDateTime.now()));
        runtime.getOperationLogs().add(0, log);
        if (runtime.getOperationLogs().size() > 200) {
            runtime.setOperationLogs(new ArrayList<>(runtime.getOperationLogs().subList(0, 200)));
        }
    }

    private <T> String nextNo(String prefix, List<T> list, java.util.function.Function<T, String> getter) {
        String ym = LocalDate.now().format(YM_FMT);
        String fullPrefix = prefix + ym;
        int max = list.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .filter(no -> no.startsWith(fullPrefix))
                .mapToInt(no -> {
                    try {
                        return Integer.parseInt(no.substring(fullPrefix.length()));
                    } catch (Exception e) {
                        return 0;
                    }
                }).max().orElse(0);
        return fullPrefix + String.format("%03d", max + 1);
    }

    private String nextRuntimeId(String prefix, List<Map<String, Object>> list) {
        int max = list.stream()
                .map(m -> String.valueOf(m.get("id")))
                .filter(id -> id.startsWith(prefix))
                .mapToInt(id -> {
                    try {
                        return Integer.parseInt(id.replaceAll("\\D+", ""));
                    } catch (Exception e) {
                        return 0;
                    }
                }).max().orElse(0);
        return prefix + String.format("%03d", max + 1);
    }

    // —— 删除归档（级联清理关联记录）——

    private boolean deleteOrder(Map<String, Object> p, String operator, String roleKey) {
        String orderNo = str(p, "orderId");
        CustomerOrder order = findOrderByNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        Long orderId = order.getOrderId();
        MesRuntimeState runtime = mesRuntimeStore.load();

        new ArrayList<>(productionPlanMapper.planList()).stream()
                .filter(pl -> orderId.equals(pl.getSourceOrderId()))
                .forEach(pl -> removePlanTree(pl.getPlanId(), pl.getPlanNo(), runtime));

        removeAfterSalesByOrderId(orderId);

        new ArrayList<>(deliveryOrderMapper.deliveryList()).stream()
                .filter(d -> orderId.equals(d.getOrderId()))
                .forEach(d -> removeDeliveryTree(d.getDeliveryId()));

        new ArrayList<>(costSettlementMapper.settlementList()).stream()
                .filter(c -> orderId.equals(c.getOrderId()))
                .forEach(c -> costSettlementMapper.deleteSettlement(c.getSettlementId()));

        new ArrayList<>(customerOrderItemMapper.orderItemList()).stream()
                .filter(i -> orderId.equals(i.getOrderId()))
                .forEach(i -> customerOrderItemMapper.deleteOrderItem(i.getOrderItemId()));

        runtime.getInboundTasks().removeIf(t -> orderNo.equals(String.valueOf(t.get("orderId"))));
        customerOrderMapper.deleteCustomerOrder(orderId);
        appendLog(runtime, "订单管理", "删除订单", orderNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean deletePlanRecord(Map<String, Object> p, String operator, String roleKey) {
        String planNo = str(p, "planId");
        ProductionPlan plan = findPlanByNo(planNo);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }
        MesRuntimeState runtime = mesRuntimeStore.load();
        removePlanTree(plan.getPlanId(), planNo, runtime);
        appendLog(runtime, "计划管理", "删除生产计划", planNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean deleteWorkOrderRecord(Map<String, Object> p, String operator, String roleKey) {
        String woNo = str(p, "workOrderId");
        WorkOrder wo = findWorkOrderByNo(woNo);
        if (wo == null) {
            throw new BusinessException("工单不存在");
        }
        MesRuntimeState runtime = mesRuntimeStore.load();
        removeWorkOrderTree(wo.getWorkOrderId(), woNo, runtime);
        appendLog(runtime, "生产管理", "删除生产工单", woNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean deleteDispatchRecord(Map<String, Object> p, String operator, String roleKey) {
        String dispatchNo = str(p, "dispatchId");
        DispatchTask d = findDispatchByNo(dispatchNo);
        if (d == null) {
            throw new BusinessException("派工记录不存在");
        }
        MesRuntimeState runtime = mesRuntimeStore.load();
        removeDispatchTree(d.getDispatchId(), dispatchNo, runtime);
        appendLog(runtime, "生产管理", "删除派工记录", dispatchNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean deleteReportRecord(Map<String, Object> p, String operator, String roleKey) {
        String reportNo = str(p, "reportId");
        WorkReport report = findReportByNo(reportNo);
        if (report == null) {
            throw new BusinessException("报工记录不存在");
        }
        MesRuntimeState runtime = mesRuntimeStore.load();
        removeReportTree(report.getReportId(), reportNo, runtime);
        appendLog(runtime, "现场作业", "删除报工记录", reportNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean deleteInspectionRecord(Map<String, Object> p, String operator, String roleKey) {
        String qcNo = str(p, "qcId");
        QualityInspection insp = findInspectionByNo(qcNo);
        if (insp == null) {
            throw new BusinessException("质检记录不存在");
        }
        MesRuntimeState runtime = mesRuntimeStore.load();
        removeInspectionTree(insp.getInspectionId(), qcNo, runtime);
        appendLog(runtime, "质量管理", "删除质检记录", qcNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean deleteDefectRecord(Map<String, Object> p, String operator, String roleKey) {
        String defectNo = str(p, "defectId");
        NonconformingProduct defect = findDefectByNo(defectNo);
        if (defect == null) {
            throw new BusinessException("不合格品记录不存在");
        }
        MesRuntimeState runtime = mesRuntimeStore.load();
        nonconformingProductMapper.deleteNonconforming(defect.getNonconformingId());
        appendLog(runtime, "质量管理", "删除不合格品记录", defectNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean deletePurchaseOrderRecord(Map<String, Object> p, String operator, String roleKey) {
        String poNo = str(p, "purchaseOrderId");
        PurchaseOrder po = findPurchaseOrderByNo(poNo);
        if (po == null) {
            throw new BusinessException("采购订单不存在");
        }
        Long poId = po.getPurchaseOrderId();
        MesRuntimeState runtime = mesRuntimeStore.load();
        new ArrayList<>(purchaseOrderItemMapper.purchaseOrderItemList()).stream()
                .filter(i -> poId.equals(i.getPurchaseOrderId()))
                .forEach(i -> purchaseOrderItemMapper.deletePurchaseOrderItem(i.getPurchaseOrderItemId()));
        new ArrayList<>(inventoryTransactionMapper.transactionList()).stream()
                .filter(t -> poId.equals(t.getRelatedPurchaseOrderId()))
                .forEach(t -> inventoryTransactionMapper.deleteTransaction(t.getTransactionId()));
        purchaseOrderMapper.deletePurchaseOrder(poId);
        appendLog(runtime, "采购管理", "删除采购订单", poNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean deleteDeliveryRecord(Map<String, Object> p, String operator, String roleKey) {
        String dlvNo = str(p, "dlvId");
        DeliveryOrder d = findDeliveryByNo(dlvNo);
        if (d == null) {
            throw new BusinessException("发货单不存在");
        }
        MesRuntimeState runtime = mesRuntimeStore.load();
        removeDeliveryTree(d.getDeliveryId());
        appendLog(runtime, "发货管理", "删除发货单", dlvNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean deleteAlarmRecord(Map<String, Object> p, String operator, String roleKey) {
        String alarmNo = str(p, "alarmId");
        AndonAlarm alarm = findAlarmByNo(alarmNo);
        if (alarm == null) {
            throw new BusinessException("报警记录不存在");
        }
        MesRuntimeState runtime = mesRuntimeStore.load();
        removeAlarmTree(alarm.getAlarmId(), alarmNo, runtime);
        appendLog(runtime, "安灯报警", "删除报警记录", alarmNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean deleteAftersaleRecord(Map<String, Object> p, String operator, String roleKey) {
        String caseNo = str(p, "caseId");
        AfterSalesCase c = afterSalesCaseMapper.getAfterSalesCaseById(caseNo);
        if (c == null) {
            throw new BusinessException("售后案例不存在");
        }
        MesRuntimeState runtime = mesRuntimeStore.load();
        afterSalesCaseMapper.deleteAfterSalesCase(caseNo);
        appendLog(runtime, "售后管理", "删除售后案例", caseNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean deleteCostSettlementRecord(Map<String, Object> p, String operator, String roleKey) {
        String csNo = str(p, "settlementId");
        CostSettlement cs = findSettlementByNo(csNo);
        if (cs == null) {
            throw new BusinessException("成本结算不存在");
        }
        MesRuntimeState runtime = mesRuntimeStore.load();
        costSettlementMapper.deleteSettlement(cs.getSettlementId());
        appendLog(runtime, "成本管理", "删除成本结算", csNo, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean deleteInboundTask(Map<String, Object> p, String operator, String roleKey) {
        String taskId = str(p, "taskId");
        MesRuntimeState runtime = mesRuntimeStore.load();
        boolean removed = runtime.getInboundTasks().removeIf(t -> taskId.equals(String.valueOf(t.get("id"))));
        if (!removed) {
            throw new BusinessException("入库任务不存在");
        }
        appendLog(runtime, "仓储管理", "删除入库任务", taskId, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private boolean deleteIssueTask(Map<String, Object> p, String operator, String roleKey) {
        String taskId = str(p, "taskId");
        MesRuntimeState runtime = mesRuntimeStore.load();
        boolean removed = runtime.getIssueTasks().removeIf(t -> taskId.equals(String.valueOf(t.get("id"))));
        if (!removed) {
            throw new BusinessException("领料任务不存在");
        }
        appendLog(runtime, "仓储管理", "删除领料任务", taskId, operator, roleKey);
        mesRuntimeStore.save(runtime);
        return true;
    }

    private void removePlanTree(Long planId, String planNo, MesRuntimeState runtime) {
        new ArrayList<>(workOrderMapper.workOrderList()).stream()
                .filter(w -> planId.equals(w.getPlanId()))
                .forEach(w -> removeWorkOrderTree(w.getWorkOrderId(), w.getWorkOrderNo(), runtime));
        new ArrayList<>(productionPlanItemMapper.planItemList()).stream()
                .filter(i -> planId.equals(i.getPlanId()))
                .forEach(i -> productionPlanItemMapper.deletePlanItem(i.getPlanItemId()));
        runtime.getExtras().remove("plan:" + planNo);
        productionPlanMapper.deletePlan(planId);
    }

    private void removeWorkOrderTree(Long woId, String woNo, MesRuntimeState runtime) {
        new ArrayList<>(dispatchTaskMapper.dispatchList()).stream()
                .filter(d -> woId.equals(d.getWorkOrderId()))
                .forEach(d -> removeDispatchTree(d.getDispatchId(), d.getDispatchNo(), runtime));
        removeOrphanReportsByWorkOrder(woId, runtime);
        removeOrphanInspectionsByWorkOrder(woId, runtime);
        removeOrphanDefectsByWorkOrder(woId);
        new ArrayList<>(andonAlarmMapper.alarmList()).stream()
                .filter(a -> woId.equals(a.getWorkOrderId()))
                .forEach(a -> removeAlarmTree(a.getAlarmId(), a.getAlarmNo(), runtime));
        new ArrayList<>(deliveryOrderMapper.deliveryList()).stream()
                .filter(d -> woId.equals(d.getWorkOrderId()))
                .forEach(d -> removeDeliveryTree(d.getDeliveryId()));
        new ArrayList<>(costSettlementMapper.settlementList()).stream()
                .filter(c -> woId.equals(c.getWorkOrderId()))
                .forEach(c -> costSettlementMapper.deleteSettlement(c.getSettlementId()));
        new ArrayList<>(inventoryTransactionMapper.transactionList()).stream()
                .filter(t -> woId.equals(t.getRelatedWorkOrderId()))
                .forEach(t -> inventoryTransactionMapper.deleteTransaction(t.getTransactionId()));
        runtime.getIssueTasks().removeIf(t -> woNo.equals(String.valueOf(t.get("workOrderId"))));
        runtime.getInboundTasks().removeIf(t -> woNo.equals(String.valueOf(t.get("workOrderId"))));
        removeWorkProgressByWorkOrderId(woId);
        workOrderMapper.deleteWorkOrder(woId);
    }

    private void removeDispatchTree(Long dispatchId, String dispatchNo, MesRuntimeState runtime) {
        removeWorkProgressByDispatchId(dispatchId);
        new ArrayList<>(andonAlarmMapper.alarmList()).stream()
                .filter(a -> dispatchId.equals(a.getDispatchId()))
                .forEach(a -> removeAlarmTree(a.getAlarmId(), a.getAlarmNo(), runtime));
        new ArrayList<>(workReportMapper.reportList()).stream()
                .filter(r -> dispatchId.equals(r.getDispatchId()))
                .forEach(r -> removeReportTree(r.getReportId(), r.getReportNo(), runtime));
        dispatchTaskMapper.deleteDispatch(dispatchId);
    }

    private void removeReportTree(Long reportId, String reportNo, MesRuntimeState runtime) {
        new ArrayList<>(qualityInspectionMapper.inspectionList()).stream()
                .filter(i -> reportId.equals(i.getWorkReportId()))
                .forEach(i -> removeInspectionTree(i.getInspectionId(), i.getInspectionNo(), runtime));
        new ArrayList<>(nonconformingProductMapper.nonconformingList()).stream()
                .filter(d -> reportId.equals(d.getWorkReportId()))
                .forEach(d -> nonconformingProductMapper.deleteNonconforming(d.getNonconformingId()));
        workReportMapper.deleteReport(reportId);
    }

    private void removeInspectionTree(Long inspectionId, String inspectionNo, MesRuntimeState runtime) {
        new ArrayList<>(nonconformingProductMapper.nonconformingList()).stream()
                .filter(d -> inspectionId.equals(d.getInspectionId()))
                .forEach(d -> nonconformingProductMapper.deleteNonconforming(d.getNonconformingId()));
        Map<String, Object> inspExtra = runtime.getExtras().get("inspection:" + inspectionNo);
        if (inspExtra != null) {
            String reportId = String.valueOf(inspExtra.getOrDefault("qualityReportId", ""));
            if (!reportId.isBlank() && !"null".equals(reportId)) {
                runtime.getExtras().remove("qualityReport:" + reportId);
            }
        }
        runtime.getExtras().remove("inspection:" + inspectionNo);
        qualityInspectionMapper.deleteInspection(inspectionId);
    }

    private void removeOrphanReportsByWorkOrder(Long woId, MesRuntimeState runtime) {
        new ArrayList<>(workReportMapper.reportList()).stream()
                .filter(r -> woId.equals(r.getWorkOrderId()))
                .forEach(r -> removeReportTree(r.getReportId(), r.getReportNo(), runtime));
    }

    private void removeOrphanInspectionsByWorkOrder(Long woId, MesRuntimeState runtime) {
        new ArrayList<>(qualityInspectionMapper.inspectionList()).stream()
                .filter(i -> woId.equals(i.getWorkOrderId()))
                .forEach(i -> removeInspectionTree(i.getInspectionId(), i.getInspectionNo(), runtime));
    }

    private void removeOrphanDefectsByWorkOrder(Long woId) {
        new ArrayList<>(nonconformingProductMapper.nonconformingList()).stream()
                .filter(d -> woId.equals(d.getWorkOrderId()))
                .forEach(d -> nonconformingProductMapper.deleteNonconforming(d.getNonconformingId()));
    }

    private void removeAlarmTree(Long alarmId, String alarmNo, MesRuntimeState runtime) {
        new ArrayList<>(equipmentMaintenanceRecordMapper.maintenanceList()).stream()
                .filter(m -> alarmId.equals(m.getAlarmId()))
                .forEach(m -> equipmentMaintenanceRecordMapper.deleteMaintenance(m.getMaintenanceId()));
        andonAlarmMapper.deleteAlarm(alarmId);
    }

    private void removeAfterSalesByOrderId(Long orderId) {
        new ArrayList<>(afterSalesCaseMapper.afterSalesCaseList()).stream()
                .filter(c -> orderId.equals(c.getOrderId()))
                .forEach(c -> afterSalesCaseMapper.deleteAfterSalesCase(c.getCaseNo()));
    }

    private void removeAfterSalesByDeliveryId(Long deliveryId) {
        if (deliveryId == null) {
            return;
        }
        new ArrayList<>(afterSalesCaseMapper.afterSalesCaseList()).stream()
                .filter(c -> deliveryId.equals(c.getDeliveryId()))
                .forEach(c -> afterSalesCaseMapper.deleteAfterSalesCase(c.getCaseNo()));
    }

    private void removeDeliveryTree(Long deliveryId) {
        if (deliveryId == null) {
            return;
        }
        removeAfterSalesByDeliveryId(deliveryId);
        deliveryOrderMapper.deleteDelivery(deliveryId);
    }

    private void removeWorkProgressByDispatchId(Long dispatchId) {
        if (dispatchId == null) {
            return;
        }
        new ArrayList<>(workProgressMapper.progressList()).stream()
                .filter(p -> dispatchId.equals(p.getDispatchId()))
                .forEach(p -> workProgressMapper.deleteProgress(p.getProgressId()));
    }

    private void removeWorkProgressByWorkOrderId(Long workOrderId) {
        new ArrayList<>(workProgressMapper.progressList()).stream()
                .filter(p -> workOrderId.equals(p.getWorkOrderId()))
                .forEach(p -> workProgressMapper.deleteProgress(p.getProgressId()));
    }

    private CustomerOrder findOrderByNo(String orderNo) {
        return customerOrderMapper.customerOrderList().stream()
                .filter(o -> orderNo.equals(o.getOrderNo())).findFirst().orElse(null);
    }

    private ProductionPlan findPlanByNo(String planNo) {
        return productionPlanMapper.planList().stream()
                .filter(p -> planNo.equals(p.getPlanNo())).findFirst().orElse(null);
    }

    private WorkOrder findWorkOrderByNo(String woNo) {
        return workOrderMapper.workOrderList().stream()
                .filter(w -> woNo.equals(w.getWorkOrderNo())).findFirst().orElse(null);
    }

    private DispatchTask findDispatchByNo(String dispatchNo) {
        return dispatchTaskMapper.dispatchList().stream()
                .filter(d -> dispatchNo.equals(d.getDispatchNo())).findFirst().orElse(null);
    }

    private WorkReport findReportByNo(String reportNo) {
        return workReportMapper.reportList().stream()
                .filter(r -> reportNo.equals(r.getReportNo())).findFirst().orElse(null);
    }

    private QualityInspection findInspectionByNo(String inspectionNo) {
        return qualityInspectionMapper.inspectionList().stream()
                .filter(i -> inspectionNo.equals(i.getInspectionNo())).findFirst().orElse(null);
    }

    private NonconformingProduct findDefectByNo(String defectNo) {
        return nonconformingProductMapper.nonconformingList().stream()
                .filter(d -> defectNo.equals(d.getNonconformingNo())).findFirst().orElse(null);
    }

    private PurchaseOrder findPurchaseOrderByNo(String poNo) {
        return purchaseOrderMapper.purchaseOrderList().stream()
                .filter(p -> poNo.equals(p.getPurchaseOrderNo())).findFirst().orElse(null);
    }

    private DeliveryOrder findDeliveryByNo(String dlvNo) {
        return deliveryOrderMapper.deliveryList().stream()
                .filter(d -> dlvNo.equals(d.getDeliveryNo())).findFirst().orElse(null);
    }

    private AndonAlarm findAlarmByNo(String alarmNo) {
        return andonAlarmMapper.alarmList().stream()
                .filter(a -> alarmNo.equals(a.getAlarmNo())).findFirst().orElse(null);
    }

    private CostSettlement findSettlementByNo(String csNo) {
        return costSettlementMapper.settlementList().stream()
                .filter(c -> csNo.equals(c.getSettlementNo())).findFirst().orElse(null);
    }

    private Equipment findEquipmentByCode(String code) {
        return equipmentMapper.equipmentList().stream()
                .filter(e -> code.equals(e.getEquipmentCode())).findFirst().orElse(null);
    }

    private Equipment findEquipmentByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return equipmentMapper.equipmentList().stream()
                .filter(e -> name.equals(e.getEquipmentName()) || e.getEquipmentName().contains(name))
                .findFirst().orElse(null);
    }

    private ProcessStep findStepByName(String name) {
        if (name == null || name.isBlank()) {
            return processStepMapper.stepList().isEmpty() ? null : processStepMapper.stepList().get(0);
        }
        return processStepMapper.stepList().stream()
                .filter(s -> name.equals(s.getStepName())).findFirst()
                .orElse(processStepMapper.stepList().isEmpty() ? null : processStepMapper.stepList().get(0));
    }

    private Material findMaterialByCode(String code) {
        return materialMapper.materialList().stream()
                .filter(m -> code.equals(m.getMaterialCode())).findFirst().orElse(null);
    }

    private String normalizeIssueMaterialCode(String code) {
        if ("BL-MODULE".equals(code)) {
            return "MAT-002";
        }
        return code;
    }

    private void createIssueTasksFromBom(WorkOrder wo, MesRuntimeState runtime, String woNo, LocalDateTime now) {
        boolean exists = runtime.getIssueTasks().stream().anyMatch(t -> woNo.equals(t.get("workOrderId")));
        if (exists) {
            return;
        }
        int planQty = intVal(wo.getPlannedQuantity());
        Long materialId = wo.getMaterialId();
        List<Bom> bomLines = bomMapper.bomList().stream()
                .filter(b -> materialId != null && materialId.equals(b.getParentMaterialId())
                        && b.getStatus() != null && b.getStatus() == 1)
                .toList();
        if (bomLines.isEmpty()) {
            addIssueTask(runtime, woNo, "MAT-002", "背光模组", planQty, now);
            return;
        }
        for (Bom bom : bomLines) {
            Material child = materialMapper.materialList().stream()
                    .filter(m -> bom.getChildMaterialId().equals(m.getMaterialId()))
                    .findFirst().orElse(null);
            if (child == null) {
                continue;
            }
            int required = (int) Math.ceil(planQty * bomQtyWithLoss(bom));
            addIssueTask(runtime, woNo, child.getMaterialCode(), child.getMaterialName(), required, now);
        }
    }

    private void addIssueTask(MesRuntimeState runtime, String woNo, String code, String name,
                              int required, LocalDateTime now) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("id", nextRuntimeId("IS", runtime.getIssueTasks()));
        task.put("workOrderId", woNo);
        task.put("materialCode", code);
        task.put("materialName", name);
        task.put("requiredQty", required);
        task.put("issuedQty", 0);
        task.put("status", "待领料");
        task.put("createdAt", fmt(now));
        runtime.getIssueTasks().add(0, task);
    }

    private double bomQtyWithLoss(Bom bom) {
        double qty = bom.getQuantity() != null ? bom.getQuantity().doubleValue() : 1.0;
        double loss = bom.getLossRate() != null ? bom.getLossRate().doubleValue() : 0.0;
        return qty * (1.0 + loss);
    }

    private void recordInventoryTransaction(Inventory inv, Material mat, String type, BigDecimal qty,
                                            Long workOrderId, User user, LocalDateTime now, String remark) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setTransactionNo(nextNo("IT", inventoryTransactionMapper.transactionList(),
                InventoryTransaction::getTransactionNo));
        tx.setInventoryId(inv.getInventoryId());
        tx.setMaterialId(mat.getMaterialId());
        tx.setTransactionType(type);
        tx.setQuantity(qty);
        tx.setWarehouseCode(inv.getWarehouseCode());
        tx.setLocationCode(inv.getLocationCode());
        tx.setBatchNo(inv.getBatchNo());
        tx.setRelatedWorkOrderId(workOrderId);
        tx.setHandledBy(user != null ? user.getUserId() : null);
        tx.setHandledAt(now);
        tx.setRemark(remark);
        tx.setCreatedAt(now);
        inventoryTransactionMapper.insertTransaction(tx);
    }

    private Material resolveFinishedMaterialForTask(Map<String, Object> task, WorkOrder wo) {
        if (wo != null && wo.getMaterialId() != null) {
            Material mat = materialMapper.materialList().stream()
                    .filter(m -> wo.getMaterialId().equals(m.getMaterialId()))
                    .findFirst().orElse(null);
            if (mat != null) {
                return mat;
            }
        }
        String productModel = String.valueOf(task.getOrDefault("productModel", ""));
        Material byName = materialMapper.materialList().stream()
                .filter(m -> productModel.equals(m.getMaterialName()) || productModel.equals(m.getMaterialCode()))
                .findFirst().orElse(null);
        if (byName != null) {
            return byName;
        }
        return findMaterialContaining("成品");
    }

    private Long resolveDeliveryMaterialId(Material mat, WorkOrder wo, Long orderId) {
        if (mat != null) {
            return mat.getMaterialId();
        }
        if (wo != null && wo.getMaterialId() != null) {
            return wo.getMaterialId();
        }
        CustomerOrderItem item = firstOrderItem(orderId);
        if (item != null && item.getMaterialId() != null) {
            return item.getMaterialId();
        }
        return 7L;
    }

    private void syncPlanProgress(WorkOrder wo, int qualifiedQty) {
        if (wo == null || wo.getPlanItemId() == null || qualifiedQty <= 0) {
            return;
        }
        ProductionPlanItem item = productionPlanItemMapper.getPlanItemById(wo.getPlanItemId());
        if (item == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        item.setCompletedQuantity(item.getCompletedQuantity().add(BigDecimal.valueOf(qualifiedQty)));
        item.setUpdatedAt(now);
        if (item.getCompletedQuantity().compareTo(item.getPlannedQuantity()) >= 0) {
            item.setItemStatus("COMPLETED");
        } else if (!"COMPLETED".equals(item.getItemStatus())) {
            item.setItemStatus("RUNNING");
        }
        productionPlanItemMapper.updatePlanItem(item);

        ProductionPlan plan = productionPlanMapper.getPlanById(wo.getPlanId());
        if (plan == null) {
            return;
        }
        boolean allDone = productionPlanItemMapper.planItemList().stream()
                .filter(i -> plan.getPlanId().equals(i.getPlanId()))
                .allMatch(i -> "COMPLETED".equals(i.getItemStatus()));
        if (allDone) {
            plan.setPlanStatus("COMPLETED");
            plan.setUpdatedAt(now);
            productionPlanMapper.updatePlan(plan);
        }
    }

    private Material resolveFinishedMaterial(String productModel) {
        if (productModel == null || productModel.isBlank()) {
            return materialMapper.materialList().stream()
                    .filter(m -> "FINISHED".equals(m.getMaterialType()))
                    .findFirst().orElse(null);
        }
        Map<String, String> modelToCode = Map.of(
                "DM-27-LCD-FHD", "PRD-001",
                "DM-24-LCD-FHD", "PRD-001",
                "DM-32-OLED-4K", "PRD-002",
                "DM-34-OLED-UWQHD", "PRD-002"
        );
        String code = modelToCode.get(productModel);
        if (code != null) {
            Material mapped = findMaterialByCode(code);
            if (mapped != null) {
                return mapped;
            }
        }
        String upper = productModel.toUpperCase();
        return materialMapper.materialList().stream()
                .filter(m -> "FINISHED".equals(m.getMaterialType()))
                .filter(m -> {
                    String name = m.getMaterialName() != null ? m.getMaterialName() : "";
                    String spec = m.getSpecification() != null ? m.getSpecification() : "";
                    if (upper.contains("OLED")) {
                        return name.contains("OLED") || name.contains("电竞") || spec.contains("OLED");
                    }
                    if (upper.contains("4K")) {
                        return name.contains("4K") || spec.contains("4K");
                    }
                    return name.contains("商用") || name.contains("LCD") || spec.contains("1080");
                })
                .findFirst()
                .orElse(materialMapper.materialList().stream()
                        .filter(m -> "FINISHED".equals(m.getMaterialType()))
                        .findFirst().orElse(null));
    }

    private String resolveCustomerName(Object customerIdObj) {
        if (customerIdObj == null) {
            return "";
        }
        int customerId = intVal(customerIdObj);
        return CustomerCatalog.resolveName(
                customerId,
                customerOrderMapper.customerOrderList(),
                deliveryOrderMapper.deliveryList(),
                afterSalesCaseMapper.afterSalesCaseList());
    }

    private Material findMaterialContaining(String keyword) {
        return materialMapper.materialList().stream()
                .filter(m -> m.getMaterialName() != null && m.getMaterialName().contains(keyword))
                .findFirst().orElse(null);
    }

    private Inventory findInventoryByMaterial(Long materialId) {
        return inventoryMapper.inventoryList().stream()
                .filter(i -> materialId.equals(i.getMaterialId())).findFirst().orElse(null);
    }

    private PurchaseOrderItem findPurchaseItem(Long poId) {
        return purchaseOrderItemMapper.purchaseOrderItemList().stream()
                .filter(i -> poId.equals(i.getPurchaseOrderId())).findFirst().orElse(null);
    }

    private CustomerOrderItem firstOrderItem(Long orderId) {
        return customerOrderItemMapper.orderItemList().stream()
                .filter(i -> orderId.equals(i.getOrderId())).findFirst().orElse(null);
    }

    private String orderProductModel(Long orderId) {
        CustomerOrder order = customerOrderMapper.getCustomerOrderById(orderId);
        if (order == null) {
            return "";
        }
        MesRuntimeState runtime = mesRuntimeStore.load();
        Map<String, Object> extra = runtime.getExtras().getOrDefault("order:" + order.getOrderNo(), Map.of());
        Object model = extra.get("productModel");
        if (model != null && !String.valueOf(model).isBlank()) {
            return String.valueOf(model);
        }
        CustomerOrderItem item = firstOrderItem(orderId);
        return item != null && item.getProductName() != null ? item.getProductName() : "";
    }

    private Long resolveRouteId(Long materialId) {
        if (materialId == null) {
            return processRouteMapper.routeList().stream()
                    .map(ProcessRoute::getRouteId).findFirst().orElse(1L);
        }
        return processRouteMapper.routeList().stream()
                .filter(r -> materialId.equals(r.getMaterialId()))
                .map(ProcessRoute::getRouteId)
                .findFirst()
                .orElse(processRouteMapper.routeList().stream()
                        .map(ProcessRoute::getRouteId).findFirst().orElse(1L));
    }

    private ProductionPlanItem findPlanItem(Long planId, Long materialId) {
        return productionPlanItemMapper.planItemList().stream()
                .filter(i -> planId.equals(i.getPlanId()))
                .filter(i -> materialId == null || materialId.equals(i.getMaterialId()))
                .findFirst()
                .orElse(null);
    }

    private Long findLatestReportId(Long dispatchId) {
        return workReportMapper.reportList().stream()
                .filter(r -> dispatchId.equals(r.getDispatchId()))
                .filter(r -> !"REJECTED".equals(r.getReportStatus()))
                .max(Comparator.comparing(WorkReport::getReportId, Comparator.nullsLast(Long::compareTo)))
                .map(WorkReport::getReportId)
                .orElse(null);
    }

    private User findUserByUsername(String username) {
        if (username == null) {
            return null;
        }
        return userMapper.getUserByUsername(username);
    }

    private String inspectionTypeToDb(String type) {
        if (type == null || type.isBlank()) {
            return "FINAL";
        }
        return switch (type) {
            case "终检", "FINAL" -> "FINAL";
            case "复检", "RECHECK" -> "RECHECK";
            case "来料检", "INCOMING" -> "INCOMING";
            case "过程检", "PROCESS" -> "PROCESS";
            default -> type;
        };
    }

    private User findDefaultQcUser() {
        return userMapper.userList().stream()
                .filter(u -> u.getStatus() != null && u.getStatus() == 1)
                .filter(u -> {
                    Role role = roleMapper.getRoleById(u.getRoleId());
                    return role != null && "QC".equalsIgnoreCase(role.getRoleCode());
                })
                .findFirst()
                .orElse(null);
    }

    private User resolveOperator(Map<String, Object> p) {
        String op = str(p, "operator");
        if (!op.isBlank()) {
            User u = findUserByUsername(op);
            if (u != null) {
                return u;
            }
        }
        return userMapper.userList().stream()
                .filter(u -> u.getStatus() != null && u.getStatus() == 1)
                .filter(u -> {
                    Role role = roleMapper.getRoleById(u.getRoleId());
                    return role != null && "OPERATOR".equalsIgnoreCase(role.getRoleCode());
                })
                .findFirst()
                .orElse(null);
    }

    private Role findRoleByKey(String roleKey) {
        return roleMapper.roleList().stream()
                .filter(r -> roleKey.equals(MesStatusMapper.toRoleKey(r.getRoleCode())))
                .findFirst().orElse(null);
    }

    private Map<String, Object> findRuntimeTask(List<Map<String, Object>> tasks, String id) {
        return tasks.stream().filter(t -> id.equals(t.get("id"))).findFirst().orElse(null);
    }

    private Object getDispatchExtra(String dispatchNo, String key) {
        return mesRuntimeStore.load().getExtras()
                .getOrDefault("dispatch:" + dispatchNo, Map.of()).get(key);
    }

    private String getInspectionProductModel(QualityInspection qi) {
        Map<String, Object> extra = mesRuntimeStore.load().getExtras()
                .getOrDefault("inspection:" + qi.getInspectionNo(), Map.of());
        return String.valueOf(extra.getOrDefault("productModel", ""));
    }

    private String alarmLevelDb(String cn) {
        return switch (cn) {
            case "高" -> "HIGH";
            case "低" -> "LOW";
            default -> "MEDIUM";
        };
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? String.valueOf(v) : "";
    }

    private String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null && !String.valueOf(v).isBlank() ? String.valueOf(v) : def;
    }

    private boolean bool(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Boolean b) {
            return b;
        }
        return "true".equalsIgnoreCase(String.valueOf(v));
    }

    private int intVal(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return 0;
        }
    }

    private long longVal(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return 0;
        }
    }

    private double dbl(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return 0;
        }
    }

    private BigDecimal decimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private String fmt(LocalDateTime dt) {
        return dt.format(DT_FMT);
    }

    private String truncateRemark(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen - 3) + "...";
    }
}
