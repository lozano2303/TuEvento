package com.capysoft.tuevento.modules.security.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmReactivationRequest {

    @NotBlank
    @Size(min = 8, max = 8)
    private String token;
}
