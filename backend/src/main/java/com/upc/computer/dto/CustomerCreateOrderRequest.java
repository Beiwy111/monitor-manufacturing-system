package com.upc.computer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CustomerCreateOrderRequest {

    private Long materialId;
    private String productName;
    private String specification;
    private BigDecimal quantity;
    private String unit;
    private LocalDate requiredDeliveryDate;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String remark;
    private List<String> attachmentUrls;

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public LocalDate getRequiredDeliveryDate() { return requiredDeliveryDate; }
    public void setRequiredDeliveryDate(LocalDate requiredDeliveryDate) { this.requiredDeliveryDate = requiredDeliveryDate; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }

    public String getReceiverAddress() { return receiverAddress; }
    public void setReceiverAddress(String receiverAddress) { this.receiverAddress = receiverAddress; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public List<String> getAttachmentUrls() { return attachmentUrls; }
    public void setAttachmentUrls(List<String> attachmentUrls) { this.attachmentUrls = attachmentUrls; }
}
