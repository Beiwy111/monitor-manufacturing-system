package com.upc.computer.service;

import com.upc.computer.entity.CostSettlement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface CostService {

    // ── 原始 CRUD（保留兼容） ──────────────────────────────────
    ArrayList<CostSettlement> settlementList();
    CostSettlement getSettlementById(Long settlementId);
    void insertSettlement(CostSettlement settlement);
    void updateSettlement(CostSettlement settlement);
    void deleteSettlement(Long settlementId);

    // ── 视图查询 ──────────────────────────────────────────────
    List<Map<String, Object>> listSettlementViews();
    Map<String, Object> costKpi();
    List<Map<String, Object>> groupBySourceType();

    // ── 状态流转 ──────────────────────────────────────────────
    /** DRAFT -> CONFIRMED */
    CostSettlement confirmSettlement(Long settlementId, String operator);

    /** CONFIRMED -> EXPORTED */
    CostSettlement exportSettlement(Long settlementId, String operator);
}
