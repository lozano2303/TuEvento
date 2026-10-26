package com.capysoft.storage.infrastructure.external;

import com.capysoft.storage.infrastructure.config.ModerationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SightengineAdapter — umbral de desnudez raw/partial")
class SightengineAdapterTest {

    @Mock RestTemplate restTemplate;

    private SightengineAdapter adapter;

    @BeforeEach
    void setUp() {
        ModerationConfig config = new ModerationConfig();
        config.getSightengine().setApiUser("user");
        config.getSightengine().setApiSecret("secret");
        config.getSightengine().setNudityThreshold(0.5);
        adapter = new SightengineAdapter(restTemplate, config);
    }

    @SuppressWarnings("unchecked")
    private void respondWith(Map<String, Object> body) {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenReturn(ResponseEntity.ok(body));
    }

    @Test
    @DisplayName("rechaza la imagen cuando raw supera el umbral")
    void rejectsWhenRawAboveThreshold() {
        respondWith(Map.of("nudity", Map.of("raw", 0.8, "partial", 0.0)));

        assertThat(adapter.isSafe(new byte[]{1, 2, 3})).isFalse();
    }

    @Test
    @DisplayName("sin credenciales falla en abierto")
    void failsOpenWithoutCredentials() {
        adapter = new SightengineAdapter(restTemplate, new ModerationConfig());

        assertThat(adapter.isSafe(new byte[]{1})).isTrue();
    }
}
