package com.upc.computer.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface AfterSalesRcaMapper {

    @Select("""
        SELECT a.case_no caseNo, a.problem_description problemDescription, a.problem_type problemType,
               a.case_level caseLevel, a.customer_name customerName, a.batch_no batchNo,
               a.order_id orderId, co.order_no orderNo, a.material_id materialId,
               a.delivery_id deliveryId, a.opened_at openedAt,
               m.material_code materialCode, m.material_name materialName, m.standard_cost standardCost,
               qi.inspection_id inspectionId, qi.inspection_no inspectionNo, qi.work_order_id workOrderId,
               qi.work_report_id workReportId, qi.inspection_status inspectionStatus,
               qi.inspection_type inspectionType, qi.inspection_result inspectionResult,
               qi.inspected_at inspectedAt, insp.real_name inspectorName,
               qi.sample_quantity sampleQuantity, qi.qualified_quantity qualifiedQuantity,
               qi.unqualified_quantity unqualifiedQuantity, qi.remark inspectionRemark,
               wo.work_order_no workOrderNo, wo.planned_quantity plannedQuantity,
               wo.completed_quantity completedQuantity, wo.qualified_quantity workQualifiedQuantity,
               wo.unqualified_quantity workUnqualifiedQuantity, wo.actual_start_time workStartTime,
               wo.actual_end_time workEndTime
        FROM after_sales_case a
        LEFT JOIN customer_order co ON co.order_id=a.order_id
        LEFT JOIN material m ON m.material_id=a.material_id
        LEFT JOIN quality_inspection qi ON qi.inspection_id=a.quality_inspection_id
        LEFT JOIN user insp ON insp.user_id=qi.inspector_id
        LEFT JOIN work_order wo ON wo.work_order_id=qi.work_order_id
        WHERE a.case_no=#{caseNo}
        """)
    Map<String, Object> context(String caseNo);

    @Select("""
        SELECT wr.report_id reportId, wr.report_no reportNo, wr.step_id stepId,
               ps.step_name stepName, wr.operator_id operatorId, u.real_name operatorName,
               wr.equipment_id equipmentId, e.equipment_code equipmentCode,
               e.equipment_name equipmentName, e.equipment_type equipmentType,
               e.status equipmentStatus, e.last_maintenance_at lastMaintenanceAt,
               wr.start_time startTime, wr.end_time endTime, wr.completed_quantity completedQuantity,
               wr.qualified_quantity qualifiedQuantity, wr.unqualified_quantity unqualifiedQuantity,
               wr.work_hours workHours, wr.remark remark
        FROM work_report wr
        LEFT JOIN equipment e ON e.equipment_id=wr.equipment_id
        LEFT JOIN process_step ps ON ps.step_id=wr.step_id
        LEFT JOIN user u ON u.user_id=wr.operator_id
        WHERE wr.work_order_id=#{workOrderId} ORDER BY wr.start_time,wr.report_id
        """)
    List<Map<String, Object>> reports(Long workOrderId);

    @Select("""
        SELECT aa.alarm_id alarmId,aa.alarm_no alarmNo,aa.work_order_id workOrderId,
               aa.equipment_id equipmentId,e.equipment_code equipmentCode,e.equipment_name equipmentName,
               aa.alarm_type alarmType,aa.alarm_level alarmLevel,aa.alarm_description alarmDescription,
               aa.alarm_status alarmStatus,aa.reported_at reportedAt,aa.closed_at closedAt,aa.close_result closeResult
        FROM andon_alarm aa LEFT JOIN equipment e ON e.equipment_id=aa.equipment_id
        WHERE aa.work_order_id=#{workOrderId}
           OR (aa.equipment_id IN (SELECT equipment_id FROM work_report WHERE work_order_id=#{workOrderId})
               AND aa.reported_at BETWEEN DATE_SUB(#{startTime},INTERVAL 1 DAY) AND DATE_ADD(#{endTime},INTERVAL 1 DAY))
        ORDER BY aa.reported_at
        """)
    List<Map<String, Object>> alarms(@Param("workOrderId") Long workOrderId,
                                     @Param("startTime") Object startTime,
                                     @Param("endTime") Object endTime);

    @Select("""
        SELECT qii.inspection_item_id itemId,qii.item_code itemCode,qii.item_name itemName,
               qii.standard_value standardValue,qii.measured_value measuredValue,qii.unit unit,
               qii.result result,qii.defect_level defectLevel,qii.remark remark
        FROM quality_inspection_item qii WHERE qii.inspection_id=#{inspectionId}
        ORDER BY qii.sort_order,qii.inspection_item_id
        """)
    List<Map<String, Object>> inspectionItems(Long inspectionId);

    @Select("""
        SELECT out_tx.transaction_id consumeTransactionId,out_tx.material_id materialId,
               m.material_code materialCode,m.material_name materialName,m.standard_cost standardCost,
               out_tx.quantity consumeQuantity,out_tx.batch_no materialBatchNo,out_tx.handled_at consumedAt,
               in_tx.transaction_id inboundTransactionId,in_tx.related_purchase_order_id purchaseOrderId,
               po.purchase_order_no purchaseOrderNo,po.supplier_name supplierName,po.status purchaseStatus,
               po.purchase_date purchaseDate,po.supplier_contact supplierContact,po.supplier_phone supplierPhone,
               pu.real_name purchaserName,
               poi.unit_price unitPrice,poi.quantity purchaseQuantity,poi.received_quantity receivedQuantity,
               s.supplier_id supplierId,s.supplier_no supplierNo,s.supply_materials supplyMaterials
        FROM inventory_transaction out_tx
        JOIN material m ON m.material_id=out_tx.material_id
        LEFT JOIN inventory_transaction in_tx ON in_tx.material_id=out_tx.material_id
             AND in_tx.batch_no=out_tx.batch_no AND in_tx.transaction_type='PURCHASE_IN'
        LEFT JOIN purchase_order po ON po.purchase_order_id=in_tx.related_purchase_order_id
        LEFT JOIN user pu ON pu.user_id=po.purchaser_id
        LEFT JOIN purchase_order_item poi ON poi.purchase_order_id=po.purchase_order_id
             AND poi.material_id=out_tx.material_id
        LEFT JOIN supplier s ON s.supplier_name=po.supplier_name
             OR po.supplier_name LIKE CONCAT('%',REPLACE(REPLACE(s.supplier_name,'（LCD供应商）',''),'（芯片供应商）',''),'%')
        WHERE out_tx.related_work_order_id=#{workOrderId} AND out_tx.transaction_type='MATERIAL_OUT'
        ORDER BY out_tx.transaction_id
        """)
    List<Map<String, Object>> consumedMaterials(Long workOrderId);

    /** 追溯链 · 发货记录：优先按案例绑定的发货单，未绑定时回退取该订单最近一张 */
    @Select("""
        SELECT d.delivery_no deliveryNo, d.delivery_date deliveryDate, d.delivery_quantity deliveryQuantity,
               d.batch_no batchNo, d.logistics_company logisticsCompany, d.logistics_no logisticsNo,
               d.receiver_name receiverName, d.delivery_status deliveryStatus, u.real_name shippedByName
        FROM delivery_order d
        LEFT JOIN user u ON u.user_id=d.shipped_by
        WHERE (#{deliveryId} IS NOT NULL AND d.delivery_id=#{deliveryId})
           OR (#{deliveryId} IS NULL AND #{orderId} IS NOT NULL AND d.order_id=#{orderId})
        ORDER BY d.delivery_date DESC, d.delivery_id DESC LIMIT 1
        """)
    Map<String, Object> deliveryForCase(@Param("deliveryId") Long deliveryId, @Param("orderId") Long orderId);

    /** 追溯链 · 成品入库：该工单的 PRODUCT_IN 库存事务（含入库仓库员） */
    @Select("""
        SELECT t.transaction_no transactionNo, t.quantity quantity, t.batch_no batchNo,
               t.warehouse_code warehouseCode, t.location_code locationCode,
               t.handled_at handledAt, u.real_name handledByName
        FROM inventory_transaction t
        LEFT JOIN user u ON u.user_id=t.handled_by
        WHERE t.related_work_order_id=#{workOrderId} AND t.transaction_type='PRODUCT_IN'
        ORDER BY t.handled_at, t.transaction_id LIMIT 1
        """)
    Map<String, Object> productInbound(Long workOrderId);

    /** 追溯链 · 物料质检：该工单所领物料的来料检验记录（含物料质检员） */
    @Select("""
        SELECT qi.inspection_no inspectionNo, qi.material_id materialId, m.material_name materialName,
               qi.batch_no batchNo, qi.inspection_result inspectionResult,
               qi.sample_quantity sampleQuantity, qi.unqualified_quantity unqualifiedQuantity,
               qi.inspected_at inspectedAt, u.real_name inspectorName
        FROM quality_inspection qi
        LEFT JOIN material m ON m.material_id=qi.material_id
        LEFT JOIN user u ON u.user_id=qi.inspector_id
        WHERE qi.inspection_category='RAW_MATERIAL'
          AND qi.material_id IN (SELECT material_id FROM inventory_transaction
                                 WHERE related_work_order_id=#{workOrderId} AND transaction_type='MATERIAL_OUT')
        ORDER BY qi.inspected_at, qi.inspection_id
        """)
    List<Map<String, Object>> materialInspections(Long workOrderId);

    @Select("""
        SELECT cs.settlement_id settlementId,cs.settlement_no settlementNo,cs.material_cost materialCost,
               cs.labor_cost laborCost,cs.equipment_cost equipmentCost,cs.quality_cost qualityCost,
               cs.other_cost otherCost,cs.total_cost totalCost,cs.settlement_status settlementStatus
        FROM cost_settlement cs WHERE cs.work_order_id=#{workOrderId}
        ORDER BY cs.updated_at DESC LIMIT 1
        """)
    Map<String, Object> settlement(Long workOrderId);

    @Select("""
        SELECT COUNT(*) totalCases,
               SUM(CASE WHEN batch_no=#{batchNo} THEN 1 ELSE 0 END) sameBatchCases,
               SUM(CASE WHEN material_id=#{materialId} THEN 1 ELSE 0 END) sameMaterialCases,
               SUM(CASE WHEN batch_no=#{batchNo} AND problem_type=#{problemType} THEN 1 ELSE 0 END) sameProblemBatchCases
        FROM after_sales_case
        """)
    Map<String, Object> caseStats(@Param("materialId") Long materialId,@Param("batchNo") String batchNo,
                                  @Param("problemType") String problemType);

    @Insert("""
        INSERT INTO after_sales_rca_analysis
        (analysis_no,case_no,algorithm_version,model_type,top_cause,top_department,top_score,
         data_completeness,estimated_loss,conclusion,snapshot_json,created_at)
        VALUES(#{analysisNo},#{caseNo},#{algorithmVersion},#{modelType},#{topCause},#{topDepartment},
         #{topScore},#{dataCompleteness},#{estimatedLoss},#{conclusion},#{snapshotJson},NOW())
        """)
    int insertAnalysis(Map<String,Object> row);

    @Select("""
        SELECT snapshot_json snapshotJson FROM after_sales_rca_analysis
        WHERE case_no=#{caseNo} ORDER BY analysis_id DESC LIMIT 1
        """)
    Map<String,Object> latestAnalysis(String caseNo);

    @Insert("""
        INSERT IGNORE INTO after_sales_collaboration_task
        (task_no,case_no,analysis_no,department_code,title,content,priority,status,created_at)
        VALUES(#{taskNo},#{caseNo},#{analysisNo},#{department},#{title},#{content},#{priority},'PENDING',NOW())
        """)
    int insertTask(Map<String,Object> row);

    @Insert("""
        INSERT INTO sys_notification
        (receiver_role,title,content,level,business_type,business_id,target_path,read_status,created_at)
        VALUES(#{department},#{title},#{content},#{priority},'AFTER_SALES_RCA',#{caseNo},#{targetPath},0,NOW())
        """)
    int insertNotification(Map<String,Object> row);

    @Select("""
        SELECT task_id taskId,task_no taskNo,case_no caseNo,analysis_no analysisNo,
               department_code department,title,content,priority,status,result,created_at createdAt,completed_at completedAt
        FROM after_sales_collaboration_task WHERE department_code=#{department}
        ORDER BY status='PENDING' DESC,created_at DESC
        """)
    List<Map<String,Object>> tasksByDepartment(String department);

    @Update("""
        UPDATE after_sales_rca_analysis SET confirmed_cause=#{cause},confirmed_department=#{department},
        confirm_remark=#{remark},confirmed_at=NOW() WHERE analysis_no=#{analysisNo}
        """)
    int confirmRootCause(Map<String,Object> row);

    @Update("""
        UPDATE after_sales_collaboration_task SET status=#{status},result=#{result},
        completed_at=CASE WHEN #{status}='COMPLETED' THEN NOW() ELSE completed_at END
        WHERE task_id=#{taskId}
        """)
    int updateTask(Map<String,Object> row);

    @Select("""
        SELECT COUNT(*) total,
               SUM(status='PENDING') pending,
               SUM(status IN ('ACCEPTED','PROCESSING')) processing,
               SUM(status='COMPLETED') completed
        FROM after_sales_collaboration_task WHERE case_no=#{caseNo}
        """)
    Map<String,Object> taskProgress(String caseNo);

    @Select("SELECT snapshot_json snapshotJson FROM after_sales_triage WHERE case_no=#{caseNo} ORDER BY triage_id DESC LIMIT 1")
    Map<String,Object> latestTriage(String caseNo);

    @Insert("""
        INSERT INTO after_sales_triage
        (triage_no,case_no,category,category_name,escalation_score,risk_level,need_rca,model_type,
         reason_json,action_json,summary,snapshot_json,created_at)
        VALUES(#{triageNo},#{caseNo},#{category},#{categoryName},#{escalationScore},#{riskLevel},#{needRca},
         #{modelType},#{reasonJson},#{actionJson},#{summary},#{snapshotJson},NOW())
        """)
    int insertTriage(Map<String,Object> row);
}
