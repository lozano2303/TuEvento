/**
 * EventService — piloto de migración a httpClient.
 *
 * Los endpoints que requieren auth usan httpRequest() en vez de fetch() directo.
 * httpRequest() maneja automáticamente el refresh del token y logout en caso de
 * sesión expirada, sin que el componente tenga que hacer nada especial.
 *
 * Los endpoints públicos (sin auth) siguen usando fetch() directamente —
 * no necesitan el wrapper.
 */
import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/** Listado público de eventos publicados — GET /events/public, sin auth. */
export const getAllEvents = async () => {
  const response = await fetch(`${API_URL}/events/public`);
  if (!response.ok) throw new Error('Error al obtener eventos');
  return response.json();
};

/** Eventos de un usuario específico — GET /events/user/{userId}, auth requerida. */
export const getEventsByUser = async (userId) => {
  const res = await httpRequest(`${API_URL}/events/user/${userId}`);
  if (!res.ok) throw new Error('Error al obtener tus eventos');
  return res.json();
};

/** Obtener evento por ID — GET /events/{eventId}, sin auth. */
export const getEventById = async (eventId) => {
  const response = await fetch(`${API_URL}/events/${eventId}`);
  if (!response.ok) throw new Error('Error al obtener evento');
  return response.json();
};

/** Crear evento — POST /events, auth ORGANIZER. */
export const createEvent = async (eventData) => {
  const res = await httpRequest(`${API_URL}/events`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(eventData),
  });
  if (!res.ok) throw new Error('Error al crear evento');
  return res.json();
};

/** Actualizar evento — PUT /events/{eventId}, auth ORGANIZER. */
export const updateEvent = async (eventId, eventData) => {
  const res = await httpRequest(`${API_URL}/events/${eventId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(eventData),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.message || 'Error al actualizar evento');
  return data;
};

/**
 * Cambiar estado de un evento — PATCH /events/{eventId}/status, auth ORGANIZER.
 * newStatus: 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED'
 */
export const changeEventStatus = async (eventId, newStatus) => {
  const res = await httpRequest(`${API_URL}/events/${eventId}/status`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ newStatus }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.message || 'Error al cambiar el estado');
  return data;
};

/** Eliminar evento — DELETE /events/{eventId}, auth ORGANIZER. */
export const deleteEvent = async (eventId) => {
  const res = await httpRequest(`${API_URL}/events/${eventId}`, {
    method: 'DELETE',
  });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || 'Error al eliminar el evento');
  }
  return true;
};

/**
 * @deprecated — cancelEvent usaba PUT /events/{id}/cancel que no existe en el backend.
 * Usar changeEventStatus(eventId, 'CANCELLED') en su lugar.
 */
export const cancelEvent = async (eventId) => {
  return changeEventStatus(eventId, 'CANCELLED');
};
