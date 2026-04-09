const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export const getAllEvents = async () => {
  const response = await fetch(`${API_URL}/events`);
  if (!response.ok) throw new Error('Error al obtener eventos');
  return response.json();
};

export const cancelEvent = async (eventId) => {
  const token = localStorage.getItem('token');
  const response = await fetch(`${API_URL}/events/${eventId}/cancel`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });
  if (!response.ok) throw new Error('Error al cancelar evento');
  return response.json();
};

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
  return response.json();
};

export const getEventById = async (eventId) => {
  const response = await fetch(`${API_URL}/events/${eventId}`);
  if (!response.ok) throw new Error('Error al obtener evento');
  return response.json();
};

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
  if (!response.ok) throw new Error('Error al actualizar evento');
  return response.json();
};