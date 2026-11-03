package com.capysoft.commerce.payment.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.capysoft.commerce.payment.application.dto.response.OrganizerEarningsByEventResponse;
import com.capysoft.commerce.payment.domain.model.PaymentStatus;
import com.capysoft.commerce.payment.infrastructure.persistence.entity.PaymentEntity;

@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByOrderId(Long orderId);
    Optional<PaymentEntity> findByGatewayTransactionId(String gatewayTransactionId);
    List<PaymentEntity> findByOrderIdInAndStatus(List<Long> orderIds, PaymentStatus status);
    long countByStatus(PaymentStatus status);

    /**
     * Pagos en PENDING cuyo {@code createdAt} es anterior a {@code olderThan}.
     * Usado por el job de conciliación.
     */
    List<PaymentEntity> findByStatusAndCreatedAtBefore(PaymentStatus status, java.time.LocalDateTime olderThan);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentEntity p WHERE p.status = 'APPROVED'")
    BigDecimal sumApprovedRevenue();

    /** Sum of wallet credits returned to users from refunded payments. */
    @Query("SELECT COALESCE(SUM(p.walletAmountApplied), 0) FROM PaymentEntity p WHERE p.status = 'REFUNDED'")
    BigDecimal sumRefundedWalletCredits();

    /** Sum of total amount (gateway + wallet) from refunded payments. */
    @Query("SELECT COALESCE(SUM(p.amount + p.walletAmountApplied), 0) FROM PaymentEntity p WHERE p.status = 'REFUNDED'")
    BigDecimal sumTotalRefundedAmount();

    /** Most recent N payments ordered by processedAt — used for admin activity feed. */
    @Query("SELECT p FROM PaymentEntity p WHERE p.processedAt IS NOT NULL ORDER BY p.processedAt DESC")
    List<PaymentEntity> findTopByOrderByProcessedAtDesc(Pageable pageable);

    /**
     * Ganancias desglosadas de los eventos indicados.
     * Hace JOIN: payment → order filtrando por order.eventId y payment.status = APPROVED.
     * Retorna una proyección con (eventId, ticketsSold, totalRevenue, currency).
     * ticketsSold cuenta entradas (una por silla) pagadas o ya usadas, no pagos: una compra
     * de 4 sillas son 4 entradas, y las reembolsadas no cuentan.
     */
    @Query("""
            SELECT new com.capysoft.commerce.payment.application.dto.response.OrganizerEarningsByEventResponse(
                o.eventId,
                CAST((SELECT COUNT(t.ticketId) FROM TicketEntity t
                      WHERE t.eventId = o.eventId AND t.status IN ('PAID', 'USED')) AS int),
                COALESCE(SUM(p.amount + p.walletAmountApplied), 0),
                MAX(o.currency)
            )
            FROM PaymentEntity p
            JOIN OrderEntity o ON o.orderId = p.orderId
            WHERE o.eventId IN :eventIds
              AND p.status = 'APPROVED'
            GROUP BY o.eventId
            ORDER BY SUM(p.amount + p.walletAmountApplied) DESC
            """)
    List<OrganizerEarningsByEventResponse> findEarningsByEvents(@Param("eventIds") Collection<Long> eventIds);
}
