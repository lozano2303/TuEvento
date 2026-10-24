package com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.payment.application.dto.response.AdminRefundResponse;
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

    /** Reembolsos más recientes con pago, comprador y evento — panel de reembolsos del admin. */
    @Query("""
            SELECT r.refundId AS refundId, r.paymentId AS paymentId, p.orderId AS orderId,
                   o.userId AS userId, u.alias AS alias, pr.fullName AS fullName, c.email AS email,
                   o.eventId AS eventId, e.eventName AS eventName,
                   p.amount AS gatewayAmount, p.walletAmountApplied AS walletAmount, p.currency AS currency,
                   r.status AS status, r.reason AS reason, r.requestedAt AS requestedAt, r.processedAt AS processedAt
            FROM RefundEntity r
            JOIN PaymentEntity p ON p.paymentId = r.paymentId
            JOIN OrderEntity o ON o.orderId = p.orderId
            LEFT JOIN EventEntity e ON e.eventId = o.eventId
            LEFT JOIN UserEntity u ON u.userId = o.userId
            LEFT JOIN ProfileEntity pr ON pr.userId = o.userId
            LEFT JOIN LoginCredentialsEntity c ON c.user = u
            ORDER BY r.requestedAt DESC
            """)
    List<AdminRefundResponse> findRecentForAdmin(Pageable pageable);
}
