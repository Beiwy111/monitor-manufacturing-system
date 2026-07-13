package com.upc.computer.service;

import com.upc.computer.common.BusinessException;
import com.upc.computer.entity.Bom;
import com.upc.computer.entity.CustomerOrder;
import com.upc.computer.entity.CustomerOrderItem;
import com.upc.computer.entity.DispatchTask;
import com.upc.computer.entity.Equipment;
import com.upc.computer.entity.Inventory;
import com.upc.computer.entity.Material;
import com.upc.computer.entity.ProcessRoute;
import com.upc.computer.entity.ProcessStep;
import com.upc.computer.entity.Role;
import com.upc.computer.entity.User;
import com.upc.computer.entity.WorkOrder;
import com.upc.computer.mapper.BomMapper;
import com.upc.computer.mapper.CustomerOrderItemMapper;
import com.upc.computer.mapper.CustomerOrderMapper;
import com.upc.computer.mapper.DispatchTaskMapper;
import com.upc.computer.mapper.EquipmentMapper;
import com.upc.computer.mapper.InventoryMapper;
import com.upc.computer.mapper.MaterialMapper;
import com.upc.computer.mapper.ProcessRouteMapper;
import com.upc.computer.mapper.ProcessStepMapper;
import com.upc.computer.mapper.RoleMapper;
import com.upc.computer.mapper.UserMapper;
import com.upc.computer.mapper.WorkOrderMapper;
import com.upc.computer.service.MesRuntimeStore.MesRuntimeState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 计划员 Agent：根据订单数量与计划周期，自动测算人力、设备与车间资源需求。
 */
@Service
public class MesPlannerAgentService {

    private static final double SHIFT_HOURS = 16.0;
    private static final int LIVE_STEP_SECONDS = 5;
    private static final int LIVE_BATCH_TARGET = 20;

    private static List<ProductionWorkshopCatalog.WorkshopDef> workshops() {
        return ProductionWorkshopCatalog.allWorkshops();
    }

    @Autowired
    private CustomerOrderMapper customerOrderMapper;
    @Autowired
    private CustomerOrderItemMapper customerOrderItemMapper;
    @Autowired
    private MaterialMapper materialMapper;
    @Autowired
    private InventoryMapper inventoryMapper;
    @Autowired
    private BomMapper bomMapper;
    @Autowired
    private ProcessStepMapper processStepMapper;
    @Autowired
    private ProcessRouteMapper processRouteMapper;
    @Autowired
    private EquipmentMapper equipmentMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private DispatchTaskMapper dispatchTaskMapper;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private MesRuntimeStore mesRuntimeStore;

    public Map<String, Object> analyze(String orderNo, LocalDate planStart, LocalDate planEnd) {
        CustomerOrder order = findOrderByNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在：" + orderNo);
        }
        if (!List.of("PLAN_PENDING", "APPROVED").contains(order.getAuditStatus())) {
            throw new BusinessException("订单状态不允许 Agent 排产，请确认订单已审核并处于待计划状态");
        }
        if (planStart == null || planEnd == null || planEnd.isBefore(planStart)) {
            throw new BusinessException("请填写有效的计划开始与截止时间");
        }

        CustomerOrderItem item = firstOrderItem(order.getOrderId());
        int orderQuantity = item != null ? item.getQuantity().intValue() : 0;
        if (orderQuantity <= 0) {
            throw new BusinessException("订单数量无效，无法排产");
        }

        Map<String, Object> inventoryCheck = buildInventoryAnalysis(item, orderQuantity);
        int materialRecommendedQty = intVal(inventoryCheck.get("recommendedPlanQty"));
        int shipFromStock = intVal(inventoryCheck.get("shipFromStock"));
        int quantity = materialRecommendedQty > 0 ? materialRecommendedQty : orderQuantity;

        long workDays = Math.max(1, ChronoUnit.DAYS.between(planStart, planEnd) + 1);

        List<ProcessStep> steps = resolveRouteSteps(item);
        List<Equipment> allEquipment = equipmentMapper.equipmentList();
        List<User> operatorPool = activeOperators();
        int needToProduce = intVal(inventoryCheck.get("needToProduce"));
        int capacityTarget = needToProduce > 0 ? needToProduce : orderQuantity;
        CapacityPlan capacityPlan = buildCapacityPlan(steps, allEquipment, operatorPool, capacityTarget, workDays);
        int recommendedPlanQty = capacityPlan.feasibleQty();
        if (recommendedPlanQty <= 0) {
            recommendedPlanQty = materialRecommendedQty;
            if (materialRecommendedQty > 0) {
                applyCapacityDecision(inventoryCheck, materialRecommendedQty, recommendedPlanQty, capacityPlan);
            }
        } else if (materialRecommendedQty > 0 && materialRecommendedQty < recommendedPlanQty) {
            inventoryCheck.put("recommendedPlanQty", recommendedPlanQty);
            inventoryCheck.put("decision", "PARTIAL_PRODUCE");
            inventoryCheck.put("recommendation", String.format(
                    "设备产能可支撑 %d 台；物料当前仅支撑 %d 台，建议同步采购后满产。",
                    recommendedPlanQty, materialRecommendedQty));
        } else {
            inventoryCheck.put("recommendedPlanQty", recommendedPlanQty);
        }
        quantity = recommendedPlanQty > 0 ? recommendedPlanQty : (materialRecommendedQty > 0 ? materialRecommendedQty : orderQuantity);
        double dailyTarget = quantity / (double) workDays;

        List<Map<String, Object>> workshopPlans = new ArrayList<>();
        List<Map<String, Object>> stagePlans = new ArrayList<>();
        List<Map<String, Object>> dispatchSuggestions = new ArrayList<>();
        int totalMachines = 0;
        int totalOperators = 0;

        for (ProductionWorkshopCatalog.ProcessStageDef stage : ProductionWorkshopCatalog.PRODUCTION_STAGES) {
            ProcessStep routeStep = steps.stream()
                    .filter(s -> ProductionWorkshopCatalog.matchesStage(s, stage))
                    .findFirst()
                    .orElse(null);
            if (routeStep == null) {
                continue;
            }

            double hours = routeStep.getStandardWorkHours() != null
                    ? routeStep.getStandardWorkHours().doubleValue() : 1.0;
            if (hours <= 0) {
                hours = 1.0;
            }
            double dailyPerMachine = SHIFT_HOURS / hours;
            int stageMachinesNeeded = (int) Math.ceil(dailyTarget / Math.max(1.0, dailyPerMachine));
            stageMachinesNeeded = Math.max(1, stageMachinesNeeded);

            List<WorkshopSlot> slots = new ArrayList<>();
            int totalIdleMachines = 0;
            for (ProductionWorkshopCatalog.WorkshopDef ws : stage.workshops()) {
                List<Equipment> wsEquip = equipmentForWorkshop(ws, allEquipment);
                int idle = (int) wsEquip.stream()
                        .filter(e -> List.of("IDLE", "RUNNING").contains(e.getStatus()))
                        .count();
                int available = Math.max(idle, wsEquip.size());
                totalIdleMachines += available;
                slots.add(new WorkshopSlot(ws, wsEquip, available, idle));
            }

            int stageAllocated = 0;
            int stageOperators = 0;
            List<String> stageWorkshopNames = new ArrayList<>();

            for (int i = 0; i < slots.size(); i++) {
                WorkshopSlot slot = slots.get(i);
                ProductionWorkshopCatalog.WorkshopDef ws = slot.ws();
                int machinesNeeded;
                if (totalIdleMachines <= 0) {
                    machinesNeeded = i == 0 ? stageMachinesNeeded : 0;
                } else if (i == slots.size() - 1) {
                    machinesNeeded = Math.max(0, stageMachinesNeeded - stageAllocated);
                } else {
                    machinesNeeded = (int) Math.round(stageMachinesNeeded * slot.available() / (double) totalIdleMachines);
                }
                machinesNeeded = Math.min(machinesNeeded, Math.max(1, slot.available()));
                if (stageAllocated + machinesNeeded > stageMachinesNeeded) {
                    machinesNeeded = Math.max(0, stageMachinesNeeded - stageAllocated);
                }
                stageAllocated += machinesNeeded;
                int operatorsNeeded = machinesNeeded * ws.operatorsPerMachine();
                stageOperators += operatorsNeeded;

                List<Map<String, Object>> machines = new ArrayList<>();
                for (int m = 0; m < machinesNeeded && m < slot.equipment().size(); m++) {
                    Equipment eq = slot.equipment().get(m);
                    Map<String, Object> machine = new LinkedHashMap<>();
                    machine.put("code", eq.getEquipmentCode());
                    machine.put("name", eq.getEquipmentName());
                    machine.put("status", eq.getStatus());
                    machine.put("statusLabel", statusLabel(eq.getStatus()));
                    machines.add(machine);
                }

                int utilization = slot.available() > 0
                        ? (int) Math.min(100, Math.round(machinesNeeded * 100.0 / slot.available()))
                        : 100;

                Map<String, Object> wsRow = new LinkedHashMap<>();
                wsRow.put("key", ws.key());
                wsRow.put("workshopName", ws.workshopName());
                wsRow.put("parentStepKey", stage.stepKey());
                wsRow.put("parentStepName", stage.stepName());
                wsRow.put("department", ws.department());
                wsRow.put("steps", List.of(stage.stepName()));
                wsRow.put("requiredMachines", machinesNeeded);
                wsRow.put("requiredOperators", operatorsNeeded);
                wsRow.put("availableMachines", slot.available());
                wsRow.put("idleMachines", slot.idle());
                wsRow.put("availableOperators", operatorPool.size());
                wsRow.put("utilization", utilization);
                wsRow.put("machines", machines);
                wsRow.put("status", utilization >= 90 ? "warning" : machinesNeeded > 0 ? "running" : "pending");
                workshopPlans.add(wsRow);
                stageWorkshopNames.add(ws.workshopName());

                if (machinesNeeded > 0) {
                    Equipment primary = slot.equipment().isEmpty() ? null : slot.equipment().get(0);
                    Map<String, Object> suggestion = new LinkedHashMap<>();
                    suggestion.put("processStep", stage.stepName());
                    suggestion.put("workshop", ws.workshopName());
                    suggestion.put("department", ws.department());
                    suggestion.put("equipment", primary != null ? primary.getEquipmentName() : ws.workshopName() + "设备");
                    suggestion.put("equipmentCode", primary != null ? primary.getEquipmentCode() : "");
                    suggestion.put("planQty", recommendedPlanQty > 0 ? recommendedPlanQty : quantity);
                    suggestion.put("requiredMachines", machinesNeeded);
                    suggestion.put("requiredOperators", operatorsNeeded);
                    suggestion.put("idleMachines", slot.idle());
                    suggestion.put("operatorRole", "operator");
                    dispatchSuggestions.add(suggestion);
                }

                totalMachines += machinesNeeded;
                totalOperators += operatorsNeeded;
            }

            Map<String, Object> stageRow = new LinkedHashMap<>();
            stageRow.put("stepKey", stage.stepKey());
            stageRow.put("stepName", stage.stepName());
            stageRow.put("stepOrder", stage.stepOrder());
            stageRow.put("workshopCount", stage.workshopCount());
            stageRow.put("workshops", stageWorkshopNames);
            stageRow.put("requiredMachines", stageAllocated);
            stageRow.put("requiredOperators", stageOperators);
            stageRow.put("availableMachines", totalIdleMachines);
            stageRow.put("equipmentType", stage.equipmentType());
            stagePlans.add(stageRow);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", orderNo);
        result.put("productModel", item != null ? item.getProductName() : "");
        result.put("orderQuantity", orderQuantity);
        result.put("quantity", recommendedPlanQty > 0 ? recommendedPlanQty : orderQuantity);
        result.put("recommendedPlanQty", recommendedPlanQty);
        result.put("shipFromStock", shipFromStock);
        result.put("inventoryCheck", inventoryCheck);
        result.put("planStart", planStart.toString());
        result.put("planEnd", planEnd.toString());
        result.put("workDays", workDays);
        result.put("dailyTarget", round1(dailyTarget));
        result.put("totalMachines", totalMachines);
        result.put("totalOperators", totalOperators);
        result.put("availableOperators", operatorPool.size());
        result.put("workshops", workshopPlans);
        result.put("processStages", stagePlans);
        result.put("dispatchSuggestions", dispatchSuggestions);
        result.put("decision", inventoryCheck.get("decision"));
        result.put("recommendation", inventoryCheck.get("recommendation"));
        result.put("capacityAnalysis", capacityPlan.toMap());
        result.put("planExplanation", buildPlanExplanation(orderNo, inventoryCheck, capacityPlan,
                workDays, dailyTarget, totalMachines, totalOperators));
        result.put("summary", buildSummary(orderNo, orderQuantity, recommendedPlanQty, shipFromStock,
                inventoryCheck, workDays, dailyTarget, totalMachines, totalOperators, workshopPlans.size()));
        result.put("schedulingSteps", buildSchedulingSteps(orderNo, orderQuantity, planStart, planEnd,
                item, workDays, steps, inventoryCheck, capacityPlan, workshopPlans, dispatchSuggestions,
                recommendedPlanQty, allEquipment, operatorPool));
        result.put("evidenceBase", buildEvidenceBase(orderNo, orderQuantity, planStart, planEnd, item,
                inventoryCheck, capacityPlan, workshopPlans, recommendedPlanQty, allEquipment, operatorPool));
        return result;
    }

    private List<Map<String, Object>> buildEvidenceBase(String orderNo, int orderQuantity,
                                                        LocalDate planStart, LocalDate planEnd,
                                                        CustomerOrderItem item,
                                                        Map<String, Object> inventoryCheck,
                                                        CapacityPlan capacityPlan,
                                                        List<Map<String, Object>> workshopPlans,
                                                        int recommendedPlanQty,
                                                        List<Equipment> allEquipment,
                                                        List<User> operatorPool) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> materialChecks = (List<Map<String, Object>>) inventoryCheck
                .getOrDefault("materialChecks", List.of());
        List<Map<String, Object>> list = new ArrayList<>();
        String product = item != null ? item.getProductName() : "—";
        long workDays = Math.max(1, ChronoUnit.DAYS.between(planStart, planEnd) + 1);

        Map<String, Object> orderMetrics = new LinkedHashMap<>();
        orderMetrics.put("订单号", orderNo);
        orderMetrics.put("产品型号", product);
        orderMetrics.put("订购数量", orderQuantity + " 台");
        orderMetrics.put("计划开始", planStart.toString());
        orderMetrics.put("计划截止", planEnd.toString());
        orderMetrics.put("计划天数", workDays + " 天");
        list.add(evidenceItem("ev-order", "ERP", "订单", orderNo,
                "客户订单主数据",
                String.format("【订单数据】%s 订购 %s %d 台，要求在 %s ~ %s（%d 天）内完成交付。",
                        orderNo, product, orderQuantity, planStart, planEnd, workDays),
                96, List.of("order"), orderMetrics));

        int fgStock = intVal(inventoryCheck.get("finishedGoodsStock"));
        int shipStock = intVal(inventoryCheck.get("shipFromStock"));
        int needProduce = intVal(inventoryCheck.get("needToProduce"));
        Map<String, Object> invMetrics = new LinkedHashMap<>();
        invMetrics.put("成品可用库存", fgStock + " 台");
        invMetrics.put("可现货发货", shipStock + " 台");
        invMetrics.put("需生产补足", needProduce + " 台");
        list.add(evidenceItem("ev-inv", "WMS", "库存", "FG-STOCK",
                "成品库存核查结果",
                String.format("【库存数据】成品仓现有 %d 台，其中 %d 台可直接发货；订单缺口 %d 台需排产。",
                        fgStock, shipStock, needProduce),
                92, List.of("inventory"), invMetrics));

        StringBuilder bomSnippet = new StringBuilder("【BOM 物料数据】");
        for (Map<String, Object> mat : materialChecks) {
            if ("FINISHED".equals(mat.get("materialType"))) {
                continue;
            }
            bomSnippet.append(String.format(" %s：可用 %s、需求 %s、可支撑 %s 台；",
                    mat.get("materialName"), mat.get("available"), mat.get("requiredForPlan"), mat.get("maxSupportQty")));
            Map<String, Object> matMetrics = new LinkedHashMap<>();
            matMetrics.put("物料编码", mat.get("materialCode"));
            matMetrics.put("可用量", mat.get("available"));
            matMetrics.put("计划需求量", mat.get("requiredForPlan"));
            matMetrics.put("可支撑产量", mat.get("maxSupportQty") + " 台");
            matMetrics.put("是否充足", Boolean.TRUE.equals(mat.get("sufficient")) ? "是" : "否");
            list.add(evidenceItem("ev-mat-" + mat.get("materialCode"), "BOM", "物料",
                    String.valueOf(mat.get("materialCode")),
                    String.valueOf(mat.get("materialName")),
                    String.format("可用 %s %s，按 BOM 可支撑生产 %s 台%s",
                            mat.get("available"), mat.get("unit"), mat.get("maxSupportQty"),
                            Boolean.TRUE.equals(mat.get("sufficient")) ? "" : "（不足）"),
                    88, List.of("material"), matMetrics));
        }
        Map<String, Object> bomSummary = new LinkedHashMap<>();
        bomSummary.put("核查物料种数", materialChecks.size());
        bomSummary.put("物料支撑上限", intVal(inventoryCheck.get("recommendedPlanQty")) + " 台");
        list.add(evidenceItem("ev-bom", "BOM", "汇总", "BOM-ALL",
                "BOM 齐套汇总",
                bomSnippet + String.format(" 物料维度最多可排 %d 台。", intVal(inventoryCheck.get("recommendedPlanQty"))),
                88, List.of("material"), bomSummary));

        int totalEq = allEquipment.size();
        long idleEq = allEquipment.stream().filter(e -> "IDLE".equals(e.getStatus())).count();
        long runEq = allEquipment.stream().filter(e -> "RUNNING".equals(e.getStatus())).count();
        long faultEq = allEquipment.stream().filter(e -> "FAULT".equals(e.getStatus()) || "MAINTENANCE".equals(e.getStatus())).count();
        Map<String, Object> eqMetrics = new LinkedHashMap<>();
        eqMetrics.put("设备总数", totalEq + " 台");
        eqMetrics.put("运行中", runEq + " 台");
        eqMetrics.put("空闲", idleEq + " 台");
        eqMetrics.put("故障/维保", faultEq + " 台");
        eqMetrics.put("产能上限", capacityPlan.equipmentLimit() + " 台/周期");
        eqMetrics.put("覆盖车间", workshopPlans.size() + " 个");
        list.add(evidenceItem("ev-eq", "MES", "设备", "EQ-POOL",
                "设备资源池",
                String.format("【设备数据】全厂 %d 台设备（运行 %d、空闲 %d、故障/维保 %d），本周期设备产能上限 %d 台。",
                        totalEq, runEq, idleEq, faultEq, capacityPlan.equipmentLimit()),
                90, List.of("equipment", "allocate"), eqMetrics));

        for (Map<String, Object> alloc : capacityPlan.allocations()) {
            Map<String, Object> allocMetrics = new LinkedHashMap<>();
            allocMetrics.put("工序", alloc.get("stepName"));
            allocMetrics.put("车间", alloc.get("workshopName"));
            allocMetrics.put("分配设备", alloc.get("machines") + " 台");
            allocMetrics.put("分配人员", alloc.get("operators") + " 人");
            allocMetrics.put("日产能", alloc.get("dailyCapacity") + " 台");
            list.add(evidenceItem("ev-alloc-" + alloc.get("stepName"), "MES", "分配",
                    String.valueOf(alloc.get("stepName")),
                    String.valueOf(alloc.get("workshopName")) + " · " + alloc.get("stepName"),
                    String.format("【分配数据】%s 投入设备 %s 台、人员 %s 人，日产能 %s 台。",
                            alloc.get("stepName"), alloc.get("machines"), alloc.get("operators"), alloc.get("dailyCapacity")),
                    89, List.of("allocate"), allocMetrics));
        }

        String operatorNames = operatorPool.stream()
                .map(u -> u.getRealName() + "(" + u.getDepartment() + ")")
                .collect(Collectors.joining("、"));
        if (operatorNames.isBlank()) {
            operatorNames = "暂无在岗操作员";
        }
        Map<String, Object> hrMetrics = new LinkedHashMap<>();
        hrMetrics.put("在岗操作员", operatorPool.size() + " 人");
        hrMetrics.put("人员可行产量", capacityPlan.operatorLimit() + " 台");
        hrMetrics.put("人员名单", operatorNames);
        list.add(evidenceItem("ev-hr", "HR", "人员", "OP-POOL",
                "操作员编制",
                String.format("【人员数据】在岗操作员 %d 人：%s；人员维度可行产量 %d 台。",
                        operatorPool.size(), operatorNames, capacityPlan.operatorLimit()),
                87, List.of("operator", "allocate"), hrMetrics));

        Map<String, Object> capMetrics = new LinkedHashMap<>();
        capMetrics.put("建议排产量", recommendedPlanQty + " 台");
        capMetrics.put("物料上限", capacityPlan.materialLimit() + " 台");
        capMetrics.put("设备上限", capacityPlan.equipmentLimit() + " 台");
        capMetrics.put("人员上限", capacityPlan.operatorLimit() + " 台");
        capMetrics.put("最终可行产量", capacityPlan.feasibleQty() + " 台");
        list.add(evidenceItem("ev-cap", "APS", "结论", "PLAN-RESULT",
                "排产结论",
                String.format("【排产结论】综合约束后建议排产 %d 台（物料上限 %d、设备上限 %d、人员上限 %d，可行 %d）。",
                        recommendedPlanQty, capacityPlan.materialLimit(), capacityPlan.equipmentLimit(),
                        capacityPlan.operatorLimit(), capacityPlan.feasibleQty()),
                95, List.of("result"), capMetrics));
        return list;
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

    private List<Map<String, Object>> buildSchedulingSteps(String orderNo, int orderQuantity,
                                                           LocalDate planStart, LocalDate planEnd,
                                                           CustomerOrderItem item, long workDays,
                                                           List<ProcessStep> steps,
                                                           Map<String, Object> inventoryCheck,
                                                           CapacityPlan capacityPlan,
                                                           List<Map<String, Object>> workshopPlans,
                                                           List<Map<String, Object>> dispatchSuggestions,
                                                           int recommendedPlanQty,
                                                           List<Equipment> allEquipment,
                                                           List<User> operatorPool) {
        List<Map<String, Object>> flow = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> materialChecks = (List<Map<String, Object>>) inventoryCheck
                .getOrDefault("materialChecks", List.of());
        @SuppressWarnings("unchecked")
        List<String> bottlenecks = (List<String>) inventoryCheck.getOrDefault("bottlenecks", List.of());
        int insufficient = (int) materialChecks.stream().filter(m -> !Boolean.TRUE.equals(m.get("sufficient"))).count();
        String product = item != null ? item.getProductName() : "—";
        String bottleneckText = bottlenecks.isEmpty() ? "无" : String.join("、", bottlenecks);
        int fgStock = intVal(inventoryCheck.get("finishedGoodsStock"));
        int shipStock = intVal(inventoryCheck.get("shipFromStock"));
        int needProduce = intVal(inventoryCheck.get("needToProduce"));
        int totalEq = allEquipment.size();
        long idleEq = allEquipment.stream().filter(e -> "IDLE".equals(e.getStatus())).count();
        String operatorSummary = operatorPool.stream()
                .map(u -> u.getRealName())
                .collect(Collectors.joining("、"));

        List<String> orderLines = List.of(
                String.format("订单号：%s", orderNo),
                String.format("产品型号：%s", product),
                String.format("订购数量：%d 台", orderQuantity),
                String.format("计划窗口：%s 至 %s（%d 天）", planStart, planEnd, workDays),
                "交期约束：计划截止日为排产硬约束，不可突破");
        flow.add(thoughtStep("order", "订单分析员", "发现", "发现",
                "读取 ERP 客户订单",
                String.format("「%s」读取订单：%s 订购 %d 台，交期 %d 天。", orderNo, product, orderQuantity, workDays),
                String.format("【订单分析员】读取订单 %s：产品「%s」，客户订购 %d 台，计划窗口 %s 至 %s（共 %d 天），将交期设为排产硬约束。",
                        orderNo, product, orderQuantity, planStart, planEnd, workDays),
                orderLines, 1));

        List<String> invLines = List.of(
                String.format("成品可用库存：%d 台", fgStock),
                String.format("可现货直发：%d 台", shipStock),
                String.format("订单缺口：%d 台", needProduce),
                String.format("订单需求：%d 台", orderQuantity));
        flow.add(thoughtStep("inventory", "库存协调员", "执行", "执行",
                "核查 WMS 成品库存",
                String.format("「成品仓」库存 %d 台，可直发 %d 台，缺口 %d 台；累计证据库 2 条。", fgStock, shipStock, needProduce),
                String.format("【库存协调员】查询成品仓：现有库存 %d 台，可现货直发 %d 台，仍需生产 %d 台才能满足订单 %d 台。",
                        fgStock, shipStock, needProduce, orderQuantity),
                invLines, 2));

        List<String> matLines = new ArrayList<>();
        for (Map<String, Object> mat : materialChecks) {
            if ("FINISHED".equals(mat.get("materialType"))) {
                continue;
            }
            matLines.add(String.format("%s：可用 %s %s，需求 %s %s，可支撑 %s 台%s",
                    mat.get("materialName"), mat.get("available"), mat.get("unit"),
                    mat.get("requiredForPlan"), mat.get("unit"), mat.get("maxSupportQty"),
                    Boolean.TRUE.equals(mat.get("sufficient")) ? "" : "（不足）"));
        }
        if (matLines.isEmpty()) {
            matLines.add("无子项物料需核查");
        }
        matLines.add(String.format("瓶颈物料：%s", bottleneckText));
        matLines.add(String.format("物料维度最多支撑：%d 台", intVal(inventoryCheck.get("recommendedPlanQty"))));
        StringBuilder matAction = new StringBuilder();
        for (Map<String, Object> mat : materialChecks) {
            if ("FINISHED".equals(mat.get("materialType"))) {
                continue;
            }
            matAction.append(String.format("%s(可用%s/需%s) ", mat.get("materialName"), mat.get("available"), mat.get("requiredForPlan")));
        }
        flow.add(thoughtStep("material", "物料计划员", "执行", "执行",
                "展开 BOM 做齐套分析",
                String.format("「BOM」核查 %d 种原材料，%d 项不足，物料上限 %d 台；累计证据库 %d 条。",
                        Math.max(0, materialChecks.size() - 1), insufficient,
                        intVal(inventoryCheck.get("recommendedPlanQty")), 2 + matLines.size()),
                String.format("【物料计划员】核查 %d 种原材料：%d 项不足，瓶颈物料「%s」。明细：%s物料维度最多支撑 %d 台。",
                        materialChecks.size() - 1, insufficient, bottleneckText,
                        matAction.length() > 0 ? matAction : "无子项 ",
                        intVal(inventoryCheck.get("recommendedPlanQty"))),
                matLines, 2 + matLines.size()));

        long runEq = allEquipment.stream().filter(e -> "RUNNING".equals(e.getStatus())).count();
        List<String> eqLines = List.of(
                String.format("设备总数：%d 台", totalEq),
                String.format("空闲：%d 台，运行中：%d 台", idleEq, runEq),
                String.format("工艺工序：%d 道", steps.size()),
                String.format("本周期设备产能上限：%d 台", capacityPlan.equipmentLimit()),
                String.format("瓶颈工序：%s", capacityPlan.limits().isEmpty() ? "无" : capacityPlan.limits().get(0)));
        flow.add(thoughtStep("equipment", "设备调度员", "派遣", "派遣",
                "统计设备资源并测算产能",
                String.format("「设备池」扫描 %d 台（空闲 %d），设备上限 %d 台；累计证据库 %d 条。",
                        totalEq, idleEq, capacityPlan.equipmentLimit(), 3 + eqLines.size()),
                String.format("【设备调度员】扫描全厂 %d 台设备（空闲 %d 台），加载 %d 道工序工艺；测算得本周期设备产能上限 %d 台。",
                        totalEq, idleEq, steps.size(), capacityPlan.equipmentLimit()),
                eqLines, 3 + eqLines.size()));

        List<String> opLines = new ArrayList<>();
        opLines.add(String.format("在岗操作员：%d 人", operatorPool.size()));
        for (User u : operatorPool) {
            opLines.add(String.format("· %s（%s）", u.getRealName(), u.getDepartment()));
        }
        if (operatorPool.isEmpty()) {
            opLines.add("· 暂无在岗操作员");
        }
        opLines.add(String.format("人员可行产量：%d 台", capacityPlan.operatorLimit()));
        flow.add(thoughtStep("operator", "人员协调员", "派遣", "派遣",
                "统计操作员编制与负荷",
                String.format("「人员池」在岗 %d 人，可行产量 %d 台；累计证据库 %d 条。",
                        operatorPool.size(), capacityPlan.operatorLimit(), 4 + opLines.size()),
                String.format("【人员协调员】在岗操作员 %d 人（%s），结合在途派工测算人员可行产量 %d 台。",
                        operatorPool.size(),
                        operatorSummary.isBlank() ? "暂无" : operatorSummary,
                        capacityPlan.operatorLimit()),
                opLines, 4 + opLines.size()));

        List<String> allocLines = new ArrayList<>();
        for (Map<String, Object> ws : workshopPlans) {
            allocLines.add(String.format("%s：设备 %s 台、人员 %s 人、负荷 %s%%",
                    ws.get("workshopName"), ws.get("requiredMachines"), ws.get("requiredOperators"), ws.get("utilization")));
        }
        StringBuilder allocDetail = new StringBuilder();
        for (Map<String, Object> ws : workshopPlans) {
            allocDetail.append(String.format("%s(设备%d/人%d) ",
                    ws.get("workshopName"), ws.get("requiredMachines"), ws.get("requiredOperators")));
        }
        flow.add(thoughtStep("allocate", "产能优化员", "执行", "执行",
                "分配车间设备与工序资源",
                String.format("「车间分配」%d 道工序 → %d 个车间，生成 %d 条派工建议；累计证据库 %d 条。",
                        steps.size(), workshopPlans.size(), dispatchSuggestions.size(), 5 + allocLines.size()),
                String.format("【产能优化员】将 %d 道工序分配至 %d 个车间：%s共生成 %d 条工序派工建议。",
                        steps.size(), workshopPlans.size(), allocDetail, dispatchSuggestions.size()),
                allocLines, 5 + allocLines.size()));

        List<String> resultLines = List.of(
                String.format("订单数量：%d 台", orderQuantity),
                String.format("物料上限：%d 台", capacityPlan.materialLimit()),
                String.format("设备上限：%d 台", capacityPlan.equipmentLimit()),
                String.format("人员上限：%d 台", capacityPlan.operatorLimit()),
                String.format("综合可行产量：%d 台", capacityPlan.feasibleQty()),
                String.format("建议排产量：%d 台", recommendedPlanQty),
                String.valueOf(inventoryCheck.getOrDefault("recommendation", "")));
        flow.add(thoughtStep("result", "计划汇总员", "发现", "发现",
                "汇总约束输出生产计划",
                String.format("「排产结论」建议排产 %d 台（订单 %d 台）；累计证据库 %d 条。",
                        recommendedPlanQty, orderQuantity, 6 + resultLines.size()),
                String.format("【计划汇总员】综合订单 %d 台、库存、物料、设备、人员约束，输出建议排产量 %d 台。%s",
                        orderQuantity, recommendedPlanQty,
                        inventoryCheck.getOrDefault("recommendation", "")),
                resultLines, 6 + resultLines.size()));
        return flow;
    }

    private Map<String, Object> thoughtStep(String key, String agentName, String actionType, String badge,
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

    private Map<String, Object> schedulingStep(String key, String title, String detail) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("title", title);
        row.put("detail", detail);
        row.put("status", "success");
        return row;
    }

    private CapacityPlan buildCapacityPlan(List<ProcessStep> steps, List<Equipment> allEquipment,
                                           List<User> operatorPool, int targetQty, long workDays) {
        if (targetQty <= 0) {
            return new CapacityPlan(0, 0, 0, 0, operatorPool.size(), List.of(), List.of());
        }

        List<StepCapacity> capacities = steps.stream()
                .map(step -> buildStepCapacity(step, allEquipment, workDays, targetQty))
                .toList();
        int equipmentLimit = capacities.stream()
                .mapToInt(StepCapacity::maxByEquipment)
                .min()
                .orElse(targetQty);

        int feasibleQty = 0;
        List<String> limits = new ArrayList<>();
        List<Map<String, Object>> allocations = List.of();

        // 离散可行性搜索：从订单缺口向下找第一个同时满足设备和人员约束的产量。
        for (int qty = Math.min(targetQty, equipmentLimit); qty >= 1; qty--) {
            AllocationResult allocation = allocateForQuantity(capacities, operatorPool.size(), qty, workDays);
            if (allocation.feasible()) {
                feasibleQty = qty;
                allocations = allocation.allocations();
                limits = buildCapacityLimits(capacities, operatorPool.size(), qty, targetQty, equipmentLimit, allocation.requiredOperators());
                break;
            }
        }

        int operatorLimit = estimateOperatorLimit(capacities, operatorPool.size(), targetQty, workDays);
        if (feasibleQty == 0) {
            limits = buildCapacityLimits(capacities, operatorPool.size(), 0, targetQty, equipmentLimit, 0);
        }
        return new CapacityPlan(targetQty, feasibleQty, equipmentLimit, operatorLimit,
                operatorPool.size(), limits, allocations);
    }

    private StepCapacity buildStepCapacity(ProcessStep step, List<Equipment> allEquipment, long workDays, int targetQty) {
        String equipType = normalizeType(step.getStandardEquipmentType());
        List<Equipment> allAvailable = availableEquipment(allEquipment);
        List<Equipment> pool = allAvailable.stream()
                .filter(e -> equipType.equals(normalizeType(e.getEquipmentType())))
                .toList();
        int availableMachines = pool.size();
        if (availableMachines == 0) {
            availableMachines = Math.max(1, allAvailable.size());
        }
        double hours = step.getStandardWorkHours() != null ? step.getStandardWorkHours().doubleValue() : 0.5;
        if (hours <= 0) {
            hours = 0.5;
        }
        double dailyPerMachine = SHIFT_HOURS / hours;
        if (targetQty > 0 && workDays > 0) {
            int neededMachines = (int) Math.ceil((targetQty / (double) workDays) / Math.max(1.0, dailyPerMachine));
            availableMachines = Math.min(Math.max(availableMachines, neededMachines), Math.max(availableMachines, allAvailable.size()));
        }
        int maxByEquipment = (int) Math.floor(availableMachines * dailyPerMachine * workDays);
        return new StepCapacity(step, resolveWorkshopLabel(step, pool), availableMachines,
                resolveOperatorsPerMachine(step), dailyPerMachine, Math.max(0, maxByEquipment));
    }

    private String resolveWorkshopLabel(ProcessStep step, List<Equipment> pool) {
        if (!pool.isEmpty()) {
            String workshop = pool.get(0).getWorkshop();
            if (workshop != null && !workshop.isBlank()) {
                return workshop;
            }
        }
        return step.getStepName() != null ? step.getStepName() : "未命名工序";
    }

    private int resolveOperatorsPerMachine(ProcessStep step) {
        String name = step.getStepName() != null ? step.getStepName() : "";
        if (name.contains("整机组装") || (name.contains("组装") && !name.contains("主板"))) {
            return 2;
        }
        return 1;
    }

    private AllocationResult allocateForQuantity(List<StepCapacity> capacities, int operatorCount,
                                                 int qty, long workDays) {
        List<Map<String, Object>> allocations = new ArrayList<>();
        int peakOperators = 0;
        for (StepCapacity cap : capacities) {
            int machines = (int) Math.ceil((qty / (double) workDays) / Math.max(1.0, cap.dailyPerMachine()));
            machines = Math.min(Math.max(1, machines), Math.max(1, cap.availableMachines()));
            int stepOperators = machines * cap.operatorsPerMachine();
            peakOperators = Math.max(peakOperators, stepOperators);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stepName", cap.step().getStepName());
            row.put("workshopName", cap.workshopLabel());
            row.put("equipmentType", cap.step().getStandardEquipmentType());
            row.put("machines", machines);
            row.put("operators", stepOperators);
            row.put("dailyCapacity", round1(machines * cap.dailyPerMachine()));
            row.put("maxByEquipment", cap.maxByEquipment());
            allocations.add(row);
        }
        return new AllocationResult(peakOperators <= Math.max(operatorCount, 1), peakOperators, allocations);
    }

    private int estimateOperatorLimit(List<StepCapacity> capacities, int operatorCount, int targetQty, long workDays) {
        if (operatorCount <= 0 || capacities.isEmpty()) {
            return 0;
        }
        int best = 0;
        int upper = capacities.stream().mapToInt(StepCapacity::maxByEquipment).min().orElse(targetQty);
        for (int qty = Math.min(targetQty, upper); qty >= 1; qty--) {
            if (allocateForQuantity(capacities, operatorCount, qty, workDays).feasible()) {
                best = qty;
                break;
            }
        }
        return best;
    }

    private List<String> buildCapacityLimits(List<StepCapacity> capacities, int operatorCount,
                                             int feasibleQty, int targetQty, int equipmentLimit,
                                             int requiredOperators) {
        List<String> limits = new ArrayList<>();
        if (equipmentLimit < targetQty) {
            capacities.stream()
                    .filter(c -> c.maxByEquipment() == equipmentLimit)
                    .forEach(c -> limits.add(String.format("%s（%s）可用设备 %d 台，周期内最多支撑 %d 台",
                            c.step().getStepName(),
                            c.step().getStandardEquipmentType(),
                            c.availableMachines(),
                            c.maxByEquipment())));
        }
        if (operatorCount <= 0) {
            limits.add("未配置可用生产操作员");
        } else if (requiredOperators > operatorCount || feasibleQty < Math.min(targetQty, equipmentLimit)) {
            limits.add(String.format("可用操作员 %d 人，无法支撑更高排产量", operatorCount));
        }
        if (limits.isEmpty()) {
            limits.add("设备与操作员数量可支撑建议排产量");
        }
        return limits;
    }

    private void applyCapacityDecision(Map<String, Object> inventoryCheck, int materialRecommendedQty,
                                       int capacityQty, CapacityPlan capacityPlan) {
        if (capacityQty >= materialRecommendedQty) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<String> bottlenecks = (List<String>) inventoryCheck.getOrDefault("bottlenecks", new ArrayList<String>());
        List<String> merged = new ArrayList<>(bottlenecks);
        merged.addAll(capacityPlan.limits());
        inventoryCheck.put("bottlenecks", merged);
        inventoryCheck.put("recommendedPlanQty", capacityQty);
        if (capacityQty > 0) {
            inventoryCheck.put("decision", "PARTIAL_PRODUCE");
            inventoryCheck.put("recommendation", String.format(
                    "物料可支撑 %d 台，但受设备/人员产能约束，本周期建议先排产 %d 台。瓶颈：%s",
                    materialRecommendedQty, capacityQty, String.join("、", capacityPlan.limits())));
        } else {
            inventoryCheck.put("decision", "CAPACITY_SHORTAGE");
            inventoryCheck.put("recommendation", String.format(
                    "当前设备或操作员资源不足，暂不建议创建生产计划。瓶颈：%s",
                    String.join("、", capacityPlan.limits())));
        }
    }

    private String buildPlanExplanation(String orderNo, Map<String, Object> inventoryCheck,
                                        CapacityPlan capacityPlan, long workDays, double dailyTarget,
                                        int totalMachines, int totalOperators) {
        int orderQty = intVal(inventoryCheck.get("orderQuantity"));
        int stockQty = intVal(inventoryCheck.get("shipFromStock"));
        int needQty = intVal(inventoryCheck.get("needToProduce"));
        int planQty = intVal(inventoryCheck.get("recommendedPlanQty"));
        if (planQty <= 0) {
            return String.format("订单 %s 共 %d 台，成品库存可发 %d 台，生产缺口 %d 台；%s",
                    orderNo, orderQty, stockQty, needQty, inventoryCheck.get("recommendation"));
        }
        return String.format(
                "订单 %s 共 %d 台，先使用成品库存 %d 台，剩余缺口 %d 台。算法在物料上限 %d 台、设备上限 %d 台、操作员约束可行 %d 台之间取最小可行值，建议排产 %d 台；周期 %d 天，日均 %.1f 台，需要 %d 台设备、%d 名操作员。",
                orderNo, orderQty, stockQty, needQty, capacityPlan.materialLimit(), capacityPlan.equipmentLimit(),
                capacityPlan.operatorLimit(), planQty, workDays, dailyTarget, totalMachines, totalOperators);
    }

    /**
     * 核查库存并计算建议排产数量：优先使用成品库存，再按 BOM 物料可用量约束生产量。
     */
    private Map<String, Object> buildInventoryAnalysis(CustomerOrderItem item, int orderQuantity) {
        Map<Long, Material> materialById = materialMapper.materialList().stream()
                .collect(Collectors.toMap(Material::getMaterialId, m -> m, (a, b) -> a));
        Long productMaterialId = item != null ? item.getMaterialId() : null;
        Material product = productMaterialId != null ? materialById.get(productMaterialId) : null;

        int finishedAvailable = sumAvailableQty(productMaterialId);
        int shipFromStock = Math.min(orderQuantity, finishedAvailable);
        int needToProduce = Math.max(0, orderQuantity - shipFromStock);

        List<Map<String, Object>> materialChecks = new ArrayList<>();
        Map<String, Object> finishedRow = new LinkedHashMap<>();
        finishedRow.put("materialCode", product != null ? product.getMaterialCode() : "-");
        finishedRow.put("materialName", product != null ? product.getMaterialName() : (item != null ? item.getProductName() : "成品"));
        finishedRow.put("materialType", "FINISHED");
        finishedRow.put("unit", product != null ? product.getUnit() : "台");
        finishedRow.put("available", finishedAvailable);
        finishedRow.put("required", orderQuantity);
        finishedRow.put("requiredForPlan", shipFromStock);
        finishedRow.put("maxSupportQty", finishedAvailable);
        finishedRow.put("shortage", Math.max(0, orderQuantity - finishedAvailable));
        finishedRow.put("sufficient", finishedAvailable >= orderQuantity);
        finishedRow.put("remark", finishedAvailable >= orderQuantity ? "成品库存可满足订单" : "成品库存不足，需安排生产");
        materialChecks.add(finishedRow);

        List<Bom> bomLines = bomMapper.bomList().stream()
                .filter(b -> productMaterialId != null && productMaterialId.equals(b.getParentMaterialId()))
                .toList();

        int maxFromMaterials = needToProduce > 0 ? Integer.MAX_VALUE : 0;
        List<String> bottlenecks = new ArrayList<>();

        for (Bom bom : bomLines) {
            Material child = materialById.get(bom.getChildMaterialId());
            if (child == null) {
                continue;
            }
            double perUnit = bomQtyWithLoss(bom);
            int available = sumAvailableQty(bom.getChildMaterialId());
            int requiredForPlan = needToProduce > 0 ? (int) Math.ceil(needToProduce * perUnit) : 0;
            int maxSupport = perUnit > 0 ? (int) Math.floor(available / perUnit) : 0;
            maxFromMaterials = Math.min(maxFromMaterials, maxSupport);

            boolean sufficient = available >= requiredForPlan;
            if (!sufficient && needToProduce > 0) {
                bottlenecks.add(child.getMaterialName());
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("materialCode", child.getMaterialCode());
            row.put("materialName", child.getMaterialName());
            row.put("materialType", child.getMaterialType());
            row.put("unit", child.getUnit());
            row.put("available", available);
            row.put("requiredPerUnit", round1(perUnit));
            row.put("requiredForPlan", requiredForPlan);
            row.put("maxSupportQty", maxSupport);
            row.put("shortage", Math.max(0, requiredForPlan - available));
            row.put("sufficient", sufficient);
            row.put("remark", sufficient ? "物料充足" : String.format("缺口 %d %s", Math.max(0, requiredForPlan - available), child.getUnit()));
            materialChecks.add(row);
        }

        if (bomLines.isEmpty() && needToProduce > 0) {
            maxFromMaterials = needToProduce;
        }

        int recommendedPlanQty = 0;
        if (needToProduce > 0) {
            recommendedPlanQty = maxFromMaterials == Integer.MAX_VALUE
                    ? needToProduce
                    : Math.min(needToProduce, Math.max(0, maxFromMaterials));
        }

        String decision;
        String recommendation;
        if (needToProduce == 0) {
            decision = "FULL_STOCK";
            recommendation = String.format("成品仓可用 %d 台，订单 %d 台可全部现货交付，无需新建生产计划。",
                    finishedAvailable, orderQuantity);
        } else if (recommendedPlanQty >= needToProduce) {
            decision = shipFromStock > 0 ? "PARTIAL_STOCK" : "PRODUCE_ALL";
            recommendation = shipFromStock > 0
                    ? String.format("建议现货发 %d 台、排产 %d 台，物料可支撑全部缺口。",
                    shipFromStock, recommendedPlanQty)
                    : String.format("成品库存不足，建议排产 %d 台，物料可完全支撑。", recommendedPlanQty);
        } else if (recommendedPlanQty > 0) {
            decision = "PARTIAL_PRODUCE";
            recommendation = String.format("物料存在缺口，最多可排产 %d 台（订单缺口 %d 台）。瓶颈：%s",
                    recommendedPlanQty, needToProduce, String.join("、", bottlenecks));
        } else {
            decision = "MATERIAL_SHORTAGE";
            recommendation = String.format("原材料不足，当前无法排产。请先采购：%s",
                    bottlenecks.isEmpty() ? "关键物料" : String.join("、", bottlenecks));
        }

        Map<String, Object> check = new LinkedHashMap<>();
        check.put("orderQuantity", orderQuantity);
        check.put("finishedGoodsStock", finishedAvailable);
        check.put("shipFromStock", shipFromStock);
        check.put("needToProduce", needToProduce);
        check.put("recommendedPlanQty", recommendedPlanQty);
        check.put("materialChecks", materialChecks);
        check.put("bottlenecks", bottlenecks);
        check.put("decision", decision);
        check.put("recommendation", recommendation);
        return check;
    }

    private String buildSummary(String orderNo, int orderQuantity, int recommendedPlanQty, int shipFromStock,
                                Map<String, Object> inventoryCheck, long workDays, double dailyTarget,
                                int totalMachines, int totalOperators, int workshopCount) {
        String invPart = String.valueOf(inventoryCheck.get("recommendation"));
        if (recommendedPlanQty <= 0) {
            return String.format("订单 %s：%s", orderNo, invPart);
        }
        return String.format(
                "订单 %s 共 %d 台。%s 建议生产计划 %d 台，周期 %d 天（日均 %.1f 台），投入 %d 台设备、%d 名操作员，覆盖 %d 个车间。",
                orderNo, orderQuantity, invPart, recommendedPlanQty, workDays, dailyTarget,
                totalMachines, totalOperators, workshopCount);
    }

    private int sumAvailableQty(Long materialId) {
        if (materialId == null) {
            return 0;
        }
        return inventoryMapper.inventoryList().stream()
                .filter(inv -> materialId.equals(inv.getMaterialId()))
                .map(inv -> inv.getQuantityAvailable() != null ? inv.getQuantityAvailable() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .intValue();
    }

    private List<Equipment> availableEquipment(List<Equipment> allEquipment) {
        return allEquipment.stream()
                .filter(e -> {
                    String status = e.getStatus() != null ? e.getStatus() : "IDLE";
                    return !"FAULT".equals(status) && !"MAINTENANCE".equals(status);
                })
                .toList();
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

    private double bomQtyWithLoss(Bom bom) {
        double qty = bom.getQuantity() != null ? bom.getQuantity().doubleValue() : 1.0;
        double loss = bom.getLossRate() != null ? bom.getLossRate().doubleValue() : 0.0;
        return qty * (1.0 + loss);
    }

    private int intVal(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 批量智能生成生产计划预览：对待计划订单按交期、库存、设备可用性评分并排优先级。
     */
    public Map<String, Object> generateSmartPlanProposals() {
        List<CustomerOrder> pendingOrders = customerOrderMapper.customerOrderList().stream()
                .filter(o -> List.of("PLAN_PENDING", "APPROVED").contains(o.getAuditStatus()))
                .sorted(Comparator.comparing(CustomerOrder::getRequiredDeliveryDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<Map<String, Object>> proposals = new ArrayList<>();
        for (CustomerOrder order : pendingOrders) {
            proposals.add(buildSmartPlanProposal(order));
        }
        proposals.sort((a, b) -> Double.compare(
                doubleVal(b.get("priorityScore")),
                doubleVal(a.get("priorityScore"))));

        long feasibleCount = proposals.stream().filter(p -> Boolean.TRUE.equals(p.get("feasible"))).count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("proposals", proposals);
        result.put("total", proposals.size());
        result.put("feasibleCount", feasibleCount);
        result.put("generatedAt", java.time.LocalDateTime.now().toString().replace('T', ' '));
        result.put("summary", String.format("共 %d 笔待计划订单，%d 笔可智能生成生产计划",
                proposals.size(), feasibleCount));
        return result;
    }

    /**
     * 单笔订单智能计划提案（含优先级评分与风险提示）。
     */
    public Map<String, Object> buildSmartPlanProposal(CustomerOrder order) {
        CustomerOrderItem item = firstOrderItem(order.getOrderId());
        int orderQuantity = item != null ? item.getQuantity().intValue() : 0;
        String productModel = item != null ? item.getProductName() : "";

        LocalDate deliveryDate = order.getRequiredDeliveryDate() != null
                ? order.getRequiredDeliveryDate()
                : LocalDate.now().plusDays(14);
        long daysToDelivery = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), deliveryDate));

        Map<String, Object> inventoryCheck = buildInventoryAnalysis(item, orderQuantity);
        int needToProduce = intVal(inventoryCheck.get("needToProduce"));
        int recommendedPlanQty = intVal(inventoryCheck.get("recommendedPlanQty"));
        String decision = String.valueOf(inventoryCheck.getOrDefault("decision", ""));

        List<ProcessStep> steps = resolveRouteSteps(item);
        List<Equipment> allEquipment = equipmentMapper.equipmentList();
        List<User> operatorPool = activeOperators();
        int availableEquipCount = availableEquipment(allEquipment).size();
        int requiredEquipEstimate = Math.max(1, (int) Math.ceil(Math.max(needToProduce, 1) / 20.0));

        long workDays = Math.max(1, Math.min(daysToDelivery > 2 ? daysToDelivery - 2 : 7, 21));
        CapacityPlan capacityPlan = buildCapacityPlan(steps, allEquipment, operatorPool,
                Math.max(recommendedPlanQty, needToProduce), workDays);
        int feasibleQty = capacityPlan.feasibleQty();
        if (recommendedPlanQty > 0 && feasibleQty > 0 && feasibleQty < recommendedPlanQty) {
            recommendedPlanQty = feasibleQty;
            inventoryCheck.put("recommendedPlanQty", feasibleQty);
            applyCapacityDecision(inventoryCheck, intVal(inventoryCheck.get("recommendedPlanQty")), feasibleQty, capacityPlan);
            decision = String.valueOf(inventoryCheck.get("decision"));
        }

        int planQuantity = recommendedPlanQty;
        if (planQuantity <= 0 && needToProduce > 0) {
            planQuantity = 0;
        }

        if (planQuantity > 0) {
            workDays = Math.max(1, Math.min(workDays, Math.max(1, daysToDelivery > 2 ? daysToDelivery - 2 : 7)));
        }
        LocalDate planEnd = deliveryDate.minusDays(2);
        if (planEnd.isBefore(LocalDate.now())) {
            planEnd = LocalDate.now().plusDays(workDays);
        }
        LocalDate planStart = planEnd.minusDays(workDays - 1);
        if (planStart.isBefore(LocalDate.now())) {
            planStart = LocalDate.now();
        }

        Map<String, Object> scoreBreakdown = computePriorityScore(
                daysToDelivery, orderQuantity, inventoryCheck, availableEquipCount, requiredEquipEstimate);
        double priorityScore = doubleVal(scoreBreakdown.get("totalScore"));

        List<String> requiredEquipment = steps.stream()
                .map(ProcessStep::getStandardEquipmentType)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .toList();
        int requiredPersonnel = capacityPlan.allocations().stream()
                .mapToInt(a -> intVal(a.get("operators")))
                .sum();
        if (requiredPersonnel <= 0 && planQuantity > 0) {
            requiredPersonnel = Math.max(2, (int) Math.ceil(planQuantity / (double) workDays));
        }

        List<String> riskWarnings = buildRiskWarnings(inventoryCheck, capacityPlan, daysToDelivery,
                planStart, planEnd, operatorPool.size());

        String planStatus;
        boolean feasible;
        if ("FULL_STOCK".equals(decision)) {
            planStatus = "库存满足";
            feasible = false;
        } else if (planQuantity <= 0) {
            planStatus = "不可生成";
            feasible = false;
        } else if ("CAPACITY_SHORTAGE".equals(decision)) {
            planStatus = "产能不足";
            feasible = false;
        } else {
            planStatus = "可生成";
            feasible = true;
        }

        Map<String, Object> proposal = new LinkedHashMap<>();
        proposal.put("orderId", order.getOrderNo());
        proposal.put("orderNo", order.getOrderNo());
        proposal.put("customerName", order.getCustomerName());
        proposal.put("productModel", productModel);
        proposal.put("orderQuantity", orderQuantity);
        proposal.put("planQuantity", planQuantity);
        proposal.put("planStart", planStart.toString());
        proposal.put("planEnd", planEnd.toString());
        proposal.put("requiredDeliveryDate", deliveryDate.toString());
        proposal.put("requiredEquipment", requiredEquipment);
        proposal.put("requiredPersonnel", requiredPersonnel);
        proposal.put("availableOperators", operatorPool.size());
        proposal.put("availableEquipment", availableEquipCount);
        proposal.put("planStatus", planStatus);
        proposal.put("riskWarnings", riskWarnings);
        proposal.put("priorityScore", round1(priorityScore));
        proposal.put("priorityLevel", priorityLevel(priorityScore));
        proposal.put("scoreBreakdown", scoreBreakdown);
        proposal.put("feasible", feasible);
        proposal.put("decision", decision);
        proposal.put("inventoryCheck", inventoryCheck);
        proposal.put("capacityAnalysis", capacityPlan.toMap());
        proposal.put("recommendation", inventoryCheck.get("recommendation"));
        return proposal;
    }

    private Map<String, Object> computePriorityScore(long daysToDelivery, int orderQuantity,
                                                     Map<String, Object> inventoryCheck,
                                                     int availableEquip, int requiredEquip) {
        double deliveryScore = daysToDelivery <= 3 ? 100
                : daysToDelivery <= 7 ? 85
                : daysToDelivery <= 14 ? 70
                : daysToDelivery <= 30 ? 50
                : 30;

        int shipFromStock = intVal(inventoryCheck.get("shipFromStock"));
        int recommendedPlanQty = intVal(inventoryCheck.get("recommendedPlanQty"));
        int needToProduce = intVal(inventoryCheck.get("needToProduce"));
        double inventoryScore;
        if (needToProduce <= 0) {
            inventoryScore = 100;
        } else if (orderQuantity > 0) {
            double coverage = (shipFromStock + recommendedPlanQty) / (double) orderQuantity;
            inventoryScore = Math.min(100, coverage * 100);
        } else {
            inventoryScore = 50;
        }

        double equipmentScore = requiredEquip <= 0 ? 100
                : Math.min(100, availableEquip * 100.0 / requiredEquip);

        double totalScore = deliveryScore * 0.4 + inventoryScore * 0.3 + equipmentScore * 0.3;

        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("deliveryScore", round1(deliveryScore));
        breakdown.put("inventoryScore", round1(inventoryScore));
        breakdown.put("equipmentScore", round1(equipmentScore));
        breakdown.put("totalScore", round1(totalScore));
        breakdown.put("daysToDelivery", daysToDelivery);
        return breakdown;
    }

    private List<String> buildRiskWarnings(Map<String, Object> inventoryCheck, CapacityPlan capacityPlan,
                                             long daysToDelivery, LocalDate planStart, LocalDate planEnd,
                                             int availableOperators) {
        List<String> warnings = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<String> bottlenecks = (List<String>) inventoryCheck.getOrDefault("bottlenecks", List.of());
        for (String b : bottlenecks) {
            warnings.add("物料瓶颈：" + b);
        }
        if (daysToDelivery <= 5) {
            warnings.add("交期紧迫（剩余 " + daysToDelivery + " 天）");
        }
        if (ChronoUnit.DAYS.between(planStart, planEnd) < 3) {
            warnings.add("计划周期偏短，建议关注产能");
        }
        if (capacityPlan.feasibleQty() < intVal(inventoryCheck.get("needToProduce"))) {
            warnings.add("设备/人员产能不足，建议分批排产");
        }
        if (availableOperators <= 2) {
            warnings.add("操作员数量偏紧（可用 " + availableOperators + " 人）");
        }
        long faultCount = equipmentMapper.equipmentList().stream()
                .filter(e -> "FAULT".equals(e.getStatus()) || "MAINTENANCE".equals(e.getStatus()))
                .count();
        if (faultCount > 0) {
            warnings.add("有 " + faultCount + " 台设备故障或保养中");
        }
        if (warnings.isEmpty()) {
            warnings.add("无明显风险");
        }
        return warnings;
    }

    private String priorityLevel(double score) {
        if (score >= 80) {
            return "高";
        }
        if (score >= 60) {
            return "中";
        }
        return "低";
    }

    private double doubleVal(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public Map<String, Object> buildWorkshops3dSnapshot() {
        List<Equipment> allEquipment = equipmentMapper.equipmentList();
        List<DispatchTask> dispatches = dispatchTaskMapper.dispatchList();
        Map<Long, WorkOrder> woById = workOrderMapper.workOrderList().stream()
                .collect(Collectors.toMap(WorkOrder::getWorkOrderId, w -> w, (a, b) -> a));
        Map<Long, ProcessStep> stepById = processStepMapper.stepList().stream()
                .collect(Collectors.toMap(ProcessStep::getStepId, s -> s, (a, b) -> a));
        Map<Long, Equipment> equipmentById = allEquipment.stream()
                .collect(Collectors.toMap(Equipment::getEquipmentId, e -> e, (a, b) -> a));
        MesRuntimeState runtime = mesRuntimeStore.load();
        List<Map<String, Object>> workshops = new ArrayList<>();

        for (ProductionWorkshopCatalog.WorkshopDef ws : workshops()) {
            List<Equipment> inWorkshop = equipmentForWorkshop(ws, allEquipment);

            long running = inWorkshop.stream().filter(e -> "RUNNING".equals(e.getStatus())).count();
            long fault = inWorkshop.stream().filter(e -> "FAULT".equals(e.getStatus())).count();
            String status = fault > 0 ? "abnormal" : running > 0 ? "running" : "pending";

            List<Map<String, Object>> machines = inWorkshop.stream().map(eq -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("code", eq.getEquipmentCode());
                m.put("name", eq.getEquipmentName());
                m.put("type", eq.getEquipmentType());
                m.put("equipmentType", eq.getEquipmentType());
                m.put("status", eq.getStatus());
                m.put("statusLabel", statusLabel(eq.getStatus()));
                m.put("workstation", eq.getWorkstation());
                m.put("lineCode", resolveLineCode(eq.getWorkstation()));
                return m;
            }).collect(Collectors.toList());

            Map<String, Object> progressInfo = resolveWorkshopProgress(
                    ws, dispatches, woById, stepById, equipmentById, runtime, inWorkshop);
            boolean isRunning = Boolean.TRUE.equals(progressInfo.get("isRunning"));

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", ws.key());
            row.put("name", ws.workshopName());
            row.put("parentStepKey", ws.parentStepKey());
            ProductionWorkshopCatalog.ProcessStageDef parentStage = ProductionWorkshopCatalog.stageByKey(ws.parentStepKey());
            row.put("parentStepName", parentStage != null ? parentStage.stepName() : "");
            row.put("department", ws.department());
            row.put("taskTitle", ws.taskTitle());
            row.put("taskDescription", ws.taskDescription());
            row.put("workOrderNo", progressInfo.get("workOrderNo"));
            row.put("progress", progressInfo.get("progress"));
            row.put("completedQty", progressInfo.get("completedQty"));
            row.put("plannedQty", progressInfo.get("plannedQty"));
            row.put("batchCompletedQty", progressInfo.get("batchCompletedQty"));
            row.put("batchTargetQty", progressInfo.get("batchTargetQty"));
            row.put("batchProgress", progressInfo.get("batchProgress"));
            row.put("stepSeconds", progressInfo.get("stepSeconds"));
            row.put("isRunning", isRunning);
            row.put("currentStep", progressInfo.get("currentStep"));
            row.put("progressLabel", progressInfo.get("progressLabel"));
            row.put("total", inWorkshop.size());
            row.put("running", (int) running);
            row.put("idle", (int) inWorkshop.stream().filter(e -> "IDLE".equals(e.getStatus())).count());
            row.put("fault", (int) fault);
            row.put("abnormal", (int) fault);
            row.put("active", (int) running);
            row.put("status", fault > 0 ? "abnormal" : isRunning ? "running" : "pending");
            row.put("machines", machines);
            workshops.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", workshops);
        return result;
    }

    /**
     * 生产主管大屏总览：与智能排产共用车间/设备/产能模型，按产线聚合设备状态。
     */
    public Map<String, Object> buildProductionOverview() {
        Map<String, Object> workshops3d = buildWorkshops3dSnapshot();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workshops = new ArrayList<>((List<Map<String, Object>>) workshops3d.get("items"));

        List<ProcessStep> steps = processStepMapper.stepList();
        List<Equipment> allEquipment = equipmentMapper.equipmentList();
        List<User> operators = activeOperators();

        Map<String, StepCapacity> capByType = new LinkedHashMap<>();
        for (ProductionWorkshopCatalog.ProcessStageDef stage : ProductionWorkshopCatalog.PRODUCTION_STAGES) {
            ProcessStep step = steps.stream()
                    .filter(s -> ProductionWorkshopCatalog.matchesStage(s, stage))
                    .findFirst()
                    .orElse(null);
            if (step != null) {
                String type = normalizeType(step.getStandardEquipmentType());
                capByType.putIfAbsent(type, buildStepCapacity(step, allEquipment, 1L, 0));
            }
        }

        int totalEquipment = 0;
        int totalRunning = 0;
        int totalFault = 0;
        int totalIdle = 0;

        for (Map<String, Object> ws : workshops) {
            ProductionWorkshopCatalog.WorkshopDef def = workshops().stream()
                    .filter(w -> w.key().equals(ws.get("key")))
                    .findFirst()
                    .orElse(null);
            if (def != null && !def.equipmentTypes().isEmpty()) {
                String equipType = normalizeType(def.equipmentTypes().get(0));
                StepCapacity cap = capByType.get(equipType);
                if (cap != null) {
                    ws.put("equipmentType", def.equipmentTypes().get(0));
                    ws.put("availableMachines", cap.availableMachines());
                    ws.put("dailyCapacity", round1(cap.availableMachines() * cap.dailyPerMachine()));
                    ws.put("operatorsPerMachine", cap.operatorsPerMachine());
                }
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> machines = (List<Map<String, Object>>) ws.getOrDefault("machines", List.of());
            Map<String, List<Map<String, Object>>> lineGroups = new LinkedHashMap<>();
            for (Map<String, Object> machine : machines) {
                String lineCode = String.valueOf(machine.getOrDefault("lineCode", resolveLineCode((String) machine.get("workstation"))));
                lineGroups.computeIfAbsent(lineCode, k -> new ArrayList<>()).add(machine);
            }

            List<Map<String, Object>> lines = new ArrayList<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : lineGroups.entrySet()) {
                List<Map<String, Object>> lineMachines = entry.getValue();
                long running = lineMachines.stream().filter(m -> "RUNNING".equals(m.get("status"))).count();
                long fault = lineMachines.stream().filter(m -> "FAULT".equals(m.get("status"))).count();
                Map<String, Object> line = new LinkedHashMap<>();
                line.put("lineCode", entry.getKey());
                line.put("lineName", entry.getKey());
                line.put("equipmentCount", lineMachines.size());
                line.put("running", (int) running);
                line.put("fault", (int) fault);
                line.put("status", fault > 0 ? "abnormal" : running > 0 ? "running" : "idle");
                line.put("equipment", lineMachines);
                lines.add(line);
            }
            ws.put("lines", lines);

            totalEquipment += intVal(ws.get("total"));
            totalRunning += intVal(ws.get("running"));
            totalFault += intVal(ws.get("fault"));
            totalIdle += intVal(ws.get("idle"));
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("workshopCount", workshops.size());
        summary.put("productionStageCount", ProductionWorkshopCatalog.PRODUCTION_STAGES.size());
        summary.put("equipmentTotal", totalEquipment);
        summary.put("running", totalRunning);
        summary.put("fault", totalFault);
        summary.put("idle", totalIdle);
        summary.put("availableOperators", operators.size());
        summary.put("activeWorkshops", workshops.stream().filter(w -> Boolean.TRUE.equals(w.get("isRunning"))).count());
        summary.put("stepSeconds", LIVE_STEP_SECONDS);
        summary.put("batchTargetQty", LIVE_BATCH_TARGET);
        summary.put("bottleneckHint", buildBottleneckHint(capByType, operators.size()));

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("dataSource", "planner-agent-live");
        overview.put("refreshTime", java.time.LocalDateTime.now().toString().replace('T', ' '));
        overview.put("stepSeconds", LIVE_STEP_SECONDS);
        overview.put("batchTargetQty", LIVE_BATCH_TARGET);
        overview.put("summary", summary);
        overview.put("workshops", workshops);
        return overview;
    }

    private String buildBottleneckHint(Map<String, StepCapacity> capByType, int operatorCount) {
        if (operatorCount <= 0) {
            return "未配置可用操作员";
        }
        Optional<StepCapacity> bottleneck = capByType.values().stream()
                .min(Comparator.comparingInt(StepCapacity::maxByEquipment));
        if (bottleneck.isEmpty()) {
            return "产能数据正常";
        }
        StepCapacity cap = bottleneck.get();
        return String.format("当前瓶颈工序「%s」（%s），可用 %d 台，日产能约 %.1f 台",
                cap.step().getStepName(),
                cap.step().getStandardEquipmentType(),
                cap.availableMachines(),
                cap.availableMachines() * cap.dailyPerMachine());
    }

    private String resolveLineCode(String workstation) {
        if (workstation == null || workstation.isBlank()) {
            return "默认产线";
        }
        if (workstation.contains("A线")) {
            return "A线";
        }
        if (workstation.contains("B线")) {
            return "B线";
        }
        if (workstation.contains("E线")) {
            return "E线";
        }
        int dash = workstation.indexOf('-');
        if (dash > 0) {
            return workstation.substring(0, dash).trim();
        }
        return workstation.trim();
    }

    private Map<String, Object> resolveWorkshopProgress(ProductionWorkshopCatalog.WorkshopDef ws, List<DispatchTask> dispatches,
                                                        Map<Long, WorkOrder> woById,
                                                        Map<Long, ProcessStep> stepById,
                                                        Map<Long, Equipment> equipmentById,
                                                        MesRuntimeState runtime,
                                                        List<Equipment> equipment) {
        BigDecimal planned = BigDecimal.ZERO;
        BigDecimal completed = BigDecimal.ZERO;
        BigDecimal activePlanned = BigDecimal.ZERO;
        BigDecimal activeCompleted = BigDecimal.ZERO;
        String workOrderNo = "";
        String currentStep = "";
        boolean isRunning = false;
        boolean assignedOnly = false;

        for (DispatchTask d : dispatches) {
            if (!dispatchBelongsToWorkshop(d, ws, stepById, equipmentById, runtime)) {
                continue;
            }
            BigDecimal assigned = d.getAssignedQuantity() != null ? d.getAssignedQuantity() : BigDecimal.ZERO;
            BigDecimal done = d.getCompletedQuantity() != null ? d.getCompletedQuantity() : BigDecimal.ZERO;
            planned = planned.add(assigned);
            completed = completed.add(done);

            WorkOrder wo = woById.get(d.getWorkOrderId());
            boolean activeWorkOrder = wo != null
                    && List.of("RUNNING", "RELEASED", "DISPATCHED", "PRODUCING").contains(wo.getStatus());
            boolean activeDispatch = activeWorkOrder
                    && List.of("ASSIGNED", "ACCEPTED", "RUNNING").contains(d.getStatus());
            if (activeDispatch) {
                isRunning = true;
                if ("ASSIGNED".equals(d.getStatus()) && done.compareTo(BigDecimal.ZERO) == 0) {
                    assignedOnly = true;
                }
                BigDecimal target = assigned.compareTo(BigDecimal.ZERO) > 0
                        ? assigned.min(BigDecimal.valueOf(LIVE_BATCH_TARGET))
                        : BigDecimal.valueOf(LIVE_BATCH_TARGET);
                activePlanned = activePlanned.add(target);
                activeCompleted = activeCompleted.add(done.min(target));
                if (currentStep.isBlank()) {
                    currentStep = resolveDispatchStepName(d, stepById, runtime);
                }
            }
            if (workOrderNo.isBlank()) {
                if (wo != null) {
                    workOrderNo = wo.getWorkOrderNo();
                }
            }
        }

        int progress = 0;
        int plannedInt = 0;
        int completedInt = 0;
        int batchProgress = 0;
        int batchCompleted = 0;
        int batchTarget = LIVE_BATCH_TARGET;
        String progressLabel;

        if (isRunning && assignedOnly && completedInt == 0) {
            progressLabel = workOrderNo.isBlank()
                    ? "已派工，待操作员接收"
                    : String.format("工单 %s 已派工，待操作员接收", workOrderNo);
        } else if (isRunning && activePlanned.compareTo(BigDecimal.ZERO) > 0) {
            plannedInt = activePlanned.intValue();
            completedInt = activeCompleted.intValue();
            progress = Math.min(100, (int) Math.round(activeCompleted.doubleValue() * 100.0 / activePlanned.doubleValue()));
            batchCompleted = Math.min(LIVE_BATCH_TARGET, completedInt);
            batchProgress = Math.min(100, (int) Math.round(batchCompleted * 100.0 / LIVE_BATCH_TARGET));
            progressLabel = String.format("实时生产 %d / %d 台（%d%%）", completedInt, plannedInt, progress);
        } else if (planned.compareTo(BigDecimal.ZERO) > 0) {
            plannedInt = 0;
            completedInt = 0;
            progress = 0;
            batchCompleted = 0;
            batchProgress = 0;
            progressLabel = "等待工单下发";
        } else {
            progressLabel = "暂无工单下发";
        }

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("progress", progress);
        info.put("completedQty", completedInt);
        info.put("plannedQty", plannedInt);
        info.put("batchCompletedQty", batchCompleted);
        info.put("batchTargetQty", batchTarget);
        info.put("batchProgress", batchProgress);
        info.put("stepSeconds", LIVE_STEP_SECONDS);
        info.put("isRunning", isRunning);
        info.put("currentStep", currentStep);
        info.put("workOrderNo", workOrderNo);
        info.put("progressLabel", progressLabel);
        return info;
    }

    private boolean dispatchBelongsToWorkshop(DispatchTask d, ProductionWorkshopCatalog.WorkshopDef ws,
                                              Map<Long, ProcessStep> stepById,
                                              Map<Long, Equipment> equipmentById,
                                              MesRuntimeState runtime) {
        ProcessStep step = stepById.get(d.getStepId());
        Equipment eq = d.getEquipmentId() != null ? equipmentById.get(d.getEquipmentId()) : null;
        if (step == null) {
            String stepName = resolveDispatchStepName(d, stepById, runtime);
            if (!stepName.isBlank()) {
                step = new ProcessStep();
                step.setStepName(stepName);
            }
        }
        return ProductionWorkshopCatalog.dispatchBelongsToWorkshop(d, ws, step, eq);
    }

    private List<Equipment> equipmentForWorkshop(ProductionWorkshopCatalog.WorkshopDef ws, List<Equipment> all) {
        List<Equipment> matched = availableEquipment(all).stream()
                .filter(e -> ProductionWorkshopCatalog.equipmentBelongsToWorkshop(e, ws))
                .toList();
        if (!matched.isEmpty()) {
            return matched;
        }
        return availableEquipment(all).stream()
                .filter(e -> ws.equipmentTypes().contains(normalizeType(e.getEquipmentType())))
                .toList();
    }

    private int countAvailableEquipment(List<Equipment> all, ProductionWorkshopCatalog.WorkshopDef ws) {
        int count = equipmentForWorkshop(ws, all).size();
        return Math.max(1, count);
    }

    private String resolveDispatchStepName(DispatchTask d, Map<Long, ProcessStep> stepById,
                                           MesRuntimeState runtime) {
        Map<String, Object> extra = runtime.getExtras().getOrDefault("dispatch:" + d.getDispatchNo(), Map.of());
        Object fromExtra = extra.get("processStep");
        if (fromExtra != null) {
            String text = String.valueOf(fromExtra).trim();
            if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) {
                return text;
            }
        }
        ProcessStep step = stepById.get(d.getStepId());
        return step != null && step.getStepName() != null ? step.getStepName() : "";
    }

    private CustomerOrder findOrderByNo(String orderNo) {
        return customerOrderMapper.customerOrderList().stream()
                .filter(o -> orderNo.equals(o.getOrderNo()))
                .findFirst().orElse(null);
    }

    private CustomerOrderItem firstOrderItem(Long orderId) {
        return customerOrderItemMapper.orderItemList().stream()
                .filter(i -> orderId.equals(i.getOrderId()))
                .findFirst()
                .orElse(null);
    }

    private List<ProcessStep> resolveRouteSteps(CustomerOrderItem item) {
        List<ProcessStep> all = processStepMapper.stepList();
        if (all.isEmpty()) {
            return defaultSteps();
        }
        Long routeId = item != null && item.getMaterialId() != null ? resolveRouteId(item.getMaterialId()) : 1L;
        List<ProcessStep> routeSteps = all.stream()
                .filter(s -> routeId.equals(s.getRouteId()))
                .filter(ProductionWorkshopCatalog::isProductionStep)
                .sorted(Comparator.comparing(ProcessStep::getStepNo, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        if (!routeSteps.isEmpty()) {
            return routeSteps;
        }
        List<ProcessStep> productionOnly = all.stream()
                .filter(ProductionWorkshopCatalog::isProductionStep)
                .sorted(Comparator.comparing(ProcessStep::getStepNo, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        return productionOnly.isEmpty() ? defaultSteps() : productionOnly;
    }

    private Long resolveRouteId(Long materialId) {
        if (materialId == null) {
            return processRouteMapper.routeList().stream()
                    .map(ProcessRoute::getRouteId)
                    .findFirst()
                    .orElse(1L);
        }
        return processRouteMapper.routeList().stream()
                .filter(r -> materialId.equals(r.getMaterialId()))
                .map(ProcessRoute::getRouteId)
                .findFirst()
                .orElse(processRouteMapper.routeList().stream()
                        .map(ProcessRoute::getRouteId)
                        .findFirst()
                        .orElse(1L));
    }

    private List<ProcessStep> defaultStepsFrom(List<ProcessStep> all) {
        return all.stream()
                .filter(s -> Long.valueOf(1L).equals(s.getRouteId()))
                .sorted(Comparator.comparing(ProcessStep::getStepNo, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private List<ProcessStep> defaultSteps() {
        List<ProcessStep> steps = new ArrayList<>();
        steps.add(step("显示屏加工", "显示屏线", 0.4));
        steps.add(step("主板装配", "主板线", 0.5));
        steps.add(step("面板贴附", "贴附机", 0.5));
        steps.add(step("整机组装", "组装线", 0.8));
        return steps;
    }

    private ProcessStep step(String name, String equipType, double hours) {
        ProcessStep s = new ProcessStep();
        s.setStepName(name);
        s.setStandardEquipmentType(equipType);
        s.setStandardWorkHours(BigDecimal.valueOf(hours));
        return s;
    }

    private boolean matchesWorkshop(ProcessStep step, ProductionWorkshopCatalog.WorkshopDef ws) {
        return ProductionWorkshopCatalog.matchesWorkshop(step, ws);
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim();
    }

    private String statusLabel(String status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case "RUNNING" -> "运行中";
            case "IDLE" -> "待机";
            case "FAULT" -> "故障";
            case "MAINTENANCE" -> "保养中";
            default -> status;
        };
    }

    private double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private record WorkshopSlot(
            ProductionWorkshopCatalog.WorkshopDef ws,
            List<Equipment> equipment,
            int available,
            int idle
    ) {
    }

    private record StepCapacity(
            ProcessStep step,
            String workshopLabel,
            int availableMachines,
            int operatorsPerMachine,
            double dailyPerMachine,
            int maxByEquipment
    ) {
    }

    private record AllocationResult(
            boolean feasible,
            int requiredOperators,
            List<Map<String, Object>> allocations
    ) {
    }

    private record CapacityPlan(
            int materialLimit,
            int feasibleQty,
            int equipmentLimit,
            int operatorLimit,
            int availableOperators,
            List<String> limits,
            List<Map<String, Object>> allocations
    ) {
        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("materialLimit", materialLimit);
            m.put("feasibleQty", feasibleQty);
            m.put("equipmentLimit", equipmentLimit);
            m.put("operatorLimit", operatorLimit);
            m.put("availableOperators", availableOperators);
            m.put("limits", limits);
            m.put("allocations", allocations);
            return m;
        }
    }
}
