package com.upc.computer.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * BOM实体，对应表 bom
 */
public class Bom {

    /** bom_id */
    private Long bomId;

    /** parent_material_id */
    private Long parentMaterialId;

    /** child_material_id */
    private Long childMaterialId;

    /** quantity */
    private BigDecimal quantity;

    /** loss_rate */
    private BigDecimal lossRate;

    /** version_no */
    private String versionNo;

    /** effective_date */
    private LocalDate effectiveDate;

    /** expire_date */
    private LocalDate expireDate;

    /** status */
    private Integer status;

    /** remark */
    private String remark;

    /** created_at */
    private LocalDateTime createdAt;

    /** updated_at */
    private LocalDateTime updatedAt;

    public Long getBomId() {
        return bomId;
    }

    public void setBomId(Long bomId) {
        this.bomId = bomId;
    }

    public Long getParentMaterialId() {
        return parentMaterialId;
    }

    public void setParentMaterialId(Long parentMaterialId) {
        this.parentMaterialId = parentMaterialId;
    }

    public Long getChildMaterialId() {
        return childMaterialId;
    }

    public void setChildMaterialId(Long childMaterialId) {
        this.childMaterialId = childMaterialId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLossRate() {
        return lossRate;
    }

    public void setLossRate(BigDecimal lossRate) {
        this.lossRate = lossRate;
    }

    public String getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(String versionNo) {
        this.versionNo = versionNo;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(LocalDate expireDate) {
        this.expireDate = expireDate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
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
