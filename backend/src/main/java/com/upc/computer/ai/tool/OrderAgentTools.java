package com.upc.computer.ai.tool;

import com.upc.computer.service.OrderService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** 订单角色只读工具。 */
@Component
public class OrderAgentTools {

    private final OrderService orderService;

    public OrderAgentTools(OrderService orderService) {
        this.orderService = orderService;
    }

    @Tool(name = "order_list_customer_orders", description = "查询客户订单列表及订单当前状态")
    public Object listCustomerOrders() {
        return AgentToolSupport.limit(orderService.customerOrderList(), 100);
    }

    @Tool(name = "order_get_customer_order", description = "根据订单数据库主键查询客户订单详情")
    public Object getCustomerOrder(
            @ToolParam(description = "客户订单数据库主键") Long orderId) {
        return orderService.getCustomerOrderById(AgentToolSupport.requiredId(orderId, "订单ID"));
    }

    @Tool(name = "order_list_order_items", description = "查询客户订单明细列表")
    public Object listOrderItems() {
        return AgentToolSupport.limit(orderService.orderItemList(), 200);
    }

    @Tool(name = "order_list_deliveries", description = "查询发货单及交付状态")
    public Object listDeliveries() {
        return AgentToolSupport.limit(orderService.deliveryList(), 100);
    }
}
