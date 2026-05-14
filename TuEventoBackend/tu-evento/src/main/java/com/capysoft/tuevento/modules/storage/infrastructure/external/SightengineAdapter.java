package com.capysoft.tuevento.modules.storage.infrastructure.external;

import com.capysoft.tuevento.modules.storage.infrastructure.config.ModerationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Calls the Sightengine REST API to detect gore/violent content.
 * Only invoked after OpenNSFW2 has already approved the image (cascade pattern).
 * Fail-open: returns {@code true} (safe) on any infrastructure error.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SightengineAdapter {

    private static final String SIGHTENGINE_URL =
            "https://api.sightengine.com/1.0/check.json";

    private final RestTemplate     restTemplate;
    private final ModerationConfig moderationConfig;

    /**
     * @return {@code true} if the image is safe (gore.prob below threshold),
     *         {@code true} on any error (fail-open).
     */
    public boolean isSafe(byte[] imageBytes) {
        try {
            ModerationConfig.Sightengine cfg = moderationConfig.getSightengine();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource resource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return "image.jpg";
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("media",      resource);
            body.add("models",     cfg.getModels());
            body.add("api_user",   cfg.getApiUser());
            body.add("api_secret", cfg.getApiSecret());

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response =
                    restTemplate.postForEntity(SIGHTENGINE_URL, request, (Class<Map<String, Object>>) (Class<?>) Map.class);

            if (response.getBody() == null) {
                log.warn("Sightengine returned empty body — failing open");
                return true;
            }

            // Response shape: { "gore": { "prob": 0.02 }, ... }
            @SuppressWarnings("unchecked")
            Map<String, Object> gore = (Map<String, Object>) response.getBody().get("gore");
            if (gore == null) {
                log.warn("Sightengine response missing 'gore' field — failing open");
                return true;
            }

            double prob = ((Number) gore.get("prob")).doubleValue();
            log.debug("Sightengine gore.prob: {}", prob);
            return prob < cfg.getThreshold();

        } catch (Exception e) {
            log.error("Sightengine unreachable or failed — failing open: {}", e.getMessage());
            return true;
        }
    }
}
