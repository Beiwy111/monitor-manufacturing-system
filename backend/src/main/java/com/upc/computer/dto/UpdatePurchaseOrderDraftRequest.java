package com.upc.computer.dto;

import com.upc.computer.entity.PurchaseOrderItem;

import java.time.LocalDate;
import java.util.List;

/**
 * 草稿采购单保存请求：支持修改表头与明细数量、单价，自动重算行金额与总金额。
 */
public class UpdatePurchaseOrderDraftRequest {

    private Long purchaseOrderId;
    private String supplierName;
    private String supplierContact;
    private String supplierPhone;
    private LocalDate purchaseDate;
    private LocalDate expectedArrivalDate;
    private String remark;
    private List<PurchaseOrderItem> items;

    public Long getPurchaseOrderId() { return purchaseOrderId; }
    public void setPurchaseOrderId(Long purchaseOrderId) { this.purchaseOrderId = purchaseOrderId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getSupplierContact() { return supplierContact; }
    public void setSupplierContact(String supplierContact) { this.supplierContact = supplierContact; }

    public String getSupplierPhone() { return supplierPhone; }
    public void setSupplierPhone(String supplierPhone) { this.supplierPhone = supplierPhone; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public LocalDate getExpectedArrivalDate() { return expectedArrivalDate; }
    public void setExpectedArrivalDate(LocalDate expectedArrivalDate) { this.expectedArrivalDate = expectedArrivalDate; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public List<PurchaseOrderItem> getItems() { return items; }
    public void setItems(List<PurchaseOrderItem> items) { this.items = items; }
}
