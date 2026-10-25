import { useEffect, useRef, useCallback } from 'react';
import { performLogout, refreshAccessToken } from '../services/httpClient.js';
import { getAccessToken } from '../services/tokenStore.js';

// El access token dura 15 minutos: se renueva cada 12 para no llegar a expirar
const REFRESH_INTERVAL_MS = 12 * 60 * 1000;

/**
 * Hook para refrescar automáticamente el token JWT antes de que expire
 * y notificar cuando se actualiza para reconectar WebSocket.
 * El primer token ya lo obtuvo restoreSession() al cargar la página.
 */
export const useTokenRefresh = (onTokenRefreshed) => {
  const intervalRef = useRef(null);
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
      // Un fallo de red se reintenta en el próximo intervalo; un rechazo del backend cierra la sesión
      if (!(error instanceof TypeError)) {
        await performLogout();
        window.location.href = '/login';
      }
    } finally {
      isRefreshing.current = false;
    }
  }, [onTokenRefreshed]);

  // Iniciar el intervalo de refresh automático
  useEffect(() => {
    if (!getAccessToken()) {
      return;
    }

    intervalRef.current = setInterval(() => {
      refreshToken();
    }, REFRESH_INTERVAL_MS);

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [refreshToken]);

  return { refreshToken };
};
