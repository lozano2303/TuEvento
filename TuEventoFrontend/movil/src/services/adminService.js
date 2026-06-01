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
