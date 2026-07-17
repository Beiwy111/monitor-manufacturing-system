package com.upc.computer.ai.tool;

import com.upc.computer.service.AfterSalesService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** 售后案例和根因协查只读工具。 */
@Component
public class AfterSalesAgentTools {

    private final AfterSalesService afterSalesService;

    public AfterSalesAgentTools(AfterSalesService afterSalesService) {
        this.afterSalesService = afterSalesService;
    }

    @Tool(name = "aftersales_case_kpi", description = "查询售后案例数量、状态和处理时效 KPI")
    public Object caseKpi() {
        return afterSalesService.caseKpi();
    }

    @Tool(name = "aftersales_list_cases", description = "查询售后案例、客户、问题类型和处理状态")
    public Object listCases() {
        return AgentToolSupport.limit(afterSalesService.listCaseViews(), 150);
    }

    @Tool(name = "aftersales_trace_case", description = "根据售后案例编号查询订单、质检、生产和处理追溯信息")
    public Object traceCase(@ToolParam(description = "售后案例编号") String caseNo) {
        return afterSalesService.getTraceDetail(
                AgentToolSupport.requiredText(caseNo, "售后案例编号"));
    }

    @Tool(name = "aftersales_rca_progress", description = "查询指定售后案例的跨部门根因协查进度")
    public Object rcaProgress(@ToolParam(description = "售后案例编号") String caseNo) {
        return afterSalesService.rcaTaskProgress(
                AgentToolSupport.requiredText(caseNo, "售后案例编号"));
    }

    @Tool(name = "aftersales_list_rca_tasks", description = "按部门查询售后根因协查任务，部门可不传")
    public Object listRcaTasks(
            @ToolParam(description = "部门名称或代码", required = false) String department) {
        return AgentToolSupport.limit(afterSalesService.listRcaTasks(department), 150);
    }
}
