package com.upc.computer.ai.action;

import com.fasterxml.jackson.core.type.TypeReference;
import com.upc.computer.common.RedisUtil;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AgentPlanRepository {

    static final long PLAN_TTL_SECONDS = 900;
    static final long RESULT_TTL_SECONDS = 3600;
    private static final String PLAN_PREFIX = "mes:ai:plan:";
    private static final String DRAFT_PREFIX = "mes:ai:draft:";

    private final RedisUtil redisUtil;

    public AgentPlanRepository(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    public void save(AgentActionPlan plan) {
        long ttl = plan.status() == AgentPlanStatus.EXECUTED ? RESULT_TTL_SECONDS : PLAN_TTL_SECONDS;
        redisUtil.setJson(PLAN_PREFIX + plan.planId(), plan, ttl);
    }

    public AgentActionPlan find(String planId) {
        return redisUtil.getJson(PLAN_PREFIX + planId, AgentActionPlan.class);
    }

    public Map<String, Object> findDraft(Long userId, String sessionId, String actionCode) {
        Map<String, Object> draft = redisUtil.getJson(draftKey(userId, sessionId, actionCode),
                new TypeReference<Map<String, Object>>() { });
        return draft == null ? new LinkedHashMap<>() : new LinkedHashMap<>(draft);
    }

    public void saveDraft(Long userId, String sessionId, String actionCode, Map<String, Object> parameters) {
        redisUtil.setJson(draftKey(userId, sessionId, actionCode), parameters, PLAN_TTL_SECONDS);
    }

    public void deleteDraft(Long userId, String sessionId, String actionCode) {
        redisUtil.delete(draftKey(userId, sessionId, actionCode));
    }

    private String draftKey(Long userId, String sessionId, String actionCode) {
        return DRAFT_PREFIX + userId + ":" + sessionId + ":" + actionCode;
    }
}
