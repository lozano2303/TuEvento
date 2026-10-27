package com.capysoft.tuevento.modules.event.application.port.in;

/**
 * Cierra los eventos activos de un organizador al que Identidad le quitó el rol: los PUBLISHED
 * se cancelan y los PENDING_REVIEW se rechazan. Los demás estados no se tocan.
 *
 * <p>Se puede repetir sin efectos dobles: un evento ya cerrado no vuelve a cerrarse. Si alguno
 * falla, los demás se cierran igual y al final se lanza {@link IllegalStateException} para que
 * quien lo invoca lo reintente.
 */
public interface CloseRevokedOrganizerEventsPort {

    /** @return cuántos eventos se cerraron en esta llamada */
    int close(Integer organizerUserId, Integer revokedBy, String reason);
}
