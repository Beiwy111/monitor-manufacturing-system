package com.upc.computer.controller;

import com.upc.computer.common.Result;
import com.upc.computer.dto.MesActionRequest;
import com.upc.computer.service.MesDispatchRecommendService;
import com.upc.computer.service.MesPlannerSchedulingService;
import com.upc.computer.service.MesSnapshotService;
import com.upc.computer.service.MesWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
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

    @Autowired
    private MesPlannerSchedulingService plannerSchedulingService;
    @Autowired
    private MesDispatchRecommendService dispatchRecommendService;

    @GetMapping("/snapshot")
    public Result<Map<String, Object>> snapshot() {
        return Result.success(mesSnapshotService.buildSnapshot());
    }

    /** 操作员业务通知（派工等），按登录账号过滤 */
    @GetMapping("/notifications/inbox")
    public Result<List<Map<String, Object>>> operatorNotifications(@RequestParam String username) {
        return Result.success(mesWorkflowService.listOperatorNotices(username));
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

    @PostMapping("/manager/smart-dispatch/validate")
    public Result<Object> validateSmartDispatch(@RequestBody Map<String, Object> body) {
        String planId = body != null ? String.valueOf(body.get("planId")) : "";
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = body != null && body.get("recommendations") instanceof List<?> list
                ? (List<Map<String, Object>>) list : List.of();
        return Result.success(dispatchRecommendService.validateRecommendations(planId, rows));
    }

    @GetMapping("/manager/plan/{planNo}/context")
    public Result<Object> managerPlanContext(@PathVariable String planNo) {
        return Result.success(dispatchRecommendService.loadPlanContext(planNo));
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

    /** 订单排产上下文预览 */
    @GetMapping("/planner/order/{orderId}/context")
    public Result<Object> previewOrderPlanning(@PathVariable String orderId) {
        return Result.success(plannerSchedulingService.previewOrderContext(orderId));
    }

    /** 三方案智能排产对比 */
    @PostMapping("/planner/schemes/compare")
    public Result<Object> comparePlanSchemes(@RequestBody Map<String, Object> body) {
        return Result.success(plannerSchedulingService.compareSchemes(
                String.valueOf(body.get("orderId")),
                parsePlanDate(body.get("planStart")),
                parsePlanDate(body.get("planEnd")),
                body.get("plannedQty") instanceof Number n ? n.intValue() : 0));
    }

    private LocalDate parsePlanDate(Object raw) {
        if (raw == null) return null;
        String s = String.valueOf(raw);
        if (s.isBlank()) return null;
        return LocalDate.parse(s.substring(0, Math.min(10, s.length())));
    }

    /** 订单附件 OCR 识别（当前为模拟实现） */
    @PostMapping("/order/ocr/recognize")
    public Result<Object> recognizeOrderOcr(@RequestBody Map<String, Object> body) {
        MesActionRequest req = new MesActionRequest();
        req.setAction("recognizeOrderOcr");
        req.setPayload(body != null ? body : Map.of());
        req.setOperator(String.valueOf(body != null ? body.getOrDefault("operator", "system") : "system"));
        req.setRoleKey(String.valueOf(body != null ? body.getOrDefault("roleKey", "order") : "order"));
        return Result.success(mesWorkflowService.execute(req));
    }
}
