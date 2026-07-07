package com.upc.computer.service.impl;

import com.upc.computer.service.PurchaseService;
import com.upc.computer.entity.PurchaseOrder;
import com.upc.computer.mapper.PurchaseOrderMapper;
import com.upc.computer.entity.PurchaseOrderItem;
import com.upc.computer.mapper.PurchaseOrderItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;

@Service
public class PurchaseServiceImpl implements PurchaseService {

    @Autowired
    private PurchaseOrderMapper purchaseOrderMapper;

    @Autowired
    private PurchaseOrderItemMapper purchaseOrderItemMapper;

    // 查询所有采购订单
    @Override
    public ArrayList<PurchaseOrder> purchaseOrderList() {
        return purchaseOrderMapper.purchaseOrderList();
    }

    // 根据主键查询采购订单
    @Override
    public PurchaseOrder getPurchaseOrderById(Long purchaseOrderId) {
        return purchaseOrderMapper.getPurchaseOrderById(purchaseOrderId);
    }

    // 新增采购订单
    @Override
    public void insertPurchaseOrder(PurchaseOrder purchaseOrder) {
        purchaseOrderMapper.insertPurchaseOrder(purchaseOrder);
    }

    // 修改采购订单
    @Override
    public void updatePurchaseOrder(PurchaseOrder purchaseOrder) {
        purchaseOrderMapper.updatePurchaseOrder(purchaseOrder);
    }

    // 删除采购订单
    @Override
    public void deletePurchaseOrder(Long purchaseOrderId) {
        purchaseOrderMapper.deletePurchaseOrder(purchaseOrderId);
    }

    // 查询所有采购明细
    @Override
    public ArrayList<PurchaseOrderItem> purchaseOrderItemList() {
        return purchaseOrderItemMapper.purchaseOrderItemList();
    }

    // 根据主键查询采购明细
    @Override
    public PurchaseOrderItem getPurchaseOrderItemById(Long purchaseOrderItemId) {
        return purchaseOrderItemMapper.getPurchaseOrderItemById(purchaseOrderItemId);
    }

    // 新增采购明细
    @Override
    public void insertPurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) {
        purchaseOrderItemMapper.insertPurchaseOrderItem(purchaseOrderItem);
    }

    // 修改采购明细
    @Override
    public void updatePurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) {
        purchaseOrderItemMapper.updatePurchaseOrderItem(purchaseOrderItem);
    }

    // 删除采购明细
    @Override
    public void deletePurchaseOrderItem(Long purchaseOrderItemId) {
        purchaseOrderItemMapper.deletePurchaseOrderItem(purchaseOrderItemId);
    }

}
