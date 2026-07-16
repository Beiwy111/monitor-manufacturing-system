package com.upc.computer.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface AfterSalesWorkflowMapper {

    @Select("""
        SELECT p.plan_id planId, p.plan_no planNo, p.case_no caseNo, p.plan_type planType,
               p.trace_summary traceSummary, p.plan_detail planDetail, p.owner_name ownerName,
               p.expected_finish_at expectedFinishAt, p.estimated_cost estimatedCost,
               p.parts_json partsJson, p.customer_opinion customerOpinion,
               p.approval_status approvalStatus, p.approved_at approvedAt,
               p.created_at createdAt, p.updated_at updatedAt
        FROM after_sales_plan p
        ORDER BY p.updated_at DESC
        """)
    List<Map<String, Object>> listPlans();

    @Select("""
        SELECT p.plan_id planId, p.plan_no planNo, p.case_no caseNo, p.plan_type planType,
               p.trace_summary traceSummary, p.plan_detail planDetail, p.owner_name ownerName,
               p.expected_finish_at expectedFinishAt, p.estimated_cost estimatedCost,
               p.parts_json partsJson, p.customer_opinion customerOpinion,
               p.approval_status approvalStatus, p.approved_at approvedAt,
               p.created_at createdAt, p.updated_at updatedAt
        FROM after_sales_plan p WHERE p.case_no=#{caseNo}
        ORDER BY p.plan_id DESC LIMIT 1
        """)
    Map<String, Object> latestPlanByCase(String caseNo);

    @Select("SELECT plan_id planId, case_no caseNo, approval_status approvalStatus FROM after_sales_plan WHERE plan_id=#{planId}")
    Map<String, Object> getPlanById(Long planId);

    @Insert("""
        INSERT INTO after_sales_plan
        (plan_no, case_no, plan_type, trace_summary, plan_detail, owner_name,
         expected_finish_at, estimated_cost, parts_json, customer_opinion,
         approval_status, created_at, updated_at)
        VALUES (#{planNo}, #{caseNo}, #{planType}, #{traceSummary}, #{planDetail}, #{ownerName},
         #{expectedFinishAt}, #{estimatedCost}, #{partsJson}, #{customerOpinion},
         #{approvalStatus}, NOW(), NOW())
        """)
    @Options(useGeneratedKeys = true, keyProperty = "planId")
    int insertPlan(Map<String, Object> row);

    @Update("""
        UPDATE after_sales_plan SET plan_type=#{planType}, trace_summary=#{traceSummary},
          plan_detail=#{planDetail}, owner_name=#{ownerName}, expected_finish_at=#{expectedFinishAt},
          estimated_cost=#{estimatedCost}, parts_json=#{partsJson}, customer_opinion=#{customerOpinion},
          approval_status=#{approvalStatus}, approved_at=#{approvedAt}, updated_at=NOW()
        WHERE plan_id=#{planId}
        """)
    int updatePlan(Map<String, Object> row);

    @Select("""
        SELECT t.task_id taskId, t.task_no taskNo, t.case_no caseNo, t.plan_id planId,
               t.task_type taskType, t.title, t.assignee, t.status, t.due_at dueAt,
               t.started_at startedAt, t.completed_at completedAt, t.remark,
               t.created_at createdAt, t.updated_at updatedAt
        FROM after_sales_task t
        ORDER BY t.case_no, t.task_id
        """)
    List<Map<String, Object>> listTasks();

    @Select("""
        SELECT t.task_id taskId, t.task_no taskNo, t.case_no caseNo, t.plan_id planId,
               t.task_type taskType, t.title, t.assignee, t.status, t.due_at dueAt,
               t.started_at startedAt, t.completed_at completedAt, t.remark,
               t.created_at createdAt, t.updated_at updatedAt
        FROM after_sales_task t WHERE t.case_no=#{caseNo}
        ORDER BY t.task_id
        """)
    List<Map<String, Object>> tasksByCase(String caseNo);

    @Insert("""
        INSERT INTO after_sales_task
        (task_no, case_no, plan_id, task_type, title, assignee, status, due_at, created_at, updated_at)
        VALUES (#{taskNo}, #{caseNo}, #{planId}, #{taskType}, #{title}, #{assignee}, #{status}, #{dueAt}, NOW(), NOW())
        """)
    int insertTask(Map<String, Object> row);

    @Select("SELECT task_id taskId, case_no caseNo, status FROM after_sales_task WHERE task_id=#{taskId}")
    Map<String, Object> getTaskById(Long taskId);

    @Update("""
        UPDATE after_sales_task SET status=#{status}, assignee=#{assignee}, remark=#{remark},
          started_at=CASE WHEN #{status}='IN_PROGRESS' AND started_at IS NULL THEN NOW() ELSE started_at END,
          completed_at=CASE WHEN #{status}='DONE' THEN NOW()
            WHEN #{status} IN ('PENDING','IN_PROGRESS') THEN NULL ELSE completed_at END,
          updated_at=NOW()
        WHERE task_id=#{taskId}
        """)
    int updateTask(Map<String, Object> row);

    @Select("""
        SELECT closure_id closureId, case_no caseNo, recheck_result recheckResult,
               recheck_passed recheckPassed, customer_confirmed customerConfirmed,
               satisfaction_score satisfactionScore, actual_cost actualCost,
               root_cause rootCause, responsibility, improvement_measures improvementMeasures,
               closed_remark closedRemark, created_at createdAt, updated_at updatedAt
        FROM after_sales_closure WHERE case_no=#{caseNo}
        """)
    Map<String, Object> getClosure(String caseNo);

    @Select("""
        SELECT c.closure_id closureId, c.case_no caseNo, c.recheck_passed recheckPassed,
               c.customer_confirmed customerConfirmed, c.satisfaction_score satisfactionScore,
               c.actual_cost actualCost, c.root_cause rootCause, c.responsibility,
               a.customer_name customerName, a.case_status caseStatus
        FROM after_sales_closure c
        JOIN after_sales_case a ON a.case_no = c.case_no
        ORDER BY c.updated_at DESC
        """)
    List<Map<String, Object>> listClosures();

    @Insert("""
        INSERT INTO after_sales_closure
        (case_no, recheck_result, recheck_passed, customer_confirmed, satisfaction_score,
         actual_cost, root_cause, responsibility, improvement_measures, closed_remark, created_at, updated_at)
        VALUES (#{caseNo}, #{recheckResult}, #{recheckPassed}, #{customerConfirmed}, #{satisfactionScore},
         #{actualCost}, #{rootCause}, #{responsibility}, #{improvementMeasures}, #{closedRemark}, NOW(), NOW())
        """)
    int insertClosure(Map<String, Object> row);

    @Update("""
        UPDATE after_sales_closure SET recheck_result=#{recheckResult}, recheck_passed=#{recheckPassed},
          customer_confirmed=#{customerConfirmed}, satisfaction_score=#{satisfactionScore},
          actual_cost=#{actualCost}, root_cause=#{rootCause}, responsibility=#{responsibility},
          improvement_measures=#{improvementMeasures}, closed_remark=#{closedRemark}, updated_at=NOW()
        WHERE case_no=#{caseNo}
        """)
    int updateClosure(Map<String, Object> row);

    @Select("""
        SELECT COUNT(*) total,
               SUM(status='DONE') done,
               SUM(status IN ('PENDING','IN_PROGRESS')) pending
        FROM after_sales_task WHERE case_no=#{caseNo}
        """)
    Map<String, Object> taskStats(String caseNo);
}
