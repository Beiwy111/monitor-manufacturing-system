package com.upc.computer.assistant;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音助手会话上下文（多轮槽位填充 / 候选选择）。
 * key = sessionId（前端生成，通常为 用户名-时间戳）。
 * 参照 AfterSalesServiceImpl.latestAnalyses 的 ConcurrentHashMap 用法，进程内存储、无持久化。
 */
@Component
public class AssistantSessionStore {

    private final Map<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();

    public Map<String, Object> get(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) sessionId = "_default";
        return sessions.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());
    }

    public void put(String sessionId, String key, Object value) {
        if (value == null) get(sessionId).remove(key);
        else get(sessionId).put(key, value);
    }

    public Object get(String sessionId, String key) {
        return get(sessionId).get(key);
    }

    public void clear(String sessionId) {
        if (sessionId != null) sessions.remove(sessionId);
    }
}
