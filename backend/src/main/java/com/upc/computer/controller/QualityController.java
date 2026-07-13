package com.upc.computer.controller;

import com.upc.computer.common.BusinessException;
import com.upc.computer.common.Result;
import com.upc.computer.dto.QualityActionRequest;
import com.upc.computer.entity.NonconformingProduct;
import com.upc.computer.entity.QualityInspection;
import com.upc.computer.entity.QualityInspectionItem;
import com.upc.computer.service.QualityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 质量管理接口
 *
 * GET  /quality/inspection/views              质检列表（含展示字段）
 * GET  /quality/inspection/detail?id=         质检详情（含检测项 + 不良品）
 * GET  /quality/nonconforming/views           不良品列表
 * GET  /quality/recheck/views                 需复检列表
 * GET  /quality/kpi                           KPI 统计
 * GET  /quality/inspection/items?id=          检测项列表
 * POST /quality/inspection/generateDefaultItems   生成默认检测项
 * POST /quality/inspection/items/save         保存检测项
 * POST /quality/inspection/evaluate           汇总判定建议
 * POST /quality/inspection/pass               质检通过
 * POST /quality/inspection/fail               质检不通过
 * POST /quality/inspection/recheck            转复检
 * POST /quality/inspection/recheckPass        复检通过
 * POST /quality/inspection/recheckFail        复检不通过
 * POST /quality/inspection/close              关闭质检单
 * POST /quality/nonconforming/handle          不良品处置
 */
@RestController
@RequestMapping("/quality")
public class QualityController {

    @Autowired
    private QualityService qualityService;

    // ── 查询视图 ──────────────────────────────────────────────

    @GetMapping("/inspection/views")
    public Result<List<Map<String, Object>>> inspectionViews() {
        return Result.success(qualityService.listInspectionViews());
    }

    @GetMapping("/inspection/detail")
    public Result<Map<String, Object>> inspectionDetail(@RequestParam Long inspectionId) {
        return Result.success(qualityService.getInspectionDetail(inspectionId));
    }

    @GetMapping("/nonconforming/views")
    public Result<List<Map<String, Object>>> nonconformingViews() {
        return Result.success(qualityService.listNonconformingViews());
    }

    @GetMapping("/recheck/views")
    public Result<List<Map<String, Object>>> recheckViews() {
        return Result.success(qualityService.listRecheckViews());
    }

    @GetMapping("/kpi")
    public Result<Map<String, Object>> kpi() {
        return Result.success(qualityService.inspectionKpi());
    }

    // ── 检测项 ────────────────────────────────────────────────

    @GetMapping("/inspection/items")
    public Result<List<Map<String, Object>>> listItems(@RequestParam Long inspectionId) {
        return Result.success(qualityService.listItems(inspectionId));
    }

    @PostMapping("/inspection/generateDefaultItems")
    public Result<List<QualityInspectionItem>> generateDefaultItems(@RequestBody Map<String, Object> body) {
        Long id = toLong(body.get("inspectionId"));
        if (id == null) throw new BusinessException("inspectionId 不能为空");
        return Result.success(qualityService.generateDefaultItems(id));
    }

    @PostMapping("/inspection/items/save")
    public Result<Void> saveItems(@RequestBody Map<String, Object> body) {
        Long id = toLong(body.get("inspectionId"));
        if (id == null) throw new BusinessException("inspectionId 不能为空");
        @SuppressWarnings("unchecked")
        List<QualityInspectionItem> items = (List<QualityInspectionItem>) body.get("items");
        if (items == null) throw new BusinessException("items 不能为空");
        qualityService.saveItems(id, items);
        return Result.success();
    }

    @PostMapping("/inspection/evaluate")
    public Result<Map<String, Object>> evaluate(@RequestBody Map<String, Object> body) {
        Long id = toLong(body.get("inspectionId"));
        if (id == null) throw new BusinessException("inspectionId 不能为空");
        return Result.success(qualityService.evaluate(id));
    }

    // ── 质检状态流转 ──────────────────────────────────────────

    @PostMapping("/inspection/pass")
    public Result<QualityInspection> pass(@RequestBody QualityActionRequest req) {
        validate(req.getInspectionId(), "inspectionId");
        return Result.success("质检通过", qualityService.passInspection(
                req.getInspectionId(), req.getRemark(), req.getOperator()));
    }

    @PostMapping("/inspection/fail")
    public Result<QualityInspection> fail(@RequestBody QualityActionRequest req) {
        validate(req.getInspectionId(), "inspectionId");
        BigDecimal qty = req.getDefectQuantity() != null ? req.getDefectQuantity() : BigDecimal.ONE;
        return Result.success("质检不通过，已生成不良品记录", qualityService.failInspection(
                req.getInspectionId(), req.getDefectType(), req.getDefectReason(),
                qty, req.getSeverity(), req.getRemark(), req.getOperator()));
    }

    @PostMapping("/inspection/recheck")
    public Result<QualityInspection> recheck(@RequestBody QualityActionRequest req) {
        validate(req.getInspectionId(), "inspectionId");
        return Result.success("已转复检", qualityService.requireRecheck(
                req.getInspectionId(), req.getReason(), req.getOperator()));
    }

    @PostMapping("/inspection/recheckPass")
    public Result<QualityInspection> recheckPass(@RequestBody QualityActionRequest req) {
        validate(req.getInspectionId(), "inspectionId");
        return Result.success("复检通过", qualityService.recheckPass(
                req.getInspectionId(), req.getRemark(), req.getOperator()));
    }

    @PostMapping("/inspection/recheckFail")
    public Result<QualityInspection> recheckFail(@RequestBody QualityActionRequest req) {
        validate(req.getInspectionId(), "inspectionId");
        BigDecimal qty = req.getDefectQuantity() != null ? req.getDefectQuantity() : BigDecimal.ONE;
        return Result.success("复检不通过", qualityService.recheckFail(
                req.getInspectionId(), req.getDefectType(), req.getDefectReason(),
                qty, req.getSeverity(), req.getRemark(), req.getOperator()));
    }

    @PostMapping("/inspection/close")
    public Result<QualityInspection> close(@RequestBody QualityActionRequest req) {
        validate(req.getInspectionId(), "inspectionId");
        return Result.success("已关闭", qualityService.closeInspection(
                req.getInspectionId(), req.getRemark(), req.getOperator()));
    }

    @PostMapping("/nonconforming/handle")
    public Result<NonconformingProduct> handleNonconforming(@RequestBody QualityActionRequest req) {
        validate(req.getNonconformingId(), "nonconformingId");
        if (req.getHandleMethod() == null || req.getHandleMethod().isBlank())
            throw new BusinessException("处置方式不能为空");
        return Result.success("处置完成", qualityService.handleNonconforming(
                req.getNonconformingId(), req.getHandleMethod(),
                req.getRemark(), req.getOperator()));
    }

    @PostMapping("/inspection/createIncoming")
    public Result<QualityInspection> createIncoming(@RequestBody Map<String, Object> body) {
        Long materialId = toLong(body.get("materialId"));
        if (materialId == null) throw new BusinessException("materialId 不能为空");
        String batchNo = body.get("batchNo") != null ? body.get("batchNo").toString() : "";
        int lotQty = body.get("lotQuantity") instanceof Number n ? n.intValue() : 0;
        int sampleQty = body.get("sampleQuantity") instanceof Number n ? n.intValue() : 0;
        String operator = body.get("operator") != null ? body.get("operator").toString() : "system";
        return Result.success("来料检已登记", qualityService.createIncomingInspection(
                materialId, batchNo, lotQty, sampleQty, operator));
    }

    @PostMapping("/inspection/updateSampling")
    public Result<QualityInspection> updateSampling(@RequestBody Map<String, Object> body) {
        Long id = toLong(body.get("inspectionId"));
        if (id == null) throw new BusinessException("inspectionId 不能为空");
        int sample = body.get("sampleQuantity") instanceof Number n ? n.intValue() : 0;
        int qualified = body.get("qualifiedQuantity") instanceof Number n ? n.intValue() : 0;
        int unqualified = body.get("unqualifiedQuantity") instanceof Number n ? n.intValue() : 0;
        return Result.success(qualityService.updateSampling(id, sample, qualified, unqualified));
    }

    // ── 原始 CRUD 兼容 ────────────────────────────────────────

    @RequestMapping("/inspection/list")
    public Result<List<QualityInspection>> inspectionList() {
        return Result.success(qualityService.inspectionList());
    }

    @RequestMapping("/inspection/get")
    public Result<QualityInspection> getInspectionById(Long inspectionId) {
        return Result.success(qualityService.getInspectionById(inspectionId));
    }

    @RequestMapping("/inspection/insert")
    public Result<Void> insertInspection(@RequestBody QualityInspection inspection) {
        qualityService.insertInspection(inspection);
        return Result.success();
    }

    @RequestMapping("/inspection/update")
    public Result<Void> updateInspection(@RequestBody QualityInspection inspection) {
        qualityService.updateInspection(inspection);
        return Result.success();
    }

    @RequestMapping("/inspection/delete")
    public Result<Void> deleteInspection(Long inspectionId) {
        qualityService.deleteInspection(inspectionId);
        return Result.success();
    }

    @RequestMapping("/nonconforming/list")
    public Result<List<NonconformingProduct>> nonconformingList() {
        return Result.success(qualityService.nonconformingList());
    }

    @RequestMapping("/nonconforming/get")
    public Result<NonconformingProduct> getNonconformingById(Long nonconformingId) {
        return Result.success(qualityService.getNonconformingById(nonconformingId));
    }

    @RequestMapping("/nonconforming/insert")
    public Result<Void> insertNonconforming(@RequestBody NonconformingProduct nonconforming) {
        qualityService.insertNonconforming(nonconforming);
        return Result.success();
    }

    @RequestMapping("/nonconforming/update")
    public Result<Void> updateNonconforming(@RequestBody NonconformingProduct nonconforming) {
        qualityService.updateNonconforming(nonconforming);
        return Result.success();
    }

    @RequestMapping("/nonconforming/delete")
    public Result<Void> deleteNonconforming(Long nonconformingId) {
        qualityService.deleteNonconforming(nonconformingId);
        return Result.success();
    }

    // ── 工具 ──────────────────────────────────────────────────

    private void validate(Long id, String field) {
        if (id == null || id <= 0) throw new BusinessException(field + " 不能为空");
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
}
