import { useEffect, useRef, useCallback } from 'react';
import { httpRequest } from '../services/httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

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
 * Never refreshes on mount: a page load right after login (e.g. the OAuth redirect
 * /login → /) would rotate the tokens while the page is navigating away, leaving a
 * revoked refresh token in localStorage. The refresh is scheduled from the exp claim.
 */
export const useTokenRefresh = (onTokenRefreshed) => {
  const timeoutRef = useRef(null);
  const isRefreshing = useRef(false);

  const refreshToken = useCallback(async () => {
    // Evitar múltiples refresh simultáneos
    if (isRefreshing.current) {
      return;
    }

    const token = localStorage.getItem('token');
    const refreshTokenValue = localStorage.getItem('refreshToken');

    if (!token || !refreshTokenValue) {
      return;
    }

    try {
      isRefreshing.current = true;

      const response = await httpRequest(`${API_URL}/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ refreshToken: refreshTokenValue }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();

      if (data?.data) {
        const { accessToken, refreshToken: newRefreshToken } = data.data;

        // Actualizar tokens en localStorage
        localStorage.setItem('token', accessToken);
        localStorage.setItem('refreshToken', newRefreshToken);

        // Notificar al callback (para reconectar WebSocket)
        if (onTokenRefreshed) {
          onTokenRefreshed(accessToken);
        }
      }
    } catch (error) {
      // Si el refresh falla con 401 o 404, limpiar tokens y redirigir al login
      if (error.message.includes('401') || error.message.includes('404')) {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        localStorage.removeItem('userID');
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
      const token = localStorage.getItem('token');
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
