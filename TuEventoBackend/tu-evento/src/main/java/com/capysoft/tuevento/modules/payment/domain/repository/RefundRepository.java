package com.capysoft.tuevento.modules.payment.domain.repository;

import com.capysoft.tuevento.modules.payment.domain.model.Refund;

import java.util.Optional;

/**
 * Repositorio del dominio para Refund.
 * Estructura lista para fase futura (sin casos de uso implementados todavía).
 */
public interface RefundRepository {
    Refund save(Refund refund);
    Optional<Refund> findById(Long refundId);
    Optional<Refund> findByPaymentId(Long paymentId);
}
