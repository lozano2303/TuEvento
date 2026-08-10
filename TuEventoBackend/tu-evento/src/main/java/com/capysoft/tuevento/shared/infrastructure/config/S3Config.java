package com.capysoft.tuevento.shared.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Configures the AWS S3 client and presigner beans.
 * In development, endpoint override points to MinIO.
 * In production, remove the endpoint override to use real AWS S3.
 *
 * S3Client  → uses the internal endpoint (localhost / container) for upload/delete ops.
 * S3Presigner → uses the public endpoint so generated URLs are reachable from
 *               external clients (e.g. mobile devices on the same network).
 */
@Configuration
public class S3Config {

    @Value("${app.storage.endpoint}")
    private String endpoint;

    /** Public-facing MinIO URL used in presigned URLs — must be reachable by clients. */
    @Value("${minio.public-url:http://localhost:9000}")
    private String publicUrl;

    @Value("${app.storage.access-key}")
    private String accessKey;

    @Value("${app.storage.secret-key}")
    private String secretKey;

    @Value("${app.storage.region}")
    private String region;

    @Value("${app.storage.bucket-default:tuevento}")
    private String bucketDefault;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true) // required for MinIO
                        .build())
                .build();
    }

    @Bean
    public ApplicationRunner bucketInitializer(S3Client s3Client) {
        return args -> {
            boolean exists = s3Client.listBuckets()
                    .buckets().stream()
                    .anyMatch(b -> b.name().equals(bucketDefault));
            if (!exists) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketDefault).build());
                System.out.println("Bucket '" + bucketDefault + "' creado automáticamente en MinIO.");
            }
        };
    }

    @Bean
    public S3Presigner s3Presigner() {
        // Uses publicUrl so presigned URLs contain the network-accessible host,
        // not localhost — required for mobile clients on the same LAN.
        return S3Presigner.builder()
                .endpointOverride(URI.create(publicUrl))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true) // required for MinIO
                        .build())
                .build();
    }
}
