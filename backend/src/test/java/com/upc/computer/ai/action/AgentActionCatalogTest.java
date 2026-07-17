package com.upc.computer.ai.action;

import com.upc.computer.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentActionCatalogTest {

    private final AgentActionCatalog catalog = new AgentActionCatalog();

    @Test
    void roleShouldOnlySeeItsOwnWriteActions() {
        assertThat(catalog.allowedActions("ORDER"))
                .extracting(item -> item.get("actionCode"))
                .contains("createOrder", "crud.order.customer.update")
                .doesNotContain("confirmSmartDispatch", "admin.user.create", "quality.pass");

        assertThat(catalog.allowedActions("QC"))
                .extracting(item -> item.get("actionCode"))
                .contains("quality.pass", "quality.fail")
                .doesNotContain("createOrder", "purchase.orders.generate");
    }

    @Test
    void adminShouldOwnBackendActionsButNotCustomerSelfServiceActions() {
        assertThat(catalog.allowedActions("ADMIN"))
                .extracting(item -> item.get("actionCode"))
                .contains("admin.user.create", "createOrder", "quality.pass")
                .doesNotContain("customer.order.create", "customer.profile.update");
    }

    @Test
    void forbiddenActionShouldBeRejectedEvenWhenModelSuppliesValidCode() {
        assertThatThrownBy(() -> catalog.requireAllowed("createOrder", "QC"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权执行");
    }
}
