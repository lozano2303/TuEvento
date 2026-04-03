package com.capysoft.tuevento.modules.storage.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeleteFileRequest {

    @NotNull
    private Integer storedFileId;

    @NotNull
    private Integer deletedBy;
}
