package com.upc.computer.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PurchaseRequirement {

    private Long requirementId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private BigDecimal requiredQuantity;
    private BigDecimal stockQuantity;
    private BigDecimal onPurchaseQuantity;
    private BigDecimal shortageQuantity;
    private BigDecimal suggestedPurchaseQuantity;
    private String status;
    private Integer priority;
    private LocalDate expectedArrivalDate;
    private Long purchaseOrderId;
    private Long supplierId;
    private String supplierName;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getRequirementId() { return requirementId; }
    public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public BigDecimal getRequiredQuantity() { return requiredQuantity; }
    public void setRequiredQuantity(BigDecimal requiredQuantity) { this.requiredQuantity = requiredQuantity; }

    public BigDecimal getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(BigDecimal stockQuantity) { this.stockQuantity = stockQuantity; }

    public BigDecimal getOnPurchaseQuantity() { return onPurchaseQuantity; }
    public void setOnPurchaseQuantity(BigDecimal onPurchaseQuantity) { this.onPurchaseQuantity = onPurchaseQuantity; }

    public BigDecimal getShortageQuantity() { return shortageQuantity; }
    public void setShortageQuantity(BigDecimal shortageQuantity) { this.shortageQuantity = shortageQuantity; }

    public BigDecimal getSuggestedPurchaseQuantity() { return suggestedPurchaseQuantity; }
    public void setSuggestedPurchaseQuantity(BigDecimal suggestedPurchaseQuantity) { this.suggestedPurchaseQuantity = suggestedPurchaseQuantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public LocalDate getExpectedArrivalDate() { return expectedArrivalDate; }
    public void setExpectedArrivalDate(LocalDate expectedArrivalDate) { this.expectedArrivalDate = expectedArrivalDate; }

    public Long getPurchaseOrderId() { return purchaseOrderId; }
    public void setPurchaseOrderId(Long purchaseOrderId) { this.purchaseOrderId = purchaseOrderId; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
