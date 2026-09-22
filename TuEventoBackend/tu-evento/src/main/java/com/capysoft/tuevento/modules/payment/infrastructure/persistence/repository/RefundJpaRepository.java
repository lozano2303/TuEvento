package com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.payment.infrastructure.persistence.entity.RefundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundJpaRepository extends JpaRepository<RefundEntity, Long> {
    Optional<RefundEntity> findByPaymentId(Long paymentId);
}
