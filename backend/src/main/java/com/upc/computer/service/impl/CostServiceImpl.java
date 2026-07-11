package com.upc.computer.service.impl;

import com.upc.computer.common.BusinessException;
import com.upc.computer.entity.CostSettlement;
import com.upc.computer.mapper.CostSettlementMapper;
import com.upc.computer.service.CostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CostServiceImpl implements CostService {

    @Autowired
    private CostSettlementMapper mapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── CRUD ──────────────────────────────────────────────────
    @Override public ArrayList<CostSettlement> settlementList() { return mapper.settlementList(); }
    @Override public CostSettlement getSettlementById(Long id)  { return mapper.getSettlementById(id); }

    @Override
    public void insertSettlement(CostSettlement s) {
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        if (s.getSettlementStatus() == null) s.setSettlementStatus("DRAFT");
        recalcTotal(s);
        mapper.insertSettlement(s);
    }

    @Override
    public void updateSettlement(CostSettlement s) {
        s.setUpdatedAt(LocalDateTime.now());
        recalcTotal(s);
        mapper.updateSettlement(s);
    }

    @Override public void deleteSettlement(Long id) { mapper.deleteSettlement(id); }

    // ── 视图查询 ──────────────────────────────────────────────
    @Override
    public List<Map<String, Object>> listSettlementViews() {
        List<Map<String, Object>> list = mapper.listSettlementViews();
        list.forEach(row -> {
            row.put("settlementStatusCn", statusCn(str(row, "settlementStatus")));
            row.put("sourceTypeCn",       sourceTypeCn(str(row, "sourceType")));
            fmtTime(row, "confirmedAt");
            fmtTime(row, "createdAt");
        });
        return list;
    }

    @Override
    public Map<String, Object> costKpi() {
        Map<String, Object> kpi = mapper.costKpi();
        if (kpi == null) return Map.of();
        return kpi;
    }

    @Override
    public List<Map<String, Object>> groupBySourceType() {
        List<Map<String, Object>> list = mapper.groupBySourceType();
        list.forEach(row -> row.put("sourceTypeCn", sourceTypeCn(str(row, "sourceType"))));
        return list;
    }

    // ── 状态流转 ──────────────────────────────────────────────
    @Override
    @Transactional
    public CostSettlement confirmSettlement(Long settlementId, String operator) {
        CostSettlement s = require(settlementId);
        if (!"DRAFT".equals(s.getSettlementStatus()))
            throw new BusinessException("仅草稿状态可确认（当前：" + statusCn(s.getSettlementStatus()) + "）");
        s.setSettlementStatus("CONFIRMED");
        s.setConfirmedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        mapper.updateSettlement(s);
        return s;
    }

    @Override
    @Transactional
    public CostSettlement exportSettlement(Long settlementId, String operator) {
        CostSettlement s = require(settlementId);
        if (!"CONFIRMED".equals(s.getSettlementStatus()))
            throw new BusinessException("仅已确认状态可导出（当前：" + statusCn(s.getSettlementStatus()) + "）");
        s.setSettlementStatus("EXPORTED");
        s.setExportedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        mapper.updateSettlement(s);
        return s;
    }

    // ── 工具 ──────────────────────────────────────────────────
    private CostSettlement require(Long id) {
        if (id == null) throw new BusinessException("settlementId 不能为空");
        CostSettlement s = mapper.getSettlementById(id);
        if (s == null) throw new BusinessException("结算单不存在：" + id);
        return s;
    }

    private void recalcTotal(CostSettlement s) {
        BigDecimal total = BigDecimal.ZERO;
        if (s.getMaterialCost()  != null) total = total.add(s.getMaterialCost());
        if (s.getLaborCost()     != null) total = total.add(s.getLaborCost());
        if (s.getEquipmentCost() != null) total = total.add(s.getEquipmentCost());
        if (s.getQualityCost()   != null) total = total.add(s.getQualityCost());
        if (s.getOtherCost()     != null) total = total.add(s.getOtherCost());
        s.setTotalCost(total);
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
            case "DRAFT"     -> "草稿";
            case "CONFIRMED" -> "已确认";
            case "EXPORTED"  -> "已导出";
            default -> s;
        };
    }

    private String sourceTypeCn(String s) {
        if (s == null || s.isEmpty()) return "其他";
        return switch (s) {
            case "NONCONFORMING_PRODUCT"  -> "不良品处理";
            case "AFTER_SALES"            -> "售后维修";
            case "EQUIPMENT_MAINTENANCE"  -> "设备维修";
            case "PURCHASE_RETURN"        -> "采购退货损失";
            case "WORK_ORDER"             -> "工单成本";
            case "OTHER"                  -> "其他";
            default -> s;
        };
    }
}
