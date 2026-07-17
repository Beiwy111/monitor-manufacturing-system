package com.upc.computer.ai.tool;

import com.upc.computer.dto.LoginResponse;
import com.upc.computer.service.CustomerPortalService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** 与当前客户登录会话绑定的只读工具。 */
public class CustomerAgentTools {

    private final CustomerPortalService customerPortalService;
    private final LoginResponse session;

    public CustomerAgentTools(CustomerPortalService customerPortalService, LoginResponse session) {
        this.customerPortalService = customerPortalService;
        this.session = session;
    }

    @Tool(name = "customer_dashboard", description = "查询当前客户自己的门户首页和订单概况")
    public Object dashboard() {
        return customerPortalService.dashboard(session);
    }

    @Tool(name = "customer_list_my_orders", description = "查询当前客户自己的订单，不会返回其他客户数据")
    public Object listMyOrders() {
        return AgentToolSupport.limit(customerPortalService.listOrders(session), 100);
    }

    @Tool(name = "customer_my_order_detail", description = "根据订单数据库主键查询当前客户自己的订单详情")
    public Object myOrderDetail(@ToolParam(description = "订单数据库主键") Long orderId) {
        return customerPortalService.getOrderDetail(
                session, AgentToolSupport.requiredId(orderId, "订单ID"));
    }

    @Tool(name = "customer_list_products", description = "查询可以下单的显示器产品和规格")
    public Object listProducts() {
        return AgentToolSupport.limit(customerPortalService.listProducts(), 150);
    }

    @Tool(name = "customer_product_detail", description = "根据物料数据库主键查询显示器产品详情")
    public Object productDetail(@ToolParam(description = "产品物料数据库主键") Long materialId) {
        return customerPortalService.getProductDetail(
                AgentToolSupport.requiredId(materialId, "产品物料ID"));
    }

    @Tool(name = "customer_list_my_feedback", description = "查询当前客户自己提交的售后反馈")
    public Object listMyFeedback() {
        return AgentToolSupport.limit(customerPortalService.listFeedbacks(session), 100);
    }

    @Tool(name = "customer_my_profile", description = "查询当前客户自己的账户和企业资料")
    public Object myProfile() {
        return customerPortalService.getProfile(session);
    }
}
