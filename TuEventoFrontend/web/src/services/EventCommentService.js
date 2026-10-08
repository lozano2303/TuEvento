/**
 * EventCommentService — gestión de comentarios/ratings y respuestas de eventos.
 *
 * Endpoints:
 *   GET    /events/{eventId}/ratings                       — lista pública plana con parentRatingId
 *   POST   /events/{eventId}/ratings                       — crear comentario o respuesta (R1)
 *   PATCH  /events/{eventId}/ratings/{ratingId}            — editar texto propio (R1, ventana 2 h)
 *   DELETE /events/{eventId}/ratings/{ratingId}            — borrar propio (R1)
 *
 * Reglas de negocio:
 *   R1 — Cualquier usuario autenticado puede comentar, responder, editar y borrar los suyos.
 *   R2 — Múltiples comentarios por persona y evento.
 *   R3 — El primer comentario PRINCIPAL con rating lleva calificación; los siguientes no.
 *   R4 — El organizador del evento nunca lleva calificación.
 *   R5 — Antispam: máximo 1 comentario/respuesta cada 10 s por persona/evento.
 *   A  — Respuestas: un solo nivel (no se responde a una respuesta).
 *   B  — Edición: solo el texto, solo durante 2 h desde createdAt.
 *   C  — Borrar un principal borra sus respuestas.
 *
 * Payload del GET (lista plana):
 *   { ratingId, userId, authorName, rating (nullable), comment, isVisible,
 *     isOrganizer, createdAt, editedAt (nullable), parentRatingId (nullable) }
 */
import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/** Lanza un Error enriquecido con err.code del backend. */
function buildError(body, fallback) {
  const err = new Error(body.message || fallback);
  err.code = body.code || null;
  return err;
}

/**
 * Obtiene todos los comentarios/ratings visibles de un evento (lista plana).
 * Endpoint público — no requiere autenticación.
 */
export const getEventComments = async (eventId) => {
  const res = await fetch(`${API_URL}/events/${eventId}/ratings`);
  if (!res.ok) throw new Error('Error al obtener los comentarios del evento');
  return res.json();
};

/**
 * Publica un nuevo comentario/rating o una respuesta en un evento.
 * Requiere autenticación (R1).
 *
 * @param {string|number} eventId
 * @param {{ rating?: number, comment: string, parentRatingId?: number, replyToUserId?: number }} payload
 */
export const addEventComment = async (eventId, payload) => {
  const res = await httpRequest(`${API_URL}/events/${eventId}/ratings`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  const body = await res.json();
  if (!res.ok) throw buildError(body, 'Error al publicar el comentario');
  return body;
};

/**
 * Edita el texto de un comentario propio (solo texto, solo dentro de 2 h).
 *
 * @param {string|number} eventId
 * @param {string|number} ratingId
 * @param {string} comment  Nuevo texto (no vacío, máx 500)
 */
export const editEventComment = async (eventId, ratingId, comment) => {
  const res = await httpRequest(
    `${API_URL}/events/${eventId}/ratings/${ratingId}`,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ comment }),
    }
  );
  const body = await res.json();
  if (!res.ok) throw buildError(body, 'Error al editar el comentario');
  return body;
};

/**
 * Elimina de forma permanente un comentario/rating propio.
 * Al borrar un principal, el backend elimina también sus respuestas.
 */
export const deleteEventComment = async (eventId, ratingId) => {
  const res = await httpRequest(
    `${API_URL}/events/${eventId}/ratings/${ratingId}`,
    { method: 'DELETE' }
  );
  const body = await res.json();
  if (!res.ok) throw buildError(body, 'Error al eliminar el comentario');
  return body;
};
