package com.upc.computer.entity;

import java.time.LocalDateTime;

public class Supplier {

    private Long supplierId;
    private String supplierNo;
    private String supplierName;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String address;
    private String supplyMaterials;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public String getSupplierNo() { return supplierNo; }
    public void setSupplierNo(String supplierNo) { this.supplierNo = supplierNo; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getSupplyMaterials() { return supplyMaterials; }
    public void setSupplyMaterials(String supplyMaterials) { this.supplyMaterials = supplyMaterials; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
