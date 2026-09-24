package com.capysoft.fakepaymentgateway.infrastructure.persistence;

import org.hibernate.annotations.Check;

import com.capysoft.fakepaymentgateway.domain.model.PaymentMethod;
import com.capysoft.fakepaymentgateway.domain.model.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entidad JPA para persistencia de pagos.
 */
@Entity
@Table(name = "payments")
@Check(constraints = "status IN ('PENDING','PROCESSING','APPROVED','DECLINED','FAILED','CANCELLED','REFUNDED')")
@Getter
@Setter
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String paymentId;
    
    @Column(nullable = false, length = 100)
    private String externalReference;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant updatedAt;
    
    @Column(nullable = false, unique = true, length = 50)
    private String webhookEventId;
}
