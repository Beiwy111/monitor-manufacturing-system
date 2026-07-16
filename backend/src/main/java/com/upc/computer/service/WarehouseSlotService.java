package com.upc.computer.service;

import com.upc.computer.common.BusinessException;
import com.upc.computer.entity.Inventory;
import com.upc.computer.entity.InventoryTransaction;
import com.upc.computer.entity.Material;
import com.upc.computer.entity.PurchaseOrder;
import com.upc.computer.entity.PurchaseOrderItem;
import com.upc.computer.mapper.InventoryMapper;
import com.upc.computer.mapper.InventoryTransactionMapper;
import com.upc.computer.mapper.MaterialMapper;
import com.upc.computer.mapper.PurchaseOrderItemMapper;
import com.upc.computer.mapper.PurchaseOrderMapper;
import com.upc.computer.mapper.WarehouseLocationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class WarehouseSlotService {

    private static final String PO_RECEIVED = "RECEIVED";
    private static final String PO_CANCELLED = "CANCELLED";

    @Autowired
    private WarehouseLocationMapper warehouseLocationMapper;
    @Autowired
    private InventoryMapper inventoryMapper;
    @Autowired
    private InventoryTransactionMapper inventoryTransactionMapper;
    @Autowired
    private MaterialMapper materialMapper;
    @Autowired
    private PurchaseOrderMapper purchaseOrderMapper;
    @Autowired
    private PurchaseOrderItemMapper purchaseOrderItemMapper;

    public List<Map<String, Object>> listAvailableSlots(String category, String zoneCode) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try {
            if (zoneCode != null && !zoneCode.isBlank()) {
                rows.addAll(safeAvailable(zoneCode));
            } else if ("FINISHED".equalsIgnoreCase(category)) {
                rows.addAll(safeAvailable("FG-WH"));
            } else {
                rows.addAll(safeAvailable("RM-WH"));
                rows.addAll(safeAvailable("AM-WH"));
            }
        } catch (Exception e) {
            if (!isMissingSchema(e)) {
                throw e;
            }
        }
        return rows.stream().map(this::enrichSlotOption).toList();
    }

    public Map<String, Object> requireAvailableSlot(String slotCode) {
        if (slotCode == null || slotCode.isBlank()) {
            throw new BusinessException("请选择存放库位");
        }
        Map<String, Object> slot = safeSlotDetail(slotCode);
        if (slot == null) {
            throw new BusinessException("库位不存在: " + slotCode);
        }
        if (intVal(slot.get("occupied")) > 0) {
            throw new BusinessException("储位已被占用: " + slotCode);
        }
        return slot;
    }

    @Transactional
    public Inventory inboundToSlot(String slotCode, Long materialId, BigDecimal qty, String batchNo,
                                   String transactionType, String remark, Long handlerId,
                                   Long purchaseOrderId, Long workOrderId) {
        Map<String, Object> slot = requireAvailableSlot(slotCode);
        Material mat = materialMapper.getMaterialById(materialId);
        if (mat == null) {
            throw new BusinessException("物料不存在");
        }
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("入库数量必须大于 0");
        }

        String warehouseCode = text(slot.get("warehouseCode"));
        String warehouseName = text(slot.get("zoneName"));
        LocalDateTime now = LocalDateTime.now();
        String normalizedBatch = batchNo != null ? batchNo : "";

        Inventory inv = inventoryMapper.getByMaterialBatchLocation(materialId, normalizedBatch, warehouseCode, slotCode);
        if (inv == null) {
            inv = new Inventory();
            inv.setMaterialId(materialId);
            inv.setWarehouseCode(warehouseCode);
            inv.setWarehouseName(warehouseName);
            inv.setLocationCode(slotCode);
            inv.setBatchNo(normalizedBatch);
            inv.setQuantityOnHand(BigDecimal.ZERO);
            inv.setQuantityReserved(BigDecimal.ZERO);
            inv.setQuantityAvailable(BigDecimal.ZERO);
            inv.setInventoryStatus("NORMAL");
            inv.setCreatedAt(now);
            inv.setUpdatedAt(now);
            inventoryMapper.insertInventory(inv);
        }

        inv.setQuantityOnHand(safe(inv.getQuantityOnHand()).add(qty));
        inv.setQuantityAvailable(safe(inv.getQuantityAvailable()).add(qty));
        inv.setLastTransactionAt(now);
        inv.setUpdatedAt(now);
        inventoryMapper.updateInventory(inv);

        occupySlot(slotCode, inv.getInventoryId(), materialId, mat.getMaterialName());
        recordTransaction(inv, mat, transactionType, qty, remark, handlerId, purchaseOrderId, workOrderId, now);
        return inv;
    }

    public void occupySlot(String slotCode, Long inventoryId, Long materialId, String materialName) {
        try {
            int updated = warehouseLocationMapper.occupySlot(slotCode, materialId, materialName, inventoryId);
            if (updated == 0) {
                throw new BusinessException("储位占用失败，可能已被其他物料占用");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            if (isMissingSchema(e)) {
                return;
            }
            throw e;
        }
    }

    public void releaseIfEmpty(Inventory inv) {
        if (inv == null || inv.getInventoryId() == null) {
            return;
        }
        if (safe(inv.getQuantityOnHand()).compareTo(BigDecimal.ZERO) > 0) {
            return;
        }
        try {
            warehouseLocationMapper.releaseSlotsByInventoryId(inv.getInventoryId());
        } catch (Exception e) {
            if (!isMissingSchema(e)) {
                throw e;
            }
        }
    }

    public String formatSlotLabel(String locationCode) {
        if (locationCode == null || locationCode.isBlank()) {
            return "—";
        }
        try {
            Map<String, Object> slot = safeSlotDetail(locationCode);
            if (slot != null) {
                return buildLabel(slot);
            }
        } catch (Exception e) {
            if (!isMissingSchema(e)) {
                throw e;
            }
        }
        return locationCode;
    }

    public String resolveZoneCodeForMaterial(Material mat) {
        if (mat == null) {
            return "RM-WH";
        }
        if ("FINISHED".equalsIgnoreCase(mat.getMaterialType())) {
            return "FG-WH";
        }
        String name = mat.getMaterialName() != null ? mat.getMaterialName() : "";
        if (name.contains("驱动IC") || name.contains("边框") || name.contains("适配器")) {
            return "AM-WH";
        }
        return "RM-WH";
    }

    public List<Map<String, Object>> listPendingPurchaseArrivals() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (PurchaseOrder order : purchaseOrderMapper.purchaseOrderList()) {
            if (order == null || order.getPurchaseOrderId() == null) {
                continue;
            }
            String status = order.getStatus();
            if (PO_RECEIVED.equals(status) || PO_CANCELLED.equals(status)) {
                continue;
            }
            if (!List.of("APPROVED", "SUBMITTED", "DRAFT", "PART_RECEIVED").contains(status)) {
                continue;
            }
            List<PurchaseOrderItem> items = purchaseOrderItemMapper.listByOrderIdWithMaterial(order.getPurchaseOrderId());
            if (items.isEmpty()) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("purchaseOrderId", order.getPurchaseOrderId());
            row.put("purchaseOrderNo", order.getPurchaseOrderNo());
            row.put("supplierName", order.getSupplierName());
            row.put("status", order.getStatus());
            row.put("expectedArrivalDate", order.getExpectedArrivalDate());
            row.put("items", items.stream().map(item -> {
                Map<String, Object> line = new LinkedHashMap<>();
                line.put("purchaseOrderItemId", item.getPurchaseOrderItemId());
                line.put("materialId", item.getMaterialId());
                line.put("materialCode", item.getMaterialCode());
                line.put("materialName", item.getMaterialName());
                line.put("quantity", item.getQuantity());
                line.put("unit", item.getUnit());
                Material mat = item.getMaterialId() != null ? materialMapper.getMaterialById(item.getMaterialId()) : null;
                line.put("zoneCode", resolveZoneCodeForMaterial(mat));
                return line;
            }).toList());
            list.add(row);
        }
        return list;
    }

    public Map<String, Object> enrichSlotOption(Map<String, Object> slot) {
        Map<String, Object> item = new LinkedHashMap<>(slot);
        item.put("label", buildLabel(slot));
        return item;
    }

    public String buildLabel(Map<String, Object> slot) {
        return String.format("%s / %s / %s (第%s层第%s格)",
                text(slot.get("zoneName")),
                text(slot.get("locationName")),
                text(slot.get("slotCode")),
                text(slot.get("rowNo")),
                text(slot.get("colNo")));
    }

    private List<Map<String, Object>> safeAvailable(String zoneCode) {
        List<Map<String, Object>> rows = warehouseLocationMapper.listAvailableSlotDetails(zoneCode);
        return rows != null ? rows : List.of();
    }

    private Map<String, Object> safeSlotDetail(String slotCode) {
        try {
            return warehouseLocationMapper.getSlotDetailByCode(slotCode);
        } catch (Exception e) {
            if (isMissingSchema(e)) {
                return null;
            }
            throw e;
        }
    }

    private void recordTransaction(Inventory inv, Material mat, String transactionType, BigDecimal qty,
                                   String remark, Long handlerId, Long purchaseOrderId, Long workOrderId,
                                   LocalDateTime now) {
        try {
            InventoryTransaction tx = new InventoryTransaction();
            tx.setTransactionNo("TX" + System.currentTimeMillis() + "-" + Objects.hash(inv.getInventoryId(), now));
            tx.setInventoryId(inv.getInventoryId());
            tx.setMaterialId(mat.getMaterialId());
            tx.setTransactionType(transactionType);
            tx.setQuantity(qty);
            tx.setWarehouseCode(inv.getWarehouseCode());
            tx.setLocationCode(inv.getLocationCode());
            tx.setBatchNo(inv.getBatchNo());
            tx.setRelatedPurchaseOrderId(purchaseOrderId);
            tx.setRelatedWorkOrderId(workOrderId);
            tx.setHandledBy(handlerId);
            tx.setHandledAt(now);
            tx.setRemark(remark);
            tx.setCreatedAt(now);
            inventoryTransactionMapper.insertTransaction(tx);
        } catch (Exception ignored) {
            // 流水表不可用时忽略，不影响主流程
        }
    }

    private static BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int intVal(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean isMissingSchema(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        return msg.contains("warehouse_zone") || msg.contains("warehouse_location")
                || msg.contains("doesn't exist") || msg.contains("不存在");
    }
}
