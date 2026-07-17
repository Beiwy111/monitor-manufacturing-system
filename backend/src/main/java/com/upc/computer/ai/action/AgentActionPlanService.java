package com.upc.computer.ai.action;

import com.upc.computer.common.BusinessException;
import com.upc.computer.dto.LoginResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class AgentActionPlanService {

    private final AgentActionCatalog catalog;
    private final AgentPlanRepository repository;
    private final AgentActionStateService stateService;
    private final AgentActionExecutionService executionService;
    private final AgentAuditService auditService;

    public AgentActionPlanService(AgentActionCatalog catalog,
                                  AgentPlanRepository repository,
                                  AgentActionStateService stateService,
                                  AgentActionExecutionService executionService,
                                  AgentAuditService auditService) {
        this.catalog = catalog;
        this.repository = repository;
        this.stateService = stateService;
        this.executionService = executionService;
        this.auditService = auditService;
    }

    public List<Map<String, Object>> allowedActions(LoginResponse session) {
        requireSession(session);
        return catalog.allowedActions(session.getRoleCode());
    }

    public AgentPlanOutcome preview(String actionCode, Map<String, Object> supplied,
                                    LoginResponse session, String sessionId) {
        requireSession(session);
        AgentActionDefinition action = catalog.requireAllowed(actionCode, session.getRoleCode());
        Map<String, Object> parameters = repository.findDraft(session.getUserId(), sessionId, action.code());
        if (supplied != null) parameters.putAll(new LinkedHashMap<>(supplied));
        validateNoGenericExecution(parameters);

        List<AgentRequiredField> missing = collectMissing(action, parameters);
        if (!missing.isEmpty()) {
            repository.saveDraft(session.getUserId(), sessionId, action.code(), parameters);
            String labels = String.join("、", missing.stream().map(AgentRequiredField::label).toList());
            return new AgentPlanOutcome("NEEDS_INPUT", "信息不完整，请补充：" + labels, missing, null);
        }

        String planId = "AIP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        List<String> affected = affectedResources(action, parameters);
        String snapshot = stateService.fingerprint(action, parameters, session);
        AgentActionPlan plan = new AgentActionPlan(
                planId, action.code(), action.title(), action.module(), session.getUserId(),
                session.getUsername(), normalizeRole(session.getRoleCode()), sessionId,
                immutableMap(parameters), List.copyOf(affected), snapshot, AgentPlanStatus.PENDING,
                null, null, now, now.plusSeconds(AgentPlanRepository.PLAN_TTL_SECONDS), null);
        repository.save(plan);
        repository.deleteDraft(session.getUserId(), sessionId, action.code());
        auditService.record(session, action, "AI_PLAN_CREATED", planId, parameters, true, null);

        Map<String, Object> confirmation = new LinkedHashMap<>();
        confirmation.put("type", "confirm");
        confirmation.put("proposalId", planId);
        confirmation.put("action", action.code());
        confirmation.put("title", action.title());
        confirmation.put("humanReadable", "准备执行“" + action.title() + "”，请核对参数后确认。");
        confirmation.put("params", redact(parameters));
        confirmation.put("affectedResources", affected);
        confirmation.put("expiresAt", plan.expiresAt());
        return new AgentPlanOutcome("PENDING", "已生成待确认方案，确认前不会修改业务数据。", List.of(), confirmation);
    }

    public synchronized Map<String, Object> confirm(String planId, String decision,
                                                     Object finalParams, LoginResponse session) {
        requireSession(session);
        AgentActionPlan plan = repository.find(planId);
        if (plan == null) throw new BusinessException(404, "待确认方案不存在或已过期");
        assertOwner(plan, session);
        AgentActionDefinition action = catalog.requireAllowed(plan.actionCode(), session.getRoleCode());

        if (plan.status() == AgentPlanStatus.EXECUTED) {
            return executionResult(plan, true, "该方案已经执行，已返回原执行结果");
        }
        if (plan.status() != AgentPlanStatus.PENDING) {
            throw new BusinessException(409, "方案当前状态为 " + plan.status() + "，不能重复执行");
        }
        if (LocalDateTime.now().isAfter(plan.expiresAt())) {
            AgentActionPlan stale = plan.withStatus(AgentPlanStatus.STALE, null, "方案已过期", null);
            repository.save(stale);
            throw new BusinessException(409, "方案已过期，请重新发起操作");
        }

        String normalizedDecision = decision == null ? "" : decision.trim().toUpperCase(Locale.ROOT);
        if ("SKIP".equals(normalizedDecision) || "REJECT".equals(normalizedDecision)) {
            AgentActionPlan rejected = plan.withStatus(AgentPlanStatus.REJECTED, null, null, LocalDateTime.now());
            repository.save(rejected);
            auditService.record(session, action, "AI_PLAN_REJECTED", planId, plan.parameters(), true, null);
            return Map.of("proposalId", planId, "status", "REJECTED", "ok", false,
                    "message", "已取消，未修改任何业务数据", "reply", "已取消，未修改任何业务数据");
        }
        if (!"APPROVE".equals(normalizedDecision) && !"MODIFY".equals(normalizedDecision)) {
            throw new BusinessException(400, "decision 只能是 APPROVE、MODIFY 或 SKIP");
        }

        Map<String, Object> parameters = new LinkedHashMap<>(plan.parameters());
        if ("MODIFY".equals(normalizedDecision)) {
            Map<String, Object> changes = toMap(finalParams);
            rejectIdentityChanges(parameters, changes);
            parameters.putAll(changes);
            ensureComplete(action, parameters);
            plan = new AgentActionPlan(plan.planId(), plan.actionCode(), plan.title(), plan.module(),
                    plan.userId(), plan.username(), plan.roleCode(), plan.sessionId(), immutableMap(parameters),
                    plan.affectedResources(), plan.snapshotHash(), plan.status(), plan.result(), plan.errorMessage(),
                    plan.createdAt(), plan.expiresAt(), plan.executedAt());
        }

        String currentSnapshot = stateService.fingerprint(action, parameters, session);
        if (!Objects.equals(plan.snapshotHash(), currentSnapshot)) {
            AgentActionPlan stale = plan.withStatus(AgentPlanStatus.STALE, null, "业务状态已变化", null);
            repository.save(stale);
            auditService.record(session, action, "AI_PLAN_STALE", planId, parameters, false, "业务状态已变化");
            throw new BusinessException(409, "业务数据在确认前已发生变化，请重新查询并生成方案");
        }

        AgentActionPlan executing = plan.withStatus(AgentPlanStatus.EXECUTING, null, null, null);
        repository.save(executing);
        try {
            Object result = executionService.execute(executing, session);
            AgentActionPlan executed = executing.withStatus(AgentPlanStatus.EXECUTED, result, null, LocalDateTime.now());
            repository.save(executed);
            auditService.record(session, action, "AI_PLAN_EXECUTED", planId, parameters, true, null);
            return executionResult(executed, false, "操作执行成功");
        } catch (RuntimeException ex) {
            AgentActionPlan failed = executing.withStatus(AgentPlanStatus.FAILED, null, safeMessage(ex), LocalDateTime.now());
            repository.save(failed);
            auditService.record(session, action, "AI_PLAN_FAILED", planId, parameters, false, safeMessage(ex));
            throw ex;
        }
    }

    private void requireSession(LoginResponse session) {
        if (session == null || session.getUserId() == null || session.getUsername() == null) {
            throw new BusinessException(401, "登录状态已失效，请重新登录");
        }
    }

    private void assertOwner(AgentActionPlan plan, LoginResponse session) {
        if (!Objects.equals(plan.userId(), session.getUserId())
                || !Objects.equals(plan.username(), session.getUsername())
                || !Objects.equals(plan.roleCode(), normalizeRole(session.getRoleCode()))) {
            throw new BusinessException(403, "只能确认本人以当前角色创建的方案");
        }
    }

    private void ensureComplete(AgentActionDefinition action, Map<String, Object> parameters) {
        List<String> missing = collectMissing(action, parameters).stream().map(AgentRequiredField::label).toList();
        if (!missing.isEmpty()) throw new BusinessException(400, "修改后仍缺少：" + String.join("、", missing));
    }

    private List<AgentRequiredField> collectMissing(AgentActionDefinition action, Map<String, Object> parameters) {
        List<AgentRequiredField> result = new ArrayList<>(action.requiredFields().stream()
                .filter(field -> missing(parameters.get(field.name())))
                .toList());
        if ("customer.profile.update".equals(action.code()) && meaningfulValueCount(parameters) == 0) {
            result.add(new AgentRequiredField("profileField", "至少提供姓名、电话、邮箱或收货地址中的一项"));
        }
        if (requiresChangeField(action) && !hasNonIdentityParameter(parameters)) {
            result.add(new AgentRequiredField("changes", "至少提供一个要修改的字段及新值"));
        }
        return result.stream().distinct().toList();
    }

    private boolean requiresChangeField(AgentActionDefinition action) {
        String code = action.code().toLowerCase(Locale.ROOT);
        return action.title().startsWith("修改") || code.equals("updateplan") || code.equals("updateequipment");
    }

    private boolean hasNonIdentityParameter(Map<String, Object> parameters) {
        return parameters.entrySet().stream().anyMatch(entry -> {
            String key = entry.getKey() == null ? "" : entry.getKey().toLowerCase(Locale.ROOT);
            boolean identity = key.equals("id") || key.endsWith("id") || key.endsWith("no");
            return !identity && !missing(entry.getValue());
        });
    }

    private void validateNoGenericExecution(Map<String, Object> parameters) {
        for (String key : parameters.keySet()) {
            String normalized = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
            if (List.of("sql", "shell", "command", "script", "sourcecode", "executable").contains(normalized)) {
                throw new BusinessException(400, "不允许提供 SQL、Shell、脚本或代码执行参数");
            }
        }
    }

    private void rejectIdentityChanges(Map<String, Object> original, Map<String, Object> changes) {
        for (Map.Entry<String, Object> entry : changes.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey();
            String lower = key.toLowerCase(Locale.ROOT);
            boolean identity = lower.equals("id") || lower.endsWith("id") || lower.endsWith("no");
            if (identity && original.containsKey(key) && !Objects.equals(String.valueOf(original.get(key)), String.valueOf(entry.getValue()))) {
                throw new BusinessException(400, "确认阶段不能修改业务对象标识：" + key);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object value) {
        if (value == null) return Map.of();
        if (!(value instanceof Map<?, ?> raw)) throw new BusinessException(400, "finalParams 必须是对象");
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, val) -> result.put(String.valueOf(key), val));
        validateNoGenericExecution(result);
        return result;
    }

    private List<String> affectedResources(AgentActionDefinition action, Map<String, Object> parameters) {
        List<String> resources = new ArrayList<>();
        resources.add(action.module());
        parameters.forEach((key, value) -> {
            if (value == null) return;
            String lower = key.toLowerCase(Locale.ROOT);
            if (lower.equals("id") || lower.endsWith("id") || lower.endsWith("no") || lower.endsWith("code")) {
                resources.add(key + "=" + value);
            }
        });
        return resources.stream().distinct().limit(12).toList();
    }

    private Map<String, Object> executionResult(AgentActionPlan plan, boolean idempotent, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("proposalId", plan.planId());
        result.put("status", plan.status().name());
        result.put("action", plan.actionCode());
        result.put("message", message);
        result.put("reply", message);
        result.put("ok", true);
        result.put("idempotent", idempotent);
        result.put("result", plan.result());
        result.put("data", plan.result());
        return result;
    }

    private Map<String, Object> redact(Map<String, Object> parameters) {
        Map<String, Object> safe = new LinkedHashMap<>(parameters);
        for (String key : new String[]{"password", "passwordHash", "token", "apiKey"}) {
            if (safe.containsKey(key)) safe.put(key, "***");
        }
        return safe;
    }

    private boolean missing(Object value) {
        if (value == null) return true;
        if (value instanceof String text) return text.isBlank();
        if (value instanceof java.util.Collection<?> collection) return collection.isEmpty();
        if (value instanceof Map<?, ?> map) return map.isEmpty();
        return false;
    }

    private int meaningfulValueCount(Map<String, Object> values) {
        return (int) values.values().stream().filter(value -> !missing(value)).count();
    }

    private String normalizeRole(String roleCode) {
        return roleCode == null ? "" : roleCode.trim().toUpperCase(Locale.ROOT);
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) return throwable.getClass().getSimpleName();
        return message.length() > 300 ? message.substring(0, 300) : message;
    }

    private Map<String, Object> immutableMap(Map<String, Object> source) {
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
