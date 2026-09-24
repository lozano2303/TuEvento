package com.capysoft.tuevento.modules.wallet.domain.model;

/**
 * Lanzada cuando una operación intentaría dejar el balance de la wallet negativo.
 */
public class InsufficientWalletBalanceException extends RuntimeException {
    public InsufficientWalletBalanceException(String message) {
        super(message);
    }
}
