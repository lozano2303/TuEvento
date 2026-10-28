package com.capysoft.storage.application.service;

import com.capysoft.storage.application.port.out.ModerationPort;
import com.capysoft.storage.infrastructure.external.NsfwClientAdapter;
import com.capysoft.storage.infrastructure.external.SightengineAdapter;
import com.capysoft.common.domain.exception.ImagePolicyViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the two-stage moderation cascade:
 * <ol>
 *   <li>OpenNSFW2 — detects sexually explicit content (self-hosted, unlimited).</li>
 *   <li>Sightengine — detects gore (external API, called only if stage 1 passes).</li>
 * </ol>
 * Throws {@link ImagePolicyViolationException} if either stage rejects the image.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageModerationService implements ModerationPort {

    private final NsfwClientAdapter  nsfwClient;
    private final SightengineAdapter sightengineAdapter;

    /**
     * Runs the full cascade. Call this method from the upload use case.
     * Throws {@link ImagePolicyViolationException} on policy violation.
     */
    public void moderate(byte[] imageBytes) {
        if (!isNsfwSafe(imageBytes)) {
            log.warn("Image rejected by NSFW filter");
            throw new ImagePolicyViolationException("NSFW_CONTENT");
        }
        if (!isGoreSafe(imageBytes)) {
            log.warn("Image rejected by gore filter");
            throw new ImagePolicyViolationException("VIOLENT_CONTENT");
        }
    }

    @Override
    public boolean isNsfwSafe(byte[] imageBytes) {
        return nsfwClient.isSafe(imageBytes);
    }

    @Override
    public boolean isGoreSafe(byte[] imageBytes) {
        return sightengineAdapter.isSafe(imageBytes);
    }
}
