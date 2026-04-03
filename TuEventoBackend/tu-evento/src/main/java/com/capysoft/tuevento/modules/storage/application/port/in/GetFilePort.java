package com.capysoft.tuevento.modules.storage.application.port.in;

import com.capysoft.tuevento.modules.storage.application.dto.response.StoredFileResponse;

public interface GetFilePort {

    StoredFileResponse getById(Integer storedFileId);
}
