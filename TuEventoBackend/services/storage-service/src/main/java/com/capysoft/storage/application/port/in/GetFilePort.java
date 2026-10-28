package com.capysoft.storage.application.port.in;

import com.capysoft.storage.application.dto.request.StorageRequester;
import com.capysoft.storage.application.dto.response.StoredFileResponse;

public interface GetFilePort {

    StoredFileResponse getById(Integer storedFileId, StorageRequester requester);
}
