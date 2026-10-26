package com.capysoft.storage.application.service;

import com.capysoft.storage.application.dto.request.StorageRequester;
import com.capysoft.storage.domain.model.StoredFile;
import com.capysoft.common.domain.exception.ForbiddenException;
import com.capysoft.common.domain.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Access rules for stored files.
 * <ul>
 *   <li>Read: files in a public category are readable by anyone; private files only by
 *       the uploader or an ADMIN.</li>
 *   <li>Delete: the uploader or an ADMIN; organizer documents only by an ADMIN, so a pending
 *       petition never loses its evidence.</li>
 *   <li>Upload through the REST API: authenticated users, profile pictures only. Other
 *       categories are uploaded by their own flows (organizer petition, event media).</li>
 * </ul>
 */
@Component
public class StorageAccessPolicy {

    public static final String ORGANIZER_DOCUMENT = "ORGANIZER_DOCUMENT";

    private static final Set<String> API_UPLOADABLE_CATEGORIES = Set.of("PROFILE_PICTURE");

    public boolean canRead(StoredFile file, StorageRequester requester) {
        return isPublic(file) || requester.isAdmin() || requester.isUser(file.getUploadedBy());
    }

    public void assertCanRead(StoredFile file, StorageRequester requester) {
        if (canRead(file, requester)) {
            return;
        }
        requireAuthenticated(requester);
        throw accessDenied();
    }

    public void assertCanDelete(StoredFile file, StorageRequester requester) {
        requireAuthenticated(requester);
        if (requester.isAdmin()) {
            return;
        }
        if (ORGANIZER_DOCUMENT.equals(categoryCode(file)) || !requester.isUser(file.getUploadedBy())) {
            throw accessDenied();
        }
    }

    public void assertCanUploadViaApi(String categoryCode, StorageRequester requester) {
        requireAuthenticated(requester);
        if (!API_UPLOADABLE_CATEGORIES.contains(categoryCode)) {
            throw new ForbiddenException("STORAGE_CATEGORY_NOT_ALLOWED",
                    "This file category cannot be uploaded through this endpoint");
        }
    }

    public boolean isPublic(StoredFile file) {
        return file.getFileCategory() != null && Boolean.TRUE.equals(file.getFileCategory().getIsPublic());
    }

    private void requireAuthenticated(StorageRequester requester) {
        if (requester.isAnonymous()) {
            throw new UnauthorizedException("STORAGE_AUTHENTICATION_REQUIRED",
                    "Authentication is required to access this file");
        }
    }

    private ForbiddenException accessDenied() {
        return new ForbiddenException("STORAGE_ACCESS_DENIED", "You do not have access to this file");
    }

    private String categoryCode(StoredFile file) {
        return file.getFileCategory() != null ? file.getFileCategory().getCode() : null;
    }
}
