package com.upc.computer.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存流水实体，对应表 inventory_transaction
 */
public class InventoryTransaction {

    /** transaction_id */
    private Long transactionId;

    /** transaction_no */
    private String transactionNo;

    /** inventory_id */
    private Long inventoryId;

    /** material_id */
    private Long materialId;

    /** transaction_type */
    private String transactionType;

    /** quantity */
    private BigDecimal quantity;

    /** warehouse_code */
    private String warehouseCode;

    /** location_code */
    private String locationCode;

    /** batch_no */
    private String batchNo;

    /** related_purchase_order_id */
    private Long relatedPurchaseOrderId;

    /** related_work_order_id */
    private Long relatedWorkOrderId;

    /** handled_by */
    private Long handledBy;

    /** handled_at */
    private LocalDateTime handledAt;

    /** remark */
    private String remark;

    /** created_at */
    private LocalDateTime createdAt;

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public void setTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
    }

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Long getRelatedPurchaseOrderId() {
        return relatedPurchaseOrderId;
    }

    public void setRelatedPurchaseOrderId(Long relatedPurchaseOrderId) {
        this.relatedPurchaseOrderId = relatedPurchaseOrderId;
    }

    public Long getRelatedWorkOrderId() {
        return relatedWorkOrderId;
    }

    public void setRelatedWorkOrderId(Long relatedWorkOrderId) {
        this.relatedWorkOrderId = relatedWorkOrderId;
    }

    public Long getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(Long handledBy) {
        this.handledBy = handledBy;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
