package com.upc.computer.controller;

import com.upc.computer.common.Result;
import com.upc.computer.dto.MesActionRequest;
import com.upc.computer.service.MesSnapshotService;
import com.upc.computer.service.MesWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * MES 实时数据接口
 */
@RestController
@RequestMapping("/mes")
public class MesController {

    @Autowired
    private MesSnapshotService mesSnapshotService;

    @Autowired
    private MesWorkflowService mesWorkflowService;

    @GetMapping("/snapshot")
    public Result<Map<String, Object>> snapshot() {
        return Result.success(mesSnapshotService.buildSnapshot());
    }

    @PostMapping("/action")
    public Result<Object> action(@RequestBody MesActionRequest req) {
        return Result.success(mesWorkflowService.execute(req));
    }

    /** 智能生成生产计划 - 批量预览（优先级评分） */
    @GetMapping("/planner/smart-plans/preview")
    public Result<Object> previewSmartPlans() {
        MesActionRequest req = new MesActionRequest();
        req.setAction("previewSmartPlans");
        req.setPayload(Map.of());
        req.setOperator("system");
        req.setRoleKey("planner");
        return Result.success(mesWorkflowService.execute(req));
    }

    /** 智能生成生产计划 - 批量创建 */
    @PostMapping("/planner/smart-plans/generate")
    public Result<Object> generateSmartPlans(@RequestBody Map<String, Object> body) {
        MesActionRequest req = new MesActionRequest();
        req.setAction("generateSmartPlans");
        req.setPayload(body != null ? body : Map.of());
        req.setOperator(String.valueOf(body != null ? body.getOrDefault("operator", "system") : "system"));
        req.setRoleKey(String.valueOf(body != null ? body.getOrDefault("roleKey", "planner") : "planner"));
        return Result.success(mesWorkflowService.execute(req));
    }

    /** 智能派工推荐 - 预览 */
    @GetMapping("/manager/smart-dispatch/preview")
    public Result<Object> previewSmartDispatch(@RequestParam(required = false) String planId) {
        MesActionRequest req = new MesActionRequest();
        req.setAction("previewSmartDispatch");
        req.setPayload(planId != null && !planId.isBlank() ? Map.of("planId", planId) : Map.of());
        req.setOperator("system");
        req.setRoleKey("manager");
        return Result.success(mesWorkflowService.execute(req));
    }

    /** 智能派工推荐 - 确认派工并生成工单 */
    @PostMapping("/manager/smart-dispatch/confirm")
    public Result<Object> confirmSmartDispatch(@RequestBody Map<String, Object> body) {
        MesActionRequest req = new MesActionRequest();
        req.setAction("confirmSmartDispatch");
        req.setPayload(body != null ? body : Map.of());
        req.setOperator(String.valueOf(body != null ? body.getOrDefault("operator", "system") : "system"));
        req.setRoleKey(String.valueOf(body != null ? body.getOrDefault("roleKey", "manager") : "manager"));
        return Result.success(mesWorkflowService.execute(req));
    }

    /** 质检报告 - 刷新/重新生成（调用千问 AI） */
    @PostMapping("/quality/reports/refresh")
    public Result<Object> refreshQualityReport(@RequestBody Map<String, Object> body) {
        MesActionRequest req = new MesActionRequest();
        req.setAction("generateQualityReport");
        req.setPayload(body != null ? body : Map.of());
        req.setOperator(String.valueOf(body != null ? body.getOrDefault("operator", "system") : "system"));
        req.setRoleKey(String.valueOf(body != null ? body.getOrDefault("roleKey", "quality") : "quality"));
        return Result.success(mesWorkflowService.execute(req));
    }
}
