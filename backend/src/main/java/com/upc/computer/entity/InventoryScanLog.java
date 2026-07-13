package com.upc.computer.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 库存扫码流水，对应 inventory_scan_log。 */
public class InventoryScanLog {
    private Long scanId;
    private String scanNo;
    private String barcodeNo;
    private String scanType;
    private BigDecimal quantity;
    private String warehouseCode;
    private String locationCode;
    private String businessNo;
    private String resultStatus;
    private String message;
    private Long handledBy;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;

    public Long getScanId() { return scanId; }
    public void setScanId(Long scanId) { this.scanId = scanId; }
    public String getScanNo() { return scanNo; }
    public void setScanNo(String scanNo) { this.scanNo = scanNo; }
    public String getBarcodeNo() { return barcodeNo; }
    public void setBarcodeNo(String barcodeNo) { this.barcodeNo = barcodeNo; }
    public String getScanType() { return scanType; }
    public void setScanType(String scanType) { this.scanType = scanType; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    public String getBusinessNo() { return businessNo; }
    public void setBusinessNo(String businessNo) { this.businessNo = businessNo; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getHandledBy() { return handledBy; }
    public void setHandledBy(Long handledBy) { this.handledBy = handledBy; }
    public LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(LocalDateTime handledAt) { this.handledAt = handledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
