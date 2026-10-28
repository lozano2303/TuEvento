package com.capysoft.tuevento.shared.application.storage;

import com.capysoft.tuevento.shared.application.storage.FileStorage.FileUpload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileStorage.FileUpload")
class FileStorageTest {

    private static FileUpload upload(byte[] content, Integer ownerId) {
        return new FileUpload("EVENT_MEDIA", 42, "cover.jpg", "image/jpeg", content, "EVENT", ownerId);
    }

    @Test
    @DisplayName("dos subidas con el mismo contenido (otro array) son iguales y tienen el mismo hash")
    void equalByContent() {
        FileUpload a = upload(new byte[]{1, 2, 3}, 9);
        FileUpload b = upload(new byte[]{1, 2, 3}, 9);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isEqualTo(a);
    }

    @Test
    @DisplayName("otro contenido, otro campo o otro tipo no son iguales")
    void differentUploads() {
        FileUpload a = upload(new byte[]{1, 2, 3}, 9);

        assertThat(a).isNotEqualTo(upload(new byte[]{1, 2, 4}, 9))
                .isNotEqualTo(upload(new byte[]{1, 2, 3}, 10))
                .isNotEqualTo(new FileUpload("PROFILE_PICTURE", 42, "cover.jpg", "image/jpeg", new byte[]{1, 2, 3},
                        "EVENT", 9))
                .isNotEqualTo("cover.jpg")
                .isNotEqualTo(null);
    }

    @Test
    @DisplayName("toString muestra el tamaño del archivo, no sus bytes")
    void toStringHidesContent() {
        assertThat(upload(new byte[]{1, 2, 3}, 9).toString())
                .contains("cover.jpg", "content=3 bytes", "ownerEntityId=9")
                .doesNotContain("[B@");
        assertThat(upload(null, null).toString()).contains("content=null");
    }
}
