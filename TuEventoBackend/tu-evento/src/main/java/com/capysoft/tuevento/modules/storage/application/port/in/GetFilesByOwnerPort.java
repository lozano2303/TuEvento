package com.capysoft.tuevento.modules.storage.application.port.in;

import com.capysoft.tuevento.modules.storage.application.dto.response.StoredFileResponse;

import java.util.List;

public interface GetFilesByOwnerPort {

    List<StoredFileResponse> getByOwner(Integer ownerEntityId, String ownerEntityType);
}
