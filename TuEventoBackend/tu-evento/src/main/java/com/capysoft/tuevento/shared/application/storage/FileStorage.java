package com.capysoft.tuevento.shared.application.storage;

import java.util.List;
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
