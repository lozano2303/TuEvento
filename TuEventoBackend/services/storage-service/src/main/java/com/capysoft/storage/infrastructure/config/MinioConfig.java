package com.capysoft.storage.infrastructure.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Cliente S3 y presigner para MinIO ({@code app.storage.provider=minio}, el valor por defecto).
 *
 * <p>S3Client usa el endpoint interno (subidas y borrados); S3Presigner usa el público, para que
 * las URL firmadas funcionen desde el navegador y el móvil. El bucket se crea al arrancar si no
 * existe, antes que cualquier otro runner que suba archivos (el avatar por defecto).
 */
@Slf4j
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

    @Value("${app.storage.bucket-default:tuevento}")
    private String bucketDefault;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ApplicationRunner minioBucketInitializer(S3Client s3Client) {
        return args -> {
            try {
                // headBucket funciona en MinIO, R2 y AWS S3 (listBuckets no está en R2)
                s3Client.headBucket(b -> b.bucket(bucketDefault));
            } catch (NoSuchBucketException e) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketDefault).build());
                log.info("Bucket '{}' created", bucketDefault);
            } catch (Exception e) {
                // En un S3 externo el bucket ya existe: no se impide el arranque
                log.warn("Bucket check for '{}' failed, continuing: {}", bucketDefault, e.getMessage());
            }
        };
    }

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
