package com.capysoft.tuevento.modules.storage.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadFileResponse {

    private Integer storedFileId;
    private String s3Key;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private String publicUrl;
}
