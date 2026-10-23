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

  // Iniciar el intervalo de refresh automático
  useEffect(() => {
    const token = localStorage.getItem('token');

    if (!token) {
      return;
    }

    // Ejecutar refresh inmediatamente en la primera carga
    refreshToken();

    // Intervalo regular
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
