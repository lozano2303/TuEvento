package com.capysoft.tuevento.modules.wallet.domain.repository;

import com.capysoft.tuevento.modules.wallet.domain.model.Wallet;

import java.util.Optional;

/**
 * Puerto de salida para persistencia del aggregate Wallet.
 */
public interface WalletRepository {
    Wallet save(Wallet wallet);
    Optional<Wallet> findByUserId(Long userId);
    Optional<Wallet> findById(Long walletId);
    boolean existsByUserId(Long userId);
}
