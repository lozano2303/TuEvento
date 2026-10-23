import { httpRequest } from './httpClient';

/**
 * Servicio para interactuar con el módulo de notificaciones.
 */

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';
const BASE_PATH = `${API_URL}/notifications/me`;

/**
 * Obtiene todas las notificaciones del usuario autenticado.
 * 
 * @param {Object} params - Parámetros de filtrado (opcional)
 * @param {boolean} params.unreadOnly - Si true, solo devuelve no leídas
 * @param {number} params.page - Página (paginación futura)
 * @param {number} params.size - Tamaño de página
 * @returns {Promise<Object>} Respuesta con lista de notificaciones
 */
export async function getUserNotifications(params = {}) {
  const { unreadOnly = false, page = 0, size = 20 } = params;
  const queryParams = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
    unreadOnly: unreadOnly.toString(),
  });
  
  const endpoint = `${BASE_PATH}?${queryParams.toString()}`;
  
  const response = await httpRequest(endpoint, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
  });
  
  if (!response.ok) {
    throw new Error('Failed to fetch notifications');
  }
  
  return response.json();
}

/**
 * Marca una notificación específica como leída.
 * 
 * @param {number} notificationUserId - ID de la entrada en notification_user
 * @returns {Promise<Object>} Respuesta del servidor
 */
export async function markAsRead(notificationUserId) {
  const response = await httpRequest(`${API_URL}/notifications/${notificationUserId}/read`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
  });
  
  if (!response.ok) {
    throw new Error('Failed to mark notification as read');
  }
  
  return response.json();
}

/**
 * Marca todas las notificaciones del usuario como leídas.
 * 
 * @returns {Promise<Object>} Respuesta del servidor
 */
export async function markAllAsRead() {
  const response = await httpRequest(`${BASE_PATH}/read-all`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
  });
  
  if (!response.ok) {
    throw new Error('Failed to mark all notifications as read');
  }
  
  return response.json();
}

/**
 * Obtiene el conteo de notificaciones no leídas.
 * 
 * @returns {Promise<number>} Cantidad de notificaciones no leídas
 */
export async function getUnreadCount() {
  const response = await httpRequest(`${BASE_PATH}/unread-count`, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
  });
  
  if (!response.ok) {
    throw new Error('Failed to fetch unread count');
  }
  
  const data = await response.json();
  return data.data || 0;
}
