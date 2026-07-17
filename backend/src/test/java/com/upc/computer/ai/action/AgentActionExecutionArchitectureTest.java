package com.upc.computer.ai.action;

import com.upc.computer.ai.tool.AgentWritePlanTools;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AgentActionExecutionArchitectureTest {

    @Test
    void llmWriteToolShouldOnlyDependOnPlanService() {
        assertThat(Arrays.stream(AgentWritePlanTools.class.getDeclaredFields())
                .map(field -> field.getType().getName())
                .filter(name -> name.startsWith("com.upc.computer")))
                .containsExactly(AgentActionPlanService.class.getName(),
                        com.upc.computer.dto.LoginResponse.class.getName(), AgentPlanOutcome.class.getName());

        assertThat(Arrays.stream(AgentWritePlanTools.class.getDeclaredFields())
                .map(field -> field.getType().getName()))
                .noneMatch(name -> name.contains(".mapper.") || name.contains("Repository"));
    }

    @Test
    void confirmedExecutionMustRunInsideRollbackEnabledTransaction() throws Exception {
        Method execute = AgentActionExecutionService.class.getMethod("execute", AgentActionPlan.class,
                com.upc.computer.dto.LoginResponse.class);
        Transactional transactional = execute.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.rollbackFor()).contains(Exception.class);
    }
}
