package com.upc.computer.service;

import com.upc.computer.common.BusinessException;
import com.upc.computer.entity.*;
import com.upc.computer.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 生产主管智能派工推荐：按操作员空闲度、岗位、历史任务与当前负载推荐工序负责人。
 */
@Service
public class MesDispatchRecommendService {

    private static final List<String> ACTIVE_DISPATCH_STATUS = List.of(
            "ASSIGNED", "ACCEPTED", "PRODUCING", "RUNNING");

    @Autowired
    private ProductionPlanMapper productionPlanMapper;
    @Autowired
    private ProductionPlanItemMapper productionPlanItemMapper;
    @Autowired
    private CustomerOrderItemMapper customerOrderItemMapper;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private ProcessStepMapper processStepMapper;
    @Autowired
    private ProcessRouteMapper processRouteMapper;
    @Autowired
    private EquipmentMapper equipmentMapper;
    @Autowired
    private DispatchTaskMapper dispatchTaskMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private MaterialMapper materialMapper;

    /**
     * 批量预览：所有已提交且待派工的计划。
     */
    public Map<String, Object> generateAllRecommendations() {
        Set<Long> planIdsWithWo = workOrderMapper.workOrderList().stream()
                .map(WorkOrder::getPlanId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<ProductionPlan> plans = productionPlanMapper.planList().stream()
                .filter(p -> "SUBMITTED".equals(p.getPlanStatus()) || "EXECUTING".equals(p.getPlanStatus()))
                .sorted(Comparator.comparing(ProductionPlan::getPlannedEndDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<Map<String, Object>> planResults = new ArrayList<>();
        for (ProductionPlan plan : plans) {
            boolean hasWo = planIdsWithWo.contains(plan.getPlanId());
            WorkOrder wo = workOrderMapper.workOrderList().stream()
                    .filter(w -> plan.getPlanId().equals(w.getPlanId()))
                    .findFirst().orElse(null);
            if (wo != null && hasActiveDispatches(wo)) {
                continue;
            }
            planResults.add(generateRecommendations(plan.getPlanNo()));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("plans", planResults);
        result.put("total", planResults.size());
        result.put("summary", String.format("共 %d 份计划可智能派工推荐", planResults.size()));
        return result;
    }

    /**
     * 单笔计划智能派工推荐。
     */
    public Map<String, Object> generateRecommendations(String planNo) {
        ProductionPlan plan = findPlanByNo(planNo);
        if (plan == null) {
            throw new BusinessException("计划不存在：" + planNo);
        }
        if (!List.of("SUBMITTED", "EXECUTING").contains(plan.getPlanStatus())) {
            throw new BusinessException("计划状态不允许派工推荐，请确认计划已提交生产主管");
        }

        WorkOrder wo = workOrderMapper.workOrderList().stream()
                .filter(w -> plan.getPlanId().equals(w.getPlanId()))
                .findFirst().orElse(null);

        Long materialId = resolveMaterialId(plan);
        Long routeId = resolveRouteId(materialId);
        List<ProcessStep> steps = processStepMapper.stepList().stream()
                .filter(s -> routeId.equals(s.getRouteId()))
                .filter(s -> s.getStatus() == null || s.getStatus() == 1)
                .sorted(Comparator.comparing(ProcessStep::getStepNo, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        if (steps.isEmpty()) {
            steps = processStepMapper.stepList().stream()
                    .filter(s -> Long.valueOf(1L).equals(s.getRouteId()))
                    .sorted(Comparator.comparing(ProcessStep::getStepNo, Comparator.nullsLast(Integer::compareTo)))
                    .toList();
        }

        int planQty = resolvePlanQty(plan, materialId);
        List<User> operators = activeOperators();
        List<DispatchTask> allDispatches = dispatchTaskMapper.dispatchList();
        List<Equipment> allEquipment = equipmentMapper.equipmentList();

        String workOrderNo = wo != null ? wo.getWorkOrderNo() : "待生成";
        List<Map<String, Object>> recommendations = new ArrayList<>();

        for (ProcessStep step : steps) {
            OperatorPick pick = pickOperator(step, operators, allDispatches);
            EquipmentPick equipPick = pickEquipment(step, allEquipment, allDispatches);
            double hoursPerUnit = step.getStandardWorkHours() != null
                    ? step.getStandardWorkHours().doubleValue() : 1.0;
            double estimatedHours = round1(hoursPerUnit * planQty);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("planId", planNo);
            row.put("workOrderId", workOrderNo);
            row.put("processStep", step.getStepName());
            row.put("recommendedOperator", pick.username());
            row.put("recommendedOperatorName", pick.realName());
            row.put("recommendReason", pick.reason());
            row.put("equipmentCode", equipPick.code());
            row.put("equipmentName", equipPick.name());
            row.put("estimatedHours", estimatedHours);
            row.put("planQty", planQty);
            row.put("department", pick.department());
            row.put("activeLoad", pick.activeLoad());
            row.put("historyCount", pick.historyCount());
            recommendations.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("planId", planNo);
        result.put("workOrderId", workOrderNo);
        result.put("planStatus", plan.getPlanStatus());
        result.put("productModel", orderProductModel(plan.getSourceOrderId()));
        result.put("planQuantity", planQty);
        result.put("recommendations", recommendations);
        result.put("hasWorkOrder", wo != null);
        result.put("summary", String.format("计划 %s 共 %d 道工序，已生成智能派工推荐",
                planNo, recommendations.size()));
        return result;
    }

    private boolean hasActiveDispatches(WorkOrder wo) {
        return dispatchTaskMapper.dispatchList().stream()
                .anyMatch(d -> wo.getWorkOrderId().equals(d.getWorkOrderId()));
    }

    private OperatorPick pickOperator(ProcessStep step, List<User> operators, List<DispatchTask> dispatches) {
        String stepDept = resolveStepDepartment(step);
        List<ScoredOperator> scored = new ArrayList<>();

        for (User user : operators) {
            int activeLoad = countActiveLoad(user.getUserId(), dispatches);
            int historyCount = countHistory(user.getUserId(), dispatches);
            boolean deptMatch = departmentMatches(user.getDepartment(), stepDept);
            boolean idle = activeLoad == 0;

            int score = 0;
            if (deptMatch) {
                score += 100;
            }
            if (idle) {
                score += 50;
            }
            score -= activeLoad * 20;
            score -= Math.min(historyCount, 20) * 2;

            scored.add(new ScoredOperator(user, score, activeLoad, historyCount, deptMatch, idle));
        }

        scored.sort(Comparator.comparingInt(ScoredOperator::score).reversed());
        ScoredOperator best = scored.isEmpty() ? null : scored.get(0);
        if (best == null) {
            return new OperatorPick("wang_operator", "王操作", "生产一部",
                    "默认推荐（无可用操作员）", 0, 0);
        }

        String reason = buildRecommendReason(best);
        return new OperatorPick(best.user().getUsername(), best.user().getRealName(),
                best.user().getDepartment(), reason, best.activeLoad(), best.historyCount());
    }

    private String buildRecommendReason(ScoredOperator best) {
        List<String> parts = new ArrayList<>();
        if (best.deptMatch()) {
            parts.add("岗位匹配「" + best.user().getDepartment() + "」");
        } else {
            parts.add("跨岗位调配");
        }
        if (best.idle()) {
            parts.add("当前空闲");
        } else {
            parts.add("在途任务 " + best.activeLoad() + " 条");
        }
        parts.add("历史任务 " + best.historyCount() + " 条");
        return String.join("，", parts);
    }

    private EquipmentPick pickEquipment(ProcessStep step, List<Equipment> allEquipment,
                                      List<DispatchTask> dispatches) {
        String equipType = step.getStandardEquipmentType() != null
                ? step.getStandardEquipmentType().trim() : "";
        Set<Long> busyEquipIds = dispatches.stream()
                .filter(d -> ACTIVE_DISPATCH_STATUS.contains(d.getStatus()))
                .map(DispatchTask::getEquipmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Equipment> candidates = allEquipment.stream()
                .filter(e -> equipType.isEmpty() || equipType.equals(e.getEquipmentType()))
                .filter(e -> !"FAULT".equals(e.getStatus()) && !"MAINTENANCE".equals(e.getStatus()))
                .sorted(Comparator
                        .comparing((Equipment e) -> busyEquipIds.contains(e.getEquipmentId()))
                        .thenComparing(e -> !"IDLE".equals(e.getStatus()))
                        .thenComparing(Equipment::getEquipmentCode))
                .toList();

        Equipment eq = candidates.isEmpty() ? null : candidates.get(0);
        if (eq != null) {
            return new EquipmentPick(eq.getEquipmentCode(), eq.getEquipmentName());
        }
        return new EquipmentPick("", step.getStandardEquipmentType() != null
                ? step.getStandardEquipmentType() + "（待分配）" : "待分配");
    }

    private int countActiveLoad(Long operatorId, List<DispatchTask> dispatches) {
        return (int) dispatches.stream()
                .filter(d -> operatorId.equals(d.getOperatorId()))
                .filter(d -> ACTIVE_DISPATCH_STATUS.contains(d.getStatus()))
                .count();
    }

    private int countHistory(Long operatorId, List<DispatchTask> dispatches) {
        return (int) dispatches.stream()
                .filter(d -> operatorId.equals(d.getOperatorId()))
                .count();
    }

    private String resolveStepDepartment(ProcessStep step) {
        String equipType = step.getStandardEquipmentType();
        if (equipType == null) {
            return "生产部";
        }
        return switch (equipType) {
            case "贴附机", "组装线" -> "生产一部";
            case "老化架", "调校台", "包装线" -> "生产二部";
            default -> "生产部";
        };
    }

    private boolean departmentMatches(String operatorDept, String stepDept) {
        if (operatorDept == null || stepDept == null) {
            return false;
        }
        if (operatorDept.equals(stepDept)) {
            return true;
        }
        return operatorDept.contains(stepDept.replace("生产", ""))
                || stepDept.contains(operatorDept.replace("生产", ""));
    }

    private List<User> activeOperators() {
        return userMapper.userList().stream()
                .filter(u -> u.getStatus() != null && u.getStatus() == 1)
                .filter(u -> {
                    Role role = roleMapper.getRoleById(u.getRoleId());
                    return role != null && "OPERATOR".equalsIgnoreCase(role.getRoleCode());
                })
                .toList();
    }

    private int resolvePlanQty(ProductionPlan plan, Long materialId) {
        return productionPlanItemMapper.planItemList().stream()
                .filter(i -> plan.getPlanId().equals(i.getPlanId()))
                .filter(i -> materialId == null || materialId.equals(i.getMaterialId()))
                .map(i -> i.getPlannedQuantity().intValue())
                .findFirst()
                .orElseGet(() -> {
                    CustomerOrderItem item = customerOrderItemMapper.orderItemList().stream()
                            .filter(i -> plan.getSourceOrderId().equals(i.getOrderId()))
                            .findFirst().orElse(null);
                    return item != null ? item.getQuantity().intValue() : 1;
                });
    }

    private Long resolveMaterialId(ProductionPlan plan) {
        CustomerOrderItem item = customerOrderItemMapper.orderItemList().stream()
                .filter(i -> plan.getSourceOrderId().equals(i.getOrderId()))
                .findFirst().orElse(null);
        return item != null ? item.getMaterialId() : null;
    }

    private Long resolveRouteId(Long materialId) {
        if (materialId == null) {
            return processRouteMapper.routeList().stream()
                    .map(ProcessRoute::getRouteId).findFirst().orElse(1L);
        }
        return processRouteMapper.routeList().stream()
                .filter(r -> materialId.equals(r.getMaterialId()))
                .map(ProcessRoute::getRouteId)
                .findFirst()
                .orElse(1L);
    }

    private String orderProductModel(Long orderId) {
        return customerOrderItemMapper.orderItemList().stream()
                .filter(i -> orderId.equals(i.getOrderId()))
                .map(CustomerOrderItem::getProductName)
                .findFirst().orElse("");
    }

    private ProductionPlan findPlanByNo(String planNo) {
        return productionPlanMapper.planList().stream()
                .filter(p -> planNo.equals(p.getPlanNo()))
                .findFirst().orElse(null);
    }

    private double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private record OperatorPick(String username, String realName, String department,
                                String reason, int activeLoad, int historyCount) {
    }

    private record EquipmentPick(String code, String name) {
    }

    private record ScoredOperator(User user, int score, int activeLoad, int historyCount,
                                  boolean deptMatch, boolean idle) {
    }
}
