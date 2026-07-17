package com.upc.computer.ai.action;

import java.util.List;
import java.util.Set;

public record AgentActionDefinition(
        String code,
        String title,
        String module,
        Set<String> roles,
        List<AgentRequiredField> requiredFields
) {
}
