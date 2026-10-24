import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/**
 * Lista usuarios para el panel admin (más recientes primero).
 * GET /api/v1/admin/users?q=&role=&limit=
 * @param {{ q?: string, role?: 'USER'|'ORGANIZER'|'ADMIN'|'', limit?: number }} params
 * @returns {Promise<Array<{ userId, alias, fullName, email, role, status, createdAt }>>}
 */
export const getAdminUsers = async ({ q = '', role = '', limit = 100 } = {}) => {
  const params = new URLSearchParams();
  if (q.trim()) params.set('q', q.trim());
  if (role) params.set('role', role);
  params.set('limit', String(limit));
  const res  = await httpRequest(`${API_URL}/admin/users?${params}`);
  const body = await res.json();
  if (!res.ok) throw new Error(body.message || 'Error al obtener usuarios');
  return body.data ?? [];
};

/**
 * Quita el rol de organizador a un usuario (vuelve a USER) y le notifica el motivo.
 * PATCH /api/v1/admin/users/{userId}/revoke-organizer
 * @param {number} userId
 * @param {string} reason - entre 10 y 500 caracteres
 */
export const revokeOrganizerRole = async (userId, reason) => {
  const res = await httpRequest(`${API_URL}/admin/users/${userId}/revoke-organizer`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason }),
  });
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(body.message || 'Error al quitar el rol de organizador');
  return body;
};
