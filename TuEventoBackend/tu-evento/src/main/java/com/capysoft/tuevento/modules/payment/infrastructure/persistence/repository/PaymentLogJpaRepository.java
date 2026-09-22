package com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.payment.infrastructure.persistence.entity.PaymentLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentLogJpaRepository extends JpaRepository<PaymentLogEntity, Long> {
    List<PaymentLogEntity> findByPaymentId(Long paymentId);
}
