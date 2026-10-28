package com.capysoft.identity.shared.infrastructure.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente hacia la API interna del monolito ({@code /internal/**}), que aún sirve lo que Identidad
 * necesita de otros dominios (estadísticas del panel, documentos en Archivos). Lleva la clave entre
 * servicios y timeouts cortos: un monolito lento no debe retener los hilos de este servicio.
 */
@Configuration
public class MonolithInternalApiConfig {

    public static final String BEAN = "monolithInternalRestClient";
    static final String API_KEY_HEADER = "X-Internal-Api-Key";

    @Bean(BEAN)
    public RestClient monolithInternalRestClient(RestClient.Builder builder,
                                                 @Value("${app.monolith.base-url:http://localhost:8080}") String baseUrl,
                                                 @Value("${app.internal.api-key:}") String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        // La subida de un documento pasa por la moderación de imágenes del monolito: más margen
        factory.setReadTimeout(Duration.ofSeconds(60));
        return builder.baseUrl(baseUrl).defaultHeader(API_KEY_HEADER, apiKey).requestFactory(factory).build();
    }
}
