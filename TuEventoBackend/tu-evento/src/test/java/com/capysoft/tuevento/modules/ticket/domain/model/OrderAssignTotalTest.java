package com.capysoft.tuevento.modules.ticket.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El total de una orden solo se fija mientras está en DRAFT, con los precios del bloqueo. */
@DisplayName("Order.assignTotal")
class OrderAssignTotalTest {

    private static Order order(OrderStatus status) {
        return Order.builder().orderId(40L).status(status).totalAmount(new Money(BigDecimal.ZERO, "COP")).build();
    }

    @Test
    @DisplayName("en DRAFT → fija el total")
    void draft_assignsTotal() {
        Order order = order(OrderStatus.DRAFT);

        order.assignTotal(new Money(new BigDecimal("350"), "COP"));

        assertThat(order.getTotalAmount().getAmount()).isEqualByComparingTo("350");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = OrderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
    @DisplayName("fuera de DRAFT → se rechaza y el total no cambia")
    void notDraft_rejected(OrderStatus status) {
        Order order = order(status);

        assertThatThrownBy(() -> order.assignTotal(new Money(BigDecimal.TEN, "COP")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT");
        assertThat(order.getTotalAmount().getAmount()).isEqualByComparingTo("0");
    }
}
