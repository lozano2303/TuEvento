package com.capysoft.tuevento.modules.storage.infrastructure.external;

import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link S3StorageClient}: public URLs use the public host and SDK errors are not leaked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("S3StorageClient")
class S3StorageClientTest {

    private static final String SDK_DETAIL = "Access Denied (Service: S3, Request ID: 17A2B, host minio:9000)";

    @Mock private S3Client    s3Client;
    @Mock private S3Presigner s3Presigner;

    @InjectMocks
    private S3StorageClient client;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(client, "publicUrl", "https://s3.example.com/");
    }

    @Test
    @DisplayName("generatePublicUrl → built from the public host")
    void publicUrl_usesPublicHost() {
        assertThat(client.generatePublicUrl("tuevento", "event_media/a.jpg"))
                .isEqualTo("https://s3.example.com/tuevento/event_media/a.jpg");
    }

    @Test
    @DisplayName("upload failure → S3_UPLOAD_FAILED with a fixed message")
    void uploadFailure_fixedMessage() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message(SDK_DETAIL).build());

        assertThatThrownBy(() -> client.uploadFile("tuevento", "k", new byte[]{1}, "image/jpeg"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "S3_UPLOAD_FAILED")
                .hasMessage("Failed to upload file");
    }

    @Test
    @DisplayName("delete failure → S3_DELETE_FAILED with a fixed message")
    void deleteFailure_fixedMessage() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().message(SDK_DETAIL).build());

        assertThatThrownBy(() -> client.deleteFile("tuevento", "k"))
                .hasFieldOrPropertyWithValue("code", "S3_DELETE_FAILED")
                .hasMessage("Failed to delete file");
    }
}
