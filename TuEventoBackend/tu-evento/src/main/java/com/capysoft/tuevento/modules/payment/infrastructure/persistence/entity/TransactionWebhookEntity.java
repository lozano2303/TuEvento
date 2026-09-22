package com.capysoft.tuevento.modules.payment.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA para TransactionWebhook (auditoría de webhooks recibidos).
 */
@Entity
@Table(name = "transaction_webhook")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionWebhookEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "webhook_id")
    private Long webhookId;
    
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;
    
    @Column(name = "gateway_event_id", nullable = false, unique = true, length = 100)
    private String gatewayEventId;
    
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}
