package com.capysoft.tuevento.modules.security.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeactivateAccountRequest {

    @NotBlank(message = "Password is required")
    private String password;
}
