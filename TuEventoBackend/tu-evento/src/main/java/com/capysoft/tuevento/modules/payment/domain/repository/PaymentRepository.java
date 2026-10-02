package com.capysoft.tuevento.modules.payment.domain.repository;

import java.util.List;
import java.util.Optional;

import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;

/**
 * Repositorio del dominio para Payment.
 */
public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(Long paymentId);
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);
    boolean existsById(Long paymentId);
    List<Payment> findByOrderIdInAndStatus(List<Long> orderIds, PaymentStatus status);
}
