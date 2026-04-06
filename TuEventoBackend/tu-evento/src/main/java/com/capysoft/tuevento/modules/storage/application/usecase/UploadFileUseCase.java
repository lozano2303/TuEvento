package com.capysoft.tuevento.modules.storage.application.usecase;

import com.capysoft.tuevento.modules.storage.application.dto.request.UploadFileRequest;
import com.capysoft.tuevento.modules.storage.application.dto.response.UploadFileResponse;
import com.capysoft.tuevento.modules.storage.application.port.in.UploadFilePort;
import com.capysoft.tuevento.modules.storage.application.port.out.StorageClientPort;
import com.capysoft.tuevento.modules.storage.domain.event.FileUploadedEvent;
import com.capysoft.tuevento.modules.storage.domain.model.FileCategory;
import com.capysoft.tuevento.modules.storage.domain.model.StorageOperationLog;
import com.capysoft.tuevento.modules.storage.domain.model.StoredFile;
import com.capysoft.tuevento.modules.storage.domain.repository.FileCategoryRepository;
import com.capysoft.tuevento.modules.storage.domain.repository.StorageOperationLogRepository;
import com.capysoft.tuevento.modules.storage.domain.repository.StoredFileRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadFileUseCase implements UploadFilePort {

    private final FileCategoryRepository        fileCategoryRepository;
    private final StoredFileRepository          storedFileRepository;
    private final StorageOperationLogRepository operationLogRepository;
    private final StorageClientPort             storageClient;
    private final ApplicationEventPublisher     eventPublisher;

    @Value("${app.storage.bucket-default}")
    private String defaultBucket;

    @Override
    @Transactional
    public UploadFileResponse upload(UploadFileRequest request) {
        FileCategory category = fileCategoryRepository.findByCode(request.getFileCategoryCode())
                .orElseThrow(() -> new NotFoundException("FILE_CATEGORY_NOT_FOUND",
                        "File category not found: " + request.getFileCategoryCode()));

        validateExtension(request.getOriginalFilename(), category.getAllowedExtensions());
        validateSize(request.getContent().length, category.getMaxSizeMb());

        String extension = extractExtension(request.getOriginalFilename());
        String s3Key = category.getCode().toLowerCase() + "/" + UUID.randomUUID() + "." + extension;

        StoredFile savedFile;
        String publicUrl = null;

        try {
            storageClient.uploadFile(defaultBucket, s3Key, request.getContent(), request.getContentType());

            if (Boolean.TRUE.equals(category.getIsPublic())) {
                publicUrl = storageClient.generatePublicUrl(defaultBucket, s3Key);
            }

            savedFile = storedFileRepository.save(StoredFile.builder()
                    .fileCategory(category)
                    .uploadedBy(request.getUploadedBy())
                    .s3Key(s3Key)
                    .originalFilename(request.getOriginalFilename())
                    .contentType(request.getContentType())
                    .sizeBytes((long) request.getContent().length)
                    .publicUrl(publicUrl)
                    .ownerEntityId(request.getOwnerEntityId())
                    .ownerEntityType(request.getOwnerEntityType())
                    .deleted(false)
                    .uploadedAt(LocalDateTime.now())
                    .build());

            operationLogRepository.save(StorageOperationLog.builder()
                    .storedFile(savedFile)
                    .performedBy(request.getUploadedBy())
                    .operation("UPLOAD")
                    .status("SUCCESS")
                    .occurredAt(LocalDateTime.now())
                    .build());

        } catch (Exception e) {
            log.error("Failed to upload file: {}", request.getOriginalFilename(), e);
            throw new BusinessException("UPLOAD_FAILED", "Failed to upload file: " + e.getMessage());
        }

        eventPublisher.publishEvent(FileUploadedEvent.builder()
                .storedFileId(savedFile.getStoredFileId())
                .fileCategory(category.getCode())
                .uploadedBy(request.getUploadedBy())
                .s3Key(s3Key)
                .originalFilename(request.getOriginalFilename())
                .occurredAt(LocalDateTime.now())
                .build());

        return UploadFileResponse.builder()
                .storedFileId(savedFile.getStoredFileId())
                .s3Key(savedFile.getS3Key())
                .originalFilename(savedFile.getOriginalFilename())
                .contentType(savedFile.getContentType())
                .sizeBytes(savedFile.getSizeBytes())
                .publicUrl(savedFile.getPublicUrl())
                .build();
    }

    private void validateExtension(String filename, String allowedExtensions) {
        String ext = extractExtension(filename).toLowerCase();
        boolean allowed = Arrays.stream(allowedExtensions.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(ext::equals);
        if (!allowed) {
            throw new BusinessException("INVALID_FILE_EXTENSION",
                    "Extension '." + ext + "' is not allowed. Allowed: " + allowedExtensions);
        }
    }

    private void validateSize(int contentLengthBytes, Integer maxSizeMb) {
        long maxBytes = (long) maxSizeMb * 1024 * 1024;
        if (contentLengthBytes > maxBytes) {
            throw new BusinessException("FILE_TOO_LARGE",
                    "File exceeds maximum allowed size of " + maxSizeMb + " MB");
        }
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0 && dot < filename.length() - 1) ? filename.substring(dot + 1) : "";
    }
}
