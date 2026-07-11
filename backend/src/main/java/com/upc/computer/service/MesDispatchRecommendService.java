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
    @Autowired
    private CustomerOrderMapper customerOrderMapper;

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
                .filter(ProductionWorkshopCatalog::isProductionStep)
                .sorted(Comparator.comparing(ProcessStep::getStepNo, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        if (steps.isEmpty()) {
            steps = processStepMapper.stepList().stream()
                    .filter(s -> Long.valueOf(1L).equals(s.getRouteId()))
                    .filter(ProductionWorkshopCatalog::isProductionStep)
                    .sorted(Comparator.comparing(ProcessStep::getStepNo, Comparator.nullsLast(Integer::compareTo)))
                    .toList();
        }

        int planQty = resolvePlanQty(plan, materialId);
        List<User> operators = activeOperators();
        List<DispatchTask> allDispatches = dispatchTaskMapper.dispatchList();
        List<Equipment> allEquipment = equipmentMapper.equipmentList();

        String workOrderNo = wo != null ? wo.getWorkOrderNo() : "待生成";
        List<Map<String, Object>> recommendations = new ArrayList<>();
        Set<Long> reservedOperatorIds = new HashSet<>();

        for (ProcessStep step : steps) {
            OperatorPick pick = pickOperator(step, operators, allDispatches, reservedOperatorIds);
            if (pick.userId() != null) {
                reservedOperatorIds.add(pick.userId());
            }
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
            ProductionWorkshopCatalog.WorkshopDef ws = OperatorWorkshopCatalog.workshopForOperator(pick.username());
            row.put("workshopName", ws != null ? ws.workshopName() : "");
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
        result.put("schedulingSteps", buildDispatchSchedulingSteps(planNo, planQty, steps,
                operators, allEquipment, recommendations, wo));
        result.put("evidenceBase", buildDispatchEvidenceBase(planNo, planQty, steps,
                operators, allEquipment, recommendations, wo));
        Map<String, Object> validation = validateRecommendations(planNo, recommendations);
        attachConflictHints(recommendations, (List<Map<String, Object>>) validation.get("conflicts"));
        result.put("validation", validation);
        result.put("canSubmit", validation.get("canSubmit"));
        return result;
    }

    private List<Map<String, Object>> buildDispatchSchedulingSteps(String planNo, int planQty,
                                                                   List<ProcessStep> steps,
                                                                   List<User> operators,
                                                                   List<Equipment> allEquipment,
                                                                   List<Map<String, Object>> recommendations,
                                                                   WorkOrder wo) {
        List<Map<String, Object>> flow = new ArrayList<>();
        String stepNames = steps.stream().map(ProcessStep::getStepName).collect(Collectors.joining("→"));
        long idleEq = allEquipment.stream().filter(e -> "IDLE".equals(e.getStatus())).count();
        String operatorNames = operators.stream().map(User::getRealName).collect(Collectors.joining("、"));

        List<String> routeLines = List.of(
                String.format("计划号：%s", planNo),
                String.format("排产量：%d 台", planQty),
                String.format("工序数：%d 道", steps.size()),
                String.format("工序清单：%s", stepNames));
        flow.add(dispatchThought("route", "工艺工程师", "发现", "发现",
                "加载计划关联的工艺路线",
                String.format("「%s」展开 %d 道工序：%s；累计证据库 1 条。", planNo, steps.size(), stepNames),
                String.format("【工艺工程师】读取计划 %s，排产量 %d 台，展开工艺路线共 %d 道工序：%s。",
                        planNo, planQty, steps.size(), stepNames),
                routeLines, 1));

        long runEq = allEquipment.stream().filter(e -> "RUNNING".equals(e.getStatus())).count();
        List<String> eqLines = List.of(
                String.format("设备总数：%d 台", allEquipment.size()),
                String.format("空闲：%d 台，运行中：%d 台", idleEq, runEq),
                "按各工序设备类型筛选可用机台");
        flow.add(dispatchThought("equipment", "设备调度员", "派遣", "派遣",
                "扫描设备状态并匹配工序",
                String.format("「设备池」扫描 %d 台，空闲 %d 台；累计证据库 2 条。", allEquipment.size(), idleEq),
                String.format("【设备调度员】扫描全厂 %d 台设备（空闲 %d 台），按各工序设备类型筛选可用机台。",
                        allEquipment.size(), idleEq),
                eqLines, 2));

        List<String> opLines = new ArrayList<>();
        opLines.add(String.format("在岗操作员：%d 人", operators.size()));
        for (User op : operators) {
            opLines.add(String.format("· %s（%s）", op.getRealName(), op.getDepartment()));
        }
        if (operators.isEmpty()) {
            opLines.add("· 暂无在岗操作员");
        }
        flow.add(dispatchThought("operator", "人员协调员", "执行", "执行",
                "评估操作员岗位与在途负荷",
                String.format("「人员池」统计 %d 人在岗；累计证据库 %d 条。", operators.size(), 2 + operators.size()),
                String.format("【人员协调员】统计在岗操作员 %d 人：%s；逐一计算岗位匹配度、空闲度与历史任务权重。",
                        operators.size(), operatorNames.isBlank() ? "暂无" : operatorNames),
                opLines, 2 + operators.size()));

        List<String> matchLines = new ArrayList<>();
        for (Map<String, Object> rec : recommendations) {
            matchLines.add(String.format("%s → %s @ %s（%s 台，%s h）：%s",
                    rec.get("processStep"), rec.get("recommendedOperatorName"), rec.get("equipmentName"),
                    rec.get("planQty"), rec.get("estimatedHours"), rec.get("recommendReason")));
        }
        StringBuilder matchDetail = new StringBuilder();
        for (Map<String, Object> rec : recommendations) {
            matchDetail.append(String.format("「%s」→%s@%s；",
                    rec.get("processStep"), rec.get("recommendedOperatorName"), rec.get("equipmentName")));
        }
        flow.add(dispatchThought("match", "派工优化员", "执行", "执行",
                "逐道工序匹配负责人与设备",
                String.format("「智能匹配」完成 %d 道工序；累计证据库 %d 条。", recommendations.size(), 3 + matchLines.size()),
                String.format("【派工优化员】完成 %d 道工序匹配：%s",
                        recommendations.size(), matchDetail.length() > 0 ? matchDetail : "无推荐"),
                matchLines, 3 + matchLines.size()));

        long idleCount = recommendations.stream()
                .filter(r -> String.valueOf(r.get("recommendReason")).contains("空闲"))
                .count();
        List<String> resultLines = List.of(
                String.format("推荐工序：%d 道", recommendations.size()),
                String.format("优先空闲人员：%d 道", idleCount),
                wo != null ? "已有工单，将补全派工" : "确认后将自动生成工单并派工");
        flow.add(dispatchThought("result", "派工汇总员", "发现", "发现",
                "输出派工方案",
                String.format("「派工结论」%d 道工序推荐完成；累计证据库 %d 条。", recommendations.size(), 4 + resultLines.size()),
                String.format("【派工汇总员】生成派工方案：%d 道工序，其中 %d 道优先分配给空闲人员；确认后将创建工单并正式派工。",
                        recommendations.size(), idleCount),
                resultLines, 4 + resultLines.size()));
        return flow;
    }

    private List<Map<String, Object>> buildDispatchEvidenceBase(String planNo, int planQty,
                                                                 List<ProcessStep> steps,
                                                                 List<User> operators,
                                                                 List<Equipment> allEquipment,
                                                                 List<Map<String, Object>> recommendations,
                                                                 WorkOrder wo) {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> planMetrics = new LinkedHashMap<>();
        planMetrics.put("计划号", planNo);
        planMetrics.put("计划数量", planQty + " 台");
        planMetrics.put("工序数", steps.size() + " 道");
        planMetrics.put("工序清单", steps.stream().map(ProcessStep::getStepName).collect(Collectors.joining("、")));
        list.add(evidenceItem("dv-plan", "APS", "计划", planNo,
                "生产计划数据",
                String.format("【计划数据】%s 需生产 %d 台，工艺路线含 %d 道工序。", planNo, planQty, steps.size()),
                95, List.of("route"), planMetrics));

        long idleEq = allEquipment.stream().filter(e -> "IDLE".equals(e.getStatus())).count();
        long runEq = allEquipment.stream().filter(e -> "RUNNING".equals(e.getStatus())).count();
        Map<String, Object> eqMetrics = new LinkedHashMap<>();
        eqMetrics.put("设备总数", allEquipment.size() + " 台");
        eqMetrics.put("空闲", idleEq + " 台");
        eqMetrics.put("运行中", runEq + " 台");
        list.add(evidenceItem("dv-eq", "MES", "设备", "EQ-STATUS",
                "设备资源数据",
                String.format("【设备数据】全厂 %d 台设备，空闲 %d 台、运行 %d 台，按工序类型逐一匹配。",
                        allEquipment.size(), idleEq, runEq),
                90, List.of("equipment", "match"), eqMetrics));

        for (User op : operators) {
            Map<String, Object> opMetrics = new LinkedHashMap<>();
            opMetrics.put("姓名", op.getRealName());
            opMetrics.put("部门", op.getDepartment());
            opMetrics.put("账号", op.getUsername());
            list.add(evidenceItem("dv-op-" + op.getUserId(), "HR", "人员", op.getUsername(),
                    op.getRealName(),
                    String.format("【人员数据】%s（%s）在岗可派工", op.getRealName(), op.getDepartment()),
                    88, List.of("operator", "match"), opMetrics));
        }

        for (int i = 0; i < recommendations.size(); i++) {
            Map<String, Object> rec = recommendations.get(i);
            Map<String, Object> recMetrics = new LinkedHashMap<>();
            recMetrics.put("工序", rec.get("processStep"));
            recMetrics.put("推荐人", rec.get("recommendedOperatorName"));
            recMetrics.put("设备", rec.get("equipmentName"));
            recMetrics.put("派工数量", rec.get("planQty") + " 台");
            recMetrics.put("预计工时", rec.get("estimatedHours") + " h");
            recMetrics.put("推荐原因", rec.get("recommendReason"));
            list.add(evidenceItem("dv-rec-" + i, "AI", "推荐", String.valueOf(rec.get("processStep")),
                    String.valueOf(rec.get("processStep")) + " 派工推荐",
                    String.format("【匹配结果】%s → 操作员 %s、设备 %s，派工 %s 台，原因：%s",
                            rec.get("processStep"), rec.get("recommendedOperatorName"),
                            rec.get("equipmentName"), rec.get("planQty"), rec.get("recommendReason")),
                    86, List.of("match", "result"), recMetrics));
        }

        Map<String, Object> woMetrics = new LinkedHashMap<>();
        woMetrics.put("工单号", wo != null ? wo.getWorkOrderNo() : "待生成");
        woMetrics.put("状态", wo != null ? "已有工单" : "确认后生成");
        list.add(evidenceItem("dv-wo", "MES", "工单", wo != null ? wo.getWorkOrderNo() : "待生成",
                "工单派工状态",
                wo != null ? "【工单数据】已有工单 " + wo.getWorkOrderNo() + "，将按推荐补全派工"
                        : "【工单数据】尚无工单，确认后将自动生成并派工",
                84, List.of("result"), woMetrics));
        return list;
    }

    private Map<String, Object> dispatchThought(String key, String agentName, String actionType, String badge,
                                                String action, String summary, String thought,
                                                List<String> detailLines, int evidenceCount) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("agentName", agentName);
        row.put("actionType", actionType);
        row.put("badge", badge);
        row.put("title", action);
        row.put("action", action);
        row.put("summary", summary);
        row.put("thought", thought);
        row.put("detail", thought);
        row.put("detailLines", detailLines != null ? detailLines : List.of());
        row.put("evidenceCount", evidenceCount);
        row.put("status", "success");
        return row;
    }

    private Map<String, Object> evidenceItem(String id, String source, String tag, String code,
                                             String title, String snippet, int reliability,
                                             List<String> relatedSteps, Map<String, Object> metrics) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("source", source);
        row.put("tag", tag);
        row.put("code", code);
        row.put("title", title);
        row.put("snippet", snippet);
        row.put("reliability", reliability);
        row.put("relatedSteps", relatedSteps);
        row.put("metrics", metrics != null ? metrics : Map.of());
        return row;
    }

    private boolean hasActiveDispatches(WorkOrder wo) {
        return dispatchTaskMapper.dispatchList().stream()
                .anyMatch(d -> wo.getWorkOrderId().equals(d.getWorkOrderId()));
    }

    /**
     * 为单道工序推荐操作员（批量派工时可传入本批次已占用的操作员 ID）。
     */
    public OperatorPick recommendOperator(ProcessStep step, Set<Long> excludeOperatorIds) {
        return pickOperator(step, activeOperators(), dispatchTaskMapper.dispatchList(), excludeOperatorIds);
    }

    private OperatorPick pickOperator(ProcessStep step, List<User> operators, List<DispatchTask> dispatches) {
        return pickOperator(step, operators, dispatches, Set.of());
    }

    private OperatorPick pickOperator(ProcessStep step, List<User> operators, List<DispatchTask> dispatches,
                                      Set<Long> reservedOperatorIds) {
        String primaryUsername = OperatorWorkshopCatalog.primaryOperatorUsername(step);
        ProductionWorkshopCatalog.ProcessStageDef stage = ProductionWorkshopCatalog.stageForStep(step);
        List<User> eligible = operators.stream()
                .filter(user -> OperatorWorkshopCatalog.isBoundOperator(user.getUsername()))
                .filter(user -> OperatorWorkshopCatalog.operatorMatchesStep(user.getUsername(), step))
                .toList();
        if (eligible.isEmpty()) {
            String stageName = stage != null ? stage.stepName() : "该工序";
            return new OperatorPick(null, "", "", "",
                    "工序「" + stageName + "」暂无绑定车间的空闲操作员", 0, 0);
        }
        String stepDept = resolveStepDepartment(step);
        List<ScoredOperator> scored = new ArrayList<>();

        for (User user : eligible) {
            if (reservedOperatorIds != null && reservedOperatorIds.contains(user.getUserId())) {
                continue;
            }
            int activeLoad = countActiveLoad(user.getUserId(), dispatches);
            if (activeLoad > 0) {
                continue;
            }
            int historyCount = countHistory(user.getUserId(), dispatches);
            boolean deptMatch = departmentMatches(user.getDepartment(), stepDept);
            boolean idle = activeLoad == 0;
            boolean primary = primaryUsername != null && primaryUsername.equals(user.getUsername());

            int score = 0;
            if (primary) {
                score += 1000;
            }
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
            String stageName = stage != null ? stage.stepName() : "该工序";
            return new OperatorPick(null, "", "", "",
                    "工序「" + stageName + "」暂无空闲操作员（本批次已占用其他工序人员）", 0, 0);
        }

        String reason = buildRecommendReason(best, primaryUsername);
        return new OperatorPick(best.user().getUserId(), best.user().getUsername(), best.user().getRealName(),
                best.user().getDepartment(), reason, best.activeLoad(), best.historyCount());
    }

    private String buildRecommendReason(ScoredOperator best, String primaryUsername) {
        List<String> parts = new ArrayList<>();
        if (primaryUsername != null && primaryUsername.equals(best.user().getUsername())) {
            ProductionWorkshopCatalog.WorkshopDef ws = OperatorWorkshopCatalog.workshopForOperator(primaryUsername);
            parts.add("本工序固定车间负责人" + (ws != null ? "（" + ws.workshopName() + "）" : ""));
        } else if (best.deptMatch()) {
            parts.add("岗位匹配「" + best.user().getDepartment() + "」");
        } else {
            parts.add("同车间替补");
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
        String equipType = resolveEquipmentType(step);
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

    private String resolveEquipmentType(ProcessStep step) {
        String raw = step.getStandardEquipmentType() != null ? step.getStandardEquipmentType().trim() : "";
        if (!raw.isBlank() && !raw.contains("?") && !raw.contains("？")) {
            return raw;
        }
        ProductionWorkshopCatalog.ProcessStageDef stage = ProductionWorkshopCatalog.stageForStep(step);
        if (stage != null && stage.equipmentType() != null) {
            return stage.equipmentType();
        }
        String name = step.getStepName() != null ? step.getStepName() : "";
        if (name.contains("显示屏")) return "显示屏线";
        if (name.contains("主板")) return "主板线";
        if (name.contains("贴附")) return "贴附机";
        if (name.contains("组装")) return "组装线";
        if (name.contains("老化")) return "老化架";
        if (name.contains("包装")) return "包装线";
        if (name.contains("调校")) return "调校台";
        return raw;
    }

    private String resolveStepDepartment(ProcessStep step) {
        String equipType = resolveEquipmentType(step);
        if (equipType == null) {
            return "生产部";
        }
        return switch (equipType) {
            case "显示屏线", "主板线", "贴附机", "组装线" -> "生产一部";
            default -> "生产一部";
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
                .filter(u -> OperatorWorkshopCatalog.isBoundOperator(u.getUsername()))
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

    /** 从数据库读取计划关联订单、工艺路线、交期 */
    public Map<String, Object> loadPlanContext(String planNo) {
        ProductionPlan plan = findPlanByNo(planNo);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }
        CustomerOrder order = customerOrderMapper.customerOrderList().stream()
                .filter(o -> plan.getSourceOrderId().equals(o.getOrderId()))
                .findFirst().orElse(null);
        CustomerOrderItem item = customerOrderItemMapper.orderItemList().stream()
                .filter(i -> plan.getSourceOrderId().equals(i.getOrderId()))
                .findFirst().orElse(null);
        Long materialId = resolveMaterialId(plan);
        Long routeId = resolveRouteId(materialId);
        List<Map<String, Object>> steps = processStepMapper.stepList().stream()
                .filter(s -> routeId.equals(s.getRouteId()))
                .filter(s -> s.getStatus() == null || s.getStatus() == 1)
                .filter(ProductionWorkshopCatalog::isProductionStep)
                .sorted(Comparator.comparing(ProcessStep::getStepNo, Comparator.nullsLast(Integer::compareTo)))
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("stepNo", s.getStepNo());
                    m.put("stepName", s.getStepName());
                    m.put("standardEquipmentType", s.getStandardEquipmentType());
                    m.put("standardWorkHours", s.getStandardWorkHours());
                    return m;
                }).toList();

        WorkOrder wo = workOrderMapper.workOrderList().stream()
                .filter(w -> plan.getPlanId().equals(w.getPlanId()))
                .findFirst().orElse(null);

        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("planId", planNo);
        ctx.put("orderId", order != null ? order.getOrderNo() : "");
        ctx.put("customerName", order != null ? order.getCustomerName() : "");
        ctx.put("productModel", item != null ? item.getProductName() : orderProductModel(plan.getSourceOrderId()));
        ctx.put("quantity", resolvePlanQty(plan, materialId));
        ctx.put("deliveryDate", order != null && order.getRequiredDeliveryDate() != null
                ? order.getRequiredDeliveryDate().toString() : "");
        ctx.put("planStart", plan.getPlannedStartDate() != null ? plan.getPlannedStartDate().toString() : "");
        ctx.put("planEnd", plan.getPlannedEndDate() != null ? plan.getPlannedEndDate().toString() : "");
        ctx.put("processRoute", steps);
        ctx.put("workOrderId", wo != null ? wo.getWorkOrderNo() : "");
        ctx.put("hasWorkOrder", wo != null);
        return ctx;
    }

    public Map<String, Object> validateRecommendations(String planNo, List<Map<String, Object>> rows) {
        ProductionPlan plan = findPlanByNo(planNo);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException("派工明细为空");
        }
        List<DispatchTask> allDispatches = dispatchTaskMapper.dispatchList();
        List<Equipment> allEquipment = equipmentMapper.equipmentList();
        List<User> operators = activeOperators();
        Map<String, ProcessStep> stepByName = processStepMapper.stepList().stream()
                .collect(Collectors.toMap(ProcessStep::getStepName, s -> s, (a, b) -> a));

        List<Map<String, Object>> conflicts = new ArrayList<>();
        Set<String> usedOperators = new HashSet<>();
        Set<String> usedEquipCodes = new HashSet<>();
        int planQty = resolvePlanQty(plan, resolveMaterialId(plan));
        Integer prevStepNo = null;

        for (Map<String, Object> row : rows) {
            String stepName = String.valueOf(row.getOrDefault("processStep", ""));
            String operator = String.valueOf(row.getOrDefault("recommendedOperator", row.get("operator")));
            String equipName = String.valueOf(row.getOrDefault("equipmentName", row.get("equipment")));
            String equipCode = String.valueOf(row.getOrDefault("equipmentCode", ""));
            int qty = row.get("planQty") instanceof Number n ? n.intValue() : planQty;

            ProcessStep step = stepByName.get(stepName);
            if (step != null && step.getStepNo() != null) {
                if (prevStepNo != null && step.getStepNo() < prevStepNo) {
                    conflicts.add(conflict("danger", "process_order", "工序顺序", stepName + " 顺序异常"));
                }
                prevStepNo = step.getStepNo();
            }
            if (operator.isBlank() || "null".equals(operator)) {
                conflicts.add(conflict("danger", "no_operator", "人员缺失", stepName + " 未指定操作员"));
            } else if (!usedOperators.add(operator)) {
                conflicts.add(conflict("danger", "operator_duplicate", "人员重复", operator + " 被重复派工"));
            } else {
                User op = operators.stream().filter(u -> operator.equals(u.getUsername())).findFirst().orElse(null);
                if (op != null) {
                    long active = allDispatches.stream()
                            .filter(d -> op.getUserId().equals(d.getOperatorId()))
                            .filter(d -> ACTIVE_DISPATCH_STATUS.contains(d.getStatus()))
                            .count();
                    if (active > 0) {
                        conflicts.add(conflict("danger", "operator_busy", "人员占用",
                                op.getRealName() + " 已有进行中的派工"));
                    }
                }
            }
            Equipment eq = allEquipment.stream()
                    .filter(e -> (!equipCode.isBlank() && !"null".equals(equipCode) && equipCode.equals(e.getEquipmentCode()))
                            || equipName.equals(e.getEquipmentName()) || equipName.equals(e.getEquipmentCode()))
                    .findFirst().orElse(null);
            if (eq != null) {
                if ("FAULT".equals(eq.getStatus()) || "MAINTENANCE".equals(eq.getStatus())) {
                    conflicts.add(conflict("danger", "equipment_fault", "设备故障", eq.getEquipmentName() + " 不可用"));
                }
                if (!usedEquipCodes.add(eq.getEquipmentCode())) {
                    conflicts.add(conflict("danger", "equipment_duplicate", "设备占用", eq.getEquipmentName() + " 重复分配"));
                }
                boolean busy = allDispatches.stream()
                        .filter(d -> eq.getEquipmentId().equals(d.getEquipmentId()))
                        .anyMatch(d -> ACTIVE_DISPATCH_STATUS.contains(d.getStatus()));
                if (busy) {
                    conflicts.add(conflict("warning", "equipment_busy", "设备占用", eq.getEquipmentName() + " 使用中"));
                }
            }
            String workshop = String.valueOf(row.getOrDefault("workshopName", ""));
            if (!workshop.isBlank() && qty > 300) {
                conflicts.add(conflict("warning", "workshop_overload", "车间负载", workshop + " 派工量偏大"));
            }
        }

        boolean hasDanger = conflicts.stream().anyMatch(c -> "danger".equals(c.get("level")));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conflicts", conflicts);
        result.put("hasDanger", hasDanger);
        result.put("canSubmit", !hasDanger);
        return result;
    }

    public void attachConflictHints(List<Map<String, Object>> rows, List<Map<String, Object>> conflicts) {
        if (rows == null || conflicts == null) return;
        for (Map<String, Object> row : rows) {
            String step = String.valueOf(row.get("processStep"));
            List<Map<String, Object>> rowConflicts = conflicts.stream()
                    .filter(c -> String.valueOf(c.get("detail")).contains(step))
                    .toList();
            row.put("conflicts", rowConflicts);
            row.put("conflictStatus", rowConflicts.stream().anyMatch(c -> "danger".equals(c.get("level")))
                    ? "冲突" : rowConflicts.isEmpty() ? "正常" : "提示");
        }
    }

    private Map<String, Object> conflict(String level, String code, String label, String detail) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("level", level);
        c.put("code", code);
        c.put("label", label);
        c.put("detail", detail);
        return c;
    }

    private double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    public static record OperatorPick(Long userId, String username, String realName, String department,
                                      String reason, int activeLoad, int historyCount) {
    }

    private record EquipmentPick(String code, String name) {
    }

    private record ScoredOperator(User user, int score, int activeLoad, int historyCount,
                                  boolean deptMatch, boolean idle) {
    }
}
