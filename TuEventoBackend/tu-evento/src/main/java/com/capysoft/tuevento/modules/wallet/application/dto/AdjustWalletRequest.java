package com.capysoft.tuevento.modules.wallet.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request body para ajuste administrativo de wallet (solo ADMIN).
 * amount puede ser positivo (suma) o negativo (resta).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdjustWalletRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "amount is required")
    private BigDecimal amount; // positivo o negativo, pero distinto de 0

    @NotBlank(message = "reason is required")
    private String reason;
}
