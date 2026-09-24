package com.capysoft.tuevento.modules.wallet.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.wallet.domain.model.Wallet;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletRepository;
import com.capysoft.tuevento.modules.wallet.infrastructure.persistence.entity.WalletEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WalletRepositoryImpl implements WalletRepository {

    private final WalletJpaRepository jpaRepository;

    @Override
    public Wallet save(Wallet wallet) {
        WalletEntity entity = toEntity(wallet);
        WalletEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Wallet> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public Optional<Wallet> findById(Long walletId) {
        return jpaRepository.findById(walletId).map(this::toDomain);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return jpaRepository.existsByUserId(userId);
    }

    private WalletEntity toEntity(Wallet wallet) {
        return WalletEntity.builder()
            .walletId(wallet.getWalletId())
            .userId(wallet.getUserId())
            .balance(wallet.getBalance())
            .currency(wallet.getCurrency())
            .version(wallet.getVersion())
            .build();
    }

    private Wallet toDomain(WalletEntity entity) {
        return Wallet.builder()
            .walletId(entity.getWalletId())
            .userId(entity.getUserId())
            .balance(entity.getBalance())
            .currency(entity.getCurrency())
            .version(entity.getVersion())
            .build();
    }
}
