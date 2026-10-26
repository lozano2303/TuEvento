package com.capysoft.storage.application.dto.request;

/**
 * Identity of whoever calls a storage use case. {@code userId} is null for anonymous requests.
 */
public record StorageRequester(Integer userId, String role) {

    private static final String ADMIN_ROLE = "ADMIN";

    public static StorageRequester anonymous() {
        return new StorageRequester(null, null);
    }

    public static StorageRequester of(Integer userId, String role) {
        return new StorageRequester(userId, role);
    }

    public boolean isAnonymous() {
        return userId == null;
    }

    public boolean isAdmin() {
        return !isAnonymous() && ADMIN_ROLE.equals(role);
    }

    public boolean isUser(Integer otherUserId) {
        return !isAnonymous() && userId.equals(otherUserId);
    }
}
