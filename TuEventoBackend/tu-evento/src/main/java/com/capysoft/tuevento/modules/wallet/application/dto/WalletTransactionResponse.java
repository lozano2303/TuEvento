package com.capysoft.tuevento.modules.wallet.application.dto;

import com.capysoft.tuevento.modules.wallet.domain.model.WalletTransaction;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletTransactionStatus;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionResponse {
    private Long transactionId;
    private Long walletId;
    private WalletTransactionType type;
    private BigDecimal amount;
    private WalletTransactionStatus status;
    private BigDecimal balanceAfter;
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private String createdBy;

    public static WalletTransactionResponse fromDomain(WalletTransaction t) {
        return WalletTransactionResponse.builder()
            .transactionId(t.getTransactionId())
            .walletId(t.getWalletId())
            .type(t.getType())
            .amount(t.getAmount())
            .status(t.getStatus())
            .balanceAfter(t.getBalanceAfter())
            .idempotencyKey(t.getIdempotencyKey())
            .createdAt(t.getCreatedAt())
            .createdBy(t.getCreatedBy())
            .build();
    }
}
