const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/** Listado público de eventos publicados — GET /events/public, sin auth. */
export const getAllEvents = async () => {
  const response = await fetch(`${API_URL}/events/public`);
  if (!response.ok) throw new Error('Error al obtener eventos');
  return response.json(); // ApiResponse<List<EventSummaryResponse>>
};

/** Eventos de un usuario específico — GET /events/user/{userId}, auth requerida.
 *  Siempre se llama con el propio userId del usuario logueado, por lo que
 *  el token siempre está disponible. Devuelve también eventos en DRAFT. */
export const getEventsByUser = async (userId) => {
  const token = localStorage.getItem('token');
  const res = await fetch(`${API_URL}/events/user/${userId}`, {
    headers: token ? { 'Authorization': `Bearer ${token}` } : {},
  });
  if (!res.ok) throw new Error('Error al obtener tus eventos');
  return res.json(); // ApiResponse<List<EventSummaryResponse>>
};

/** Obtener evento por ID — GET /events/{eventId}, sin auth. */
export const getEventById = async (eventId) => {
  const response = await fetch(`${API_URL}/events/${eventId}`);
  if (!response.ok) throw new Error('Error al obtener evento');
  return response.json(); // ApiResponse<EventResponse>
};

/** Crear evento — POST /events, auth ORGANIZER. */
export const createEvent = async (eventData) => {
  const token = localStorage.getItem('token');
  const response = await fetch(`${API_URL}/events`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(eventData),
  });
  if (!response.ok) throw new Error('Error al crear evento');
  return response.json(); // ApiResponse<EventResponse>
};

/** Actualizar evento — PUT /events/{eventId}, auth ORGANIZER. */
export const updateEvent = async (eventId, eventData) => {
  const token = localStorage.getItem('token');
  const response = await fetch(`${API_URL}/events/${eventId}`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(eventData),
  });
  const data = await response.json();
  if (!response.ok) throw new Error(data.message || 'Error al actualizar evento');
  return data; // ApiResponse<EventResponse>
};

/**
 * Cambiar estado de un evento — PATCH /events/{eventId}/status, auth ORGANIZER.
 * newStatus: 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED'
 */
export const changeEventStatus = async (eventId, newStatus) => {
  const token = localStorage.getItem('token');
  const res = await fetch(`${API_URL}/events/${eventId}/status`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify({ newStatus }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.message || 'Error al cambiar el estado');
  return data; // ApiResponse<EventStatusLogResponse>
};

/** Eliminar evento — DELETE /events/{eventId}, auth ORGANIZER. */
export const deleteEvent = async (eventId) => {
  const token = localStorage.getItem('token');
  const res = await fetch(`${API_URL}/events/${eventId}`, {
    method: 'DELETE',
    headers: { 'Authorization': `Bearer ${token}` },
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
 * Se mantiene temporalmente para no romper Events.jsx hasta que se migre.
 */
export const cancelEvent = async (eventId) => {
  return changeEventStatus(eventId, 'CANCELLED');
};
