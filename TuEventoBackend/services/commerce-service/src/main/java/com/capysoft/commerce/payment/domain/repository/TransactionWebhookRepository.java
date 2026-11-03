package com.capysoft.commerce.payment.domain.repository;

import com.capysoft.commerce.payment.domain.model.TransactionWebhook;

import java.util.Optional;

/**
 * Repositorio del dominio para TransactionWebhook.
 */
public interface TransactionWebhookRepository {
    TransactionWebhook save(TransactionWebhook webhook);
    Optional<TransactionWebhook> findByGatewayEventId(String gatewayEventId);
    boolean existsByGatewayEventId(String gatewayEventId);
}
