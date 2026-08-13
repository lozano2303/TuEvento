/**
 * httpClient — wrapper de fetch centralizado con refresh automático de JWT.
 *
 * Comportamiento:
 *   1. Adjunta el token actual a cada request vía Authorization: Bearer <token>
 *   2. Si la respuesta es 401, intenta renovar el access token con POST /auth/refresh
 *   3. Si el refresh tiene éxito, reintenta la request original una sola vez
 *   4. Si el refresh falla (token expirado, revocado, no existe), hace logout limpio
 *      y redirige a /login — el usuario ve la pantalla de login, no un error opaco
 *
 * Anti-condición-de-carrera:
 *   refreshPromise es un singleton en módulo. Si varios requests fallan con 401
 *   simultáneamente, todos esperan la misma promesa de refresh — solo se hace
 *   una llamada a /auth/refresh aunque haya 10 requests en vuelo.
 *
 * No migrar los servicios existentes todavía — este archivo se adopta progresivamente.
 * Ver EventService.js como prueba piloto.
 *
 * Mantenibilidad futura:
 *   El catch de refreshAccessToken recibe el error original de la API
 *   ({ message, errorCode }) — si en el futuro se quiere mostrar "tu sesión expiró"
 *   vs "sesión inválida" vs "cuenta suspendida", el error está disponible para
 *   que performLogout reciba el motivo y lo muestre antes de redirigir.
 */

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

// Singleton de refresh en vuelo — evita condición de carrera
let refreshPromise = null;

/**
 * Limpia todas las claves de sesión, revoca el token en el backend, y redirige a /login.
 * Esta es la única función de logout que debe usarse en todo el proyecto.
 * 
 * Comportamiento:
 *   1. Intenta revocar la sesión en el backend vía POST /auth/logout
 *   2. Limpia todas las claves de localStorage relacionadas con la sesión
 *   3. Redirige a /login
 * 
 * Si el backend falla (red, token ya revocado, etc.), igual limpia el cliente
 * para garantizar que el usuario pueda iniciar sesión de nuevo.
 */
export async function performLogout() {
  const accessToken = localStorage.getItem('token');
  
  // Intentar revocar en el backend si tenemos token
  if (accessToken) {
    try {
      await fetch(`${API_URL}/auth/logout`, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${accessToken}`
        },
        body: JSON.stringify({ accessToken }),
      });
    } catch (err) {
      // Si falla (red, token expirado, etc.), igual limpiamos el cliente
      console.warn('[performLogout] No se pudo revocar la sesión en el backend:', err.message);
    }
  }

  // Limpieza completa de todas las claves de sesión
  const sessionKeys = [
    'token', 'refreshToken', 'userID', 'alias',
    'userEmail', 'name', 'role',
    'pendingActivationUserID', 'pendingActivationEmail', 'adminLoggedIn',
    'activeThemeId', 'activeThemeName',
  ];
  sessionKeys.forEach((k) => localStorage.removeItem(k));
}

/**
 * Renueva el access token usando el refresh token almacenado.
 * Retorna el nuevo access token en éxito.
 * Lanza un error en cualquier otro caso (el caller hará logout).
 */
async function refreshAccessToken() {
  // Si ya hay un refresh en vuelo, encolar en esa misma promesa
  if (refreshPromise) return refreshPromise;

  refreshPromise = (async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) throw new Error('NO_REFRESH_TOKEN');

    const res = await fetch(`${API_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
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
    localStorage.setItem('token', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    return data.accessToken;
  })();

  try {
    return await refreshPromise;
  } finally {
    // Liberar el lock para el próximo ciclo de 401
    refreshPromise = null;
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
  const token = localStorage.getItem('token');

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
