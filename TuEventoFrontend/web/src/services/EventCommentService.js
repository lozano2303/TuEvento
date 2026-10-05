/**
 * EventCommentService — gestión de comentarios/ratings de eventos.
 *
 * Endpoints:
 *   GET  /events/{eventId}/ratings   — lista pública (sin auth)
 *   POST /events/{eventId}/ratings   — crear comentario (cualquier usuario autenticado, R1)
 *
 * Reglas de negocio aplicadas en el backend:
 *   R1 — Cualquier usuario autenticado puede comentar.
 *   R2 — Múltiples comentarios por persona y evento.
 *   R3 — El primer comentario con rating en el evento lleva calificación; los siguientes no.
 *   R4 — El organizador del evento nunca lleva calificación.
 *   R5 — Antispam: máximo 1 comentario cada 10 segundos por persona/evento.
 *
 * El payload de GET y POST tiene la misma forma:
 *   { ratingId, userId, authorName, rating (nullable), comment, isVisible, isOrganizer, createdAt }
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
 * Requiere autenticación — cualquier rol puede comentar (R1).
 *
 * @param {string|number} eventId
 * @param {{ rating?: number, comment: string }} payload
 *   - rating: entero entre 1 y 5, opcional (R3/R4 deciden si aplica)
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
