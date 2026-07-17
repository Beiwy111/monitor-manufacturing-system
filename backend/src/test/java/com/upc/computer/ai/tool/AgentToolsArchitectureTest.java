package com.upc.computer.ai.tool;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentToolsArchitectureTest {

    private static final List<Class<?>> TOOL_TYPES = List.of(
            AdminAgentTools.class,
            OrderAgentTools.class,
            ProductionQueryAgentTools.class,
            PlannerAgentTools.class,
            ManagerAgentTools.class,
            EquipmentAgentTools.class,
            MaterialWarehouseAgentTools.class,
            QualityAgentTools.class,
            PurchaseAgentTools.class,
            AfterSalesAgentTools.class,
            FinanceAgentTools.class,
            CustomerAgentTools.class
    );

    @Test
    void toolsMustNotDependOnMapperRepositoryShellOrSqlTypes() {
        for (Class<?> toolType : TOOL_TYPES) {
            for (Field field : toolType.getDeclaredFields()) {
                String dependency = field.getType().getName().toLowerCase();
                assertThat(dependency)
                        .as("%s.%s", toolType.getSimpleName(), field.getName())
                        .doesNotContain(".mapper.")
                        .doesNotContain("repository")
                        .doesNotContain("processbuilder")
                        .doesNotContain("datasource")
                        .doesNotContain("jdbctemplate");
            }
        }
    }

    @Test
    void everyPublicBusinessMethodMustBeAnExplicitSpringAiTool() {
        for (Class<?> toolType : TOOL_TYPES) {
            for (Method method : toolType.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && !method.isSynthetic()) {
                    assertThat(method.isAnnotationPresent(Tool.class))
                            .as("%s.%s must declare @Tool", toolType.getSimpleName(), method.getName())
                            .isTrue();
                }
            }
        }
    }
}
