package com.upc.computer.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 管理员全局分析与财务分析共用的稳定评级规范。 */
public final class AiAnalysisRating {

    private AiAnalysisRating() {
    }

    public record Rating(String code, String name, String description) {
    }

    /** 固定规则产生的评级结果；相同证据输入必然得到相同结果。 */
    public record Assessment(Rating rating, String riskLevel, int score) {
    }

    /**
     * 财务评级采用可复算的固定扣分规则，不采用大模型的随机评级结果。
     * 主要维度：盈利、毛利、逾期、高风险客户、未回款、亏损/低毛利订单和数据完整度。
     */
    public static Assessment finance(Map<String, Object> evidence) {
        Map<String, Object> summary = map(evidence.get("summary"));
        Map<String, Object> receivable = map(evidence.get("receivableRisk"));
        Map<String, Object> payment = map(evidence.get("paymentStatus"));
        List<?> lossOrders = list(evidence.get("lossAndLowMarginOrders"));

        BigDecimal income = decimal(summary.get("totalIncome"));
        BigDecimal cost = decimal(summary.get("totalCost"));
        BigDecimal profit = decimal(summary.get("totalProfit"));
        BigDecimal margin = decimal(summary.get("avgMargin"));
        BigDecimal debt = decimal(receivable.get("totalDebt"));
        BigDecimal overdue = decimal(receivable.get("overdueAmount"));
        BigDecimal paymentReceivable = decimal(payment.get("receivableAmount"));
        BigDecimal unreceived = decimal(payment.get("unreceivedAmount"));
        int paymentRecords = integer(payment.get("recordCount"), 0);
        int receivableCustomers = integer(receivable.get("customerCount"), 0);
        int highRiskCustomers = integer(receivable.get("highRiskCustomerCount"), 0);

        boolean noBusinessData = income.signum() == 0 && cost.signum() == 0
                && paymentRecords == 0 && receivableCustomers == 0 && lossOrders.isEmpty();
        if (noBusinessData) return assessment(60);

        int score = 100;
        if (profit.signum() < 0) score -= 35;
        else if (income.signum() > 0 && profit.signum() == 0) score -= 15;

        if (income.signum() > 0) {
            if (margin.signum() < 0) score -= 25;
            else if (margin.compareTo(new BigDecimal("10")) < 0) score -= 20;
            else if (margin.compareTo(new BigDecimal("20")) < 0) score -= 10;
        }

        BigDecimal overdueBase = debt.signum() > 0 ? debt : paymentReceivable;
        BigDecimal overdueRate = percent(overdue, overdueBase);
        if (overdueRate.compareTo(new BigDecimal("30")) >= 0) score -= 25;
        else if (overdueRate.compareTo(new BigDecimal("15")) >= 0) score -= 15;
        else if (overdue.signum() > 0) score -= 6;

        if (highRiskCustomers >= 3) score -= 20;
        else if (highRiskCustomers > 0) score -= 10;

        BigDecimal unreceivedRate = percent(unreceived, paymentReceivable);
        if (unreceivedRate.compareTo(new BigDecimal("50")) >= 0) score -= 15;
        else if (unreceivedRate.compareTo(new BigDecimal("25")) >= 0) score -= 8;

        if (lossOrders.size() >= 3) score -= 20;
        else if (!lossOrders.isEmpty()) score -= 10;

        // 已有经营数据但尚无回款记录，评级最多为 B，避免数据不完整时误评为优秀。
        if (income.signum() > 0 && paymentRecords == 0) score -= 12;
        return assessment(score);
    }

    /**
     * 全局评级采用跨模块固定规则，关注订单到计划、计划到工单、生产、质量、供应、设备和售后。
     */
    public static Assessment global(Map<String, Object> evidence) {
        Map<String, Object> modules = map(evidence.get("modules"));
        Map<String, Object> system = map(modules.get("system"));
        Map<String, Object> order = map(modules.get("order"));
        Map<String, Object> planning = map(modules.get("planning"));
        Map<String, Object> production = map(modules.get("production"));
        Map<String, Object> quality = map(modules.get("quality"));
        Map<String, Object> supply = map(modules.get("supply"));
        Map<String, Object> equipment = map(modules.get("equipment"));
        Map<String, Object> aftersales = map(modules.get("aftersales"));
        Map<String, Object> finance = map(modules.get("finance"));

        int userCount = integer(system.get("userCount"), 0);
        int enabledUsers = integer(system.get("enabledUserCount"), 0);
        int orderCount = integer(order.get("orderCount"), 0);
        int planCount = integer(planning.get("planCount"), 0);
        int workOrderCount = integer(production.get("workOrderCount"), 0);
        int inspectionCount = integer(quality.get("inspectionCount"), 0);
        int purchaseOrderCount = integer(supply.get("purchaseOrderCount"), 0);
        int equipmentCount = integer(equipment.get("equipmentCount"), 0);
        int caseCount = integer(aftersales.get("caseCount"), 0);
        int settlementCount = integer(finance.get("settlementCount"), 0);

        int evidenceCount = userCount + orderCount + planCount + workOrderCount + inspectionCount
                + purchaseOrderCount + equipmentCount + caseCount + settlementCount;
        if (evidenceCount == 0) return assessment(60);

        int score = 100;
        if (userCount > 0 && enabledUsers < userCount) {
            BigDecimal disabledRate = percent(BigDecimal.valueOf(userCount - enabledUsers), BigDecimal.valueOf(userCount));
            score -= disabledRate.compareTo(new BigDecimal("20")) >= 0 ? 10 : 4;
        }
        if (orderCount > 0 && planCount == 0) score -= 15;
        if (planCount > 0 && workOrderCount == 0) score -= 12;

        BigDecimal completionRate = decimal(production.get("completionRate"));
        if (workOrderCount > 0) {
            if (completionRate.compareTo(new BigDecimal("50")) < 0) score -= 18;
            else if (completionRate.compareTo(new BigDecimal("80")) < 0) score -= 10;
        }

        BigDecimal reported = decimal(production.get("reportedQuantity"));
        BigDecimal unqualified = decimal(production.get("reportedUnqualifiedQuantity"));
        BigDecimal reportDefectRate = percent(unqualified, reported);
        if (reportDefectRate.compareTo(new BigDecimal("5")) >= 0) score -= 15;
        else if (unqualified.signum() > 0) score -= 7;

        BigDecimal passRate = decimal(quality.get("passRate"));
        if (inspectionCount > 0) {
            if (passRate.compareTo(new BigDecimal("90")) < 0) score -= 18;
            else if (passRate.compareTo(new BigDecimal("95")) < 0) score -= 10;
        } else if (decimal(production.get("completedQuantity")).signum() > 0) {
            score -= 12;
        }

        int defectCount = integer(quality.get("defectCount"), 0);
        if (defectCount >= 5) score -= 10;
        else if (defectCount > 0) score -= 5;

        int lowStockCount = integer(supply.get("lowStockMaterialCount"), 0);
        if (lowStockCount >= 10) score -= 15;
        else if (lowStockCount >= 3) score -= 10;
        else if (lowStockCount > 0) score -= 5;

        int openAlarmCount = integer(equipment.get("openAlarmCount"), 0);
        if (openAlarmCount >= 5) score -= 15;
        else if (openAlarmCount > 0) score -= 7;
        if (workOrderCount > 0 && equipmentCount == 0) score -= 10;

        int openCaseCount = integer(aftersales.get("openCaseCount"), 0);
        if (openCaseCount >= 5) score -= 10;
        else if (openCaseCount > 0) score -= 5;
        return assessment(score);
    }

    /**
     * A 优秀：核心指标健康、无高风险；B 良好：总体稳定、存在可控关注项；
     * C 关注：多项风险需要限期整改；D 风险：关键链路存在高风险，需要立即升级处理。
     */
    public static Rating resolve(Object rawRating, String riskLevel, Object legacyScore) {
        String code = string(rawRating).toUpperCase(Locale.ROOT);
        if (!"A".equals(code) && !"B".equals(code) && !"C".equals(code) && !"D".equals(code)) {
            int score = integer(legacyScore, 60);
            code = score >= 85 ? "A" : score >= 70 ? "B" : score >= 55 ? "C" : "D";
        }

        String risk = string(riskLevel).toUpperCase(Locale.ROOT);
        // 风险等级是评级下限，避免出现“高风险但评级优秀”的矛盾报告。
        if ("HIGH".equals(risk) && ("A".equals(code) || "B".equals(code))) code = "C";
        if ("MEDIUM".equals(risk) && "A".equals(code)) code = "B";

        return switch (code) {
            case "A" -> new Rating("A", "优秀", "核心指标健康，未识别到高风险事项，维持当前管控并持续监测。");
            case "B" -> new Rating("B", "良好", "整体运行稳定，存在少量可控关注项，应按计划完成改进。");
            case "D" -> new Rating("D", "风险", "关键业务链路存在高风险，应立即升级处理并跟踪闭环。");
            default -> new Rating("C", "关注", "存在多项经营或财务风险，需要明确责任人并限期整改。");
        };
    }

    private static Assessment assessment(int rawScore) {
        int score = Math.max(0, Math.min(100, rawScore));
        String code = score >= 90 ? "A" : score >= 75 ? "B" : score >= 55 ? "C" : "D";
        String riskLevel = score >= 85 ? "LOW" : score >= 60 ? "MEDIUM" : "HIGH";
        return new Assessment(rating(code), riskLevel, score);
    }

    private static Rating rating(String code) {
        return switch (code) {
            case "A" -> new Rating("A", "优秀", "固定证据规则评级：核心指标健康，未识别到高风险事项。");
            case "B" -> new Rating("B", "良好", "固定证据规则评级：整体运行稳定，存在少量可控关注项。");
            case "D" -> new Rating("D", "风险", "固定证据规则评级：关键业务链路存在高风险，应立即升级处理。");
            default -> new Rating("C", "关注", "固定证据规则评级：存在多项风险，需要明确责任人并限期整改。");
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> source ? (Map<String, Object>) source : Map.of();
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> source ? source : List.of();
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        try {
            return value instanceof BigDecimal number ? number : new BigDecimal(String.valueOf(value));
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() <= 0) return BigDecimal.ZERO;
        return numerator.max(BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private static int integer(Object value, int fallback) {
        try {
            return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
