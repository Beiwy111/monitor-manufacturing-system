package com.upc.computer.service;

import java.util.List;
import java.util.Map;

public interface AfterSalesWorkflowService {

    List<Map<String, Object>> listPlans();

    Map<String, Object> getPlanByCase(String caseNo);

    Map<String, Object> savePlan(Map<String, Object> body);

    Map<String, Object> submitPlan(Long planId);

    Map<String, Object> approvePlan(Long planId, String operator);

    Map<String, Object> rejectPlan(Long planId, String remark);

    List<Map<String, Object>> listTasks();

    List<Map<String, Object>> listTasksByCase(String caseNo);

    Map<String, Object> updateTask(Map<String, Object> body);

    Map<String, Object> advanceCase(String caseNo, String targetStatus);

    Map<String, Object> getClosure(String caseNo);

    List<Map<String, Object>> listClosures();

    Map<String, Object> saveClosure(Map<String, Object> body);

    Map<String, Object> confirmCustomer(String caseNo);

    Map<String, Object> closeWithClosure(String caseNo, String operator);
}
