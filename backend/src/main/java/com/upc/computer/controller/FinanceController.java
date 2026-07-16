package com.upc.computer.controller;

import com.upc.computer.common.Result;
import com.upc.computer.service.FinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/finance")
public class FinanceController {

    @Autowired
    private FinanceService financeService;

    @GetMapping("/cost/overview")
    public Result<List<Map<String, Object>>> workOrderCostOverview() {
        return Result.success(financeService.listWorkOrderCostOverview());
    }

    @GetMapping("/cost/breakdown")
    public Result<Map<String, Object>> costBreakdown() {
        return Result.success(financeService.costBreakdownSummary());
    }

    @GetMapping("/revenue/orders")
    public Result<List<Map<String, Object>>> orderRevenue() {
        return Result.success(financeService.listOrderRevenue());
    }

    @GetMapping("/revenue/payments")
    public Result<List<Map<String, Object>>> payments() {
        return Result.success(financeService.listPayments());
    }

    @GetMapping("/revenue/receivables")
    public Result<List<Map<String, Object>>> receivables() {
        return Result.success(financeService.listReceivables());
    }

    @GetMapping("/revenue/profit-analysis")
    public Result<Map<String, Object>> profitAnalysis() {
        return Result.success(financeService.profitAnalysis());
    }

    @GetMapping("/screen")
    public Result<Map<String, Object>> screen(@RequestParam(defaultValue = "30") int days) {
        return Result.success(financeService.financeScreen(days));
    }

    @GetMapping("/report")
    public Result<Map<String, Object>> report(@RequestParam(required = false) String period) {
        return Result.success(financeService.financeReport(period));
    }
}
