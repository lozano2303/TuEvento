package com.capysoft.tuevento.shared.domain.exception;

/**
 * Thrown when an uploaded image fails content moderation (NSFW or gore detection).
 * The {@code reason} field carries the internal policy code (e.g. "NSFW_CONTENT",
 * "VIOLENT_CONTENT") for logging, while the user-facing message is intentionally generic.
 */
public class ImagePolicyViolationException extends BusinessException {

    private static final String USER_MESSAGE =
            "Tu imagen no cumple con nuestras políticas de contenido.";

    public ImagePolicyViolationException(String reason) {
        super(reason, USER_MESSAGE);
    }
}
