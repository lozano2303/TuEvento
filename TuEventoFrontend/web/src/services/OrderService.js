import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/**
 * Crea una orden con las sillas seleccionadas.
 * POST /api/v1/orders
 * @param {{ eventId: number, seatIds: number[] }} params
 * @returns {{ success: true, data: OrderResponse }}
 */
export const createOrder = async ({ eventId, seatIds }) => {
  const res = await httpRequest(`${API_URL}/orders`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ eventId, seatIds }),
  });
  const body = await res.json();
  if (!res.ok) throw new Error(body.message || 'Error al crear la orden');
  return { success: true, data: body.data ?? body };
};

/**
 * Obtiene una orden por ID.
 * GET /api/v1/orders/:orderId
 * @param {number} orderId
 * @returns {{ success: true, data: OrderResponse }}
 */
export const getOrder = async (orderId) => {
  const res = await httpRequest(`${API_URL}/orders/${orderId}`);
  const body = await res.json();
  if (!res.ok) throw new Error(body.message || 'Error al obtener la orden');
  return { success: true, data: body.data ?? body };
};

/**
 * Obtiene los tickets de una orden.
 * GET /api/v1/orders/:orderId/tickets
 * @param {number} orderId
 * @returns {{ success: true, data: TicketResponse[] }}
 */
export const getOrderTickets = async (orderId) => {
  const res = await httpRequest(`${API_URL}/orders/${orderId}/tickets`);
  const body = await res.json();
  if (!res.ok) throw new Error(body.message || 'Error al obtener los tickets');
  return { success: true, data: body.data ?? body };
};

/**
 * Cancela una orden.
 * POST /api/v1/orders/:orderId/cancel
 * @param {number} orderId
 * @returns {{ success: true, data: OrderResponse }}
 */
export const cancelOrder = async (orderId) => {
  const res = await httpRequest(`${API_URL}/orders/${orderId}/cancel`, {
    method: 'POST',
  });
  const body = await res.json();
  if (!res.ok) throw new Error(body.message || 'Error al cancelar la orden');
  return { success: true, data: body.data ?? body };
};
