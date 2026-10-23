import { httpRequest } from "./httpClient";

const BASE_URL = process.env.EXPO_PUBLIC_API_URL;

const JSON_HEADERS = { "Content-Type": "application/json" };

// ══════════════════════════════════════════════════════════════════════════════
// COMPRADOR — órdenes y tickets
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Órdenes del usuario autenticado, opcionalmente filtradas por estado.
 * GET /api/v1/orders/my?status=PAID
 * @param {string|null} status - "PAID" | "PAYMENT_PENDING" | ... | null (todas)
 * @returns {Promise<Array>} OrderResponse[] con sus tickets
 */
export const getMyOrders = async (status = null) => {
  const url = status ? `${BASE_URL}/orders/my?status=${status}` : `${BASE_URL}/orders/my`;
  const response = await httpRequest(url);
  const json = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(json.message || "Error al obtener las órdenes");
  return json.data ?? [];
};

/**
 * Tickets del usuario autenticado.
 * GET /api/v1/tickets/my
 * @returns {Promise<Array>} TicketResponse[]
 */
export const getMyTickets = async () => {
  const response = await httpRequest(`${BASE_URL}/tickets/my`);
  const json = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(json.message || "Error al obtener los tickets");
  return json.data ?? [];
};

/**
 * Tickets de una orden.
 * GET /api/v1/orders/{orderId}/tickets
 */
export const getOrderTickets = async (orderId) => {
  const response = await httpRequest(`${BASE_URL}/orders/${orderId}/tickets`);
  const json = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(json.message || "Error al obtener los tickets de la orden");
  return json.data ?? [];
};

// ══════════════════════════════════════════════════════════════════════════════
// QR — generación (comprador)
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Genera el QR de un ticket individual. Caduca en segundos; regenerarlo invalida
 * el anterior.
 * POST /api/v1/tickets/{ticketId}/qr
 * @returns {Promise<{scope, targetId, qrToken, expiresAt, expiresInSeconds}>}
 */
export const generateTicketQr = async (ticketId) => {
  const response = await httpRequest(`${BASE_URL}/tickets/${ticketId}/qr`, {
    method: "POST",
    headers: JSON_HEADERS,
  });
  const json = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(json.message || "Error al generar el QR del ticket");
  return json.data;
};

/**
 * Genera el QR de la orden completa (valida las sillas PAID restantes).
 * POST /api/v1/orders/{orderId}/qr
 * @returns {Promise<{scope, targetId, qrToken, expiresAt, expiresInSeconds}>}
 */
export const generateOrderQr = async (orderId) => {
  const response = await httpRequest(`${BASE_URL}/orders/${orderId}/qr`, {
    method: "POST",
    headers: JSON_HEADERS,
  });
  const json = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(json.message || "Error al generar el QR de la orden");
  return json.data;
};

// ══════════════════════════════════════════════════════════════════════════════
// QR — escaneo (organizador)
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Valida un QR escaneado. Solo el organizador dueño del evento puede.
 * POST /api/v1/tickets/scan
 * @param {string} qrToken - token leído del código QR
 * @returns {Promise<Object>} ScanResultResponse
 * @throws Error con .code del backend (QR_INVALID, QR_EXPIRED_OR_REPLACED, etc.)
 */
export const scanQr = async (qrToken) => {
  const response = await httpRequest(`${BASE_URL}/tickets/scan`, {
    method: "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify({ qrToken }),
  });
  const json = await response.json().catch(() => ({}));
  if (!response.ok) {
    const err = new Error(json.message || "No se pudo validar el QR");
    err.code = json.code ?? null;
    err.status = response.status;
    throw err;
  }
  return json.data;
};
