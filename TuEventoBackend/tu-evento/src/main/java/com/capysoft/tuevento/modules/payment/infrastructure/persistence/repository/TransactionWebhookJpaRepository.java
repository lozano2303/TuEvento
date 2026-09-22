package com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.payment.infrastructure.persistence.entity.TransactionWebhookEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionWebhookJpaRepository extends JpaRepository<TransactionWebhookEntity, Long> {
    Optional<TransactionWebhookEntity> findByGatewayEventId(String gatewayEventId);
    boolean existsByGatewayEventId(String gatewayEventId);
}
