package com.capysoft.identity.security.application.port.out;

import com.capysoft.identity.security.application.port.out.OrganizerDocumentPort.DocumentUpload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrganizerDocumentPort.DocumentUpload")
class OrganizerDocumentPortTest {

    private static DocumentUpload upload(byte[] content) {
        return new DocumentUpload("ORGANIZER_DOCUMENT", "organizer_petition", 7, "id.pdf", "application/pdf", content);
    }

    @Test
    @DisplayName("uploads with the same file content are equal and share a hash code")
    void equalByContent() {
        DocumentUpload first = upload(new byte[] {1, 2, 3});
        DocumentUpload second = upload(new byte[] {1, 2, 3});

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    @Test
    @DisplayName("uploads differ when the file content or any other field differs")
    void differentContentOrField() {
        DocumentUpload base = upload(new byte[] {1, 2, 3});

        assertThat(base)
                .isNotEqualTo(upload(new byte[] {1, 2, 4}))
                .isNotEqualTo(new DocumentUpload("OTHER", "organizer_petition", 7, "id.pdf", "application/pdf",
                        new byte[] {1, 2, 3}))
                .isNotEqualTo(null)
                .isNotEqualTo("id.pdf");
    }

    @Test
    @DisplayName("toString shows the content length, not the raw bytes")
    void toStringHidesBytes() {
        assertThat(upload(new byte[] {1, 2, 3}).toString())
                .contains("originalFilename=id.pdf", "contentLength=3")
                .doesNotContain("[B@");
        assertThat(upload(null).toString()).contains("contentLength=0");
    }
}
