package com.capysoft.storage.application.usecase;

import com.capysoft.storage.application.dto.request.StorageRequester;
import com.capysoft.storage.application.dto.response.PublicUrlResponse;
import com.capysoft.storage.application.port.in.GeneratePublicUrlPort;
import com.capysoft.storage.application.port.out.StorageClientPort;
import com.capysoft.storage.application.service.StorageAccessPolicy;
import com.capysoft.storage.domain.event.FileUrlGeneratedEvent;
import com.capysoft.storage.domain.model.StorageOperationLog;
import com.capysoft.storage.domain.model.StoredFile;
import com.capysoft.storage.domain.repository.StorageOperationLogRepository;
import com.capysoft.storage.domain.repository.StoredFileRepository;
import com.capysoft.common.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GeneratePublicUrlUseCase implements GeneratePublicUrlPort {

    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 60;

    // Sentinel for "system" in storage_operation_log.performed_by (NOT NULL) when nobody is logged in.
    private static final int SYSTEM_USER_ID = 0;

    private final StoredFileRepository          storedFileRepository;
    private final StorageOperationLogRepository operationLogRepository;
    private final StorageClientPort             storageClient;
    private final ApplicationEventPublisher     eventPublisher;
    private final StorageAccessPolicy           accessPolicy;

    @Value("${app.storage.bucket-default}")
    private String defaultBucket;

    @Override
    @Transactional
    public PublicUrlResponse generate(Integer storedFileId, StorageRequester requester) {
        StoredFile file = storedFileRepository.findById(storedFileId)
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new NotFoundException("FILE_NOT_FOUND", "File not found or has been deleted"));

        accessPolicy.assertCanRead(file, requester);

        Integer performedBy = requester.isAnonymous() ? SYSTEM_USER_ID : requester.userId();

        String url = storageClient.generatePresignedUrl(defaultBucket, file.getS3Key(), PRESIGNED_URL_EXPIRATION_MINUTES);

        operationLogRepository.save(StorageOperationLog.builder()
                .storedFile(file)
                .performedBy(performedBy)
                .operation("URL_GENERATED")
                .status("SUCCESS")
                .occurredAt(LocalDateTime.now())
                .build());

        eventPublisher.publishEvent(FileUrlGeneratedEvent.builder()
                .storedFileId(file.getStoredFileId())
                .publicUrl(url)
                .generatedBy(performedBy)
                .occurredAt(LocalDateTime.now())
                .build());

        return PublicUrlResponse.builder()
                .storedFileId(file.getStoredFileId())
                .publicUrl(url)
                .build();
    }
}
