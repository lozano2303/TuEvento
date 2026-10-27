package com.capysoft.tuevento.modules.identitybridge.port;

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
    }

    /** Misma forma JSON que la respuesta de Archivos: {@code storedFileId} y {@code publicUrl}. */
    record DocumentUrl(Integer storedFileId, String publicUrl) {
    }
}
