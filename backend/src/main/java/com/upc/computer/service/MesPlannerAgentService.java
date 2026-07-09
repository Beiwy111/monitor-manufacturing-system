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

    private static final double SHIFT_HOURS = 8.0;
    private static final int LIVE_STEP_SECONDS = 5;
    private static final int LIVE_BATCH_TARGET = 20;

    private static final List<WorkshopDef> WORKSHOPS = List.of(
            new WorkshopDef("attach", "贴附车间", "生产一部", List.of("贴附机"), List.of("面板贴附"),
                    1, "面板贴附", "液晶面板与背光模组高精度贴附，完成显示器前段贴合"),
            new WorkshopDef("assembly", "组装车间", "生产一部", List.of("组装线"), List.of("背光组装"),
                    2, "背光组装", "背光模组、边框与主控板组装，形成显示器半成品"),
            new WorkshopDef("aging", "老化测试车间", "生产二部", List.of("老化架"), List.of("整机老化测试", "老化测试"),
                    1, "整机老化测试", "通电老化与亮度均匀性测试，筛除早期失效品"),
            new WorkshopDef("tuning", "调校质检车间", "生产二部", List.of("调校台"), List.of("电竞调校", "亮度检测"),
                    1, "电竞调校质检", "刷新率、色域、亮度等参数调校与过程质检"),
            new WorkshopDef("packing", "包装发货车间", "生产二部", List.of("包装线"), List.of("外观检验包装", "包装"),
                    2, "外观检验包装", "终检、附件装配、装箱贴标，等待成品入库发货")
    );

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
        CapacityPlan capacityPlan = buildCapacityPlan(steps, allEquipment, operatorPool, materialRecommendedQty, workDays);
        int recommendedPlanQty = capacityPlan.feasibleQty();
        if (materialRecommendedQty > 0) {
            applyCapacityDecision(inventoryCheck, materialRecommendedQty, recommendedPlanQty, capacityPlan);
        }
        quantity = recommendedPlanQty > 0 ? recommendedPlanQty : (materialRecommendedQty > 0 ? materialRecommendedQty : orderQuantity);
        double dailyTarget = quantity / (double) workDays;

        Map<String, List<Equipment>> equipByType = availableEquipment(allEquipment).stream()
                .collect(Collectors.groupingBy(e -> normalizeType(e.getEquipmentType())));

        List<Map<String, Object>> workshopPlans = new ArrayList<>();
        List<Map<String, Object>> dispatchSuggestions = new ArrayList<>();
        int totalMachines = 0;
        int totalOperators = 0;

        for (WorkshopDef ws : WORKSHOPS) {
            List<ProcessStep> matchedSteps = steps.stream()
                    .filter(s -> matchesWorkshop(s, ws))
                    .toList();
            if (matchedSteps.isEmpty()) {
                continue;
            }

            int machinesNeeded = 0;
            int operatorsNeeded = 0;
            List<String> stepNames = new ArrayList<>();
            List<Map<String, Object>> machines = new ArrayList<>();

            for (ProcessStep step : matchedSteps) {
                stepNames.add(step.getStepName());
                String equipType = normalizeType(step.getStandardEquipmentType());
                double hours = step.getStandardWorkHours() != null
                        ? step.getStandardWorkHours().doubleValue() : 1.0;
                if (hours <= 0) {
                    hours = 1.0;
                }
                double dailyPerMachine = SHIFT_HOURS / hours;
                int need = (int) Math.ceil(dailyTarget / Math.max(1.0, dailyPerMachine));
                need = Math.max(1, need);

                List<Equipment> pool = equipByType.getOrDefault(equipType, List.of());
                if (pool.isEmpty()) {
                    pool = availableEquipment(allEquipment).stream()
                            .filter(e -> ws.equipmentTypes.contains(normalizeType(e.getEquipmentType())))
                            .toList();
                }
                int available = Math.max(1, pool.size());
                need = Math.min(need, available);

                machinesNeeded = Math.max(machinesNeeded, need);
                operatorsNeeded += need * ws.operatorsPerMachine;

                Equipment primary = pool.isEmpty() ? null : pool.get(0);
                Map<String, Object> suggestion = new LinkedHashMap<>();
                suggestion.put("processStep", step.getStepName());
                suggestion.put("workshop", ws.workshopName);
                suggestion.put("department", ws.department);
                suggestion.put("equipment", primary != null ? primary.getEquipmentName() : ws.workshopName + "设备");
                suggestion.put("equipmentCode", primary != null ? primary.getEquipmentCode() : "");
                suggestion.put("planQty", recommendedPlanQty > 0 ? recommendedPlanQty : quantity);
                suggestion.put("requiredMachines", need);
                suggestion.put("requiredOperators", need * ws.operatorsPerMachine);
                suggestion.put("operatorRole", "operator");
                dispatchSuggestions.add(suggestion);

                for (int i = 0; i < need && i < pool.size(); i++) {
                    Equipment eq = pool.get(i);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("code", eq.getEquipmentCode());
                    m.put("name", eq.getEquipmentName());
                    m.put("status", eq.getStatus());
                    m.put("statusLabel", statusLabel(eq.getStatus()));
                    machines.add(m);
                }
            }

            int availableCount = countAvailableEquipment(allEquipment, ws);
            int utilization = availableCount > 0
                    ? (int) Math.min(100, Math.round(machinesNeeded * 100.0 / availableCount))
                    : 100;

            Map<String, Object> wsRow = new LinkedHashMap<>();
            wsRow.put("key", ws.key);
            wsRow.put("workshopName", ws.workshopName);
            wsRow.put("department", ws.department);
            wsRow.put("steps", stepNames);
            wsRow.put("requiredMachines", machinesNeeded);
            wsRow.put("requiredOperators", operatorsNeeded);
            wsRow.put("availableMachines", availableCount);
            wsRow.put("availableOperators", operatorPool.size());
            wsRow.put("utilization", utilization);
            wsRow.put("machines", machines);
            wsRow.put("status", utilization >= 90 ? "warning" : machinesNeeded > 0 ? "running" : "pending");
            workshopPlans.add(wsRow);

            totalMachines += machinesNeeded;
            totalOperators += operatorsNeeded;
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
        result.put("dispatchSuggestions", dispatchSuggestions);
        result.put("decision", inventoryCheck.get("decision"));
        result.put("recommendation", inventoryCheck.get("recommendation"));
        result.put("capacityAnalysis", capacityPlan.toMap());
        result.put("planExplanation", buildPlanExplanation(orderNo, inventoryCheck, capacityPlan,
                workDays, dailyTarget, totalMachines, totalOperators));
        result.put("summary", buildSummary(orderNo, orderQuantity, recommendedPlanQty, shipFromStock,
                inventoryCheck, workDays, dailyTarget, totalMachines, totalOperators, workshopPlans.size()));
        return result;
    }

    private CapacityPlan buildCapacityPlan(List<ProcessStep> steps, List<Equipment> allEquipment,
                                           List<User> operatorPool, int targetQty, long workDays) {
        if (targetQty <= 0) {
            return new CapacityPlan(0, 0, 0, 0, operatorPool.size(), List.of(), List.of());
        }

        List<StepCapacity> capacities = steps.stream()
                .map(step -> buildStepCapacity(step, allEquipment, workDays))
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

    private StepCapacity buildStepCapacity(ProcessStep step, List<Equipment> allEquipment, long workDays) {
        String equipType = normalizeType(step.getStandardEquipmentType());
        List<Equipment> pool = availableEquipment(allEquipment).stream()
                .filter(e -> equipType.equals(normalizeType(e.getEquipmentType())))
                .toList();
        int availableMachines = pool.size();
        double hours = step.getStandardWorkHours() != null ? step.getStandardWorkHours().doubleValue() : 1.0;
        if (hours <= 0) {
            hours = 1.0;
        }
        double dailyPerMachine = SHIFT_HOURS / hours;
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
        if (name.contains("组装") || name.contains("包装")) {
            return 2;
        }
        return 1;
    }

    private AllocationResult allocateForQuantity(List<StepCapacity> capacities, int operatorCount,
                                                 int qty, long workDays) {
        List<Map<String, Object>> allocations = new ArrayList<>();
        int requiredOperators = 0;
        for (StepCapacity cap : capacities) {
            int machines = (int) Math.ceil((qty / (double) workDays) / Math.max(1.0, cap.dailyPerMachine()));
            machines = Math.max(1, machines);
            if (machines > cap.availableMachines()) {
                return new AllocationResult(false, requiredOperators, allocations);
            }
            int stepOperators = machines * cap.operatorsPerMachine();
            requiredOperators += stepOperators;
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
        return new AllocationResult(requiredOperators <= operatorCount, requiredOperators, allocations);
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

        for (WorkshopDef ws : WORKSHOPS) {
            List<Equipment> inWorkshop = allEquipment.stream()
                    .filter(e -> ws.equipmentTypes.contains(normalizeType(e.getEquipmentType()))
                            || (e.getWorkshop() != null && e.getWorkshop().contains(ws.department.replace("生产", ""))))
                    .toList();
            if (inWorkshop.isEmpty()) {
                inWorkshop = allEquipment.stream()
                        .filter(e -> ws.equipmentTypes.contains(normalizeType(e.getEquipmentType())))
                        .toList();
            }

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
            row.put("key", ws.key);
            row.put("name", ws.workshopName);
            row.put("department", ws.department);
            row.put("taskTitle", ws.taskTitle);
            row.put("taskDescription", ws.taskDescription);
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
        for (ProcessStep step : steps) {
            String type = normalizeType(step.getStandardEquipmentType());
            capByType.putIfAbsent(type, buildStepCapacity(step, allEquipment, 1));
        }

        int totalEquipment = 0;
        int totalRunning = 0;
        int totalFault = 0;
        int totalIdle = 0;

        for (Map<String, Object> ws : workshops) {
            WorkshopDef def = WORKSHOPS.stream()
                    .filter(w -> w.key.equals(ws.get("key")))
                    .findFirst()
                    .orElse(null);
            if (def != null && !def.equipmentTypes.isEmpty()) {
                String equipType = normalizeType(def.equipmentTypes.get(0));
                StepCapacity cap = capByType.get(equipType);
                if (cap != null) {
                    ws.put("equipmentType", def.equipmentTypes.get(0));
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

    private Map<String, Object> resolveWorkshopProgress(WorkshopDef ws, List<DispatchTask> dispatches,
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

        for (DispatchTask d : dispatches) {
            if (!dispatchBelongsToWorkshop(d, ws, stepById, equipmentById, runtime)) {
                continue;
            }
            BigDecimal assigned = d.getAssignedQuantity() != null ? d.getAssignedQuantity() : BigDecimal.ZERO;
            BigDecimal done = d.getCompletedQuantity() != null ? d.getCompletedQuantity() : BigDecimal.ZERO;
            planned = planned.add(assigned);
            completed = completed.add(done);

            WorkOrder wo = woById.get(d.getWorkOrderId());
            boolean activeWorkOrder = wo != null && List.of("RUNNING", "RELEASED").contains(wo.getStatus());
            boolean activeDispatch = activeWorkOrder && List.of("ASSIGNED", "ACCEPTED", "RUNNING").contains(d.getStatus());
            if (activeDispatch) {
                isRunning = true;
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

        if (isRunning && activePlanned.compareTo(BigDecimal.ZERO) > 0) {
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

    private boolean dispatchBelongsToWorkshop(DispatchTask d, WorkshopDef ws,
                                              Map<Long, ProcessStep> stepById,
                                              Map<Long, Equipment> equipmentById,
                                              MesRuntimeState runtime) {
        String stepName = resolveDispatchStepName(d, stepById, runtime);
        if (!stepName.isBlank()) {
            return ws.stepKeywords.stream().anyMatch(stepName::contains);
        }
        if (d.getEquipmentId() == null) {
            return false;
        }
        Equipment eq = equipmentById.get(d.getEquipmentId());
        if (eq == null) {
            return false;
        }
        return ws.equipmentTypes.contains(normalizeType(eq.getEquipmentType()));
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
                .sorted(Comparator.comparing(ProcessStep::getStepNo, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        return routeSteps.isEmpty() ? defaultStepsFrom(all) : routeSteps;
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
        steps.add(step("面板贴附", "贴附机", 0.5));
        steps.add(step("背光组装", "组装线", 0.8));
        steps.add(step("整机老化测试", "老化架", 4.0));
        steps.add(step("外观检验包装", "包装线", 0.3));
        return steps;
    }

    private ProcessStep step(String name, String equipType, double hours) {
        ProcessStep s = new ProcessStep();
        s.setStepName(name);
        s.setStandardEquipmentType(equipType);
        s.setStandardWorkHours(BigDecimal.valueOf(hours));
        return s;
    }

    private boolean matchesWorkshop(ProcessStep step, WorkshopDef ws) {
        String equipType = normalizeType(step.getStandardEquipmentType());
        if (ws.equipmentTypes.contains(equipType)) {
            return true;
        }
        String name = step.getStepName() != null ? step.getStepName() : "";
        return ws.stepKeywords.stream().anyMatch(name::contains);
    }

    private int countAvailableEquipment(List<Equipment> all, WorkshopDef ws) {
        int count = (int) availableEquipment(all).stream()
                .filter(e -> ws.equipmentTypes.contains(normalizeType(e.getEquipmentType())))
                .count();
        return Math.max(1, count);
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

    private record WorkshopDef(
            String key,
            String workshopName,
            String department,
            List<String> equipmentTypes,
            List<String> stepKeywords,
            int operatorsPerMachine,
            String taskTitle,
            String taskDescription
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
