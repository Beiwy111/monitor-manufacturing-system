package com.upc.computer.service.impl;

import com.upc.computer.service.OrderService;
import com.upc.computer.entity.CustomerOrder;
import com.upc.computer.mapper.CustomerOrderMapper;
import com.upc.computer.entity.CustomerOrderItem;
import com.upc.computer.mapper.CustomerOrderItemMapper;
import com.upc.computer.entity.DeliveryOrder;
import com.upc.computer.mapper.DeliveryOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private CustomerOrderMapper customerOrderMapper;

    @Autowired
    private CustomerOrderItemMapper customerOrderItemMapper;

    @Autowired
    private DeliveryOrderMapper deliveryOrderMapper;

    // 查询所有客户订单
    @Override
    public ArrayList<CustomerOrder> customerOrderList() {
        return customerOrderMapper.customerOrderList();
    }

    // 根据主键查询客户订单
    @Override
    public CustomerOrder getCustomerOrderById(Long orderId) {
        return customerOrderMapper.getCustomerOrderById(orderId);
    }

    // 新增客户订单
    @Override
    public void insertCustomerOrder(CustomerOrder customerOrder) {
        customerOrderMapper.insertCustomerOrder(customerOrder);
    }

    // 修改客户订单
    @Override
    public void updateCustomerOrder(CustomerOrder customerOrder) {
        customerOrderMapper.updateCustomerOrder(customerOrder);
    }

    // 删除客户订单
    @Override
    public void deleteCustomerOrder(Long orderId) {
        customerOrderMapper.deleteCustomerOrder(orderId);
    }

    // 查询所有订单明细
    @Override
    public ArrayList<CustomerOrderItem> orderItemList() {
        return customerOrderItemMapper.orderItemList();
    }

    // 根据主键查询订单明细
    @Override
    public CustomerOrderItem getOrderItemById(Long orderItemId) {
        return customerOrderItemMapper.getOrderItemById(orderItemId);
    }

    // 新增订单明细
    @Override
    public void insertOrderItem(CustomerOrderItem orderItem) {
        customerOrderItemMapper.insertOrderItem(orderItem);
    }

    // 修改订单明细
    @Override
    public void updateOrderItem(CustomerOrderItem orderItem) {
        customerOrderItemMapper.updateOrderItem(orderItem);
    }

    // 删除订单明细
    @Override
    public void deleteOrderItem(Long orderItemId) {
        customerOrderItemMapper.deleteOrderItem(orderItemId);
    }

    // 查询所有发货单
    @Override
    public ArrayList<DeliveryOrder> deliveryList() {
        return deliveryOrderMapper.deliveryList();
    }

    // 根据主键查询发货单
    @Override
    public DeliveryOrder getDeliveryById(Long deliveryId) {
        return deliveryOrderMapper.getDeliveryById(deliveryId);
    }

    // 新增发货单
    @Override
    public void insertDelivery(DeliveryOrder delivery) {
        deliveryOrderMapper.insertDelivery(delivery);
    }

    // 修改发货单
    @Override
    public void updateDelivery(DeliveryOrder delivery) {
        deliveryOrderMapper.updateDelivery(delivery);
    }

    // 删除发货单
    @Override
    public void deleteDelivery(Long deliveryId) {
        deliveryOrderMapper.deleteDelivery(deliveryId);
    }

}
