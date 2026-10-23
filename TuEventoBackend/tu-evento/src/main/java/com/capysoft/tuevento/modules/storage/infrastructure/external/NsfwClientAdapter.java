package com.capysoft.tuevento.modules.storage.infrastructure.external;

import com.capysoft.tuevento.modules.storage.infrastructure.config.ModerationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NsfwClientAdapter {

    private final ModerationConfig moderationConfig;

    public boolean isSafe(byte[] imageBytes) {
        try {
            String nsfwUrl = moderationConfig.getNsfw().getUrl();

            if (nsfwUrl == null || nsfwUrl.isBlank()) {
                log.warn("NSFW URL not configured — failing open");
                return true;
            }

            String url = nsfwUrl + "/classify";

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(10000);
            factory.setReadTimeout(30000);
            RestTemplate rt = new RestTemplate(factory);

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