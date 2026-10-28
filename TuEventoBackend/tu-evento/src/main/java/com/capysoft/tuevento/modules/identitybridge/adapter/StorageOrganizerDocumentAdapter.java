package com.capysoft.tuevento.modules.identitybridge.adapter;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.identitybridge.port.OrganizerDocumentPort;
import com.capysoft.tuevento.shared.application.storage.FileStorage;

import lombok.RequiredArgsConstructor;

/** Único punto de Identidad que conoce a Archivos: sube documentos y pide URLs al servicio de archivos. */
@Component
@RequiredArgsConstructor
class StorageOrganizerDocumentAdapter implements OrganizerDocumentPort {

    private final FileStorage fileStorage;

    @Override
    public Integer upload(DocumentUpload upload) {
        return fileStorage.upload(new FileStorage.FileUpload(
                upload.fileCategoryCode(),
                upload.uploadedBy(),
                upload.originalFilename(),
                upload.contentType(),
                upload.content(),
                upload.ownerEntityType(),
                null)).storedFileId();
    }

    @Override
    public DocumentUrl publicUrl(Integer storedFileId, Integer requesterUserId, String requesterRole) {
        var url = fileStorage.publicUrl(storedFileId, requesterUserId, requesterRole);
        return new DocumentUrl(url.storedFileId(), url.publicUrl());
    }
}
