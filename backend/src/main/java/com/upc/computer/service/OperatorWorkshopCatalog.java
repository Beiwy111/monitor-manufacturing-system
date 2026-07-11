package com.upc.computer.service;

import com.upc.computer.entity.DispatchTask;
import com.upc.computer.entity.ProcessStep;
import com.upc.computer.entity.User;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 操作员固定车间绑定：每名操作员只属于一个车间，只承担对应工序。
 */
public final class OperatorWorkshopCatalog {

    private OperatorWorkshopCatalog() {
    }

    /** 操作员正在执行中（已接收/生产中），不含仅「已分配」与「待质检」 */
    public static final List<String> IN_PROGRESS_DISPATCH_STATUS = List.of(
            "ACCEPTED", "PRODUCING", "RUNNING");

    /** 占用操作员名额的派工（含已分配未接收） */
    public static final List<String> OCCUPIED_DISPATCH_STATUS = List.of(
            "ASSIGNED", "ACCEPTED", "PRODUCING", "RUNNING");

    private static final Map<String, String> OPERATOR_WORKSHOP_KEY = Map.ofEntries(
            Map.entry("zhao_operator", "display-1"),
            Map.entry("ma_operator", "display-2"),
            Map.entry("li_operator", "mb-1"),
            Map.entry("wu_operator", "mb-2"),
            Map.entry("wang_operator", "attach-1"),
            Map.entry("zhou_operator", "attach-2"),
            Map.entry("sun_operator", "assembly-1"),
            Map.entry("chen_operator", "assembly-2"),
            Map.entry("lin_operator", "assembly-3")
    );

    /** 四道工序默认主责操作员（智能派工优先） */
    private static final Map<String, String> PRIMARY_OPERATOR_BY_STAGE = Map.of(
            "display", "zhao_operator",
            "motherboard", "li_operator",
            "attach", "wang_operator",
            "assembly", "sun_operator"
    );

    public static ProductionWorkshopCatalog.WorkshopDef workshopForOperator(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        String key = OPERATOR_WORKSHOP_KEY.get(username.trim());
        return key != null ? ProductionWorkshopCatalog.workshopByKey(key) : null;
    }

    public static ProductionWorkshopCatalog.ProcessStageDef stageForOperator(String username) {
        ProductionWorkshopCatalog.WorkshopDef ws = workshopForOperator(username);
        return ws != null ? ProductionWorkshopCatalog.stageByKey(ws.parentStepKey()) : null;
    }

    public static String primaryOperatorUsername(ProcessStep step) {
        ProductionWorkshopCatalog.ProcessStageDef stage = ProductionWorkshopCatalog.stageForStep(step);
        return stage != null ? PRIMARY_OPERATOR_BY_STAGE.get(stage.stepKey()) : null;
    }

    public static boolean isBoundOperator(String username) {
        return workshopForOperator(username) != null;
    }

    public static boolean operatorMatchesStep(String username, ProcessStep step) {
        if (step == null || username == null || username.isBlank()) {
            return false;
        }
        String processStep = step.getStepName() != null ? step.getStepName() : "";
        if (processStep.contains("返修")) {
            return true;
        }
        ProductionWorkshopCatalog.WorkshopDef ws = workshopForOperator(username);
        if (ws == null) {
            return false;
        }
        return ProductionWorkshopCatalog.matchesWorkshop(step, ws);
    }

    public static boolean operatorMatchesStepName(String username, String processStepName) {
        if (processStepName != null && processStepName.contains("返修")) {
            return true;
        }
        ProductionWorkshopCatalog.ProcessStageDef stage = stageForOperator(username);
        if (stage == null) {
            return true;
        }
        String name = processStepName != null ? processStepName : "";
        return stage.stepKeywords().stream().anyMatch(name::contains)
                || Objects.equals(stage.stepName(), processStepName);
    }

    public static void ensureOperatorWorkshopMatch(User operator, ProcessStep step) {
        if (operator == null || step == null) {
            return;
        }
        if (!isBoundOperator(operator.getUsername())) {
            throw new com.upc.computer.common.BusinessException(
                    "操作员「" + operator.getRealName() + "」未绑定固定车间，不能参与生产派工");
        }
        if (operatorMatchesStep(operator.getUsername(), step)) {
            return;
        }
        ProductionWorkshopCatalog.WorkshopDef ws = workshopForOperator(operator.getUsername());
        ProductionWorkshopCatalog.ProcessStageDef stage = ProductionWorkshopCatalog.stageForStep(step);
        String wsName = ws != null ? ws.workshopName() : "未绑定车间";
        String stepName = stage != null ? stage.stepName() : step.getStepName();
        throw new com.upc.computer.common.BusinessException(
                "操作员「" + operator.getRealName() + "」固定于「" + wsName + "」，不能承担「" + stepName + "」工序");
    }

    /**
     * 同一工单的生产派工，每道工序必须由不同操作员承担。
     */
    public static void ensureDistinctOperatorOnWorkOrder(Long workOrderId, Long operatorId,
                                                        Long excludeDispatchId,
                                                        List<DispatchTask> dispatches,
                                                        Map<Long, ProcessStep> stepById) {
        if (workOrderId == null || operatorId == null || dispatches == null) {
            return;
        }
        for (DispatchTask dispatch : dispatches) {
            if (!workOrderId.equals(dispatch.getWorkOrderId())) {
                continue;
            }
            if (!operatorId.equals(dispatch.getOperatorId())) {
                continue;
            }
            if (excludeDispatchId != null && excludeDispatchId.equals(dispatch.getDispatchId())) {
                continue;
            }
            if ("COMPLETED".equals(dispatch.getStatus()) || "CANCELLED".equals(dispatch.getStatus())) {
                continue;
            }
            ProcessStep step = stepById != null ? stepById.get(dispatch.getStepId()) : null;
            String stepName = step != null ? step.getStepName() : "未知工序";
            throw new com.upc.computer.common.BusinessException(String.format(
                    "工单已有该操作员负责的工序「%s」（派工 %s），一台显示器四道工序须派给四名不同操作员",
                    stepName, dispatch.getDispatchNo()));
        }
    }

    public static void ensureSingleActiveDispatch(Long operatorId, Long excludeDispatchId,
                                                  List<DispatchTask> dispatches,
                                                  Map<Long, ProcessStep> stepById) {
        if (operatorId == null || dispatches == null) {
            return;
        }
        for (DispatchTask dispatch : dispatches) {
            if (!operatorId.equals(dispatch.getOperatorId())) {
                continue;
            }
            if (excludeDispatchId != null && excludeDispatchId.equals(dispatch.getDispatchId())) {
                continue;
            }
            if (!OCCUPIED_DISPATCH_STATUS.contains(dispatch.getStatus())) {
                continue;
            }
            ProcessStep step = stepById != null ? stepById.get(dispatch.getStepId()) : null;
            String stepName = step != null ? step.getStepName() : "未知工序";
            String statusCn = com.upc.computer.service.MesStatusMapper.toDispatchCn(dispatch.getStatus());
            throw new com.upc.computer.common.BusinessException(String.format(
                    "操作员已有未完成的派工 %s（%s · %s），不能同时承担多道工序",
                    dispatch.getDispatchNo(), stepName, statusCn));
        }
    }

    public static void ensureSingleActiveDispatch(Long operatorId, Long excludeDispatchId,
                                                  List<DispatchTask> dispatches) {
        ensureSingleActiveDispatch(operatorId, excludeDispatchId, dispatches, Map.of());
    }
}
