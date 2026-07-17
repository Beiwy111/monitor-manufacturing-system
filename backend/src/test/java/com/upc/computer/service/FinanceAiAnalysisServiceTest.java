package com.upc.computer.service;

import com.upc.computer.config.DeepseekJsonClient;
import com.upc.computer.config.FinanceDeepseekProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceAiAnalysisServiceTest {

    @Mock private FinanceService financeService;
    @Mock private RoleWorkbenchDashboardService dashboardService;
    @Mock private DeepseekJsonClient deepseekClient;
    @Mock private FinanceDeepseekProperties properties;

    private FinanceAiAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new FinanceAiAnalysisService(financeService, dashboardService, deepseekClient, properties);

        when(financeService.financeScreen(7)).thenReturn(Map.of(
                "summary", Map.of(
                        "totalIncome", new BigDecimal("100000"),
                        "totalCost", new BigDecimal("70000"),
                        "totalProfit", new BigDecimal("30000"),
                        "avgMargin", new BigDecimal("30")
                ),
                "costStructure", List.of(Map.of("name", "材料成本", "value", 50000)),
                "dayLabels", List.of("7/10", "7/11"),
                "incomeTrend", List.of(50000, 50000),
                "costTrend", List.of(30000, 40000),
                "profitTrend", List.of(20000, 10000),
                "marginTrend", List.of(40, 20),
                "collectionTrend", List.of(50000, 50000)
        ));
        when(financeService.profitAnalysis()).thenReturn(Map.of(
                "orderRank", List.of(),
                "customerRank", List.of(),
                "alerts", List.of()
        ));
        when(financeService.listReceivables()).thenReturn(List.of());
        when(financeService.listPayments()).thenReturn(List.of());
        when(dashboardService.buildDashboard("cost", 9L, 7, null)).thenReturn(Map.of("metrics", List.of()));
        when(properties.getModel()).thenReturn("deepseek-v4-flash");
    }

    @Test
    void generateClampsPeriodAndNormalizesAgentSlots() {
        when(deepseekClient.generateJson(anyString(), any())).thenReturn(Map.of(
                "summary", "测试财务结论",
                "rating", "A",
                "riskLevel", "MEDIUM",
                "agents", List.of(Map.of(
                        "key", "revenue",
                        "name", "收入分析员",
                        "status", "NORMAL",
                        "summary", "收入稳定",
                        "findings", List.of("收入为100000元")
                )),
                "risks", List.of(),
                "actions", List.of(Map.of(
                        "priority", "P2",
                        "department", "财务部",
                        "action", "复核数据",
                        "basis", "测试证据"
                ))
        ));

        Map<String, Object> result = service.generate(9L, 2);

        assertEquals(7, result.get("periodDays"));
        assertEquals("B", result.get("rating"));
        assertEquals("良好", result.get("ratingName"));
        assertEquals("deepseek-v4-flash", result.get("model"));
        assertEquals(5, ((List<?>) result.get("agents")).size());
        assertEquals(3, ((List<?>) result.get("evidenceCards")).size());
        assertNotNull(result.get("generatedAt"));
        verify(financeService).financeScreen(7);
        verify(dashboardService).buildDashboard("cost", 9L, 7, null);
        verify(deepseekClient).generateJson(anyString(), any());
    }
}
