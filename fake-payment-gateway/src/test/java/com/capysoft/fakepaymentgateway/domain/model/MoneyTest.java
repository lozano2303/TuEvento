package com.capysoft.fakepaymentgateway.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void normalizaLaMonedaAMayusculas() {
        assertThat(new Money(BigDecimal.TEN, "cop").getCurrency()).isEqualTo("COP");
    }

    @Test
    void rechazaMontoNegativoONulo() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-1"), "COP")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Money(null, "COP")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaMonedaVacia() {
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void igualdadIgnoraLaEscalaDelMonto() {
        assertThat(new Money(new BigDecimal("150000"), "COP"))
            .isEqualTo(new Money(new BigDecimal("150000.0000"), "COP"));
    }
}
