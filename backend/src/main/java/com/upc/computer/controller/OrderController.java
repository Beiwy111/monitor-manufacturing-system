package com.upc.computer.controller;

import com.upc.computer.common.Result;
import com.upc.computer.service.OrderOcrService;
import com.upc.computer.service.OrderService;
import com.upc.computer.entity.CustomerOrder;
import com.upc.computer.entity.CustomerOrderItem;
import com.upc.computer.entity.DeliveryOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderOcrService orderOcrService;

    // 查询客户订单列表
    @RequestMapping("/customerOrder/list")
    public ArrayList<CustomerOrder> customerOrderList() {
        return orderService.customerOrderList();
    }

    // 根据主键查询客户订单
    @RequestMapping("/customerOrder/get")
    public CustomerOrder getCustomerOrderById(Long orderId) {
        return orderService.getCustomerOrderById(orderId);
    }

    // 新增客户订单
    @RequestMapping("/customerOrder/insert")
    public void insertCustomerOrder(CustomerOrder customerOrder) {
        orderService.insertCustomerOrder(customerOrder);
    }

    // 修改客户订单
    @RequestMapping("/customerOrder/update")
    public void updateCustomerOrder(CustomerOrder customerOrder) {
        orderService.updateCustomerOrder(customerOrder);
    }

    // 删除客户订单
    @RequestMapping("/customerOrder/delete")
    public void deleteCustomerOrder(Long orderId) {
        orderService.deleteCustomerOrder(orderId);
    }

    // 查询订单明细列表
    @RequestMapping("/orderItem/list")
    public ArrayList<CustomerOrderItem> orderItemList() {
        return orderService.orderItemList();
    }

    // 根据主键查询订单明细
    @RequestMapping("/orderItem/get")
    public CustomerOrderItem getOrderItemById(Long orderItemId) {
        return orderService.getOrderItemById(orderItemId);
    }

    // 新增订单明细
    @RequestMapping("/orderItem/insert")
    public void insertOrderItem(CustomerOrderItem orderItem) {
        orderService.insertOrderItem(orderItem);
    }

    // 修改订单明细
    @RequestMapping("/orderItem/update")
    public void updateOrderItem(CustomerOrderItem orderItem) {
        orderService.updateOrderItem(orderItem);
    }

    // 删除订单明细
    @RequestMapping("/orderItem/delete")
    public void deleteOrderItem(Long orderItemId) {
        orderService.deleteOrderItem(orderItemId);
    }

    // 查询发货单列表
    @RequestMapping("/delivery/list")
    public ArrayList<DeliveryOrder> deliveryList() {
        return orderService.deliveryList();
    }

    // 根据主键查询发货单
    @RequestMapping("/delivery/get")
    public DeliveryOrder getDeliveryById(Long deliveryId) {
        return orderService.getDeliveryById(deliveryId);
    }

    // 新增发货单
    @RequestMapping("/delivery/insert")
    public void insertDelivery(DeliveryOrder delivery) {
        orderService.insertDelivery(delivery);
    }

    // 修改发货单
    @RequestMapping("/delivery/update")
    public void updateDelivery(DeliveryOrder delivery) {
        orderService.updateDelivery(delivery);
    }

    // 删除发货单
    @RequestMapping("/delivery/delete")
    public void deleteDelivery(Long deliveryId) {
        orderService.deleteDelivery(deliveryId);
    }

    /** AI识图下单：上传微信聊天截图，提取订单字段 */
    @PostMapping("/ai/screenshot/parse")
    public Result<Map<String, Object>> parseWechatScreenshot(
            @RequestParam("file") MultipartFile file) {
        return Result.success(orderOcrService.recognizeUpload(file, null));
    }

    /** 兼容旧路径 */
    @PostMapping("/ai/ocr/parse")
    public Result<Map<String, Object>> parseOrderAiOcr(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "orderId", required = false) String orderId) {
        return Result.success(orderOcrService.recognizeUpload(file, orderId));
    }

}
