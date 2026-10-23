package com.capysoft.tuevento.modules.wallet.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.wallet.domain.model.WalletReference;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletReferenceRepository;
import com.capysoft.tuevento.modules.wallet.infrastructure.persistence.entity.WalletReferenceEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WalletReferenceRepositoryImpl implements WalletReferenceRepository {

    private final WalletReferenceJpaRepository jpaRepository;

    @Override
    public WalletReference save(WalletReference reference) {
        WalletReferenceEntity entity = toEntity(reference);
        WalletReferenceEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<WalletReference> findByTransactionId(Long transactionId) {
        return jpaRepository.findByTransactionId(transactionId)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    private WalletReferenceEntity toEntity(WalletReference r) {
        return WalletReferenceEntity.builder()
            .walletReferenceId(r.getWalletReferenceId())
            .transactionId(r.getTransactionId())
            .entityType(r.getEntityType())
            .entityId(r.getEntityId())
            .build();
    }

    private WalletReference toDomain(WalletReferenceEntity e) {
        return WalletReference.builder()
            .walletReferenceId(e.getWalletReferenceId())
            .transactionId(e.getTransactionId())
            .entityType(e.getEntityType())
            .entityId(e.getEntityId())
            .build();
    }
}
