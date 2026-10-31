package com.capysoft.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** El contexto completo arranca (consultas JPQL y beans incluidos) y las reglas de acceso son las del monolito. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("commerce-service arranca")
class CommerceServiceApplicationTest {

    @MockitoBean StringRedisTemplate redis;
    @MockitoBean KafkaTemplate<String, String> kafka;

    @Autowired MockMvc mvc;

    @Value("${payment.reconciliation.enabled}") boolean reconciliationEnabled;

    @Test
    @DisplayName("la conciliación de pagos viene apagada: hasta el corte la hace el monolito")
    void reconciliationIsOffByDefault() {
        assertThat(reconciliationEnabled).isFalse();
    }

    @Test
    @DisplayName("el health es público")
    void healthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("sin token no se puede ver la wallet ni acreditar")
    void walletNeedsAToken() throws Exception {
        mvc.perform(get("/api/v1/wallet")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/wallet/credit").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("el webhook no pide token: lo autentica su firma, y sin firma no pasa de ahí")
    void webhookIsNotBehindTheToken() throws Exception {
        int status = mvc.perform(post("/api/v1/webhooks/payment").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isNotEqualTo(401).isGreaterThanOrEqualTo(400);
    }
}
