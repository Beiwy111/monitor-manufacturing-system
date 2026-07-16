package com.upc.computer.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface FinanceMapper {

    @Select("""
        SELECT
          wo.work_order_id   AS workOrderId,
          wo.work_order_no   AS workOrderNo,
          co.order_no        AS orderNo,
          co.customer_name   AS customerName,
          m.material_name    AS productName,
          wo.status          AS workOrderStatus,
          COALESCE(cs.material_cost, 0)   AS materialCost,
          COALESCE(cs.labor_cost, 0)      AS laborCost,
          COALESCE(cs.equipment_cost, 0)  AS equipmentCost,
          COALESCE(cs.quality_cost, 0)    AS qualityCost,
          COALESCE(CASE WHEN cs.source_type='NONCONFORMING_PRODUCT' THEN cs.total_cost ELSE 0 END, 0) AS reworkScrapCost,
          COALESCE(CASE WHEN cs.source_type IN ('PURCHASE_RETURN','WORK_ORDER') THEN cs.other_cost ELSE 0 END, 0) AS warehouseLogisticsCost,
          COALESCE(CASE WHEN cs.source_type='AFTER_SALES' THEN cs.total_cost ELSE 0 END, 0) AS afterSalesCost,
          COALESCE(cs.other_cost, 0)      AS otherCost,
          COALESCE(cs.total_cost, 0)      AS totalCost,
          cs.settlement_no   AS settlementNo,
          cs.settlement_status AS settlementStatus,
          cs.settlement_period AS settlementPeriod
        FROM work_order wo
        LEFT JOIN production_plan pp ON pp.plan_id = wo.plan_id
        LEFT JOIN customer_order co ON co.order_id = pp.source_order_id
        LEFT JOIN material m ON m.material_id = wo.material_id
        LEFT JOIN cost_settlement cs ON cs.work_order_id = wo.work_order_id
        ORDER BY wo.created_at DESC
        """)
    List<Map<String, Object>> listWorkOrderCostOverview();

    @Select("""
        SELECT
          co.order_id        AS orderId,
          co.order_no        AS orderNo,
          co.customer_name   AS customerName,
          GROUP_CONCAT(DISTINCT oi.product_name ORDER BY oi.order_item_id SEPARATOR '、') AS productName,
          COALESCE(SUM(CASE WHEN d.delivery_status='SHIPPED' THEN d.delivery_quantity ELSE 0 END), 0) AS deliveredQty,
          COALESCE(MAX(oi.unit_price), 0) AS unitPrice,
          COALESCE(SUM(oi.line_amount), co.order_amount) AS orderLineAmount,
          co.order_amount    AS contractAmount,
          co.audit_status    AS auditStatus,
          co.required_delivery_date AS requiredDeliveryDate
        FROM customer_order co
        LEFT JOIN customer_order_item oi ON oi.order_id = co.order_id
        LEFT JOIN delivery_order d ON d.order_id = co.order_id
        WHERE co.audit_status NOT IN ('PENDING','REJECTED','CANCELLED')
        GROUP BY co.order_id, co.order_no, co.customer_name, co.order_amount, co.audit_status, co.required_delivery_date
        ORDER BY co.order_date DESC
        """)
    List<Map<String, Object>> listOrderRevenueBase();

    @Select("""
        SELECT order_id AS orderId,
          COALESCE(SUM(material_cost),0) AS materialCost,
          COALESCE(SUM(labor_cost),0) AS laborCost,
          COALESCE(SUM(equipment_cost),0) AS equipmentCost,
          COALESCE(SUM(quality_cost),0) AS qualityCost,
          COALESCE(SUM(CASE WHEN source_type='NONCONFORMING_PRODUCT' THEN total_cost ELSE 0 END),0) AS reworkScrapCost,
          COALESCE(SUM(CASE WHEN source_type IN ('PURCHASE_RETURN','WORK_ORDER') THEN other_cost ELSE 0 END),0) AS warehouseLogisticsCost,
          COALESCE(SUM(CASE WHEN source_type='AFTER_SALES' THEN total_cost ELSE 0 END),0) AS afterSalesCost,
          COALESCE(SUM(other_cost),0) AS otherCost,
          COALESCE(SUM(total_cost),0) AS totalCost
        FROM cost_settlement
        WHERE order_id IS NOT NULL
        GROUP BY order_id
        """)
    List<Map<String, Object>> costSumByOrder();

    @Select("""
        SELECT payment_id paymentId, order_id orderId, order_no orderNo, customer_name customerName,
               contract_amount contractAmount, discount_amount discountAmount, refund_amount refundAmount,
               tax_amount taxAmount, receivable_amount receivableAmount, received_amount receivedAmount,
               planned_date plannedDate, actual_date actualDate, payment_status paymentStatus, remark,
               created_at createdAt, updated_at updatedAt
        FROM finance_payment
        ORDER BY planned_date DESC, order_no
        """)
    List<Map<String, Object>> listPayments();

    @Select("""
        SELECT log_id logId, order_id orderId, order_no orderNo, customer_name customerName,
               note, operator, created_at createdAt
        FROM finance_collection_log
        ORDER BY created_at DESC
        """)
    List<Map<String, Object>> listCollectionLogs();

    @Select("""
        SELECT
          fp.customer_name AS customerName,
          SUM(fp.receivable_amount) AS totalDebt,
          SUM(CASE WHEN fp.payment_status='OVERDUE' OR (fp.planned_date < CURDATE() AND fp.receivable_amount > 0)
            THEN fp.receivable_amount ELSE 0 END) AS overdueAmount,
          MAX(CASE WHEN fp.planned_date < CURDATE() AND fp.receivable_amount > 0
            THEN DATEDIFF(CURDATE(), fp.planned_date) ELSE 0 END) AS maxOverdueDays,
          MAX(fp.payment_status) AS worstStatus
        FROM finance_payment fp
        WHERE fp.receivable_amount > 0
        GROUP BY fp.customer_name
        ORDER BY overdueAmount DESC, totalDebt DESC
        """)
    List<Map<String, Object>> listReceivableByCustomer();

    @Select("""
        SELECT DATE_FORMAT(confirmed_at, '%Y-%m') AS period,
               COALESCE(SUM(total_cost),0) AS amount
        FROM cost_settlement
        WHERE settlement_status IN ('CONFIRMED','EXPORTED') AND confirmed_at IS NOT NULL
        GROUP BY DATE_FORMAT(confirmed_at, '%Y-%m')
        ORDER BY period
        """)
    List<Map<String, Object>> monthlyCostTrend();

    @Select("""
        SELECT DATE_FORMAT(actual_date, '%Y-%m') AS period,
               COALESCE(SUM(received_amount),0) AS amount
        FROM finance_payment
        WHERE actual_date IS NOT NULL
        GROUP BY DATE_FORMAT(actual_date, '%Y-%m')
        ORDER BY period
        """)
    List<Map<String, Object>> monthlyCollectionTrend();
}
