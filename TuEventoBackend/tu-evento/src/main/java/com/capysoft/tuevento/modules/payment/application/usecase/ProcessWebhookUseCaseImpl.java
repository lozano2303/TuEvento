package com.capysoft.tuevento.modules.payment.application.usecase;

import com.capysoft.tuevento.modules.payment.application.dto.GatewayPaymentEvent;
import com.capysoft.tuevento.modules.payment.application.port.out.PaymentGatewayPort;
import com.capysoft.tuevento.modules.payment.domain.model.*;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentLogRepository;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import com.capysoft.tuevento.modules.payment.domain.repository.TransactionWebhookRepository;
import com.capysoft.tuevento.modules.ticket.application.port.in.ConfirmOrderPaymentUseCase;
import com.capysoft.tuevento.modules.ticket.application.port.in.FailOrderPaymentUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use case para procesar webhooks del gateway de pago.
 * 1. Verifica firma HMAC
 * 2. Verifica idempotencia (gatewayEventId)
 * 3. Persiste TransactionWebhook
 * 4. Actualiza Payment con PaymentLog
 * 5. Llama a ConfirmOrderPaymentUseCase o FailOrderPaymentUseCase del módulo ticket
 */
@Service
@RequiredArgsConstructor
public class ProcessWebhookUseCaseImpl {
    
    private static final Logger log = LoggerFactory.getLogger(ProcessWebhookUseCaseImpl.class);
    
    private final PaymentRepository paymentRepository;
    private final TransactionWebhookRepository webhookRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final PaymentGatewayPort paymentGatewayPort;
    private final ConfirmOrderPaymentUseCase confirmOrderPaymentUseCase;
    private final FailOrderPaymentUseCase failOrderPaymentUseCase;
    
    @Transactional
    public void execute(String payload, String signature) {
        // 1. Procesar webhook (verifica firma HMAC internamente)
        GatewayPaymentEvent event = paymentGatewayPort.processWebhook(payload, signature);
        
        // 2. Verificar idempotencia
        if (webhookRepository.existsByGatewayEventId(event.getEventId())) {
            log.info("Webhook already processed: eventId={}", event.getEventId());
            return;
        }
        
        // 3. Buscar pago por gatewayTransactionId
        Payment payment = paymentRepository.findByGatewayTransactionId(event.getPaymentId())
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found for gatewayTransactionId: " + event.getPaymentId()));
        
        // 4. Persistir TransactionWebhook
        TransactionWebhook webhook = TransactionWebhook.builder()
            .paymentId(payment.getPaymentId())
            .gatewayEventId(event.getEventId())
            .payload(payload)
            .receivedAt(LocalDateTime.now())
            .build();
        webhookRepository.save(webhook);
        
        // 5. Actualizar Payment según el evento
        PaymentStatus oldStatus = payment.getStatus();
        String reason = "Webhook: " + event.getEventType();
        
        try {
            switch (event.getStatus().toUpperCase()) {
                case "APPROVED":
                    payment.approve();
                    paymentRepository.save(payment);
                    
                    // Log de auditoría
                    savePaymentLog(payment.getPaymentId(), oldStatus, PaymentStatus.APPROVED, reason);
                    
                    // Confirmar orden y tickets en módulo ticket
                    confirmOrderPaymentUseCase.confirmOrderPayment(payment.getOrderId(), event.getPaymentId());
                    log.info("Payment approved and order confirmed: paymentId={}, orderId={}", 
                        payment.getPaymentId(), payment.getOrderId());
                    break;
                    
                case "DECLINED":
                    payment.reject();
                    paymentRepository.save(payment);
                    
                    savePaymentLog(payment.getPaymentId(), oldStatus, PaymentStatus.REJECTED, reason);
                    
                    // Fallar orden en módulo ticket (libera sillas)
                    failOrderPaymentUseCase.failOrderPayment(payment.getOrderId(), "Payment declined");
                    log.info("Payment declined and order cancelled: paymentId={}, orderId={}", 
                        payment.getPaymentId(), payment.getOrderId());
                    break;
                    
                case "FAILED":
                    payment.markAsError();
                    paymentRepository.save(payment);
                    
                    savePaymentLog(payment.getPaymentId(), oldStatus, PaymentStatus.ERROR, reason);
                    
                    failOrderPaymentUseCase.failOrderPayment(payment.getOrderId(), "Payment failed");
                    log.info("Payment failed and order cancelled: paymentId={}, orderId={}", 
                        payment.getPaymentId(), payment.getOrderId());
                    break;
                    
                case "CANCELLED":
                    payment.markAsError();
                    paymentRepository.save(payment);
                    
                    savePaymentLog(payment.getPaymentId(), oldStatus, PaymentStatus.ERROR, "Webhook: payment cancelled by user");
                    
                    // Cancelar orden en módulo ticket (libera sillas)
                    failOrderPaymentUseCase.failOrderPayment(payment.getOrderId(), "Payment cancelled");
                    log.info("Payment cancelled and order released: paymentId={}, orderId={}", 
                        payment.getPaymentId(), payment.getOrderId());
                    break;
                    
                default:
                    log.warn("Unhandled payment status from webhook: status={}, eventId={}", 
                        event.getStatus(), event.getEventId());
            }
        } catch (InvalidPaymentStatusTransitionException e) {
            log.error("Invalid payment status transition: paymentId={}, from={}, to={}, reason={}", 
                payment.getPaymentId(), oldStatus, event.getStatus(), e.getMessage());
            throw e;
        }
    }
    
    private void savePaymentLog(Long paymentId, PaymentStatus oldStatus, PaymentStatus newStatus, String reason) {
        PaymentLog log = PaymentLog.builder()
            .paymentId(paymentId)
            .oldStatus(oldStatus)
            .newStatus(newStatus)
            .changedAt(LocalDateTime.now())
            .reason(reason)
            .build();
        paymentLogRepository.save(log);
    }
}
