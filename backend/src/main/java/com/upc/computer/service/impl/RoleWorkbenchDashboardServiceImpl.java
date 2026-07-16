package com.upc.computer.service.impl;

import com.upc.computer.common.BusinessException;
import com.upc.computer.entity.*;
import com.upc.computer.mapper.*;
import com.upc.computer.service.CostService;
import com.upc.computer.service.MesSnapshotService;
import com.upc.computer.service.MesStatusMapper;
import com.upc.computer.service.RoleWorkbenchDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoleWorkbenchDashboardServiceImpl implements RoleWorkbenchDashboardService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final List<String> ORDER_STATUS_ORDER = List.of(
            "PENDING", "PLAN_PENDING", "PLANNED", "PRODUCING", "SHIPPED", "REJECTED", "APPROVED"
    );
    private static final Map<String, String> ORDER_COLORS = Map.of(
            "PENDING", "#b89a4a", "PLAN_PENDING", "#c4854a", "PLANNED", "#4a6fa5",
            "PRODUCING", "#5a9a6a", "SHIPPED", "#4a9090", "REJECTED", "#c45a5a", "APPROVED", "#8a9199"
    );
    private static final Map<String, String> PLAN_COLORS = Map.of(
            "DRAFT", "#8a9199", "PUBLISHED", "#b89a4a", "SUBMITTED", "#4a6fa5",
            "RELEASED", "#4a6fa5", "EXECUTING", "#5a9a6a", "RUNNING", "#5a9a6a",
            "COMPLETED", "#4a9090", "CANCELLED", "#c45a5a", "ADJUSTED", "#7a6a9a"
    );
    private static final Map<String, String> DISPATCH_COLORS = Map.of(
            "ASSIGNED", "#b89a4a", "ACCEPTED", "#4a6fa5", "PRODUCING", "#5a9a6a",
            "RUNNING", "#5a9a6a", "QC_PENDING", "#c4854a", "COMPLETED", "#4a9090"
    );
    private static final Map<String, String> COST_STATUS_COLORS = Map.of(
            "DRAFT", "#b89a4a", "CONFIRMED", "#5a9a6a", "EXPORTED", "#4a6fa5"
    );

    // 工业大屏低饱和色
    private static final String C_BLUE = "#4a6fa5";
    private static final String C_CYAN = "#4a9090";
    private static final String C_ORANGE = "#c4854a";
    private static final String C_PURPLE = "#7a6a9a";
    private static final String C_RED = "#c45a5a";
    private static final String C_GREEN = "#5a9a6a";
    private static final String C_YELLOW = "#b89a4a";

    @Autowired private CustomerOrderMapper customerOrderMapper;
    @Autowired private CustomerOrderItemMapper customerOrderItemMapper;
    @Autowired private ProductionPlanMapper productionPlanMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private RoleMapper roleMapper;
    @Autowired private PermissionMapper permissionMapper;
    @Autowired private OperationLogMapper operationLogMapper;
    @Autowired private DispatchTaskMapper dispatchTaskMapper;
    @Autowired private WorkReportMapper workReportMapper;
    @Autowired private WorkOrderMapper workOrderMapper;
    @Autowired private AttendanceRecordMapper attendanceRecordMapper;
    @Autowired private CostService costService;
    @Autowired private MesSnapshotService mesSnapshotService;
    @Autowired private InventoryMapper inventoryMapper;
    @Autowired private MaterialMapper materialMapper;
    @Autowired private EquipmentMapper equipmentMapper;
    @Autowired private AndonAlarmMapper andonAlarmMapper;
    @Autowired private PurchaseOrderMapper purchaseOrderMapper;
    @Autowired private PurchaseRequirementMapper purchaseRequirementMapper;

    private static final Map<String, String> PURCHASE_REQ_COLORS = Map.of(
            "PENDING", "#c4854a", "ORDERED", "#4a6fa5"
    );
    private static final Map<String, String> PURCHASE_ORDER_COLORS = Map.of(
            "RELEASED", "#4a6fa5", "PARTIAL_ARRIVED", "#b89a4a",
            "ARRIVED", "#5a9a6a", "CANCELLED", "#8a9199"
    );
    public Map<String, Object> buildDashboard(String roleKey, Long userId, int days, String statusFilter) {
        int rangeDays = Math.max(7, Math.min(days, 30));
        return switch (roleKey) {
            case "order" -> buildOrderDashboard(rangeDays, statusFilter);
            case "admin" -> buildAdminDashboard(rangeDays);
            case "planner" -> buildPlannerDashboard(rangeDays);
            case "operator" -> buildOperatorDashboard(userId, rangeDays);
            case "cost" -> buildCostDashboard(rangeDays);
            case "purchase" -> buildPurchaseDashboard(userId, rangeDays);
            case "warehouse" -> buildWarehouseDashboard(userId, rangeDays);
            default -> throw new BusinessException("不支持的角色大屏: " + roleKey);
        };
    }

    // ── 订单审核员 ─────────────────────────────────────────────

    private Map<String, Object> buildOrderDashboard(int days, String statusFilter) {
        List<CustomerOrder> allOrders = customerOrderMapper.customerOrderList();
        List<CustomerOrder> orders = filterOrdersByStatus(allOrders, statusFilter);
        Map<Long, List<CustomerOrderItem>> itemsByOrder = customerOrderItemMapper.orderItemList().stream()
                .collect(Collectors.groupingBy(CustomerOrderItem::getOrderId));
        Map<Long, ProductionPlan> planByOrderId = productionPlanMapper.planList().stream()
                .filter(p -> p.getSourceOrderId() != null)
                .collect(Collectors.toMap(ProductionPlan::getSourceOrderId, p -> p, (a, b) -> a));
        Map<Long, WorkOrder> woByPlanId = workOrderMapper.workOrderList().stream()
                .filter(w -> w.getPlanId() != null)
                .collect(Collectors.toMap(WorkOrder::getPlanId, w -> w, (a, b) -> a));

        LocalDate today = LocalDate.now();
        LocalDate rangeStart = today.minusDays(days - 1L);

        long pending = orders.stream().filter(o -> "PENDING".equals(o.getAuditStatus())).count();
        long todayAudited = allOrders.stream()
                .filter(o -> o.getAuditAt() != null && o.getAuditAt().toLocalDate().equals(today)
                        && !"PENDING".equals(o.getAuditStatus()))
                .count();
        long approved = allOrders.stream().filter(o -> List.of("PLAN_PENDING", "APPROVED", "PLANNED", "PRODUCING", "SHIPPED").contains(o.getAuditStatus())).count();
        long rejected = allOrders.stream().filter(o -> "REJECTED".equals(o.getAuditStatus())).count();
        int passRate = (approved + rejected) > 0 ? (int) Math.round(approved * 100.0 / (approved + rejected)) : 0;
        long urgentDelivery = orders.stream()
                .filter(o -> o.getRequiredDeliveryDate() != null
                        && !o.getRequiredDeliveryDate().isBefore(today)
                        && o.getRequiredDeliveryDate().isBefore(today.plusDays(7))
                        && !List.of("SHIPPED", "REJECTED").contains(o.getAuditStatus()))
                .count();
        long stockRisk = countLowStockMaterials();

        List<Map<String, Object>> metrics = List.of(
                metricEx("pending", "待审核", pending, null, pending > 0 ? "warn" : "normal", null, "/order/audit"),
                metricEx("todayAudit", "今日审核", todayAudited, "单", "normal", "较昨日实时", "/order/audit"),
                metricEx("passRate", "通过率", passRate, "%", passRate >= 80 ? "normal" : "warn", null, "/order/audit"),
                metricEx("rejected", "已驳回", rejected, null, rejected > 0 ? "danger" : "normal", null, "/order/list"),
                metricEx("urgent", "交期风险", urgentDelivery, null, urgentDelivery > 0 ? "warn" : "normal", "7日内", "/order/track"),
                metricEx("stockRisk", "库存/BOM风险", stockRisk, "项", stockRisk > 0 ? "danger" : "normal", "低于安全库存", "/warehouse/capacity")
        );

        Map<String, Long> rejectReasons = allOrders.stream()
                .filter(o -> "REJECTED".equals(o.getAuditStatus()))
                .collect(Collectors.groupingBy(o -> {
                    String op = o.getAuditOpinion();
                    if (op == null || op.isBlank()) return "未填写原因";
                    return op.length() > 12 ? op.substring(0, 12) : op;
                }, Collectors.counting()));

        List<String> rejectCats = rejectReasons.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(5)
                .map(Map.Entry::getKey).toList();
        List<Integer> rejectVals = rejectCats.stream().map(c -> rejectReasons.get(c).intValue()).toList();

        List<String> riskCats = List.of("7日内交期", "14日内交期", "已逾期");
        List<Integer> riskVals = List.of(
                (int) urgentDelivery,
                (int) orders.stream().filter(o -> o.getRequiredDeliveryDate() != null
                        && o.getRequiredDeliveryDate().isBefore(today.plusDays(14))
                        && !o.getRequiredDeliveryDate().isBefore(today.plusDays(7))
                        && !List.of("SHIPPED", "REJECTED").contains(o.getAuditStatus())).count(),
                (int) orders.stream().filter(o -> o.getRequiredDeliveryDate() != null
                        && o.getRequiredDeliveryDate().isBefore(today)
                        && !List.of("SHIPPED", "REJECTED").contains(o.getAuditStatus())).count()
        );

        List<Map<String, Object>> stockItems = lowStockItems(5);

        List<String> dayLabels = new ArrayList<>();
        List<Integer> auditTrend = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            dayLabels.add(d.getMonthValue() + "/" + d.getDayOfMonth());
            auditTrend.add((int) allOrders.stream()
                    .filter(o -> o.getAuditAt() != null && o.getAuditAt().toLocalDate().equals(d)).count());
        }

        Map<String, Long> statusCount = new LinkedHashMap<>();
        for (CustomerOrder o : orders) {
            ProductionPlan plan = planByOrderId.get(o.getOrderId());
            WorkOrder wo = plan != null ? woByPlanId.get(plan.getPlanId()) : null;
            String dbKey = normalizeOrderKey(o.getAuditStatus(),
                    MesSnapshotService.deriveOrderStatus(o, plan, wo, "SHIPPED".equals(o.getAuditStatus())));
            statusCount.merge(dbKey, 1L, Long::sum);
        }
        List<Map<String, Object>> statusItems = chartItemsFromCount(statusCount, ORDER_STATUS_ORDER, ORDER_COLORS, MesStatusMapper::toOrderCn);

        List<Map<String, Object>> pendingRows = orders.stream()
                .filter(o -> "PENDING".equals(o.getAuditStatus()))
                .sorted(Comparator.comparing(CustomerOrder::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(o -> orderRow(o, itemsByOrder.get(o.getOrderId())))
                .toList();

        Map<String, Object> result = baseResult("order", "订单管理工作台", metrics);
        result.put("statusOptions", orderStatusOptions());
        result.put("panels", List.of(
                panel("statusDonut", "订单状态概览", 3, "donut", "/order/list", null, statusItems, orders.size() + "单"),
                panelHBar("rejectRank", "驳回原因排行", 3, "/order/audit", rejectCats, rejectVals),
                panelHBar("deliveryRisk", "交期风险分布", 3, "/order/track", riskCats, riskVals),
                panelHBar("stockRisk", "库存/BOM 预警", 3, "/warehouse/capacity", stockItems),
                panelLine("auditTrend", "审核量趋势", 8, "/order/audit", dayLabels,
                        List.of(lineSeries("审核单", auditTrend, C_BLUE)), true),
                panel("passDonut", "审核通过率", 4, "donut", "/order/audit", null,
                        List.of(chartItem("通过", (int) approved, C_GREEN), chartItem("驳回", (int) rejected, C_RED)),
                        passRate + "%")
        ));
        result.put("tables", List.of(
                table("pending", "待处理订单", "/order/audit", "下钻审核", orderColumns(), pendingRows, 168)
        ));
        return result;
    }

    // ── 系统管理员 ─────────────────────────────────────────────

    private Map<String, Object> buildAdminDashboard(int days) {
        List<User> users = userMapper.userList();
        List<Role> roles = roleMapper.roleList();
        List<OperationLog> logs = operationLogMapper.operationLogList();
        Map<Long, Role> roleById = roles.stream().collect(Collectors.toMap(Role::getRoleId, r -> r, (a, b) -> a));

        long enabledUsers = users.stream().filter(u -> u.getStatus() != null && u.getStatus() == 1).count();
        int permTotal = roles.stream().mapToInt(r -> permissionMapper.permissionListByRoleId(r.getRoleId()).size()).sum();
        LocalDateTime onlineSince = LocalDateTime.now().minusMinutes(30);
        long onlineUsers = logs.stream().filter(l -> l.getOperatedAt() != null && l.getOperatedAt().isAfter(onlineSince))
                .map(OperationLog::getUserId).filter(Objects::nonNull).distinct().count();
        long logsToday = logs.stream().filter(l -> l.getOperatedAt() != null && l.getOperatedAt().toLocalDate().equals(LocalDate.now())).count();
        long loginFail = logs.stream().filter(l -> l.getOperationType() != null && l.getOperationType().contains("登录")
                && l.getResultStatus() != null && !"SUCCESS".equalsIgnoreCase(l.getResultStatus())).count();

        List<Map<String, Object>> metrics = List.of(
                metricEx("users", "用户总数", users.size(), null, "normal", null, "/system/user"),
                metricEx("roles", "角色数", roles.size(), null, "normal", null, "/system/role"),
                metricEx("online", "在线用户", onlineUsers, null, "normal", "30分钟内活跃", "/system/user"),
                metricEx("perms", "权限绑定", permTotal, null, "normal", null, "/system/permission"),
                metricEx("logsToday", "今日操作", logsToday, null, "normal", null, "/system/log"),
                metricEx("loginFail", "异常登录", loginFail, null, loginFail > 0 ? "danger" : "normal", null, "/system/log")
        );

        Map<String, Long> usersByRole = new LinkedHashMap<>();
        for (User u : users) {
            Role r = roleById.get(u.getRoleId());
            usersByRole.merge(r != null ? r.getRoleName() : "未分配", 1L, Long::sum);
        }
        List<String> roleNames = usersByRole.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey).limit(6).toList();
        List<Integer> roleCounts = roleNames.stream().map(n -> usersByRole.get(n).intValue()).toList();

        LocalDate today = LocalDate.now();
        List<String> dayLabels = new ArrayList<>();
        List<Integer> dayCounts = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            dayLabels.add(d.getMonthValue() + "/" + d.getDayOfMonth());
            dayCounts.add((int) logs.stream().filter(l -> l.getOperatedAt() != null && l.getOperatedAt().toLocalDate().equals(d)).count());
        }

        Map<String, Long> moduleCount = logs.stream().filter(l -> l.getModuleName() != null)
                .collect(Collectors.groupingBy(OperationLog::getModuleName, Collectors.counting()));
        List<Map<String, Object>> moduleItems = moduleCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(5)
                .map(e -> chartItem(e.getKey(), e.getValue().intValue(), pickColor(0))).toList();

        List<Map<String, Object>> healthList = List.of(
                statusRow("MES 接口", "正常", "normal", "200"),
                statusRow("数据库", "正常", "normal", "连接池 OK"),
                statusRow("Redis 缓存", logsToday > 0 ? "正常" : "关注", logsToday > 0 ? "normal" : "warn", "—"),
                statusRow("权限变更", permTotal > 0 ? "已配置" : "待配置", "normal", String.valueOf(permTotal)),
                statusRow("异常登录", loginFail > 0 ? "预警" : "正常", loginFail > 0 ? "warn" : "normal", String.valueOf(loginFail))
        );

        List<Map<String, Object>> logRows = logs.stream()
                .sorted(Comparator.comparing(OperationLog::getOperatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10).map(this::logRow).toList();

        Map<String, Object> result = baseResult("admin", "系统管理工作台", metrics);
        result.put("panels", List.of(
                panelHBar("usersByRole", "用户角色分布", 4, "/system/user", roleNames, roleCounts),
                panelLine("logTrend", "操作日志趋势", 5, "/system/log", dayLabels,
                        List.of(lineSeries("操作次数", dayCounts, C_PURPLE)), true),
                panel("modulePie", "操作模块占比", 3, "pie", "/system/log", null, moduleItems, null),
                panelStatus("health", "服务健康状态", 12, "/system/log", healthList)
        ));
        result.put("tables", List.of(table("logs", "操作日志明细", "/system/log", "下钻日志",
                List.of(col("module", "模块", 90), col("operationType", "操作", 80), col("operationContent", "内容", 180),
                        col("operator", "操作人", 80), col("operatedAt", "时间", 140)), logRows, 160)));
        return result;
    }

    // ── 生产计划员 ─────────────────────────────────────────────

    private Map<String, Object> buildPlannerDashboard(int days) {
        List<CustomerOrder> orders = customerOrderMapper.customerOrderList();
        List<ProductionPlan> plans = productionPlanMapper.planList();
        Map<Long, List<CustomerOrderItem>> itemsByOrder = customerOrderItemMapper.orderItemList().stream()
                .collect(Collectors.groupingBy(CustomerOrderItem::getOrderId));
        Set<Long> plannedOrderIds = plans.stream()
                .map(ProductionPlan::getSourceOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();
        List<CustomerOrder> pendingPlanOrders = orders.stream()
                .filter(o -> List.of("PLAN_PENDING", "APPROVED", "PASS", "PASSED").contains(nullToEmpty(o.getAuditStatus())))
                .filter(o -> !plannedOrderIds.contains(o.getOrderId()))
                .toList();

        Map<String, Long> planStatusCount = plans.stream()
                .collect(Collectors.groupingBy(p -> nullToEmpty(p.getPlanStatus()), Collectors.counting()));
        long draft = planStatusCount.getOrDefault("DRAFT", 0L);
        long executing = planStatusCount.getOrDefault("EXECUTING", 0L) + planStatusCount.getOrDefault("RUNNING", 0L)
                + planStatusCount.getOrDefault("RELEASED", 0L);
        long completed = planStatusCount.getOrDefault("COMPLETED", 0L);
        int completionRate = plans.isEmpty() ? 0 : (int) Math.round(completed * 100.0 / plans.size());

        long deliveryRisk = orders.stream()
                .filter(o -> o.getRequiredDeliveryDate() != null
                        && !o.getRequiredDeliveryDate().isBefore(today)
                        && o.getRequiredDeliveryDate().isBefore(today.plusDays(7))
                        && !List.of("SHIPPED", "REJECTED").contains(o.getAuditStatus()))
                .count();
        long materialGap = countLowStockMaterials();
        List<Equipment> equipments = equipmentMapper.equipmentList();
        long runningEquip = equipments.stream().filter(e -> List.of("RUNNING", "PRODUCING", "BUSY").contains(nullToEmpty(e.getStatus()))).count();
        int equipLoadPct = equipments.isEmpty() ? 0 : (int) Math.round(runningEquip * 100.0 / equipments.size());

        List<Map<String, Object>> metrics = List.of(
                metricEx("pendingOrders", "待排订单", pendingPlanOrders.size(), null, pendingPlanOrders.isEmpty() ? "normal" : "warn", null, "/production/plan"),
                metricEx("completionRate", "计划完成率", completionRate, "%", completionRate >= 70 ? "normal" : "warn", null, "/production/plan"),
                metricEx("deliveryRisk", "交期风险", deliveryRisk, null, deliveryRisk > 0 ? "warn" : "normal", "7日内", "/order/track"),
                metricEx("equipLoad", "设备负载", equipLoadPct, "%", equipLoadPct > 85 ? "danger" : equipLoadPct > 60 ? "warn" : "normal", null, "/equipment/list"),
                metricEx("materialGap", "物料缺口", materialGap, "项", materialGap > 0 ? "danger" : "normal", "低于安全库存", "/warehouse/capacity"),
                metricEx("executing", "执行中", executing, null, executing > 0 ? "normal" : "normal", null, "/production/plan")
        );

        List<String> planStatusOrder = List.of("DRAFT", "PUBLISHED", "SUBMITTED", "RELEASED", "EXECUTING", "RUNNING", "COMPLETED", "CANCELLED");
        List<Map<String, Object>> planStatusItems = chartItemsFromCount(planStatusCount, planStatusOrder, PLAN_COLORS, MesStatusMapper::toPlanCn);

        List<String> equipLabels = equipments.stream().limit(5).map(Equipment::getEquipmentName).toList();
        List<Integer> equipLoads = equipments.stream().limit(5).map(e -> {
            String st = nullToEmpty(e.getStatus());
            if (List.of("RUNNING", "PRODUCING", "BUSY").contains(st)) return 85;
            if ("IDLE".equals(st) || "空闲".equals(st)) return 20;
            if ("MAINTENANCE".equals(st) || "维护".equals(st)) return 0;
            return 45;
        }).toList();

        List<Map<String, Object>> gapItems = lowStockItems(5);

        List<String> riskCats = List.of("7日内交期", "14日内交期", "已逾期");
        List<Integer> riskVals = List.of(
                (int) deliveryRisk,
                (int) orders.stream().filter(o -> o.getRequiredDeliveryDate() != null
                        && o.getRequiredDeliveryDate().isBefore(today.plusDays(14))
                        && !o.getRequiredDeliveryDate().isBefore(today.plusDays(7))
                        && !List.of("SHIPPED", "REJECTED").contains(o.getAuditStatus())).count(),
                (int) orders.stream().filter(o -> o.getRequiredDeliveryDate() != null
                        && o.getRequiredDeliveryDate().isBefore(today)
                        && !List.of("SHIPPED", "REJECTED").contains(o.getAuditStatus())).count()
        );

        List<ProductionPlan> ganttPlans = plans.stream()
                .filter(p -> p.getPlannedStartDate() != null && p.getPlannedEndDate() != null)
                .filter(p -> !List.of("COMPLETED", "CANCELLED").contains(nullToEmpty(p.getPlanStatus())))
                .sorted(Comparator.comparing(ProductionPlan::getPlannedStartDate))
                .limit(6)
                .toList();
        LocalDate ganttStart = ganttPlans.stream().map(ProductionPlan::getPlannedStartDate).min(LocalDate::compareTo).orElse(today);
        LocalDate ganttEnd = ganttPlans.stream().map(ProductionPlan::getPlannedEndDate).max(LocalDate::compareTo).orElse(today.plusDays(days));
        long rangeDays = Math.max(1, ganttEnd.toEpochDay() - ganttStart.toEpochDay() + 1);
        List<Map<String, Object>> ganttRows = new ArrayList<>();
        int gi = 0;
        for (ProductionPlan p : ganttPlans) {
            long start = p.getPlannedStartDate().toEpochDay() - ganttStart.toEpochDay();
            long duration = Math.max(1, p.getPlannedEndDate().toEpochDay() - p.getPlannedStartDate().toEpochDay() + 1);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("label", p.getPlanNo());
            row.put("start", (int) start);
            row.put("duration", (int) duration);
            row.put("color", pickIndustrialColor(gi++));
            row.put("tag", MesStatusMapper.toPlanCn(p.getPlanStatus()));
            ganttRows.add(row);
        }

        List<Map<String, Object>> conflicts = detectPlanConflicts(plans);
        List<Map<String, Object>> adjustTasks = plans.stream()
                .filter(p -> "DRAFT".equals(p.getPlanStatus()) || (p.getAdjustReason() != null && !p.getAdjustReason().isBlank()))
                .sorted(Comparator.comparing(ProductionPlan::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(p -> statusRow(p.getPlanNo(), p.getAdjustReason() != null ? "待调整" : "草稿",
                        "DRAFT".equals(p.getPlanStatus()) ? "warn" : "normal",
                        MesStatusMapper.toPlanCn(p.getPlanStatus())))
                .toList();

        List<Map<String, Object>> pendingOrderRows = pendingPlanOrders.stream().limit(8).map(o -> {
            Map<String, Object> row = new LinkedHashMap<>();
            CustomerOrderItem item = firstItem(itemsByOrder.get(o.getOrderId()));
            row.put("orderNo", o.getOrderNo());
            row.put("customerName", o.getCustomerName());
            row.put("productModel", item != null ? item.getProductName() : "—");
            row.put("quantity", item != null ? intVal(item.getQuantity()) : 0);
            row.put("deliveryDate", fmtDate(o.getRequiredDeliveryDate()));
            row.put("status", MesStatusMapper.toOrderCn(o.getAuditStatus()));
            return row;
        }).toList();

        Map<String, Object> result = baseResult("planner", "计划员工作台", metrics);
        result.put("panels", List.of(
                panel("planStatus", "计划状态分布", 3, "donut", "/production/plan", null, planStatusItems, plans.size() + "单"),
                panelHBar("equipLoad", "设备负载", 3, "/equipment/list", equipLabels, equipLoads),
                panelHBar("materialGap", "物料缺口", 3, "/warehouse/capacity", gapItems),
                panelHBar("deliveryRisk", "交期风险", 3, "/order/track", riskCats, riskVals),
                panelGantt("planGantt", "生产甘特图", 8, "/production/plan", ganttRows, 0, (int) rangeDays),
                panelStatus("conflicts", "计划冲突", 4, "/production/plan", conflicts.isEmpty()
                        ? List.of(statusRow("排产检查", "无冲突", "normal", "OK"))
                        : conflicts)
        ));
        result.put("tables", List.of(
                table("pendingOrders", "待编制计划订单", "/production/plan", "去编制",
                        List.of(col("orderNo", "订单号", 120), col("customerName", "客户", 90), col("productModel", "产品", 120),
                                col("quantity", "数量", 60), col("deliveryDate", "交期", 90), col("status", "状态", 80)),
                        pendingOrderRows, 140),
                table("adjustTasks", "待调整任务", "/production/plan", "调整计划",
                        List.of(col("label", "计划/任务", 140), col("tag", "类型", 80), col("status", "状态", 80), col("value", "说明", 120)),
                        adjustTasks.stream().map(s -> {
                            Map<String, Object> r = new LinkedHashMap<>();
                            r.put("label", s.get("label"));
                            r.put("tag", s.get("tag"));
                            r.put("status", s.get("status"));
                            r.put("value", s.get("value"));
                            return r;
                        }).toList(), 120)
        ));
        return result;
    }

    // ── 生产操作员 ─────────────────────────────────────────────

    private Map<String, Object> buildOperatorDashboard(Long userId, int days) {
        List<DispatchTask> allDispatches = dispatchTaskMapper.dispatchList();
        List<WorkReport> allReports = workReportMapper.reportList();
        Map<Long, WorkOrder> woById = workOrderMapper.workOrderList().stream()
                .collect(Collectors.toMap(WorkOrder::getWorkOrderId, w -> w, (a, b) -> a));
        Map<Long, Equipment> equipById = equipmentMapper.equipmentList().stream()
                .collect(Collectors.toMap(Equipment::getEquipmentId, e -> e, (a, b) -> a));

        List<DispatchTask> mine = userId == null ? List.of() : allDispatches.stream()
                .filter(d -> userId.equals(d.getOperatorId()))
                .toList();
        List<WorkReport> myReports = userId == null ? List.of() : allReports.stream()
                .filter(r -> userId.equals(r.getOperatorId()))
                .toList();

        List<String> activeStatuses = List.of("ASSIGNED", "ACCEPTED", "PRODUCING", "RUNNING");
        long pendingAccept = mine.stream().filter(d -> "ASSIGNED".equals(d.getStatus())).count();
        long active = mine.stream().filter(d -> activeStatuses.contains(d.getStatus())).count();
        long completed = mine.stream().filter(d -> "COMPLETED".equals(d.getStatus())).count();

        LocalDate today = LocalDate.now();
        int todayReportQty = myReports.stream()
                .filter(r -> r.getReportDate() != null && r.getReportDate().equals(today))
                .mapToInt(r -> intVal(r.getCompletedQuantity()))
                .sum();
        int todayQualified = myReports.stream()
                .filter(r -> r.getReportDate() != null && r.getReportDate().equals(today))
                .mapToInt(r -> intVal(r.getQualifiedQuantity()))
                .sum();
        double todayHours = myReports.stream()
                .filter(r -> r.getReportDate() != null && r.getReportDate().equals(today))
                .mapToDouble(r -> r.getWorkHours() != null ? r.getWorkHours().doubleValue() : 0)
                .sum();

        AttendanceRecord attendance = userId != null ? attendanceRecordMapper.getByUserAndDate(userId, today) : null;
        List<AndonAlarm> openAlarms = andonAlarmMapper.listOpenAlarms(20);

        List<Map<String, Object>> metrics = List.of(
                metricEx("currentWo", "当前工单", active, null, active > 0 ? "normal" : "warn", "进行中", "/production/my-dispatch"),
                metricEx("todayQty", "今日产量", todayReportQty, "件", todayReportQty > 0 ? "normal" : "normal", null, "/production/report"),
                metricEx("qualified", "合格数", todayQualified, "件", todayQualified < todayReportQty && todayReportQty > 0 ? "warn" : "normal", null, "/production/report"),
                metricEx("todayHours", "今日工时", String.format("%.1f", todayHours), "h", "normal", null, "/production/report"),
                metricEx("pendingAccept", "待接收", pendingAccept, null, pendingAccept > 0 ? "warn" : "normal", null, "/production/my-dispatch"),
                metricEx("alarms", "异常提醒", openAlarms.size(), null, openAlarms.isEmpty() ? "normal" : "danger", "未关闭安灯", "/equipment/alarm")
        );

        Map<String, Long> dispatchStatus = mine.stream()
                .collect(Collectors.groupingBy(d -> nullToEmpty(d.getStatus()), Collectors.counting()));
        List<String> dispatchOrder = List.of("ASSIGNED", "ACCEPTED", "PRODUCING", "RUNNING", "QC_PENDING", "COMPLETED");
        List<Map<String, Object>> dispatchItems = chartItemsFromCount(dispatchStatus, dispatchOrder, DISPATCH_COLORS, MesStatusMapper::toDispatchCn);

        List<Map<String, Object>> progressList = mine.stream()
                .filter(d -> activeStatuses.contains(d.getStatus()))
                .sorted(Comparator.comparing(DispatchTask::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(d -> {
                    int plan = Math.max(1, intVal(d.getAssignedQuantity()));
                    int done = intVal(d.getCompletedQuantity());
                    WorkOrder wo = woById.get(d.getWorkOrderId());
                    String label = wo != null ? wo.getWorkOrderNo() : d.getDispatchNo();
                    String st = done >= plan ? "normal" : done > 0 ? "warn" : "normal";
                    return progressRow(label, done, plan, st, null, "/production/my-dispatch");
                }).toList();

        Set<Long> myEquipIds = mine.stream().map(DispatchTask::getEquipmentId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<Map<String, Object>> equipStatus = myEquipIds.isEmpty()
                ? equipmentsFallbackStatus(equipById)
                : myEquipIds.stream().limit(5).map(id -> {
                    Equipment e = equipById.get(id);
                    if (e == null) return statusRow("设备#" + id, "未知", "warn", "—");
                    String st = nullToEmpty(e.getStatus());
                    String tag = List.of("RUNNING", "PRODUCING", "BUSY").contains(st) ? "运行" :
                            "MAINTENANCE".equals(st) ? "维护" : "IDLE".equals(st) ? "空闲" : st;
                    String level = List.of("RUNNING", "PRODUCING", "BUSY").contains(st) ? "normal" :
                            "MAINTENANCE".equals(st) ? "warn" : "normal";
                    return statusRow(e.getEquipmentName(), tag, level, e.getWorkstation());
                }).toList();

        List<String> dayLabels = new ArrayList<>();
        List<Integer> dayQty = new ArrayList<>();
        List<Double> dayHours = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            dayLabels.add(d.getMonthValue() + "/" + d.getDayOfMonth());
            List<WorkReport> dayReports = myReports.stream().filter(r -> d.equals(r.getReportDate())).toList();
            dayQty.add(dayReports.stream().mapToInt(r -> intVal(r.getCompletedQuantity())).sum());
            dayHours.add(dayReports.stream().mapToDouble(r -> r.getWorkHours() != null ? r.getWorkHours().doubleValue() : 0).sum());
        }

        List<Map<String, Object>> alarmList = openAlarms.stream().limit(5).map(a -> {
            String level = "IMPORTANT".equals(a.getAlarmLevel()) || "CRITICAL".equals(a.getAlarmLevel()) ? "danger" : "warn";
            return statusRow(a.getAlarmNo(), a.getAlarmDescription() != null && a.getAlarmDescription().length() > 16
                    ? a.getAlarmDescription().substring(0, 16) : nullToEmpty(a.getAlarmDescription()), level, a.getAlarmStatus());
        }).toList();

        List<Map<String, Object>> pendingRows = mine.stream()
                .filter(d -> List.of("ASSIGNED", "ACCEPTED", "PRODUCING", "RUNNING").contains(d.getStatus()))
                .sorted(Comparator.comparing(DispatchTask::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .map(d -> dispatchRow(d, woById.get(d.getWorkOrderId())))
                .toList();

        Map<String, Object> result = baseResult("operator", "生产操作员工作台", metrics);
        if (attendance != null) {
            Map<String, Object> att = new LinkedHashMap<>();
            att.put("checkInTime", fmtDateTime(attendance.getCheckInTime()));
            att.put("checkOutTime", fmtDateTime(attendance.getCheckOutTime()));
            att.put("status", attendanceStatusCn(attendance.getStatus()));
            result.put("attendance", att);
        }
        result.put("panels", List.of(
                panel("dispatchStatus", "派工状态", 3, "donut", "/production/my-dispatch", null, dispatchItems, mine.size() + "单"),
                panelProgress("processProgress", "工序进度", 4, "/production/my-dispatch", progressList),
                panelStatus("equipStatus", "设备状态", 3, "/equipment/list", equipStatus),
                panelCombo("outputTrend", "报工趋势", 5, "/production/report", dayLabels,
                        List.of(barSeries("完成件数", dayQty, C_BLUE), lineSeries("工时(h)", dayHours, C_CYAN)), true),
                panelStatus("alarms", "异常提醒", 3, "/equipment/alarm", alarmList.isEmpty()
                        ? List.of(statusRow("安灯监控", "无未关闭报警", "normal", "OK"))
                        : alarmList)
        ));
        result.put("tables", List.of(
                table("pendingDispatch", "待执行任务", "/production/my-dispatch", "我的派工",
                        List.of(col("dispatchNo", "派工单", 120), col("workOrderNo", "工单", 120),
                                col("planQty", "计划", 60), col("completedQty", "完成", 60), col("status", "状态", 80)),
                        pendingRows, 140)
        ));
        return result;
    }

    // ── 财务人员 ───────────────────────────────────────────────

    private Map<String, Object> buildCostDashboard(int days) {
        Map<String, Object> kpi = costService.costKpi();
        List<Map<String, Object>> settlements = costService.listSettlementViews();
        List<Map<String, Object>> sourceGroups = costService.groupBySourceType();
        List<CustomerOrder> orders = customerOrderMapper.customerOrderList();

        int total = intVal(kpi.get("total"));
        int draft = intVal(kpi.get("draft"));
        BigDecimal totalCost = nzDec(kpi.get("totalAmount"));
        BigDecimal materialCost = nzDec(kpi.get("totalMaterialCost"));
        BigDecimal laborCost = nzDec(kpi.get("totalLaborCost"));
        BigDecimal equipCost = nzDec(kpi.get("totalEquipmentCost"));
        BigDecimal qualityCost = nzDec(kpi.get("totalQualityCost"));

        BigDecimal orderAmount = orders.stream()
                .filter(o -> !List.of("PENDING", "REJECTED").contains(nullToEmpty(o.getAuditStatus())))
                .map(o -> nz(o.getOrderAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal received = settlements.stream()
                .filter(s -> List.of("CONFIRMED", "EXPORTED").contains(String.valueOf(s.get("settlementStatus"))))
                .map(s -> nzDec(s.get("totalCost")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receivable = orderAmount.subtract(received).max(BigDecimal.ZERO);
        BigDecimal profit = orderAmount.subtract(totalCost);

        List<Map<String, Object>> metrics = List.of(
                metricEx("orderAmount", "订单金额", fmtAmt(orderAmount), "万", "normal", null, "/order/list"),
                metricEx("received", "已收款", fmtAmt(received), "万", "normal", "结算确认", "/cost/settlement"),
                metricEx("receivable", "待收款", fmtAmt(receivable), "万", receivable.compareTo(BigDecimal.ZERO) > 0 ? "warn" : "normal", null, "/order/list"),
                metricEx("cost", "成本", fmtAmt(totalCost), "万", "normal", null, "/cost/report"),
                metricEx("profit", "利润", fmtAmt(profit), "万", profit.compareTo(BigDecimal.ZERO) >= 0 ? "normal" : "danger", "订单-成本", "/cost/report"),
                metricEx("draft", "待处理结算", draft, null, draft > 0 ? "warn" : "normal", null, "/cost/settlement")
        );

        LocalDate today = LocalDate.now();
        List<String> dayLabels = new ArrayList<>();
        List<Double> collectionTrend = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            dayLabels.add(d.getMonthValue() + "/" + d.getDayOfMonth());
            LocalDate fd = d;
            double dayAmt = settlements.stream()
                    .filter(s -> List.of("CONFIRMED", "EXPORTED").contains(String.valueOf(s.get("settlementStatus"))))
                    .filter(s -> {
                        Object ca = s.get("confirmedAt");
                        if (ca == null) return false;
                        try {
                            LocalDate cd = ca instanceof LocalDateTime ldt ? ldt.toLocalDate()
                                    : LocalDate.parse(String.valueOf(ca).substring(0, 10));
                            return cd.equals(fd);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .mapToDouble(s -> nzDec(s.get("totalCost")).doubleValue() / 10000.0)
                    .sum();
            collectionTrend.add(Math.round(dayAmt * 100.0) / 100.0);
        }

        Map<String, BigDecimal> debtByCustomer = new LinkedHashMap<>();
        for (CustomerOrder o : orders) {
            if (List.of("PENDING", "REJECTED").contains(nullToEmpty(o.getAuditStatus()))) continue;
            debtByCustomer.merge(nullToEmpty(o.getCustomerName()).isBlank() ? "未知客户" : o.getCustomerName(),
                    nz(o.getOrderAmount()), BigDecimal::add);
        }
        for (Map<String, Object> s : settlements) {
            if (!List.of("CONFIRMED", "EXPORTED").contains(String.valueOf(s.get("settlementStatus")))) continue;
            Object orderId = s.get("orderId");
            if (orderId == null) continue;
            CustomerOrder o = orders.stream().filter(x -> Objects.equals(x.getOrderId(), Long.valueOf(String.valueOf(orderId)))).findFirst().orElse(null);
            if (o != null && o.getCustomerName() != null) {
                debtByCustomer.merge(o.getCustomerName(), nzDec(s.get("totalCost")).negate(), BigDecimal::add);
            }
        }
        List<String> debtCats = debtByCustomer.entrySet().stream()
                .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) > 0)
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5).map(Map.Entry::getKey).toList();
        List<Integer> debtVals = debtCats.stream()
                .map(c -> debtByCustomer.get(c).divide(BigDecimal.valueOf(10000), 0, RoundingMode.HALF_UP).intValue())
                .toList();

        List<String> costTypes = List.of("材料", "人工", "设备", "质量");
        List<Integer> costVals = List.of(
                materialCost.divide(BigDecimal.valueOf(10000), 0, RoundingMode.HALF_UP).intValue(),
                laborCost.divide(BigDecimal.valueOf(10000), 0, RoundingMode.HALF_UP).intValue(),
                equipCost.divide(BigDecimal.valueOf(10000), 0, RoundingMode.HALF_UP).intValue(),
                qualityCost.divide(BigDecimal.valueOf(10000), 0, RoundingMode.HALF_UP).intValue()
        );

        Map<String, Long> statusCount = new LinkedHashMap<>();
        statusCount.put("DRAFT", (long) draft);
        statusCount.put("CONFIRMED", (long) intVal(kpi.get("confirmed")));
        statusCount.put("EXPORTED", (long) intVal(kpi.get("exported")));
        List<Map<String, Object>> statusItems = chartItemsFromCount(statusCount,
                List.of("DRAFT", "CONFIRMED", "EXPORTED"), COST_STATUS_COLORS, MesStatusMapper::toCostCn);

        List<Map<String, Object>> sourceItems = sourceGroups.stream()
                .map(g -> chartItem(String.valueOf(g.getOrDefault("sourceTypeCn", g.get("sourceType"))),
                        intVal(g.get("count")), pickIndustrialColor(sourceGroups.indexOf(g))))
                .filter(i -> intVal(i.get("value")) > 0)
                .limit(5).toList();

        List<Map<String, Object>> draftRows = settlements.stream()
                .filter(s -> "DRAFT".equals(String.valueOf(s.get("settlementStatus"))))
                .limit(8)
                .map(this::settlementRow)
                .toList();

        Map<String, Object> result = baseResult("cost", "财务/成本工作台", metrics);
        result.put("panels", List.of(
                panelLine("collectionTrend", "回款趋势", 5, "/cost/settlement", dayLabels,
                        List.of(lineSeries("回款(万)", collectionTrend, C_ORANGE)), true),
                panelHBar("customerDebt", "客户欠款排行", 4, "/order/list", debtCats, debtVals),
                panelHBar("costBreakdown", "成本构成", 3, "/cost/report", costTypes, costVals),
                panel("settlementStatus", "结算状态", 3, "donut", "/cost/settlement", null, statusItems, total + "单"),
                panel("sourcePie", "成本来源", 3, "pie", "/cost/report", null, sourceItems, null)
        ));
        result.put("tables", List.of(
                table("draftSettlements", "待处理结算单", "/cost/settlement", "去确认", settlementColumns(), draftRows, 140)
        ));
        return result;
    }

    // ── 采购员 ─────────────────────────────────────────────────

    private Map<String, Object> buildPurchaseDashboard(Long userId, int days) {
        List<PurchaseOrder> orders = purchaseOrderMapper.purchaseOrderList();
        List<PurchaseRequirement> reqs = purchaseRequirementMapper.requirementList();
        LocalDate today = LocalDate.now();

        long pendingReq = reqs.stream().filter(r -> "PENDING".equals(nullToEmpty(r.getStatus()))).count();
        long orderedReq = reqs.stream().filter(r -> "ORDERED".equals(nullToEmpty(r.getStatus()))).count();
        long openOrders = orders.stream()
                .filter(o -> !List.of("ARRIVED", "CANCELLED", "RECEIVED").contains(nullToEmpty(o.getStatus())))
                .count();
        long overdue = orders.stream().filter(o -> {
            String st = nullToEmpty(o.getStatus());
            if (List.of("ARRIVED", "CANCELLED", "RECEIVED").contains(st)) return false;
            return o.getExpectedArrivalDate() != null && o.getExpectedArrivalDate().isBefore(today);
        }).count();
        long todayOrders = orders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().equals(today))
                .count();
        int shortageKinds = (int) reqs.stream()
                .filter(r -> "PENDING".equals(nullToEmpty(r.getStatus())))
                .filter(r -> nz(r.getShortageQuantity()).compareTo(BigDecimal.ZERO) > 0)
                .count();
        AttendanceRecord attendance = userId != null ? attendanceRecordMapper.getByUserAndDate(userId, today) : null;

        List<Map<String, Object>> metrics = List.of(
                metricEx("pendingReq", "待采购", pendingReq, "种", pendingReq > 0 ? "warn" : "normal", null, "/purchase/demand"),
                metricEx("overdue", "逾期未到", overdue, "单", overdue > 0 ? "danger" : "normal", null, "/purchase/order"),
                metricEx("openOrders", "在途订单", openOrders, null, "normal", null, "/purchase/order"),
                metricEx("todayOrders", "今日下单", todayOrders, null, "normal", null, "/purchase/order"),
                metricEx("orderedReq", "已转采购", orderedReq, null, "normal", null, "/purchase/demand"),
                metricEx("shortageKinds", "缺料物料", shortageKinds, null, shortageKinds > 0 ? "warn" : "normal", null, "/purchase/workbench")
        );

        Map<String, Long> reqStatus = reqs.stream()
                .collect(Collectors.groupingBy(r -> nullToEmpty(r.getStatus()), Collectors.counting()));
        List<Map<String, Object>> reqItems = chartItemsFromCount(reqStatus,
                List.of("PENDING", "ORDERED"), PURCHASE_REQ_COLORS, MesStatusMapper::toPurchaseDemandCn);

        Map<String, Long> orderStatus = orders.stream()
                .collect(Collectors.groupingBy(o -> nullToEmpty(o.getStatus()), Collectors.counting()));
        List<Map<String, Object>> orderItems = chartItemsFromCount(orderStatus,
                List.of("RELEASED", "PARTIAL_ARRIVED", "ARRIVED", "CANCELLED"), PURCHASE_ORDER_COLORS, MesStatusMapper::toPurchaseOrderCn);

        List<String> dayLabels = new ArrayList<>();
        List<Integer> orderTrend = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            dayLabels.add(d.getMonthValue() + "/" + d.getDayOfMonth());
            orderTrend.add((int) orders.stream()
                    .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().equals(d))
                    .count());
        }

        List<Map<String, Object>> overdueAlerts = orders.stream()
                .filter(o -> {
                    String st = nullToEmpty(o.getStatus());
                    if (List.of("ARRIVED", "CANCELLED", "RECEIVED").contains(st)) return false;
                    return o.getExpectedArrivalDate() != null && o.getExpectedArrivalDate().isBefore(today);
                })
                .sorted(Comparator.comparing(PurchaseOrder::getExpectedArrivalDate))
                .limit(5)
                .map(o -> statusRow(o.getPurchaseOrderNo(),
                        o.getSupplierName() != null ? o.getSupplierName() : "供应商",
                        "danger",
                        fmtDate(o.getExpectedArrivalDate())))
                .toList();

        List<Map<String, Object>> pendingRows = reqs.stream()
                .filter(r -> "PENDING".equals(nullToEmpty(r.getStatus())))
                .sorted(Comparator.comparing(PurchaseRequirement::getPriority, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PurchaseRequirement::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(this::purchaseReqRow)
                .toList();

        Map<String, Object> result = baseResult("purchase", "采购员工作台", metrics);
        if (attendance != null) {
            Map<String, Object> att = new LinkedHashMap<>();
            att.put("checkInTime", fmtDateTime(attendance.getCheckInTime()));
            att.put("checkOutTime", fmtDateTime(attendance.getCheckOutTime()));
            att.put("status", attendanceStatusCn(attendance.getStatus()));
            result.put("attendance", att);
        }
        result.put("panels", List.of(
                panel("reqStatus", "采购需求状态", 3, "donut", "/purchase/demand", null, reqItems, reqs.size() + "项"),
                panel("orderStatus", "采购订单状态", 3, "donut", "/purchase/order", null, orderItems, orders.size() + "单"),
                panelLine("orderTrend", "下单趋势", 5, "/purchase/order", dayLabels,
                        List.of(lineSeries("采购单", orderTrend, C_BLUE)), true),
                panelStatus("overdueAlerts", "逾期提醒", 3, "/purchase/order", overdueAlerts.isEmpty()
                        ? List.of(statusRow("到货跟踪", "无逾期订单", "normal", "OK"))
                        : overdueAlerts)
        ));
        result.put("tables", List.of(
                table("pendingReq", "待采购物料", "/purchase/workbench", "采购工作台",
                        List.of(col("materialCode", "编码", 100), col("materialName", "物料", 140),
                                col("shortageQuantity", "缺料", 70), col("suggestedPurchaseQuantity", "建议量", 70),
                                col("supplierName", "供应商", 100), col("status", "状态", 80)),
                        pendingRows, 140)
        ));
        return result;
    }

    // ── 仓储管理员 ─────────────────────────────────────────────

    private Map<String, Object> buildWarehouseDashboard(Long userId, int days) {
        Map<String, Object> snap = mesSnapshotService.buildSnapshot();
        List<Map<String, Object>> inbound = snapshotList(snap, "inboundTasks");
        List<Map<String, Object>> issues = snapshotList(snap, "issueTasks");
        List<Map<String, Object>> deliveries = snapshotList(snap, "deliveries");
        List<Map<String, Object>> inventory = snapshotList(snap, "inventory");
        List<Map<String, Object>> stockFlows = snapshotList(snap, "stockFlows");

        LocalDate today = LocalDate.now();
        long pendingInbound = inbound.stream().filter(t -> "待入库".equals(mapStr(t, "status"))).count();
        long pendingIssue = issues.stream().filter(t -> !"已完成".equals(mapStr(t, "status"))).count();
        long pendingDelivery = deliveries.stream().filter(d -> "待出库".equals(mapStr(d, "status"))).count();
        long pendingOutbound = pendingIssue + pendingDelivery;
        long stockAlert = countLowStockMaterials();
        long invKinds = inventory.size();
        long todayFlows = stockFlows.stream()
                .filter(f -> {
                    String at = mapStr(f, "createdAt");
                    return at.length() >= 10 && at.substring(0, 10).equals(today.format(DAY_FMT));
                })
                .count();

        AttendanceRecord attendance = userId != null ? attendanceRecordMapper.getByUserAndDate(userId, today) : null;

        List<Map<String, Object>> metrics = List.of(
                metricEx("pendingInbound", "待入库", pendingInbound, "单", pendingInbound > 0 ? "warn" : "normal", null, "/warehouse/inbound-hub"),
                metricEx("pendingOutbound", "待出库", pendingOutbound, "单", pendingOutbound > 0 ? "warn" : "normal", "领料+发货", "/warehouse/outbound-hub"),
                metricEx("stockAlert", "库存预警", stockAlert, "项", stockAlert > 0 ? "danger" : "normal", "低于安全库存", "/warehouse/capacity"),
                metricEx("invKinds", "库存品种", invKinds, null, "normal", null, "/warehouse/capacity"),
                metricEx("todayFlows", "今日流水", todayFlows, "笔", "normal", null, "/warehouse/capacity"),
                metricEx("pendingIssue", "待领料", pendingIssue, "单", pendingIssue > 0 ? "warn" : "normal", null, "/warehouse/outbound-hub?tab=issue")
        );

        List<Map<String, Object>> taskTypeItems = new ArrayList<>();
        if (pendingInbound > 0) taskTypeItems.add(chartItem("待入库", (int) pendingInbound, C_BLUE));
        if (pendingIssue > 0) taskTypeItems.add(chartItem("待领料", (int) pendingIssue, C_ORANGE));
        if (pendingDelivery > 0) taskTypeItems.add(chartItem("待发货", (int) pendingDelivery, C_CYAN));
        if (stockAlert > 0) taskTypeItems.add(chartItem("库存预警", (int) stockAlert, C_RED));
        if (taskTypeItems.isEmpty()) {
            taskTypeItems.add(chartItem("暂无待办", 1, C_GREEN));
        }

        List<Map<String, Object>> stockItems = lowStockItems(5);
        if (stockItems.isEmpty()) {
            stockItems = List.of(chartItem("库存正常", 1, C_GREEN));
        }

        List<String> dayLabels = new ArrayList<>();
        List<Integer> inTrend = new ArrayList<>();
        List<Integer> outTrend = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            dayLabels.add(d.getMonthValue() + "/" + d.getDayOfMonth());
            String dayStr = d.format(DAY_FMT);
            int inCnt = 0;
            int outCnt = 0;
            for (Map<String, Object> f : stockFlows) {
                String at = mapStr(f, "createdAt");
                if (at.length() < 10 || !at.substring(0, 10).equals(dayStr)) continue;
                if ("入库".equals(mapStr(f, "direction"))) inCnt++;
                else if ("出库".equals(mapStr(f, "direction"))) outCnt++;
            }
            inTrend.add(inCnt);
            outTrend.add(outCnt);
        }

        List<Map<String, Object>> alertStatus = lowStockEntries().stream().limit(5)
                .map(e -> statusRow(e.getKey(), "低于安全库存", "danger", String.valueOf(e.getValue())))
                .toList();

        List<Map<String, Object>> todoRows = new ArrayList<>();
        issues.stream()
                .filter(t -> !"已完成".equals(mapStr(t, "status")))
                .sorted(Comparator.comparing((Map<String, Object> t) -> mapStr(t, "createdAt"), Comparator.reverseOrder()))
                .limit(6)
                .forEach(t -> todoRows.add(warehouseTodoRow("领料", mapStr(t, "materialName"), mapStr(t, "id"), mapStr(t, "createdAt"))));
        inbound.stream()
                .filter(t -> "待入库".equals(mapStr(t, "status")))
                .limit(4)
                .forEach(t -> todoRows.add(warehouseTodoRow("入库", mapStr(t, "productModel") + " " + mapStr(t, "quantity") + "台",
                        mapStr(t, "id"), mapStr(t, "createdAt"))));
        deliveries.stream()
                .filter(d -> "待出库".equals(mapStr(d, "status")))
                .limit(4)
                .forEach(d -> todoRows.add(warehouseTodoRow("发货", mapStr(d, "productModel"), mapStr(d, "id"), mapStr(d, "createdAt"))));

        Map<String, Object> result = baseResult("warehouse", "仓储人员首页", metrics);
        if (attendance != null) {
            Map<String, Object> att = new LinkedHashMap<>();
            att.put("checkInTime", fmtDateTime(attendance.getCheckInTime()));
            att.put("checkOutTime", fmtDateTime(attendance.getCheckOutTime()));
            att.put("status", attendanceStatusCn(attendance.getStatus()));
            result.put("attendance", att);
        }
        result.put("panels", List.of(
                panel("taskMix", "待办类型分布", 3, "donut", "/warehouse/workbench", null, taskTypeItems,
                        (pendingInbound + pendingOutbound + stockAlert) + "项"),
                panelHBar("stockAlertBar", "库存预警物料", 4, "/warehouse/capacity", stockItems),
                panelCombo("flowTrend", "出入库流水趋势", 5, "/warehouse/capacity", dayLabels,
                        List.of(barSeries("入库", inTrend, C_BLUE), barSeries("出库", outTrend, C_ORANGE)), true),
                panelStatus("stockAlerts", "库存预警", 3, "/warehouse/capacity", alertStatus.isEmpty()
                        ? List.of(statusRow("库存监控", "暂无预警", "normal", "OK"))
                        : alertStatus)
        ));
        result.put("tables", List.of(
                table("warehouseTodos", "仓储待办", "/warehouse/workbench", "仓储管理工作台",
                        List.of(col("type", "类型", 72), col("title", "待办内容", 180), col("refNo", "单号", 100),
                                col("createdAt", "时间", 130)),
                        todoRows.stream().limit(12).toList(), 160)
        ));
        return result;
    }

    private Map<String, Object> warehouseTodoRow(String type, String title, String refNo, String createdAt) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("type", type);
        row.put("title", title != null && !title.isBlank() ? title : "—");
        row.put("refNo", refNo != null && !refNo.isBlank() ? refNo : "—");
        row.put("createdAt", createdAt != null && !createdAt.isBlank() ? createdAt : LocalDateTime.now().format(DT_FMT));
        return row;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> snapshotList(Map<String, Object> snap, String key) {
        Object v = snap != null ? snap.get(key) : null;
        if (!(v instanceof List<?> list)) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                out.add((Map<String, Object>) m);
            }
        }
        return out;
    }

    private String mapStr(Map<String, Object> m, String key) {
        if (m == null || key == null) return "";
        Object v = m.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    private Map<String, Object> purchaseReqRow(PurchaseRequirement r) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("materialCode", r.getMaterialCode());
        row.put("materialName", r.getMaterialName());
        row.put("shortageQuantity", nz(r.getShortageQuantity()).stripTrailingZeros().toPlainString());
        row.put("suggestedPurchaseQuantity", nz(r.getSuggestedPurchaseQuantity()).stripTrailingZeros().toPlainString());
        row.put("supplierName", r.getSupplierName() != null ? r.getSupplierName() : "—");
        row.put("status", MesStatusMapper.toPurchaseDemandCn(nullToEmpty(r.getStatus())));
        row.put("expectedArrivalDate", fmtDate(r.getExpectedArrivalDate()));
        return row;
    }

    // ── helpers ────────────────────────────────────────────────

    private Map<String, Object> baseResult(String roleKey, String title, List<Map<String, Object>> metrics) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("roleKey", roleKey);
        m.put("title", title);
        m.put("refreshTime", LocalDateTime.now().format(DT_FMT));
        m.put("metrics", metrics);
        return m;
    }

    private Map<String, Object> metric(String key, String label, Object value, String suffix, boolean warn, String link) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("label", label);
        m.put("value", value);
        if (suffix != null) m.put("suffix", suffix);
        m.put("warn", warn);
        m.put("link", link);
        return m;
    }

    private Map<String, Object> metricEx(String key, String label, Object value, String suffix,
                                         String status, String delta, String link) {
        Map<String, Object> m = metric(key, label, value, suffix,
                "warn".equals(status) || "danger".equals(status), link);
        m.put("status", status != null ? status : "normal");
        if (delta != null) m.put("delta", delta);
        return m;
    }

    private Map<String, Object> panel(String key, String title, int span, String type, String link,
                                      List<String> categories, Object payload, String centerSub) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("key", key);
        p.put("title", title);
        p.put("span", span);
        p.put("type", type);
        if (link != null) p.put("link", link);
        if (categories != null) p.put("categories", categories);
        if (payload instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> m) {
            Object ct = m.get("chartType");
            if (ct != null || "line".equals(type) || "combo".equals(type) || "bar".equals(type)) {
                p.put("series", payload);
            } else {
                p.put("items", payload);
            }
        } else if (payload != null) {
            p.put("items", payload);
        }
        if (centerSub != null) p.put("centerSub", centerSub);
        return p;
    }

    private Map<String, Object> panelHBar(String key, String title, int span, String link,
                                          List<String> categories, List<Integer> values) {
        Map<String, Object> p = panel(key, title, span, "horizontalBar", link, categories, null, null);
        p.put("values", values);
        return p;
    }

    private Map<String, Object> panelHBar(String key, String title, int span, String link, List<Map<String, Object>> items) {
        Map<String, Object> p = panel(key, title, span, "horizontalBar", link, null, items, null);
        return p;
    }

    private Map<String, Object> panelLine(String key, String title, int span, String link,
                                          List<String> categories, List<Map<String, Object>> series, boolean wide) {
        Map<String, Object> p = panel(key, title, span, "line", link, categories, series, null);
        if (wide) p.put("wide", true);
        return p;
    }

    private Map<String, Object> panelCombo(String key, String title, int span, String link,
                                           List<String> categories, List<Map<String, Object>> series, boolean wide) {
        Map<String, Object> p = panel(key, title, span, "combo", link, categories, series, null);
        if (wide) p.put("wide", true);
        return p;
    }

    private Map<String, Object> panelGantt(String key, String title, int span, String link,
                                           List<Map<String, Object>> rows, int rangeStart, int rangeEnd) {
        Map<String, Object> p = panel(key, title, span, "gantt", link, null, null, null);
        p.put("rows", rows);
        p.put("rangeStart", rangeStart);
        p.put("rangeEnd", rangeEnd);
        return p;
    }

    private Map<String, Object> panelProgress(String key, String title, int span, String link, List<Map<String, Object>> progress) {
        Map<String, Object> p = panel(key, title, span, "progressList", link, null, null, null);
        p.put("progress", progress);
        return p;
    }

    private Map<String, Object> panelStatus(String key, String title, int span, String link, List<Map<String, Object>> statusList) {
        Map<String, Object> p = panel(key, title, span, "statusList", link, null, null, null);
        p.put("statusList", statusList);
        return p;
    }

    private Map<String, Object> statusRow(String label, String tag, String status, String value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("tag", tag);
        m.put("status", status);
        m.put("value", value);
        return m;
    }

    private Map<String, Object> progressRow(String label, int value, int max, String status, String color, String link) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("value", value);
        m.put("max", max);
        m.put("status", status);
        if (color != null) m.put("color", color);
        if (link != null) m.put("link", link);
        return m;
    }

    private List<Map<String, Object>> orderStatusOptions() {
        return ORDER_STATUS_ORDER.stream()
                .map(s -> {
                    Map<String, Object> o = new LinkedHashMap<>();
                    o.put("value", s);
                    o.put("label", MesStatusMapper.toOrderCn(s));
                    return o;
                }).toList();
    }

    private List<CustomerOrder> filterOrdersByStatus(List<CustomerOrder> orders, String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) return orders;
        return orders.stream().filter(o -> statusFilter.equals(o.getAuditStatus())).toList();
    }

    private long countLowStockMaterials() {
        return lowStockEntries().size();
    }

    private List<Map<String, Object>> lowStockItems(int limit) {
        return lowStockEntries().stream().limit(limit)
                .map(e -> chartItem(e.getKey(), e.getValue(), C_RED))
                .toList();
    }

    private List<Map.Entry<String, Integer>> lowStockEntries() {
        List<Inventory> invs = inventoryMapper.inventoryListWithMaterial();
        if (invs == null || invs.isEmpty()) {
            invs = inventoryMapper.inventoryList();
        }
        Map<Long, BigDecimal> qtyByMaterial = new HashMap<>();
        Map<Long, String> nameByMaterial = new HashMap<>();
        Map<Long, BigDecimal> safetyByMaterial = new HashMap<>();
        for (Inventory inv : invs) {
            Long mid = inv.getMaterialId();
            if (mid == null) continue;
            BigDecimal qty = inv.getQuantityAvailable() != null ? inv.getQuantityAvailable()
                    : (inv.getQuantityOnHand() != null ? inv.getQuantityOnHand() : BigDecimal.ZERO);
            qtyByMaterial.merge(mid, qty, BigDecimal::add);
            if (inv.getMaterialName() != null) nameByMaterial.put(mid, inv.getMaterialName());
            if (inv.getSafetyStock() != null && inv.getSafetyStock().compareTo(BigDecimal.ZERO) > 0) {
                safetyByMaterial.put(mid, inv.getSafetyStock());
            }
        }
        for (Material m : materialMapper.materialList()) {
            if (m.getMaterialId() == null) continue;
            nameByMaterial.putIfAbsent(m.getMaterialId(), m.getMaterialName());
            if (m.getSafetyStock() != null && m.getSafetyStock().compareTo(BigDecimal.ZERO) > 0) {
                safetyByMaterial.putIfAbsent(m.getMaterialId(), m.getSafetyStock());
            }
        }
        List<Map.Entry<String, Integer>> result = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : safetyByMaterial.entrySet()) {
            BigDecimal avail = qtyByMaterial.getOrDefault(e.getKey(), BigDecimal.ZERO);
            if (avail.compareTo(e.getValue()) < 0) {
                String name = nameByMaterial.getOrDefault(e.getKey(), "物料#" + e.getKey());
                if (name.length() > 10) name = name.substring(0, 10);
                int gap = e.getValue().subtract(avail).setScale(0, RoundingMode.UP).intValue();
                result.add(Map.entry(name, Math.max(1, gap)));
            }
        }
        result.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return result;
    }

    private List<Map<String, Object>> detectPlanConflicts(List<ProductionPlan> plans) {
        List<ProductionPlan> active = plans.stream()
                .filter(p -> p.getPlannedStartDate() != null && p.getPlannedEndDate() != null)
                .filter(p -> List.of("RELEASED", "EXECUTING", "RUNNING", "PUBLISHED", "SUBMITTED").contains(nullToEmpty(p.getPlanStatus())))
                .toList();
        List<Map<String, Object>> conflicts = new ArrayList<>();
        for (int i = 0; i < active.size(); i++) {
            ProductionPlan a = active.get(i);
            for (int j = i + 1; j < active.size(); j++) {
                ProductionPlan b = active.get(j);
                if (datesOverlap(a.getPlannedStartDate(), a.getPlannedEndDate(), b.getPlannedStartDate(), b.getPlannedEndDate())) {
                    conflicts.add(statusRow(a.getPlanNo() + " ↔ " + b.getPlanNo(), "日期重叠", "warn", fmtDate(a.getPlannedStartDate())));
                }
            }
        }
        return conflicts.stream().limit(5).toList();
    }

    private boolean datesOverlap(LocalDate s1, LocalDate e1, LocalDate s2, LocalDate e2) {
        return !s1.isAfter(e2) && !s2.isAfter(e1);
    }

    private List<Map<String, Object>> equipmentsFallbackStatus(Map<Long, Equipment> equipById) {
        return equipById.values().stream().limit(4).map(e -> {
            String st = nullToEmpty(e.getStatus());
            String tag = List.of("RUNNING", "PRODUCING", "BUSY").contains(st) ? "运行" : "空闲";
            return statusRow(e.getEquipmentName(), tag, "normal", e.getWorkstation());
        }).toList();
    }

    private Map<String, Object> chartItem(String name, int value, String color) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        m.put("color", color);
        return m;
    }

    private List<Map<String, Object>> chartItemsFromCount(Map<String, Long> count, List<String> order,
                                                          Map<String, String> colors,
                                                          java.util.function.Function<String, String> labelFn) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (String key : order) {
            long v = count.getOrDefault(key, 0L);
            if (v > 0) {
                items.add(chartItem(labelFn.apply(key), (int) v, colors.getOrDefault(key, pickColor(items.size()))));
            }
        }
        for (Map.Entry<String, Long> e : count.entrySet()) {
            if (!order.contains(e.getKey()) && e.getValue() > 0) {
                items.add(chartItem(labelFn.apply(e.getKey()), e.getValue().intValue(),
                        colors.getOrDefault(e.getKey(), pickColor(items.size()))));
            }
        }
        return items;
    }

    private Map<String, Object> donutChart(String key, String title, List<Map<String, Object>> items, String centerSub) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("key", key);
        c.put("title", title);
        c.put("type", "donut");
        c.put("items", items);
        c.put("centerSub", centerSub);
        return c;
    }

    private Map<String, Object> barChart(String key, String title, List<String> categories, List<Map<String, Object>> series) {
        return barChart(key, title, categories, series, false);
    }

    private Map<String, Object> barChart(String key, String title, List<String> categories, List<Map<String, Object>> series, boolean wide) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("key", key);
        c.put("title", title);
        c.put("type", "bar");
        c.put("categories", categories);
        c.put("series", series);
        if (wide) c.put("wide", true);
        return c;
    }

    private Map<String, Object> comboChart(String key, String title, List<String> categories, List<Map<String, Object>> series) {
        return comboChart(key, title, categories, series, false);
    }

    private Map<String, Object> comboChart(String key, String title, List<String> categories, List<Map<String, Object>> series, boolean wide) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("key", key);
        c.put("title", title);
        c.put("type", "combo");
        c.put("categories", categories);
        c.put("series", series);
        if (wide) c.put("wide", true);
        return c;
    }

    private Map<String, Object> lineChart(String key, String title, List<String> categories, List<Map<String, Object>> series) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("key", key);
        c.put("title", title);
        c.put("type", "line");
        c.put("categories", categories);
        c.put("series", series);
        return c;
    }

    private Map<String, Object> pieChart(String key, String title, List<Map<String, Object>> items) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("key", key);
        c.put("title", title);
        c.put("type", "pie");
        c.put("items", items);
        return c;
    }

    private Map<String, Object> barSeries(String name, List<?> data, String color) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("name", name);
        s.put("data", data);
        s.put("color", color);
        s.put("chartType", "bar");
        return s;
    }

    private Map<String, Object> lineSeries(String name, List<?> data, String color) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("name", name);
        s.put("data", data);
        s.put("color", color);
        s.put("chartType", "line");
        return s;
    }

    private Map<String, Object> table(String key, String title, String link, String linkText,
                                      List<Map<String, Object>> columns, List<Map<String, Object>> rows) {
        return table(key, title, link, linkText, columns, rows, null);
    }

    private Map<String, Object> table(String key, String title, String link, String linkText,
                                      List<Map<String, Object>> columns, List<Map<String, Object>> rows, Integer maxHeight) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("key", key);
        t.put("title", title);
        t.put("link", link);
        t.put("linkText", linkText);
        t.put("columns", columns);
        t.put("rows", rows);
        if (maxHeight != null) t.put("maxHeight", maxHeight);
        return t;
    }

    private Map<String, Object> col(String prop, String label, int width) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("prop", prop);
        c.put("label", label);
        c.put("width", width);
        return c;
    }

    private List<Map<String, Object>> orderColumns() {
        return List.of(
                col("orderNo", "订单号", 130),
                col("customerName", "客户", 100),
                col("productModel", "产品", 140),
                col("quantity", "数量", 70),
                col("amount", "金额", 90),
                col("deliveryDate", "交期", 100),
                col("status", "状态", 90)
        );
    }

    private List<Map<String, Object>> settlementColumns() {
        return List.of(
                col("settlementNo", "结算单号", 130),
                col("workOrderNo", "工单", 120),
                col("sourceTypeCn", "来源", 100),
                col("totalCost", "总成本", 90),
                col("settlementStatusCn", "状态", 90),
                col("createdAt", "创建时间", 150)
        );
    }

    private Map<String, Object> orderRow(CustomerOrder o, List<CustomerOrderItem> items) {
        CustomerOrderItem item = firstItem(items);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("orderNo", o.getOrderNo());
        row.put("customerName", o.getCustomerName());
        row.put("productModel", item != null ? item.getProductName() : "—");
        row.put("quantity", item != null ? intVal(item.getQuantity()) : 0);
        row.put("amount", "¥" + nz(o.getOrderAmount()).setScale(0, RoundingMode.HALF_UP));
        row.put("deliveryDate", fmtDate(o.getRequiredDeliveryDate()));
        row.put("status", MesStatusMapper.toOrderCn(o.getAuditStatus()));
        return row;
    }

    private Map<String, Object> logRow(OperationLog log) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("module", log.getModuleName());
        row.put("operationType", log.getOperationType());
        row.put("operationContent", log.getOperationContent());
        User u = log.getUserId() != null ? userMapper.getUserById(log.getUserId()) : null;
        row.put("operator", u != null ? u.getRealName() : "系统");
        row.put("operatedAt", fmtDateTime(log.getOperatedAt()));
        return row;
    }

    private Map<String, Object> dispatchRow(DispatchTask d, WorkOrder wo) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("dispatchNo", d.getDispatchNo());
        row.put("workOrderNo", wo != null ? wo.getWorkOrderNo() : "—");
        row.put("planQty", intVal(d.getAssignedQuantity()));
        row.put("completedQty", intVal(d.getCompletedQuantity()));
        row.put("status", MesStatusMapper.toDispatchCn(d.getStatus()));
        return row;
    }

    private Map<String, Object> settlementRow(Map<String, Object> s) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("settlementNo", s.get("settlementNo"));
        row.put("workOrderNo", s.getOrDefault("workOrderNo", "—"));
        row.put("sourceTypeCn", s.getOrDefault("sourceTypeCn", "—"));
        Object tc = s.get("totalCost");
        row.put("totalCost", tc != null ? "¥" + tc : "—");
        row.put("settlementStatusCn", s.getOrDefault("settlementStatusCn", "—"));
        row.put("createdAt", s.getOrDefault("createdAt", "—"));
        return row;
    }

    private String normalizeOrderKey(String auditStatus, String derived) {
        if (auditStatus != null && !auditStatus.isBlank()) return auditStatus;
        return switch (derived) {
            case "待审核" -> "PENDING";
            case "待计划" -> "PLAN_PENDING";
            case "已计划" -> "PLANNED";
            case "生产中" -> "PRODUCING";
            case "已发货" -> "SHIPPED";
            case "已作废" -> "REJECTED";
            default -> "APPROVED";
        };
    }

    private CustomerOrderItem firstItem(List<CustomerOrderItem> items) {
        return items != null && !items.isEmpty() ? items.get(0) : null;
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private BigDecimal nzDec(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private int intVal(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return 0;
        }
    }

    private String fmtAmt(BigDecimal v) {
        BigDecimal wan = v.divide(BigDecimal.valueOf(10000), 2, RoundingMode.HALF_UP);
        return wan.compareTo(BigDecimal.ZERO) == 0 ? "0" : wan.stripTrailingZeros().toPlainString();
    }

    private String fmtDate(LocalDate d) {
        return d != null ? d.format(DAY_FMT) : "—";
    }

    private String fmtDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(DT_FMT) : "—";
    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private List<Double> toDoubleList(List<Integer> ints) {
        return ints.stream().map(Integer::doubleValue).toList();
    }

    private String pickColor(int idx) {
        return pickIndustrialColor(idx);
    }

    private String pickIndustrialColor(int idx) {
        String[] palette = {C_BLUE, C_GREEN, C_ORANGE, C_PURPLE, C_CYAN, C_YELLOW, C_RED};
        return palette[Math.floorMod(idx, palette.length)];
    }

    private String attendanceStatusCn(String status) {
        if (status == null) return "—";
        return switch (status) {
            case "NORMAL" -> "正常";
            case "LATE" -> "迟到";
            case "EARLY_LEAVE" -> "早退";
            case "ABSENT" -> "缺勤";
            case "LEAVE" -> "请假";
            default -> status;
        };
    }
}
