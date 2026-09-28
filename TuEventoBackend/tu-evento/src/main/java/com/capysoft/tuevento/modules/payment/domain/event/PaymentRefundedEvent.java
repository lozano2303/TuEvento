package com.capysoft.tuevento.modules.payment.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefundedEvent {
    private Long paymentId;
    private Integer userId;
    private BigDecimal walletAmount;
    private BigDecimal gatewayAmount;
    private String currency;
}