package com.capysoft.commerce.ticket.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request para escanear/validar un token QR (lo envía el organizador).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScanQrRequest {

    @NotBlank(message = "qrToken is required")
    private String qrToken;
}
