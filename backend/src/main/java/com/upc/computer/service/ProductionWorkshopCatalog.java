package com.upc.computer.service;

import com.upc.computer.entity.DispatchTask;
import com.upc.computer.entity.Equipment;
import com.upc.computer.entity.ProcessStep;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 生产结构：8 道工序，每道工序 2~3 个并行车间。
 * 工序顺序：主板装配 → 电源板装配 → 接口板装配 → 显示屏加工 → 面板贴附 → 外壳装配 → 整机组装 → 支架底座装配
 */
public final class ProductionWorkshopCatalog {

    private ProductionWorkshopCatalog() {
    }

    public static final List<ProcessStageDef> PRODUCTION_STAGES = List.of(
            stage("motherboard", "主板装配", 1, "主板线",
                    List.of("主板装配", "主板"),
                    1, "主板装配", "PCB 贴装 · 主控焊接 · 电路通电测试",
                    List.of(
                            ws("mb-1", "主板装配一车间", "motherboard", "生产一部"),
                            ws("mb-2", "主板装配二车间", "motherboard", "生产一部"),
                            ws("mb-3", "主板装配三车间", "motherboard", "生产一部")
                    )),
            stage("powerboard", "电源板装配", 2, "电源板线",
                    List.of("电源板装配", "电源板"),
                    1, "电源板装配", "PCB 贴装 · 主控焊接 · 电路通电测试",
                    List.of(
                            ws("pb-1", "电源板装配一车间", "powerboard", "生产一部"),
                            ws("pb-2", "电源板装配二车间", "powerboard", "生产一部")
                    )),
            stage("interface", "接口板装配", 3, "接口板线",
                    List.of("接口板装配", "接口板"),
                    1, "接口板装配", "PCB 贴装 · 主控焊接 · 电路通电测试",
                    List.of(
                            ws("if-1", "接口板装配一车间", "interface", "生产一部"),
                            ws("if-2", "接口板装配二车间", "interface", "生产一部")
                    )),
            stage("display", "显示屏加工", 4, "显示屏线",
                    List.of("显示屏加工", "显示屏"),
                    1, "显示屏加工", "面板点亮 · 背光组装 · 显示功能初检",
                    List.of(
                            ws("display-1", "显示屏加工一车间", "display", "生产一部"),
                            ws("display-2", "显示屏加工二车间", "display", "生产一部"),
                            ws("display-3", "显示屏加工三车间", "display", "生产一部")
                    )),
            stage("attach", "面板贴附", 5, "贴附机",
                    List.of("面板贴附", "贴附"),
                    1, "面板贴附", "面板与背光贴合 · 边框压合 · 贴合精度校验",
                    List.of(
                            ws("attach-1", "贴附一车间", "attach", "生产一部"),
                            ws("attach-2", "贴附二车间", "attach", "生产一部")
                    )),
            stage("shell", "外壳装配", 6, "外壳线",
                    List.of("外壳装配", "外壳"),
                    1, "外壳装配", "面板与背光贴合 · 边框压合 · 贴合精度校验",
                    List.of(
                            ws("shell-1", "外壳装配一车间", "shell", "生产一部"),
                            ws("shell-2", "外壳装配二车间", "shell", "生产一部")
                    )),
            stage("assembly", "整机组装", 7, "组装线",
                    List.of("整机组装", "背光组装", "组装"),
                    2, "整机组装", "整机总装 · 线缆连接 · 老化前整机联调",
                    List.of(
                            ws("assembly-1", "组装一车间", "assembly", "生产一部"),
                            ws("assembly-2", "组装二车间", "assembly", "生产一部"),
                            ws("assembly-3", "组装三车间", "assembly", "生产一部")
                    )),
            stage("bracket", "支架底座装配", 8, "支架线",
                    List.of("支架底座装配", "支架", "底座"),
                    1, "支架底座装配", "整机总装 · 线缆连接 · 老化前整机联调",
                    List.of(
                            ws("bracket-1", "支架底座装配一车间", "bracket", "生产一部"),
                            ws("bracket-2", "支架底座装配二车间", "bracket", "生产一部")
                    ))
    );

    private static final List<String> NON_PRODUCTION_KEYWORDS = List.of(
            "老化", "调校", "包装", "质检", "检验", "终检", "发货", "售后"
    );

    public static List<WorkshopDef> allWorkshops() {
        List<WorkshopDef> list = new ArrayList<>();
        for (ProcessStageDef stage : PRODUCTION_STAGES) {
            list.addAll(stage.workshops());
        }
        return list;
    }

    public static ProcessStageDef stageByKey(String stepKey) {
        return PRODUCTION_STAGES.stream()
                .filter(s -> s.stepKey().equals(stepKey))
                .findFirst()
                .orElse(null);
    }

    public static WorkshopDef workshopByKey(String key) {
        return allWorkshops().stream()
                .filter(w -> w.key().equals(key))
                .findFirst()
                .orElse(null);
    }

    public static ProcessStageDef stageForStep(ProcessStep step) {
        if (step == null || isNonProductionStepName(step.getStepName())) {
            return null;
        }
        String name = step.getStepName() != null ? step.getStepName() : "";
        for (ProcessStageDef stage : PRODUCTION_STAGES) {
            if (stage.stepName().equals(name)) {
                return stage;
            }
        }
        for (ProcessStageDef stage : PRODUCTION_STAGES) {
            if (matchesStage(step, stage)) {
                return stage;
            }
        }
        return null;
    }

    /**
     * 按八道生产工序标准顺序，从工艺路线中解析生产工序（智能派工/生成工单统一入口）。
     */
    public static List<ProcessStep> resolveProductionStepsForRoute(Long routeId, List<ProcessStep> allSteps) {
        if (routeId == null || allSteps == null || allSteps.isEmpty()) {
            return List.of();
        }
        List<ProcessStep> routeSteps = allSteps.stream()
                .filter(s -> routeId.equals(s.getRouteId()))
                .filter(s -> s.getStatus() == null || s.getStatus() == 1)
                .toList();
        List<ProcessStep> ordered = new ArrayList<>();
        for (ProcessStageDef stage : PRODUCTION_STAGES) {
            ProcessStep hit = routeSteps.stream()
                    .filter(s -> matchesStage(s, stage))
                    .min(Comparator.comparing(ProcessStep::getStepNo, Comparator.nullsLast(Integer::compareTo)))
                    .orElse(null);
            if (hit != null) {
                ordered.add(hit);
            }
        }
        return ordered;
    }

    public static WorkshopDef workshopForStep(ProcessStep step) {
        ProcessStageDef stage = stageForStep(step);
        if (stage == null || stage.workshops().isEmpty()) {
            return null;
        }
        return stage.workshops().get(0);
    }

    public static boolean isProductionStep(ProcessStep step) {
        return stageForStep(step) != null;
    }

    public static boolean isFinalProductionStep(ProcessStep step) {
        ProcessStageDef stage = stageForStep(step);
        return stage != null && stage.stepOrder() == PRODUCTION_STAGES.stream()
                .mapToInt(ProcessStageDef::stepOrder)
                .max()
                .orElse(stage.stepOrder());
    }

    public static boolean isNonProductionStepName(String stepName) {
        if (stepName == null || stepName.isBlank()) {
            return true;
        }
        return NON_PRODUCTION_KEYWORDS.stream().anyMatch(stepName::contains);
    }

    public static boolean matchesStage(ProcessStep step, ProcessStageDef stage) {
        if (step == null || stage == null || isNonProductionStepName(step.getStepName())) {
            return false;
        }
        String equipType = normalize(step.getStandardEquipmentType());
        if (equipType.equals(normalize(stage.equipmentType()))) {
            return true;
        }
        String name = step.getStepName() != null ? step.getStepName() : "";
        if (stage.stepName().equals(name)) {
            return true;
        }
        return stage.stepKeywords().stream().anyMatch(name::contains);
    }

    public static boolean matchesWorkshop(ProcessStep step, WorkshopDef ws) {
        ProcessStageDef stage = stageForStep(step);
        return stage != null && stage.stepKey().equals(ws.parentStepKey());
    }

    public static boolean equipmentBelongsToWorkshop(Equipment equipment, WorkshopDef ws) {
        if (equipment == null || ws == null) {
            return false;
        }
        String workshop = equipment.getWorkshop() != null ? equipment.getWorkshop().trim() : "";
        if (!workshop.isEmpty()) {
            return workshop.equals(ws.workshopName())
                    || workshop.contains(ws.workshopName())
                    || ws.workshopName().contains(workshop);
        }
        String equipType = normalize(equipment.getEquipmentType());
        return ws.equipmentTypes().contains(equipType);
    }

    public static boolean dispatchBelongsToWorkshop(DispatchTask dispatch, WorkshopDef ws,
                                                    ProcessStep step, Equipment equipment) {
        if (ws == null) {
            return false;
        }
        if (equipment != null && equipmentBelongsToWorkshop(equipment, ws)) {
            return true;
        }
        if (step != null && matchesStage(step, stageByKey(ws.parentStepKey()))) {
            if (equipment == null) {
                return true;
            }
            return equipmentBelongsToWorkshop(equipment, ws);
        }
        return false;
    }

    /**
     * 成品完成量 = 各工序完成量之和取瓶颈（多车间并行时同工序产量累加，再取八道生产工序最小值）。
     */
    public static int finishedGoodsQty(List<DispatchTask> dispatches, Map<Long, ProcessStep> stepById) {
        if (dispatches == null || dispatches.isEmpty()) {
            return 0;
        }
        BigDecimal bottleneck = null;
        for (ProcessStageDef stage : PRODUCTION_STAGES) {
            BigDecimal stageTotal = BigDecimal.ZERO;
            for (DispatchTask d : dispatches) {
                ProcessStep step = stepById != null ? stepById.get(d.getStepId()) : null;
                if (step == null || !matchesStage(step, stage)) {
                    continue;
                }
                BigDecimal done = d.getCompletedQuantity() != null ? d.getCompletedQuantity() : BigDecimal.ZERO;
                stageTotal = stageTotal.add(done);
            }
            bottleneck = bottleneck == null ? stageTotal : bottleneck.min(stageTotal);
        }
        return bottleneck == null ? 0 : bottleneck.intValue();
    }

    private static ProcessStageDef stage(String stepKey, String stepName, int stepOrder, String equipmentType,
                                         List<String> stepKeywords, int operatorsPerMachine,
                                         String taskTitle, String taskDescription,
                                         List<WorkshopDef> workshops) {
        List<WorkshopDef> enriched = workshops.stream()
                .map(ws -> new WorkshopDef(
                        ws.key(),
                        ws.workshopName(),
                        ws.parentStepKey(),
                        ws.department(),
                        List.of(equipmentType),
                        stepKeywords,
                        operatorsPerMachine,
                        taskTitle,
                        taskDescription
                ))
                .toList();
        return new ProcessStageDef(stepKey, stepName, stepOrder, equipmentType, stepKeywords,
                operatorsPerMachine, taskTitle, taskDescription, enriched);
    }

    private static WorkshopDef ws(String key, String workshopName, String parentStepKey, String department) {
        return new WorkshopDef(key, workshopName, parentStepKey, department,
                List.of(), List.of(), 1, "", "");
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim();
    }

    public record ProcessStageDef(
            String stepKey,
            String stepName,
            int stepOrder,
            String equipmentType,
            List<String> stepKeywords,
            int operatorsPerMachine,
            String taskTitle,
            String taskDescription,
            List<WorkshopDef> workshops
    ) {
        public int workshopCount() {
            return workshops.size();
        }
    }

    public record WorkshopDef(
            String key,
            String workshopName,
            String parentStepKey,
            String department,
            List<String> equipmentTypes,
            List<String> stepKeywords,
            int operatorsPerMachine,
            String taskTitle,
            String taskDescription
    ) {
        public int stepOrder() {
            ProcessStageDef stage = stageByKey(parentStepKey);
            return stage != null ? stage.stepOrder() : 99;
        }
    }
}
