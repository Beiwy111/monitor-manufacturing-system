package com.upc.computer.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PurchaseOrderItem {

    private Long purchaseOrderItemId;
    private Long purchaseOrderId;
    private Long materialId;

    /** joined from material table, not a DB column */
    private String materialCode;
    private String materialName;

    private BigDecimal quantity;
    private BigDecimal receivedQuantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal lineAmount;
    private String itemStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getPurchaseOrderItemId() { return purchaseOrderItemId; }
    public void setPurchaseOrderItemId(Long purchaseOrderItemId) { this.purchaseOrderItemId = purchaseOrderItemId; }

    public Long getPurchaseOrderId() { return purchaseOrderId; }
    public void setPurchaseOrderId(Long purchaseOrderId) { this.purchaseOrderId = purchaseOrderId; }

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(BigDecimal receivedQuantity) { this.receivedQuantity = receivedQuantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getLineAmount() { return lineAmount; }
    public void setLineAmount(BigDecimal lineAmount) { this.lineAmount = lineAmount; }

    public String getItemStatus() { return itemStatus; }
    public void setItemStatus(String itemStatus) { this.itemStatus = itemStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
