package com.capysoft.tuevento.modules.storage.application.port.in;

import com.capysoft.tuevento.modules.storage.application.dto.response.StoredFileResponse;

import java.util.List;

public interface GetFilesByOwnerPort {

    /** Devuelve solo los archivos de la entidad que el solicitante puede ver. */
    List<StoredFileResponse> getByOwner(Integer ownerEntityId, String ownerEntityType,
                                        Integer requesterId, boolean requesterIsAdmin);
}
