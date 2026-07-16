package com.upc.computer.service.impl;

import com.upc.computer.common.BusinessException;
import com.upc.computer.entity.AfterSalesCase;
import com.upc.computer.mapper.AfterSalesCaseMapper;
import com.upc.computer.mapper.AfterSalesRcaMapper;
import com.upc.computer.mapper.AfterSalesWorkflowMapper;
import com.upc.computer.service.AfterSalesWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AfterSalesWorkflowServiceImpl implements AfterSalesWorkflowService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AfterSalesWorkflowMapper workflowMapper;
    @Autowired
    private AfterSalesCaseMapper caseMapper;
    @Autowired
    private AfterSalesRcaMapper rcaMapper;

    @Override
    public List<Map<String, Object>> listPlans() {
        List<Map<String, Object>> list = workflowMapper.listPlans();
        list.forEach(this::enrichPlan);
        return list;
    }

    @Override
    public Map<String, Object> getPlanByCase(String caseNo) {
        Map<String, Object> plan = workflowMapper.latestPlanByCase(caseNo);
        if (plan == null) return Map.of();
        enrichPlan(plan);
        return plan;
    }

    @Override
    @Transactional
    public Map<String, Object> savePlan(Map<String, Object> body) {
        String caseNo = str(body, "caseNo");
        AfterSalesCase c = requireCase(caseNo);
        if (!List.of("ACCEPTED", "PENDING_PLAN", "PENDING_APPROVAL").contains(c.getCaseStatus())
                && !"PROCESSING".equals(c.getCaseStatus())) {
            throw new BusinessException("当前状态不可编制方案：" + statusCn(c.getCaseStatus()));
        }
        Map<String, Object> existing = workflowMapper.latestPlanByCase(caseNo);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("caseNo", caseNo);
        row.put("planType", str(body, "planType"));
        row.put("traceSummary", str(body, "traceSummary"));
        row.put("planDetail", str(body, "planDetail"));
        row.put("ownerName", str(body, "ownerName"));
        row.put("expectedFinishAt", normalizeDateTime(body.get("expectedFinishAt")));
        row.put("estimatedCost", body.get("estimatedCost"));
        row.put("partsJson", str(body, "partsJson"));
        row.put("customerOpinion", str(body, "customerOpinion"));
        row.put("approvalStatus", "DRAFT");
        row.put("approvedAt", null);

        if (str(row, "traceSummary").isBlank()) {
            String fromRca = buildTraceSummaryFromRca(caseNo);
            if (!fromRca.isBlank()) {
                row.put("traceSummary", fromRca);
            }
        }

        if (existing == null || "APPROVED".equals(str(existing, "approvalStatus"))) {
            row.put("planNo", "PLAN-" + caseNo + "-" + System.currentTimeMillis());
            workflowMapper.insertPlan(row);
        } else {
            row.put("planId", existing.get("planId"));
            String st = str(existing, "approvalStatus");
            row.put("approvalStatus", "REJECTED".equals(st) ? "DRAFT" : st);
            workflowMapper.updatePlan(row);
        }

        c.setCaseStatus("PENDING_PLAN");
        c.setUpdatedAt(LocalDateTime.now());
        caseMapper.updateAfterSalesCase(c);
        return getPlanByCase(caseNo);
    }

    @Override
    @Transactional
    public Map<String, Object> submitPlan(Long planId) {
        Map<String, Object> plan = workflowMapper.getPlanById(planId);
        if (plan == null) throw new BusinessException("方案不存在");
        String caseNo = str(plan, "caseNo");
        Map<String, Object> full = workflowMapper.latestPlanByCase(caseNo);
        if (str(full, "planType").isBlank()) throw new BusinessException("请选择方案类型");
        if (str(full, "ownerName").isBlank()) throw new BusinessException("请填写负责人");
        full.put("approvalStatus", "PENDING");
        workflowMapper.updatePlan(full);
        AfterSalesCase c = requireCase(caseNo);
        c.setCaseStatus("PENDING_APPROVAL");
        c.setUpdatedAt(LocalDateTime.now());
        caseMapper.updateAfterSalesCase(c);
        enrichPlan(full);
        return full;
    }

    @Override
    @Transactional
    public Map<String, Object> approvePlan(Long planId, String operator) {
        Map<String, Object> plan = workflowMapper.getPlanById(planId);
        if (plan == null) throw new BusinessException("方案不存在");
        String caseNo = str(plan, "caseNo");
        Map<String, Object> full = workflowMapper.latestPlanByCase(caseNo);
        if (!"PENDING".equals(str(full, "approvalStatus"))) {
            throw new BusinessException("仅待审批方案可通过");
        }
        full.put("approvalStatus", "APPROVED");
        full.put("approvedAt", LocalDateTime.now());
        workflowMapper.updatePlan(full);
        generateTasks(caseNo, longVal(full.get("planId")), str(full, "planType"), str(full, "ownerName"));
        AfterSalesCase c = requireCase(caseNo);
        c.setCaseStatus("EXECUTING");
        c.setUpdatedAt(LocalDateTime.now());
        caseMapper.updateAfterSalesCase(c);
        enrichPlan(full);
        return full;
    }

    @Override
    @Transactional
    public Map<String, Object> rejectPlan(Long planId, String remark) {
        Map<String, Object> plan = workflowMapper.getPlanById(planId);
        if (plan == null) throw new BusinessException("方案不存在");
        String caseNo = str(plan, "caseNo");
        Map<String, Object> full = workflowMapper.latestPlanByCase(caseNo);
        full.put("approvalStatus", "REJECTED");
        if (remark != null && !remark.isBlank()) {
            full.put("customerOpinion", str(full, "customerOpinion") + " | 驳回：" + remark);
        }
        workflowMapper.updatePlan(full);
        AfterSalesCase c = requireCase(caseNo);
        c.setCaseStatus("PENDING_PLAN");
        c.setUpdatedAt(LocalDateTime.now());
        caseMapper.updateAfterSalesCase(c);
        enrichPlan(full);
        return full;
    }

    @Override
    public List<Map<String, Object>> listTasks() {
        List<Map<String, Object>> list = workflowMapper.listTasks();
        list.forEach(this::enrichTask);
        return list;
    }

    @Override
    public List<Map<String, Object>> listTasksByCase(String caseNo) {
        List<Map<String, Object>> list = workflowMapper.tasksByCase(caseNo);
        list.forEach(this::enrichTask);
        return list;
    }

    @Override
    @Transactional
    public Map<String, Object> updateTask(Map<String, Object> body) {
        Long taskId = longVal(body.get("taskId"));
        if (taskId == null) throw new BusinessException("taskId 不能为空");
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("taskId", taskId);
        row.put("status", str(body, "status"));
        row.put("assignee", str(body, "assignee"));
        row.put("remark", str(body, "remark"));
        if (workflowMapper.updateTask(row) == 0) throw new BusinessException("任务不存在");
        Map<String, Object> task = workflowMapper.getTaskById(taskId);
        String caseNo = str(body, "caseNo");
        if (caseNo.isBlank() && task != null) caseNo = str(task, "caseNo");
        Map<String, Object> stats = workflowMapper.taskStats(caseNo);
        if (stats != null && longVal(stats.get("total")) > 0 && longVal(stats.get("pending")) == 0) {
            AfterSalesCase c = requireCase(caseNo);
            if ("EXECUTING".equals(c.getCaseStatus())) {
                c.setCaseStatus("PENDING_RECHECK");
                c.setUpdatedAt(LocalDateTime.now());
                caseMapper.updateAfterSalesCase(c);
            }
        }
        return row;
    }

    @Override
    @Transactional
    public Map<String, Object> advanceCase(String caseNo, String targetStatus) {
        AfterSalesCase c = requireCase(caseNo);
        String from = c.getCaseStatus();
        boolean ok = switch (targetStatus) {
            case "PENDING_RECHECK" -> "EXECUTING".equals(from);
            case "PENDING_CONFIRM" -> "PENDING_RECHECK".equals(from);
            case "RESOLVED" -> "PENDING_CONFIRM".equals(from);
            default -> false;
        };
        if (!ok) throw new BusinessException("不允许从 " + statusCn(from) + " 推进到 " + statusCn(targetStatus));
        c.setCaseStatus(targetStatus);
        if ("RESOLVED".equals(targetStatus)) c.setResolvedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        caseMapper.updateAfterSalesCase(c);
        return Map.of("caseNo", caseNo, "caseStatus", targetStatus);
    }

    @Override
    public Map<String, Object> getClosure(String caseNo) {
        Map<String, Object> row = workflowMapper.getClosure(caseNo);
        if (row == null) return Map.of("caseNo", caseNo);
        enrichClosure(row);
        return row;
    }

    @Override
    public List<Map<String, Object>> listClosures() {
        List<Map<String, Object>> list = workflowMapper.listClosures();
        list.forEach(row -> {
            row.put("caseStatusCn", statusCn(str(row, "caseStatus")));
            fmt(row, "updatedAt");
        });
        return list;
    }

    @Override
    @Transactional
    public Map<String, Object> saveClosure(Map<String, Object> body) {
        String caseNo = str(body, "caseNo");
        requireCase(caseNo);
        Map<String, Object> row = closureRow(body);
        Map<String, Object> existing = workflowMapper.getClosure(caseNo);
        if (existing == null) {
            workflowMapper.insertClosure(row);
        } else {
            workflowMapper.updateClosure(row);
        }
        if (bool(body.get("recheckPassed"))) {
            AfterSalesCase c = requireCase(caseNo);
            if ("EXECUTING".equals(c.getCaseStatus()) || "PENDING_RECHECK".equals(c.getCaseStatus())) {
                c.setCaseStatus("PENDING_CONFIRM");
                c.setUpdatedAt(LocalDateTime.now());
                caseMapper.updateAfterSalesCase(c);
            }
        }
        return getClosure(caseNo);
    }

    @Override
    @Transactional
    public Map<String, Object> confirmCustomer(String caseNo) {
        AfterSalesCase c = requireCase(caseNo);
        if (!"PENDING_CONFIRM".equals(c.getCaseStatus()) && !"PENDING_RECHECK".equals(c.getCaseStatus())) {
            throw new BusinessException("当前状态不可客户确认");
        }
        Map<String, Object> closure = workflowMapper.getClosure(caseNo);
        if (closure == null) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("caseNo", caseNo);
            row.put("customerConfirmed", 1);
            workflowMapper.insertClosure(row);
        } else {
            closure.put("customerConfirmed", 1);
            workflowMapper.updateClosure(closure);
        }
        c.setCaseStatus("RESOLVED");
        c.setResolvedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        caseMapper.updateAfterSalesCase(c);
        return Map.of("caseNo", caseNo, "caseStatus", "RESOLVED");
    }

    @Override
    @Transactional
    public Map<String, Object> closeWithClosure(String caseNo, String operator) {
        AfterSalesCase c = requireCase(caseNo);
        if ("CLOSED".equals(c.getCaseStatus())) throw new BusinessException("案例已关闭");
        Map<String, Object> closure = workflowMapper.getClosure(caseNo);
        if (closure == null) throw new BusinessException("请先填写验证闭环信息");
        if (!bool(closure.get("recheckPassed"))) throw new BusinessException("复检未通过，不可关闭");
        if (!bool(closure.get("customerConfirmed"))) throw new BusinessException("客户未确认，不可关闭");
        if (str(closure, "rootCause").isBlank()) throw new BusinessException("请填写根本原因");
        if (str(closure, "responsibility").isBlank()) throw new BusinessException("请填写责任归属");
        if (str(closure, "improvementMeasures").isBlank()) throw new BusinessException("请填写质量改进措施");
        c.setCaseStatus("CLOSED");
        c.setClosedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        caseMapper.updateAfterSalesCase(c);
        return Map.of("caseNo", caseNo, "caseStatus", "CLOSED");
    }

    private void generateTasks(String caseNo, Long planId, String planType, String owner) {
        List<String[]> defs = taskDefs(planType);
        LocalDateTime due = LocalDateTime.now().plusDays(3);
        int seq = 1;
        for (String[] def : defs) {
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("taskNo", "AST-" + caseNo + "-" + seq++);
            task.put("caseNo", caseNo);
            task.put("planId", planId);
            task.put("taskType", def[0]);
            task.put("title", def[1]);
            task.put("assignee", owner);
            task.put("status", "PENDING");
            task.put("dueAt", due);
            workflowMapper.insertTask(task);
        }
    }

    private List<String[]> taskDefs(String planType) {
        return switch (planType) {
            case "REMOTE_GUIDE" -> List.of(new String[][]{ arr("REMOTE_GUIDE", "远程指导与客户确认") });
            case "RETURN_INSPECTION" -> List.of(new String[][]{
                    arr("RETURN_LOGISTICS", "返厂物流安排"),
                    arr("INBOUND_INSPECTION", "入库检测"),
                    arr("RECHECK", "售后复检") });
            case "REPAIR" -> List.of(new String[][]{
                    arr("RETURN_LOGISTICS", "返厂物流"),
                    arr("REPAIR_ORDER", "维修工单"),
                    arr("RECHECK", "售后复检") });
            case "EXCHANGE" -> List.of(new String[][]{
                    arr("RETURN_LOGISTICS", "返厂收货"),
                    arr("EXCHANGE_SHIPMENT", "换货发货"),
                    arr("RECHECK", "售后复检") });
            case "RETURN" -> List.of(new String[][]{
                    arr("RETURN_LOGISTICS", "退货物流"),
                    arr("REFUND_PROCESS", "退款处理") });
            case "PARTS_RESUPPLY" -> List.of(new String[][]{
                    arr("PARTS_OUTBOUND", "配件出库"),
                    arr("RECHECK", "售后复检") });
            case "SUPPLIER_CLAIM" -> List.of(new String[][]{ arr("SUPPLIER_CLAIM", "供应商索赔跟进") });
            case "BATCH_RECALL" -> List.of(new String[][]{
                    arr("BATCH_RECALL", "批次召回通知"),
                    arr("RECHECK", "召回复检") });
            default -> List.of(new String[][]{ arr("EXECUTION", "售后执行任务") });
        };
    }

    private String[] arr(String type, String title) {
        return new String[]{type, title};
    }

    private Map<String, Object> closureRow(Map<String, Object> body) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("caseNo", str(body, "caseNo"));
        row.put("recheckResult", str(body, "recheckResult"));
        row.put("recheckPassed", bool(body.get("recheckPassed")) ? 1 : 0);
        row.put("customerConfirmed", bool(body.get("customerConfirmed")) ? 1 : 0);
        row.put("satisfactionScore", body.get("satisfactionScore"));
        row.put("actualCost", body.get("actualCost"));
        row.put("rootCause", str(body, "rootCause"));
        row.put("responsibility", str(body, "responsibility"));
        row.put("improvementMeasures", str(body, "improvementMeasures"));
        row.put("closedRemark", str(body, "closedRemark"));
        return row;
    }

    private AfterSalesCase requireCase(String caseNo) {
        if (caseNo == null || caseNo.isBlank()) throw new BusinessException("案例编号不能为空");
        AfterSalesCase c = caseMapper.getAfterSalesCaseById(caseNo);
        if (c == null) throw new BusinessException("售后案例不存在：" + caseNo);
        return c;
    }

    private void enrichPlan(Map<String, Object> row) {
        row.put("planTypeCn", planTypeCn(str(row, "planType")));
        row.put("approvalStatusCn", approvalCn(str(row, "approvalStatus")));
        fmt(row, "expectedFinishAt");
        fmt(row, "approvedAt");
        fmt(row, "createdAt");
        fmt(row, "updatedAt");
    }

    private void enrichTask(Map<String, Object> row) {
        row.put("taskTypeCn", taskTypeCn(str(row, "taskType")));
        row.put("statusCn", taskStatusCn(str(row, "status")));
        fmt(row, "dueAt");
        fmt(row, "startedAt");
        fmt(row, "completedAt");
        fmt(row, "createdAt");
    }

    private void enrichClosure(Map<String, Object> row) {
        fmt(row, "createdAt");
        fmt(row, "updatedAt");
    }

    private void fmt(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v instanceof LocalDateTime ldt) row.put(key, ldt.format(FMT));
    }

    private Object normalizeDateTime(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private String buildTraceSummaryFromRca(String caseNo) {
        Map<String, Object> stored = rcaMapper.latestAnalysis(caseNo);
        if (stored == null) return "";
        StringBuilder sb = new StringBuilder();
        String conclusion = str(stored, "conclusion");
        if (!conclusion.isBlank()) {
            sb.append("追溯结论：").append(conclusion);
        }
        String topCause = str(stored, "topCause");
        if (!topCause.isBlank()) {
            if (!sb.isEmpty()) sb.append('\n');
            sb.append("主要根因：").append(topCause);
            String dept = str(stored, "topDepartment");
            if (!dept.isBlank()) sb.append("（").append(dept).append("）");
            Object score = stored.get("topScore");
            if (score != null) sb.append("，置信度 ").append(score).append('%');
        }
        return sb.toString();
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "" : v.toString();
    }

    private Long longVal(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return 0L; }
    }

    private boolean bool(Object v) {
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        return "1".equals(String.valueOf(v)) || "true".equalsIgnoreCase(String.valueOf(v));
    }

    private String statusCn(String s) {
        if (s == null) return "-";
        return switch (s) {
            case "OPEN" -> "待受理";
            case "ACCEPTED", "PROCESSING" -> "已受理";
            case "PENDING_PLAN" -> "待方案";
            case "PENDING_APPROVAL" -> "待审批";
            case "EXECUTING" -> "执行中";
            case "PENDING_RECHECK" -> "待复检";
            case "PENDING_CONFIRM" -> "待客户确认";
            case "RESOLVED" -> "已解决";
            case "CLOSED" -> "已关闭";
            case "TRACING" -> "追溯中";
            case "CANCELLED" -> "已取消";
            default -> s;
        };
    }

    private String planTypeCn(String t) {
        return switch (t) {
            case "REMOTE_GUIDE" -> "远程指导";
            case "RETURN_INSPECTION" -> "返厂检测";
            case "REPAIR" -> "维修";
            case "EXCHANGE" -> "换货";
            case "RETURN" -> "退货";
            case "PARTS_RESUPPLY" -> "补发配件";
            case "SUPPLIER_CLAIM" -> "供应商索赔";
            case "BATCH_RECALL" -> "批次召回";
            default -> t.isBlank() ? "-" : t;
        };
    }

    private String approvalCn(String s) {
        return switch (s) {
            case "DRAFT" -> "草稿";
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已驳回";
            default -> s;
        };
    }

    private String taskTypeCn(String t) {
        return switch (t) {
            case "RETURN_LOGISTICS" -> "返厂物流";
            case "INBOUND_INSPECTION" -> "入库检测";
            case "REPAIR_ORDER" -> "维修工单";
            case "PARTS_OUTBOUND" -> "配件出库";
            case "EXCHANGE_SHIPMENT" -> "换货发货";
            case "RECHECK" -> "售后复检";
            case "REMOTE_GUIDE" -> "远程指导";
            case "REFUND_PROCESS" -> "退款处理";
            case "SUPPLIER_CLAIM" -> "供应商索赔";
            case "BATCH_RECALL" -> "批次召回";
            default -> t;
        };
    }

    private String taskStatusCn(String s) {
        return switch (s) {
            case "PENDING" -> "待开始";
            case "IN_PROGRESS" -> "进行中";
            case "DONE" -> "已完成";
            case "CANCELLED" -> "已取消";
            default -> s;
        };
    }
}
