package com.upc.computer.service;

import com.upc.computer.entity.PurchaseOrder;
import com.upc.computer.entity.PurchaseOrderItem;
import java.util.ArrayList;

public interface PurchaseService {

    public ArrayList<PurchaseOrder> purchaseOrderList();

    public PurchaseOrder getPurchaseOrderById(Long purchaseOrderId);

    public void insertPurchaseOrder(PurchaseOrder purchaseOrder);

    public void updatePurchaseOrder(PurchaseOrder purchaseOrder);

    public void deletePurchaseOrder(Long purchaseOrderId);

    public ArrayList<PurchaseOrderItem> purchaseOrderItemList();

    public PurchaseOrderItem getPurchaseOrderItemById(Long purchaseOrderItemId);

    public void insertPurchaseOrderItem(PurchaseOrderItem purchaseOrderItem);

    public void updatePurchaseOrderItem(PurchaseOrderItem purchaseOrderItem);

    public void deletePurchaseOrderItem(Long purchaseOrderItemId);

}
