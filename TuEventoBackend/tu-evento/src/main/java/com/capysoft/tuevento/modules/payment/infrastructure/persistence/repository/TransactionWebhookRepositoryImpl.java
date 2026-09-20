package com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.payment.domain.model.TransactionWebhook;
import com.capysoft.tuevento.modules.payment.domain.repository.TransactionWebhookRepository;
import com.capysoft.tuevento.modules.payment.infrastructure.persistence.entity.TransactionWebhookEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TransactionWebhookRepositoryImpl implements TransactionWebhookRepository {
    
    private final TransactionWebhookJpaRepository jpaRepository;
    
    @Override
    public TransactionWebhook save(TransactionWebhook webhook) {
        TransactionWebhookEntity entity = toEntity(webhook);
        TransactionWebhookEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }
    
    @Override
    public Optional<TransactionWebhook> findByGatewayEventId(String gatewayEventId) {
        return jpaRepository.findByGatewayEventId(gatewayEventId).map(this::toDomain);
    }
    
    @Override
    public boolean existsByGatewayEventId(String gatewayEventId) {
        return jpaRepository.existsByGatewayEventId(gatewayEventId);
    }
    
    private TransactionWebhookEntity toEntity(TransactionWebhook webhook) {
        return TransactionWebhookEntity.builder()
            .webhookId(webhook.getWebhookId())
            .paymentId(webhook.getPaymentId())
            .gatewayEventId(webhook.getGatewayEventId())
            .payload(webhook.getPayload())
            .receivedAt(webhook.getReceivedAt())
            .build();
    }
    
    private TransactionWebhook toDomain(TransactionWebhookEntity entity) {
        return TransactionWebhook.builder()
            .webhookId(entity.getWebhookId())
            .paymentId(entity.getPaymentId())
            .gatewayEventId(entity.getGatewayEventId())
            .payload(entity.getPayload())
            .receivedAt(entity.getReceivedAt())
            .build();
    }
}
