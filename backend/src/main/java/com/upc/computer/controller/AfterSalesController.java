package com.upc.computer.controller;

import com.upc.computer.common.BusinessException;
import com.upc.computer.common.Result;
import com.upc.computer.entity.AfterSalesCase;
import com.upc.computer.entity.CostSettlement;
import com.upc.computer.service.AfterSalesService;
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
}
