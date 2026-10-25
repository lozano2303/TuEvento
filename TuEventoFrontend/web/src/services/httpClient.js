/**
 * httpClient — wrapper de fetch centralizado con refresh automático de JWT.
 *
 * Comportamiento:
 *   1. Adjunta el access token (en memoria, ver tokenStore.js) vía Authorization: Bearer <token>
 *   2. Si la respuesta es 401, renueva el access token con POST /auth/refresh; el refresh
 *      token viaja solo en la cookie httpOnly (credentials: 'include' + X-Auth-Mode: cookie)
 *   3. Si el refresh tiene éxito, reintenta la request original una sola vez
 *   4. Si el refresh falla (token expirado, revocado, no existe), hace logout limpio
 *      y redirige a /login — el usuario ve la pantalla de login, no un error opaco
 *
 * Anti-condición-de-carrera:
 *   refreshPromise es un singleton en módulo. Si varios requests fallan con 401
 *   simultáneamente, todos esperan la misma promesa de refresh — solo se hace
 *   una llamada a /auth/refresh aunque haya 10 requests en vuelo.
 *   Entre pestañas, el refresh corre dentro de un Web Lock: el backend rota el
 *   refresh token en cada uso, y dos pestañas renovando a la vez invalidarían la sesión.
 *
 * Mantenibilidad futura:
 *   El catch de refreshAccessToken recibe el error original de la API
 *   ({ message, errorCode }) — si en el futuro se quiere mostrar "tu sesión expiró"
 *   vs "sesión inválida" vs "cuenta suspendida", el error está disponible para
 *   que performLogout reciba el motivo y lo muestre antes de redirigir.
 */

import { clearAccessToken, getAccessToken, setAccessToken } from './tokenStore.js';
import { disableGoogleAutoSelect } from './googleIdentity.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

// El backend pone el refresh token en una cookie httpOnly cuando recibe esta cabecera
export const AUTH_MODE_HEADERS = { 'X-Auth-Mode': 'cookie' };

const REFRESH_LOCK = 'tuevento-auth-refresh';

// Claves antiguas: antes los tokens se guardaban en localStorage
const LEGACY_TOKEN_KEYS = ['token', 'refreshToken'];

const SESSION_KEYS = [
  ...LEGACY_TOKEN_KEYS, 'userID', 'alias',
  'userEmail', 'name', 'role',
  'pendingActivationUserID', 'pendingActivationEmail', 'adminLoggedIn',
  'activeThemeId', 'activeThemeName',
];

// Singleton de refresh en vuelo — evita condición de carrera
let refreshPromise = null;

const clearLocalSession = () => {
  clearAccessToken();
  SESSION_KEYS.forEach((k) => localStorage.removeItem(k));
};

/**
 * Revoca la sesión en el backend (que además borra la cookie), limpia el cliente
 * y deja que el caller redirija a /login.
 * Esta es la única función de logout que debe usarse en todo el proyecto.
 *
 * Si el backend falla (red, token ya revocado, etc.), igual limpia el cliente
 * para garantizar que el usuario pueda iniciar sesión de nuevo.
 */
export async function performLogout() {
  let accessToken = getAccessToken();

  // Sin token en memoria (p. ej. falló restoreSession por red) se pide uno con la cookie
  // para poder revocar la sesión en el backend; si la cookie ya no vale, no hay nada que revocar
  if (!accessToken && localStorage.getItem('userID')) {
    try {
      accessToken = await refreshAccessToken();
    } catch {
      accessToken = null;
    }
  }

  if (accessToken) {
    try {
      await fetch(`${API_URL}/auth/logout`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${accessToken}`,
          ...AUTH_MODE_HEADERS,
        },
        body: JSON.stringify({ accessToken }),
      });
    } catch (err) {
      // Si falla (red, token expirado, etc.), igual limpiamos el cliente
      console.warn('[performLogout] No se pudo revocar la sesión en el backend:', err.message);
    }
  }

  clearLocalSession();
  // Que Google no vuelva a elegir la cuenta automáticamente en el próximo inicio de sesión
  disableGoogleAutoSelect();
}

const withRefreshLock = (fn) =>
  navigator.locks?.request ? navigator.locks.request(REFRESH_LOCK, fn) : fn();

async function requestNewAccessToken(tokenBefore) {
  // Otra pestaña renovó mientras esperábamos el lock: su token ya llegó por BroadcastChannel
  const current = getAccessToken();
  if (current && current !== tokenBefore) return current;

  // Migración: un refresh token antiguo en localStorage se canjea una vez y pasa a la cookie
  const legacyRefreshToken = localStorage.getItem('refreshToken');

  const res = await fetch(`${API_URL}/auth/refresh`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...AUTH_MODE_HEADERS },
    body: legacyRefreshToken ? JSON.stringify({ refreshToken: legacyRefreshToken }) : undefined,
  });

  if (!res.ok) {
    // Preservar el mensaje del backend para diagnóstico futuro
    // { success, message, data } — el campo message es legible por el usuario
    let errorCode = 'REFRESH_FAILED';
    try {
      const body = await res.json();
      errorCode = body.message || errorCode;
    } catch (_) { /* JSON parse failed — usar el código genérico */ }
    throw new Error(errorCode);
  }

  const { data } = await res.json();
  LEGACY_TOKEN_KEYS.forEach((k) => localStorage.removeItem(k));
  setAccessToken(data.accessToken);
  return data.accessToken;
}

/**
 * Renueva el access token con la cookie del refresh token.
 * Retorna el nuevo access token en éxito.
 * Lanza un error en cualquier otro caso (el caller hará logout).
 */
export async function refreshAccessToken() {
  // Si ya hay un refresh en vuelo, encolar en esa misma promesa
  if (refreshPromise) return refreshPromise;

  const tokenBefore = getAccessToken();
  refreshPromise = withRefreshLock(() => requestNewAccessToken(tokenBefore));

  try {
    return await refreshPromise;
  } finally {
    // Liberar el lock para el próximo ciclo de 401
    refreshPromise = null;
  }
}

/**
 * Recupera el access token al cargar la página (vive solo en memoria).
 * Se llama en main.jsx antes de montar React, así las comprobaciones síncronas
 * de sesión (getAccessToken()) funcionan desde el primer render.
 * Solo se intenta si hay rastro de una sesión (userID); si el backend la rechaza
 * se limpia el cliente. Un fallo de red no borra nada: el usuario reintenta al recargar.
 */
export async function restoreSession() {
  if (!localStorage.getItem('userID')) return;
  try {
    await refreshAccessToken();
  } catch (err) {
    if (!(err instanceof TypeError)) clearLocalSession();
  }
}

/**
 * Hace un fetch con Authorization header automático + refresh transparente en 401.
 *
 * @param {string} url — URL completa o relativa al API_URL
 * @param {RequestInit} options — mismas opciones que fetch()
 * @returns {Promise<Response>} — la Response de fetch (no parseada, igual que fetch nativo)
 */
export async function httpRequest(url, options = {}) {
  const token = getAccessToken();

  // Construir headers con Authorization inyectado
  const buildHeaders = (authToken) => ({
    ...options.headers,
    ...(authToken ? { 'Authorization': `Bearer ${authToken}` } : {}),
  });

  // Request inicial
  let res = await fetch(url, { ...options, headers: buildHeaders(token) });

  // Si no es 401, o la request era al propio endpoint de refresh (evitar loop)
  if (res.status !== 401 || url.includes('/auth/refresh')) {
    return res;
  }

  // 401 — intentar renovar el token
  try {
    const newToken = await refreshAccessToken();
    // Reintento único con el token nuevo
    res = await fetch(url, { ...options, headers: buildHeaders(newToken) });
  } catch {
    // Refresh falló (expirado, revocado, no existe) → logout limpio
    await performLogout();
    window.location.href = '/login';
    // Lanzar para que el caller no procese una respuesta vacía
    throw new Error('SESSION_EXPIRED');
  }

  return res;
}
