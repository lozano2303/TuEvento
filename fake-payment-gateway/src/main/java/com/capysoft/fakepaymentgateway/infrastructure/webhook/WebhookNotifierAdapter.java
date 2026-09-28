package com.capysoft.fakepaymentgateway.infrastructure.webhook;

import java.nio.charset.StandardCharsets;
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
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.capysoft.fakepaymentgateway.application.port.out.WebhookNotifierPort;
import com.capysoft.fakepaymentgateway.domain.event.PaymentStatusChanged;
import com.capysoft.fakepaymentgateway.infrastructure.persistence.WebhookDeliveryLogEntity;
import com.capysoft.fakepaymentgateway.infrastructure.persistence.WebhookDeliveryLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Adaptador que implementa el envío de webhooks con firma HMAC y reintentos.
 */
@Component
public class WebhookNotifierAdapter implements WebhookNotifierPort {
    private static final Logger log = LoggerFactory.getLogger(WebhookNotifierAdapter.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long[] BACKOFF_DELAYS_MS = {2000, 10000, 30000}; // 2s, 10s, 30s
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WebhookDeliveryLogRepository logRepository;
    
    @Value("${webhook.callback-url}")
    private String callbackUrl;
    
    @Value("${webhook.secret}")
    private String webhookSecret;
    
    public WebhookNotifierAdapter(
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        WebhookDeliveryLogRepository logRepository
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.logRepository = logRepository;
    }
    
    /**
     * Envía el webhook de forma síncrona (primer intento).
     * Si falla el primer intento, encola los reintentos de forma asíncrona.
     * 
     * El primer intento debe ser síncrono para que participe en la transacción:
     * si falla el save() del payment, el webhook nunca se envía.
     * Los reintentos pueden ser async porque el estado local ya está persistido.
     */
    @Override
    public void notify(PaymentStatusChanged event) {
        try {
            // Primer intento síncrono
            boolean success = attemptDelivery(event, 1);
            
            if (!success) {
                // Si el primer intento falló, encolar reintentos async
                retryAsync(event, 2);
            }
        } catch (Exception e) {
            log.error("First webhook attempt failed critically: eventId={}, error={}", 
                event.eventId(), e.getMessage());
            // Encolar reintentos incluso si el primer intento lanzó excepción
            retryAsync(event, 2);
        }
    }
    
    /**
     * Realiza un intento de entrega del webhook.
     * @return true si el webhook fue entregado exitosamente (2xx), false en caso contrario
     */
    private boolean attemptDelivery(PaymentStatusChanged event, int attemptNumber) {
        try {
            String payload = buildPayload(event);
            String signature = generateSignature(payload);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Signature", signature);
            
            HttpEntity<String> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                callbackUrl,
                HttpMethod.POST,
                request,
                String.class
            );
            
            int statusCode = response.getStatusCode().value();
            logDelivery(event.eventId(), attemptNumber, statusCode, null);
            
            if (statusCode >= 200 && statusCode < 300) {
                log.info("Webhook delivered successfully: eventId={}, attempt={}", 
                    event.eventId(), attemptNumber);
                return true;
            }
            
            log.warn("Webhook returned non-2xx status: eventId={}, attempt={}, status={}", 
                event.eventId(), attemptNumber, statusCode);
            return false;
            
        } catch (Exception e) {
            log.error("Webhook delivery failed: eventId={}, attempt={}, error={}", 
                event.eventId(), attemptNumber, e.getMessage());
            
            logDelivery(event.eventId(), attemptNumber, null, e.getMessage());
            return false;
        }
    }
    
    /**
     * Encola los reintentos de forma asíncrona.
     * Se ejecuta en un thread separado, no bloquea la transacción.
     */
    @Async
    private void retryAsync(PaymentStatusChanged event, int startAttempt) {
        for (int attempt = startAttempt; attempt <= MAX_ATTEMPTS; attempt++) {
            // Backoff antes del reintento
            if (attempt > 1) {
                try {
                    Thread.sleep(BACKOFF_DELAYS_MS[attempt - 2]);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Backoff interrupted for eventId={}", event.eventId());
                    return;
                }
            }
            
            boolean success = attemptDelivery(event, attempt);
            if (success) {
                return; // Éxito, detener reintentos
            }
        }
        
        log.error("Webhook delivery failed after {} attempts: eventId={}", 
            MAX_ATTEMPTS, event.eventId());
    }
    
    private String buildPayload(PaymentStatusChanged event) throws Exception {
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
    
    private String generateSignature(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
            webhookSecret.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        );
        mac.init(secretKeySpec);
        
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
    
    private void logDelivery(String eventId, int attempt, Integer httpStatus, String errorMessage) {
        WebhookDeliveryLogEntity log = new WebhookDeliveryLogEntity();
        log.setEventId(eventId);
        log.setAttemptNumber(attempt);
        log.setHttpStatus(httpStatus);
        log.setErrorMessage(errorMessage);
        log.setSentAt(Instant.now());
        
        logRepository.save(log);
    }
}
