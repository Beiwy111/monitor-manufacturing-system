package com.upc.computer.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 小时产量与质量趋势，对应表 prod_hourly_metric
 */
public class ProdHourlyMetric {

    private Long metricId;
    private LocalDate statDate;
    private Integer statHour;
    private BigDecimal plannedOutput;
    private BigDecimal actualOutput;
    private BigDecimal qualifiedQty;
    private BigDecimal unqualifiedQty;
    private Integer alarmCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getMetricId() {
        return metricId;
    }

    public void setMetricId(Long metricId) {
        this.metricId = metricId;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public Integer getStatHour() {
        return statHour;
    }

    public void setStatHour(Integer statHour) {
        this.statHour = statHour;
    }

    public BigDecimal getPlannedOutput() {
        return plannedOutput;
    }

    public void setPlannedOutput(BigDecimal plannedOutput) {
        this.plannedOutput = plannedOutput;
    }

    public BigDecimal getActualOutput() {
        return actualOutput;
    }

    public void setActualOutput(BigDecimal actualOutput) {
        this.actualOutput = actualOutput;
    }

    public BigDecimal getQualifiedQty() {
        return qualifiedQty;
    }

    public void setQualifiedQty(BigDecimal qualifiedQty) {
        this.qualifiedQty = qualifiedQty;
    }

    public BigDecimal getUnqualifiedQty() {
        return unqualifiedQty;
    }

    public void setUnqualifiedQty(BigDecimal unqualifiedQty) {
        this.unqualifiedQty = unqualifiedQty;
    }

    public Integer getAlarmCount() {
        return alarmCount;
    }

    public void setAlarmCount(Integer alarmCount) {
        this.alarmCount = alarmCount;
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
