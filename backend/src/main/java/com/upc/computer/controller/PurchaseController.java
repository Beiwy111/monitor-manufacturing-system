package com.upc.computer.controller;

import com.upc.computer.service.PurchaseService;
import com.upc.computer.entity.PurchaseOrder;
import com.upc.computer.entity.PurchaseOrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;

@RestController
@RequestMapping("/purchase")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    // 查询采购订单列表
    @RequestMapping("/purchaseOrder/list")
    public ArrayList<PurchaseOrder> purchaseOrderList() {
        return purchaseService.purchaseOrderList();
    }

    // 根据主键查询采购订单
    @RequestMapping("/purchaseOrder/get")
    public PurchaseOrder getPurchaseOrderById(Long purchaseOrderId) {
        return purchaseService.getPurchaseOrderById(purchaseOrderId);
    }

    // 新增采购订单
    @RequestMapping("/purchaseOrder/insert")
    public void insertPurchaseOrder(PurchaseOrder purchaseOrder) {
        purchaseService.insertPurchaseOrder(purchaseOrder);
    }

    // 修改采购订单
    @RequestMapping("/purchaseOrder/update")
    public void updatePurchaseOrder(PurchaseOrder purchaseOrder) {
        purchaseService.updatePurchaseOrder(purchaseOrder);
    }

    // 删除采购订单
    @RequestMapping("/purchaseOrder/delete")
    public void deletePurchaseOrder(Long purchaseOrderId) {
        purchaseService.deletePurchaseOrder(purchaseOrderId);
    }

    // 查询采购明细列表
    @RequestMapping("/purchaseOrderItem/list")
    public ArrayList<PurchaseOrderItem> purchaseOrderItemList() {
        return purchaseService.purchaseOrderItemList();
    }

    // 根据主键查询采购明细
    @RequestMapping("/purchaseOrderItem/get")
    public PurchaseOrderItem getPurchaseOrderItemById(Long purchaseOrderItemId) {
        return purchaseService.getPurchaseOrderItemById(purchaseOrderItemId);
    }

    // 新增采购明细
    @RequestMapping("/purchaseOrderItem/insert")
    public void insertPurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) {
        purchaseService.insertPurchaseOrderItem(purchaseOrderItem);
    }

    // 修改采购明细
    @RequestMapping("/purchaseOrderItem/update")
    public void updatePurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) {
        purchaseService.updatePurchaseOrderItem(purchaseOrderItem);
    }

    // 删除采购明细
    @RequestMapping("/purchaseOrderItem/delete")
    public void deletePurchaseOrderItem(Long purchaseOrderItemId) {
        purchaseService.deletePurchaseOrderItem(purchaseOrderItemId);
    }

}
