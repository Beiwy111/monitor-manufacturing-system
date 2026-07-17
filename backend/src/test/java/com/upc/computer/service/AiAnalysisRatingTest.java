package com.upc.computer.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiAnalysisRatingTest {

    @Test
    void appliesRiskFloorToModelRating() {
        assertEquals("B", AiAnalysisRating.resolve("A", "MEDIUM", null).code());
        assertEquals("C", AiAnalysisRating.resolve("A", "HIGH", null).code());
        assertEquals("D", AiAnalysisRating.resolve("D", "LOW", null).code());
    }

    @Test
    void mapsLegacyScoreWhenModelDoesNotReturnRating() {
        assertEquals("A", AiAnalysisRating.resolve(null, "LOW", 91).code());
        assertEquals("B", AiAnalysisRating.resolve(null, "LOW", 73).code());
        assertEquals("C", AiAnalysisRating.resolve(null, "MEDIUM", 61).code());
        assertEquals("D", AiAnalysisRating.resolve(null, "HIGH", 42).code());
    }

    @Test
    void financeRatingIsDeterministicForTheSameEvidence() {
        Map<String, Object> evidence = Map.of(
                "summary", Map.of(
                        "totalIncome", new BigDecimal("100000"),
                        "totalCost", new BigDecimal("70000"),
                        "totalProfit", new BigDecimal("30000"),
                        "avgMargin", new BigDecimal("30")
                ),
                "receivableRisk", Map.of(
                        "customerCount", 0,
                        "totalDebt", BigDecimal.ZERO,
                        "overdueAmount", BigDecimal.ZERO,
                        "highRiskCustomerCount", 0
                ),
                "paymentStatus", Map.of(
                        "recordCount", 0,
                        "receivableAmount", BigDecimal.ZERO,
                        "unreceivedAmount", BigDecimal.ZERO
                ),
                "lossAndLowMarginOrders", List.of()
        );

        AiAnalysisRating.Assessment first = AiAnalysisRating.finance(evidence);
        AiAnalysisRating.Assessment second = AiAnalysisRating.finance(evidence);

        assertEquals(first, second);
        assertEquals("B", first.rating().code());
        assertEquals(88, first.score());
    }

    @Test
    void globalRatingUsesOnlyStructuredEvidence() {
        Map<String, Object> evidence = Map.of("modules", Map.of(
                "system", Map.of("userCount", 10, "enabledUserCount", 10),
                "order", Map.of("orderCount", 5),
                "planning", Map.of("planCount", 5),
                "production", Map.of(
                        "workOrderCount", 5,
                        "completionRate", new BigDecimal("95"),
                        "reportedQuantity", new BigDecimal("100"),
                        "reportedUnqualifiedQuantity", BigDecimal.ZERO,
                        "completedQuantity", new BigDecimal("95")
                ),
                "quality", Map.of("inspectionCount", 5, "passRate", new BigDecimal("98"), "defectCount", 0),
                "supply", Map.of("purchaseOrderCount", 3, "lowStockMaterialCount", 0),
                "equipment", Map.of("equipmentCount", 4, "openAlarmCount", 0),
                "aftersales", Map.of("caseCount", 1, "openCaseCount", 0),
                "finance", Map.of("settlementCount", 2)
        ));

        AiAnalysisRating.Assessment assessment = AiAnalysisRating.global(evidence);

        assertEquals("A", assessment.rating().code());
        assertEquals("LOW", assessment.riskLevel());
        assertEquals(100, assessment.score());
    }
}
