package com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.payment.domain.model.RefundStatus;
import com.capysoft.tuevento.modules.payment.infrastructure.persistence.entity.RefundEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundJpaRepository extends JpaRepository<RefundEntity, Long> {
    Optional<RefundEntity> findByPaymentId(Long paymentId);
    long countByStatus(RefundStatus status);

    /**
     * Reembolsos más recientes con su pago — panel de reembolsos del admin. Los datos del comprador
     * (alias, nombre, email) los completa Identidad y el nombre del evento, Catálogo: aquí solo van
     * sus ids.
     */
    @Query("""
            SELECT r.refundId AS refundId, r.paymentId AS paymentId, p.orderId AS orderId,
                   o.userId AS userId,
                   o.eventId AS eventId,
                   p.amount AS gatewayAmount, p.walletAmountApplied AS walletAmount, p.currency AS currency,
                   r.status AS status, r.reason AS reason, r.requestedAt AS requestedAt, r.processedAt AS processedAt
            FROM RefundEntity r
            JOIN PaymentEntity p ON p.paymentId = r.paymentId
            JOIN OrderEntity o ON o.orderId = p.orderId
            ORDER BY r.requestedAt DESC
            """)
    List<AdminRefundRow> findRecentForAdmin(Pageable pageable);
}
