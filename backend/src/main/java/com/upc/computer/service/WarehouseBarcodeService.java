package com.upc.computer.service;

import com.upc.computer.common.BusinessException;
import com.upc.computer.entity.*;
import com.upc.computer.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 仓储条码/批次/扫码服务。 */
@Service
public class WarehouseBarcodeService {
    private static final String DEFAULT_WAREHOUSE_CODE = "FG-WH";
    private static final String DEFAULT_WAREHOUSE_NAME = "成品仓";
    private static final String DEFAULT_LOCATION_CODE = "FG-A01";

    @Autowired
    private BarcodeRuleMapper barcodeRuleMapper;
    @Autowired
    private MaterialBatchMapper materialBatchMapper;
    @Autowired
    private InventoryBarcodeMapper inventoryBarcodeMapper;
    @Autowired
    private InventoryScanLogMapper inventoryScanLogMapper;
    @Autowired
    private InventoryMapper inventoryMapper;
    @Autowired
    private InventoryTransactionMapper inventoryTransactionMapper;
    @Autowired
    private MaterialMapper materialMapper;
    @Autowired
    private UserMapper userMapper;

    public List<BarcodeRule> rules() {
        try {
            return barcodeRuleMapper.ruleList();
        } catch (Exception e) {
            if (isMissingBarcodeSchema(e)) {
                return List.of();
            }
            throw e;
        }
    }

    @Transactional
    public BarcodeRule saveRule(BarcodeRule rule) {
        if (rule == null || blank(rule.getBusinessType()) || blank(rule.getPrefix())) {
            throw new BusinessException("条码规则业务类型和前缀不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        rule.setDatePattern(defaultText(rule.getDatePattern(), "yyyyMMdd"));
        rule.setSerialLength(rule.getSerialLength() != null ? rule.getSerialLength() : 5);
        rule.setCurrentSerial(rule.getCurrentSerial() != null ? rule.getCurrentSerial() : 0L);
        rule.setStatus(rule.getStatus() != null ? rule.getStatus() : 1);
        rule.setUpdatedAt(now);
        if (rule.getRuleId() == null) {
            rule.setRuleCode(defaultText(rule.getRuleCode(), "RULE-" + rule.getBusinessType()));
            rule.setCreatedAt(now);
            barcodeRuleMapper.insertRule(rule);
        } else {
            barcodeRuleMapper.updateRule(rule);
        }
        return rule;
    }

    @Transactional
    public synchronized String generateBarcode(String businessType) {
        String type = defaultText(businessType, "PRODUCT_IN");
        BarcodeRule rule = barcodeRuleMapper.getEnabledByBusinessType(type);
        if (rule == null) {
            throw new BusinessException("未配置启用的条码规则：" + type);
        }
        long nextSerial = Optional.ofNullable(rule.getCurrentSerial()).orElse(0L) + 1;
        barcodeRuleMapper.updateSerial(rule.getRuleId(), nextSerial);
        String date = LocalDateTime.now().format(dateFormatter(rule.getDatePattern()));
        int serialLength = rule.getSerialLength() != null ? rule.getSerialLength() : 5;
        return rule.getPrefix() + date + String.format("%0" + serialLength + "d", nextSerial);
    }

    @Transactional
    public InventoryBarcode generateForInbound(Material material, Inventory inventory, BigDecimal quantity,
                                               String sourceType, String sourceNo, Long workOrderId,
                                               Long purchaseOrderId, LocalDateTime now) {
        if (material == null || material.getMaterialId() == null || inventory == null || inventory.getInventoryId() == null) {
            throw new BusinessException("入库条码缺少物料或库存信息");
        }
        BigDecimal qty = quantity != null ? quantity : BigDecimal.ZERO;
        upsertBatch(material.getMaterialId(), inventory.getBatchNo(), sourceType, sourceNo, qty, now);

        InventoryBarcode barcode = new InventoryBarcode();
        barcode.setBarcodeNo(generateBarcode(defaultText(sourceType, "PRODUCT_IN")));
        barcode.setMaterialId(material.getMaterialId());
        barcode.setBatchNo(inventory.getBatchNo());
        barcode.setInventoryId(inventory.getInventoryId());
        barcode.setQuantity(qty);
        barcode.setRemainingQuantity(qty);
        barcode.setBarcodeStatus("IN_STOCK");
        barcode.setSourceType(defaultText(sourceType, "PRODUCT_IN"));
        barcode.setSourceNo(sourceNo);
        barcode.setRelatedWorkOrderId(workOrderId);
        barcode.setRelatedPurchaseOrderId(purchaseOrderId);
        barcode.setCreatedAt(now);
        barcode.setUpdatedAt(now);
        inventoryBarcodeMapper.insertBarcode(barcode);
        writeScanLog(barcode.getBarcodeNo(), "PRODUCT_IN", qty, inventory.getWarehouseCode(),
                inventory.getLocationCode(), sourceNo, "SUCCESS", "入库生成条码", null, now);
        return barcode;
    }

    @Transactional
    public Map<String, Object> scanBarcode(Map<String, Object> body, String operator) {
        String barcodeNo = text(body.get("barcodeNo"));
        String scanType = defaultText(text(body.get("scanType")), "QUERY");
        BigDecimal qty = decimal(body.get("quantity"), BigDecimal.ONE);
        if (blank(barcodeNo)) {
            throw new BusinessException("条码不能为空");
        }
        InventoryBarcode barcode = inventoryBarcodeMapper.getByBarcodeNo(barcodeNo);
        if (barcode == null) {
            writeScanLog(barcodeNo, scanType, qty, text(body.get("warehouseCode")), text(body.get("locationCode")),
                    text(body.get("businessNo")), "FAILED", "条码不存在", userId(operator), LocalDateTime.now());
            throw new BusinessException("条码不存在");
        }
        if ("MATERIAL_OUT".equals(scanType)) {
            deductBarcode(barcode, qty);
            writeScanLog(barcodeNo, scanType, qty, text(body.get("warehouseCode")), text(body.get("locationCode")),
                    text(body.get("businessNo")), "SUCCESS", "扫码出库成功", userId(operator), LocalDateTime.now());
        } else {
            writeScanLog(barcodeNo, scanType, qty, text(body.get("warehouseCode")), text(body.get("locationCode")),
                    text(body.get("businessNo")), "SUCCESS", "扫码成功", userId(operator), LocalDateTime.now());
        }
        return queryBarcodeTrace(barcodeNo);
    }

    @Transactional
    public InventoryBarcode scanIssue(String barcodeNo, BigDecimal qty, String businessNo, String operator) {
        if (blank(barcodeNo)) {
            return null;
        }
        InventoryBarcode barcode = inventoryBarcodeMapper.getByBarcodeNo(barcodeNo);
        if (barcode == null) {
            throw new BusinessException("领料条码不存在");
        }
        deductBarcode(barcode, qty);
        writeScanLog(barcodeNo, "MATERIAL_OUT", qty, null, null, businessNo,
                "SUCCESS", "生产扫码领料", userId(operator), LocalDateTime.now());
        return barcode;
    }

    public Map<String, Object> queryBarcodeTrace(String barcodeNo) {
        InventoryBarcode barcode = inventoryBarcodeMapper.getByBarcodeNo(barcodeNo);
        if (barcode == null) {
            throw new BusinessException("条码不存在");
        }
        Material material = materialMapper.materialList().stream()
                .filter(m -> barcode.getMaterialId().equals(m.getMaterialId()))
                .findFirst().orElse(null);
        Inventory inventory = barcode.getInventoryId() != null
                ? inventoryMapper.getInventoryById(barcode.getInventoryId()) : null;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("barcode", barcode);
        result.put("material", material);
        result.put("inventory", inventory);
        result.put("scanLogs", inventoryScanLogMapper.listByBarcodeNo(barcodeNo));
        result.put("transactions", inventoryTransactionMapper.transactionList().stream()
                .filter(t -> Objects.equals(t.getInventoryId(), barcode.getInventoryId())
                        || Objects.equals(t.getBatchNo(), barcode.getBatchNo()))
                .toList());
        return result;
    }

    public List<Map<String, Object>> barcodeList() {
        try {
            return inventoryBarcodeMapper.barcodeDetailList();
        } catch (Exception e) {
            if (isMissingBarcodeSchema(e)) {
                return List.of();
            }
            throw e;
        }
    }

    public List<Map<String, Object>> scanLogs() {
        try {
            return inventoryScanLogMapper.scanDetailList();
        } catch (Exception e) {
            if (isMissingBarcodeSchema(e)) {
                return List.of();
            }
            throw e;
        }
    }

    public List<Map<String, Object>> inventoryList() {
        try {
            return inventoryMapper.inventoryDetailList();
        } catch (Exception e) {
            if (isMissingBarcodeSchema(e)) {
                return inventoryMapper.inventoryList().stream().map(inv -> {
                    Material mat = materialMapper.getMaterialById(inv.getMaterialId());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("inventoryId", inv.getInventoryId());
                    row.put("materialId", inv.getMaterialId());
                    row.put("materialCode", mat != null ? mat.getMaterialCode() : "");
                    row.put("materialName", mat != null ? mat.getMaterialName() : "");
                    row.put("unit", mat != null ? mat.getUnit() : "");
                    row.put("warehouseCode", inv.getWarehouseCode());
                    row.put("warehouseName", inv.getWarehouseName());
                    row.put("locationCode", inv.getLocationCode());
                    row.put("batchNo", inv.getBatchNo());
                    row.put("quantityOnHand", inv.getQuantityOnHand());
                    row.put("quantityReserved", inv.getQuantityReserved());
                    row.put("quantityAvailable", inv.getQuantityAvailable());
                    row.put("inventoryStatus", inv.getInventoryStatus());
                    row.put("barcodeQuantity", BigDecimal.ZERO);
                    row.put("barcodeCount", 0);
                    row.put("lastScanAt", null);
                    row.put("lastTransactionAt", inv.getLastTransactionAt());
                    row.put("updatedAt", inv.getUpdatedAt());
                    return row;
                }).toList();
            }
            throw e;
        }
    }

    public List<Map<String, Object>> inventoryCatalog() {
        List<Map<String, Object>> rows = inventoryMapper.inventoryCatalogList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>(row);
            String materialType = text(row.get("materialType"));
            boolean finished = "FINISHED".equalsIgnoreCase(materialType);
            item.put("warehouseCategory", finished ? "FINISHED" : "RAW");
            item.put("categoryLabel", finished ? "成品" : resolveRawCategory(text(row.get("materialName"))));
            BigDecimal qty = decimal(row.get("quantityOnHand"), BigDecimal.ZERO);
            BigDecimal safe = decimal(row.get("safetyStock"), BigDecimal.valueOf(100));
            item.put("stockStatus", qty.compareTo(safe) < 0 ? "预警" : "正常");
            item.put("displayQty", finished
                    ? decimal(row.get("finishedWarehouseQty"), qty)
                    : decimal(row.get("rawWarehouseQty"), qty));
            list.add(item);
        }
        return list;
    }

    public List<Map<String, Object>> transactionList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : inventoryTransactionMapper.transactionDetailList()) {
            Map<String, Object> item = new LinkedHashMap<>();
            String type = text(row.get("transactionType"));
            item.put("id", row.get("transactionNo"));
            item.put("flowType", flowTypeLabel(type));
            item.put("materialCode", row.get("materialCode"));
            item.put("materialName", row.get("materialName"));
            item.put("materialType", row.get("materialType"));
            item.put("quantity", decimal(row.get("quantity"), BigDecimal.ZERO));
            item.put("direction", flowDirection(type));
            item.put("warehouseCode", row.get("warehouseCode"));
            item.put("locationCode", row.get("locationCode"));
            item.put("batchNo", row.get("batchNo"));
            item.put("refNo", row.get("remark"));
            item.put("operator", row.get("operatorName"));
            item.put("createdAt", row.get("handledAt"));
            list.add(item);
        }
        return list;
    }

    private String resolveRawCategory(String name) {
        if (blank(name)) {
            return "原材料";
        }
        if (name.contains("面板") || name.contains("LCD") || name.contains("OLED")) {
            return "显示面板";
        }
        if (name.contains("背光")) {
            return "背光模组";
        }
        if (name.contains("主控") || name.contains("PCB") || name.contains("主板")) {
            return "主控电路";
        }
        if (name.contains("边框") || name.contains("电源") || name.contains("适配器")) {
            return "结构附件";
        }
        return "原材料";
    }

    private String flowTypeLabel(String type) {
        return switch (defaultText(type, "")) {
            case "PRODUCT_IN" -> "成品入库";
            case "PURCHASE_IN" -> "采购入库";
            case "MATERIAL_OUT" -> "生产领料";
            case "PRODUCT_OUT" -> "成品出库";
            case "ADJUST_IN" -> "盘盈入库";
            case "ADJUST_OUT" -> "盘亏出库";
            default -> type;
        };
    }

    private String flowDirection(String type) {
        return switch (defaultText(type, "")) {
            case "MATERIAL_OUT", "PRODUCT_OUT", "ADJUST_OUT" -> "出";
            default -> "入";
        };
    }

    public boolean isBarcodeSchemaAvailable() {
        try {
            barcodeRuleMapper.ruleList();
            return true;
        } catch (Exception e) {
            return isMissingBarcodeSchema(e);
        }
    }

    private boolean isMissingBarcodeSchema(Exception e) {
        String message = e.getMessage();
        Throwable cause = e.getCause();
        while ((message == null || message.isBlank()) && cause != null) {
            message = cause.getMessage();
            cause = cause.getCause();
        }
        if (message == null) {
            return false;
        }
        return message.contains("inventory_barcode")
                || message.contains("inventory_scan_log")
                || message.contains("barcode_rule")
                || message.contains("material_batch");
    }

    private void upsertBatch(Long materialId, String batchNo, String sourceType, String sourceNo,
                             BigDecimal qty, LocalDateTime now) {
        if (blank(batchNo)) {
            return;
        }
        MaterialBatch batch = materialBatchMapper.getByBatchNoAndMaterial(batchNo, materialId);
        if (batch == null) {
            batch = new MaterialBatch();
            batch.setBatchNo(batchNo);
            batch.setMaterialId(materialId);
            batch.setSourceType(defaultText(sourceType, "PRODUCT_IN"));
            batch.setSourceNo(sourceNo);
            batch.setQuantity(qty);
            batch.setBatchStatus("NORMAL");
            batch.setProducedAt(now);
            batch.setReceivedAt(now);
            batch.setCreatedAt(now);
            batch.setUpdatedAt(now);
            materialBatchMapper.insertBatch(batch);
        } else {
            batch.setSourceType(defaultText(sourceType, batch.getSourceType()));
            batch.setSourceNo(defaultText(sourceNo, batch.getSourceNo()));
            batch.setQuantity(Optional.ofNullable(batch.getQuantity()).orElse(BigDecimal.ZERO).add(qty));
            batch.setBatchStatus("NORMAL");
            batch.setReceivedAt(now);
            batch.setUpdatedAt(now);
            materialBatchMapper.updateBatch(batch);
        }
    }

    private void deductBarcode(InventoryBarcode barcode, BigDecimal qty) {
        BigDecimal remain = Optional.ofNullable(barcode.getRemainingQuantity()).orElse(BigDecimal.ZERO);
        BigDecimal outQty = qty != null ? qty : BigDecimal.ZERO;
        if (outQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("扫码数量必须大于0");
        }
        if (remain.compareTo(outQty) < 0) {
            throw new BusinessException("条码库存不足");
        }
        BigDecimal after = remain.subtract(outQty);
        barcode.setRemainingQuantity(after);
        barcode.setBarcodeStatus(after.compareTo(BigDecimal.ZERO) == 0 ? "CONSUMED" : "PARTIAL");
        barcode.setUpdatedAt(LocalDateTime.now());
        inventoryBarcodeMapper.updateBarcode(barcode);
    }

    private void writeScanLog(String barcodeNo, String scanType, BigDecimal qty, String warehouseCode,
                              String locationCode, String businessNo, String status, String message,
                              Long handledBy, LocalDateTime now) {
        InventoryScanLog log = new InventoryScanLog();
        log.setScanNo("SL" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        log.setBarcodeNo(barcodeNo);
        log.setScanType(scanType);
        log.setQuantity(qty != null ? qty : BigDecimal.ZERO);
        log.setWarehouseCode(warehouseCode);
        log.setLocationCode(locationCode);
        log.setBusinessNo(businessNo);
        log.setResultStatus(status);
        log.setMessage(message);
        log.setHandledBy(handledBy);
        log.setHandledAt(now);
        log.setCreatedAt(now);
        inventoryScanLogMapper.insertScanLog(log);
    }

    private Long userId(String username) {
        if (blank(username)) {
            return null;
        }
        return userMapper.userList().stream()
                .filter(u -> username.equals(u.getUsername()) || username.equals(u.getRealName()))
                .map(User::getUserId)
                .findFirst().orElse(null);
    }

    private DateTimeFormatter dateFormatter(String pattern) {
        try {
            return DateTimeFormatter.ofPattern(defaultText(pattern, "yyyyMMdd"));
        } catch (IllegalArgumentException e) {
            return DateTimeFormatter.ofPattern("yyyyMMdd");
        }
    }

    private BigDecimal decimal(Object value, BigDecimal defaultValue) {
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String defaultText(String value, String defaultValue) {
        return blank(value) ? defaultValue : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
