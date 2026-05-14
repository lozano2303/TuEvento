package com.capysoft.tuevento.modules.storage.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Binds the {@code moderation.*} properties from application.yaml and exposes
 * a shared {@link RestTemplate} bean for the moderation adapters.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "moderation")
public class ModerationConfig {

    private Nsfw nsfw = new Nsfw();
    private Sightengine sightengine = new Sightengine();

    @Data
    public static class Nsfw {
        private String url;
        private double threshold = 0.6;
    }

    @Data
    public static class Sightengine {
        private String apiUser;
        private String apiSecret;
        private String models = "gore";
        private double threshold = 0.6;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
