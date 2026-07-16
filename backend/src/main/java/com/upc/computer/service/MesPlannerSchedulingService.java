package com.upc.computer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.computer.common.BusinessException;
import com.upc.computer.entity.*;
import com.upc.computer.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 计划员排产：订单上下文、三方案对比、冲突校验、工序排程持久化。
 */
@Service
public class MesPlannerSchedulingService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final double SHIFT_HOURS = 16.0;

    @Autowired
    private MesPlannerAgentService plannerAgentService;
    @Autowired
    private CustomerOrderMapper customerOrderMapper;
    @Autowired
    private CustomerOrderItemMapper customerOrderItemMapper;
    @Autowired
    private ProductionPlanMapper productionPlanMapper;
    @Autowired
    private ProductionPlanItemMapper productionPlanItemMapper;
    @Autowired
    private ProductionPlanScheduleMapper planScheduleMapper;
    @Autowired
    private ProductionPlanHistoryMapper planHistoryMapper;
    @Autowired
    private ProcessRouteMapper processRouteMapper;
    @Autowired
    private ProcessStepMapper processStepMapper;
    @Autowired
    private EquipmentMapper equipmentMapper;
    @Autowired
    private MaterialMapper materialMapper;
    @Autowired
    private InventoryMapper inventoryMapper;
    @Autowired
    private BomMapper bomMapper;
    @Autowired
    private DispatchTaskMapper dispatchTaskMapper;
    @Autowired
    private UserMapper userMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> previewOrderContext(String orderNo) {
        CustomerOrder order = findOrderByNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        CustomerOrderItem item = firstOrderItem(order.getOrderId());
        LocalDate delivery = order.getRequiredDeliveryDate();

        if (!isAgentSchedulable(order)) {
            return buildReadOnlyOrderContext(order, item, delivery);
        }

        LocalDate planStart = LocalDate.now().plusDays(1);
        LocalDate planEnd = delivery != null ? delivery.minusDays(2) : planStart.plusDays(21);
        if (planEnd.isBefore(planStart)) {
            planEnd = planStart.plusDays(14);
        }

        Map<String, Object> analysis = plannerAgentService.analyze(orderNo, planStart, planEnd);
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("orderId", orderNo);
        ctx.put("customerName", order.getCustomerName());
        ctx.put("productModel", item != null ? item.getProductName() : "");
        ctx.put("orderQuantity", item != null ? intVal(item.getQuantity()) : 0);
        ctx.put("deliveryDate", delivery != null ? delivery.format(DATE_FMT) : "");
        ctx.put("amount", order.getOrderAmount());
        ctx.put("inventory", analysis.get("inventoryCheck"));
        ctx.put("materialGaps", extractMaterialGaps(analysis));
        ctx.put("processRoute", loadProcessRouteForItem(item));
        ctx.put("capacityRisks", buildCapacityRisks(analysis));
        ctx.put("recommendedPlanQty", analysis.get("recommendedPlanQty"));
        ctx.put("recommendation", analysis.get("recommendation"));
        ctx.put("summary", analysis.get("summary"));
        ctx.put("readOnly", false);
        return ctx;
    }

    /** 仅待计划/已审核订单可走 Agent 排产分析 */
    private boolean isAgentSchedulable(CustomerOrder order) {
        if (order == null || order.getAuditStatus() == null) {
            return false;
        }
        String status = order.getAuditStatus().trim().toUpperCase();
        return "PLAN_PENDING".equals(status) || "APPROVED".equals(status)
                || "PASS".equals(status) || "PASSED".equals(status);
    }

    /** 已排产/生产中/已发货等订单：返回只读上下文，不触发 Agent 分析 */
    private Map<String, Object> buildReadOnlyOrderContext(CustomerOrder order, CustomerOrderItem item, LocalDate delivery) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("orderId", order.getOrderNo());
        ctx.put("customerName", order.getCustomerName());
        ctx.put("productModel", item != null ? item.getProductName() : "");
        ctx.put("orderQuantity", item != null ? intVal(item.getQuantity()) : 0);
        ctx.put("deliveryDate", delivery != null ? delivery.format(DATE_FMT) : "");
        ctx.put("amount", order.getOrderAmount());
        ctx.put("inventory", Map.of("materialChecks", List.of()));
        ctx.put("materialGaps", List.of());
        ctx.put("processRoute", loadProcessRouteForItem(item));
        ctx.put("capacityRisks", List.of());
        ctx.put("recommendedPlanQty", 0);
        ctx.put("recommendation", "订单已进入后续流程，仅展示跟踪信息");
        ctx.put("summary", "当前状态：" + auditStatusLabel(order.getAuditStatus()));
        ctx.put("readOnly", true);
        return ctx;
    }

    private String auditStatusLabel(String status) {
        if (status == null || status.isBlank()) {
            return "未知";
        }
        return switch (status.toUpperCase()) {
            case "PLAN_PENDING" -> "待计划";
            case "APPROVED", "PASS", "PASSED" -> "已审核";
            case "PLANNED" -> "已计划";
            case "PRODUCING" -> "生产中";
            case "SHIPPED", "DELIVERED" -> "已发货";
            case "COMPLETED" -> "已完成";
            default -> status;
        };
    }

    public Map<String, Object> compareSchemes(String orderNo, LocalDate planStart, LocalDate planEnd, int plannedQty) {
        if (planStart == null || planEnd == null || planEnd.isBefore(planStart)) {
            throw new BusinessException("请填写有效的计划周期");
        }
        Map<String, Object> base = plannerAgentService.analyze(orderNo, planStart, planEnd);
        int qty = plannedQty > 0 ? plannedQty : intVal(base.get("recommendedPlanQty"));
        if (qty <= 0) {
            qty = intVal(base.get("orderQuantity"));
        }
        final int planQty = qty;

        List<Map<String, Object>> schemes = List.of(
                buildScheme("DELIVERY", "交期优先", base, planQty, planStart, planEnd, 0.85, 1.15, 3),
                buildScheme("BALANCE", "负载均衡", base, planQty, planStart, planEnd, 1.0, 1.0, 5),
                buildScheme("COST", "成本优先", base, planQty, planStart, planEnd, 1.1, 0.82, 2)
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", orderNo);
        result.put("plannedQty", planQty);
        result.put("planStart", planStart.toString());
        result.put("planEnd", planEnd.toString());
        result.put("schemes", schemes);
        result.put("conclusion", pickBestScheme(schemes));
        result.put("evidence", List.of(
                Map.of("label", "订单数量", "value", planQty + " 台"),
                Map.of("label", "计划周期", "value", ChronoUnit.DAYS.between(planStart, planEnd) + 1 + " 天"),
                Map.of("label", "物料结论", "value", String.valueOf(base.getOrDefault("recommendation", "")))
        ));
        result.put("analysisCollapsed", true);
        result.put("schedulingSteps", base.get("schedulingSteps"));
        result.put("dataSource", "live");
        result.put("dataNote", "基于订单、库存、BOM、设备、工艺路线实时计算；方案权重（交期/均衡/成本）为策略参数");
        return result;
    }

    /** 方案对比校验：复用 Agent 分析结果，避免重复判定物料 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> validatePlanForScheme(Map<String, Object> payload, Map<String, Object> analysis) {
        List<Map<String, Object>> schedules = castList(payload.get("schedules"));
        String orderNo = str(payload, "orderId");
        LocalDate planEnd = parseDate(str(payload, "planEnd"));
        int plannedQty = intVal(payload.get("plannedQty"));
        List<Map<String, Object>> conflicts = new ArrayList<>();

        CustomerOrder order = findOrderByNo(orderNo);
        if (order != null && order.getRequiredDeliveryDate() != null && planEnd != null
                && planEnd.isAfter(order.getRequiredDeliveryDate())) {
            conflicts.add(conflict("warning", "delivery_delay", "交期偏紧",
                    "计划结束 " + planEnd + " 晚于订单交期 " + order.getRequiredDeliveryDate() + "，请确认是否加急"));
        }

        Object inv = analysis.get("inventoryCheck");
        if (inv instanceof Map<?, ?> invMap) {
            Object checks = invMap.get("materialChecks");
            if (checks instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> raw) {
                        Map<String, Object> m = (Map<String, Object>) raw;
                        if (isRawMaterialShortage(m)) {
                            conflicts.add(conflict("warning", "material_shortage", "物料不足",
                                    m.get("materialName") + " 缺口 " + m.get("shortage") + "，可先排产并同步采购"));
                        }
                    }
                }
            }
        }

        conflicts.addAll(validateScheduleRows(schedules, plannedQty));

        boolean hasBlockingDanger = hasBlockingDanger(conflicts);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conflicts", conflicts);
        result.put("hasDanger", hasBlockingDanger);
        result.put("canSubmit", !hasBlockingDanger);
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> validatePlan(Map<String, Object> payload) {
        List<Map<String, Object>> schedules = castList(payload.get("schedules"));
        String orderNo = str(payload, "orderId");
        LocalDate planEnd = parseDate(str(payload, "planEnd"));
        int plannedQty = intVal(payload.get("plannedQty"));
        List<Map<String, Object>> conflicts = new ArrayList<>();

        CustomerOrder order = findOrderByNo(orderNo);
        CustomerOrderItem item = order != null ? firstOrderItem(order.getOrderId()) : null;
        if (order != null && order.getRequiredDeliveryDate() != null && planEnd != null
                && planEnd.isAfter(order.getRequiredDeliveryDate())) {
            conflicts.add(conflict("warning", "delivery_delay", "交期偏紧",
                    "计划结束 " + planEnd + " 晚于订单交期 " + order.getRequiredDeliveryDate() + "，请确认是否加急"));
        }

        if (item != null && plannedQty > 0 && order != null && isAgentSchedulable(order)) {
            Map<String, Object> analysis = plannerAgentService.analyze(orderNo,
                    parseDate(str(payload, "planStart", LocalDate.now().plusDays(1).toString())),
                    planEnd != null ? planEnd : LocalDate.now().plusDays(21));
            conflicts.addAll(extractMaterialGapConflicts(analysis));
        }

        conflicts.addAll(validateScheduleRows(schedules, plannedQty));

        boolean hasBlockingDanger = hasBlockingDanger(conflicts);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conflicts", conflicts);
        result.put("hasDanger", hasBlockingDanger);
        result.put("canSubmit", !hasBlockingDanger);
        return result;
    }

    @Transactional
    public Map<String, Object> savePlanWithSchedule(Map<String, Object> payload, String operator) {
        Map<String, Object> validation = validatePlan(payload);
        String saveAction = str(payload, "saveAction", "draft");
        if ("submit".equals(saveAction) && hasBlockingDanger(castList(validation.get("conflicts")))) {
            throw new BusinessException("存在严重冲突，禁止提交计划");
        }

        String orderNo = str(payload, "orderId");
        CustomerOrder order = findOrderByNo(orderNo);
        if (order == null || !List.of("PLAN_PENDING", "APPROVED").contains(order.getAuditStatus())) {
            throw new BusinessException("订单状态不允许创建计划");
        }

        LocalDateTime now = LocalDateTime.now();
        User planner = findUserByUsername(operator);
        String planNo = nextPlanNo();
        int plannedQty = intVal(payload.get("plannedQty"));
        if (plannedQty <= 0) {
            throw new BusinessException("计划数量必须大于0");
        }

        ProductionPlan plan = new ProductionPlan();
        plan.setPlanNo(planNo);
        plan.setPlanName("计划-" + orderNo);
        plan.setSourceOrderId(order.getOrderId());
        plan.setPlannedStartDate(parseDate(str(payload, "planStart")));
        plan.setPlannedEndDate(parseDate(str(payload, "planEnd")));
        plan.setPriority(str(payload, "priority", "NORMAL"));
        plan.setPlanStatus("DRAFT");
        plan.setPlannerId(planner != null ? planner.getUserId() : null);
        plan.setRemark(str(payload, "remark"));
        plan.setVersionNo(str(payload, "versionNo", "V1"));
        plan.setParentPlanNo(str(payload, "parentPlanNo"));
        plan.setAdjustReason(str(payload, "adjustReason"));
        plan.setSchedulingMode(str(payload, "schedulingMode", "MANUAL"));
        plan.setCreatedAt(now);
        plan.setUpdatedAt(now);
        productionPlanMapper.insertPlan(plan);

        CustomerOrderItem orderItem = firstOrderItem(order.getOrderId());
        if (orderItem != null) {
            ProductionPlanItem planItem = new ProductionPlanItem();
            planItem.setPlanId(plan.getPlanId());
            planItem.setOrderItemId(orderItem.getOrderItemId());
            planItem.setMaterialId(orderItem.getMaterialId());
            planItem.setPlannedQuantity(BigDecimal.valueOf(plannedQty));
            planItem.setCompletedQuantity(BigDecimal.ZERO);
            planItem.setPlannedStartDate(plan.getPlannedStartDate());
            planItem.setPlannedEndDate(plan.getPlannedEndDate());
            planItem.setItemStatus("PENDING");
            planItem.setCreatedAt(now);
            planItem.setUpdatedAt(now);
            productionPlanItemMapper.insertPlanItem(planItem);
        }

        order.setAuditStatus("PLANNED");
        order.setUpdatedAt(now);
        customerOrderMapper.updateCustomerOrder(order);

        persistSchedules(plan.getPlanId(), castList(payload.get("schedules")), now);
        appendHistory(plan, "CREATE", str(payload, "adjustReason"), operator, payload, now);

        if ("publish".equals(saveAction) || "submit".equals(saveAction)) {
            plan.setPlanStatus("PUBLISHED");
            plan.setUpdatedAt(now);
            productionPlanMapper.updatePlan(plan);
            appendHistory(plan, "PUBLISH", null, operator, payload, now);
        }
        if ("submit".equals(saveAction)) {
            plan.setPlanStatus("SUBMITTED");
            plan.setApprovedAt(now);
            plan.setUpdatedAt(now);
            productionPlanMapper.updatePlan(plan);
            appendHistory(plan, "SUBMIT", null, operator, payload, now);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", planNo);
        result.put("planId", planNo);
        result.put("status", MesStatusMapper.toPlanCn(plan.getPlanStatus()));
        result.put("conflicts", validation.get("conflicts"));
        result.put("message", "计划已保存");
        return result;
    }

    /** 多订单智能排产：支持同型号联合批次（合计按上限拆分）或单订单独立拆分 */
    @Transactional
    public Map<String, Object> saveBatchPlans(Map<String, Object> payload, String operator) {
        List<Map<String, Object>> orders = castList(payload.get("orders"));
        if (orders.isEmpty()) {
            throw new BusinessException("请至少勾选一个订单");
        }
        int batchSize = intVal(payload.get("batchSize"));
        if (batchSize <= 0) {
            batchSize = 500;
        }
        LocalDate planStart = parseDate(str(payload, "planStart"));
        LocalDate planEnd = parseDate(str(payload, "planEnd"));
        if (planStart == null || planEnd == null || planEnd.isBefore(planStart)) {
            throw new BusinessException("请填写有效的计划周期");
        }
        String mode = str(payload, "schedulingMode", "DELIVERY");
        String saveAction = str(payload, "saveAction", "submit");
        boolean combinedBatch = Boolean.TRUE.equals(payload.get("combinedBatch")) && orders.size() > 1;
        LocalDateTime now = LocalDateTime.now();
        User planner = findUserByUsername(operator);

        List<Map<String, Object>> created = new ArrayList<>();
        if (combinedBatch) {
            created.addAll(saveCombinedBatchPlans(orders, batchSize, planStart, planEnd, mode, saveAction, operator, planner, now, payload));
        } else {
            for (Map<String, Object> o : orders) {
                created.addAll(saveSingleOrderBatchPlans(o, batchSize, planStart, planEnd, mode, saveAction, operator, planner, now, null, payload));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("plans", created);
        result.put("totalPlans", created.size());
        result.put("orderCount", orders.size());
        result.put("batchSize", batchSize);
        result.put("combinedBatch", combinedBatch);
        result.put("message", combinedBatch
                ? "已生成 " + created.size() + " 个联合批次计划并提交生产主管"
                : "已生成 " + created.size() + " 个批次计划");
        return result;
    }

    private List<Map<String, Object>> saveCombinedBatchPlans(
            List<Map<String, Object>> orders,
            int batchSize,
            LocalDate planStart,
            LocalDate planEnd,
            String mode,
            String saveAction,
            String operator,
            User planner,
            LocalDateTime now,
            Map<String, Object> payload) {
        List<OrderAlloc> allocs = new ArrayList<>();
        String productModel = null;
        for (Map<String, Object> o : orders) {
            String orderNo = str(o, "orderId");
            CustomerOrder order = findOrderByNo(orderNo);
            if (order == null) {
                throw new BusinessException("订单不存在：" + orderNo);
            }
            if (!List.of("PLAN_PENDING", "APPROVED").contains(order.getAuditStatus())) {
                throw new BusinessException("订单 " + orderNo + " 状态不允许创建计划");
            }
            CustomerOrderItem item = firstOrderItem(order.getOrderId());
            String model = item != null && item.getProductName() != null ? item.getProductName() : "";
            if (productModel == null) {
                productModel = model;
            } else if (!productModel.equals(model)) {
                throw new BusinessException("联合排产须选择相同型号的订单");
            }
            int totalQty = intVal(o.get("plannedQty"));
            if (totalQty <= 0 && item != null) {
                totalQty = intVal(item.getQuantity());
            }
            if (totalQty <= 0) {
                throw new BusinessException("订单 " + orderNo + " 计划数量无效");
            }
            allocs.add(new OrderAlloc(order, item, orderNo, totalQty));
        }

        allocs.sort(Comparator.comparing(a -> a.order.getRequiredDeliveryDate(), Comparator.nullsLast(Comparator.naturalOrder())));

        int totalQty = allocs.stream().mapToInt(a -> a.remaining).sum();
        int batchCount = (totalQty + batchSize - 1) / batchSize;
        long totalDays = Math.max(1, ChronoUnit.DAYS.between(planStart, planEnd) + 1);
        long daysPerBatch = Math.max(1, totalDays / batchCount);

        Map<String, Object> base = plannerAgentService.analyze(allocs.get(0).orderNo, planStart, planEnd);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> suggestions =
                (List<Map<String, Object>>) base.getOrDefault("dispatchSuggestions", List.of());

        List<Map<String, Object>> created = new ArrayList<>();
        int globalLeft = totalQty;
        int allocIdx = 0;
        int allocRemain = allocs.get(0).remaining;

        for (int b = 1; b <= batchCount; b++) {
            int batchCapacity = Math.min(batchSize, globalLeft);
            globalLeft -= batchCapacity;
            LocalDate bStart = planStart.plusDays((long) (b - 1) * daysPerBatch);
            if (bStart.isAfter(planEnd)) {
                bStart = planEnd;
            }
            LocalDate bEnd = b == batchCount ? planEnd : bStart.plusDays(daysPerBatch - 1);
            if (bEnd.isAfter(planEnd)) {
                bEnd = planEnd;
            }
            if (bEnd.isBefore(bStart)) {
                bEnd = bStart;
            }

            int capacityLeft = batchCapacity;
            while (capacityLeft > 0 && allocIdx < allocs.size()) {
                OrderAlloc alloc = allocs.get(allocIdx);
                int take = Math.min(allocRemain, capacityLeft);
                String remark = "同型号联合排产 · 联合批次 " + b + "/" + batchCount + "（单批上限 " + batchSize + " 台）";
                created.add(createBatchPlan(
                        alloc.order, alloc.item, alloc.orderNo, take, b, batchCount,
                        bStart, bEnd, mode, saveAction, operator, planner, now, suggestions, remark, payload));
                capacityLeft -= take;
                allocRemain -= take;
                if (allocRemain <= 0) {
                    allocIdx++;
                    allocRemain = allocIdx < allocs.size() ? allocs.get(allocIdx).remaining : 0;
                }
            }
        }

        for (OrderAlloc alloc : allocs) {
            alloc.order.setAuditStatus("PLANNED");
            alloc.order.setUpdatedAt(now);
            customerOrderMapper.updateCustomerOrder(alloc.order);
        }
        return created;
    }

    private List<Map<String, Object>> saveSingleOrderBatchPlans(
            Map<String, Object> o,
            int batchSize,
            LocalDate planStart,
            LocalDate planEnd,
            String mode,
            String saveAction,
            String operator,
            User planner,
            LocalDateTime now,
            List<Map<String, Object>> sharedSuggestions,
            Map<String, Object> payload) {
        String orderNo = str(o, "orderId");
        CustomerOrder order = findOrderByNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在：" + orderNo);
        }
        if (!List.of("PLAN_PENDING", "APPROVED").contains(order.getAuditStatus())) {
            throw new BusinessException("订单 " + orderNo + " 状态不允许创建计划");
        }
        CustomerOrderItem item = firstOrderItem(order.getOrderId());
        int totalQty = intVal(o.get("plannedQty"));
        if (totalQty <= 0 && item != null) {
            totalQty = intVal(item.getQuantity());
        }
        if (totalQty <= 0) {
            throw new BusinessException("订单 " + orderNo + " 计划数量无效");
        }

        List<Map<String, Object>> suggestions = sharedSuggestions;
        if (suggestions == null) {
            Map<String, Object> base = plannerAgentService.analyze(orderNo, planStart, planEnd);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fromBase =
                    (List<Map<String, Object>>) base.getOrDefault("dispatchSuggestions", List.of());
            suggestions = fromBase;
        }

        int batchCount = (totalQty + batchSize - 1) / batchSize;
        long totalDays = Math.max(1, ChronoUnit.DAYS.between(planStart, planEnd) + 1);
        long daysPerBatch = Math.max(1, totalDays / batchCount);

        List<Map<String, Object>> created = new ArrayList<>();
        int remaining = totalQty;
        for (int b = 1; b <= batchCount; b++) {
            int batchQty = Math.min(batchSize, remaining);
            remaining -= batchQty;
            LocalDate bStart = planStart.plusDays((long) (b - 1) * daysPerBatch);
            if (bStart.isAfter(planEnd)) {
                bStart = planEnd;
            }
            LocalDate bEnd = b == batchCount ? planEnd : bStart.plusDays(daysPerBatch - 1);
            if (bEnd.isAfter(planEnd)) {
                bEnd = planEnd;
            }
            if (bEnd.isBefore(bStart)) {
                bEnd = bStart;
            }
            String remark = "智能排产 · 批次 " + b + "/" + batchCount + "（单批上限 " + batchSize + " 台）";
            created.add(createBatchPlan(
                    order, item, orderNo, batchQty, b, batchCount,
                    bStart, bEnd, mode, saveAction, operator, planner, now, suggestions, remark, payload));
        }

        order.setAuditStatus("PLANNED");
        order.setUpdatedAt(now);
        customerOrderMapper.updateCustomerOrder(order);
        return created;
    }

    private Map<String, Object> createBatchPlan(
            CustomerOrder order,
            CustomerOrderItem item,
            String orderNo,
            int batchQty,
            int batchNo,
            int batchCount,
            LocalDate bStart,
            LocalDate bEnd,
            String mode,
            String saveAction,
            String operator,
            User planner,
            LocalDateTime now,
            List<Map<String, Object>> suggestions,
            String remark,
            Map<String, Object> payload) {
        List<Map<String, Object>> schedules = buildSchedulesFromSuggestions(
                suggestions, batchQty, bStart, bEnd, mode);

        String planNo = nextPlanNo();
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanNo(planNo);
        plan.setPlanName("计划-" + orderNo + (batchCount > 1 ? "-批次" + batchNo : ""));
        plan.setSourceOrderId(order.getOrderId());
        plan.setPlannedStartDate(bStart);
        plan.setPlannedEndDate(bEnd);
        plan.setPriority(str(payload, "priority", "NORMAL"));
        plan.setPlanStatus("DRAFT");
        plan.setPlannerId(planner != null ? planner.getUserId() : null);
        plan.setRemark(remark);
        plan.setVersionNo("V1");
        plan.setSchedulingMode(mode);
        plan.setCreatedAt(now);
        plan.setUpdatedAt(now);
        productionPlanMapper.insertPlan(plan);

        if (item != null) {
            ProductionPlanItem planItem = new ProductionPlanItem();
            planItem.setPlanId(plan.getPlanId());
            planItem.setOrderItemId(item.getOrderItemId());
            planItem.setMaterialId(item.getMaterialId());
            planItem.setPlannedQuantity(BigDecimal.valueOf(batchQty));
            planItem.setCompletedQuantity(BigDecimal.ZERO);
            planItem.setPlannedStartDate(bStart);
            planItem.setPlannedEndDate(bEnd);
            planItem.setItemStatus("PENDING");
            planItem.setCreatedAt(now);
            planItem.setUpdatedAt(now);
            productionPlanItemMapper.insertPlanItem(planItem);
        }

        persistSchedules(plan.getPlanId(), schedules, now);
        appendHistory(plan, "CREATE", remark, operator, payload, now);

        if ("publish".equals(saveAction) || "submit".equals(saveAction)) {
            plan.setPlanStatus("PUBLISHED");
            plan.setUpdatedAt(now);
            productionPlanMapper.updatePlan(plan);
            appendHistory(plan, "PUBLISH", null, operator, payload, now);
        }
        if ("submit".equals(saveAction)) {
            plan.setPlanStatus("SUBMITTED");
            plan.setApprovedAt(now);
            plan.setUpdatedAt(now);
            productionPlanMapper.updatePlan(plan);
            appendHistory(plan, "SUBMIT", null, operator, payload, now);
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("planId", planNo);
        row.put("orderId", orderNo);
        row.put("batchNo", batchNo);
        row.put("batchCount", batchCount);
        row.put("quantity", batchQty);
        row.put("planStart", bStart.toString());
        row.put("planEnd", bEnd.toString());
        return row;
    }

    private static final class OrderAlloc {
        private final CustomerOrder order;
        private final CustomerOrderItem item;
        private final String orderNo;
        private final int remaining;

        private OrderAlloc(CustomerOrder order, CustomerOrderItem item, String orderNo, int remaining) {
            this.order = order;
            this.item = item;
            this.orderNo = orderNo;
            this.remaining = remaining;
        }
    }

    @Transactional
    public Map<String, Object> copyPlan(String planNo, String operator) {
        ProductionPlan source = findPlanByNo(planNo);
        if (source == null) {
            throw new BusinessException("计划不存在");
        }
        List<ProductionPlanSchedule> schedules = planScheduleMapper.listByPlanId(source.getPlanId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderNoOfPlan(source));
        payload.put("plannedQty", plannedQtyOfPlan(source));
        payload.put("planStart", source.getPlannedStartDate() != null ? source.getPlannedStartDate().toString() : "");
        payload.put("planEnd", source.getPlannedEndDate() != null ? source.getPlannedEndDate().toString() : "");
        payload.put("priority", source.getPriority());
        payload.put("remark", "复制自 " + planNo);
        payload.put("parentPlanNo", planNo);
        payload.put("versionNo", nextVersion(source.getVersionNo()));
        payload.put("schedulingMode", source.getSchedulingMode());
        payload.put("saveAction", "draft");
        payload.put("schedules", schedules.stream().map(this::scheduleToMap).toList());
        return savePlanWithSchedule(payload, operator);
    }

    public Map<String, Object> loadManualWizardContext(String orderNo, int plannedQty) {
        CustomerOrder order = findOrderByNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        CustomerOrderItem item = firstOrderItem(order.getOrderId());
        List<Map<String, Object>> steps = loadProcessRouteForItem(item);
        List<Equipment> equipment = equipmentMapper.equipmentList();
        List<Map<String, Object>> workshops = ProductionWorkshopCatalog.allWorkshops().stream()
                .map(w -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("key", w.key());
                    m.put("name", w.workshopName());
                    m.put("department", w.department());
                    return m;
                }).toList();

        int qty = plannedQty > 0 ? plannedQty : (item != null ? intVal(item.getQuantity()) : 0);
        List<Map<String, Object>> defaultSchedules = buildDefaultSchedules(steps, qty, equipment);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", orderNo);
        result.put("plannedQty", qty);
        result.put("steps", steps);
        result.put("workshops", workshops);
        result.put("equipment", equipment.stream().map(this::equipmentRow).toList());
        result.put("schedules", defaultSchedules);
        result.put("estimatedDays", estimateDays(defaultSchedules, qty));
        return result;
    }

    public List<Map<String, Object>> listPlanSchedules(String planNo) {
        ProductionPlan plan = findPlanByNo(planNo);
        if (plan == null) {
            return List.of();
        }
        return planScheduleMapper.listByPlanId(plan.getPlanId()).stream()
                .map(this::scheduleToMap).toList();
    }

    public List<Map<String, Object>> listPlanHistory(String planNo) {
        return planHistoryMapper.listByPlanNo(planNo).stream().map(h -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", h.getHistoryId());
            m.put("versionNo", h.getVersionNo());
            m.put("actionType", h.getActionType());
            m.put("reason", h.getReason());
            m.put("operatorName", h.getOperatorName());
            m.put("createdAt", h.getCreatedAt() != null ? h.getCreatedAt().format(DT_FMT) : "");
            return m;
        }).toList();
    }

    // —— helpers ——

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildScheme(String key, String label, Map<String, Object> base,
                                              int qty, LocalDate planStart, LocalDate planEnd,
                                              double durationFactor, double utilFactor, int lineChanges) {
        long baseDays = Math.max(1, ChronoUnit.DAYS.between(planStart, planEnd) + 1);
        long finishDays = Math.max(1, Math.round(baseDays * durationFactor));
        LocalDate finishDate = planStart.plusDays(finishDays - 1);
        int utilization = (int) Math.min(98, Math.round(62 * utilFactor + (qty / 50.0)));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> suggestions = (List<Map<String, Object>>) base.getOrDefault("dispatchSuggestions", List.of());
        List<Map<String, Object>> schedules = buildSchedulesFromSuggestions(suggestions, qty, planStart, finishDate, key);

        Map<String, Object> validation = new LinkedHashMap<>();
        validation.put("orderId", base.get("orderId"));
        validation.put("planStart", planStart.toString());
        validation.put("planEnd", finishDate.toString());
        validation.put("plannedQty", qty);
        validation.put("schedules", schedules);
        Map<String, Object> conflicts = validatePlanForScheme(validation, base);

        Map<String, Object> scheme = new LinkedHashMap<>();
        scheme.put("key", key);
        scheme.put("label", label);
        scheme.put("finishDate", finishDate.toString());
        scheme.put("equipmentUtilization", utilization);
        scheme.put("materialShortage", countMaterialShortage(base));
        scheme.put("delayDays", Math.max(0, (int) ChronoUnit.DAYS.between(
                parseDate(String.valueOf(base.getOrDefault("planEnd", planEnd))), finishDate)));
        scheme.put("lineChanges", lineChanges);
        scheme.put("schedules", schedules);
        scheme.put("conflicts", conflicts.get("conflicts"));
        scheme.put("canSubmit", conflicts.get("canSubmit"));
        scheme.put("summary", label + "：预计 " + finishDate + " 完工，设备利用率约 " + utilization + "%");
        return scheme;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildSchedulesFromSuggestions(List<Map<String, Object>> suggestions,
                                                                    int qty, LocalDate start, LocalDate end, String schemeKey) {
        if (suggestions.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        LocalDate cursor = start;
        int idx = 0;
        for (Map<String, Object> s : suggestions) {
            long remainingSteps = suggestions.size() - idx;
            long remainingDays = ChronoUnit.DAYS.between(cursor, end) + 1;
            if (remainingDays <= 0) {
                break;
            }
            long stepDays = Math.max(1, remainingDays / remainingSteps);
            if ("DELIVERY".equals(schemeKey)) {
                stepDays = Math.max(1, remainingDays / (remainingSteps + 1L));
            }

            int splitQty = qty;
            if ("BALANCE".equals(schemeKey) && suggestions.size() > 1) {
                splitQty = (int) Math.ceil(qty / (double) suggestions.size());
            } else if ("COST".equals(schemeKey) && idx > 0) {
                splitQty = Math.max(1, qty / suggestions.size());
            }

            LocalDate rowStart = cursor;
            LocalDate rowEnd = rowStart.plusDays(stepDays - 1);
            if (rowEnd.isAfter(end)) {
                rowEnd = end;
            }
            if (rowEnd.isBefore(rowStart)) {
                rowEnd = rowStart;
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stepName", s.get("processStep"));
            row.put("workshop", s.get("workshop"));
            row.put("equipmentCode", s.get("equipmentCode"));
            row.put("plannedQuantity", splitQty);
            row.put("plannedStart", rowStart.atTime(8, 0).format(DT_FMT));
            row.put("plannedEnd", rowEnd.atTime(18, 0).format(DT_FMT));
            row.put("standardHours", 1.0);
            rows.add(row);

            long advance = "BALANCE".equals(schemeKey) ? 2L : 1L;
            cursor = rowEnd.plusDays(advance);
            idx++;
        }
        return collapseAssemblySchedules(rows, qty);
    }

    /** 组装工序只保留一个车间排程（业务规则：整机组装由单一车间完成） */
    private List<Map<String, Object>> collapseAssemblySchedules(List<Map<String, Object>> rows, int plannedQty) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        int i = 0;
        while (i < rows.size()) {
            Map<String, Object> row = rows.get(i);
            String stepName = str(row, "stepName");
            if (!isAssemblyStepName(stepName)) {
                result.add(row);
                i++;
                continue;
            }
            Map<String, Object> merged = new LinkedHashMap<>(row);
            LocalDateTime minStart = parseDateTime(str(row, "plannedStart"));
            LocalDateTime maxEnd = parseDateTime(str(row, "plannedEnd"));
            i++;
            while (i < rows.size() && isAssemblyStepName(str(rows.get(i), "stepName"))
                    && stepName.equals(str(rows.get(i), "stepName"))) {
                Map<String, Object> next = rows.get(i);
                LocalDateTime s = parseDateTime(str(next, "plannedStart"));
                LocalDateTime e = parseDateTime(str(next, "plannedEnd"));
                if (s != null && (minStart == null || s.isBefore(minStart))) {
                    minStart = s;
                }
                if (e != null && (maxEnd == null || e.isAfter(maxEnd))) {
                    maxEnd = e;
                }
                i++;
            }
            if (plannedQty > 0) {
                merged.put("plannedQuantity", plannedQty);
            }
            if (minStart != null) {
                merged.put("plannedStart", minStart.format(DT_FMT));
            }
            if (maxEnd != null) {
                if (minStart != null && maxEnd.isBefore(minStart)) {
                    maxEnd = minStart;
                }
                merged.put("plannedEnd", maxEnd.format(DT_FMT));
            }
            result.add(merged);
        }
        return result;
    }

    private boolean isAssemblyStepName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String n = name.trim();
        return n.contains("整机组装") || "组装".equals(n) || n.contains("背光组装");
    }

    private boolean hasBlockingDanger(List<Map<String, Object>> conflicts) {
        if (conflicts == null) {
            return false;
        }
        Set<String> blocking = Set.of("invalid_qty", "time_reverse", "process_order");
        return conflicts.stream().anyMatch(c ->
                "danger".equals(c.get("level")) && blocking.contains(String.valueOf(c.get("code"))));
    }

    private List<Map<String, Object>> buildDefaultSchedules(List<Map<String, Object>> steps, int qty,
                                                            List<Equipment> equipment) {
        List<Map<String, Object>> rows = new ArrayList<>();
        LocalDate start = LocalDate.now().plusDays(1);
        int dayOffset = 0;
        for (Map<String, Object> step : steps) {
            String eqType = String.valueOf(step.getOrDefault("standardEquipmentType", ""));
            Equipment eq = equipment.stream()
                    .filter(e -> eqType.isBlank() || eqType.equals(e.getEquipmentType()))
                    .findFirst().orElse(equipment.isEmpty() ? null : equipment.get(0));
            double hours = doubleVal(step.get("standardWorkHours"));
            if (hours <= 0) {
                hours = 1.0;
            }
            long stepDays = Math.max(1, (long) Math.ceil(hours * qty / SHIFT_HOURS));
            LocalDate rowStart = start.plusDays(dayOffset);
            LocalDate rowEnd = rowStart.plusDays(stepDays - 1);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stepId", step.get("stepId"));
            row.put("stepNo", step.get("stepNo"));
            row.put("stepName", step.get("stepName"));
            row.put("workshop", eq != null && eq.getWorkshop() != null ? eq.getWorkshop() : "默认车间");
            row.put("equipmentId", eq != null ? eq.getEquipmentId() : null);
            row.put("equipmentCode", eq != null ? eq.getEquipmentCode() : "");
            row.put("plannedQuantity", qty);
            row.put("plannedStart", rowStart.atTime(8, 0).format(DT_FMT));
            row.put("plannedEnd", rowEnd.atTime(18, 0).format(DT_FMT));
            row.put("standardHours", hours);
            rows.add(row);
            dayOffset += (int) stepDays;
        }
        return rows;
    }

    private int estimateDays(List<Map<String, Object>> schedules, int qty) {
        double totalHours = 0;
        for (Map<String, Object> row : schedules) {
            totalHours += doubleVal(row.get("standardHours")) * qty;
        }
        return (int) Math.max(1, Math.ceil(totalHours / SHIFT_HOURS));
    }

    private List<Map<String, Object>> validateScheduleRows(List<Map<String, Object>> schedules, int plannedQty) {
        List<Map<String, Object>> conflicts = new ArrayList<>();
        if (schedules == null || schedules.isEmpty()) {
            conflicts.add(conflict("warning", "empty_schedule", "排程为空", "未配置工序排程明细"));
            return conflicts;
        }
        LocalDateTime prevEnd = null;
        int sort = 0;
        for (Map<String, Object> row : schedules) {
            sort++;
            int rowQty = intVal(row.get("plannedQuantity"));
            if (rowQty <= 0) {
                conflicts.add(conflict("danger", "invalid_qty", "数量异常", "工序 " + row.get("stepName") + " 数量无效"));
            } else if (plannedQty > 0 && rowQty > plannedQty * 1.2) {
                conflicts.add(conflict("warning", "qty_overflow", "数量偏差", "工序数量合计超过计划量"));
            }
            LocalDateTime start = parseDateTime(str(row, "plannedStart"));
            LocalDateTime end = parseDateTime(str(row, "plannedEnd"));
            if (start != null && end != null && end.isBefore(start)) {
                conflicts.add(conflict("danger", "time_reverse", "工序顺序错误",
                        row.get("stepName") + " 结束时间早于开始时间"));
            }
            if (prevEnd != null && start != null && start.isBefore(prevEnd)) {
                conflicts.add(conflict("danger", "process_order", "工序顺序错误",
                        row.get("stepName") + " 开始时间早于上道工序结束"));
            }
            if (end != null) {
                prevEnd = end;
            }
            String equipmentCode = str(row, "equipmentCode");
            if (!equipmentCode.isBlank()) {
                conflicts.addAll(checkEquipmentConflict(equipmentCode, start, end));
            }
            String workshop = str(row, "workshop");
            if (!workshop.isBlank() && rowQty > 300) {
                conflicts.add(conflict("warning", "workshop_overload", "车间超负荷",
                        workshop + " 分配 " + rowQty + " 台，建议拆分到多个车间"));
            }
        }
        return conflicts;
    }

    private List<Map<String, Object>> checkEquipmentConflict(String equipmentCode, LocalDateTime start, LocalDateTime end) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (start == null || end == null) {
            return list;
        }
        LocalDateTime scheduleStart = start;
        LocalDateTime scheduleEnd = end;
        LocalDateTime now = LocalDateTime.now();
        Set<Long> equipmentIds = equipmentMapper.equipmentList().stream()
                .filter(e -> equipmentCode.equals(e.getEquipmentCode()))
                .map(Equipment::getEquipmentId)
                .collect(Collectors.toSet());
        if (equipmentIds.isEmpty()) {
            return list;
        }
        for (DispatchTask task : dispatchTaskMapper.dispatchList()) {
            if (task.getEquipmentId() == null || !equipmentIds.contains(task.getEquipmentId())) {
                continue;
            }
            if (!List.of("ASSIGNED", "ACCEPTED", "PRODUCING", "RUNNING").contains(task.getStatus())) {
                continue;
            }
            LocalDateTime taskStart = task.getAcceptedAt() != null ? task.getAcceptedAt() : task.getAssignedAt();
            if (taskStart == null) {
                taskStart = task.getCreatedAt();
            }
            if (taskStart == null) {
                continue;
            }
            LocalDateTime taskEnd = now;
            if (!rangesOverlap(scheduleStart, scheduleEnd, taskStart, taskEnd)) {
                continue;
            }
            list.add(conflict("warning", "equipment_busy", "设备占用",
                    equipmentCode + " 当前有进行中的派工任务 " + task.getDispatchNo()));
        }
        return list;
    }

    private boolean rangesOverlap(LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) {
        return !aEnd.isBefore(bStart) && !bEnd.isBefore(aStart);
    }

    private void persistSchedules(Long planId, List<Map<String, Object>> schedules, LocalDateTime now) {
        planScheduleMapper.deleteByPlanId(planId);
        if (schedules == null) {
            return;
        }
        int sort = 0;
        for (Map<String, Object> row : schedules) {
            ProductionPlanSchedule s = new ProductionPlanSchedule();
            s.setPlanId(planId);
            s.setStepId(longVal(row.get("stepId")));
            s.setStepNo(intVal(row.get("stepNo")));
            s.setStepName(str(row, "stepName"));
            s.setWorkshop(str(row, "workshop"));
            s.setEquipmentId(longVal(row.get("equipmentId")));
            s.setEquipmentCode(str(row, "equipmentCode"));
            s.setPlannedQuantity(BigDecimal.valueOf(intVal(row.get("plannedQuantity"))));
            s.setPlannedStart(parseDateTime(str(row, "plannedStart")));
            s.setPlannedEnd(parseDateTime(str(row, "plannedEnd")));
            s.setStandardHours(BigDecimal.valueOf(doubleVal(row.get("standardHours"))));
            s.setSortNo(++sort);
            s.setCreatedAt(now);
            s.setUpdatedAt(now);
            planScheduleMapper.insertSchedule(s);
        }
    }

    private void appendHistory(ProductionPlan plan, String action, String reason, String operator,
                               Map<String, Object> snapshot, LocalDateTime now) {
        ProductionPlanHistory h = new ProductionPlanHistory();
        h.setPlanId(plan.getPlanId());
        h.setPlanNo(plan.getPlanNo());
        h.setVersionNo(plan.getVersionNo() != null ? plan.getVersionNo() : "V1");
        h.setActionType(action);
        h.setReason(reason);
        h.setOperatorName(operator);
        h.setCreatedAt(now);
        try {
            h.setSnapshotJson(objectMapper.writeValueAsString(snapshot));
        } catch (Exception e) {
            h.setSnapshotJson("{}");
        }
        planHistoryMapper.insertHistory(h);
    }

    private List<Map<String, Object>> loadProcessRouteForItem(CustomerOrderItem item) {
        if (item == null || item.getMaterialId() == null) {
            return List.of();
        }
        ProcessRoute route = processRouteMapper.routeList().stream()
                .filter(r -> Objects.equals(r.getMaterialId(), item.getMaterialId()))
                .filter(r -> r.getStatus() == null || r.getStatus() == 1)
                .findFirst().orElse(null);
        if (route == null) {
            return List.of();
        }
        return processStepMapper.listByRouteId(route.getRouteId()).stream()
                .filter(s -> s.getStatus() == null || s.getStatus() == 1)
                .filter(ProductionWorkshopCatalog::isProductionStep)
                .sorted(Comparator.comparing(s -> {
                    ProductionWorkshopCatalog.ProcessStageDef stage = ProductionWorkshopCatalog.stageForStep(s);
                    return stage != null ? stage.stepOrder() : (s.getStepNo() != null ? s.getStepNo() : 99);
                }))
                .map(this::stepToMap).toList();
    }

    private Map<String, Object> stepToMap(ProcessStep step) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stepId", step.getStepId());
        m.put("stepNo", step.getStepNo());
        m.put("stepCode", step.getStepCode());
        m.put("stepName", step.getStepName());
        m.put("standardWorkHours", step.getStandardWorkHours());
        m.put("standardEquipmentType", step.getStandardEquipmentType());
        m.put("qualityRequired", step.getQualityRequired());
        m.put("qualityRequiredText", Objects.equals(step.getQualityRequired(), 1) ? "是" : "否");
        return m;
    }

    private Map<String, Object> equipmentRow(Equipment e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("equipmentId", e.getEquipmentId());
        m.put("equipmentCode", e.getEquipmentCode());
        m.put("equipmentName", e.getEquipmentName());
        m.put("equipmentType", e.getEquipmentType());
        m.put("workshop", e.getWorkshop());
        m.put("status", e.getStatus());
        return m;
    }

    private Map<String, Object> scheduleToMap(ProductionPlanSchedule s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stepId", s.getStepId());
        m.put("stepNo", s.getStepNo());
        m.put("stepName", s.getStepName());
        m.put("workshop", s.getWorkshop());
        m.put("equipmentId", s.getEquipmentId());
        m.put("equipmentCode", s.getEquipmentCode());
        m.put("plannedQuantity", s.getPlannedQuantity());
        m.put("plannedStart", s.getPlannedStart() != null ? s.getPlannedStart().format(DT_FMT) : "");
        m.put("plannedEnd", s.getPlannedEnd() != null ? s.getPlannedEnd().format(DT_FMT) : "");
        m.put("standardHours", s.getStandardHours());
        return m;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractMaterialGaps(Map<String, Object> analysis) {
        Object inv = analysis.get("inventoryCheck");
        if (!(inv instanceof Map<?, ?> map)) {
            return List.of();
        }
        Object checks = map.get("materialChecks");
        if (!(checks instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> gaps = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> raw) {
                Map<String, Object> m = (Map<String, Object>) raw;
                if (isRawMaterialShortage(m)) {
                    Map<String, Object> gap = new LinkedHashMap<>(m);
                    gap.put("gapQty", m.get("shortage"));
                    gaps.add(gap);
                }
            }
        }
        return gaps;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractMaterialGapConflicts(Map<String, Object> analysis) {
        List<Map<String, Object>> conflicts = new ArrayList<>();
        for (Map<String, Object> gap : extractMaterialGaps(analysis)) {
            conflicts.add(conflict("warning", "material_shortage", "物料不足",
                    gap.get("materialName") + " 缺口 " + gap.get("shortage") + "，可先排产并同步采购"));
        }
        return conflicts;
    }

    private List<Map<String, Object>> buildCapacityRisks(Map<String, Object> analysis) {
        List<Map<String, Object>> risks = new ArrayList<>();
        Object cap = analysis.get("capacityAnalysis");
        if (cap instanceof Map<?, ?> map && map.get("bottleneck") != null) {
            risks.add(Map.of("level", "warning", "label", "产能瓶颈", "detail", String.valueOf(map.get("bottleneck"))));
        }
        if (intVal(analysis.get("recommendedPlanQty")) < intVal(analysis.get("orderQuantity"))) {
            risks.add(Map.of("level", "warning", "label", "产能/物料限制",
                    "detail", "建议排产量低于订单量"));
        }
        return risks;
    }

    private Map<String, Object> pickBestScheme(List<Map<String, Object>> schemes) {
        return schemes.stream()
                .filter(s -> Boolean.TRUE.equals(s.get("canSubmit")))
                .min(Comparator.comparingInt(s -> intVal(s.get("delayDays"))))
                .orElse(schemes.get(0));
    }

    private int countMaterialShortage(Map<String, Object> base) {
        return extractMaterialGaps(base).size();
    }

    /** 成品库存不足属于正常排产场景，不计入原材料缺料 */
    private boolean isRawMaterialShortage(Map<String, Object> m) {
        if ("FINISHED".equals(String.valueOf(m.get("materialType")))) {
            return false;
        }
        return Boolean.FALSE.equals(m.get("sufficient")) && intVal(m.get("shortage")) > 0;
    }

    private Map<String, Object> conflict(String level, String code, String label, String detail) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("level", level);
        c.put("code", code);
        c.put("label", label);
        c.put("detail", detail);
        return c;
    }

    private String nextPlanNo() {
        String prefix = "PP" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        int max = productionPlanMapper.planList().stream()
                .map(ProductionPlan::getPlanNo)
                .filter(Objects::nonNull)
                .filter(no -> no.startsWith(prefix))
                .mapToInt(no -> {
                    try {
                        return Integer.parseInt(no.substring(prefix.length()));
                    } catch (Exception e) {
                        return 0;
                    }
                }).max().orElse(0);
        return prefix + String.format("%03d", max + 1);
    }

    private String nextVersion(String current) {
        if (current == null || !current.startsWith("V")) {
            return "V2";
        }
        try {
            int n = Integer.parseInt(current.substring(1).split("\\.")[0]);
            return "V" + (n + 1);
        } catch (Exception e) {
            return "V2";
        }
    }

    private String orderNoOfPlan(ProductionPlan plan) {
        CustomerOrder order = customerOrderMapper.customerOrderList().stream()
                .filter(o -> Objects.equals(o.getOrderId(), plan.getSourceOrderId()))
                .findFirst().orElse(null);
        return order != null ? order.getOrderNo() : "";
    }

    private int plannedQtyOfPlan(ProductionPlan plan) {
        return productionPlanItemMapper.planItemList().stream()
                .filter(i -> Objects.equals(i.getPlanId(), plan.getPlanId()))
                .map(i -> intVal(i.getPlannedQuantity()))
                .findFirst().orElse(0);
    }

    private CustomerOrder findOrderByNo(String orderNo) {
        return customerOrderMapper.customerOrderList().stream()
                .filter(o -> orderNo.equals(o.getOrderNo())).findFirst().orElse(null);
    }

    private ProductionPlan findPlanByNo(String planNo) {
        return productionPlanMapper.planList().stream()
                .filter(p -> planNo.equals(p.getPlanNo())).findFirst().orElse(null);
    }

    private CustomerOrderItem firstOrderItem(Long orderId) {
        return customerOrderItemMapper.orderItemList().stream()
                .filter(i -> Objects.equals(i.getOrderId(), orderId))
                .findFirst().orElse(null);
    }

    private User findUserByUsername(String username) {
        return userMapper.userList().stream()
                .filter(u -> username.equals(u.getUsername())).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                out.add((Map<String, Object>) map);
            }
        }
        return out;
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? String.valueOf(v) : "";
    }

    private String str(Map<String, Object> m, String key, String def) {
        String v = str(m, key);
        return v.isBlank() ? def : v;
    }

    private int intVal(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        try { return (int) Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0; }
    }

    private long longVal(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; }
    }

    private double doubleVal(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0; }
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s.substring(0, Math.min(10, s.length())));
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        if (s.length() <= 10) {
            return LocalDate.parse(s.substring(0, 10)).atStartOfDay();
        }
        return LocalDateTime.parse(s, DT_FMT);
    }
}
