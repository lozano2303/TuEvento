package com.capysoft.fakepaymentgateway.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para logs de entrega webhook.
 */
@Repository
public interface WebhookDeliveryLogRepository extends JpaRepository<WebhookDeliveryLogEntity, Long> {
}
