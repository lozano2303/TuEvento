package com.capysoft.tuevento.shared.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;

@DisplayName("InternalApiKeyFilter")
class InternalApiKeyFilterTest {

    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private boolean authenticates(String configuredKey, String sentKey) throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/users/summary");
        if (sentKey != null) {
            request.addHeader(InternalApiKeyFilter.HEADER, sentKey);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        new InternalApiKeyFilter(configuredKey).doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> InternalApiKeyFilter.AUTHORITY.equals(a.getAuthority()));
    }

    @Test
    @DisplayName("solo la clave exacta autentica como servicio interno")
    void onlyExactKey() throws Exception {
        assertThat(authenticates("s3cret", "s3cret")).isTrue();
        assertThat(authenticates("s3cret", "s3cre")).isFalse();
        assertThat(authenticates("s3cret", "s3cret ")).isFalse();
        assertThat(authenticates("s3cret", "")).isFalse();
        assertThat(authenticates("s3cret", null)).isFalse();
    }

    @Test
    @DisplayName("fail-closed: sin clave configurada nada autentica, ni siquiera una clave vacía")
    void unconfiguredKeyRejectsEverything() throws Exception {
        assertThat(authenticates("", "")).isFalse();
        assertThat(authenticates(null, "anything")).isFalse();
        assertThat(authenticates("  ", "  ")).isFalse();
    }
}
