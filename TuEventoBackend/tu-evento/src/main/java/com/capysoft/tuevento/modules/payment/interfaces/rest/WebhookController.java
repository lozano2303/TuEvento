package com.capysoft.tuevento.modules.payment.interfaces.rest;

import com.capysoft.tuevento.modules.payment.application.usecase.ProcessWebhookUseCaseImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador público para recibir webhooks del gateway de pago.
 * Este endpoint NO requiere JWT - se autentica mediante firma HMAC-SHA256.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {
    
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    
    private final ProcessWebhookUseCaseImpl processWebhookUseCase;
    
    /**
     * Recibe webhooks del fake-payment-gateway.
     * La autenticación se realiza mediante firma HMAC en el header X-Signature.
     */
    @PostMapping("/payment")
    public ResponseEntity<Void> receivePaymentWebhook(
        @RequestBody String payload,
        @RequestHeader("X-Signature") String signature
    ) {
        log.info("Received payment webhook");
        
        try {
            processWebhookUseCase.execute(payload, signature);
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            log.error("Invalid webhook signature", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
