package com.capysoft.fakepaymentgateway.domain.model;

import static com.capysoft.fakepaymentgateway.domain.model.PaymentStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class PaymentStatusTest {

    /** Tabla de transiciones documentada en el SRS (RF-08). */
    private static final Map<PaymentStatus, Set<PaymentStatus>> EXPECTED = Map.of(
        PENDING, EnumSet.of(PROCESSING, CANCELLED),
        PROCESSING, EnumSet.of(APPROVED, DECLINED, FAILED),
        APPROVED, EnumSet.of(REFUNDED),
        DECLINED, EnumSet.noneOf(PaymentStatus.class),
        FAILED, EnumSet.noneOf(PaymentStatus.class),
        CANCELLED, EnumSet.noneOf(PaymentStatus.class),
        REFUNDED, EnumSet.noneOf(PaymentStatus.class)
    );

    @Test
    void cadaEstadoPermiteExactamenteLasTransicionesDocumentadas() {
        for (PaymentStatus from : PaymentStatus.values()) {
            for (PaymentStatus to : PaymentStatus.values()) {
                assertThat(from.canTransitionTo(to))
                    .as("%s -> %s", from, to)
                    .isEqualTo(EXPECTED.get(from).contains(to));
            }
        }
    }

    @Test
    void soloAprobadoPuedeReembolsarse() {
        for (PaymentStatus s : PaymentStatus.values()) {
            assertThat(s.canTransitionTo(REFUNDED)).isEqualTo(s == APPROVED);
        }
    }

    @Test
    void estadosTerminales() {
        assertThat(EnumSet.of(DECLINED, FAILED, CANCELLED, REFUNDED)).allMatch(PaymentStatus::isTerminal);
        assertThat(EnumSet.of(PENDING, PROCESSING, APPROVED)).noneMatch(PaymentStatus::isTerminal);
    }
}
