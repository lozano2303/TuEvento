package com.capysoft.tuevento.shared.infrastructure.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.storage.application.dto.request.UploadFileRequest;
import com.capysoft.tuevento.modules.storage.application.port.in.UploadFilePort;
import com.capysoft.tuevento.modules.storage.domain.repository.StoredFileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ProfileDataInitializer implements ApplicationRunner {

    private static final String OWNER_ENTITY_TYPE = "DEFAULT_AVATAR";
    private static final int    OWNER_ENTITY_ID   = 0;
    private static final String CATEGORY_CODE     = "PROFILE_PICTURE";
    private static final String AVATAR_PATH       = "assets/default-avatar.jpg";

    private final StoredFileRepository storedFileRepository;
    private final UploadFilePort       uploadFilePort;

    @Override
    public void run(ApplicationArguments args) {
        boolean alreadyExists = !storedFileRepository
                .findByOwnerEntity(OWNER_ENTITY_ID, OWNER_ENTITY_TYPE)
                .isEmpty();

        if (alreadyExists) {
            log.info("Default avatar already uploaded, skipping ProfileDataInitializer");
            return;
        }

        log.info("Uploading default avatar to MinIO...");

        try {
            ClassPathResource resource = new ClassPathResource(AVATAR_PATH);
            byte[] content = resource.getInputStream().readAllBytes();

            var response = uploadFilePort.upload(UploadFileRequest.builder()
                    .fileCategoryCode(CATEGORY_CODE)
                    .uploadedBy(1)
                    .originalFilename("default-avatar.jpg")
                    .contentType("image/jpeg")
                    .content(content)
                    .ownerEntityId(OWNER_ENTITY_ID)
                    .ownerEntityType(OWNER_ENTITY_TYPE)
                    .build());

            log.info("Default avatar uploaded successfully. storedFileId={}, url={}",
                    response.getStoredFileId(), response.getPublicUrl());
            log.info("Set app.profile.default-avatar-stored-file-id={} in application.yml",
                    response.getStoredFileId());

        } catch (Exception e) {
            log.warn("Could not upload default avatar (MinIO may not be running): {}", e.getMessage());
        }
    }
}
