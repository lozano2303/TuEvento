package com.capysoft.tuevento.modules.security.infrastructure.client;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.security.application.port.out.OrganizerDocumentPort;
import com.capysoft.tuevento.modules.storage.application.dto.request.StorageRequester;
import com.capysoft.tuevento.modules.storage.application.dto.request.UploadFileRequest;
import com.capysoft.tuevento.modules.storage.application.port.in.GeneratePublicUrlPort;
import com.capysoft.tuevento.modules.storage.application.port.in.UploadFilePort;

import lombok.RequiredArgsConstructor;

/** Único punto de Identidad que conoce a Archivos: sube documentos y pide URLs. Al separar los servicios será un cliente REST. */
@Component
@RequiredArgsConstructor
class StorageOrganizerDocumentAdapter implements OrganizerDocumentPort {

    private final GeneratePublicUrlPort generatePublicUrlPort;
    private final UploadFilePort        uploadFilePort;

    @Override
    public Integer upload(DocumentUpload upload) {
        return uploadFilePort.upload(UploadFileRequest.builder()
                .fileCategoryCode(upload.fileCategoryCode())
                .uploadedBy(upload.uploadedBy())
                .originalFilename(upload.originalFilename())
                .contentType(upload.contentType())
                .content(upload.content())
                .ownerEntityType(upload.ownerEntityType())
                .build()).getStoredFileId();
    }

    @Override
    public DocumentUrl publicUrl(Integer storedFileId, Integer requesterUserId, String requesterRole) {
        var url = generatePublicUrlPort.generate(storedFileId, StorageRequester.of(requesterUserId, requesterRole));
        return new DocumentUrl(url.getStoredFileId(), url.getPublicUrl());
    }
}
