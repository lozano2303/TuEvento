package com.capysoft.storage.infrastructure.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Cliente S3 y presigner para MinIO ({@code app.storage.provider=minio}, el valor por defecto),
 * con las mismas propiedades que el {@code S3Config} del monolito: los dos usan el mismo bucket.
 *
 * <p>S3Client usa el endpoint interno (subidas y borrados); S3Presigner usa el público, para que
 * las URL firmadas funcionen desde el navegador y el móvil. El bucket lo crea el monolito al
 * arrancar, así que aquí no se inicializa.
 */
@Configuration
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "minio", matchIfMissing = true)
public class MinioConfig {

    @Value("${app.storage.endpoint}")
    private String endpoint;

    @Value("${minio.public-url:http://localhost:9000}")
    private String publicUrl;

    @Value("${app.storage.access-key}")
    private String accessKey;

    @Value("${app.storage.secret-key}")
    private String secretKey;

    @Value("${app.storage.region:us-east-1}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(credentials())
                .region(Region.of(region))
                .serviceConfiguration(pathStyle())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(publicUrl))
                .credentialsProvider(credentials())
                .region(Region.of(region))
                .serviceConfiguration(pathStyle())
                .build();
    }

    private StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    /** MinIO necesita direccionamiento por ruta (http://host/bucket/key). */
    private static S3Configuration pathStyle() {
        return S3Configuration.builder().pathStyleAccessEnabled(true).build();
    }
}
