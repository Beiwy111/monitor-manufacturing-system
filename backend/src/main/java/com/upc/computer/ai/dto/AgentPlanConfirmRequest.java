package com.upc.computer.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentPlanConfirmRequest(
        @NotBlank(message = "decision 不能为空") String decision,
        Object finalParams
) {
}
