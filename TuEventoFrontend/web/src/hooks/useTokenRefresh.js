import { useEffect, useRef, useCallback } from 'react';
import { httpRequest } from '../services/httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

// Configuración de producción
const TOKEN_EXPIRATION_MS = 15 * 60 * 1000; // 15 minutos
const REFRESH_BEFORE_MS = 3 * 60 * 1000; // Refrescar 3 minutos antes
const REFRESH_INTERVAL_MS = 12 * 60 * 1000; // 12 minutos

/**
 * Hook para refrescar automáticamente el token JWT antes de que expire
 * y notificar cuando se actualiza para reconectar WebSocket
 */
export const useTokenRefresh = (onTokenRefreshed) => {
  const intervalRef = useRef(null);
  const isRefreshing = useRef(false);

  const refreshToken = useCallback(async () => {
    // Evitar múltiples refresh simultáneos
    if (isRefreshing.current) {
      return;
    }

    const token = localStorage.getItem('token');
    const refreshTokenValue = localStorage.getItem('refreshToken');

    if (!token || !refreshTokenValue) {
      console.log('[Token Refresh] No tokens found, skipping refresh');
      return;
    }

    try {
      isRefreshing.current = true;
      console.log('[Token Refresh] Refreshing token...');

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

        console.log('[Token Refresh] Token refreshed successfully');

        // Notificar al callback (para reconectar WebSocket)
        if (onTokenRefreshed) {
          onTokenRefreshed(accessToken);
        }
      }
    } catch (error) {
      console.error('[Token Refresh] Failed to refresh token:', error);

      // Si el refresh falla con 401 o 404, limpiar tokens y redirigir al login
      if (error.message.includes('401') || error.message.includes('404')) {
        console.log('[Token Refresh] Refresh token invalid, logging out');
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

  // Iniciar el intervalo de refresh automático
  useEffect(() => {
    const token = localStorage.getItem('token');

    if (!token) {
      console.log('[Token Refresh] No token found, not starting refresh interval');
      return;
    }

    console.log(`[Token Refresh] Starting automatic token refresh every ${REFRESH_INTERVAL_MS / 1000 / 60} minutes`);

    // Ejecutar refresh inmediatamente en la primera carga
    refreshToken();

    // Intervalo regular
    intervalRef.current = setInterval(() => {
      refreshToken();
    }, REFRESH_INTERVAL_MS);

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        console.log('[Token Refresh] Stopped automatic token refresh');
      }
    };
  }, [refreshToken]);

  return { refreshToken };
};
