package com.upc.computer.controller;

import com.upc.computer.common.BusinessException;
import com.upc.computer.common.Result;
import com.upc.computer.entity.CostSettlement;
import com.upc.computer.service.CostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 成本结算接口
 *
 * GET  /cost/settlement/views          结算列表（含来源类型中文、工单号）
 * GET  /cost/kpi                       KPI 统计（各成本类型合计 + 状态计数）
 * GET  /cost/group                     按成本来源分组统计
 * POST /cost/settlement/confirm        DRAFT -> CONFIRMED
 * POST /cost/settlement/export         CONFIRMED -> EXPORTED
 * POST /cost/settlement/save           新增/更新结算单（自动计算 totalCost）
 */
@RestController
@RequestMapping("/cost")
public class CostController {

    @Autowired
    private CostService costService;

    @GetMapping("/settlement/views")
    public Result<List<Map<String, Object>>> settlementViews() {
        return Result.success(costService.listSettlementViews());
    }

    @GetMapping("/kpi")
    public Result<Map<String, Object>> kpi() {
        return Result.success(costService.costKpi());
    }

    @GetMapping("/group")
    public Result<List<Map<String, Object>>> group() {
        return Result.success(costService.groupBySourceType());
    }

    @PostMapping("/settlement/confirm")
    public Result<CostSettlement> confirm(@RequestBody Map<String, Object> body) {
        Long id = toLong(body.get("settlementId"));
        if (id == null) throw new BusinessException("settlementId 不能为空");
        return Result.success("已确认", costService.confirmSettlement(id, str(body, "operator")));
    }

    @PostMapping("/settlement/export")
    public Result<CostSettlement> export(@RequestBody Map<String, Object> body) {
        Long id = toLong(body.get("settlementId"));
        if (id == null) throw new BusinessException("settlementId 不能为空");
        return Result.success("已导出", costService.exportSettlement(id, str(body, "operator")));
    }

    @PostMapping("/settlement/save")
    public Result<Void> save(@RequestBody CostSettlement settlement) {
        if (settlement.getSettlementId() != null) {
            costService.updateSettlement(settlement);
        } else {
            costService.insertSettlement(settlement);
        }
        return Result.success();
    }

    // ── 兼容原 AfterSalesController 路径 ─────────────────────

    @RequestMapping("/settlement/list")
    public Result<List<CostSettlement>> list() {
        return Result.success(costService.settlementList());
    }

    @RequestMapping("/settlement/get")
    public Result<CostSettlement> get(Long settlementId) {
        return Result.success(costService.getSettlementById(settlementId));
    }

    @RequestMapping("/settlement/insert")
    public Result<Void> insert(@RequestBody CostSettlement s) {
        costService.insertSettlement(s);
        return Result.success();
    }

    @RequestMapping("/settlement/update")
    public Result<Void> update(@RequestBody CostSettlement s) {
        costService.updateSettlement(s);
        return Result.success();
    }

    @RequestMapping("/settlement/delete")
    public Result<Void> delete(Long settlementId) {
        costService.deleteSettlement(settlementId);
        return Result.success();
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : "";
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
}
