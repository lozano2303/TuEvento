package com.capysoft.tuevento.modules.storage.application.port.in;

/**
 * Autoriza el acceso de un usuario a un archivo antes de exponerlo por la API.
 * Los llamados internos (otros casos de uso) no pasan por aquí.
 */
public interface CheckFileAccessPort {

    void assertCanRead(Integer storedFileId, Integer requesterId, boolean requesterIsAdmin);

    void assertCanDelete(Integer storedFileId, Integer requesterId, boolean requesterIsAdmin);
}
