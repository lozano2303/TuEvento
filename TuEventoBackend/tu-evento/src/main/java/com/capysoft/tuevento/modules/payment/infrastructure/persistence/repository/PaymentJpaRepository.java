package com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;
import com.capysoft.tuevento.modules.payment.infrastructure.persistence.entity.PaymentEntity;

@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByOrderId(Long orderId);
    Optional<PaymentEntity> findByGatewayTransactionId(String gatewayTransactionId);
    List<PaymentEntity> findByOrderIdInAndStatus(List<Long> orderIds, PaymentStatus status);
    long countByStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentEntity p WHERE p.status = 'APPROVED'")
    BigDecimal sumApprovedRevenue();

    /** Most recent N payments ordered by processedAt — used for admin activity feed. */
    @Query("SELECT p FROM PaymentEntity p WHERE p.processedAt IS NOT NULL ORDER BY p.processedAt DESC")
    List<PaymentEntity> findTopByOrderByProcessedAtDesc(Pageable pageable);
}
