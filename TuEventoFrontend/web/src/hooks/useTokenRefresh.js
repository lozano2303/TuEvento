import { useEffect, useRef, useCallback } from 'react';
import { performLogout, refreshAccessToken } from '../services/httpClient.js';
import { getAccessToken } from '../services/tokenStore.js';

// Configuración de producción
const REFRESH_BEFORE_MS = 3 * 60 * 1000; // Refrescar 3 minutos antes
const REFRESH_INTERVAL_MS = 12 * 60 * 1000; // 12 minutos (si no se puede leer el exp)
const RETRY_DELAY_MS = 60 * 1000; // Espera mínima tras un refresh fallido

/**
 * Milliseconds until the token must be renewed: REFRESH_BEFORE_MS before its exp claim.
 * Falls back to the fixed interval when the token cannot be decoded.
 */
function msUntilRefresh(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    if (typeof payload.exp === 'number') {
      return Math.max(0, payload.exp * 1000 - Date.now() - REFRESH_BEFORE_MS);
    }
  } catch {
    // Malformed token: fall back to the fixed interval
  }
  return REFRESH_INTERVAL_MS;
}

/**
 * Hook para refrescar automáticamente el token JWT antes de que expire
 * y notificar cuando se actualiza para reconectar WebSocket.
 *
 * Never refreshes on mount: the first access token of a page load already comes from
 * restoreSession() (main.jsx), and a second refresh right after it would only rotate the
 * cookie again. The refresh is scheduled from the exp claim of the in-memory token.
 */
export const useTokenRefresh = (onTokenRefreshed) => {
  const timeoutRef = useRef(null);
  const isRefreshing = useRef(false);

  const refreshToken = useCallback(async () => {
    // Evitar múltiples refresh simultáneos
    if (isRefreshing.current || !getAccessToken()) {
      return;
    }

    try {
      isRefreshing.current = true;
      const accessToken = await refreshAccessToken();

      // Notificar al callback (para reconectar WebSocket)
      if (onTokenRefreshed) {
        onTokenRefreshed(accessToken);
      }
    } catch (error) {
      // Un fallo de red se reintenta en el próximo ciclo; un rechazo del backend cierra la sesión
      if (!(error instanceof TypeError)) {
        await performLogout();
        window.location.href = '/login';
      }
    } finally {
      isRefreshing.current = false;
    }
  }, [onTokenRefreshed]);

  // Programar el refresh según el exp del token actual
  useEffect(() => {
    let cancelled = false;

    const schedule = (afterAttempt) => {
      // Se lee en cada ciclo: otra pestaña puede haberlo renovado (BroadcastChannel en tokenStore)
      const token = getAccessToken();
      if (cancelled || !token) {
        return;
      }
      let delay = msUntilRefresh(token);
      // If a refresh just failed, the token is still near expiry: wait before retrying
      if (afterAttempt) {
        delay = Math.max(delay, RETRY_DELAY_MS);
      }
      timeoutRef.current = setTimeout(async () => {
        await refreshToken();
        schedule(true);
      }, delay);
    };

    schedule(false);

    return () => {
      cancelled = true;
      clearTimeout(timeoutRef.current);
    };
  }, [refreshToken]);

  return { refreshToken };
};
