package com.capysoft.storage.application.usecase;

import com.capysoft.storage.application.dto.request.StorageRequester;
import com.capysoft.storage.application.dto.request.UploadFileRequest;
import com.capysoft.storage.application.dto.response.UploadFileResponse;
import com.capysoft.storage.application.port.in.UploadFilePort;
import com.capysoft.storage.application.port.in.UploadUserFilePort;
import com.capysoft.storage.application.port.out.StorageClientPort;
import com.capysoft.storage.application.service.ImageModerationService;
import com.capysoft.storage.application.service.StorageAccessPolicy;
import com.capysoft.storage.domain.event.FileUploadedEvent;
import com.capysoft.storage.domain.model.FileCategory;
import com.capysoft.storage.domain.model.StorageOperationLog;
import com.capysoft.storage.domain.model.StoredFile;
import com.capysoft.storage.domain.repository.FileCategoryRepository;
import com.capysoft.storage.domain.repository.StorageOperationLogRepository;
import com.capysoft.storage.domain.repository.StoredFileRepository;
import com.capysoft.common.domain.exception.BusinessException;
import com.capysoft.common.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadFileUseCase implements UploadFilePort, UploadUserFilePort {

    /** Extensions moderated as images whatever Content-Type the client declares. */
    private static final Set<String> IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif");

    private final FileCategoryRepository        fileCategoryRepository;
    private final StoredFileRepository          storedFileRepository;
    private final StorageOperationLogRepository operationLogRepository;
    private final StorageClientPort             storageClient;
    private final ApplicationEventPublisher     eventPublisher;
    private final ImageModerationService        moderationService;
    private final StorageAccessPolicy           accessPolicy;

    @Value("${app.storage.bucket-default}")
    private String defaultBucket;

    @Override
    @Transactional
    public UploadFileResponse uploadAsUser(String fileCategoryCode, String originalFilename, String contentType,
                                           byte[] content, StorageRequester requester) {
        accessPolicy.assertCanUploadViaApi(fileCategoryCode, requester);

        // Ownership is never taken from the client: the uploader is the authenticated user.
        return upload(UploadFileRequest.builder()
                .fileCategoryCode(fileCategoryCode)
                .uploadedBy(requester.userId())
                .originalFilename(originalFilename)
                .contentType(contentType)
                .content(content)
                .build());
    }

    @Override
    @Transactional
    public UploadFileResponse upload(UploadFileRequest request) {
        FileCategory category = fileCategoryRepository.findByCode(request.getFileCategoryCode())
                .orElseThrow(() -> new NotFoundException("FILE_CATEGORY_NOT_FOUND",
                        "File category not found: " + request.getFileCategoryCode()));

        validateExtension(request.getOriginalFilename(), category.getAllowedExtensions());
        validateSize(request.getContent().length, category.getMaxSizeMb());

        // Moderation cascade: only for images, skipped for PDFs and other types. The Content-Type is
        // chosen by the client, so an image sent as application/octet-stream (or without the header)
        // is still recognized by its extension, which validateExtension has already checked.
        if (isImage(request.getContentType(), request.getOriginalFilename())) {
            moderationService.moderate(request.getContent());
        }

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

        } catch (BusinessException e) {
            // Already carries a safe code and message (e.g. S3_UPLOAD_FAILED from the storage client).
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload file: {}", request.getOriginalFilename(), e);
            throw new BusinessException("UPLOAD_FAILED", "Failed to upload file");
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

    private boolean isImage(String contentType, String filename) {
        return (contentType != null && contentType.toLowerCase().startsWith("image/"))
                || IMAGE_EXTENSIONS.contains(extractExtension(filename).toLowerCase());
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
