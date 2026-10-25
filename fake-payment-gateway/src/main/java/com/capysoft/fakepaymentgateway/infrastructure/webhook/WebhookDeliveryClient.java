package com.capysoft.fakepaymentgateway.infrastructure.webhook;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.capysoft.fakepaymentgateway.domain.event.PaymentStatusChanged;
import com.capysoft.fakepaymentgateway.infrastructure.persistence.WebhookDeliveryLogEntity;
import com.capysoft.fakepaymentgateway.infrastructure.persistence.WebhookDeliveryLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Realiza un intento de entrega de webhook: serializa, firma con HMAC-SHA256,
 * envía y registra el resultado en la bitácora. Nunca lanza excepciones.
 */
@Component
public class WebhookDeliveryClient {
    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryClient.class);
    public static final String SIGNATURE_HEADER = "X-Signature";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WebhookDeliveryLogRepository logRepository;
    private final String callbackUrl;
    private final String webhookSecret;

    public WebhookDeliveryClient(
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        WebhookDeliveryLogRepository logRepository,
        @Value("${webhook.callback-url}") String callbackUrl,
        @Value("${webhook.secret}") String webhookSecret
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.logRepository = logRepository;
        this.callbackUrl = callbackUrl;
        this.webhookSecret = webhookSecret;
    }

    /**
     * Realiza un intento de entrega del webhook.
     * @return true si el receptor respondió 2xx, false en cualquier otro caso
     */
    public boolean deliver(PaymentStatusChanged event, int attemptNumber) {
        try {
            String payload = buildPayload(event);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(SIGNATURE_HEADER, sign(payload));

            ResponseEntity<String> response = restTemplate.exchange(
                callbackUrl,
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                String.class
            );

            int statusCode = response.getStatusCode().value();
            logDelivery(event.eventId(), attemptNumber, statusCode, null);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Webhook delivered successfully: eventId={}, attempt={}",
                    event.eventId(), attemptNumber);
                return true;
            }

            log.warn("Webhook returned non-2xx status: eventId={}, attempt={}, status={}",
                event.eventId(), attemptNumber, statusCode);
            return false;

        } catch (RestClientResponseException e) {
            // RestTemplate lanza excepción para 4xx/5xx: se registra el código recibido
            log.warn("Webhook returned non-2xx status: eventId={}, attempt={}, status={}",
                event.eventId(), attemptNumber, e.getStatusCode().value());
            logDelivery(event.eventId(), attemptNumber, e.getStatusCode().value(), e.getMessage());
            return false;

        } catch (Exception e) {
            log.error("Webhook delivery failed: eventId={}, attempt={}, error={}",
                event.eventId(), attemptNumber, e.getMessage());
            logDelivery(event.eventId(), attemptNumber, null, e.getMessage());
            return false;
        }
    }

    /**
     * Firma HMAC-SHA256 del payload, en hexadecimal minúsculo.
     */
    public String sign(String payload) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    String buildPayload(PaymentStatusChanged event) throws JsonProcessingException {
        WebhookPayload payload = new WebhookPayload(
            event.eventId(),
            "payment." + event.status().name().toLowerCase(),
            event.paymentId(),
            event.status().name(),
            event.money().getAmount(),
            event.money().getCurrency(),
            event.timestamp()
        );
        return objectMapper.writeValueAsString(payload);
    }

    private void logDelivery(String eventId, int attempt, Integer httpStatus, String errorMessage) {
        try {
            WebhookDeliveryLogEntity entry = new WebhookDeliveryLogEntity();
            entry.setEventId(eventId);
            entry.setAttemptNumber(attempt);
            entry.setHttpStatus(httpStatus);
            entry.setErrorMessage(truncate(errorMessage));
            entry.setSentAt(Instant.now());
            logRepository.save(entry);
        } catch (Exception e) {
            // La bitácora no debe interrumpir la entrega
            log.warn("Could not persist webhook delivery log: eventId={}, error={}", eventId, e.getMessage());
        }
    }

    private static String truncate(String message) {
        if (message == null || message.length() <= 1000) {
            return message;
        }
        return message.substring(0, 1000);
    }
}
