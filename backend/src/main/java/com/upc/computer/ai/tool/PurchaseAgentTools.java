package com.upc.computer.ai.tool;

import com.upc.computer.service.PurchaseService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** 采购需求、订单和供应商只读工具。 */
@Component
public class PurchaseAgentTools {

    private final PurchaseService purchaseService;

    public PurchaseAgentTools(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @Tool(name = "purchase_list_orders", description = "查询采购订单、供应商、金额和到货状态")
    public Object listPurchaseOrders() {
        return AgentToolSupport.limit(purchaseService.purchaseOrderList(), 150);
    }

    @Tool(name = "purchase_list_requirements", description = "查询现有采购需求工作台数据，不重新计算或修改采购需求")
    public Object listRequirements() {
        return AgentToolSupport.limit(purchaseService.workbenchList(null, null, null, null), 200);
    }

    @Tool(name = "purchase_requirement_detail", description = "根据采购需求数据库主键查询需求、库存和供应商详情")
    public Object requirementDetail(@ToolParam(description = "采购需求数据库主键") Long requirementId) {
        return purchaseService.workbenchDetail(
                AgentToolSupport.requiredId(requirementId, "采购需求ID"));
    }

    @Tool(name = "purchase_requirements_by_order", description = "按客户订单汇总查询采购需求")
    public Object requirementsByOrder() {
        return AgentToolSupport.limit(purchaseService.workbenchByOrder(), 150);
    }

    @Tool(name = "purchase_order_demand_overview", description = "查询客户订单、成品库存、需生产量和缺料概况")
    public Object orderDemandOverview() {
        return AgentToolSupport.limit(purchaseService.listOrderDemandOverview(), 150);
    }

    @Tool(name = "purchase_list_suppliers", description = "查询可选供应商列表")
    public Object listSuppliers() {
        return AgentToolSupport.limit(purchaseService.getSupplierList(), 150);
    }
}
