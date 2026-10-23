package com.capysoft.tuevento.modules.security.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestOrganizerRequest {

    @NotNull
    private MultipartFile document;

    /** Type of identity document submitted by the user. */
    private String documentType;
}
