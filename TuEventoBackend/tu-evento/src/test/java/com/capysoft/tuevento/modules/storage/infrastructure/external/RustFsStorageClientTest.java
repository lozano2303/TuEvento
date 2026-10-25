package com.capysoft.tuevento.modules.storage.infrastructure.external;

import com.capysoft.tuevento.modules.storage.infrastructure.config.RustFsConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RustFsStorageClient}: public URLs use the public host and SDK errors are not leaked.
 */
@DisplayName("RustFsStorageClient")
class RustFsStorageClientTest {

    private final S3Presigner presigner = mock(S3Presigner.class);

    private RustFsStorageClient clientWith(String endpoint, String publicUrl) {
        RustFsConfig config = new RustFsConfig();
        config.setEndpoint(endpoint);
        config.setPublicUrl(publicUrl);
        return new RustFsStorageClient(mock(S3Client.class), presigner, config);
    }

    @Test
    @DisplayName("generatePublicUrl → built from the public host, not the internal endpoint")
    void publicUrl_usesPublicHost() {
        assertThat(clientWith("http://rustfs:9000", "https://s3.example.com")
                .generatePublicUrl("tuevento", "event_media/a.jpg"))
                .isEqualTo("https://s3.example.com/tuevento/event_media/a.jpg");
    }

    @Test
    @DisplayName("generatePublicUrl without public host → falls back to the endpoint")
    void publicUrl_fallsBackToEndpoint() {
        assertThat(clientWith("http://localhost:9100/", null).generatePublicUrl("tuevento", "k"))
                .isEqualTo("http://localhost:9100/tuevento/k");
    }

    @Test
    @DisplayName("presign failure → S3_PRESIGN_FAILED with a fixed message")
    void presignFailure_fixedMessage() {
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid credentials rustfsadmin"));

        assertThatThrownBy(() -> clientWith("http://rustfs:9000", null).generatePresignedUrl("tuevento", "k", 60))
                .hasFieldOrPropertyWithValue("code", "S3_PRESIGN_FAILED")
                .hasMessage("Failed to generate presigned URL");
    }
}
