package com.capysoft.tuevento.modules.storage.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicUrlResponse {

    private Integer storedFileId;
    private String publicUrl;
}
