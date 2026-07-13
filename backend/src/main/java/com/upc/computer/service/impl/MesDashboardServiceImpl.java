package com.upc.computer.service.impl;

import com.upc.computer.entity.*;
import com.upc.computer.mapper.*;
import com.upc.computer.service.MesDashboardSeedService;
import com.upc.computer.service.MesDashboardService;
import com.upc.computer.service.MesPlannerAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MesDashboardServiceImpl implements MesDashboardService {

    private static final Map<String, String> STATION_STATUS_LABEL = Map.of(
            "RUNNING", "运行中",
            "WAIT_MATERIAL", "待料",
            "FAULT", "故障",
            "QC_ABNORMAL", "质检异常",
            "COMPLETED", "已完成",
            "IDLE", "空闲"
    );

    private static final Map<String, String> EQUIP_STATUS_LABEL = Map.of(
            "RUNNING", "运行中",
            "IDLE", "待机",
            "FAULT", "故障",
            "MAINTENANCE", "保养中"
    );

    @Autowired
    private MesDashboardSeedService seedService;
    @Autowired
    private ProdLineStationMapper stationMapper;
    @Autowired
    private ProdHourlyMetricMapper hourlyMetricMapper;
    @Autowired
    private ProdShiftCapacityMapper shiftCapacityMapper;
    @Autowired
    private ProdDowntimeReasonMapper downtimeReasonMapper;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private ProductionPlanItemMapper planItemMapper;
    @Autowired
    private ProductionPlanMapper planMapper;
    @Autowired
    private CustomerOrderMapper customerOrderMapper;
    @Autowired
    private EquipmentMapper equipmentMapper;
    @Autowired
    private AndonAlarmMapper alarmMapper;
    @Autowired
    private NonconformingProductMapper nonconformingMapper;
    @Autowired
    private MaterialMapper materialMapper;
    @Autowired
    private MesPlannerAgentService plannerAgentService;

    @Override
    public void ensureInitialized() {
        seedService.seedIfEmpty();
    }

    @Override
    public Map<String, Object> getSnapshot() {
        ensureInitialized();
        Map<String, Object> overview = plannerAgentService.buildProductionOverview();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workshops = (List<Map<String, Object>>) overview.getOrDefault("workshops", List.of());

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("refreshTime", overview.getOrDefault("refreshTime", LocalDateTime.now().toString().replace('T', ' ')));
        snapshot.put("productionOverview", overview);
        snapshot.put("workshops3d", workshops);
        snapshot.put("kpi", buildCompactKpi(overview));
        snapshot.put("equipment", flattenEquipment(workshops));
        snapshot.put("alarms", getAlarms().get("items"));
        snapshot.put("systemStatus", buildSystemStatus());
        snapshot.put("dataSource", "planner-agent-live");
        return snapshot;
    }

    private List<Map<String, Object>> buildCompactKpi(Map<String, Object> overview) {
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) overview.getOrDefault("summary", Map.of());
        return List.of(
                kpiItem("workshopCount", "车间", intVal(summary.get("workshopCount")), "个"),
                kpiItem("equipmentTotal", "设备", intVal(summary.get("equipmentTotal")), "台"),
                kpiItem("running", "运行中", intVal(summary.get("running")), "台"),
                kpiItem("fault", "故障", intVal(summary.get("fault")), "台"),
                kpiItem("availableOperators", "操作员", intVal(summary.get("availableOperators")), "人")
        );
    }

    private List<Map<String, Object>> flattenEquipment(List<Map<String, Object>> workshops) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> ws : workshops) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> machines = (List<Map<String, Object>>) ws.getOrDefault("machines", List.of());
            for (Map<String, Object> m : machines) {
                Map<String, Object> row = new LinkedHashMap<>(m);
                row.put("equipmentCode", m.get("code"));
                row.put("equipmentName", m.get("name"));
                row.put("workshop", ws.get("name"));
                row.put("workstation", m.get("workstation"));
                items.add(row);
            }
        }
        return items;
    }

    private static int intVal(Object v) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return v == null ? 0 : Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public Map<String, Object> getKpi() {
        ensureInitialized();
        LocalDate today = LocalDate.now();
        BigDecimal todayPlanned = sumTodayPlanned();
        BigDecimal todayActual = sumTodayActual();
        BigDecimal qualified = sumTodayQualified();
        BigDecimal unqualified = sumTodayUnqualified();
        int completionRate = ratePercent(todayActual, todayPlanned);
        int yieldRate = ratePercent(qualified, qualified.add(unqualified));
        int oee = computeOee();
        int alarmCount = countOpenAlarms();
        int delayRiskCount = countDelayRisks();

        List<Map<String, Object>> items = List.of(
                kpiItem("todayPlanned", "今日计划产量", todayPlanned.intValue(), "台"),
                kpiItem("todayActual", "今日实际产量", todayActual.intValue(), "台"),
                kpiItem("completionRate", "完成率", completionRate, "%"),
                kpiItem("yieldRate", "良品率", yieldRate, "%"),
                kpiItem("oee", "OEE", oee, "%"),
                kpiItem("alarmCount", "异常数量", alarmCount, "项"),
                kpiItem("delayRiskCount", "延期风险订单", delayRiskCount, "单")
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        return result;
    }

    @Override
    public Map<String, Object> getStations() {
        ensureInitialized();
        ArrayList<ProdLineStation> stations = stationMapper.listAll();
        List<Map<String, Object>> items = stations.stream().map(st -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stationCode", st.getStationCode());
            row.put("stationName", st.getStationName());
            row.put("lineCode", st.getLineCode());
            row.put("status", st.getStationStatus());
            row.put("statusLabel", STATION_STATUS_LABEL.getOrDefault(st.getStationStatus(), st.getStationStatus()));
            row.put("currentQty", st.getCurrentQty());
            row.put("throughputPerHour", st.getThroughputPerHour());
            row.put("workOrderNo", st.getWorkOrderNo());
            row.put("alarmFlag", st.getAlarmFlag() != null && st.getAlarmFlag() == 1);
            row.put("updatedAt", formatTime(st.getUpdatedAt()));
            return row;
        }).collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        return result;
    }

    @Override
    public Map<String, Object> getWorkOrderProgress() {
        ensureInitialized();
        LocalDate today = LocalDate.now();
        Map<Long, String> materialNames = materialMapper.materialList().stream()
                .collect(Collectors.toMap(Material::getMaterialId, Material::getMaterialName, (a, b) -> a));
        Map<Long, CustomerOrder> orderMap = customerOrderMapper.customerOrderList().stream()
                .collect(Collectors.toMap(CustomerOrder::getOrderId, o -> o, (a, b) -> a));
        Map<Long, ProductionPlan> planMap = planMapper.planList().stream()
                .collect(Collectors.toMap(ProductionPlan::getPlanId, p -> p, (a, b) -> a));

        List<Map<String, Object>> items = new ArrayList<>();
        List<Map<String, Object>> deliveryRisks = new ArrayList<>();
        for (WorkOrder wo : workOrderMapper.workOrderList()) {
            int progress = ratePercent(wo.getCompletedQuantity(), wo.getPlannedQuantity());
            ProductionPlan plan = planMap.get(wo.getPlanId());
            CustomerOrder order = plan != null ? orderMap.get(plan.getSourceOrderId()) : null;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("workOrderNo", wo.getWorkOrderNo());
            row.put("productName", materialNames.getOrDefault(wo.getMaterialId(), "显示器"));
            row.put("plannedQty", wo.getPlannedQuantity());
            row.put("completedQty", wo.getCompletedQuantity());
            row.put("progress", progress);
            row.put("status", wo.getStatus());
            row.put("statusLabel", workOrderStatusLabel(wo.getStatus()));
            row.put("plannedEndTime", formatTime(wo.getPlannedEndTime()));
            items.add(row);

            if (isDelayRisk(wo, order)) {
                Map<String, Object> risk = new LinkedHashMap<>();
                risk.put("workOrderNo", wo.getWorkOrderNo());
                risk.put("customerName", order != null ? order.getCustomerName() : "-");
                risk.put("requiredDate", order != null && order.getRequiredDeliveryDate() != null
                        ? order.getRequiredDeliveryDate().toString() : "-");
                risk.put("progress", progress);
                risk.put("riskLevel", progress < 50 ? "高" : "中");
                risk.put("reason", progress < 40 ? "进度严重滞后" : "交期临近完成率不足");
                deliveryRisks.add(risk);
            }
        }
        items.sort((a, b) -> Integer.compare((int) b.get("progress"), (int) a.get("progress")));

        List<Map<String, Object>> shiftCapacity = shiftCapacityMapper.listByDate(today).stream().map(cap -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("teamName", cap.getTeamName());
            row.put("leaderName", cap.getLeaderName());
            row.put("plannedQty", cap.getPlannedQty());
            row.put("actualQty", cap.getActualQty());
            row.put("yieldRate", cap.getYieldRate());
            row.put("rankNo", cap.getRankNo());
            return row;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("deliveryRisks", deliveryRisks);
        result.put("shiftCapacity", shiftCapacity);
        return result;
    }

    @Override
    public Map<String, Object> getWorkshops3d() {
        return plannerAgentService.buildWorkshops3dSnapshot();
    }

    @Override
    public Map<String, Object> getEquipmentStatus() {
        ensureInitialized();
        List<Map<String, Object>> items = equipmentMapper.equipmentList().stream().map(eq -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("equipmentCode", eq.getEquipmentCode());
            row.put("equipmentName", eq.getEquipmentName());
            row.put("workshop", eq.getWorkshop());
            row.put("workstation", eq.getWorkstation());
            row.put("status", eq.getStatus());
            row.put("statusLabel", EQUIP_STATUS_LABEL.getOrDefault(eq.getStatus(), eq.getStatus()));
            row.put("updatedAt", formatTime(eq.getUpdatedAt()));
            return row;
        }).collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        return result;
    }

    @Override
    public Map<String, Object> getAlarms() {
        ensureInitialized();
        List<Map<String, Object>> items = alarmMapper.alarmList().stream()
                .filter(a -> !"CLOSED".equals(a.getAlarmStatus()))
                .sorted(Comparator.comparing(AndonAlarm::getReportedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(20)
                .map(a -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("alarmNo", a.getAlarmNo());
                    row.put("alarmType", a.getAlarmType());
                    row.put("alarmLevel", a.getAlarmLevel());
                    row.put("description", a.getAlarmDescription());
                    row.put("status", a.getAlarmStatus());
                    row.put("workOrderId", a.getWorkOrderId());
                    row.put("reportedAt", formatTime(a.getReportedAt()));
                    return row;
                }).collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        return result;
    }

    @Override
    public Map<String, Object> getQualityIssues() {
        ensureInitialized();
        Map<String, Long> grouped = nonconformingMapper.nonconformingList().stream()
                .filter(n -> !"COMPLETED".equals(n.getHandleStatus()))
                .collect(Collectors.groupingBy(
                        n -> n.getDefectType() != null ? n.getDefectType() : "其他",
                        Collectors.summingLong(n -> n.getQuantity() != null ? n.getQuantity().longValue() : 0L)
                ));
        List<Map<String, Object>> items = grouped.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("defectType", e.getKey());
                    row.put("quantity", e.getValue());
                    return row;
                }).collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        return result;
    }

    @Override
    public Map<String, Object> getHourlyOutputTrend() {
        ensureInitialized();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> items = hourlyMetricMapper.listByDate(today).stream().map(m -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("hour", m.getStatHour());
            row.put("label", String.format("%02d:00", m.getStatHour()));
            row.put("planned", m.getPlannedOutput());
            row.put("actual", m.getActualOutput());
            row.put("alarmCount", m.getAlarmCount());
            return row;
        }).collect(Collectors.toList());
        List<Map<String, Object>> alarmTrend = hourlyMetricMapper.listByDate(today).stream().map(m -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("hour", m.getStatHour());
            row.put("label", String.format("%02d:00", m.getStatHour()));
            row.put("count", m.getAlarmCount());
            return row;
        }).collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("alarmTrend", alarmTrend);
        return result;
    }

    @Override
    public Map<String, Object> getYieldTrend() {
        ensureInitialized();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> items = hourlyMetricMapper.listByDate(today).stream().map(m -> {
            BigDecimal total = m.getQualifiedQty().add(m.getUnqualifiedQty());
            int yield = ratePercent(m.getQualifiedQty(), total);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("hour", m.getStatHour());
            row.put("label", String.format("%02d:00", m.getStatHour()));
            row.put("yieldRate", yield);
            row.put("qualified", m.getQualifiedQty());
            row.put("unqualified", m.getUnqualifiedQty());
            return row;
        }).collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        return result;
    }

    @Override
    public Map<String, Object> getDowntimeReasons() {
        ensureInitialized();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> items = downtimeReasonMapper.listByDate(today).stream().map(r -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("reasonCode", r.getReasonCode());
            row.put("reasonName", r.getReasonName());
            row.put("downtimeMinutes", r.getDowntimeMinutes());
            row.put("occurrenceCount", r.getOccurrenceCount());
            return row;
        }).collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        return result;
    }

    private BigDecimal sumTodayPlanned() {
        LocalDate today = LocalDate.now();
        return planItemMapper.planItemList().stream()
                .filter(item -> item.getPlannedStartDate() != null && !item.getPlannedStartDate().isAfter(today))
                .map(ProductionPlanItem::getPlannedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumTodayActual() {
        LocalDate today = LocalDate.now();
        BigDecimal hourly = hourlyMetricMapper.listByDate(today).stream()
                .map(ProdHourlyMetric::getActualOutput)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (hourly.compareTo(BigDecimal.ZERO) > 0) {
            return hourly;
        }
        return workOrderMapper.workOrderList().stream()
                .map(wo -> wo.getCompletedQuantity() == null ? BigDecimal.ZERO : wo.getCompletedQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumTodayQualified() {
        return workOrderMapper.workOrderList().stream()
                .map(wo -> wo.getQualifiedQuantity() == null ? BigDecimal.ZERO : wo.getQualifiedQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumTodayUnqualified() {
        return workOrderMapper.workOrderList().stream()
                .map(wo -> wo.getUnqualifiedQuantity() == null ? BigDecimal.ZERO : wo.getUnqualifiedQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int computeOee() {
        ArrayList<Equipment> equipment = equipmentMapper.equipmentList();
        if (equipment.isEmpty()) {
            return 0;
        }
        long fault = equipment.stream().filter(e -> "FAULT".equals(e.getStatus())).count();
        double availability = (equipment.size() - fault * 0.5) / equipment.size();

        BigDecimal planned = sumTodayPlanned().max(BigDecimal.valueOf(1));
        BigDecimal actual = sumTodayActual();
        double performance = Math.min(1.0, actual.divide(planned, 4, RoundingMode.HALF_UP).doubleValue());

        BigDecimal qualified = sumTodayQualified();
        BigDecimal total = qualified.add(sumTodayUnqualified());
        double quality = total.compareTo(BigDecimal.ZERO) > 0
                ? qualified.divide(total, 4, RoundingMode.HALF_UP).doubleValue()
                : 0.98;

        return (int) Math.round(availability * performance * quality * 100);
    }

    private int countOpenAlarms() {
        return (int) alarmMapper.alarmList().stream().filter(a -> !"CLOSED".equals(a.getAlarmStatus())).count();
    }

    private int countDelayRisks() {
        Map<Long, CustomerOrder> orderMap = customerOrderMapper.customerOrderList().stream()
                .collect(Collectors.toMap(CustomerOrder::getOrderId, o -> o, (a, b) -> a));
        Map<Long, ProductionPlan> planMap = planMapper.planList().stream()
                .collect(Collectors.toMap(ProductionPlan::getPlanId, p -> p, (a, b) -> a));
        int count = 0;
        for (WorkOrder wo : workOrderMapper.workOrderList()) {
            ProductionPlan plan = planMap.get(wo.getPlanId());
            CustomerOrder order = plan != null ? orderMap.get(plan.getSourceOrderId()) : null;
            if (isDelayRisk(wo, order)) {
                count++;
            }
        }
        return count;
    }

    private boolean isDelayRisk(WorkOrder wo, CustomerOrder order) {
        if (wo == null || !List.of("RUNNING", "RELEASED").contains(wo.getStatus())) {
            return false;
        }
        int progress = ratePercent(wo.getCompletedQuantity(), wo.getPlannedQuantity());
        if (wo.getPlannedEndTime() != null) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), wo.getPlannedEndTime().toLocalDate());
            if (daysLeft <= 3 && progress < 80) {
                return true;
            }
        }
        if (order != null && order.getRequiredDeliveryDate() != null) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), order.getRequiredDeliveryDate());
            return daysLeft <= 5 && progress < 70;
        }
        return progress < 35;
    }

    private String buildSystemStatus() {
        int alarms = countOpenAlarms();
        long faults = equipmentMapper.equipmentList().stream().filter(e -> "FAULT".equals(e.getStatus())).count();
        if (faults > 0 || alarms >= 3) {
            return "预警";
        }
        if (alarms > 0) {
            return "关注";
        }
        return "正常";
    }

    private static Map<String, Object> kpiItem(String key, String label, int value, String unit) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("label", label);
        item.put("value", value);
        item.put("unit", unit);
        return item;
    }

    private static int ratePercent(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return part != null && part.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
        }
        if (part == null) {
            return 0;
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue();
    }

    private static String workOrderStatusLabel(String status) {
        return switch (status) {
            case "RUNNING" -> "生产中";
            case "RELEASED" -> "已下达";
            case "COMPLETED" -> "已完成";
            case "DRAFT" -> "草稿";
            default -> status;
        };
    }

    private static String formatTime(LocalDateTime time) {
        if (time == null) {
            return "-";
        }
        String text = time.toString().replace('T', ' ');
        return text.length() > 19 ? text.substring(0, 19) : text;
    }
}
