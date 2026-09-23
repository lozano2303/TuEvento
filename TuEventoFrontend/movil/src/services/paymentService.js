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
 * Inicia un pago para una orden.
 * POST /api/v1/payments
 * @param {{ orderId: number, paymentMethod?: string }} params
 * @returns {Promise<Object>} PaymentResponse — contiene paymentId y gatewayTransactionId
 */
export const createPayment = async ({ orderId, paymentMethod = "QR" }) => {
  const response = await fetch(`${BASE_URL}/payments`, {
    method: "POST",
    headers: await authHeaders(),
    body: JSON.stringify({ orderId, paymentMethod }),
  });
  // PaymentController retorna el objeto directo, sin wrapper ApiResponse
  const json = await response.json();
  if (!response.ok) throw new Error(json.message || "Error al iniciar el pago");
  return json;
};

/**
 * Obtiene el estado actual de un pago.
 * GET /api/v1/payments/:paymentId
 * @param {number} paymentId
 * @returns {Promise<Object>} PaymentResponse
 */
export const getPayment = async (paymentId) => {
  const response = await fetch(`${BASE_URL}/payments/${paymentId}`, {
    headers: await authHeaders(),
  });
  // PaymentController retorna el objeto directo, sin wrapper ApiResponse
  const json = await response.json();
  if (!response.ok) throw new Error(json.message || "Error al obtener el pago");
  return json;
};
