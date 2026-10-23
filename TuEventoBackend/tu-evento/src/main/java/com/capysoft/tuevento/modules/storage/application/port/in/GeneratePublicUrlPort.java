package com.capysoft.tuevento.modules.storage.application.port.in;

import com.capysoft.tuevento.modules.storage.application.dto.response.PublicUrlResponse;

public interface GeneratePublicUrlPort {

    PublicUrlResponse generate(Integer storedFileId);
}
