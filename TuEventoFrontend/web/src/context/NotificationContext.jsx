import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { 
  getUserNotifications, 
  markAsRead, 
  markAllAsRead 
} from '../services/NotificationService';
import { useTokenRefresh } from '../hooks/useTokenRefresh';
import { refreshAccessToken } from '../services/httpClient';
import { ORGANIZER_WELCOME_KEY } from '../components/common/OrganizerWelcomeModal';
import { getActiveLanguages } from '../services/LanguageService';
import { changeUILanguage, getCurrentUILanguage } from '../i18n';
import { EVENT_CANCELLED_TOPIC, emitEventCancelled } from '../utils/eventCancellation';

/** Evento del DOM que dispara el aviso THEME_UPDATED del WebSocket (lo escucha ThemeContext). */
export const THEME_UPDATED_EVENT = 'tuevento:theme-updated';

const NotificationContext = createContext(null);

const WS_BASE_URL = import.meta.env.VITE_API_URL
  ? import.meta.env.VITE_API_URL.replace('/api/v1', '')
  : 'http://localhost:8080';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

// Notificaciones que indican que el admin cambió el estado de un evento del organizador.
// Al recibirlas se recarga la página para leer el estado real desde el backend.
const EVENT_STATUS_NOTIFICATION_TYPES = ['EVENT_PUBLISHED', 'EVENT_REJECTED'];

/**
 * Key stored in localStorage to remember that this user's organizer role
 * was revoked and the one-time modal has already been acknowledged.
 * Format: `role_revoked_ack_{userId}` so it survives page reloads until
 * the user explicitly dismisses the modal.
 */
const roleRevokedAckKey = (userId) => `role_revoked_ack_${userId}`;

/** Extrae el claim "role" del payload de un JWT (base64url), o null si no se puede leer. */
function decodeJwtRole(token) {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(payload)).role ?? null;
  } catch {
    return null;
  }
}

/**
 * Refreshes the JWT via /auth/refresh and updates localStorage with the
 * new access token and role. Returns the new access token or null on failure.
 */
async function refreshJwtAndUpdateRole() {
  try {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) return null;

    const res = await fetch(`${API_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });

    if (!res.ok) return null;

    const data = await res.json();
    if (!data?.data) return null;

    const { accessToken, refreshToken: newRefresh } = data.data;

    // Decode the role from the JWT payload (base64 middle segment)
    try {
      const payload = JSON.parse(atob(accessToken.split('.')[1]));
      const newRole = payload.role || payload.authorities?.[0] || null;
      if (newRole) localStorage.setItem('role', newRole);
    } catch (_) {
      // If decoding fails, role stays stale — acceptable; page reload will fix it
    }

    localStorage.setItem('token', accessToken);
    localStorage.setItem('refreshToken', newRefresh);

    // Notify components (Navbar, etc.) that the role changed without a full page reload
    window.dispatchEvent(new CustomEvent('roleChanged', { detail: { role: localStorage.getItem('role') } }));

    return accessToken;
  } catch {
    return null;
  }
}

/**
 * ROLE_UPDATED (p. ej. solicitud de organizador aprobada): renueva el JWT para
 * obtener el rol nuevo desde la BD, lo guarda y lleva al inicio con recarga completa.
 * Si el rol nuevo es ORGANIZER, deja marcado el modal de bienvenida (OrganizerWelcomeModal).
 */
async function handleSessionEvent(event) {
  // THEME_UPDATED: el ThemeContext escucha este evento del DOM y recarga la paleta
  if (event?.type === 'THEME_UPDATED') {
    window.dispatchEvent(new CustomEvent(THEME_UPDATED_EVENT));
    return;
  }
  // LANGUAGE_UPDATED: el idioma preferido cambió (en otro dispositivo o pestaña)
  if (event?.type === 'LANGUAGE_UPDATED') {
    try {
      const languages = await getActiveLanguages();
      const lang = languages.find(l => l.languageId === event.languageId);
      if (lang && lang.code !== getCurrentUILanguage()) changeUILanguage(lang.code);
    } catch {
      // no crítico — se mantiene el idioma actual
    }
    return;
  }
  if (event?.type !== 'ROLE_UPDATED') return;
  try {
    const newToken = await refreshAccessToken();
    const role = decodeJwtRole(newToken);
    if (role) localStorage.setItem('role', role);
    if (role === 'ORGANIZER') sessionStorage.setItem(ORGANIZER_WELCOME_KEY, 'true');
  } catch {
    // Si el refresh falla, la recarga deja que httpClient gestione la sesión.
  }
  window.location.href = '/';
}

/** Suscribe el canal de eventos de sesión del usuario: /topic/users/{userId}/session */
function subscribeSessionEvents(client, userId, subscriptionsRef) {
  const sub = client.subscribe(`/topic/users/${userId}/session`, (message) => {
    try {
      void handleSessionEvent(JSON.parse(message.body));
    } catch (err) {
      // Error parsing session event
    }
  });
  subscriptionsRef.current.set('session', sub);
}

/** Canal global de eventos cancelados: los listados abiertos quitan el evento en vivo. */
function subscribeEventCancellations(client, subscriptionsRef) {
  const sub = client.subscribe(EVENT_CANCELLED_TOPIC, (message) => {
    try {
      const payload = JSON.parse(message.body);
      if (payload?.type === 'EVENT_CANCELLED' && payload.eventId != null) {
        emitEventCancelled(payload.eventId);
      }
    } catch (err) {
      // Error parsing event cancellation
    }
  });
  subscriptionsRef.current.set('events-cancelled', sub);
}

/**
 * Provider de notificaciones que mantiene UN SOLO WebSocket centralizado.
 * Este WebSocket maneja:
 * - Notificaciones (siempre activo)
 * - Actualizaciones de sillas (dinámico, solo en páginas de eventos)
 * - Role-change signals (/topic/role-change/{userId}) — revocación de rol
 * - Session events (/topic/users/{userId}/session) — aprobación de rol
 * - Eventos cancelados (/topic/events/cancelled) — se quitan de los listados en vivo
 */
export function NotificationProvider({ children }) {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  // true when a ROLE_REVOKED WS message arrives and modal hasn't been shown yet
  const [roleRevokedPending, setRoleRevokedPending] = useState(false);
  
  // Cliente WebSocket centralizado (persiste entre renders)
  const wsClientRef = useRef(null);
  const notificationsRef = useRef(notifications);
  const initializedRef = useRef(false);
  
  // Map de subscripciones activas (para poder desuscribir)
  const subscriptionsRef = useRef(new Map());
  
  // Polling para traducciones pendientes
  const pollingIntervalRef = useRef(null);
  
  // Actualizar ref cuando cambia el estado
  useEffect(() => {
    notificationsRef.current = notifications;
  }, [notifications]);

  // Cargar notificaciones iniciales
  const loadNotifications = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await getUserNotifications();
      
      const notificationList = response.data?.content || [];
      setNotifications(notificationList);
      
      const unread = notificationList.filter(n => !n.readAt).length;
      setUnreadCount(unread);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  // Marcar notificación como leída
  const markNotificationAsRead = useCallback(async (notificationUserId) => {
    try {
      const notification = notificationsRef.current.find(n => n.notificationUserId === notificationUserId);
      const wasUnread = notification && !notification.readAt;
      
      await markAsRead(notificationUserId);
      
      setNotifications(prev => 
        prev.map(n => 
          n.notificationUserId === notificationUserId 
            ? { ...n, readAt: new Date().toISOString() }
            : n
        )
      );
      
      if (wasUnread) {
        setUnreadCount(prev => Math.max(0, prev - 1));
      }
    } catch (err) {
      // Error silenciado
    }
  }, []);

  // Marcar todas como leídas
  const markAllNotificationsAsRead = useCallback(async () => {
    try {
      await markAllAsRead();
      
      const now = new Date().toISOString();
      setNotifications(prev => 
        prev.map(n => ({ ...n, readAt: n.readAt || now }))
      );
      
      setUnreadCount(0);
    } catch (err) {
      // Error silenciado
    }
  }, []);

  /**
   * Verificar si una notificación necesita polling de traducción.
   * Retorna true si:
   * - El idioma preferido del usuario NO es español
   * - No existe traducción PUBLISHED para ese idioma
   * - La notificación es reciente (menos de 2 minutos)
   */
  const needsTranslationPolling = useCallback((notification) => {
    const userLanguage = localStorage.getItem('i18nextLng') || 'es';
    
    // Si es español, no necesita traducción
    if (userLanguage === 'es') {
      return false;
    }

    // Si no tiene translations array, asumir que necesita
    if (!notification.translations || notification.translations.length === 0) {
      return true;
    }

    // Buscar traducción para el idioma actual
    const translation = notification.translations.find(t => t.languageCode === userLanguage);
    
    // Si no existe o no está PUBLISHED, necesita polling
    if (!translation || translation.status !== 'PUBLISHED') {
      // Solo hacer polling si es reciente (últimos 2 minutos)
      const notificationTime = new Date(notification.sentAt).getTime();
      const now = Date.now();
      const twoMinutes = 2 * 60 * 1000;
      
      return (now - notificationTime) < twoMinutes;
    }

    return false;
  }, []);

  /**
   * Actualizar una notificación específica desde el backend.
   */
  const updateNotification = useCallback(async (notificationUserId) => {
    try {
      const response = await getUserNotifications();
      const notificationList = response.data?.content || [];
      
      const updatedNotification = notificationList.find(
        n => n.notificationUserId === notificationUserId
      );

      if (updatedNotification) {
        setNotifications(prev => 
          prev.map(n => 
            n.notificationUserId === notificationUserId ? updatedNotification : n
          )
        );
      }
    } catch (err) {
      // Error silenciado
    }
  }, []);

  /**
   * Iniciar polling para notificaciones que necesitan traducción.
   */
  useEffect(() => {
    // Buscar notificaciones que necesitan polling
    const notificationsNeedingPolling = notifications.filter(needsTranslationPolling);

    if (notificationsNeedingPolling.length === 0) {
      // No hay notificaciones pendientes, limpiar polling
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current);
        pollingIntervalRef.current = null;
      }
      return;
    }

    // Iniciar polling cada 3 segundos
    if (!pollingIntervalRef.current) {
      pollingIntervalRef.current = setInterval(async () => {
        const currentNotifications = notificationsRef.current;
        const pendingNotifications = currentNotifications.filter(needsTranslationPolling);

        if (pendingNotifications.length === 0) {
          clearInterval(pollingIntervalRef.current);
          pollingIntervalRef.current = null;
          return;
        }

        // Actualizar cada notificación pendiente
        for (const notification of pendingNotifications) {
          await updateNotification(notification.notificationUserId);
        }
      }, 3000);
    }

    // Cleanup
    return () => {
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current);
        pollingIntervalRef.current = null;
      }
    };
  }, [notifications, needsTranslationPolling, updateNotification]);

  /**
   * Suscribirse a un topic/queue dinámicamente.
   * @param {string} destination - Destino de la subscripción (ej: '/topic/events/4/seats')
   * @param {function} callback - Callback que recibe el mensaje
   * @param {string} key - Clave única para esta subscripción
   * @returns {function} Función para desuscribirse
   */
  const subscribe = useCallback((destination, callback, key) => {
    if (!wsClientRef.current || !wsClientRef.current.connected) {
      return () => {};
    }

    // Si ya existe una subscripción con esta clave, desuscribir primero
    if (subscriptionsRef.current.has(key)) {
      subscriptionsRef.current.get(key).unsubscribe();
    }
    
    const subscription = wsClientRef.current.subscribe(destination, (message) => {
      try {
        const data = JSON.parse(message.body);
        callback(data);
      } catch (err) {
        // Error parsing message
      }
    });

    subscriptionsRef.current.set(key, subscription);

    // Retornar función de cleanup
    return () => {
      subscription.unsubscribe();
      subscriptionsRef.current.delete(key);
    };
  }, []);

  // Función para reconectar WebSocket con nuevo token
  const reconnectWebSocket = useCallback((newToken) => {
    const userId = localStorage.getItem('userID');
    if (!userId || !wsClientRef.current) {
      return;
    }

    // Desconectar el cliente actual
    wsClientRef.current.deactivate();

    // Crear nuevo cliente con el token actualizado
    const handleNewNotification = (notification) => {
      setNotifications(prev => [notification, ...prev]);
      setUnreadCount(prev => prev + 1);
      if (EVENT_STATUS_NOTIFICATION_TYPES.includes(notification.type)) {
        window.location.reload();
      }
    };

    const handleRoleRevoked = async () => {
      const freshToken = await refreshJwtAndUpdateRole();
      // Mark in localStorage so the modal re-appears after a hard reload too
      localStorage.setItem(roleRevokedAckKey(userId), 'pending');
      setRoleRevokedPending(true);
      if (freshToken && wsClientRef.current) {
        // Reconnect again with the brand-new token that carries the USER role
        reconnectWebSocket(freshToken);
      }
    };

    const client = new Client({
      webSocketFactory: () => new SockJS(`${WS_BASE_URL}/ws?token=${newToken}`),

      onConnect: () => {
        const notificationDest = `/topic/notifications/${userId}`;
        const notifSubscription = client.subscribe(notificationDest, (message) => {
          try {
            const notification = JSON.parse(message.body);
            handleNewNotification(notification);
          } catch (err) {
            // Error parsing notification
          }
        });
        subscriptionsRef.current.set('notifications', notifSubscription);

        // Role-change subscription: ROLE_REVOKED (admin revoca el rol al organizador)
        const roleChangeDest = `/topic/role-change/${userId}`;
        const roleSubscription = client.subscribe(roleChangeDest, (message) => {
          try {
            const payload = JSON.parse(message.body);
            if (payload?.type === 'ROLE_REVOKED') {
              void handleRoleRevoked();
            }
          } catch (err) {
            // Error parsing role-change message
          }
        });
        subscriptionsRef.current.set('role-change', roleSubscription);

        // Session events subscription: ROLE_UPDATED (solicitud de organizador aprobada)
        subscribeSessionEvents(client, userId, subscriptionsRef);

        // Eventos cancelados: los listados abiertos los quitan sin recargar
        subscribeEventCancellations(client, subscriptionsRef);
      },

      onStompError: (frame) => {
        setError('WebSocket connection error');
      },

      onWebSocketError: (event) => {
        setError('WebSocket network error');
      },

      onDisconnect: () => {
        subscriptionsRef.current.clear();
      },

      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    client.activate();
    wsClientRef.current = client;
  }, []);

  // Hook de refresh automático de token
  useTokenRefresh(reconnectWebSocket);

  // Inicializar: cargar notificaciones y conectar WebSocket (solo una vez)
  useEffect(() => {
    // Prevenir doble inicialización en StrictMode
    if (initializedRef.current) {
      return;
    }

    const token = localStorage.getItem('token');
    if (!token) {
      setLoading(false);
      return;
    }

    const userId = localStorage.getItem('userID');
    if (!userId) {
      setLoading(false);
      return;
    }

    initializedRef.current = true;

    // Manejar nueva notificación desde WebSocket
    const handleNewNotification = (notification) => {
      setNotifications(prev => [notification, ...prev]);
      setUnreadCount(prev => prev + 1);
      if (EVENT_STATUS_NOTIFICATION_TYPES.includes(notification.type)) {
        window.location.reload();
      }
    };

    // Handle role revocation signal from admin
    const handleRoleRevoked = async () => {
      const newToken = await refreshJwtAndUpdateRole();
      localStorage.setItem(roleRevokedAckKey(userId), 'pending');
      setRoleRevokedPending(true);
      if (newToken) {
        reconnectWebSocket(newToken);
      }
    };

    // Crear cliente WebSocket centralizado
    const client = new Client({
      webSocketFactory: () => new SockJS(`${WS_BASE_URL}/ws?token=${token}`),

      onConnect: () => {
        // SIEMPRE suscribirse a notificaciones (subscripción permanente)
        // Usar /topic/notifications/{userId} porque el simple broker de Spring
        // no soporta user destinations correctamente
        const notificationDest = `/topic/notifications/${userId}`;
        
        const notifSubscription = client.subscribe(notificationDest, (message) => {
          try {
            const notification = JSON.parse(message.body);
            handleNewNotification(notification);
          } catch (err) {
            // Error parsing notification
          }
        });

        subscriptionsRef.current.set('notifications', notifSubscription);

        // Role-change subscription: ROLE_REVOKED (admin revoca el rol al organizador)
        const roleChangeDest = `/topic/role-change/${userId}`;
        const roleSubscription = client.subscribe(roleChangeDest, (message) => {
          try {
            const payload = JSON.parse(message.body);
            if (payload?.type === 'ROLE_REVOKED') {
              void handleRoleRevoked();
            }
          } catch (err) {
            // Error parsing role-change message
          }
        });
        subscriptionsRef.current.set('role-change', roleSubscription);

        // Session events subscription: ROLE_UPDATED (solicitud de organizador aprobada)
        subscribeSessionEvents(client, userId, subscriptionsRef);

        // Eventos cancelados: los listados abiertos los quitan sin recargar
        subscribeEventCancellations(client, subscriptionsRef);
      },

      onStompError: (frame) => {
        setError('WebSocket connection error');
      },

      onWebSocketError: (event) => {
        setError('WebSocket network error');
      },

      onDisconnect: () => {
        subscriptionsRef.current.clear();
      },

      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    client.activate();
    wsClientRef.current = client;

    // Cargar notificaciones
    loadNotifications();

    // Cleanup: desconectar WebSocket solo al desmontar el provider
    return () => {
      // Desuscribir todas las subscripciones activas
      subscriptionsRef.current.forEach((subscription) => {
        subscription.unsubscribe();
      });
      subscriptionsRef.current.clear();

      // Desactivar cliente
      if (wsClientRef.current) {
        wsClientRef.current.deactivate();
        wsClientRef.current = null;
      }
      
      initializedRef.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // Array vacío: solo ejecutar al montar

  const value = {
    notifications,
    unreadCount,
    loading,
    error,
    markAsRead: markNotificationAsRead,
    markAllAsRead: markAllNotificationsAsRead,
    refresh: loadNotifications,
    // Exponer función de subscripción para uso dinámico
    subscribe,
    // Estado de conexión
    isConnected: wsClientRef.current?.connected || false,
    // Role revocation signal — consumed by App.jsx to show the one-time modal
    roleRevokedPending,
    clearRoleRevokedPending: () => {
      const userId = localStorage.getItem('userID');
      if (userId) localStorage.removeItem(roleRevokedAckKey(userId));
      setRoleRevokedPending(false);
    },
  };

  return (
    <NotificationContext.Provider value={value}>
      {children}
    </NotificationContext.Provider>
  );
}

/**
 * Hook para acceder al contexto de notificaciones.
 */
export function useNotifications() {
  const context = useContext(NotificationContext);
  
  if (!context) {
    throw new Error('useNotifications must be used within NotificationProvider');
  }
  
  return context;
}
