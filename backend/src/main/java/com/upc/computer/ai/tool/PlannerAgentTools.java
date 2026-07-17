package com.upc.computer.ai.tool;

import com.upc.computer.service.MesPlannerSchedulingService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** 计划员智能排产预览与分析工具。 */
@Component
public class PlannerAgentTools {

    private final MesPlannerSchedulingService schedulingService;

    public PlannerAgentTools(MesPlannerSchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    @Tool(name = "planner_preview_scheduling", description = "复用计划员智能排产功能，分析指定订单的库存、BOM、产能、设备和工艺路线，仅返回排产预览")
    public Object previewScheduling(@ToolParam(description = "客户订单编号，例如 CO202607001") String orderNo) {
        return schedulingService.previewOrderContext(AgentToolSupport.requiredText(orderNo, "订单编号"));
    }

    @Tool(name = "planner_compare_scheduling_schemes", description = "复用计划员智能排产功能，对比交期优先、负载均衡和成本优先三种排产方案")
    public Object compareSchemes(
            @ToolParam(description = "客户订单编号") String orderNo,
            @ToolParam(description = "计划开始日期，格式 yyyy-MM-dd") String planStart,
            @ToolParam(description = "计划结束日期，格式 yyyy-MM-dd") String planEnd,
            @ToolParam(description = "计划生产数量，必须大于零") Integer plannedQty) {
        if (plannedQty == null || plannedQty <= 0) {
            throw new com.upc.computer.common.BusinessException("计划生产数量必须大于零");
        }
        return schedulingService.compareSchemes(
                AgentToolSupport.requiredText(orderNo, "订单编号"),
                AgentToolSupport.requiredDate(planStart, "计划开始日期"),
                AgentToolSupport.requiredDate(planEnd, "计划结束日期"),
                plannedQty);
    }

    @Tool(name = "planner_list_plan_schedules", description = "查询指定生产计划的工序排程")
    public Object listPlanSchedules(@ToolParam(description = "生产计划编号") String planNo) {
        return schedulingService.listPlanSchedules(AgentToolSupport.requiredText(planNo, "计划编号"));
    }

    @Tool(name = "planner_list_plan_history", description = "查询指定生产计划的历史版本和变更记录")
    public Object listPlanHistory(@ToolParam(description = "生产计划编号") String planNo) {
        return schedulingService.listPlanHistory(AgentToolSupport.requiredText(planNo, "计划编号"));
    }
}
