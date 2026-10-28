package com.capysoft.tuevento.shared.application.storage;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Archivos del servicio de archivos (storage-service), para los módulos del monolito que suben o
 * consultan archivos. La subida es de confianza: quien llama ya validó al usuario y decide la
 * categoría y el dueño; el servicio aplica el tamaño, la extensión y la moderación de imágenes.
 */
public interface FileStorage {

    /** Archivo a subir en nombre de {@code uploadedBy}; el dueño es opcional. */
    record FileUpload(String categoryCode, Integer uploadedBy, String originalFilename, String contentType,
                      byte[] content, String ownerEntityType, Integer ownerEntityId) {

        // Un record compara los arrays por referencia: aquí cuenta el contenido del archivo
        @Override
        public boolean equals(Object other) {
            return other instanceof FileUpload o
                    && Objects.equals(categoryCode, o.categoryCode) && Objects.equals(uploadedBy, o.uploadedBy)
                    && Objects.equals(originalFilename, o.originalFilename)
                    && Objects.equals(contentType, o.contentType) && Arrays.equals(content, o.content)
                    && Objects.equals(ownerEntityType, o.ownerEntityType)
                    && Objects.equals(ownerEntityId, o.ownerEntityId);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hash(categoryCode, uploadedBy, originalFilename, contentType, ownerEntityType,
                    ownerEntityId) + Arrays.hashCode(content);
        }

        /** Sin el contenido: solo su tamaño, para no volcar el archivo en los logs. */
        @Override
        public String toString() {
            return "FileUpload[categoryCode=" + categoryCode + ", uploadedBy=" + uploadedBy
                    + ", originalFilename=" + originalFilename + ", contentType=" + contentType
                    + ", content=" + (content == null ? "null" : content.length + " bytes")
                    + ", ownerEntityType=" + ownerEntityType + ", ownerEntityId=" + ownerEntityId + "]";
        }
    }

    /** Archivo guardado y la URL con la que lo lee el navegador. */
    record StoredFileRef(Integer storedFileId, String publicUrl) {
    }

    StoredFileRef upload(FileUpload upload);

    /** URL del archivo para el usuario en cuyo nombre se pide (aplica la política de acceso). */
    StoredFileRef publicUrl(Integer storedFileId, Integer requesterUserId, String requesterRole);

    /** URL de un archivo de categoría pública; vacío si no existe o fue borrado. */
    Optional<String> findPublicUrl(Integer storedFileId);

    /** IDs de los archivos públicos de un dueño, p. ej. el avatar por defecto. */
    List<Integer> findIdsByOwner(String ownerEntityType, Integer ownerEntityId);
}
