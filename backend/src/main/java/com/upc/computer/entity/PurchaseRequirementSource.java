package com.upc.computer.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购需求来源追溯，对应表 purchase_requirement_source
 */
public class PurchaseRequirementSource {

    /** source_id */
    private Long sourceId;

    /** requirement_id 关联采购需求 */
    private Long requirementId;

    /** customer_order_id 来源销售订单ID */
    private Long customerOrderId;

    /** customer_order_no 来源销售订单编号 */
    private String customerOrderNo;

    /** work_order_id 来源生产工单ID */
    private Long workOrderId;

    /** work_order_no 来源生产工单编号 */
    private String workOrderNo;

    /** source_type ORDER/WORK_ORDER 来源类型 */
    private String sourceType;

    /** material_id 缺料物料 */
    private Long materialId;

    /** required_quantity 该来源需求数量 */
    private BigDecimal requiredQuantity;

    /** shortage_quantity 该来源缺料数量 */
    private BigDecimal shortageQuantity;

    /** created_at */
    private LocalDateTime createdAt;

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Long getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(Long requirementId) {
        this.requirementId = requirementId;
    }

    public Long getCustomerOrderId() {
        return customerOrderId;
    }

    public void setCustomerOrderId(Long customerOrderId) {
        this.customerOrderId = customerOrderId;
    }

    public String getCustomerOrderNo() {
        return customerOrderNo;
    }

    public void setCustomerOrderNo(String customerOrderNo) {
        this.customerOrderNo = customerOrderNo;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public String getWorkOrderNo() {
        return workOrderNo;
    }

    public void setWorkOrderNo(String workOrderNo) {
        this.workOrderNo = workOrderNo;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public BigDecimal getRequiredQuantity() {
        return requiredQuantity;
    }

    public void setRequiredQuantity(BigDecimal requiredQuantity) {
        this.requiredQuantity = requiredQuantity;
    }

    public BigDecimal getShortageQuantity() {
        return shortageQuantity;
    }

    public void setShortageQuantity(BigDecimal shortageQuantity) {
        this.shortageQuantity = shortageQuantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
