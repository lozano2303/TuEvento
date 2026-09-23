import AsyncStorage from "@react-native-async-storage/async-storage";

const BASE_URL = process.env.EXPO_PUBLIC_API_URL;

async function authHeaders() {
  const token = await AsyncStorage.getItem("accessToken");
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

/**
 * Crea una orden con las sillas seleccionadas.
 * POST /api/v1/orders
 * @param {{ eventId: number, seatIds: number[] }} params
 * @returns {Promise<Object>} OrderResponse
 */
export const createOrder = async ({ eventId, seatIds }) => {
  const response = await fetch(`${BASE_URL}/orders`, {
    method: "POST",
    headers: await authHeaders(),
    body: JSON.stringify({ eventId, seatIds }),
  });
  const json = await response.json();
  if (!response.ok) throw new Error(json.message || "Error al crear la orden");
  return json.data ?? json;
};

/**
 * Obtiene una orden por ID.
 * GET /api/v1/orders/:orderId
 * @param {number} orderId
 * @returns {Promise<Object>} OrderResponse
 */
export const getOrder = async (orderId) => {
  const response = await fetch(`${BASE_URL}/orders/${orderId}`, {
    headers: await authHeaders(),
  });
  const json = await response.json();
  if (!response.ok) throw new Error(json.message || "Error al obtener la orden");
  return json.data ?? json;
};

/**
 * Obtiene los tickets de una orden.
 * GET /api/v1/orders/:orderId/tickets
 * @param {number} orderId
 * @returns {Promise<Array>} Array de TicketResponse
 */
export const getOrderTickets = async (orderId) => {
  const response = await fetch(`${BASE_URL}/orders/${orderId}/tickets`, {
    headers: await authHeaders(),
  });
  const json = await response.json();
  if (!response.ok) throw new Error(json.message || "Error al obtener los tickets");
  return json.data ?? json;
};
