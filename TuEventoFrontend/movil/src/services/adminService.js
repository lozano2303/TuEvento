const BASE_URL = process.env.EXPO_PUBLIC_API_URL;

/**
 * Lista todas las solicitudes de organizador pendientes.
 * Requiere rol ADMIN.
 * @returns {Promise<Array<{organizerPetitionId, userId, alias, status, applicationDate, storedFileId}>>}
 */
export const getOrganizerRequests = async (accessToken) => {
  const response = await fetch(`${BASE_URL}/admin/organizer-requests`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!response.ok) throw new Error(`Error fetching organizer requests: ${response.status}`);
  const json = await response.json();
  return json.data;
};

/**
 * Obtiene la URL presignada de un archivo almacenado.
 * @returns {Promise<{storedFileId: number, publicUrl: string}>}
 */
export const getFilePresignedUrl = async (fileId, accessToken) => {
  const response = await fetch(`${BASE_URL}/storage/${fileId}/url`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!response.ok) throw new Error(`Error fetching presigned URL: ${response.status}`);
  const json = await response.json();
  return json.data;
};

/**
 * Aprueba una solicitud de organizador y asigna el rol ORGANIZER al usuario.
 * @returns {Promise<void>}
 */
export const approveOrganizerRequest = async (petitionId, accessToken) => {
  const response = await fetch(
    `${BASE_URL}/admin/organizer-requests/${petitionId}/approve`,
    {
      method: "PUT",
      headers: { Authorization: `Bearer ${accessToken}` },
    }
  );
  if (!response.ok) throw new Error(`Error approving organizer request: ${response.status}`);
  const json = await response.json();
  return json.data;
};

/**
 * Rechaza una solicitud de organizador.
 * @returns {Promise<void>}
 */
export const rejectOrganizerRequest = async (petitionId, accessToken) => {
  const response = await fetch(
    `${BASE_URL}/admin/organizer-requests/${petitionId}/reject`,
    {
      method: "PUT",
      headers: { Authorization: `Bearer ${accessToken}` },
    }
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
  const token = await import("@react-native-async-storage/async-storage").then(
    (m) => m.default.getItem("accessToken")
  );
  const url = status
    ? `${BASE_URL}/admin/events?status=${status}`
    : `${BASE_URL}/admin/events`;
  const response = await fetch(url, {
    headers: { Authorization: `Bearer ${token}` },
  });
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
  const token = await import("@react-native-async-storage/async-storage").then(
    (m) => m.default.getItem("accessToken")
  );
  const body = { newStatus, ...(reason ? { reason } : {}) };
  const response = await fetch(`${BASE_URL}/admin/events/${eventId}/status`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    const err = await response.json().catch(() => ({}));
    throw new Error(err.message || `Error changing event status: ${response.status}`);
  }
  return response.json();
};
