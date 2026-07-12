package com.upc.computer.service;

import com.upc.computer.entity.NonconformingProduct;
import com.upc.computer.entity.QualityInspection;
import com.upc.computer.entity.QualityInspectionItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface QualityService {

    // ── 原始 CRUD ──────────────────────────────────────────────
    ArrayList<QualityInspection> inspectionList();
    QualityInspection getInspectionById(Long inspectionId);
    void insertInspection(QualityInspection inspection);
    void updateInspection(QualityInspection inspection);
    void deleteInspection(Long inspectionId);

    ArrayList<NonconformingProduct> nonconformingList();
    NonconformingProduct getNonconformingById(Long nonconformingId);
    void insertNonconforming(NonconformingProduct nonconforming);
    void updateNonconforming(NonconformingProduct nonconforming);
    void deleteNonconforming(Long nonconformingId);

    // ── 视图查询 ──────────────────────────────────────────────
    List<Map<String, Object>> listInspectionViews();
    Map<String, Object> getInspectionDetail(Long inspectionId);
    List<Map<String, Object>> listNonconformingViews();
    List<Map<String, Object>> listRecheckViews();
    Map<String, Object> inspectionKpi();

    // ── 检测项 ────────────────────────────────────────────────
    List<Map<String, Object>> listItems(Long inspectionId);
    void saveItems(Long inspectionId, List<QualityInspectionItem> items);
    List<QualityInspectionItem> generateDefaultItems(Long inspectionId);
    Map<String, Object> evaluate(Long inspectionId);

    // ── 质检状态流转 ──────────────────────────────────────────
    QualityInspection passInspection(Long inspectionId, String remark, String operator,
                                     Integer sampleQty, Integer qualifiedQty, Integer unqualifiedQty);
    QualityInspection failInspection(Long inspectionId, String defectType, String defectReason,
                                     BigDecimal defectQuantity, String severity, String remark, String operator);
    QualityInspection requireRecheck(Long inspectionId, String reason, String operator);
    QualityInspection recheckPass(Long inspectionId, String remark, String operator);
    QualityInspection recheckFail(Long inspectionId, String defectType, String defectReason,
                                  BigDecimal defectQuantity, String severity, String remark, String operator);
    QualityInspection closeInspection(Long inspectionId, String remark, String operator);

    // ── 不良品处置 ────────────────────────────────────────────
    NonconformingProduct handleNonconforming(Long nonconformingId, String handleMethod,
                                             String remark, String operator);

    // ── 来料检 / 抽样 ─────────────────────────────────────────
    QualityInspection createIncomingInspection(Long materialId, String batchNo,
                                               int lotQuantity, int sampleQuantity, String operator);
    QualityInspection updateSampling(Long inspectionId, int sampleQty, int qualifiedQty, int unqualifiedQty);
}
