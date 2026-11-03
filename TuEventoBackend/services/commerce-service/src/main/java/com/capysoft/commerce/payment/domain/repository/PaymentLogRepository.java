package com.capysoft.commerce.payment.domain.repository;

import com.capysoft.commerce.payment.domain.model.PaymentLog;

import java.util.List;

/**
 * Repositorio del dominio para PaymentLog.
 */
public interface PaymentLogRepository {
    PaymentLog save(PaymentLog log);
    List<PaymentLog> findByPaymentId(Long paymentId);
}
