package com.capysoft.tuevento.modules.event.application.port.in;

import com.capysoft.tuevento.modules.event.application.dto.response.EventRatingResponse;

public interface EditEventRatingUseCase {

    /**
     * Edita el texto de un comentario propio dentro de la ventana de 2 horas.
     *
     * @param eventId   id del evento al que pertenece el comentario
     * @param ratingId  id del comentario a editar
     * @param userId    id del usuario autenticado (extraído del JWT, nunca del body)
     * @param comment   nuevo texto (no vacío, máx 500 caracteres)
     * @return el comentario actualizado con editedAt poblado
     */
    EventRatingResponse execute(Long eventId, Long ratingId, Long userId, String comment);
}
