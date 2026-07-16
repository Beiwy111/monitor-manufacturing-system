package com.upc.computer.controller;

import com.upc.computer.common.BusinessException;
import com.upc.computer.common.Result;
import com.upc.computer.entity.AfterSalesCase;
import com.upc.computer.entity.CostSettlement;
import com.upc.computer.service.AfterSalesService;
import com.upc.computer.service.AfterSalesWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 售后管理接口
 *
 * GET  /afterSales/case/views              案例列表（含订单/物料/质检关联展示字段）
 * GET  /afterSales/case/trace?caseNo=      追溯详情（订单→质检→不良品→售后完整链路）
 * GET  /afterSales/kpi                     KPI 统计
 * POST /afterSales/case/accept             受理：OPEN -> PROCESSING
 * POST /afterSales/case/resolve            解决：PROCESSING -> RESOLVED
 * POST /afterSales/case/close              关闭：RESOLVED/PROCESSING -> CLOSED
 *
 * 原始 CRUD 路径保留兼容
 */
@RestController
@RequestMapping("/afterSales")
public class AfterSalesController {

    @Autowired
    private AfterSalesService afterSalesService;

    @Autowired
    private AfterSalesWorkflowService workflowService;

    // ── 视图查询 ──────────────────────────────────────────────

    @GetMapping("/case/views")
    public Result<List<Map<String, Object>>> caseViews() {
        return Result.success(afterSalesService.listCaseViews());
    }

    @GetMapping("/case/trace")
    public Result<Map<String, Object>> traceDetail(@RequestParam String caseNo) {
        if (caseNo == null || caseNo.isBlank())
            throw new BusinessException("caseNo 不能为空");
        return Result.success(afterSalesService.getTraceDetail(caseNo));
    }

    @GetMapping("/kpi")
    public Result<Map<String, Object>> kpi() {
        return Result.success(afterSalesService.caseKpi());
    }

    @GetMapping("/rca/analysis")
    public Result<Map<String, Object>> rcaAnalysis(@RequestParam String caseNo,
                                                   @RequestParam(defaultValue = "false") boolean force) {
        if (caseNo == null || caseNo.isBlank())
            throw new BusinessException("caseNo 不能为空");
        return Result.success(afterSalesService.buildRcaAnalysis(caseNo, force));
    }

    @PostMapping("/rca/dispatch")
    public Result<Map<String, Object>> dispatchRca(@RequestBody Map<String, Object> body) {
        String caseNo = str(body, "caseNo");
        if (caseNo.isBlank()) throw new BusinessException("caseNo 不能为空");
        Object raw = body.get("departments");
        List<String> departments = raw instanceof List<?> list
                ? list.stream().map(String::valueOf).toList() : List.of();
        return Result.success("协同任务派发成功", afterSalesService.dispatchRcaTasks(caseNo, departments));
    }

    @GetMapping("/rca/tasks")
    public Result<List<Map<String, Object>>> rcaTasks(@RequestParam String department) {
        return Result.success(afterSalesService.listRcaTasks(department));
    }

    @PostMapping("/rca/confirm-root-cause")
    public Result<Map<String,Object>> confirmRootCause(@RequestBody Map<String,Object> body) {
        return Result.success("最终根因已确认", afterSalesService.confirmRootCause(body));
    }

    @PostMapping("/rca/task/update")
    public Result<Map<String,Object>> updateRcaTask(@RequestBody Map<String,Object> body) {
        return Result.success("协同任务已更新", afterSalesService.updateRcaTask(body));
    }

    @GetMapping("/rca/task/progress")
    public Result<Map<String,Object>> taskProgress(@RequestParam String caseNo) {
        return Result.success(afterSalesService.rcaTaskProgress(caseNo));
    }

    @GetMapping("/triage")
    public Result<Map<String,Object>> triage(@RequestParam String caseNo,
                                             @RequestParam(defaultValue = "false") boolean force) {
        return Result.success(afterSalesService.triageCase(caseNo, force));
    }

    // ── 状态流转 ──────────────────────────────────────────────

    @PostMapping("/case/accept")
    public Result<AfterSalesCase> accept(@RequestBody Map<String, Object> body) {
        String caseNo = str(body, "caseNo");
        if (caseNo.isBlank()) throw new BusinessException("caseNo 不能为空");
        return Result.success("受理成功", afterSalesService.acceptCase(caseNo, str(body, "operator")));
    }

    @PostMapping("/case/resolve")
    public Result<AfterSalesCase> resolve(@RequestBody Map<String, Object> body) {
        String caseNo = str(body, "caseNo");
        if (caseNo.isBlank()) throw new BusinessException("caseNo 不能为空");
        return Result.success("已标记解决", afterSalesService.resolveCase(
                caseNo, str(body, "solution"), str(body, "traceResult"), str(body, "operator")));
    }

    @PostMapping("/case/close")
    public Result<AfterSalesCase> close(@RequestBody Map<String, Object> body) {
        String caseNo = str(body, "caseNo");
        if (caseNo.isBlank()) throw new BusinessException("caseNo 不能为空");
        return Result.success("案例已关闭", afterSalesService.closeCase(
                caseNo, str(body, "remark"), str(body, "operator")));
    }

    // ── 售后流程：方案 / 执行 / 闭环 ───────────────────────────

    @GetMapping("/workflow/plans")
    public Result<List<Map<String, Object>>> listPlans() {
        return Result.success(workflowService.listPlans());
    }

    @GetMapping("/workflow/plan")
    public Result<Map<String, Object>> getPlan(@RequestParam String caseNo) {
        return Result.success(workflowService.getPlanByCase(caseNo));
    }

    @PostMapping("/workflow/plan/save")
    public Result<Map<String, Object>> savePlan(@RequestBody Map<String, Object> body) {
        return Result.success("方案已保存", workflowService.savePlan(body));
    }

    @PostMapping("/workflow/plan/submit")
    public Result<Map<String, Object>> submitPlan(@RequestBody Map<String, Object> body) {
        Long planId = longVal(body.get("planId"));
        if (planId == null) throw new BusinessException("planId 不能为空");
        return Result.success("方案已提交审批", workflowService.submitPlan(planId));
    }

    @PostMapping("/workflow/plan/approve")
    public Result<Map<String, Object>> approvePlan(@RequestBody Map<String, Object> body) {
        Long planId = longVal(body.get("planId"));
        if (planId == null) throw new BusinessException("planId 不能为空");
        return Result.success("方案已通过，执行任务已生成", workflowService.approvePlan(planId, str(body, "operator")));
    }

    @PostMapping("/workflow/plan/reject")
    public Result<Map<String, Object>> rejectPlan(@RequestBody Map<String, Object> body) {
        Long planId = longVal(body.get("planId"));
        if (planId == null) throw new BusinessException("planId 不能为空");
        return Result.success("方案已驳回", workflowService.rejectPlan(planId, str(body, "remark")));
    }

    @GetMapping("/workflow/tasks")
    public Result<List<Map<String, Object>>> listWorkflowTasks(@RequestParam(required = false) String caseNo) {
        if (caseNo != null && !caseNo.isBlank()) {
            return Result.success(workflowService.listTasksByCase(caseNo));
        }
        return Result.success(workflowService.listTasks());
    }

    @PostMapping("/workflow/task/update")
    public Result<Map<String, Object>> updateWorkflowTask(@RequestBody Map<String, Object> body) {
        return Result.success("任务已更新", workflowService.updateTask(body));
    }

    @PostMapping("/workflow/case/advance")
    public Result<Map<String, Object>> advanceCase(@RequestBody Map<String, Object> body) {
        String caseNo = str(body, "caseNo");
        String targetStatus = str(body, "targetStatus");
        if (caseNo.isBlank() || targetStatus.isBlank()) throw new BusinessException("caseNo 与 targetStatus 不能为空");
        return Result.success("状态已更新", workflowService.advanceCase(caseNo, targetStatus));
    }

    @GetMapping("/workflow/closure")
    public Result<Map<String, Object>> getClosure(@RequestParam String caseNo) {
        return Result.success(workflowService.getClosure(caseNo));
    }

    @GetMapping("/workflow/closures")
    public Result<List<Map<String, Object>>> listClosures() {
        return Result.success(workflowService.listClosures());
    }

    @PostMapping("/workflow/closure/save")
    public Result<Map<String, Object>> saveClosure(@RequestBody Map<String, Object> body) {
        return Result.success("闭环信息已保存", workflowService.saveClosure(body));
    }

    @PostMapping("/workflow/closure/confirm-customer")
    public Result<Map<String, Object>> confirmCustomer(@RequestBody Map<String, Object> body) {
        String caseNo = str(body, "caseNo");
        if (caseNo.isBlank()) throw new BusinessException("caseNo 不能为空");
        return Result.success("客户已确认", workflowService.confirmCustomer(caseNo));
    }

    @PostMapping("/workflow/closure/close")
    public Result<Map<String, Object>> closeWithClosure(@RequestBody Map<String, Object> body) {
        String caseNo = str(body, "caseNo");
        if (caseNo.isBlank()) throw new BusinessException("caseNo 不能为空");
        return Result.success("案例已关闭", workflowService.closeWithClosure(caseNo, str(body, "operator")));
    }

    // ── 原始 CRUD 兼容 ────────────────────────────────────────

    @RequestMapping("/afterSalesCase/list")
    public Result<List<AfterSalesCase>> afterSalesCaseList() {
        return Result.success(afterSalesService.afterSalesCaseList());
    }

    @RequestMapping("/afterSalesCase/get")
    public Result<AfterSalesCase> getAfterSalesCaseById(String caseNo) {
        return Result.success(afterSalesService.getAfterSalesCaseById(caseNo));
    }

    @RequestMapping("/afterSalesCase/insert")
    public Result<Void> insertAfterSalesCase(@RequestBody AfterSalesCase c) {
        afterSalesService.insertAfterSalesCase(c);
        return Result.success();
    }

    @RequestMapping("/afterSalesCase/update")
    public Result<Void> updateAfterSalesCase(@RequestBody AfterSalesCase c) {
        afterSalesService.updateAfterSalesCase(c);
        return Result.success();
    }

    @RequestMapping("/afterSalesCase/delete")
    public Result<Void> deleteAfterSalesCase(String caseNo) {
        afterSalesService.deleteAfterSalesCase(caseNo);
        return Result.success();
    }

    @RequestMapping("/settlement/list")
    public Result<List<CostSettlement>> settlementList() {
        return Result.success(afterSalesService.settlementList());
    }

    @RequestMapping("/settlement/get")
    public Result<CostSettlement> getSettlementById(Long settlementId) {
        return Result.success(afterSalesService.getSettlementById(settlementId));
    }

    @RequestMapping("/settlement/insert")
    public Result<Void> insertSettlement(@RequestBody CostSettlement s) {
        afterSalesService.insertSettlement(s);
        return Result.success();
    }

    @RequestMapping("/settlement/update")
    public Result<Void> updateSettlement(@RequestBody CostSettlement s) {
        afterSalesService.updateSettlement(s);
        return Result.success();
    }

    @RequestMapping("/settlement/delete")
    public Result<Void> deleteSettlement(Long settlementId) {
        afterSalesService.deleteSettlement(settlementId);
        return Result.success();
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : "";
    }

    private Long longVal(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
}
