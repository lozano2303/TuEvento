import AsyncStorage from "@react-native-async-storage/async-storage";
import { jwtDecode } from "jwt-decode";

/**
 * httpClient — wrapper de fetch con refresh automático de JWT (espejo del web).
 *
 * Comportamiento:
 *   1. Adjunta el access token actual vía Authorization: Bearer <token>.
 *   2. Si la respuesta es 401, intenta renovar el token con POST /auth/refresh.
 *   3. Si el refresh tiene éxito, reintenta la request original una sola vez.
 *   4. Si el refresh falla, limpia la sesión local (el AuthContext detectará
 *      la ausencia de token y mandará al login).
 *
 * Un único refresh en vuelo (singleton) evita condiciones de carrera cuando
 * varias requests fallan con 401 simultáneamente.
 */

const BASE_URL = process.env.EXPO_PUBLIC_API_URL;

let refreshPromise = null;

async function refreshAccessToken() {
  if (refreshPromise) return refreshPromise;

  refreshPromise = (async () => {
    const refreshToken = await AsyncStorage.getItem("refreshToken");
    if (!refreshToken) throw new Error("NO_REFRESH_TOKEN");

    const res = await fetch(`${BASE_URL}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });

    if (!res.ok) throw new Error("REFRESH_FAILED");

    const { data } = await res.json();
    await AsyncStorage.setItem("accessToken", data.accessToken);
    await AsyncStorage.setItem("refreshToken", data.refreshToken);
    return data.accessToken;
  })();

  try {
    return await refreshPromise;
  } finally {
    refreshPromise = null;
  }
}

/**
 * Devuelve un access token vigente: si el guardado expira en menos de 30 s,
 * lo renueva con el refresh token. Retorna null si no hay sesión o el refresh falla.
 * Lo usa el WebSocket antes de cada (re)conexión, que no pasa por httpRequest.
 */
export async function getValidAccessToken() {
  const token = await AsyncStorage.getItem("accessToken");
  if (!token) return null;
  try {
    const { exp } = jwtDecode(token);
    if (exp && exp * 1000 - Date.now() > 30_000) return token;
    return await refreshAccessToken();
  } catch {
    return null;
  }
}

/**
 * fetch con Authorization automático + refresh transparente en 401.
 * @param {string} url  URL completa.
 * @param {RequestInit} options  mismas opciones que fetch().
 * @returns {Promise<Response>}  la Response sin parsear (igual que fetch nativo).
 */
export async function httpRequest(url, options = {}) {
  const token = await AsyncStorage.getItem("accessToken");

  const buildHeaders = (authToken) => ({
    ...options.headers,
    ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
  });

  let res = await fetch(url, { ...options, headers: buildHeaders(token) });

  if (res.status !== 401 || url.includes("/auth/refresh")) {
    return res;
  }

  try {
    const newToken = await refreshAccessToken();
    res = await fetch(url, { ...options, headers: buildHeaders(newToken) });
  } catch {
    await AsyncStorage.multiRemove(["accessToken", "refreshToken"]);
    throw new Error("SESSION_EXPIRED");
  }

  return res;
}
