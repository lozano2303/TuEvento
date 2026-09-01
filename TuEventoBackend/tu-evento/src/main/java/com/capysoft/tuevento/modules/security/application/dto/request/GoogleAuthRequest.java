package com.capysoft.tuevento.modules.security.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthRequest {

    /** Raw ID Token (JWT) issued by Google Identity Services to the frontend. */
    @NotBlank
    private String idToken;
}
