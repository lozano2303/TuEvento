import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/** Obtiene las secciones de un evento — público, sin auth. */
export const getByEvent = async (eventId) => {
  const res = await fetch(`${API_URL}/event-sections/event/${eventId}`);
  if (!res.ok) throw new Error('Error al obtener secciones del evento');
  return res.json(); // ApiResponse<List<EventSectionResponse>>
  // EventSectionResponse: { eventSectionId, eventId, sectionTypeName, capacity, availableSeats, price, isActive }
};

/** Crea una sección para un evento — requiere ORGANIZER/ADMIN. */
export const create = async ({ eventId, sectionTypeId, capacity, price }) => {
  const res = await httpRequest(`${API_URL}/event-sections`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ eventId, sectionTypeId, capacity, price }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.message || 'Error al crear la sección');
  return data; // ApiResponse<EventSectionResponse>
};

/** Actualiza capacidad, precio o isActive de una sección — requiere ORGANIZER/ADMIN. */
export const update = async (eventSectionId, { capacity, price, isActive }) => {
  const body  = {};
  if (capacity  !== undefined) body.capacity  = capacity;
  if (price     !== undefined) body.price      = price;
  if (isActive  !== undefined) body.isActive   = isActive;
  const res = await httpRequest(`${API_URL}/event-sections/${eventSectionId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.message || 'Error al actualizar la sección');
  return data; // ApiResponse<EventSectionResponse>
};

/** Elimina una sección — requiere ORGANIZER/ADMIN. */
export const remove = async (eventSectionId) => {
  const res = await httpRequest(`${API_URL}/event-sections/${eventSectionId}`, {
    method: 'DELETE',
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.message || 'Error al eliminar la sección');
  return data;
};
