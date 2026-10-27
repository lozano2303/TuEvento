package com.capysoft.tuevento.modules.security.infrastructure.client;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.security.application.port.out.OrganizerDocumentPort;
import com.capysoft.tuevento.modules.storage.application.dto.request.StorageRequester;
import com.capysoft.tuevento.modules.storage.application.port.in.GeneratePublicUrlPort;

import lombok.RequiredArgsConstructor;

/** Único punto de Identidad que pide URLs a Archivos para los documentos de las solicitudes. */
@Component
@RequiredArgsConstructor
class StorageOrganizerDocumentAdapter implements OrganizerDocumentPort {

    private final GeneratePublicUrlPort generatePublicUrlPort;

    @Override
    public DocumentUrl publicUrl(Integer storedFileId, Integer requesterUserId, String requesterRole) {
        var url = generatePublicUrlPort.generate(storedFileId, StorageRequester.of(requesterUserId, requesterRole));
        return new DocumentUrl(url.getStoredFileId(), url.getPublicUrl());
    }
}
