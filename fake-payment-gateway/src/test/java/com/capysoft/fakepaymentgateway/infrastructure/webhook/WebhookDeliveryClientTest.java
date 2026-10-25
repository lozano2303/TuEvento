package com.capysoft.fakepaymentgateway.infrastructure.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.capysoft.fakepaymentgateway.domain.event.PaymentStatusChanged;
import com.capysoft.fakepaymentgateway.domain.model.Money;
import com.capysoft.fakepaymentgateway.domain.model.PaymentStatus;
import com.capysoft.fakepaymentgateway.infrastructure.persistence.WebhookDeliveryLogEntity;
import com.capysoft.fakepaymentgateway.infrastructure.persistence.WebhookDeliveryLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class WebhookDeliveryClientTest {

    private static final String URL = "http://client.test/webhooks/payment";
    private static final String SECRET = "test-secret";

    private MockRestServiceServer server;
    private WebhookDeliveryLogRepository logRepository;
    private WebhookDeliveryClient client;

    private final PaymentStatusChanged event = new PaymentStatusChanged(
        "5b0e9a52-7f0c-4a8a-9a3e-0c1d2e3f4a5b",
        "fake_123",
        PaymentStatus.APPROVED,
        new Money(new BigDecimal("150000.0000"), "COP"),
        Instant.parse("2026-10-24T15:05:10Z")
    );

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        logRepository = mock(WebhookDeliveryLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        client = new WebhookDeliveryClient(restTemplate, objectMapper, logRepository, URL, SECRET);
    }

    @Test
    void firmaCoincideConVectorHmacSha256Conocido() throws Exception {
        // Vector de RFC 4231, caso 2: clave "Jefe", mensaje "what do ya want for nothing?"
        WebhookDeliveryClient rfcClient = new WebhookDeliveryClient(null, null, null, URL, "Jefe");

        assertThat(rfcClient.sign("what do ya want for nothing?"))
            .isEqualTo("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843");
    }

    @Test
    void enviaPayloadFirmadoYRegistraElIntento() throws Exception {
        String payload = client.buildPayload(event);
        server.expect(requestTo(URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(WebhookDeliveryClient.SIGNATURE_HEADER, client.sign(payload)))
            .andExpect(content().json("""
                {"eventId":"5b0e9a52-7f0c-4a8a-9a3e-0c1d2e3f4a5b","event":"payment.approved",
                 "paymentId":"fake_123","status":"APPROVED","amount":150000.0000,"currency":"COP",
                 "timestamp":"2026-10-24T15:05:10Z"}
                """))
            .andRespond(withSuccess());

        assertThat(client.deliver(event, 1)).isTrue();

        server.verify();
        WebhookDeliveryLogEntity log = capturedLog();
        assertThat(log.getAttemptNumber()).isEqualTo(1);
        assertThat(log.getHttpStatus()).isEqualTo(200);
        assertThat(log.getErrorMessage()).isNull();
    }

    @Test
    void respuestaNo2xxEsFalloYRegistraElCodigo() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThat(client.deliver(event, 2)).isFalse();

        WebhookDeliveryLogEntity log = capturedLog();
        assertThat(log.getAttemptNumber()).isEqualTo(2);
        assertThat(log.getHttpStatus()).isEqualTo(401);
    }

    @Test
    void errorDeRedEsFalloSinCodigoHttp() {
        server.expect(requestTo(URL)).andRespond(request -> { throw new IOException("Connection refused"); });

        assertThat(client.deliver(event, 1)).isFalse();

        WebhookDeliveryLogEntity log = capturedLog();
        assertThat(log.getHttpStatus()).isNull();
        assertThat(log.getErrorMessage()).contains("Connection refused");
    }

    @Test
    void unFalloAlGuardarLaBitacoraNoRompeLaEntrega() {
        org.mockito.Mockito.when(logRepository.save(org.mockito.ArgumentMatchers.any()))
            .thenThrow(new IllegalStateException("db down"));
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThat(client.deliver(event, 1)).isFalse();
    }

    private WebhookDeliveryLogEntity capturedLog() {
        ArgumentCaptor<WebhookDeliveryLogEntity> captor = ArgumentCaptor.forClass(WebhookDeliveryLogEntity.class);
        verify(logRepository).save(captor.capture());
        return captor.getValue();
    }
}
