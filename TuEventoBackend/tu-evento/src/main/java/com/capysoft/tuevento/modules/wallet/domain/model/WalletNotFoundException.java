package com.capysoft.tuevento.modules.wallet.domain.model;

import com.capysoft.tuevento.shared.domain.exception.NotFoundException;

/**
 * Lanzada cuando no se encuentra una wallet para el usuario dado.
 * Extiende NotFoundException para ser manejada automáticamente como 404 por GlobalExceptionHandler.
 */
public class WalletNotFoundException extends NotFoundException {

    public WalletNotFoundException(Long userId) {
        super("WALLET_NOT_FOUND", "Wallet not found for userId: " + userId);
    }

    public WalletNotFoundException(String message) {
        super("WALLET_NOT_FOUND", message);
    }
}
