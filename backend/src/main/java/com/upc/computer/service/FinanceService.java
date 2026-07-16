package com.upc.computer.service;

import java.util.List;
import java.util.Map;

public interface FinanceService {

    List<Map<String, Object>> listWorkOrderCostOverview();

    Map<String, Object> costBreakdownSummary();

    List<Map<String, Object>> listOrderRevenue();

    List<Map<String, Object>> listPayments();

    List<Map<String, Object>> listReceivables();

    Map<String, Object> profitAnalysis();

    Map<String, Object> financeScreen(int days);

    Map<String, Object> financeReport(String period);
}
