package com.capysoft.tuevento.modules.storage.application.service;

import com.capysoft.tuevento.modules.storage.application.dto.response.StoredFileResponse;
import com.capysoft.tuevento.modules.storage.application.port.out.StorageClientPort;
import com.capysoft.tuevento.modules.storage.domain.model.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Builds {@link StoredFileResponse}. The public URL is derived on read from the configured public
 * host instead of trusting {@code stored_file.public_url}, which older rows saved with the internal
 * storage endpoint. Private files never expose a URL here; they use the presigned URL endpoint.
 */
@Component
@RequiredArgsConstructor
public class StoredFileResponseFactory {

    private final StorageClientPort   storageClient;
    private final StorageAccessPolicy accessPolicy;

    @Value("${app.storage.bucket-default}")
    private String defaultBucket;

    public StoredFileResponse toResponse(StoredFile file) {
        String publicUrl = accessPolicy.isPublic(file)
                ? storageClient.generatePublicUrl(defaultBucket, file.getS3Key())
                : null;

        return StoredFileResponse.builder()
                .storedFileId(file.getStoredFileId())
                .fileCategoryCode(file.getFileCategory().getCode())
                .uploadedBy(file.getUploadedBy())
                .s3Key(file.getS3Key())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSizeBytes())
                .publicUrl(publicUrl)
                .ownerEntityId(file.getOwnerEntityId())
                .ownerEntityType(file.getOwnerEntityType())
                .uploadedAt(file.getUploadedAt())
                .build();
    }
}
