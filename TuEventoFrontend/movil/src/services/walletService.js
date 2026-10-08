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
 * Obtiene el balance total y disponible de la wallet del usuario autenticado.
 * GET /api/v1/wallet/me
 * @returns {Promise<Object>} WalletResponse — { walletId, userId, balance, availableBalance, currency }
 * @throws si el usuario no tiene wallet (404)
 */
export const getMyWallet = async () => {
  const response = await fetch(`${BASE_URL}/wallet/me`, {
    headers: await authHeaders(),
  });
  const json = await response.json();
  if (!response.ok) throw new Error(json.message || "Error al obtener la wallet");
  return json.data;
};

/**
 * Obtiene el historial de movimientos de la wallet del usuario autenticado.
 * GET /api/v1/wallet/me/transactions
 * @returns {Promise<Array>} Array de WalletTransactionResponse
 */
export const getMyWalletTransactions = async () => {
  const response = await fetch(`${BASE_URL}/wallet/me/transactions`, {
    headers: await authHeaders(),
  });
  const json = await response.json();
  if (!response.ok) throw new Error(json.message || "Error al obtener el historial");
  return json.data ?? [];
};
