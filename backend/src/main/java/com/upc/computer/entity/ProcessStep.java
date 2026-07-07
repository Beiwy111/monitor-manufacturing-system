package com.upc.computer.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工序实体，对应表 process_step
 */
public class ProcessStep {

    /** step_id */
    private Long stepId;

    /** route_id */
    private Long routeId;

    /** step_no */
    private Integer stepNo;

    /** step_code */
    private String stepCode;

    /** step_name */
    private String stepName;

    /** standard_work_hours */
    private BigDecimal standardWorkHours;

    /** standard_equipment_type */
    private String standardEquipmentType;

    /** quality_required */
    private Integer qualityRequired;

    /** status */
    private Integer status;

    /** created_at */
    private LocalDateTime createdAt;

    /** updated_at */
    private LocalDateTime updatedAt;

    public Long getStepId() {
        return stepId;
    }

    public void setStepId(Long stepId) {
        this.stepId = stepId;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public Integer getStepNo() {
        return stepNo;
    }

    public void setStepNo(Integer stepNo) {
        this.stepNo = stepNo;
    }

    public String getStepCode() {
        return stepCode;
    }

    public void setStepCode(String stepCode) {
        this.stepCode = stepCode;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public BigDecimal getStandardWorkHours() {
        return standardWorkHours;
    }

    public void setStandardWorkHours(BigDecimal standardWorkHours) {
        this.standardWorkHours = standardWorkHours;
    }

    public String getStandardEquipmentType() {
        return standardEquipmentType;
    }

    public void setStandardEquipmentType(String standardEquipmentType) {
        this.standardEquipmentType = standardEquipmentType;
    }

    public Integer getQualityRequired() {
        return qualityRequired;
    }

    public void setQualityRequired(Integer qualityRequired) {
        this.qualityRequired = qualityRequired;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
