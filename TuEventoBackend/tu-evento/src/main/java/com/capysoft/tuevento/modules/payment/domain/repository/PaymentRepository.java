package com.capysoft.tuevento.modules.payment.domain.repository;

import com.capysoft.tuevento.modules.payment.domain.model.Payment;

import java.util.Optional;

/**
 * Repositorio del dominio para Payment.
 */
public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(Long paymentId);
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);
    boolean existsById(Long paymentId);
}
