package com.upc.computer.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 库存条码实例，对应 inventory_barcode。 */
public class InventoryBarcode {
    private Long barcodeId;
    private String barcodeNo;
    private Long materialId;
    private String batchNo;
    private Long inventoryId;
    private BigDecimal quantity;
    private BigDecimal remainingQuantity;
    private String barcodeStatus;
    private String sourceType;
    private String sourceNo;
    private Long relatedWorkOrderId;
    private Long relatedPurchaseOrderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getBarcodeId() { return barcodeId; }
    public void setBarcodeId(Long barcodeId) { this.barcodeId = barcodeId; }
    public String getBarcodeNo() { return barcodeNo; }
    public void setBarcodeNo(String barcodeNo) { this.barcodeNo = barcodeNo; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Long getInventoryId() { return inventoryId; }
    public void setInventoryId(Long inventoryId) { this.inventoryId = inventoryId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(BigDecimal remainingQuantity) { this.remainingQuantity = remainingQuantity; }
    public String getBarcodeStatus() { return barcodeStatus; }
    public void setBarcodeStatus(String barcodeStatus) { this.barcodeStatus = barcodeStatus; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceNo() { return sourceNo; }
    public void setSourceNo(String sourceNo) { this.sourceNo = sourceNo; }
    public Long getRelatedWorkOrderId() { return relatedWorkOrderId; }
    public void setRelatedWorkOrderId(Long relatedWorkOrderId) { this.relatedWorkOrderId = relatedWorkOrderId; }
    public Long getRelatedPurchaseOrderId() { return relatedPurchaseOrderId; }
    public void setRelatedPurchaseOrderId(Long relatedPurchaseOrderId) { this.relatedPurchaseOrderId = relatedPurchaseOrderId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
