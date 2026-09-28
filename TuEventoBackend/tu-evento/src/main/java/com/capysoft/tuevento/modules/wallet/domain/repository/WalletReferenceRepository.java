package com.capysoft.tuevento.modules.wallet.domain.repository;

import com.capysoft.tuevento.modules.wallet.domain.model.WalletReference;

import java.util.List;

/**
 * Puerto de salida para persistencia de WalletReference.
 */
public interface WalletReferenceRepository {
    WalletReference save(WalletReference reference);
    List<WalletReference> findByTransactionId(Long transactionId);
}
