package com.capysoft.storage.infrastructure.config;

import com.capysoft.storage.application.dto.request.UploadFileRequest;
import com.capysoft.storage.application.port.in.UploadFilePort;
import com.capysoft.storage.application.port.out.StorageClientPort;
import com.capysoft.storage.domain.model.StoredFile;
import com.capysoft.storage.domain.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Sube el avatar por defecto de los perfiles al arrancar, después de crear el bucket. Si el
 * registro existe pero el objeto ya no (se recreó el volumen), lo vuelve a subir con la misma
 * clave para que el storedFileId que ya tienen los perfiles siga valiendo. Un fallo del
 * almacenamiento no impide arrancar: se reintenta en el próximo arranque.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class DefaultAvatarInitializer implements ApplicationRunner {

    public static final String OWNER_ENTITY_TYPE = "DEFAULT_AVATAR";
    public static final int    OWNER_ENTITY_ID   = 0;
    private static final String CATEGORY_CODE    = "PROFILE_PICTURE";
    private static final String AVATAR_PATH      = "assets/default-avatar.jpg";
    private static final String CONTENT_TYPE     = "image/jpeg";
    private static final int    SYSTEM_USER_ID   = 1;

    private final StoredFileRepository storedFileRepository;
    private final UploadFilePort       uploadFilePort;
    private final StorageClientPort    storageClient;

    @Value("${app.storage.bucket-default:tuevento}")
    private String bucket;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<StoredFile> existing = storedFileRepository.findByOwnerEntity(OWNER_ENTITY_ID, OWNER_ENTITY_TYPE);
            if (existing.isEmpty()) {
                upload();
            } else {
                restoreIfMissing(existing.get(0));
            }
        } catch (Exception e) {
            log.warn("Could not prepare the default avatar now: {}", e.getMessage());
        }
    }

    private void upload() throws IOException {
        var response = uploadFilePort.upload(UploadFileRequest.builder()
                .fileCategoryCode(CATEGORY_CODE)
                .uploadedBy(SYSTEM_USER_ID)
                .originalFilename("default-avatar.jpg")
                .contentType(CONTENT_TYPE)
                .content(avatar())
                .ownerEntityId(OWNER_ENTITY_ID)
                .ownerEntityType(OWNER_ENTITY_TYPE)
                .build());
        log.info("Default avatar uploaded (storedFileId={})", response.getStoredFileId());
    }

    private void restoreIfMissing(StoredFile record) throws IOException {
        String key = record.getS3Key();
        if (key != null && storageClient.objectExists(bucket, key)) {
            log.info("Default avatar already uploaded (storedFileId={})", record.getStoredFileId());
            return;
        }
        log.warn("Default avatar record {} has no object in the bucket: uploading it again with key {}",
                record.getStoredFileId(), key);
        storageClient.uploadFile(bucket, key, avatar(), CONTENT_TYPE);
    }

    private static byte[] avatar() throws IOException {
        try (var in = new ClassPathResource(AVATAR_PATH).getInputStream()) {
            return in.readAllBytes();
        }
    }
}
