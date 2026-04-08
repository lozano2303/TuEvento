package com.capysoft.tuevento.shared.infrastructure.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.profile.domain.model.Profile;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
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
    private final ProfileRepository    profileRepository;

    @Value("${app.profile.default-avatar-stored-file-id:1}")
    private Integer defaultAvatarStoredFileId;

    @Override
    public void run(ApplicationArguments args) {
        Integer avatarStoredFileId = resolveAvatarStoredFileId();

        if (avatarStoredFileId != null) {
            assignAvatarToProfilesWithNullStoredFileId(avatarStoredFileId);
        }
    }

    /**
     * Returns the storedFileId of the default avatar.
     * Uploads it to MinIO if it doesn't exist yet.
     * Returns null if MinIO is unavailable.
     */
    private Integer resolveAvatarStoredFileId() {
        List<?> existing = storedFileRepository.findByOwnerEntity(OWNER_ENTITY_ID, OWNER_ENTITY_TYPE);

        if (!existing.isEmpty()) {
            log.info("Default avatar already uploaded (storedFileId={}), skipping upload",
                    defaultAvatarStoredFileId);
            return defaultAvatarStoredFileId;
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
            return response.getStoredFileId();

        } catch (Exception e) {
            log.warn("Could not upload default avatar (MinIO may not be running): {}", e.getMessage());
            return null;
        }
    }

    /**
     * Assigns the default avatar to all profiles that have storedFileId=null.
     * Covers profiles created before MinIO was available (e.g. admin on first run).
     */
    private void assignAvatarToProfilesWithNullStoredFileId(Integer avatarStoredFileId) {
        List<Profile> profiles = profileRepository.findAllWithNullStoredFileId();
        if (profiles.isEmpty()) {
            log.info("No profiles with null storedFileId found, skipping avatar assignment");
            return;
        }

        log.info("Assigning default avatar (storedFileId={}) to {} profile(s) with null storedFileId",
                avatarStoredFileId, profiles.size());

        for (Profile profile : profiles) {
            profile.setStoredFileId(avatarStoredFileId);
            profileRepository.save(profile);
        }

        log.info("Default avatar assigned to {} profile(s)", profiles.size());
    }
}
