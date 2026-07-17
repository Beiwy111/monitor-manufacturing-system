package com.upc.computer.service;

import com.upc.computer.config.DeepseekJsonClient;
import com.upc.computer.config.FinanceDeepseekProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 财务工作台 AI 分析：先由本地服务汇总可核验数据，再交给 DeepSeek 形成结论。
 */
@Service
public class FinanceAiAnalysisService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SYSTEM_PROMPT = """
            你是电脑显示器制造 MES 的高级财务分析助手。你只能依据用户提供的结构化证据进行分析，禁止虚构订单、金额、客户、趋势或原因。
            所有金额字段默认单位为人民币元；只有字段名或值中明确标注“万”时才使用万元。数据为0或缺失时必须明确写“当前数据不足”，不得补造。
            分析目标：判断收入、成本、利润、回款、应收和亏损订单风险，并给出可以由业务部门执行的建议。结论是辅助决策，不得直接替代财务审批。

            只返回一个 JSON 对象，不得输出 Markdown、代码块或额外说明。JSON 字段必须为：
            summary(string，120至240字总体结论)、rating(string，只能是A、B、C、D)、riskLevel(string，只能是LOW、MEDIUM、HIGH)、
            highlights(array，2至4项，每项含title、evidence、impact)，
            agents(array，必须依次包含5项：收入分析员、成本分析员、回款分析员、盈利分析员、风险控制员；每项含key、name、status、summary、findings，
              key依次为revenue、cost、collection、profit、risk，status只能是NORMAL、WARNING、RISK，findings为1至4条字符串)，
            risks(array，0至5项，每项含level、title、evidence、suggestion，level只能是HIGH、MEDIUM、LOW)，
            actions(array，2至6项，每项含priority、department、action、basis，priority只能是P1、P2、P3)，
            disclaimer(string，固定说明这是AI辅助分析且最终结论需财务人员复核)。
            评级必须按以下统一标准并采用证据支持下的较保守等级：
            A（优秀）：收入、利润、回款和成本结构整体健康，且没有高风险事项；
            B（良好）：总体稳定，仅有少量可控关注项，可按计划改进；
            C（关注）：应收、盈利、成本或结算存在多项风险，需要明确责任人并限期整改；
            D（风险）：现金流、亏损、逾期或关键财务链路存在高风险，需要立即升级处理。
            riskLevel为HIGH时rating不得为A或B，riskLevel为MEDIUM时rating不得为A。
            evidence、basis 等字段必须引用输入中的具体数字或记录，不得只写空泛判断。
            """;

    private final FinanceService financeService;
    private final RoleWorkbenchDashboardService dashboardService;
    private final DeepseekJsonClient deepseekClient;
    private final FinanceDeepseekProperties deepseekProperties;

    public FinanceAiAnalysisService(FinanceService financeService,
                                    RoleWorkbenchDashboardService dashboardService,
                                    DeepseekJsonClient deepseekClient,
                                    FinanceDeepseekProperties deepseekProperties) {
        this.financeService = financeService;
        this.dashboardService = dashboardService;
        this.deepseekClient = deepseekClient;
        this.deepseekProperties = deepseekProperties;
    }

    public Map<String, Object> generate(Long userId, int requestedDays) {
        int days = Math.max(7, Math.min(requestedDays, 30));
        Map<String, Object> evidence = buildEvidence(userId, days);
        Map<String, Object> rawResult = deepseekClient.generateJson(SYSTEM_PROMPT, evidence);
        return normalizeResult(rawResult, evidence, days);
    }

    private Map<String, Object> buildEvidence(Long userId, int days) {
        Map<String, Object> screen = financeService.financeScreen(days);
        Map<String, Object> profit = financeService.profitAnalysis();
        List<Map<String, Object>> receivables = financeService.listReceivables();
        List<Map<String, Object>> payments = financeService.listPayments();
        Map<String, Object> dashboard = dashboardService.buildDashboard("cost", userId, days, null);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("analysisType", "MES_FINANCE");
        evidence.put("period", Map.of(
                "days", days,
                "startDate", startDate.toString(),
                "endDate", endDate.toString()
        ));
        evidence.put("summary", mapValue(screen.get("summary")));
        evidence.put("costStructure", listValue(screen.get("costStructure"), 10));
        evidence.put("trends", Map.of(
                "labels", listValue(screen.get("dayLabels"), 30),
                "income", listValue(screen.get("incomeTrend"), 30),
                "cost", listValue(screen.get("costTrend"), 30),
                "profit", listValue(screen.get("profitTrend"), 30),
                "marginPercent", listValue(screen.get("marginTrend"), 30),
                "collection", listValue(screen.get("collectionTrend"), 30)
        ));
        evidence.put("profitRanking", listValue(profit.get("orderRank"), 8));
        evidence.put("customerProfitRanking", listValue(profit.get("customerRank"), 8));
        evidence.put("lossAndLowMarginOrders", listValue(profit.get("alerts"), 10));
        evidence.put("receivableRisk", summarizeReceivables(receivables));
        evidence.put("paymentStatus", summarizePayments(payments));
        evidence.put("workbenchMetrics", listValue(dashboard.get("metrics"), 12));
        return evidence;
    }

    private Map<String, Object> summarizeReceivables(List<Map<String, Object>> rows) {
        BigDecimal totalDebt = BigDecimal.ZERO;
        BigDecimal overdueAmount = BigDecimal.ZERO;
        int highRiskCount = 0;
        List<Map<String, Object>> topRisks = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            totalDebt = totalDebt.add(decimal(row.get("totalDebt")));
            overdueAmount = overdueAmount.add(decimal(row.get("overdueAmount")));
            if ("HIGH".equalsIgnoreCase(string(row.get("creditRisk")))) highRiskCount++;
        }
        rows.stream()
                .sorted((a, b) -> decimal(b.get("overdueAmount")).compareTo(decimal(a.get("overdueAmount"))))
                .limit(8)
                .forEach(row -> topRisks.add(pick(row,
                        "customerName", "totalDebt", "overdueAmount", "overdueDays", "creditRisk", "creditRiskCn")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customerCount", rows.size());
        result.put("totalDebt", totalDebt);
        result.put("overdueAmount", overdueAmount);
        result.put("highRiskCustomerCount", highRiskCount);
        result.put("topRisks", topRisks);
        return result;
    }

    private Map<String, Object> summarizePayments(List<Map<String, Object>> rows) {
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        BigDecimal receivableAmount = BigDecimal.ZERO;
        BigDecimal receivedAmount = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            String status = string(row.get("paymentStatus"));
            statusCounts.merge(status.isBlank() ? "UNKNOWN" : status, 1, Integer::sum);
            receivableAmount = receivableAmount.add(decimal(row.get("receivableAmount")));
            receivedAmount = receivedAmount.add(decimal(row.get("receivedAmount")));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recordCount", rows.size());
        result.put("statusCounts", statusCounts);
        result.put("receivableAmount", receivableAmount);
        result.put("receivedAmount", receivedAmount);
        result.put("unreceivedAmount", receivableAmount.subtract(receivedAmount).max(BigDecimal.ZERO));
        return result;
    }

    private Map<String, Object> normalizeResult(Map<String, Object> raw,
                                                Map<String, Object> evidence,
                                                int days) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", "AI 财务分析报告");
        result.put("summary", fallback(string(raw.get("summary")), "当前财务数据已完成分析，请结合证据库进行人工复核。"));
        AiAnalysisRating.Assessment assessment = AiAnalysisRating.finance(evidence);
        result.put("rating", assessment.rating().code());
        result.put("ratingName", assessment.rating().name());
        result.put("ratingDescription", assessment.rating().description());
        result.put("score", assessment.score()); // 旧客户端兼容；数值同样由固定证据规则产生
        result.put("riskLevel", assessment.riskLevel());
        result.put("highlights", objectList(raw.get("highlights"), 4));
        result.put("agents", normalizeAgents(raw.get("agents")));
        result.put("risks", objectList(raw.get("risks"), 5));
        result.put("actions", objectList(raw.get("actions"), 6));
        result.put("disclaimer", fallback(string(raw.get("disclaimer")), "本报告由 AI 基于当前 MES 数据生成，仅供辅助分析，最终结论需由财务人员复核。"));
        result.put("periodDays", days);
        result.put("period", evidence.get("period"));
        result.put("generatedAt", LocalDateTime.now().format(TIME_FORMAT));
        result.put("model", deepseekProperties.getModel());
        result.put("evidenceCards", buildEvidenceCards(evidence));
        return result;
    }

    private List<Map<String, Object>> normalizeAgents(Object value) {
        List<Map<String, Object>> rawAgents = objectList(value, 8);
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        for (Map<String, Object> agent : rawAgents) {
            byKey.put(string(agent.get("key")).toLowerCase(Locale.ROOT), agent);
        }
        String[][] definitions = {
                {"revenue", "收入分析员"}, {"cost", "成本分析员"}, {"collection", "回款分析员"},
                {"profit", "盈利分析员"}, {"risk", "风险控制员"}
        };
        List<Map<String, Object>> result = new ArrayList<>();
        for (String[] definition : definitions) {
            Map<String, Object> source = byKey.getOrDefault(definition[0], Map.of());
            Map<String, Object> agent = new LinkedHashMap<>();
            agent.put("key", definition[0]);
            agent.put("name", fallback(string(source.get("name")), definition[1]));
            agent.put("status", enumValue(source.get("status"), List.of("NORMAL", "WARNING", "RISK"), "NORMAL"));
            agent.put("summary", fallback(string(source.get("summary")), "该维度暂未识别到足够数据，请人工复核。"));
            agent.put("findings", stringList(source.get("findings"), 4));
            result.add(agent);
        }
        return result;
    }

    private List<Map<String, Object>> buildEvidenceCards(Map<String, Object> evidence) {
        Map<String, Object> summary = mapValue(evidence.get("summary"));
        Map<String, Object> receivable = mapValue(evidence.get("receivableRisk"));
        Map<String, Object> payment = mapValue(evidence.get("paymentStatus"));
        List<?> losses = listValue(evidence.get("lossAndLowMarginOrders"), 20);

        return List.of(
                evidenceCard("经营结果", "财务核算", List.of(
                        metric("收入", summary.get("totalIncome"), "元"),
                        metric("成本", summary.get("totalCost"), "元"),
                        metric("利润", summary.get("totalProfit"), "元"),
                        metric("毛利率", summary.get("avgMargin"), "%")
                )),
                evidenceCard("回款与应收", "回款记录", List.of(
                        metric("应收", payment.get("receivableAmount"), "元"),
                        metric("已收", payment.get("receivedAmount"), "元"),
                        metric("逾期", receivable.get("overdueAmount"), "元"),
                        metric("高风险客户", receivable.get("highRiskCustomerCount"), "家")
                )),
                evidenceCard("盈利风险", "订单利润分析", List.of(
                        metric("低毛利/亏损订单", losses.size(), "单"),
                        metric("应收客户", receivable.get("customerCount"), "家")
                ))
        );
    }

    private Map<String, Object> evidenceCard(String title, String source, List<Map<String, Object>> metrics) {
        return Map.of("title", title, "source", source, "metrics", metrics);
    }

    private Map<String, Object> metric(String label, Object value, String unit) {
        Map<String, Object> metric = new LinkedHashMap<>();
        metric.put("label", label);
        metric.put("value", value == null ? 0 : value);
        metric.put("unit", unit);
        return metric;
    }

    private Map<String, Object> pick(Map<String, Object> source, String... keys) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : keys) result.put(key, source.get(key));
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (!(value instanceof Map<?, ?> map)) return new LinkedHashMap<>();
        return new LinkedHashMap<>((Map<String, Object>) map);
    }

    private List<?> listValue(Object value, int limit) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().limit(limit).toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> objectList(Object value, int limit) {
        if (!(value instanceof List<?> list)) return new ArrayList<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) result.add(new LinkedHashMap<>((Map<String, Object>) map));
            if (result.size() >= limit) break;
        }
        return result;
    }

    private List<String> stringList(Object value, int limit) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().map(this::string).filter(s -> !s.isBlank()).limit(limit).toList();
    }

    private BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        try {
            return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    private String enumValue(Object value, List<String> allowed, String fallback) {
        String result = string(value).toUpperCase(Locale.ROOT);
        return allowed.contains(result) ? result : fallback;
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
