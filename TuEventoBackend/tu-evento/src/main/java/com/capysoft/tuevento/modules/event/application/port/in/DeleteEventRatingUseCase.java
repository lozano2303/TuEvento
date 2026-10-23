package com.capysoft.tuevento.modules.event.application.port.in;

/**
 * Elimina de forma permanente un comentario/rating de evento.
 *
 * @param eventId   ID del evento al que pertenece el rating
 * @param ratingId  ID del rating a borrar
 * @param userId    ID del usuario autenticado (extraído del JWT, nunca del body)
 * @throws com.capysoft.tuevento.shared.domain.exception.NotFoundException si el rating
 *         no existe o no pertenece al evento indicado
 * @throws com.capysoft.tuevento.shared.domain.exception.BusinessException con código
 *         RATING_ACCESS_DENIED si el rating existe pero pertenece a otro usuario
 */
public interface DeleteEventRatingUseCase {

    void execute(Long eventId, Long ratingId, Long userId);
}
