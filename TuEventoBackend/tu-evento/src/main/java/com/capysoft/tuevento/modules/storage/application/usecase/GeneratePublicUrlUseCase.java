package com.capysoft.tuevento.modules.storage.application.usecase;

import com.capysoft.tuevento.modules.storage.application.dto.response.PublicUrlResponse;
import com.capysoft.tuevento.modules.storage.application.port.in.GeneratePublicUrlPort;
import com.capysoft.tuevento.modules.storage.application.port.out.StorageClientPort;
import com.capysoft.tuevento.modules.storage.domain.event.FileUrlGeneratedEvent;
import com.capysoft.tuevento.modules.storage.domain.model.StorageOperationLog;
import com.capysoft.tuevento.modules.storage.domain.model.StoredFile;
import com.capysoft.tuevento.modules.storage.domain.repository.StorageOperationLogRepository;
import com.capysoft.tuevento.modules.storage.domain.repository.StoredFileRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GeneratePublicUrlUseCase implements GeneratePublicUrlPort {

    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 60;

    private final StoredFileRepository          storedFileRepository;
    private final StorageOperationLogRepository operationLogRepository;
    private final StorageClientPort             storageClient;
    private final ApplicationEventPublisher     eventPublisher;

    @Value("${app.storage.bucket-default}")
    private String defaultBucket;

    @Override
    public PublicUrlResponse generate(Integer storedFileId) {
        StoredFile file = storedFileRepository.findById(storedFileId)
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new NotFoundException("FILE_NOT_FOUND", "File not found or has been deleted"));

        Integer performedBy = resolveCurrentUserId();

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

    private Integer resolveCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser securityUser) {
            return securityUser.getUserId();
        }
        return null;
    }
}
