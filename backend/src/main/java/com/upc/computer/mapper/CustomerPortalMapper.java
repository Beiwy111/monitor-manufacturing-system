package com.upc.computer.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface CustomerPortalMapper {

    @Select("""
        SELECT order_id AS orderId, order_no AS orderNo, customer_name AS customerName,
               customer_contact AS customerContact, customer_phone AS customerPhone,
               order_date AS orderDate, required_delivery_date AS requiredDeliveryDate,
               order_amount AS orderAmount, audit_status AS auditStatus,
               audit_at AS auditAt, audit_opinion AS auditOpinion, remark,
               created_at AS createdAt, updated_at AS updatedAt
        FROM customer_order
        WHERE customer_name = #{customerName}
        ORDER BY created_at DESC
        """)
    List<Map<String, Object>> listOrdersByCustomer(String customerName);

    @Select("""
        SELECT order_id AS orderId, order_no AS orderNo, customer_name AS customerName,
               customer_contact AS customerContact, customer_phone AS customerPhone,
               order_date AS orderDate, required_delivery_date AS requiredDeliveryDate,
               order_amount AS orderAmount, audit_status AS auditStatus,
               audit_at AS auditAt, audit_opinion AS auditOpinion, remark,
               created_at AS createdAt, updated_at AS updatedAt
        FROM customer_order
        WHERE order_id = #{orderId} AND customer_name = #{customerName}
        LIMIT 1
        """)
    Map<String, Object> getOrderByIdAndCustomer(@Param("orderId") Long orderId, @Param("customerName") String customerName);

    @Select("""
        SELECT i.order_item_id AS orderItemId, i.order_id AS orderId, i.material_id AS materialId,
               i.product_name AS productName, i.specification, i.quantity, i.unit,
               i.unit_price AS unitPrice, i.line_amount AS lineAmount,
               i.delivery_date AS deliveryDate, i.item_status AS itemStatus
        FROM customer_order_item i
        INNER JOIN customer_order o ON o.order_id = i.order_id
        WHERE i.order_id = #{orderId} AND o.customer_name = #{customerName}
        """)
    List<Map<String, Object>> listOrderItems(@Param("orderId") Long orderId, @Param("customerName") String customerName);

    @Select("""
        SELECT p.plan_id AS planId, p.plan_no AS planNo, p.plan_name AS planName,
               p.source_order_id AS sourceOrderId, p.planned_start_date AS plannedStartDate,
               p.planned_end_date AS plannedEndDate, p.plan_status AS planStatus,
               p.approved_at AS approvedAt, p.priority,
               COALESCE(SUM(pi.completed_quantity), 0) AS completedQty,
               COALESCE(SUM(pi.planned_quantity), 0) AS plannedQty
        FROM production_plan p
        LEFT JOIN production_plan_item pi ON pi.plan_id = p.plan_id
        WHERE p.source_order_id = #{orderId}
        GROUP BY p.plan_id
        ORDER BY p.created_at DESC
        LIMIT 1
        """)
    Map<String, Object> getPlanProgressByOrder(Long orderId);

    @Select("""
        SELECT qi.inspection_id AS inspectionId, qi.inspection_no AS inspectionNo,
               qi.inspection_type AS inspectionType, qi.inspection_result AS inspectionResult,
               qi.inspection_status AS inspectionStatus, qi.inspected_at AS inspectedAt,
               qi.qualified_quantity AS qualifiedQty, qi.unqualified_quantity AS unqualifiedQty
        FROM quality_inspection qi
        INNER JOIN work_order wo ON wo.work_order_id = qi.work_order_id
        INNER JOIN production_plan p ON p.plan_id = wo.plan_id
        WHERE p.source_order_id = #{orderId}
        ORDER BY qi.inspected_at DESC
        LIMIT 1
        """)
    Map<String, Object> getLatestInspectionByOrder(Long orderId);

    @Select("""
        SELECT it.transaction_id AS transactionId, it.transaction_no AS transactionNo,
               it.quantity, it.handled_at AS handledAt, it.remark
        FROM inventory_transaction it
        INNER JOIN work_order wo ON wo.work_order_id = it.related_work_order_id
        INNER JOIN production_plan p ON p.plan_id = wo.plan_id
        WHERE p.source_order_id = #{orderId} AND it.transaction_type = 'PRODUCT_IN'
        ORDER BY it.handled_at DESC
        LIMIT 1
        """)
    Map<String, Object> getLatestInboundByOrder(Long orderId);

    @Select("""
        SELECT delivery_id AS deliveryId, delivery_no AS deliveryNo, delivery_quantity AS deliveryQuantity,
               delivery_date AS deliveryDate, delivery_status AS deliveryStatus,
               logistics_company AS logisticsCompany, logistics_no AS logisticsNo,
               receiver_name AS receiverName, receiver_phone AS receiverPhone,
               receiver_address AS receiverAddress, created_at AS createdAt
        FROM delivery_order
        WHERE order_id = #{orderId} AND customer_name = #{customerName}
        ORDER BY created_at DESC
        """)
    List<Map<String, Object>> listDeliveriesByOrder(@Param("orderId") Long orderId, @Param("customerName") String customerName);

    @Select("""
        SELECT material_id AS materialId, material_code AS materialCode, material_name AS materialName,
               material_type AS materialType, specification, unit, standard_cost AS standardCost, status
        FROM material
        WHERE material_type = 'FINISHED' AND status = 1
        ORDER BY material_code
        """)
    List<Map<String, Object>> listFinishedProducts();

    @Select("""
        SELECT a.case_no AS caseNo, a.order_id AS orderId, co.order_no AS orderNo,
               a.material_id AS materialId, m.material_name AS materialName,
               a.batch_no AS batchNo, a.problem_type AS problemType,
               a.problem_description AS problemDescription, a.attachment_urls AS attachmentUrls,
               a.case_status AS caseStatus, a.handle_result AS handleResult,
               a.opened_at AS openedAt, a.processing_at AS processingAt,
               a.resolved_at AS resolvedAt, a.closed_at AS closedAt, a.updated_at AS updatedAt
        FROM after_sales_case a
        LEFT JOIN customer_order co ON co.order_id = a.order_id
        LEFT JOIN material m ON m.material_id = a.material_id
        WHERE a.customer_name = #{customerName}
        ORDER BY a.created_at DESC
        """)
    List<Map<String, Object>> listFeedbacksByCustomer(String customerName);

    @Select("""
        SELECT COUNT(*) FROM customer_order
        WHERE customer_name = #{customerName} AND audit_status = 'PENDING'
        """)
    int countPendingOrders(String customerName);

    @Select("""
        SELECT COUNT(*) FROM after_sales_case
        WHERE customer_name = #{customerName} AND case_status IN ('OPEN', 'PROCESSING')
        """)
    int countOpenFeedbacks(String customerName);

    @Select("SELECT MAX(CAST(SUBSTRING(order_no, 9) AS UNSIGNED)) FROM customer_order WHERE order_no LIKE CONCAT(#{prefix}, '%')")
    Integer maxOrderSeqByPrefix(String prefix);

    @Select("SELECT MAX(CAST(SUBSTRING(case_no, 9) AS UNSIGNED)) FROM after_sales_case WHERE case_no LIKE CONCAT(#{prefix}, '%')")
    Integer maxCaseSeqByPrefix(String prefix);
}
