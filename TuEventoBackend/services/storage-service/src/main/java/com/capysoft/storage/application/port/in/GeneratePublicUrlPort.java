package com.capysoft.storage.application.port.in;

import com.capysoft.storage.application.dto.request.StorageRequester;
import com.capysoft.storage.application.dto.response.PublicUrlResponse;

public interface GeneratePublicUrlPort {

    PublicUrlResponse generate(Integer storedFileId, StorageRequester requester);
}
