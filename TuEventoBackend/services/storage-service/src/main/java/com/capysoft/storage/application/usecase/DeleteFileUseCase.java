package com.capysoft.storage.application.usecase;

import com.capysoft.storage.application.dto.request.DeleteFileRequest;
import com.capysoft.storage.application.port.in.DeleteFilePort;
import com.capysoft.storage.application.port.out.StorageClientPort;
import com.capysoft.storage.application.service.StorageAccessPolicy;
import com.capysoft.storage.domain.event.FileDeletedEvent;
import com.capysoft.storage.domain.model.StorageOperationLog;
import com.capysoft.storage.domain.model.StoredFile;
import com.capysoft.storage.domain.repository.StorageOperationLogRepository;
import com.capysoft.storage.domain.repository.StoredFileRepository;
import com.capysoft.common.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteFileUseCase implements DeleteFilePort {

    // storage_operation_log.error_message is VARCHAR(500).
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final StoredFileRepository          storedFileRepository;
    private final StorageOperationLogRepository operationLogRepository;
    private final StorageClientPort             storageClient;
    private final ApplicationEventPublisher     eventPublisher;
    private final StorageAccessPolicy           accessPolicy;

    @Value("${app.storage.bucket-default}")
    private String defaultBucket;

    @Override
    @Transactional
    public void delete(DeleteFileRequest request) {
        StoredFile file = storedFileRepository.findById(request.getStoredFileId())
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new NotFoundException("FILE_NOT_FOUND", "File not found or already deleted"));

        accessPolicy.assertCanDelete(file, request.getRequester());
        Integer deletedBy = request.getRequester().userId();

        storedFileRepository.softDelete(file.getStoredFileId());

        String status = "SUCCESS";
        String errorMessage = null;

        try {
            storageClient.deleteFile(defaultBucket, file.getS3Key());
        } catch (Exception e) {
            log.error("Failed to delete file from storage: {}", file.getS3Key(), e);
            status = "FAILED";
            errorMessage = truncate(e.getMessage());
        }

        operationLogRepository.save(StorageOperationLog.builder()
                .storedFile(file)
                .performedBy(deletedBy)
                .operation("DELETE")
                .status(status)
                .errorMessage(errorMessage)
                .occurredAt(LocalDateTime.now())
                .build());

        eventPublisher.publishEvent(FileDeletedEvent.builder()
                .storedFileId(file.getStoredFileId())
                .s3Key(file.getS3Key())
                .deletedBy(deletedBy)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private static String truncate(String message) {
        if (message == null || message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
