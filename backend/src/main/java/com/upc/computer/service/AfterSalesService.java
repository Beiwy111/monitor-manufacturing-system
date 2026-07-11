package com.upc.computer.service;

import com.upc.computer.entity.AfterSalesCase;
import com.upc.computer.entity.CostSettlement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface AfterSalesService {

    // ── 原始 CRUD ──────────────────────────────────────────────
    ArrayList<AfterSalesCase> afterSalesCaseList();
    AfterSalesCase getAfterSalesCaseById(String caseNo);
    void insertAfterSalesCase(AfterSalesCase afterSalesCase);
    void updateAfterSalesCase(AfterSalesCase afterSalesCase);
    void deleteAfterSalesCase(String caseNo);

    ArrayList<CostSettlement> settlementList();
    CostSettlement getSettlementById(Long settlementId);
    void insertSettlement(CostSettlement settlement);
    void updateSettlement(CostSettlement settlement);
    void deleteSettlement(Long settlementId);

    // ── 视图查询 ──────────────────────────────────────────────
    List<Map<String, Object>> listCaseViews();
    Map<String, Object> getTraceDetail(String caseNo);
    Map<String, Object> caseKpi();

    // ── 状态流转 ──────────────────────────────────────────────
    /** OPEN -> PROCESSING：受理案例 */
    AfterSalesCase acceptCase(String caseNo, String operator);

    /** PROCESSING -> RESOLVED：标记已解决 */
    AfterSalesCase resolveCase(String caseNo, String solution, String traceResult, String operator);

    /** RESOLVED/PROCESSING -> CLOSED：关闭案例 */
    AfterSalesCase closeCase(String caseNo, String remark, String operator);
}
