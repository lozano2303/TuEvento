package com.capysoft.tuevento.modules.storage.application.port.in;

import com.capysoft.tuevento.modules.storage.application.dto.request.StorageRequester;
import com.capysoft.tuevento.modules.storage.application.dto.response.StoredFileResponse;

import java.util.List;

public interface GetFilesByOwnerPort {

    /** Returns only the files the requester is allowed to read. */
    List<StoredFileResponse> getByOwner(Integer ownerEntityId, String ownerEntityType, StorageRequester requester);
}
