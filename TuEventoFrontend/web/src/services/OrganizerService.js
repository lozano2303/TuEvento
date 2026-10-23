import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/**
 * Obtiene las ganancias del organizador autenticado.
 * GET /api/v1/organizer/earnings
 *
 * @returns {{ success: true, data: OrganizerEarningsResponse }}
 * @throws si el usuario no está autenticado (401) o no es ORGANIZER/ADMIN (403)
 */
export const getOrganizerEarnings = async () => {
  const res  = await httpRequest(`${API_URL}/organizer/earnings`);
  const body = await res.json();
  if (!res.ok) throw new Error(body.message || 'Error al obtener las ganancias');
  return { success: true, data: body.data };
};
