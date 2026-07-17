package com.upc.computer.ai.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.computer.dto.LoginResponse;
import com.upc.computer.entity.OperationLog;
import com.upc.computer.service.SystemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AgentAuditService {

    private final SystemService systemService;
    private final ObjectMapper objectMapper;

    public AgentAuditService(SystemService systemService, ObjectMapper objectMapper) {
        this.systemService = systemService;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(LoginResponse session, AgentActionDefinition action, String phase,
                       String planId, Map<String, Object> parameters, boolean success, String error) {
        OperationLog log = new OperationLog();
        log.setUserId(session.getUserId());
        log.setModuleName("AI Agent-" + action.module());
        log.setOperationType(phase);
        log.setBusinessTable("agent_action_plan");
        log.setOperationContent(limit(toJson(Map.of(
                "planId", planId,
                "actionCode", action.code(),
                "parameters", redact(parameters)
        )), 1000));
        log.setIpAddress("agent");
        log.setResultStatus(success ? "SUCCESS" : "FAILED");
        log.setErrorMessage(error == null ? null : limit(error, 500));
        log.setOperatedAt(LocalDateTime.now());
        systemService.insertOperationLog(log);
    }

    private Map<String, Object> redact(Map<String, Object> parameters) {
        Map<String, Object> safe = new LinkedHashMap<>(parameters == null ? Map.of() : parameters);
        for (String key : new String[]{"password", "passwordHash", "token", "apiKey"}) {
            if (safe.containsKey(key)) safe.put(key, "***");
        }
        return safe;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }
}
