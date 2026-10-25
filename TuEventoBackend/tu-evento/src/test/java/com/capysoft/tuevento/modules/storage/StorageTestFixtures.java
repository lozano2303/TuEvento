package com.capysoft.tuevento.modules.storage;

import com.capysoft.tuevento.modules.storage.application.dto.request.StorageRequester;
import com.capysoft.tuevento.modules.storage.domain.model.FileCategory;
import com.capysoft.tuevento.modules.storage.domain.model.StoredFile;

import java.time.LocalDateTime;

/** Shared fixtures for the storage access-control tests. */
public final class StorageTestFixtures {

    public static final Integer FILE_ID  = 10;
    public static final Integer OWNER_ID = 42;
    public static final Integer OTHER_ID = 99;
    public static final Integer ADMIN_ID = 1;

    public static final StorageRequester ANONYMOUS = StorageRequester.anonymous();
    public static final StorageRequester OWNER     = StorageRequester.of(OWNER_ID, "USER");
    public static final StorageRequester OTHER     = StorageRequester.of(OTHER_ID, "ORGANIZER");
    public static final StorageRequester ADMIN     = StorageRequester.of(ADMIN_ID, "ADMIN");

    private StorageTestFixtures() {
    }

    public static StoredFile profilePicture() {
        return file("PROFILE_PICTURE", true, "profile_picture/avatar.jpg");
    }

    public static StoredFile eventMedia() {
        return file("EVENT_MEDIA", true, "event_media/cover.jpg");
    }

    public static StoredFile organizerDocument() {
        return file("ORGANIZER_DOCUMENT", false, "organizer_document/id.pdf");
    }

    private static StoredFile file(String categoryCode, boolean isPublic, String s3Key) {
        return StoredFile.builder()
                .storedFileId(FILE_ID)
                .fileCategory(FileCategory.builder().code(categoryCode).isPublic(isPublic).build())
                .uploadedBy(OWNER_ID)
                .s3Key(s3Key)
                .originalFilename(s3Key.substring(s3Key.indexOf('/') + 1))
                .contentType(isPublic ? "image/jpeg" : "application/pdf")
                .sizeBytes(1024L)
                // Simulates a legacy row saved with the internal storage endpoint.
                .publicUrl(isPublic ? "http://minio:9000/tuevento/" + s3Key : null)
                .deleted(false)
                .uploadedAt(LocalDateTime.now())
                .build();
    }
}
