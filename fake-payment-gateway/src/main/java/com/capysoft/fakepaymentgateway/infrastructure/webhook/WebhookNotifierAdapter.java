package com.capysoft.fakepaymentgateway.infrastructure.webhook;

import com.capysoft.fakepaymentgateway.application.port.out.WebhookNotifierPort;
import com.capysoft.fakepaymentgateway.domain.event.PaymentStatusChanged;
import com.capysoft.fakepaymentgateway.infrastructure.persistence.WebhookDeliveryLogEntity;
import com.capysoft.fakepaymentgateway.infrastructure.persistence.WebhookDeliveryLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

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
    
    @Override
    @Async
    public void notify(PaymentStatusChanged event) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
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
                logDelivery(event.eventId(), attempt, statusCode, null);
                
                if (statusCode >= 200 && statusCode < 300) {
                    log.info("Webhook delivered successfully: eventId={}, attempt={}", 
                        event.eventId(), attempt);
                    return;
                }
                
                log.warn("Webhook returned non-2xx status: eventId={}, attempt={}, status={}", 
                    event.eventId(), attempt, statusCode);
                
            } catch (Exception e) {
                log.error("Webhook delivery failed: eventId={}, attempt={}, error={}", 
                    event.eventId(), attempt, e.getMessage());
                
                logDelivery(event.eventId(), attempt, null, e.getMessage());
                
                if (attempt < MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(BACKOFF_DELAYS_MS[attempt - 1]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Backoff interrupted for eventId={}", event.eventId());
                        return;
                    }
                }
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
