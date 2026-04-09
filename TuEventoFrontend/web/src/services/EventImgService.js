const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export const getEventImages = async (eventId) => {
  const response = await fetch(`${API_URL}/events/${eventId}/images`);
  if (!response.ok) throw new Error('Error al obtener imágenes');
  return response.json();
};

export const uploadEventImage = async (eventId, imageFile) => {
  const token = localStorage.getItem('token');
  const formData = new FormData();
  formData.append('image', imageFile);
  
  const response = await fetch(`${API_URL}/events/${eventId}/images`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
    body: formData,
  });
  if (!response.ok) throw new Error('Error al subir imagen');
  return response.json();
};

export const deleteEventImage = async (eventId, imageId) => {
  const token = localStorage.getItem('token');
  const response = await fetch(`${API_URL}/events/${eventId}/images/${imageId}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
  if (!response.ok) throw new Error('Error al eliminar imagen');
  return response.json();
};