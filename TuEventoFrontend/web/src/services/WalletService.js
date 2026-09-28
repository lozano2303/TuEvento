import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/**
 * Obtiene el balance y balance disponible del usuario autenticado.
 * GET /api/v1/wallet/me
 * @returns {{ success: true, data: WalletResponse }}
 * @throws si el usuario no tiene wallet (404) o no está autenticado (401)
 */
export const getMyWallet = async () => {
  const res  = await httpRequest(`${API_URL}/wallet/me`);
  const body = await res.json();
  if (!res.ok) throw new Error(body.message || 'Error al obtener la wallet');
  return { success: true, data: body.data };
};

/**
 * Obtiene el historial de movimientos de la wallet del usuario autenticado.
 * GET /api/v1/wallet/me/transactions
 * @returns {{ success: true, data: WalletTransactionResponse[] }}
 */
export const getMyWalletTransactions = async () => {
  const res  = await httpRequest(`${API_URL}/wallet/me/transactions`);
  const body = await res.json();
  if (!res.ok) throw new Error(body.message || 'Error al obtener el historial de wallet');
  return { success: true, data: body.data };
};

/**
 * Acredita saldo en la wallet de un usuario (solo ADMIN).
 * POST /api/v1/wallet/credit
 * @param {{ userId: number, amount: number, reason: string }} params
 * @returns {{ success: true, data: WalletResponse }}
 */
export const creditWallet = async ({ userId, amount, reason }) => {
  const res  = await httpRequest(`${API_URL}/wallet/credit`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, amount, reason }),
  });
  const body = await res.json();
  if (!res.ok) throw new Error(body.message || 'Error al acreditar la wallet');
  return { success: true, data: body.data };
};

/**
 * Ajuste administrativo de balance (solo ADMIN).
 * POST /api/v1/wallet/adjust
 * @param {{ userId: number, amount: number, reason: string }} params — amount puede ser negativo
 * @returns {{ success: true, data: WalletResponse }}
 */
export const adjustWallet = async ({ userId, amount, reason }) => {
  const res  = await httpRequest(`${API_URL}/wallet/adjust`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, amount, reason }),
  });
  const body = await res.json();
  if (!res.ok) throw new Error(body.message || 'Error al ajustar la wallet');
  return { success: true, data: body.data };
};
