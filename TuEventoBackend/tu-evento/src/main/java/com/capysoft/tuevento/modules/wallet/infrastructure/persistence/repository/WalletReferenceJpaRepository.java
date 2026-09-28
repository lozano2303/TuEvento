package com.capysoft.tuevento.modules.wallet.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.wallet.infrastructure.persistence.entity.WalletReferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletReferenceJpaRepository extends JpaRepository<WalletReferenceEntity, Long> {
    List<WalletReferenceEntity> findByTransactionId(Long transactionId);
}
