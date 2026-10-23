package com.capysoft.tuevento.modules.storage.application.port.out;

/**
 * Secondary port (driven) for image content moderation.
 * Implementations must NOT throw on infrastructure failures — they return {@code true}
 * (safe) to keep the upload path available when moderation services are unreachable
 * (fail-open policy).
 */
public interface ModerationPort {

    /**
     * Returns {@code true} if the image passes the NSFW check (score below threshold).
     * Returns {@code true} on any infrastructure error (fail-open).
     */
    boolean isNsfwSafe(byte[] imageBytes);

    /**
     * Returns {@code true} if the image passes the gore check (score below threshold).
     * Returns {@code true} on any infrastructure error (fail-open).
     */
    boolean isGoreSafe(byte[] imageBytes);
}
