import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/**
 * Inicia un pago para una orden.
 * POST /api/v1/payments
 * @param {{ orderId: number, paymentMethod: string }} params
 * @returns {{ success: true, data: PaymentResponse }}
 */
export const createPayment = async ({ orderId, paymentMethod }) => {
  const res = await httpRequest(`${API_URL}/payments`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ orderId, paymentMethod }),
  });
  const body = await res.json();
  if (!res.ok) throw new Error(body.message || 'Error al iniciar el pago');
  // PaymentController retorna PaymentResponse directo, sin wrapper ApiResponse
  return { success: true, data: body };
};

/**
 * Obtiene el estado actual de un pago.
 * GET /api/v1/payments/:paymentId
 * Nota: PaymentController retorna PaymentResponse directamente (sin wrapper ApiResponse).
 * @param {number} paymentId
 * @returns {{ success: true, data: PaymentResponse }}
 */
export const getPayment = async (paymentId) => {
  const res = await httpRequest(`${API_URL}/payments/${paymentId}`);
  const body = await res.json();
  if (!res.ok) throw new Error(body.message || 'Error al obtener el pago');
  // PaymentController retorna el objeto directo, sin data wrapper
  return { success: true, data: body };
};
