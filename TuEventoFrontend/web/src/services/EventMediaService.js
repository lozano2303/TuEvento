const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/**
 * Obtiene las imágenes de un evento — GET /events/{eventId}/media, público.
 * Devuelve ApiResponse<List<EventMediaResponse>>
 * Cada item tiene: { mediaId, eventId, imgUrl }
 */
export const getEventMedia = async (eventId) => {
  const res = await fetch(`${API_URL}/events/${eventId}/media`);
  if (!res.ok) throw new Error('Error al obtener las imágenes');
  return res.json();
};

/**
 * Sube una imagen para un evento — POST /events/{eventId}/media, auth ORGANIZER.
 * El campo multipart correcto es "file" (no "image").
 * No se debe setear Content-Type manualmente — el browser lo incluye con boundary.
 * Devuelve ApiResponse<EventMediaResponse>
 */
export const uploadEventMedia = async (eventId, imageFile) => {
  const token = localStorage.getItem('token');
  const formData = new FormData();
  formData.append('file', imageFile); // campo correcto según EventMediaController
  const res = await fetch(`${API_URL}/events/${eventId}/media`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
    body: formData,
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.message || 'Error al subir la imagen');
  return data;
};

// NOTA: El backend (EventMediaController) no expone un endpoint DELETE para media.
// Las imágenes de evento no se pueden eliminar una vez subidas — solo agregar más.
// Limitación conocida — pendiente de implementar en el backend si se requiere.
