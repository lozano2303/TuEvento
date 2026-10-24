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

/**
 * Lista usuarios para el panel admin (más recientes primero).
 * GET /api/v1/admin/users?q=&role=&limit=
 * @param {{ q?: string, role?: string, limit?: number }} params
 * @returns {Promise<Array<{userId, alias, fullName, email, role, status, createdAt}>>}
 */
export const getAdminUsers = async ({ q = "", role = "", limit = 100 } = {}) => {
  const params = [`limit=${limit}`];
  if (q.trim()) params.push(`q=${encodeURIComponent(q.trim())}`);
  if (role) params.push(`role=${role}`);
  const response = await httpRequest(`${BASE_URL}/admin/users?${params.join("&")}`);
  if (!response.ok) throw new Error(`Error fetching users: ${response.status}`);
  const json = await response.json();
  return json.data ?? [];
};

/**
 * Quita el rol de organizador (vuelve a USER) y notifica el motivo al usuario.
 * PATCH /api/v1/admin/users/{userId}/revoke-organizer
 * @param {number} userId
 * @param {string} reason - entre 10 y 500 caracteres
 */
export const revokeOrganizerRole = async (userId, reason) => {
  const response = await httpRequest(`${BASE_URL}/admin/users/${userId}/revoke-organizer`, {
    method: "PATCH",
    headers: JSON_HEADERS,
    body: JSON.stringify({ reason }),
  });
  if (!response.ok) {
    const err = await response.json().catch(() => ({}));
    throw new Error(err.message || `Error revoking organizer role: ${response.status}`);
  }
  return response.json();
};

/**
 * Reembolsos más recientes con desglose del pago, comprador y evento.
 * GET /api/v1/admin/refunds?limit=
 * @returns {Promise<Array<{refundId, userId, alias, fullName, email, eventId, eventName, orderId,
 *   gatewayAmount, walletAmount, currency, status, reason, requestedAt, processedAt}>>}
 */
export const getAdminRefunds = async (limit = 200) => {
  const response = await httpRequest(`${BASE_URL}/admin/refunds?limit=${limit}`);
  if (!response.ok) throw new Error(`Error fetching refunds: ${response.status}`);
  const json = await response.json();
  return json.data ?? [];
};

/**
 * Acredita saldo manualmente en la cartera de un usuario (crea la cartera si no existe).
 * POST /api/v1/wallet/credit — el usuario recibe una notificación con el motivo.
 * @param {{ userId: number, amount: number, reason: string }} params
 */
export const adminCreditWallet = async ({ userId, amount, reason }) => {
  const response = await httpRequest(`${BASE_URL}/wallet/credit`, {
    method: "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify({ userId, amount, reason }),
  });
  if (!response.ok) {
    const err = await response.json().catch(() => ({}));
    throw new Error(err.message || `Error crediting wallet: ${response.status}`);
  }
  return response.json();
};
