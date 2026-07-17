package com.upc.computer.service;

import com.upc.computer.config.DeepseekJsonClient;
import com.upc.computer.config.FinanceDeepseekProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalAiAnalysisServiceTest {

    @Mock private RoleWorkbenchDashboardService dashboardService;
    @Mock private MesSnapshotService snapshotService;
    @Mock private DeepseekJsonClient deepseekClient;
    @Mock private FinanceDeepseekProperties properties;

    private GlobalAiAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new GlobalAiAnalysisService(dashboardService, snapshotService, deepseekClient, properties);
        when(dashboardService.buildDashboard("admin", 1L, 7, null))
                .thenReturn(Map.of("metrics", List.of()));
        when(snapshotService.buildSnapshot()).thenReturn(Map.of());
        when(properties.getModel()).thenReturn("deepseek-v4-flash");
    }

    @Test
    void generateUsesSharedIndependentConfigAndNormalizesGlobalReport() {
        when(deepseekClient.generateJson(anyString(), any())).thenReturn(Map.of(
                "summary", "测试全局结论",
                "rating", "B",
                "riskLevel", "MEDIUM",
                "agents", List.of(Map.of(
                        "key", "system",
                        "name", "系统安全分析员",
                        "status", "NORMAL",
                        "summary", "系统运行正常",
                        "findings", List.of("当前未识别到异常")
                )),
                "crossModuleInsights", List.of(Map.of(
                        "title", "测试联动",
                        "chain", "订单 → 生产",
                        "evidence", "测试证据",
                        "recommendation", "人工复核"
                )),
                "risks", List.of(),
                "actions", List.of()
        ));

        Map<String, Object> result = service.generate(1L, 2);

        assertEquals(7, result.get("periodDays"));
        assertEquals("C", result.get("rating"));
        assertEquals("关注", result.get("ratingName"));
        assertEquals("deepseek-v4-flash", result.get("model"));
        assertEquals(8, ((List<?>) result.get("agents")).size());
        assertEquals(6, ((List<?>) result.get("evidenceCards")).size());
        assertEquals(1, ((List<?>) result.get("crossModuleInsights")).size());
        assertNotNull(result.get("generatedAt"));
        verify(dashboardService).buildDashboard("admin", 1L, 7, null);
        verify(snapshotService).buildSnapshot();
        verify(deepseekClient).generateJson(anyString(), any());
    }
}
