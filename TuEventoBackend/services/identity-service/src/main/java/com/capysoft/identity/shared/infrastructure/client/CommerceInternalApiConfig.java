package com.capysoft.identity.shared.infrastructure.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Client of commerce-service's internal API ({@code /internal/**}): Commerce figures for the admin
 * panel. It carries the service-to-service key and short timeouts.
 */
@Configuration
public class CommerceInternalApiConfig {

    public static final String BEAN = "commerceInternalRestClient";
    static final String API_KEY_HEADER = "X-Internal-Api-Key";

    @Bean(BEAN)
    public RestClient commerceInternalRestClient(RestClient.Builder builder,
                                                 @Value("${app.commerce.base-url:http://localhost:8084}") String baseUrl,
                                                 @Value("${app.internal.api-key:}") String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return builder.baseUrl(baseUrl).defaultHeader(API_KEY_HEADER, apiKey).requestFactory(factory).build();
    }
}
