package com.capysoft.tuevento.modules.storage.application.usecase;

import com.capysoft.tuevento.modules.storage.application.dto.request.DeleteFileRequest;
import com.capysoft.tuevento.modules.storage.application.port.in.DeleteFilePort;
import com.capysoft.tuevento.modules.storage.application.port.out.StorageClientPort;
import com.capysoft.tuevento.modules.storage.domain.event.FileDeletedEvent;
import com.capysoft.tuevento.modules.storage.domain.model.StorageOperationLog;
import com.capysoft.tuevento.modules.storage.domain.model.StoredFile;
import com.capysoft.tuevento.modules.storage.domain.repository.StorageOperationLogRepository;
import com.capysoft.tuevento.modules.storage.domain.repository.StoredFileRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
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

    private final StoredFileRepository          storedFileRepository;
    private final StorageOperationLogRepository operationLogRepository;
    private final StorageClientPort             storageClient;
    private final ApplicationEventPublisher     eventPublisher;

    @Value("${app.storage.bucket-default}")
    private String defaultBucket;

    @Override
    @Transactional
    public void delete(DeleteFileRequest request) {
        StoredFile file = storedFileRepository.findById(request.getStoredFileId())
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new NotFoundException("FILE_NOT_FOUND", "File not found or already deleted"));

        storedFileRepository.softDelete(file.getStoredFileId());

        String status = "SUCCESS";
        String errorMessage = null;

        try {
            storageClient.deleteFile(defaultBucket, file.getS3Key());
        } catch (Exception e) {
            log.error("Failed to delete file from storage: {}", file.getS3Key(), e);
            status = "FAILED";
            errorMessage = e.getMessage();
        }

        operationLogRepository.save(StorageOperationLog.builder()
                .storedFile(file)
                .performedBy(request.getDeletedBy())
                .operation("DELETE")
                .status(status)
                .errorMessage(errorMessage)
                .occurredAt(LocalDateTime.now())
                .build());

        eventPublisher.publishEvent(FileDeletedEvent.builder()
                .storedFileId(file.getStoredFileId())
                .s3Key(file.getS3Key())
                .deletedBy(request.getDeletedBy())
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
