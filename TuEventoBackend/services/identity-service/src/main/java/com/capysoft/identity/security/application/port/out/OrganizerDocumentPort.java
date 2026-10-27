package com.capysoft.identity.security.application.port.out;

import java.util.Arrays;
import java.util.Objects;

/**
 * Archivos que Identidad guarda en el servicio de Archivos (el documento de una solicitud de
 * organizador) y sus URLs. Identidad solo conserva el {@code storedFileId}.
 */
public interface OrganizerDocumentPort {

    /** Sube el archivo y devuelve su {@code storedFileId}. */
    Integer upload(DocumentUpload upload);

    /** URL de cualquier archivo guardado (documento o foto de perfil) para quien la pide. */
    DocumentUrl publicUrl(Integer storedFileId, Integer requesterUserId, String requesterRole);

    record DocumentUpload(String fileCategoryCode, String ownerEntityType, Integer uploadedBy,
                          String originalFilename, String contentType, byte[] content) {

        // Records compare arrays by reference: compare and print the file content by value instead.
        @Override
        public boolean equals(Object other) {
            return other instanceof DocumentUpload that
                    && Objects.equals(fileCategoryCode, that.fileCategoryCode)
                    && Objects.equals(ownerEntityType, that.ownerEntityType)
                    && Objects.equals(uploadedBy, that.uploadedBy)
                    && Objects.equals(originalFilename, that.originalFilename)
                    && Objects.equals(contentType, that.contentType)
                    && Arrays.equals(content, that.content);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hash(fileCategoryCode, ownerEntityType, uploadedBy, originalFilename, contentType)
                    + Arrays.hashCode(content);
        }

        @Override
        public String toString() {
            return "DocumentUpload[fileCategoryCode=" + fileCategoryCode + ", ownerEntityType=" + ownerEntityType
                    + ", uploadedBy=" + uploadedBy + ", originalFilename=" + originalFilename
                    + ", contentType=" + contentType + ", contentLength="
                    + (content == null ? 0 : content.length) + "]";
        }
    }

    /** Misma forma JSON que la respuesta de Archivos: {@code storedFileId} y {@code publicUrl}. */
    record DocumentUrl(Integer storedFileId, String publicUrl) {
    }
}
