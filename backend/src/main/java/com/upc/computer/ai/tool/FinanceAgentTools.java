package com.upc.computer.ai.tool;

import com.upc.computer.service.CostService;
import com.upc.computer.service.FinanceService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** 成本和财务只读工具。 */
@Component
public class FinanceAgentTools {

    private final CostService costService;
    private final FinanceService financeService;

    public FinanceAgentTools(CostService costService, FinanceService financeService) {
        this.costService = costService;
        this.financeService = financeService;
    }

    @Tool(name = "cost_kpi", description = "查询成本结算 KPI")
    public Object costKpi() {
        return costService.costKpi();
    }

    @Tool(name = "cost_list_settlements", description = "查询成本结算单和确认导出状态")
    public Object listSettlements() {
        return AgentToolSupport.limit(costService.listSettlementViews(), 150);
    }

    @Tool(name = "cost_group_by_source", description = "按成本来源类型汇总成本金额")
    public Object groupBySource() {
        return costService.groupBySourceType();
    }

    @Tool(name = "finance_work_order_costs", description = "查询生产工单成本概览")
    public Object workOrderCosts() {
        return AgentToolSupport.limit(financeService.listWorkOrderCostOverview(), 150);
    }

    @Tool(name = "finance_cost_breakdown", description = "查询物料、人工和设备成本结构汇总")
    public Object costBreakdown() {
        return financeService.costBreakdownSummary();
    }

    @Tool(name = "finance_order_revenue", description = "查询客户订单收入")
    public Object orderRevenue() {
        return AgentToolSupport.limit(financeService.listOrderRevenue(), 150);
    }

    @Tool(name = "finance_receivables", description = "查询应收账款")
    public Object receivables() {
        return AgentToolSupport.limit(financeService.listReceivables(), 150);
    }

    @Tool(name = "finance_profit_analysis", description = "查询收入、成本和利润分析")
    public Object profitAnalysis() {
        return financeService.profitAnalysis();
    }

    @Tool(name = "finance_screen", description = "查询最近若干天的财务大屏数据")
    public Object financeScreen(
            @ToolParam(description = "统计天数，范围 1 到 365", required = false) Integer days) {
        int safeDays = days == null ? 30 : days;
        if (safeDays < 1 || safeDays > 365) {
            throw new com.upc.computer.common.BusinessException("统计天数必须在 1 到 365 之间");
        }
        return financeService.financeScreen(safeDays);
    }
}
