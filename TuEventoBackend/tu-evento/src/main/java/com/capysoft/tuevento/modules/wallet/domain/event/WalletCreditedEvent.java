package com.capysoft.tuevento.modules.wallet.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletCreditedEvent {
    private Long walletTransactionId;
    private Integer userId;
    private BigDecimal amount;
    private String currency;
    private String reason;
}