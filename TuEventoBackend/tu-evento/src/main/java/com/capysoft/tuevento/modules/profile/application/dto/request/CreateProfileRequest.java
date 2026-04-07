package com.capysoft.tuevento.modules.profile.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProfileRequest {

    @NotNull
    private Integer userId;

    @NotBlank
    @Size(max = 100)
    private String fullName;
}
