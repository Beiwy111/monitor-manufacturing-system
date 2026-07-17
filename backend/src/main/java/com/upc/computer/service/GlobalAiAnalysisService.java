package com.upc.computer.service;

import com.upc.computer.config.DeepseekJsonClient;
import com.upc.computer.config.FinanceDeepseekProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** 管理员全局 AI 分析：跨系统、订单、生产、质量、供应链、设备、售后和财务汇总。 */
@Service
public class GlobalAiAnalysisService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SYSTEM_PROMPT = """
            你是电脑显示器制造 MES 的企业运营分析总监。只能依据用户提供的结构化汇总证据分析，禁止虚构订单、产量、客户、金额、故障或根因。
            输入同时包含“当前状态快照”和“所选周期内活动”，分析时必须区分二者。数值为0或缺失时应写明数据不足，不得自行补造。
            需要识别订单、计划、生产、质量、采购、库存、设备、售后、财务之间的跨模块影响链，并给出可执行建议。AI 不得直接替代审批或执行任何业务动作。

            只返回一个 JSON 对象，不得输出 Markdown、代码块或额外说明。字段必须为：
            summary(string，160至300字总体结论)、rating(string，只能是A、B、C、D)、riskLevel(string，只能是LOW、MEDIUM、HIGH)、
            highlights(array，2至5项，每项含title、evidence、impact)，
            agents(array，必须依次包含8项：系统安全分析员、订单履约分析员、生产运营分析员、质量分析员、供应链分析员、设备分析员、售后分析员、财务分析员；
              每项含key、name、status、summary、findings，key依次为system、order、production、quality、supply、equipment、aftersales、finance，
              status只能是NORMAL、WARNING、RISK，findings为1至4条字符串)，
            crossModuleInsights(array，1至5项，每项含title、chain、evidence、recommendation，chain描述模块影响链)，
            risks(array，0至8项，每项含level、module、title、evidence、suggestion，level只能是HIGH、MEDIUM、LOW)，
            actions(array，3至8项，每项含priority、department、action、basis，priority只能是P1、P2、P3；department必须使用订单管理部、计划部、生产部、质量部、采购部、仓储部、设备维护部、售后部、财务部、系统管理部之一)，
            disclaimer(string，说明报告为AI辅助分析且最终结论需相关负责人复核)。
            评级必须按以下统一标准并采用证据支持下的较保守等级：
            A（优秀）：核心业务指标健康、跨模块链路顺畅，且没有高风险事项；
            B（良好）：整体运营稳定，仅有少量可控关注项，可按计划改进；
            C（关注）：多个模块存在风险或联动阻塞，需要明确责任人并限期整改；
            D（风险）：交付、质量、供应、设备、系统安全或现金流存在高风险，需要立即升级处理。
            riskLevel为HIGH时rating不得为A或B，riskLevel为MEDIUM时rating不得为A。
            evidence、basis 必须引用输入中的具体数字或状态分布，不得仅给出空泛判断。
            """;

    private final RoleWorkbenchDashboardService dashboardService;
    private final MesSnapshotService snapshotService;
    private final DeepseekJsonClient deepseekClient;
    private final FinanceDeepseekProperties deepseekProperties;

    public GlobalAiAnalysisService(RoleWorkbenchDashboardService dashboardService,
                                   MesSnapshotService snapshotService,
                                   DeepseekJsonClient deepseekClient,
                                   FinanceDeepseekProperties deepseekProperties) {
        this.dashboardService = dashboardService;
        this.snapshotService = snapshotService;
        this.deepseekClient = deepseekClient;
        this.deepseekProperties = deepseekProperties;
    }

    public Map<String, Object> generate(Long userId, int requestedDays) {
        int days = Math.max(7, Math.min(requestedDays, 30));
        Map<String, Object> evidence = buildEvidence(userId, days);
        Map<String, Object> raw = deepseekClient.generateJson(SYSTEM_PROMPT, evidence);
        return normalizeResult(raw, evidence, days);
    }

    private Map<String, Object> buildEvidence(Long userId, int days) {
        Map<String, Object> dashboard = dashboardService.buildDashboard("admin", userId, days, null);
        Map<String, Object> snapshot = snapshotService.buildSnapshot();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);

        List<Map<String, Object>> users = rows(snapshot, "sysUsers");
        List<Map<String, Object>> roles = rows(snapshot, "sysRoles");
        List<Map<String, Object>> logs = rows(snapshot, "operationLogs");
        List<Map<String, Object>> orders = rows(snapshot, "orders");
        List<Map<String, Object>> plans = rows(snapshot, "plans");
        List<Map<String, Object>> workOrders = rows(snapshot, "workOrders");
        List<Map<String, Object>> dispatches = rows(snapshot, "dispatches");
        List<Map<String, Object>> reports = rows(snapshot, "workReports");
        List<Map<String, Object>> inspections = rows(snapshot, "inspections");
        List<Map<String, Object>> defects = rows(snapshot, "defects");
        List<Map<String, Object>> purchaseOrders = rows(snapshot, "purchaseOrders");
        List<Map<String, Object>> inventory = rows(snapshot, "inventory");
        List<Map<String, Object>> purchaseDemands = rows(snapshot, "purchaseDemands");
        List<Map<String, Object>> deliveries = rows(snapshot, "deliveries");
        List<Map<String, Object>> equipment = rows(snapshot, "equipment");
        List<Map<String, Object>> alarms = rows(snapshot, "alarms");
        List<Map<String, Object>> maintenance = rows(snapshot, "maintenanceRecords");
        List<Map<String, Object>> aftersales = rows(snapshot, "aftersaleCases");
        List<Map<String, Object>> settlements = rows(snapshot, "costSettlements");

        Map<String, Object> modules = new LinkedHashMap<>();
        modules.put("system", mapOf(
                "userCount", users.size(),
                "enabledUserCount", countEqual(users, "status", "启用"),
                "roleCount", roles.size(),
                "recentOperationCount", recentCount(logs, startDate),
                "roleDistribution", distribution(users, "roleName", 12),
                "operationModules", distribution(logs, "module", 10),
                "workbenchMetrics", listValue(dashboard.get("metrics"), 12)
        ));
        modules.put("order", mapOf(
                "orderCount", orders.size(),
                "recentOrderCount", recentCount(orders, startDate),
                "totalOrderAmount", sum(orders, "amount"),
                "statusDistribution", distribution(orders, "status", 12)
        ));
        modules.put("planning", mapOf(
                "planCount", plans.size(),
                "recentPlanCount", recentCount(plans, startDate),
                "plannedQuantity", sum(plans, "quantity"),
                "statusDistribution", distribution(plans, "status", 12)
        ));

        BigDecimal plannedQty = sum(workOrders, "quantity");
        BigDecimal completedQty = sum(workOrders, "completedQty");
        BigDecimal reportQty = sum(reports, "reportQty");
        BigDecimal reportQualified = sum(reports, "qualifiedQty");
        BigDecimal reportUnqualified = sum(reports, "unqualifiedQty");
        modules.put("production", mapOf(
                "workOrderCount", workOrders.size(),
                "workOrderStatus", distribution(workOrders, "status", 12),
                "plannedQuantity", plannedQty,
                "completedQuantity", completedQty,
                "completionRate", percent(completedQty, plannedQty),
                "dispatchCount", dispatches.size(),
                "dispatchStatus", distribution(dispatches, "status", 12),
                "recentReportCount", recentCount(reports, startDate),
                "reportedQuantity", reportQty,
                "reportedQualifiedQuantity", reportQualified,
                "reportedUnqualifiedQuantity", reportUnqualified,
                "reportedPassRate", percent(reportQualified, reportQualified.add(reportUnqualified))
        ));

        BigDecimal sampleQty = sum(inspections, "sampleQty");
        BigDecimal qualifiedQty = sum(inspections, "qualifiedQty");
        BigDecimal unqualifiedQty = sum(inspections, "unqualifiedQty");
        modules.put("quality", mapOf(
                "inspectionCount", inspections.size(),
                "recentInspectionCount", recentCount(inspections, startDate),
                "inspectionStatus", distribution(inspections, "status", 12),
                "sampleQuantity", sampleQty,
                "qualifiedQuantity", qualifiedQty,
                "unqualifiedQuantity", unqualifiedQty,
                "passRate", percent(qualifiedQty, qualifiedQty.add(unqualifiedQty)),
                "defectCount", defects.size(),
                "defectQuantity", sum(defects, "quantity"),
                "defectStatus", distribution(defects, "status", 12),
                "defectSeverity", distribution(defects, "severity", 8)
        ));

        modules.put("supply", mapOf(
                "purchaseOrderCount", purchaseOrders.size(),
                "recentPurchaseOrderCount", recentCount(purchaseOrders, startDate),
                "purchaseAmount", sum(purchaseOrders, "totalAmount"),
                "purchaseStatus", distribution(purchaseOrders, "status", 12),
                "inventoryRecordCount", inventory.size(),
                "inventoryStatus", distribution(inventory, "status", 10),
                "lowStockMaterialCount", purchaseDemands.size(),
                "deliveryCount", deliveries.size(),
                "deliveryStatus", distribution(deliveries, "status", 10)
        ));
        modules.put("equipment", mapOf(
                "equipmentCount", equipment.size(),
                "equipmentStatus", distribution(equipment, "status", 10),
                "alarmCount", alarms.size(),
                "recentAlarmCount", recentCount(alarms, startDate),
                "alarmStatus", distribution(alarms, "status", 10),
                "alarmLevel", distribution(alarms, "level", 8),
                "openAlarmCount", countNotAny(alarms, "statusCode", List.of("CLOSED", "RESOLVED")),
                "maintenanceCount", maintenance.size(),
                "recentMaintenanceCount", recentCount(maintenance, startDate)
        ));
        modules.put("aftersales", mapOf(
                "caseCount", aftersales.size(),
                "recentCaseCount", recentCount(aftersales, startDate),
                "caseStatus", distribution(aftersales, "status", 10),
                "openCaseCount", countNotContains(aftersales, "status", List.of("关闭", "完成", "结案"))
        ));
        modules.put("finance", mapOf(
                "settlementCount", settlements.size(),
                "recentSettlementCount", recentCount(settlements, startDate),
                "settlementStatus", distribution(settlements, "status", 10),
                "totalCost", sum(settlements, "totalCost"),
                "materialCost", sum(settlements, "materialCost"),
                "laborCost", sum(settlements, "laborCost"),
                "equipmentCost", sum(settlements, "equipmentCost"),
                "qualityCost", sum(settlements, "qualityCost")
        ));

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("analysisType", "MES_GLOBAL");
        evidence.put("period", Map.of(
                "days", days,
                "startDate", startDate.toString(),
                "endDate", endDate.toString()
        ));
        evidence.put("dataScope", "各模块当前状态快照 + 所选周期内新增或活动记录");
        evidence.put("modules", modules);
        return evidence;
    }

    private Map<String, Object> normalizeResult(Map<String, Object> raw,
                                                Map<String, Object> evidence,
                                                int days) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", "AI 全局经营分析报告");
        result.put("summary", fallback(string(raw.get("summary")), "当前 MES 全局数据已完成分析，请结合各模块证据进行人工复核。"));
        AiAnalysisRating.Assessment assessment = AiAnalysisRating.global(evidence);
        result.put("rating", assessment.rating().code());
        result.put("ratingName", assessment.rating().name());
        result.put("ratingDescription", assessment.rating().description());
        result.put("score", assessment.score()); // 旧客户端兼容；数值同样由固定证据规则产生
        result.put("riskLevel", assessment.riskLevel());
        result.put("highlights", objectList(raw.get("highlights"), 5));
        result.put("agents", normalizeAgents(raw.get("agents")));
        result.put("crossModuleInsights", objectList(raw.get("crossModuleInsights"), 5));
        result.put("risks", objectList(raw.get("risks"), 8));
        result.put("actions", objectList(raw.get("actions"), 8));
        result.put("disclaimer", fallback(string(raw.get("disclaimer")), "本报告由 AI 基于当前 MES 汇总数据生成，仅供辅助决策，最终结论需由各模块负责人复核。"));
        result.put("periodDays", days);
        result.put("period", evidence.get("period"));
        result.put("generatedAt", LocalDateTime.now().format(TIME_FORMAT));
        result.put("model", deepseekProperties.getModel());
        result.put("evidenceCards", buildEvidenceCards(evidence));
        return result;
    }

    private List<Map<String, Object>> normalizeAgents(Object value) {
        List<Map<String, Object>> rawAgents = objectList(value, 12);
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        for (Map<String, Object> agent : rawAgents) {
            byKey.put(string(agent.get("key")).toLowerCase(Locale.ROOT), agent);
        }
        String[][] definitions = {
                {"system", "系统安全分析员"}, {"order", "订单履约分析员"},
                {"production", "生产运营分析员"}, {"quality", "质量分析员"},
                {"supply", "供应链分析员"}, {"equipment", "设备分析员"},
                {"aftersales", "售后分析员"}, {"finance", "财务分析员"}
        };
        List<Map<String, Object>> result = new ArrayList<>();
        for (String[] definition : definitions) {
            Map<String, Object> source = byKey.getOrDefault(definition[0], Map.of());
            Map<String, Object> agent = new LinkedHashMap<>();
            agent.put("key", definition[0]);
            agent.put("name", fallback(string(source.get("name")), definition[1]));
            agent.put("status", enumValue(source.get("status"), List.of("NORMAL", "WARNING", "RISK"), "NORMAL"));
            agent.put("summary", fallback(string(source.get("summary")), "该模块暂未识别到足够数据，请人工复核。"));
            agent.put("findings", stringList(source.get("findings"), 4));
            result.add(agent);
        }
        return result;
    }

    private List<Map<String, Object>> buildEvidenceCards(Map<String, Object> evidence) {
        Map<String, Object> modules = mapValue(evidence.get("modules"));
        Map<String, Object> system = mapValue(modules.get("system"));
        Map<String, Object> order = mapValue(modules.get("order"));
        Map<String, Object> planning = mapValue(modules.get("planning"));
        Map<String, Object> production = mapValue(modules.get("production"));
        Map<String, Object> quality = mapValue(modules.get("quality"));
        Map<String, Object> supply = mapValue(modules.get("supply"));
        Map<String, Object> equipment = mapValue(modules.get("equipment"));
        Map<String, Object> aftersales = mapValue(modules.get("aftersales"));
        Map<String, Object> finance = mapValue(modules.get("finance"));
        return List.of(
                evidenceCard("系统与安全", "系统管理", List.of(
                        metric("用户", system.get("userCount"), "人"), metric("启用用户", system.get("enabledUserCount"), "人"),
                        metric("角色", system.get("roleCount"), "个"), metric("周期操作", system.get("recentOperationCount"), "次")
                )),
                evidenceCard("订单与生产", "订单/计划/生产", List.of(
                        metric("订单", order.get("orderCount"), "单"), metric("计划", planning.get("planCount"), "单"),
                        metric("工单", production.get("workOrderCount"), "单"), metric("完成率", production.get("completionRate"), "%")
                )),
                evidenceCard("质量与售后", "质量/售后", List.of(
                        metric("质检", quality.get("inspectionCount"), "次"), metric("合格率", quality.get("passRate"), "%"),
                        metric("不良品", quality.get("defectCount"), "条"), metric("未结售后", aftersales.get("openCaseCount"), "单")
                )),
                evidenceCard("供应与库存", "采购/仓储", List.of(
                        metric("采购单", supply.get("purchaseOrderCount"), "单"), metric("采购额", supply.get("purchaseAmount"), "元"),
                        metric("库存预警", supply.get("lowStockMaterialCount"), "项"), metric("发货", supply.get("deliveryCount"), "单")
                )),
                evidenceCard("设备与报警", "设备管理", List.of(
                        metric("设备", equipment.get("equipmentCount"), "台"), metric("报警", equipment.get("alarmCount"), "条"),
                        metric("未关闭报警", equipment.get("openAlarmCount"), "条"), metric("维保", equipment.get("maintenanceCount"), "次")
                )),
                evidenceCard("成本结算", "财务管理", List.of(
                        metric("结算单", finance.get("settlementCount"), "单"), metric("总成本", finance.get("totalCost"), "元"),
                        metric("材料成本", finance.get("materialCost"), "元"), metric("质量成本", finance.get("qualityCost"), "元")
                ))
        );
    }

    private Map<String, Object> evidenceCard(String title, String source, List<Map<String, Object>> metrics) {
        return Map.of("title", title, "source", source, "metrics", metrics);
    }

    private Map<String, Object> metric(String label, Object value, String unit) {
        return mapOf("label", label, "value", value == null ? 0 : value, "unit", unit);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(Map<String, Object> snapshot, String key) {
        Object value = snapshot.get(key);
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance).map(v -> (Map<String, Object>) v).toList();
    }

    private Map<String, Long> distribution(List<Map<String, Object>> list, String key, int limit) {
        return list.stream()
                .map(row -> string(row.get(key)))
                .filter(value -> !value.isBlank())
                .collect(Collectors.groupingBy(value -> value, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private BigDecimal sum(List<Map<String, Object>> list, String key) {
        return list.stream().map(row -> decimal(row.get(key))).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private long countEqual(List<Map<String, Object>> list, String key, String expected) {
        return list.stream().filter(row -> expected.equals(string(row.get(key)))).count();
    }

    private long countNotAny(List<Map<String, Object>> list, String key, List<String> closed) {
        return list.stream().filter(row -> !closed.contains(string(row.get(key)).toUpperCase(Locale.ROOT))).count();
    }

    private long countNotContains(List<Map<String, Object>> list, String key, List<String> closedWords) {
        return list.stream().filter(row -> {
            String value = string(row.get(key));
            return closedWords.stream().noneMatch(value::contains);
        }).count();
    }

    private long recentCount(List<Map<String, Object>> list, LocalDate startDate) {
        return list.stream().filter(row -> {
            String value = string(row.get("createdAt"));
            if (value.length() < 10) return false;
            try {
                return !LocalDate.parse(value.substring(0, 10)).isBefore(startDate);
            } catch (Exception ignored) {
                return false;
            }
        }).count();
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < entries.length; i += 2) {
            result.put(String.valueOf(entries[i]), entries[i + 1]);
        }
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
