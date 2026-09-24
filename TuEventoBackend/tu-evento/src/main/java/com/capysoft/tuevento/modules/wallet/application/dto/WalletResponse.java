package com.capysoft.tuevento.modules.wallet.application.dto;

import com.capysoft.tuevento.modules.wallet.domain.model.Wallet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Respuesta de consulta de wallet.
 * Incluye balance total y balance disponible (balance - PAYMENTs PENDING).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private Long walletId;
    private Long userId;
    private BigDecimal balance;          // Balance total confirmado
    private BigDecimal availableBalance; // Balance - PAYMENTs PENDING
    private String currency;

    public static WalletResponse fromDomain(Wallet wallet, BigDecimal pendingPayments) {
        BigDecimal available = wallet.getBalance().subtract(pendingPayments);
        return WalletResponse.builder()
            .walletId(wallet.getWalletId())
            .userId(wallet.getUserId())
            .balance(wallet.getBalance())
            .availableBalance(available.max(BigDecimal.ZERO))
            .currency(wallet.getCurrency())
            .build();
    }
}
