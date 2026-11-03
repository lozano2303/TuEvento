package com.capysoft.commerce.wallet.domain.repository;

import com.capysoft.commerce.wallet.domain.model.WalletReference;

import java.util.List;

/**
 * Puerto de salida para persistencia de WalletReference.
 */
public interface WalletReferenceRepository {
    WalletReference save(WalletReference reference);
    List<WalletReference> findByTransactionId(Long transactionId);
}
