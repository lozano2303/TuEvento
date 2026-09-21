package com.capysoft.fakepaymentgateway.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Registro de auditoría de entregas webhook.
 */
@Entity
@Table(name = "webhook_delivery_logs")
@Getter
@Setter
public class WebhookDeliveryLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String eventId;
    
    @Column(nullable = false)
    private Integer attemptNumber;
    
    @Column
    private Integer httpStatus;
    
    @Column(length = 1000)
    private String errorMessage;
    
    @Column(nullable = false)
    private Instant sentAt;
}
