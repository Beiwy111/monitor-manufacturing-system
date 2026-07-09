package com.upc.computer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.upc.computer.common.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MES 运行时补充数据（Redis 持久化）
 */
@Component
public class MesRuntimeStore {

    public static final String REDIS_KEY = "mes:runtime:state";

    @Autowired
    private RedisUtil redisUtil;

    public MesRuntimeState load() {
        MesRuntimeState state = redisUtil.getJson(REDIS_KEY, new TypeReference<MesRuntimeState>() {
        });
        if (state == null) {
            state = new MesRuntimeState();
        }
        if (state.inboundTasks == null) {
            state.inboundTasks = new ArrayList<>();
        }
        if (state.issueTasks == null) {
            state.issueTasks = new ArrayList<>();
        }
        if (state.stockFlows == null) {
            state.stockFlows = new ArrayList<>();
        }
        if (state.operationLogs == null) {
            state.operationLogs = new ArrayList<>();
        }
        if (state.extras == null) {
            state.extras = new HashMap<>();
        }
        normalizeLegacyIssueTasks(state);
        return state;
    }

    private void normalizeLegacyIssueTasks(MesRuntimeState state) {
        boolean changed = false;
        for (Map<String, Object> task : state.issueTasks) {
            String code = String.valueOf(task.getOrDefault("materialCode", ""));
            if ("BL-MODULE".equals(code)) {
                task.put("materialCode", "MAT-002");
                changed = true;
            }
        }
        if (changed) {
            save(state);
        }
    }

    public void save(MesRuntimeState state) {
        redisUtil.setJson(REDIS_KEY, state);
    }

    public void mergeIntoSnapshot(Map<String, Object> snapshot, MesRuntimeState state) {
        snapshot.put("inboundTasks", new ArrayList<>(state.inboundTasks));
        snapshot.put("issueTasks", new ArrayList<>(state.issueTasks));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dbFlows = (List<Map<String, Object>>) snapshot.get("stockFlows");
        List<Map<String, Object>> mergedFlows = new ArrayList<>();
        if (dbFlows != null) {
            mergedFlows.addAll(dbFlows);
        }
        mergedFlows.addAll(state.stockFlows);
        snapshot.put("stockFlows", mergedFlows);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dbLogs = (List<Map<String, Object>>) snapshot.get("operationLogs");
        List<Map<String, Object>> mergedLogs = new ArrayList<>();
        mergedLogs.addAll(state.operationLogs);
        if (dbLogs != null) {
            mergedLogs.addAll(dbLogs);
        }
        mergedLogs.sort((a, b) -> {
            String ta = String.valueOf(a.getOrDefault("createdAt", ""));
            String tb = String.valueOf(b.getOrDefault("createdAt", ""));
            return tb.compareTo(ta);
        });
        if (mergedLogs.size() > 200) {
            mergedLogs = new ArrayList<>(mergedLogs.subList(0, 200));
        }
        snapshot.put("operationLogs", mergedLogs);
    }

    public Map<String, Object> getExtra(MesRuntimeState state, String key) {
        return state.extras.computeIfAbsent(key, k -> new HashMap<>());
    }

    public static class MesRuntimeState {
        private List<Map<String, Object>> inboundTasks = new ArrayList<>();
        private List<Map<String, Object>> issueTasks = new ArrayList<>();
        private List<Map<String, Object>> stockFlows = new ArrayList<>();
        private List<Map<String, Object>> operationLogs = new ArrayList<>();
        private Map<String, Map<String, Object>> extras = new HashMap<>();
        private long logSeq;

        public List<Map<String, Object>> getInboundTasks() {
            return inboundTasks;
        }

        public void setInboundTasks(List<Map<String, Object>> inboundTasks) {
            this.inboundTasks = inboundTasks;
        }

        public List<Map<String, Object>> getIssueTasks() {
            return issueTasks;
        }

        public void setIssueTasks(List<Map<String, Object>> issueTasks) {
            this.issueTasks = issueTasks;
        }

        public List<Map<String, Object>> getStockFlows() {
            return stockFlows;
        }

        public void setStockFlows(List<Map<String, Object>> stockFlows) {
            this.stockFlows = stockFlows;
        }

        public List<Map<String, Object>> getOperationLogs() {
            return operationLogs;
        }

        public void setOperationLogs(List<Map<String, Object>> operationLogs) {
            this.operationLogs = operationLogs;
        }

        public Map<String, Map<String, Object>> getExtras() {
            return extras;
        }

        public void setExtras(Map<String, Map<String, Object>> extras) {
            this.extras = extras;
        }

        public long getLogSeq() {
            return logSeq;
        }

        public void setLogSeq(long logSeq) {
            this.logSeq = logSeq;
        }
    }
}
