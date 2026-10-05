package com.capysoft.tuevento.modules.language.infrastructure.external;

import com.capysoft.tuevento.modules.language.application.port.out.TranslationException;
import com.capysoft.tuevento.modules.language.application.port.out.TranslationPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * Adaptador para LibreTranslate.
 */
@Component
@Slf4j
public class LibreTranslateAdapter implements TranslationPort {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    
    public LibreTranslateAdapter(
            @Value("${translation.libretranslate.base-url:http://localhost:5001}") String baseUrl,
            @Value("${translation.libretranslate.timeout.connect:10s}") Duration connectTimeout,
            @Value("${translation.libretranslate.timeout.read:30s}") Duration readTimeout,
            ObjectMapper objectMapper) {
        
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
                
        log.info("LibreTranslate adapter configured with base URL: {}", baseUrl);
    }

    @Override
    public String translate(String sourceText, String sourceLang, String targetLang) throws TranslationException {
        if (sourceText == null || sourceText.trim().isEmpty()) {
            return sourceText;
        }

        try {
            Map<String, String> request = Map.of(
                "q", sourceText,
                "source", sourceLang,
                "target", targetLang,
                "format", "text"
            );

            String response = restClient.post()
                    .uri("/translate")
                    .header("Content-Type", "application/json")
                    .body(request)
                    .retrieve()
                    .body(String.class);

            // Parse JSON response
            JsonNode jsonResponse = objectMapper.readTree(response);
            String translatedText = jsonResponse.get("translatedText").asText();
            
            log.debug("Translation successful: {} -> {} ({}->{})", 
                sourceText.substring(0, Math.min(50, sourceText.length())), 
                translatedText.substring(0, Math.min(50, translatedText.length())),
                sourceLang, targetLang);
                
            return translatedText;

        } catch (Exception e) {
            String error = String.format("Translation failed for %s->%s: %s", sourceLang, targetLang, e.getMessage());
            log.error(error, e);
            throw new TranslationException(error, e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // Simple health check
            restClient.get()
                    .uri("/")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("LibreTranslate service is not available: {}", e.getMessage());
            return false;
        }
    }
}