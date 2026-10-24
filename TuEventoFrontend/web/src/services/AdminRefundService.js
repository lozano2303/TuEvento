import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/**
 * Reembolsos más recientes con desglose del pago, comprador y evento.
 * GET /api/v1/admin/refunds?limit=
 * @returns {Promise<Array<{ refundId, paymentId, orderId, userId, alias, fullName, email,
 *   eventId, eventName, gatewayAmount, walletAmount, currency, status, reason, requestedAt, processedAt }>>}
 */
export const getAdminRefunds = async (limit = 200) => {
  const res  = await httpRequest(`${API_URL}/admin/refunds?limit=${limit}`);
  const body = await res.json();
  if (!res.ok) throw new Error(body.message || 'Error al obtener reembolsos');
  return body.data ?? [];
};

/**
 * Estadísticas globales (incluye pendingRefunds, processedRefunds, totalRefundedAmount).
 * GET /api/v1/admin/stats
 */
export const getAdminStats = async () => {
  const res  = await httpRequest(`${API_URL}/admin/stats`);
  const body = await res.json();
  if (!res.ok) throw new Error(body.message || 'Error al obtener estadísticas');
  return body.data ?? {};
};
