package com.capysoft.identity.security.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RevokeOrganizerRoleRequest {

    @NotBlank
    @Size(min = 10, max = 500)
    private String reason;
}
