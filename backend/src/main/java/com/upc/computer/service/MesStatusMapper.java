package com.upc.computer.service;

import java.util.HashMap;
import java.util.Map;

/**
 * DB 英文状态与前端中文状态双向映射
 */
public final class MesStatusMapper {

    private static final Map<String, String> ORDER_TO_CN = Map.of(
            "PENDING", "待审核",
            "APPROVED", "已审核",
            "PLAN_PENDING", "待计划",
            "PLANNED", "已计划",
            "PRODUCING", "生产中",
            "SHIPPED", "已发货",
            "REJECTED", "已作废"
    );

    private static final Map<String, String> PLAN_TO_CN = Map.of(
            "DRAFT", "草稿",
            "PUBLISHED", "已发布",
            "SUBMITTED", "已提交",
            "EXECUTING", "执行中"
    );

    private static final Map<String, String> WORK_ORDER_TO_CN = Map.of(
            "DRAFT", "草稿",
            "RELEASED", "已下达",
            "DISPATCHED", "已派工",
            "PRODUCING", "生产中",
            "QC_PENDING", "待质检",
            "COMPLETED", "已完成"
    );

    private static final Map<String, String> DISPATCH_TO_CN = Map.of(
            "ASSIGNED", "已分配",
            "ACCEPTED", "已接收",
            "PRODUCING", "生产中",
            "RUNNING", "生产中",
            "QC_PENDING", "待质检",
            "COMPLETED", "已完成"
    );

    private static final Map<String, String> REPORT_TO_CN = Map.of(
            "SUBMITTED", "已提交",
            "CONFIRMED", "已确认",
            "REJECTED", "已驳回"
    );

    private static final Map<String, String> INSPECTION_TO_CN = Map.of(
            "PENDING", "待检",
            "QUALIFIED", "合格",
            "UNQUALIFIED", "不合格",
            "PASS", "合格",
            "FAIL", "不合格",
            "CONCESSION", "让步接收"
    );

    private static final Map<String, String> DEFECT_TO_CN = Map.of(
            "PENDING", "待处理",
            "REWORKING", "返修中",
            "SCRAPPED", "已报废",
            "REWORKED", "已返修"
    );

    private static final Map<String, String> PURCHASE_ORDER_TO_CN = Map.of(
            "RELEASED", "已下达",
            "PARTIAL_ARRIVED", "部分到货",
            "ARRIVED", "已到货",
            "CANCELLED", "已取消"
    );

    private static final Map<String, String> PURCHASE_DEMAND_TO_CN = Map.of(
            "PENDING", "待采购",
            "ORDERED", "已下单"
    );

    private static final Map<String, String> DELIVERY_TO_CN = Map.of(
            "PREPARED", "待出库",
            "PENDING", "待出库",
            "SHIPPED", "已出库"
    );

    private static final Map<String, String> ALARM_TO_CN = Map.of(
            "REPORTED", "已上报",
            "RECEIVED", "已接收",
            "PROCESSING", "处理中",
            "CLOSED", "已关闭"
    );

    private static final Map<String, String> EQUIPMENT_TO_CN = Map.of(
            "RUNNING", "运行中",
            "FAULT", "故障",
            "MAINTENANCE", "维护中",
            "IDLE", "空闲"
    );

    private static final Map<String, String> AFTERSALE_TO_CN = Map.of(
            "CREATED", "已创建",
            "PROCESSING", "处理中",
            "CLOSED", "已关闭"
    );

    private static final Map<String, String> COST_TO_CN = Map.of(
            "DRAFT", "草稿",
            "CONFIRMED", "已确认",
            "EXPORTED", "已导出"
    );

    private static final Map<String, String> INBOUND_TO_CN = Map.of(
            "PENDING", "待入库",
            "DONE", "已入库"
    );

    private static final Map<String, String> ISSUE_TO_CN = Map.of(
            "PENDING", "待领料",
            "PARTIAL", "部分领料",
            "DONE", "已完成"
    );

    private static final Map<String, String> INVENTORY_TO_CN = Map.of(
            "NORMAL", "正常",
            "LOW", "预警",
            "FROZEN", "冻结"
    );

    private static final Map<String, String> ROLE_CODE_TO_KEY = new HashMap<>();

    static {
        ROLE_CODE_TO_KEY.put("ADMIN", "admin");
        ROLE_CODE_TO_KEY.put("ORDER", "order");
        ROLE_CODE_TO_KEY.put("PLANNER", "planner");
        ROLE_CODE_TO_KEY.put("MANAGER", "manager");
        ROLE_CODE_TO_KEY.put("OPERATOR", "operator");
        ROLE_CODE_TO_KEY.put("QC", "quality");
        ROLE_CODE_TO_KEY.put("PURCHASER", "purchase");
        ROLE_CODE_TO_KEY.put("WAREHOUSE", "warehouse");
        ROLE_CODE_TO_KEY.put("DEVICE", "device");
        ROLE_CODE_TO_KEY.put("SERVICE", "aftersale");
        ROLE_CODE_TO_KEY.put("COST", "cost");
    }

    private MesStatusMapper() {
    }

    public static String toOrderCn(String db) {
        return ORDER_TO_CN.getOrDefault(db, db);
    }

    public static String toOrderDb(String cn) {
        return reverse(ORDER_TO_CN).getOrDefault(cn, cn);
    }

    public static String toPlanCn(String db) {
        return PLAN_TO_CN.getOrDefault(db, db);
    }

    public static String toPlanDb(String cn) {
        return reverse(PLAN_TO_CN).getOrDefault(cn, cn);
    }

    public static String toWorkOrderCn(String db) {
        return WORK_ORDER_TO_CN.getOrDefault(db, db);
    }

    public static String toWorkOrderDb(String cn) {
        return reverse(WORK_ORDER_TO_CN).getOrDefault(cn, cn);
    }

    public static String toDispatchCn(String db) {
        return DISPATCH_TO_CN.getOrDefault(db, db);
    }

    public static String toDispatchDb(String cn) {
        return reverse(DISPATCH_TO_CN).getOrDefault(cn, cn);
    }

    public static String toReportCn(String db) {
        return REPORT_TO_CN.getOrDefault(db, db);
    }

    public static String toReportDb(String cn) {
        return reverse(REPORT_TO_CN).getOrDefault(cn, cn);
    }

    public static String toInspectionCn(String db) {
        return INSPECTION_TO_CN.getOrDefault(db, db);
    }

    public static String toInspectionDb(String cn) {
        return reverse(INSPECTION_TO_CN).getOrDefault(cn, cn);
    }

    public static String toDefectCn(String db) {
        return DEFECT_TO_CN.getOrDefault(db, db);
    }

    public static String toDefectDb(String cn) {
        return reverse(DEFECT_TO_CN).getOrDefault(cn, cn);
    }

    public static String toPurchaseOrderCn(String db) {
        return PURCHASE_ORDER_TO_CN.getOrDefault(db, db);
    }

    public static String toPurchaseOrderDb(String cn) {
        return reverse(PURCHASE_ORDER_TO_CN).getOrDefault(cn, cn);
    }

    public static String toPurchaseDemandCn(String db) {
        return PURCHASE_DEMAND_TO_CN.getOrDefault(db, db);
    }

    public static String toDeliveryCn(String db) {
        return DELIVERY_TO_CN.getOrDefault(db, db);
    }

    public static String toDeliveryDb(String cn) {
        return reverse(DELIVERY_TO_CN).getOrDefault(cn, cn);
    }

    public static String toAlarmCn(String db) {
        return ALARM_TO_CN.getOrDefault(db, db);
    }

    public static String toAlarmDb(String cn) {
        return reverse(ALARM_TO_CN).getOrDefault(cn, cn);
    }

    public static String toEquipmentCn(String db) {
        return EQUIPMENT_TO_CN.getOrDefault(db, db);
    }

    public static String toEquipmentDb(String cn) {
        return reverse(EQUIPMENT_TO_CN).getOrDefault(cn, cn);
    }

    public static String toAftersaleCn(String db) {
        return AFTERSALE_TO_CN.getOrDefault(db, db);
    }

    public static String toAftersaleDb(String cn) {
        return reverse(AFTERSALE_TO_CN).getOrDefault(cn, cn);
    }

    public static String toCostCn(String db) {
        return COST_TO_CN.getOrDefault(db, db);
    }

    public static String toCostDb(String cn) {
        return reverse(COST_TO_CN).getOrDefault(cn, cn);
    }

    public static String toInboundCn(String db) {
        return INBOUND_TO_CN.getOrDefault(db, db);
    }

    public static String toInboundDb(String cn) {
        return reverse(INBOUND_TO_CN).getOrDefault(cn, cn);
    }

    public static String toIssueCn(String db) {
        return ISSUE_TO_CN.getOrDefault(db, db);
    }

    public static String toIssueDb(String cn) {
        return reverse(ISSUE_TO_CN).getOrDefault(cn, cn);
    }

    public static String toInventoryCn(String db) {
        return INVENTORY_TO_CN.getOrDefault(db, db != null ? db : "NORMAL");
    }

    public static String toRoleKey(String roleCode) {
        if (roleCode == null) {
            return "system";
        }
        String key = ROLE_CODE_TO_KEY.get(roleCode.toUpperCase());
        if (key != null) {
            return key;
        }
        return roleCode.toLowerCase();
    }

    public static String inspectionResultToDb(String result) {
        if ("合格".equals(result)) {
            return "QUALIFIED";
        }
        if ("不合格".equals(result)) {
            return "UNQUALIFIED";
        }
        if ("让步接收".equals(result)) {
            return "CONCESSION";
        }
        if ("PASS".equals(result)) {
            return "QUALIFIED";
        }
        if ("FAIL".equals(result)) {
            return "UNQUALIFIED";
        }
        return toInspectionDb(result);
    }

    public static String inspectionResultToCn(String result) {
        if (result == null || result.isBlank()) {
            return "待检";
        }
        return toInspectionCn(result);
    }

    private static Map<String, String> reverse(Map<String, String> source) {
        Map<String, String> reversed = new HashMap<>();
        source.forEach((k, v) -> reversed.put(v, k));
        return reversed;
    }
}
