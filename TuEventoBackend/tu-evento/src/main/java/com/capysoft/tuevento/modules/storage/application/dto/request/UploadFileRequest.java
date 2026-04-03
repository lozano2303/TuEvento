package com.capysoft.tuevento.modules.storage.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadFileRequest {

    @NotBlank
    private String fileCategoryCode;

    @NotNull
    private Integer uploadedBy;

    @NotBlank
    private String originalFilename;

    @NotBlank
    private String contentType;

    @NotNull
    private byte[] content;

    private Integer ownerEntityId;
    private String ownerEntityType;
}
