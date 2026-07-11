package com.upc.computer.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成本结算实体，对应表 cost_settlement
 * 状态流转：DRAFT -> CONFIRMED -> EXPORTED
 */
public class CostSettlement {

    private Long settlementId;
    private String settlementNo;
    private Long workOrderId;
    private Long orderId;

    /** 成本来源类型：NONCONFORMING_PRODUCT/AFTER_SALES/EQUIPMENT_MAINTENANCE/PURCHASE_RETURN/WORK_ORDER/OTHER */
    private String sourceType;
    /** 来源业务主键，如 nonconforming_id / case_no / equipment_id */
    private String sourceId;
    /** 质量成本产生原因 */
    private String costReason;

    private BigDecimal materialCost;
    private BigDecimal laborCost;
    private BigDecimal equipmentCost;
    private BigDecimal qualityCost;
    private BigDecimal otherCost;
    private BigDecimal totalCost;

    private String settlementPeriod;
    private String settlementStatus;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
    private LocalDateTime exportedAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getSettlementId() { return settlementId; }
    public void setSettlementId(Long settlementId) { this.settlementId = settlementId; }

    public String getSettlementNo() { return settlementNo; }
    public void setSettlementNo(String settlementNo) { this.settlementNo = settlementNo; }

    public Long getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(Long workOrderId) { this.workOrderId = workOrderId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getCostReason() { return costReason; }
    public void setCostReason(String costReason) { this.costReason = costReason; }

    public BigDecimal getMaterialCost() { return materialCost; }
    public void setMaterialCost(BigDecimal materialCost) { this.materialCost = materialCost; }

    public BigDecimal getLaborCost() { return laborCost; }
    public void setLaborCost(BigDecimal laborCost) { this.laborCost = laborCost; }

    public BigDecimal getEquipmentCost() { return equipmentCost; }
    public void setEquipmentCost(BigDecimal equipmentCost) { this.equipmentCost = equipmentCost; }

    public BigDecimal getQualityCost() { return qualityCost; }
    public void setQualityCost(BigDecimal qualityCost) { this.qualityCost = qualityCost; }

    public BigDecimal getOtherCost() { return otherCost; }
    public void setOtherCost(BigDecimal otherCost) { this.otherCost = otherCost; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public String getSettlementPeriod() { return settlementPeriod; }
    public void setSettlementPeriod(String settlementPeriod) { this.settlementPeriod = settlementPeriod; }

    public String getSettlementStatus() { return settlementStatus; }
    public void setSettlementStatus(String settlementStatus) { this.settlementStatus = settlementStatus; }

    public Long getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(Long confirmedBy) { this.confirmedBy = confirmedBy; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public LocalDateTime getExportedAt() { return exportedAt; }
    public void setExportedAt(LocalDateTime exportedAt) { this.exportedAt = exportedAt; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
