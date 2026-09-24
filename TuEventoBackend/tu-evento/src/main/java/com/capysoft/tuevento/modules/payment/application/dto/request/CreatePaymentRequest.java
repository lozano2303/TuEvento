package com.capysoft.tuevento.modules.payment.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    private String paymentMethod; // Opcional, por defecto QR

    /**
     * Si true, el orquestador consulta el saldo disponible y aplica
     * crédito de wallet antes de cobrar por pasarela.
     * Default: false — flujo idéntico al anterior.
     */
    private boolean applyWalletCredit = false;
}
