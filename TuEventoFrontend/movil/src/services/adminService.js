import { httpRequest } from "./httpClient";

const BASE_URL = process.env.EXPO_PUBLIC_API_URL;

const JSON_HEADERS = { "Content-Type": "application/json" };

/**
 * Lista todas las solicitudes de organizador pendientes.
 * Requiere rol ADMIN.
 * @returns {Promise<Array<{organizerPetitionId, userId, fullName, email, documentType, status, applicationDate, storedFileId, profilePicture}>>}
 */
export const getOrganizerRequests = async () => {
  const response = await httpRequest(`${BASE_URL}/admin/organizer-requests`);
  if (!response.ok) throw new Error(`Error fetching organizer requests: ${response.status}`);
  const json = await response.json();
  return json.data;
};

/**
 * Contadores de pendientes del admin (solicitudes de organizador y eventos en revisión).
 * Las actualizaciones en vivo llegan por WebSocket en /topic/admin/pending-counts.
 * @returns {Promise<{organizerRequests: number, eventsInReview: number}>}
 */
export const getAdminPendingCounts = async () => {
  const response = await httpRequest(`${BASE_URL}/admin/pending-counts`);
  if (!response.ok) throw new Error(`Error fetching pending counts: ${response.status}`);
  const json = await response.json();
  return json.data;
};

/**
 * Obtiene la URL presignada de un archivo almacenado.
 * @returns {Promise<{storedFileId: number, publicUrl: string}>}
 */
export const getFilePresignedUrl = async (fileId) => {
  const response = await httpRequest(`${BASE_URL}/storage/${fileId}/url`);
  if (!response.ok) throw new Error(`Error fetching presigned URL: ${response.status}`);
  const json = await response.json();
  return json.data;
};

/**
 * Aprueba una solicitud de organizador y asigna el rol ORGANIZER al usuario.
 * @returns {Promise<void>}
 */
export const approveOrganizerRequest = async (petitionId) => {
  const response = await httpRequest(
    `${BASE_URL}/admin/organizer-requests/${petitionId}/approve`,
    { method: "PUT" }
  );
  if (!response.ok) throw new Error(`Error approving organizer request: ${response.status}`);
  const json = await response.json();
  return json.data;
};

/**
 * Rechaza una solicitud de organizador.
 * @returns {Promise<void>}
 */
export const rejectOrganizerRequest = async (petitionId) => {
  const response = await httpRequest(
    `${BASE_URL}/admin/organizer-requests/${petitionId}/reject`,
    { method: "PUT" }
  );
  if (!response.ok) throw new Error(`Error rejecting organizer request: ${response.status}`);
  const json = await response.json();
  return json.data;
};

/**
 * Lista todos los eventos para revisión/gestión de admin.
 * GET /api/v1/admin/events?status=XXX
 * @param {string|null} status - Filtrar por estado (null = todos)
 * @returns {Promise<Array>}
 */
export const getAdminEvents = async (status = null) => {
  const url = status
    ? `${BASE_URL}/admin/events?status=${status}`
    : `${BASE_URL}/admin/events`;
  const response = await httpRequest(url);
  if (!response.ok) throw new Error(`Error fetching admin events: ${response.status}`);
  const json = await response.json();
  return json.data ?? [];
};

/**
 * Cambia el estado de un evento (publicar, rechazar, cancelar, finalizar).
 * PATCH /api/v1/admin/events/{eventId}/status
 * @param {number} eventId
 * @param {string} newStatus - 'PUBLISHED' | 'REJECTED' | 'CANCELLED' | 'COMPLETED'
 * @param {string|null} reason - Motivo (obligatorio para REJECTED)
 */
export const adminChangeEventStatus = async (eventId, newStatus, reason = null) => {
  const body = { newStatus, ...(reason ? { reason } : {}) };
  const response = await httpRequest(`${BASE_URL}/admin/events/${eventId}/status`, {
    method: "PATCH",
    headers: JSON_HEADERS,
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    const err = await response.json().catch(() => ({}));
    throw new Error(err.message || `Error changing event status: ${response.status}`);
  }
  return response.json();
};

/**
 * Estadísticas globales de la plataforma (dashboard, financiero, reportes).
 * GET /api/v1/admin/stats
 * @returns {Promise<Object>} AdminStats — totalUsers, totalEvents, totalRevenue, etc.
 */
export const getAdminStats = async () => {
  const response = await httpRequest(`${BASE_URL}/admin/stats`);
  if (!response.ok) throw new Error(`Error fetching admin stats: ${response.status}`);
  const json = await response.json();
  return json.data ?? {};
};

/**
 * Actividad reciente de la plataforma.
 * GET /api/v1/admin/activity?limit=N
 * @param {number} limit
 * @returns {Promise<Array<{type, title, description, actor, occurredAt}>>}
 */
export const getAdminActivity = async (limit = 5) => {
  const response = await httpRequest(`${BASE_URL}/admin/activity?limit=${limit}`);
  if (!response.ok) throw new Error(`Error fetching admin activity: ${response.status}`);
  const json = await response.json();
  return json.data ?? [];
};
