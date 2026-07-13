package com.upc.computer.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 生产进度实体，对应表 work_progress
 */
public class WorkProgress {

    /** progress_id */
    private Long progressId;

    /** work_order_id */
    private Long workOrderId;

    /** dispatch_id */
    private Long dispatchId;

    /** progress_date */
    private LocalDate progressDate;

    /** progress_percent */
    private BigDecimal progressPercent;

    /** completed_quantity */
    private BigDecimal completedQuantity;

    /** current_status */
    private String currentStatus;

    /** progress_description */
    private String progressDescription;

    /** recorded_by */
    private Long recordedBy;

    /** recorded_at */
    private LocalDateTime recordedAt;

    public Long getProgressId() {
        return progressId;
    }

    public void setProgressId(Long progressId) {
        this.progressId = progressId;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public Long getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(Long dispatchId) {
        this.dispatchId = dispatchId;
    }

    public LocalDate getProgressDate() {
        return progressDate;
    }

    public void setProgressDate(LocalDate progressDate) {
        this.progressDate = progressDate;
    }

    public BigDecimal getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(BigDecimal progressPercent) {
        this.progressPercent = progressPercent;
    }

    public BigDecimal getCompletedQuantity() {
        return completedQuantity;
    }

    public void setCompletedQuantity(BigDecimal completedQuantity) {
        this.completedQuantity = completedQuantity;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getProgressDescription() {
        return progressDescription;
    }

    public void setProgressDescription(String progressDescription) {
        this.progressDescription = progressDescription;
    }

    public Long getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(Long recordedBy) {
        this.recordedBy = recordedBy;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

}
