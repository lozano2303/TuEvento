package com.capysoft.storage.infrastructure.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Binds the {@code app.storage.rustfs.*} properties and configures the S3 client
 * and presigner for RustFS (S3-compatible object storage written in Rust).
 * Active only when {@code app.storage.provider=rustfs}.
 *
 * S3Client    → uses the internal endpoint for upload/delete ops.
 * S3Presigner → uses the public endpoint so generated URLs are reachable from
 *               external clients (e.g. mobile devices on the same network).
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "app.storage.rustfs")
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "rustfs")
public class RustFsConfig {

    /** Prefixes readable anonymously (same public prefixes as the MinIO setup). */
    private static final String[] PUBLIC_PREFIXES = {"profile_picture", "event_media"};

    private String endpoint;
    private String publicUrl;
    private String accessKey;
    private String secretKey;
    private String region = "us-east-1";

    @Value("${app.storage.bucket-default:tuevento}")
    private String bucketDefault;

    @Bean
    public S3Client rustFsS3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(credentials())
                .region(Region.of(region))
                .serviceConfiguration(pathStyle())
                .build();
    }

    @Bean
    public S3Presigner rustFsS3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(publicUrl != null ? publicUrl : endpoint))
                .credentialsProvider(credentials())
                .region(Region.of(region))
                .serviceConfiguration(pathStyle())
                .build();
    }

    /**
     * Creates the default bucket if missing and applies anonymous read access
     * to the public prefixes (replaces the minio-init step for RustFS).
     * Runs before DataInitializer (@Order(1)) and ProfileDataInitializer (@Order(2)),
     * which upload files and need the bucket to exist.
     */
    @Bean
    public ApplicationRunner rustFsBucketInitializer(S3Client rustFsS3Client) {
        return new BucketInitializer(rustFsS3Client);
    }

    private class BucketInitializer implements ApplicationRunner, Ordered {

        private final S3Client s3Client;

        BucketInitializer(S3Client s3Client) {
            this.s3Client = s3Client;
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }

        @Override
        public void run(ApplicationArguments args) {
            boolean exists = s3Client.listBuckets()
                    .buckets().stream()
                    .anyMatch(b -> b.name().equals(bucketDefault));
            if (!exists) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketDefault).build());
                log.info("Bucket '{}' created in RustFS", bucketDefault);
            }
            try {
                s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
                        .bucket(bucketDefault)
                        .policy(publicReadPolicy())
                        .build());
            } catch (Exception e) {
                log.warn("Could not apply public read policy to RustFS bucket '{}': {}", bucketDefault, e.getMessage());
            }
        }
    }

    private String publicReadPolicy() {
        StringBuilder resources = new StringBuilder();
        for (String prefix : PUBLIC_PREFIXES) {
            if (!resources.isEmpty()) resources.append(',');
            resources.append("\"arn:aws:s3:::").append(bucketDefault).append('/').append(prefix).append("/*\"");
        }
        return "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\","
                + "\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],"
                + "\"Resource\":[" + resources + "]}]}";
    }

    private StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    private S3Configuration pathStyle() {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(true) // required for RustFS
                .build();
    }
}
