package com.capysoft.fakepaymentgateway.domain.repository;

import com.capysoft.fakepaymentgateway.domain.model.Payment;

import java.util.Optional;

/**
 * Interfaz pura del dominio para persistencia de pagos.
 * Las implementaciones de infraestructura deben adaptar JPA a este contrato.
 */
public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findByPaymentId(String paymentId);
    boolean existsByPaymentId(String paymentId);
}
