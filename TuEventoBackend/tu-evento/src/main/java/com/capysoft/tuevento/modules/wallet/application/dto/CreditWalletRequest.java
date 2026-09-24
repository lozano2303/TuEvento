package com.capysoft.tuevento.modules.wallet.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request body para acreditar saldo manualmente (solo ADMIN, uso en pruebas/ops).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreditWalletRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "reason is required")
    private String reason;
}
