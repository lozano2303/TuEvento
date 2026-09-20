package com.capysoft.tuevento.modules.payment.infrastructure.external;

import com.capysoft.tuevento.modules.payment.application.dto.CreatePaymentCommand;
import com.capysoft.tuevento.modules.payment.application.dto.GatewayPayment;
import com.capysoft.tuevento.modules.payment.application.dto.GatewayPaymentEvent;
import com.capysoft.tuevento.modules.payment.application.port.out.PaymentGatewayPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Adaptador para el fake-payment-gateway.
 * Se activa cuando payment.gateway=fake en la configuración.
 */
@Component
@ConditionalOnProperty(name = "payment.gateway", havingValue = "fake")
public class FakePaymentGatewayAdapter implements PaymentGatewayPort {
    
    private static final Logger log = LoggerFactory.getLogger(FakePaymentGatewayAdapter.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${payment.fake-gateway.url}")
    private String gatewayUrl;
    
    @Value("${payment.webhook.secret}")
    private String webhookSecret;
    
    public FakePaymentGatewayAdapter(
            @Qualifier("paymentRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public GatewayPayment createPayment(CreatePaymentCommand command) {
        String url = gatewayUrl + "/public/payments";
        
        // Mapear a la estructura del fake-gateway
        FakeGatewayCreateRequest request = new FakeGatewayCreateRequest(
            command.getExternalReference(),
            command.getAmount(),
            command.getCurrency(),
            command.getPaymentMethod()
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<FakeGatewayCreateRequest> entity = new HttpEntity<>(request, headers);
        
        log.info("Creating payment in fake-gateway: externalReference={}, amount={}", 
            command.getExternalReference(), command.getAmount());
        
        ResponseEntity<JsonNode> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            JsonNode.class
        );
        
        JsonNode body = response.getBody();
        if (body == null) {
            throw new RuntimeException("Empty response from fake-gateway");
        }
        
        return GatewayPayment.builder()
            .paymentId(body.get("paymentId").asText())
            .externalReference(body.get("externalReference").asText())
            .status(body.get("status").asText())
            .amount(new BigDecimal(body.get("amount").asText()))
            .currency(body.get("currency").asText())
            .paymentMethod(body.get("paymentMethod").asText())
            .createdAt(body.get("createdAt").asText())
            .build();
    }
    
    @Override
    public GatewayPayment getPayment(String gatewayPaymentId) {
        String url = gatewayUrl + "/public/payments/" + gatewayPaymentId;
        
        log.info("Fetching payment from fake-gateway: paymentId={}", gatewayPaymentId);
        
        ResponseEntity<JsonNode> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            JsonNode.class
        );
        
        JsonNode body = response.getBody();
        if (body == null) {
            throw new RuntimeException("Empty response from fake-gateway");
        }
        
        return GatewayPayment.builder()
            .paymentId(body.get("paymentId").asText())
            .externalReference(body.get("externalReference").asText())
            .status(body.get("status").asText())
            .amount(new BigDecimal(body.get("amount").asText()))
            .currency(body.get("currency").asText())
            .paymentMethod(body.get("paymentMethod").asText())
            .createdAt(body.get("createdAt").asText())
            .build();
    }
    
    @Override
    public void cancelPayment(String gatewayPaymentId) {
        String url = gatewayUrl + "/admin/payments/" + gatewayPaymentId + "/cancel";
        
        log.info("Cancelling payment in fake-gateway: paymentId={}", gatewayPaymentId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            Void.class
        );
    }
    
    @Override
    public GatewayPaymentEvent processWebhook(String payload, String signature) {
        // 1. Verificar firma HMAC-SHA256
        if (!verifySignature(payload, signature)) {
            log.error("Invalid webhook signature");
            throw new SecurityException("Invalid webhook signature");
        }
        
        // 2. Parsear payload
        try {
            JsonNode webhookData = objectMapper.readTree(payload);
            JsonNode data = webhookData.get("data");
            
            return GatewayPaymentEvent.builder()
                .eventId(webhookData.get("eventId").asText())
                .eventType(webhookData.get("eventType").asText())
                .paymentId(data.get("paymentId").asText())
                .status(data.get("status").asText())
                .amount(new BigDecimal(data.get("amount").asText()))
                .currency(data.get("currency").asText())
                .timestamp(webhookData.get("timestamp").asText())
                .build();
        } catch (Exception e) {
            log.error("Error parsing webhook payload", e);
            throw new RuntimeException("Invalid webhook payload", e);
        }
    }
    
    /**
     * Verifica la firma HMAC-SHA256 del webhook.
     */
    private boolean verifySignature(String payload, String receivedSignature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);
            
            return calculatedSignature.equals(receivedSignature);
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }
    
    /**
     * Record interno para mapear el request al fake-gateway.
     */
    private record FakeGatewayCreateRequest(
        String externalReference,
        BigDecimal amount,
        String currency,
        String paymentMethod
    ) {}
}
