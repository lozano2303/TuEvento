package com.capysoft.tuevento.modules.payment.infrastructure.persistence.entity;

import com.capysoft.tuevento.modules.payment.domain.model.RefundStatus;
import com.capysoft.tuevento.shared.infrastructure.persistence.JpaAuditingEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA para Refund.
 */
@Entity
@Table(name = "refund")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundEntity extends JpaAuditingEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long refundId;
    
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status;
    
    @Column(name = "reason", length = 255)
    private String reason;
    
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
