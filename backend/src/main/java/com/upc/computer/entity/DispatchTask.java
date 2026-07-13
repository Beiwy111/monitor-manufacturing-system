package com.upc.computer.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 派工任务实体，对应表 dispatch_task
 */
public class DispatchTask {

    /** dispatch_id */
    private Long dispatchId;

    /** dispatch_no */
    private String dispatchNo;

    /** work_order_id */
    private Long workOrderId;

    /** step_id */
    private Long stepId;

    /** operator_id */
    private Long operatorId;

    /** equipment_id */
    private Long equipmentId;

    /** assigned_quantity */
    private BigDecimal assignedQuantity;

    /** accepted_quantity */
    private BigDecimal acceptedQuantity;

    /** completed_quantity */
    private BigDecimal completedQuantity;

    /** assigned_by */
    private Long assignedBy;

    /** assigned_at */
    private LocalDateTime assignedAt;

    /** accepted_at */
    private LocalDateTime acceptedAt;

    /** status */
    private String status;

    /** remark */
    private String remark;

    /** created_at */
    private LocalDateTime createdAt;

    /** updated_at */
    private LocalDateTime updatedAt;

    public Long getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(Long dispatchId) {
        this.dispatchId = dispatchId;
    }

    public String getDispatchNo() {
        return dispatchNo;
    }

    public void setDispatchNo(String dispatchNo) {
        this.dispatchNo = dispatchNo;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public Long getStepId() {
        return stepId;
    }

    public void setStepId(Long stepId) {
        this.stepId = stepId;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public BigDecimal getAssignedQuantity() {
        return assignedQuantity;
    }

    public void setAssignedQuantity(BigDecimal assignedQuantity) {
        this.assignedQuantity = assignedQuantity;
    }

    public BigDecimal getAcceptedQuantity() {
        return acceptedQuantity;
    }

    public void setAcceptedQuantity(BigDecimal acceptedQuantity) {
        this.acceptedQuantity = acceptedQuantity;
    }

    public BigDecimal getCompletedQuantity() {
        return completedQuantity;
    }

    public void setCompletedQuantity(BigDecimal completedQuantity) {
        this.completedQuantity = completedQuantity;
    }

    public Long getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(Long assignedBy) {
        this.assignedBy = assignedBy;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
