package com.capysoft.tuevento.modules.event.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadEventMediaRequest {

    @NotNull
    private Long eventId;

    @NotNull
    private MultipartFile file;
}
