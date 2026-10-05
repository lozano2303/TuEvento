/**
 * EventCommentService — gestión de comentarios/ratings de eventos.
 *
 * Endpoints:
 *   GET    /events/{eventId}/ratings              — lista pública (sin auth)
 *   POST   /events/{eventId}/ratings              — crear comentario (auth USER)
 *   DELETE /events/{eventId}/ratings/{ratingId}   — borrar comentario propio (auth USER)
 *
 * El POST guarda el comentario en BD y, tras el commit, el backend
 * lo transmite por WebSocket al canal /topic/events/{eventId}/comments.
 *
 * El DELETE borra físicamente el comentario y el backend notifica en
 * /topic/events/{eventId}/comments/deleted con payload { ratingId }.
 *
 * El payload de GET y POST tiene la misma forma:
 *   { ratingId, userId, authorName, rating, comment, isVisible, createdAt }
 */
import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/**
 * Obtiene todos los comentarios/ratings visibles de un evento.
 * Endpoint público — no requiere autenticación.
 *
 * @param {string|number} eventId
 * @returns {Promise<{ data: Array }>}
 */
export const getEventComments = async (eventId) => {
  const res = await fetch(`${API_URL}/events/${eventId}/ratings`);
  if (!res.ok) throw new Error('Error al obtener los comentarios del evento');
  return res.json();
};

/**
 * Publica un nuevo comentario/rating en un evento.
 * Requiere autenticación con rol USER.
 *
 * @param {string|number} eventId
 * @param {{ rating: number, comment: string }} payload
 *   - rating: entero entre 1 y 5
 *   - comment: texto no vacío, máximo 500 caracteres
 * @returns {Promise<{ data: object }>}
 */
export const addEventComment = async (eventId, payload) => {
  const res = await httpRequest(`${API_URL}/events/${eventId}/ratings`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  const body = await res.json();
  if (!res.ok) throw new Error(body.message || 'Error al publicar el comentario');
  return body;
};

/**
 * Elimina de forma permanente un comentario/rating propio.
 * Requiere autenticación con rol USER.
 * El backend valida que el comentario pertenezca al usuario autenticado
 * (ownership extraído del JWT, nunca del body).
 *
 * @param {string|number} eventId
 * @param {string|number} ratingId
 * @returns {Promise<{ data: null }>}
 * @throws Error con mensaje del backend en caso de 403 (no propietario) o 404 (no existe)
 */
export const deleteEventComment = async (eventId, ratingId) => {
  const res = await httpRequest(`${API_URL}/events/${eventId}/ratings/${ratingId}`, {
    method: 'DELETE',
  });
  const body = await res.json();
  if (!res.ok) throw new Error(body.message || 'Error al eliminar el comentario');
  return body;
};
