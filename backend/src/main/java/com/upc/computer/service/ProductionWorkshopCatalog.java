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
 * 生产结构：4 道工序，每道工序 2~3 个并行车间。
 * 工序顺序：显示屏加工 → 主板装配 → 贴附 → 组装（不含质检/老化/包装）。
 */
public final class ProductionWorkshopCatalog {

    private ProductionWorkshopCatalog() {
    }

    public static final List<ProcessStageDef> PRODUCTION_STAGES = List.of(
            stage("display", "显示屏加工", 1, "显示屏线",
                    List.of("显示屏加工", "显示屏"),
                    1, "显示屏加工", "液晶面板检测、点亮与显示功能前加工",
                    List.of(
                            ws("display-1", "显示屏加工一车间", "display", "生产一部"),
                            ws("display-2", "显示屏加工二车间", "display", "生产一部"),
                            ws("display-3", "显示屏加工三车间", "display", "生产一部")
                    )),
            stage("motherboard", "主板装配", 2, "主板线",
                    List.of("主板装配", "主板"),
                    1, "主板装配", "主控板、电源板装配与功能预检",
                    List.of(
                            ws("mb-1", "主板装配一车间", "motherboard", "生产一部"),
                            ws("mb-2", "主板装配二车间", "motherboard", "生产一部"),
                            ws("mb-3", "主板装配三车间", "motherboard", "生产一部")
                    )),
            stage("attach", "贴附", 3, "贴附机",
                    List.of("面板贴附", "贴附"),
                    1, "面板贴附", "液晶面板与背光模组高精度贴附",
                    List.of(
                            ws("attach-1", "贴附一车间", "attach", "生产一部"),
                            ws("attach-2", "贴附二车间", "attach", "生产一部")
                    )),
            stage("assembly", "组装", 4, "组装线",
                    List.of("整机组装", "背光组装", "组装"),
                    2, "整机组装", "整机组装与结构件固定，产出待检半成品",
                    List.of(
                            ws("assembly-1", "组装一车间", "assembly", "生产一部"),
                            ws("assembly-2", "组装二车间", "assembly", "生产一部"),
                            ws("assembly-3", "组装三车间", "assembly", "生产一部")
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
        if (step == null) {
            return null;
        }
        return PRODUCTION_STAGES.stream()
                .filter(stage -> matchesStage(step, stage))
                .findFirst()
                .orElse(null);
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
     * 成品完成量 = 各工序完成量之和取瓶颈（多车间并行时同工序产量累加，再取四道工序最小值）。
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
