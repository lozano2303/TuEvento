package com.capysoft.tuevento.modules.payment.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request para crear un pago.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    private String paymentMethod; // Opcional, por ahora usa QR por defecto
}
