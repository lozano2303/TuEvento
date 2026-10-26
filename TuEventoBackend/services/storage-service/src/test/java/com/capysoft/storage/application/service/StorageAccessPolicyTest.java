package com.capysoft.storage.application.service;

import com.capysoft.storage.application.dto.request.StorageRequester;
import com.capysoft.storage.domain.model.StoredFile;
import com.capysoft.common.domain.exception.ForbiddenException;
import com.capysoft.common.domain.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.capysoft.storage.StorageTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link StorageAccessPolicy}: every rule against anonymous, owner,
 * another user and ADMIN.
 */
@DisplayName("StorageAccessPolicy")
class StorageAccessPolicyTest {

    private final StorageAccessPolicy policy = new StorageAccessPolicy();

    private void assertAllowed(Runnable action) {
        assertThatCode(action::run).doesNotThrowAnyException();
    }

    private void assertUnauthorized(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(UnauthorizedException.class)
                .hasFieldOrPropertyWithValue("code", "STORAGE_AUTHENTICATION_REQUIRED");
    }

    private void assertForbidden(Runnable action, String code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ForbiddenException.class)
                .hasFieldOrPropertyWithValue("code", code);
    }

    @Nested
    @DisplayName("Read a public file (profile picture / event media)")
    class ReadPublic {

        @Test
        @DisplayName("anonymous, owner, other user and ADMIN can read it")
        void everyoneCanRead() {
            for (StoredFile file : new StoredFile[]{profilePicture(), eventMedia()}) {
                for (StorageRequester requester : new StorageRequester[]{ANONYMOUS, OWNER, OTHER, ADMIN}) {
                    assertAllowed(() -> policy.assertCanRead(file, requester));
                }
            }
        }
    }

    @Nested
    @DisplayName("Read an organizer document (private)")
    class ReadPrivate {

        @Test
        @DisplayName("anonymous → 401 STORAGE_AUTHENTICATION_REQUIRED")
        void anonymous_unauthorized() {
            assertUnauthorized(() -> policy.assertCanRead(organizerDocument(), ANONYMOUS));
        }

        @Test
        @DisplayName("requester (uploader) → allowed")
        void owner_allowed() {
            assertAllowed(() -> policy.assertCanRead(organizerDocument(), OWNER));
        }

        @Test
        @DisplayName("other user → 403 STORAGE_ACCESS_DENIED")
        void other_forbidden() {
            assertForbidden(() -> policy.assertCanRead(organizerDocument(), OTHER), "STORAGE_ACCESS_DENIED");
        }

        @Test
        @DisplayName("ADMIN → allowed")
        void admin_allowed() {
            assertAllowed(() -> policy.assertCanRead(organizerDocument(), ADMIN));
        }
    }

    @Nested
    @DisplayName("Delete a public file")
    class DeletePublic {

        @Test
        @DisplayName("anonymous → 401")
        void anonymous_unauthorized() {
            assertUnauthorized(() -> policy.assertCanDelete(profilePicture(), ANONYMOUS));
        }

        @Test
        @DisplayName("owner → allowed")
        void owner_allowed() {
            assertAllowed(() -> policy.assertCanDelete(profilePicture(), OWNER));
        }

        @Test
        @DisplayName("other user → 403 STORAGE_ACCESS_DENIED")
        void other_forbidden() {
            assertForbidden(() -> policy.assertCanDelete(eventMedia(), OTHER), "STORAGE_ACCESS_DENIED");
        }

        @Test
        @DisplayName("ADMIN → allowed")
        void admin_allowed() {
            assertAllowed(() -> policy.assertCanDelete(eventMedia(), ADMIN));
        }
    }

    @Nested
    @DisplayName("Delete an organizer document")
    class DeletePrivate {

        @Test
        @DisplayName("anonymous → 401")
        void anonymous_unauthorized() {
            assertUnauthorized(() -> policy.assertCanDelete(organizerDocument(), ANONYMOUS));
        }

        @Test
        @DisplayName("requester (uploader) → 403: only an ADMIN may delete it")
        void owner_forbidden() {
            assertForbidden(() -> policy.assertCanDelete(organizerDocument(), OWNER), "STORAGE_ACCESS_DENIED");
        }

        @Test
        @DisplayName("other user → 403")
        void other_forbidden() {
            assertForbidden(() -> policy.assertCanDelete(organizerDocument(), OTHER), "STORAGE_ACCESS_DENIED");
        }

        @Test
        @DisplayName("ADMIN → allowed")
        void admin_allowed() {
            assertAllowed(() -> policy.assertCanDelete(organizerDocument(), ADMIN));
        }
    }

    @Nested
    @DisplayName("Upload through the REST API")
    class UploadViaApi {

        @Test
        @DisplayName("anonymous → 401")
        void anonymous_unauthorized() {
            assertUnauthorized(() -> policy.assertCanUploadViaApi("PROFILE_PICTURE", ANONYMOUS));
        }

        @Test
        @DisplayName("authenticated user, PROFILE_PICTURE → allowed")
        void profilePicture_allowed() {
            assertAllowed(() -> policy.assertCanUploadViaApi("PROFILE_PICTURE", OWNER));
            assertAllowed(() -> policy.assertCanUploadViaApi("PROFILE_PICTURE", ADMIN));
        }

        @Test
        @DisplayName("ORGANIZER_DOCUMENT or EVENT_MEDIA → 403 STORAGE_CATEGORY_NOT_ALLOWED, even for ADMIN")
        void otherCategories_forbidden() {
            assertForbidden(() -> policy.assertCanUploadViaApi("ORGANIZER_DOCUMENT", OWNER), "STORAGE_CATEGORY_NOT_ALLOWED");
            assertForbidden(() -> policy.assertCanUploadViaApi("EVENT_MEDIA", OTHER), "STORAGE_CATEGORY_NOT_ALLOWED");
            assertForbidden(() -> policy.assertCanUploadViaApi("ORGANIZER_DOCUMENT", ADMIN), "STORAGE_CATEGORY_NOT_ALLOWED");
        }
    }
}
