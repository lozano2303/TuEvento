package com.capysoft.commerce.wallet.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request para acreditar saldo a TODOS los usuarios de la plataforma.
 * Requiere rol ADMIN.
 *
 * El campo jobId es opcional — si no se envía, el backend genera uno.
 * Si se reutiliza el mismo jobId, la operación es idempotente:
 * ningún usuario recibirá doble crédito por el mismo job.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreditWalletRequest {

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "reason is required")
    private String reason;

    /** Clave de idempotencia opcional. Si null, el backend genera un UUID. */
    private String jobId;
}
