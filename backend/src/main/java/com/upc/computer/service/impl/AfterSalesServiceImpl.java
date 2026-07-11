package com.upc.computer.service.impl;

import com.upc.computer.common.BusinessException;
import com.upc.computer.entity.AfterSalesCase;
import com.upc.computer.entity.CostSettlement;
import com.upc.computer.mapper.AfterSalesCaseMapper;
import com.upc.computer.service.AfterSalesService;
import com.upc.computer.service.CostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AfterSalesServiceImpl implements AfterSalesService {

    @Autowired private AfterSalesCaseMapper caseMapper;
    @Autowired @Lazy private CostService costService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── 售后案例 CRUD ─────────────────────────────────────────
    @Override public ArrayList<AfterSalesCase> afterSalesCaseList() { return caseMapper.afterSalesCaseList(); }
    @Override public AfterSalesCase getAfterSalesCaseById(String caseNo) { return caseMapper.getAfterSalesCaseById(caseNo); }
    @Override public void insertAfterSalesCase(AfterSalesCase c) { c.setCreatedAt(LocalDateTime.now()); c.setUpdatedAt(LocalDateTime.now()); if (c.getCaseStatus()==null) c.setCaseStatus("OPEN"); caseMapper.insertAfterSalesCase(c); }
    @Override public void updateAfterSalesCase(AfterSalesCase c) { c.setUpdatedAt(LocalDateTime.now()); caseMapper.updateAfterSalesCase(c); }
    @Override public void deleteAfterSalesCase(String caseNo) { caseMapper.deleteAfterSalesCase(caseNo); }

    // ── Settlement 委托给 CostService ─────────────────────────
    @Override public ArrayList<CostSettlement> settlementList() { return costService.settlementList(); }
    @Override public CostSettlement getSettlementById(Long id)  { return costService.getSettlementById(id); }
    @Override public void insertSettlement(CostSettlement s)    { costService.insertSettlement(s); }
    @Override public void updateSettlement(CostSettlement s)    { costService.updateSettlement(s); }
    @Override public void deleteSettlement(Long id)             { costService.deleteSettlement(id); }

    // ── 视图查询 ──────────────────────────────────────────────
    @Override
    public List<Map<String, Object>> listCaseViews() {
        List<Map<String, Object>> list = caseMapper.listCaseViews();
        list.forEach(row -> {
            row.put("caseStatusCn",  statusCn(str(row, "caseStatus")));
            row.put("caseLevelCn",   levelCn(str(row, "caseLevel")));
            row.put("problemTypeCn", problemTypeCn(str(row, "problemType")));
            fmtTime(row, "openedAt"); fmtTime(row, "processingAt");
            fmtTime(row, "resolvedAt"); fmtTime(row, "closedAt"); fmtTime(row, "updatedAt");
        });
        return list;
    }

    @Override
    public Map<String, Object> getTraceDetail(String caseNo) {
        Map<String, Object> detail = caseMapper.getTraceDetail(caseNo);
        if (detail == null) return Map.of();
        detail.put("caseStatusCn",       statusCn(str(detail, "caseStatus")));
        detail.put("caseLevelCn",        levelCn(str(detail, "caseLevel")));
        detail.put("problemTypeCn",      problemTypeCn(str(detail, "problemType")));
        detail.put("inspectionStatusCn", inspectionStatusCn(str(detail, "inspectionStatus")));
        detail.put("severityCn",         severityCn(str(detail, "severity")));
        detail.put("ncHandleStatusCn",   ncHandleStatusCn(str(detail, "ncHandleStatus")));
        detail.put("traceChain",         buildTraceChain(detail));
        fmtTime(detail, "openedAt"); fmtTime(detail, "processingAt");
        fmtTime(detail, "resolvedAt"); fmtTime(detail, "closedAt");
        return detail;
    }

    @Override
    public Map<String, Object> caseKpi() {
        Map<String, Object> kpi = caseMapper.caseKpi();
        return kpi != null ? kpi : Map.of();
    }

    // ── 状态流转 ──────────────────────────────────────────────
    @Override
    @Transactional
    public AfterSalesCase acceptCase(String caseNo, String operator) {
        AfterSalesCase c = require(caseNo);
        if (!"OPEN".equals(c.getCaseStatus()))
            throw new BusinessException("仅 OPEN 状态可受理（当前：" + statusCn(c.getCaseStatus()) + "）");
        c.setCaseStatus("PROCESSING");
        c.setProcessingAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        caseMapper.updateAfterSalesCase(c);
        return c;
    }

    @Override
    @Transactional
    public AfterSalesCase resolveCase(String caseNo, String solution, String traceResult, String operator) {
        AfterSalesCase c = require(caseNo);
        if (!"PROCESSING".equals(c.getCaseStatus()))
            throw new BusinessException("仅受理中状态可标记解决（当前：" + statusCn(c.getCaseStatus()) + "）");
        if (solution == null || solution.isBlank())
            throw new BusinessException("解决方案不能为空");
        c.setCaseStatus("RESOLVED");
        c.setHandleResult(solution);
        if (traceResult != null && !traceResult.isBlank()) c.setTraceResult(traceResult);
        c.setResolvedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        caseMapper.updateAfterSalesCase(c);
        return c;
    }

    @Override
    @Transactional
    public AfterSalesCase closeCase(String caseNo, String remark, String operator) {
        AfterSalesCase c = require(caseNo);
        if ("CLOSED".equals(c.getCaseStatus()))
            throw new BusinessException("案例已关闭");
        c.setCaseStatus("CLOSED");
        if (remark != null && !remark.isBlank()) {
            String old = c.getHandleResult();
            c.setHandleResult(old == null ? remark : old + " | " + remark);
        }
        c.setClosedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        caseMapper.updateAfterSalesCase(c);
        return c;
    }

    // ── 工具 ──────────────────────────────────────────────────
    private AfterSalesCase require(String caseNo) {
        if (caseNo == null || caseNo.isBlank()) throw new BusinessException("案例编号不能为空");
        AfterSalesCase c = caseMapper.getAfterSalesCaseById(caseNo);
        if (c == null) throw new BusinessException("售后案例不存在：" + caseNo);
        return c;
    }

    private List<Map<String, Object>> buildTraceChain(Map<String, Object> d) {
        List<Map<String, Object>> chain = new ArrayList<>();
        if (d.get("orderNo") != null)
            chain.add(step("客户订单", str(d,"orderNo"), statusCn2(str(d,"orderStatus")), "order"));
        if (d.get("materialName") != null)
            chain.add(step("物料批次", str(d,"materialName") + " / " + str(d,"batchNo"), "", "material"));
        if (d.get("inspectionNo") != null)
            chain.add(step("质量检验", str(d,"inspectionNo"),
                    str(d,"inspectionStatusCn") + "，不合格数：" + d.getOrDefault("unqualifiedQty","0"), "quality"));
        if (d.get("nonconformingNo") != null)
            chain.add(step("不良品记录", str(d,"nonconformingNo"),
                    str(d,"defectType") + " / " + str(d,"severityCn") + " / " + str(d,"ncHandleStatusCn"), "defect"));
        chain.add(step("售后案例", str(d,"caseNo"),
                str(d,"problemTypeCn") + " · " + str(d,"caseStatusCn"), "aftersale"));
        return chain;
    }

    private Map<String, Object> step(String title, String no, String desc, String type) {
        return Map.of("title", title, "no", no, "desc", desc, "type", type);
    }

    private void fmtTime(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v instanceof LocalDateTime) row.put(key, ((LocalDateTime) v).format(FMT));
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : "";
    }

    private String statusCn(String s) {
        if (s == null) return "-";
        return switch (s) {
            case "OPEN"       -> "待受理";
            case "PROCESSING" -> "处理中";
            case "RESOLVED"   -> "已解决";
            case "CLOSED"     -> "已关闭";
            default -> s;
        };
    }

    private String statusCn2(String s) {
        if (s == null) return "-";
        return switch (s) {
            case "已完成","COMPLETED" -> "已完成";
            case "已发货","SHIPPED"   -> "已发货";
            default -> s;
        };
    }

    private String levelCn(String s) {
        if (s == null) return "-";
        return switch (s) {
            case "LOW"      -> "低";
            case "MEDIUM"   -> "中";
            case "HIGH"     -> "高";
            case "CRITICAL" -> "紧急";
            default -> s;
        };
    }

    private String problemTypeCn(String s) {
        if (s == null) return "-";
        return switch (s) {
            case "DISPLAY_DEFECT"  -> "显示缺陷";
            case "COLOR_ISSUE"     -> "色彩问题";
            case "DEAD_PIXEL"      -> "坏点/亮点";
            case "INTERFACE_FAULT" -> "接口故障";
            case "APPEARANCE"      -> "外观损伤";
            case "POWER_ISSUE"     -> "电源问题";
            case "OTHER"           -> "其他";
            default -> s;
        };
    }

    private String inspectionStatusCn(String s) {
        if (s == null) return "-";
        return switch (s) {
            case "PENDING"          -> "待检";
            case "PASSED"           -> "质检通过";
            case "FAILED"           -> "质检不通过";
            case "RECHECK_REQUIRED" -> "需复检";
            case "CLOSED"           -> "已关闭";
            default -> s;
        };
    }

    private String severityCn(String s) {
        if (s == null) return "-";
        return switch (s) {
            case "MINOR" -> "轻微"; case "GENERAL" -> "一般";
            case "MAJOR" -> "严重"; case "CRITICAL" -> "致命";
            default -> s;
        };
    }

    private String ncHandleStatusCn(String s) {
        if (s == null) return "-";
        return switch (s) {
            case "PENDING" -> "待处置"; case "PROCESSING" -> "处理中"; case "DONE" -> "已处置";
            default -> s;
        };
    }
}
