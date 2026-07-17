package com.upc.computer.ai.action;

import java.util.List;
import java.util.Map;

public record AgentPlanOutcome(
        String status,
        String message,
        List<AgentRequiredField> missingFields,
        Map<String, Object> confirmation
) {
    public boolean pending() {
        return "PENDING".equals(status) && confirmation != null;
    }
}
