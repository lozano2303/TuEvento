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
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Calls the self-hosted OpenNSFW2 microservice to detect sexually explicit content.
 * Fail-open: returns {@code true} (safe) on any infrastructure error so that a
 * downed moderation service never blocks legitimate uploads.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NsfwClientAdapter {

    private final ModerationConfig moderationConfig;

    /**
     * @return {@code true} if the image is safe (nsfw_score below threshold),
     *         {@code true} on any error (fail-open).
     */
    public boolean isSafe(byte[] imageBytes) {
        try {
            String url = moderationConfig.getNsfw().getUrl() + "/classify";

            ByteArrayResource resource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() { return "image.jpg"; }
            };

            LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            HttpHeaders partHeaders = new HttpHeaders();
            partHeaders.setContentType(MediaType.IMAGE_JPEG);
            partHeaders.setContentDispositionFormData("file", "image.jpg");
            body.add("file", new HttpEntity<>(resource, partHeaders));

            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

            RestTemplate rt = new RestTemplate();
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = rt.postForEntity(
                    url,
                    new HttpEntity<>(body, requestHeaders),
                    Map.class);

            if (response.getBody() == null) {
                log.warn("NSFW service returned empty body — failing open");
                return true;
            }

            double score = ((Number) response.getBody().get("nsfw_score")).doubleValue();
            log.debug("NSFW score: {}", score);
            return score < moderationConfig.getNsfw().getThreshold();

        } catch (Exception e) {
            log.error("NSFW service unreachable or failed — failing open: {}", e.getMessage());
            return true;
        }
    }
}
