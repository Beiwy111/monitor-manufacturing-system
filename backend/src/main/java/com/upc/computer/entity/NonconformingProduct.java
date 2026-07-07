package com.upc.computer.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 不合格品实体，对应表 nonconforming_product
 */
public class NonconformingProduct {

    /** nonconforming_id */
    private Long nonconformingId;

    /** nonconforming_no */
    private String nonconformingNo;

    /** inspection_id */
    private Long inspectionId;

    /** work_order_id */
    private Long workOrderId;

    /** work_report_id */
    private Long workReportId;

    /** material_id */
    private Long materialId;

    /** batch_no */
    private String batchNo;

    /** defect_type */
    private String defectType;

    /** defect_description */
    private String defectDescription;

    /** quantity */
    private BigDecimal quantity;

    /** severity */
    private String severity;

    /** handle_method */
    private String handleMethod;

    /** handle_status */
    private String handleStatus;

    /** registered_by */
    private Long registeredBy;

    /** registered_at */
    private LocalDateTime registeredAt;

    /** handled_by */
    private Long handledBy;

    /** handled_at */
    private LocalDateTime handledAt;

    /** remark */
    private String remark;

    /** created_at */
    private LocalDateTime createdAt;

    /** updated_at */
    private LocalDateTime updatedAt;

    public Long getNonconformingId() {
        return nonconformingId;
    }

    public void setNonconformingId(Long nonconformingId) {
        this.nonconformingId = nonconformingId;
    }

    public String getNonconformingNo() {
        return nonconformingNo;
    }

    public void setNonconformingNo(String nonconformingNo) {
        this.nonconformingNo = nonconformingNo;
    }

    public Long getInspectionId() {
        return inspectionId;
    }

    public void setInspectionId(Long inspectionId) {
        this.inspectionId = inspectionId;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public Long getWorkReportId() {
        return workReportId;
    }

    public void setWorkReportId(Long workReportId) {
        this.workReportId = workReportId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getDefectType() {
        return defectType;
    }

    public void setDefectType(String defectType) {
        this.defectType = defectType;
    }

    public String getDefectDescription() {
        return defectDescription;
    }

    public void setDefectDescription(String defectDescription) {
        this.defectDescription = defectDescription;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getHandleMethod() {
        return handleMethod;
    }

    public void setHandleMethod(String handleMethod) {
        this.handleMethod = handleMethod;
    }

    public String getHandleStatus() {
        return handleStatus;
    }

    public void setHandleStatus(String handleStatus) {
        this.handleStatus = handleStatus;
    }

    public Long getRegisteredBy() {
        return registeredBy;
    }

    public void setRegisteredBy(Long registeredBy) {
        this.registeredBy = registeredBy;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
