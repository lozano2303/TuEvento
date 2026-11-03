package com.capysoft.commerce.wallet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Respuesta del crédito masivo de wallet.
 *
 * usersProcessed — usuarios a los que se les acreditó exitosamente
 * skipped        — usuarios omitidos por idempotencia (ya tenían este jobId)
 * failed         — usuarios que fallaron (error inesperado, no bloquean el proceso)
 * jobId          — clave del job, úsala para reintentar sin duplicar créditos
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreditWalletResponse {
    private String     jobId;
    private int        usersProcessed;
    private int        skipped;
    private int        failed;
    private BigDecimal amountPerUser;
    private String     reason;
}
