package com.capysoft.fakepaymentgateway.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class AdminApiKeyInterceptorTest {

    @Test
    void rechazaArrancarConApiKeyVacia() {
        ObjectMapper mapper = new ObjectMapper();

        assertThatThrownBy(() -> new AdminApiKeyInterceptor("", mapper)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new AdminApiKeyInterceptor("   ", mapper)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new AdminApiKeyInterceptor(null, mapper)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rechazaArrancarConApiKeyCorta() {
        ObjectMapper mapper = new ObjectMapper();

        assertThatThrownBy(() -> new AdminApiKeyInterceptor("123456", mapper))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("at least 16");
    }
}
