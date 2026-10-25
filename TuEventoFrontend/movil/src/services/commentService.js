import { httpRequest } from "./httpClient";

/**
 * commentService — comentarios/calificaciones y respuestas de eventos (espejo de
 * EventCommentService de la web).
 *
 *   GET    /events/{eventId}/ratings              — lista pública plana con parentRatingId
 *   POST   /events/{eventId}/ratings              — crear comentario o respuesta
 *   PATCH  /events/{eventId}/ratings/{ratingId}   — editar texto propio (ventana de 2 h)
 *   DELETE /events/{eventId}/ratings/{ratingId}   — borrar propio (borra sus respuestas)
 *
 * Reglas del backend: un solo nivel de respuestas, la primera reseña principal lleva
 * calificación (salvo el organizador) y máximo 1 comentario cada 10 s por evento.
 */

const BASE_URL = process.env.EXPO_PUBLIC_API_URL;

/** Error con el código de negocio del backend en err.code (p. ej. COMMENT_RATE_LIMITED). */
function buildError(body, fallback) {
  const err = new Error(body?.message || fallback);
  err.code = body?.code || null;
  return err;
}

/** Lista plana de comentarios visibles; con languageCode trae translatedComment. */
export const getEventComments = async (eventId, languageCode = null) => {
  const lang = languageCode && languageCode !== "es" ? `?languageCode=${languageCode}` : "";
  const res = await fetch(`${BASE_URL}/events/${eventId}/ratings${lang}`);
  if (!res.ok) throw new Error(`Error al obtener los comentarios: ${res.status}`);
  const json = await res.json();
  return json.data ?? [];
};

/** @param {{ rating?: number, comment: string, parentRatingId?: number, replyToUserId?: number }} payload */
export const addEventComment = async (eventId, payload) => {
  const res = await httpRequest(`${BASE_URL}/events/${eventId}/ratings`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw buildError(body, `Error ${res.status}`);
  return body.data;
};

export const editEventComment = async (eventId, ratingId, comment) => {
  const res = await httpRequest(`${BASE_URL}/events/${eventId}/ratings/${ratingId}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ comment }),
  });
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw buildError(body, `Error ${res.status}`);
  return body.data;
};

export const deleteEventComment = async (eventId, ratingId) => {
  const res = await httpRequest(`${BASE_URL}/events/${eventId}/ratings/${ratingId}`, {
    method: "DELETE",
  });
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw buildError(body, `Error ${res.status}`);
  return body;
};
