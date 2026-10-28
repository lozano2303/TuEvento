package com.capysoft.tuevento.modules.security.application.port.out;

/** URL firmada del documento que adjuntó una solicitud de organizador. La genera Archivos. */
public interface OrganizerDocumentPort {

    DocumentUrl publicUrl(Integer storedFileId, Integer requesterUserId, String requesterRole);

    /** Misma forma JSON que la respuesta de Archivos: {@code storedFileId} y {@code publicUrl}. */
    record DocumentUrl(Integer storedFileId, String publicUrl) {
    }
}
