package com.upc.computer.mapper;

import com.upc.computer.entity.AfterSalesCase;
import org.apache.ibatis.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mapper
public interface AfterSalesCaseMapper {

    @Select("SELECT case_no,order_id,delivery_id,material_id,batch_no,quality_inspection_id,customer_name,contact_name,contact_phone,problem_description,attachment_urls,problem_type,case_level,case_status,trace_result,handle_result,service_user_id,opened_at,processing_at,sla_deadline,resolved_at,closed_at,created_at,updated_at FROM after_sales_case ORDER BY created_at DESC")
    ArrayList<AfterSalesCase> afterSalesCaseList();

    @Select("SELECT case_no,order_id,delivery_id,material_id,batch_no,quality_inspection_id,customer_name,contact_name,contact_phone,problem_description,attachment_urls,problem_type,case_level,case_status,trace_result,handle_result,service_user_id,opened_at,processing_at,sla_deadline,resolved_at,closed_at,created_at,updated_at FROM after_sales_case WHERE case_no = #{caseNo}")
    AfterSalesCase getAfterSalesCaseById(String caseNo);

    @Insert("INSERT INTO after_sales_case (case_no,order_id,delivery_id,material_id,batch_no,quality_inspection_id,customer_name,contact_name,contact_phone,problem_description,attachment_urls,problem_type,case_level,case_status,trace_result,handle_result,service_user_id,opened_at,processing_at,sla_deadline,resolved_at,closed_at,created_at,updated_at) VALUES (#{caseNo},#{orderId},#{deliveryId},#{materialId},#{batchNo},#{qualityInspectionId},#{customerName},#{contactName},#{contactPhone},#{problemDescription},#{attachmentUrls},#{problemType},#{caseLevel},#{caseStatus},#{traceResult},#{handleResult},#{serviceUserId},#{openedAt},#{processingAt},#{slaDeadline},#{resolvedAt},#{closedAt},#{createdAt},#{updatedAt})")
    void insertAfterSalesCase(AfterSalesCase afterSalesCase);

    @Update("UPDATE after_sales_case SET order_id=#{orderId},delivery_id=#{deliveryId},material_id=#{materialId},batch_no=#{batchNo},quality_inspection_id=#{qualityInspectionId},customer_name=#{customerName},contact_name=#{contactName},contact_phone=#{contactPhone},problem_description=#{problemDescription},attachment_urls=#{attachmentUrls},problem_type=#{problemType},case_level=#{caseLevel},case_status=#{caseStatus},trace_result=#{traceResult},handle_result=#{handleResult},service_user_id=#{serviceUserId},opened_at=#{openedAt},processing_at=#{processingAt},sla_deadline=#{slaDeadline},resolved_at=#{resolvedAt},closed_at=#{closedAt},updated_at=#{updatedAt} WHERE case_no=#{caseNo}")
    void updateAfterSalesCase(AfterSalesCase afterSalesCase);

    @Delete("DELETE FROM after_sales_case WHERE case_no = #{caseNo}")
    void deleteAfterSalesCase(String caseNo);

    /** 列表视图：关联订单编号、物料名称 */
    @Select("""
        SELECT
          a.case_no            AS caseNo,
          a.order_id           AS orderId,
          co.order_no          AS orderNo,
          a.delivery_id        AS deliveryId,
          a.material_id        AS materialId,
          m.material_name      AS materialName,
          a.batch_no           AS batchNo,
          a.quality_inspection_id AS qualityInspectionId,
          qi.inspection_no     AS inspectionNo,
          a.customer_name      AS customerName,
          a.contact_name       AS contactName,
          a.contact_phone      AS contactPhone,
          a.problem_description AS problemDescription,
          a.problem_type       AS problemType,
          a.case_level         AS caseLevel,
          a.case_status        AS caseStatus,
          a.trace_result       AS traceResult,
          a.handle_result      AS handleResult,
          a.service_user_id    AS serviceUserId,
          su.real_name         AS assigneeName,
          a.sla_deadline       AS slaDeadline,
          (SELECT t.risk_level FROM after_sales_triage t WHERE t.case_no=a.case_no ORDER BY t.triage_id DESC LIMIT 1) AS aiTriageLevel,
          (SELECT t.category_name FROM after_sales_triage t WHERE t.case_no=a.case_no ORDER BY t.triage_id DESC LIMIT 1) AS aiTriageCategory,
          a.opened_at          AS openedAt,
          a.processing_at      AS processingAt,
          a.resolved_at        AS resolvedAt,
          a.closed_at          AS closedAt,
          a.updated_at         AS updatedAt
        FROM after_sales_case a
        LEFT JOIN customer_order co ON co.order_id = a.order_id
        LEFT JOIN material m        ON m.material_id = a.material_id
        LEFT JOIN quality_inspection qi ON qi.inspection_id = a.quality_inspection_id
        LEFT JOIN user su ON su.user_id = a.service_user_id
        ORDER BY a.created_at DESC
        """)
    List<Map<String, Object>> listCaseViews();

    /** 追溯详情：关联订单、质检、不良品链路 */
    @Select("""
        SELECT
          a.case_no            AS caseNo,
          a.order_id           AS orderId,
          co.order_no          AS orderNo,
          co.audit_status      AS orderStatus,
          a.material_id        AS materialId,
          m.material_name      AS materialName,
          a.batch_no           AS batchNo,
          a.quality_inspection_id AS qualityInspectionId,
          qi.inspection_no     AS inspectionNo,
          qi.inspection_type   AS inspectionType,
          qi.inspection_category AS inspectionCategory,
          qi.inspection_status AS inspectionStatus,
          qi.inspection_result AS inspectionResult,
          qi.unqualified_quantity AS unqualifiedQty,
          np.nonconforming_no  AS nonconformingNo,
          np.defect_type       AS defectType,
          np.defect_description AS defectDescription,
          np.severity          AS severity,
          np.handle_status     AS ncHandleStatus,
          np.handle_method     AS ncHandleMethod,
          a.problem_type       AS problemType,
          a.problem_description AS problemDescription,
          a.case_level         AS caseLevel,
          a.case_status        AS caseStatus,
          a.trace_result       AS traceResult,
          a.handle_result      AS handleResult,
          a.opened_at          AS openedAt,
          a.processing_at      AS processingAt,
          a.resolved_at        AS resolvedAt,
          a.closed_at          AS closedAt
        FROM after_sales_case a
        LEFT JOIN customer_order co  ON co.order_id = a.order_id
        LEFT JOIN material m         ON m.material_id = a.material_id
        LEFT JOIN quality_inspection qi ON qi.inspection_id = a.quality_inspection_id
        LEFT JOIN nonconforming_product np ON np.inspection_id = a.quality_inspection_id
        WHERE a.case_no = #{caseNo}
        LIMIT 1
        """)
    Map<String, Object> getTraceDetail(String caseNo);

    @Select("""
        SELECT COUNT(*) AS total,
          SUM(CASE WHEN case_status='OPEN' THEN 1 ELSE 0 END) AS open,
          SUM(CASE WHEN case_status IN ('ACCEPTED','PROCESSING') THEN 1 ELSE 0 END) AS accepted,
          SUM(CASE WHEN case_status='PENDING_PLAN' THEN 1 ELSE 0 END) AS pendingPlan,
          SUM(CASE WHEN case_status='PENDING_APPROVAL' THEN 1 ELSE 0 END) AS pendingApproval,
          SUM(CASE WHEN case_status='EXECUTING' THEN 1 ELSE 0 END) AS executing,
          SUM(CASE WHEN case_status='PENDING_RECHECK' THEN 1 ELSE 0 END) AS pendingRecheck,
          SUM(CASE WHEN case_status='PENDING_CONFIRM' THEN 1 ELSE 0 END) AS pendingConfirm,
          SUM(CASE WHEN case_status='RESOLVED' THEN 1 ELSE 0 END) AS resolved,
          SUM(CASE WHEN case_status='CLOSED' THEN 1 ELSE 0 END) AS closed
        FROM after_sales_case
        """)
    Map<String, Object> caseKpi();
}
