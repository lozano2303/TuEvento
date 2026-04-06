package com.capysoft.tuevento.modules.storage.infrastructure.external;

import com.capysoft.tuevento.modules.storage.application.port.out.StorageClientPort;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3StorageClient implements StorageClientPort {

    private final S3Client    s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.storage.endpoint}")
    private String endpoint;

    @Override
    public String uploadFile(String bucket, String key, byte[] content, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) content.length)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(content));
            log.debug("File uploaded successfully: bucket={}, key={}", bucket, key);
            return key;
        } catch (Exception e) {
            log.error("Failed to upload file to S3: bucket={}, key={}", bucket, key, e);
            throw new BusinessException("S3_UPLOAD_FAILED", "Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String bucket, String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.debug("File deleted successfully: bucket={}, key={}", bucket, key);
        } catch (Exception e) {
            log.error("Failed to delete file from S3: bucket={}, key={}", bucket, key, e);
            throw new BusinessException("S3_DELETE_FAILED", "Failed to delete file: " + e.getMessage());
        }
    }

    @Override
    public String generatePublicUrl(String bucket, String key) {
        // Constructs the public URL directly: {endpoint}/{bucket}/{key}
        return endpoint.replaceAll("/$", "") + "/" + bucket + "/" + key;
    }

    @Override
    public String generatePresignedUrl(String bucket, String key, int expirationMinutes) {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(r -> r.bucket(bucket).key(key))
                    .build();

            String url = s3Presigner.presignGetObject(presignRequest).url().toString();
            log.debug("Presigned URL generated: bucket={}, key={}, expiresIn={}min", bucket, key, expirationMinutes);
            return url;
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: bucket={}, key={}", bucket, key, e);
            throw new BusinessException("S3_PRESIGN_FAILED", "Failed to generate presigned URL: " + e.getMessage());
        }
    }
}
