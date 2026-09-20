package com.capysoft.tuevento.modules.payment.domain.repository;

import com.capysoft.tuevento.modules.payment.domain.model.PaymentLog;

import java.util.List;

/**
 * Repositorio del dominio para PaymentLog.
 */
public interface PaymentLogRepository {
    PaymentLog save(PaymentLog log);
    List<PaymentLog> findByPaymentId(Long paymentId);
}
