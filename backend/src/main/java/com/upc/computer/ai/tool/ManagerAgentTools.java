package com.upc.computer.ai.tool;

import com.upc.computer.service.MesDashboardService;
import com.upc.computer.service.MesDispatchRecommendService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** 生产主管看板和智能派工预览工具。 */
@Component
public class ManagerAgentTools {

    private final MesDashboardService dashboardService;
    private final MesDispatchRecommendService dispatchRecommendService;

    public ManagerAgentTools(MesDashboardService dashboardService,
                             MesDispatchRecommendService dispatchRecommendService) {
        this.dashboardService = dashboardService;
        this.dispatchRecommendService = dispatchRecommendService;
    }

    @Tool(name = "manager_production_dashboard", description = "查询生产主管大屏的实时完整快照")
    public Object productionDashboard() {
        return dashboardService.getSnapshot();
    }

    @Tool(name = "manager_production_kpi", description = "查询生产主管实时生产 KPI")
    public Object productionKpi() {
        return dashboardService.getKpi();
    }

    @Tool(name = "manager_preview_all_smart_dispatch", description = "复用主管智能派工功能，为所有待派工计划生成操作员和设备推荐，仅返回预览，不创建派工")
    public Object previewAllSmartDispatch() {
        return dispatchRecommendService.generateAllRecommendations();
    }

    @Tool(name = "manager_preview_smart_dispatch", description = "复用主管智能派工功能，为指定生产计划生成操作员和设备推荐，仅返回预览，不创建派工")
    public Object previewSmartDispatch(@ToolParam(description = "生产计划编号") String planNo) {
        return dispatchRecommendService.generateRecommendations(
                AgentToolSupport.requiredText(planNo, "计划编号"));
    }

    @Tool(name = "manager_load_dispatch_context", description = "查询指定生产计划的智能派工上下文")
    public Object loadDispatchContext(@ToolParam(description = "生产计划编号") String planNo) {
        return dispatchRecommendService.loadPlanContext(
                AgentToolSupport.requiredText(planNo, "计划编号"));
    }

    @Tool(name = "manager_list_open_dispatches", description = "查询当前未完成的派工任务")
    public Object listOpenDispatches() {
        return AgentToolSupport.limit(dispatchRecommendService.openDispatchList(), 150);
    }
}
