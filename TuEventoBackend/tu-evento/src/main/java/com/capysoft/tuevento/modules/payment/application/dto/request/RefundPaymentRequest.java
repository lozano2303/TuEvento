package com.capysoft.tuevento.modules.payment.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body para solicitar el reembolso de un pago.
 */
public record RefundPaymentRequest(

    @NotBlank(message = "La razón del reembolso es obligatoria")
    @Size(max = 500, message = "La razón no puede superar 500 caracteres")
    String reason
) {}
