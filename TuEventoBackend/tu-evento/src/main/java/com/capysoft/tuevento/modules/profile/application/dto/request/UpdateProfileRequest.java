package com.capysoft.tuevento.modules.profile.application.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    private Integer cityId;
    private Integer storedFileId;

    @Size(max = 100)
    private String fullName;

    @Size(max = 255)
    private String bio;
}
