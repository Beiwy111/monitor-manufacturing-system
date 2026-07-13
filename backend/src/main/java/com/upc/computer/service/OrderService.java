package com.upc.computer.service;

import com.upc.computer.entity.CustomerOrder;
import com.upc.computer.entity.CustomerOrderItem;
import com.upc.computer.entity.DeliveryOrder;
import java.util.ArrayList;

public interface OrderService {

    public ArrayList<CustomerOrder> customerOrderList();

    public CustomerOrder getCustomerOrderById(Long orderId);

    public void insertCustomerOrder(CustomerOrder customerOrder);

    public void updateCustomerOrder(CustomerOrder customerOrder);

    public void deleteCustomerOrder(Long orderId);

    public ArrayList<CustomerOrderItem> orderItemList();

    public CustomerOrderItem getOrderItemById(Long orderItemId);

    public void insertOrderItem(CustomerOrderItem orderItem);

    public void updateOrderItem(CustomerOrderItem orderItem);

    public void deleteOrderItem(Long orderItemId);

    public ArrayList<DeliveryOrder> deliveryList();

    public DeliveryOrder getDeliveryById(Long deliveryId);

    public void insertDelivery(DeliveryOrder delivery);

    public void updateDelivery(DeliveryOrder delivery);

    public void deleteDelivery(Long deliveryId);

}
