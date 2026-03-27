package com.capysoft.tuevento.modules.security.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkOauthAccountRequest {

    @NotBlank
    @Size(max = 30)
    private String provider;

    @NotBlank
    @Size(max = 100)
    private String providerUserId;

    @Email
    @Size(max = 255)
    private String email;
}
