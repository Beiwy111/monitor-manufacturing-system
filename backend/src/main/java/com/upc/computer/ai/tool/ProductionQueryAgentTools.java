package com.upc.computer.ai.tool;

import com.upc.computer.service.ProductionService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** 生产、工单、派工和报工只读工具。 */
@Component
public class ProductionQueryAgentTools {

    private final ProductionService productionService;

    public ProductionQueryAgentTools(ProductionService productionService) {
        this.productionService = productionService;
    }

    @Tool(name = "production_list_plans", description = "查询生产计划列表和计划状态")
    public Object listPlans() {
        return AgentToolSupport.limit(productionService.planList(), 100);
    }

    @Tool(name = "production_get_plan", description = "根据生产计划数据库主键查询计划详情")
    public Object getPlan(@ToolParam(description = "生产计划数据库主键") Long planId) {
        return productionService.getPlanById(AgentToolSupport.requiredId(planId, "计划ID"));
    }

    @Tool(name = "production_list_work_orders", description = "查询生产工单列表、数量和执行状态")
    public Object listWorkOrders() {
        return AgentToolSupport.limit(productionService.workOrderList(), 100);
    }

    @Tool(name = "production_list_dispatches", description = "查询派工任务、操作员、设备和执行状态")
    public Object listDispatches() {
        return AgentToolSupport.limit(productionService.dispatchList(), 150);
    }

    @Tool(name = "production_list_reports", description = "查询工序报工记录")
    public Object listReports() {
        return AgentToolSupport.limit(productionService.reportList(), 150);
    }

    @Tool(name = "production_list_progress", description = "查询生产进度记录")
    public Object listProgress() {
        return AgentToolSupport.limit(productionService.progressList(), 150);
    }

    @Tool(name = "production_list_process_routes", description = "查询显示器生产工艺路线")
    public Object listRoutes() {
        return AgentToolSupport.limit(productionService.routeList(), 100);
    }

    @Tool(name = "production_list_process_steps", description = "查询显示器生产工序配置")
    public Object listSteps() {
        return AgentToolSupport.limit(productionService.stepList(), 150);
    }
}
