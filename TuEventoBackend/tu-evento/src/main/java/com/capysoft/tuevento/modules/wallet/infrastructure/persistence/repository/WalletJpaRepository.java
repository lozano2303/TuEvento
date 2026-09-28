package com.capysoft.tuevento.modules.wallet.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.wallet.infrastructure.persistence.entity.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletJpaRepository extends JpaRepository<WalletEntity, Long> {
    Optional<WalletEntity> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
