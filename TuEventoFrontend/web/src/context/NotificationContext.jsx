import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { 
  getUserNotifications, 
  markAsRead, 
  markAllAsRead 
} from '../services/NotificationService';
import { useTokenRefresh } from '../hooks/useTokenRefresh';

const NotificationContext = createContext(null);

const WS_BASE_URL = import.meta.env.VITE_API_URL 
  ? import.meta.env.VITE_API_URL.replace('/api/v1', '')
  : 'http://localhost:8080';

/**
 * Provider de notificaciones que mantiene UN SOLO WebSocket centralizado.
 * Este WebSocket maneja:
 * - Notificaciones (siempre activo)
 * - Actualizaciones de sillas (dinámico, solo en páginas de eventos)
 * - Otros temas a futuro
 */
export function NotificationProvider({ children }) {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Cliente WebSocket centralizado (persiste entre renders)
  const wsClientRef = useRef(null);
  const notificationsRef = useRef(notifications);
  const initializedRef = useRef(false);
  
  // Map de subscripciones activas (para poder desuscribir)
  const subscriptionsRef = useRef(new Map());
  
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
      console.error('[NotificationContext] Error loading notifications:', err);
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
      console.error('[NotificationContext] Error marking notification as read:', err);
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
      console.error('[NotificationContext] Error marking all as read:', err);
    }
  }, []);

  /**
   * Suscribirse a un topic/queue dinámicamente.
   * @param {string} destination - Destino de la subscripción (ej: '/topic/events/4/seats')
   * @param {function} callback - Callback que recibe el mensaje
   * @param {string} key - Clave única para esta subscripción
   * @returns {function} Función para desuscribirse
   */
  const subscribe = useCallback((destination, callback, key) => {
    if (!wsClientRef.current || !wsClientRef.current.connected) {
      console.warn('[WebSocket] Cannot subscribe, client not connected');
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
        console.error(`[WebSocket] Error parsing message from ${destination}:`, err);
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
    };

    const client = new Client({
      webSocketFactory: () => new SockJS(`${WS_BASE_URL}/ws?token=${newToken}`),

      onConnect: () => {
        console.log('[WebSocket] Reconnected successfully');
        
        const notificationDest = `/topic/notifications/${userId}`;
        
        const notifSubscription = client.subscribe(notificationDest, (message) => {
          try {
            const notification = JSON.parse(message.body);
            handleNewNotification(notification);
          } catch (err) {
            console.error('[WebSocket] Error parsing notification:', err);
          }
        });

        subscriptionsRef.current.set('notifications', notifSubscription);
      },

      onStompError: (frame) => {
        console.error('[WebSocket] STOMP error:', frame.headers['message']);
        setError('WebSocket connection error');
      },

      onWebSocketError: (event) => {
        console.error('[WebSocket] WebSocket error:', event);
        setError('WebSocket network error');
      },

      onDisconnect: () => {
        console.info('[WebSocket] Disconnected');
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
      console.info('[WebSocket] No token found, skipping WebSocket initialization');
      setLoading(false);
      return;
    }

    const userId = localStorage.getItem('userID');
    if (!userId) {
      console.warn('[WebSocket] No userID found');
      setLoading(false);
      return;
    }

    initializedRef.current = true;

    // Manejar nueva notificación desde WebSocket
    const handleNewNotification = (notification) => {
      setNotifications(prev => [notification, ...prev]);
      setUnreadCount(prev => prev + 1);
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
            console.error('[WebSocket] Error parsing notification:', err);
          }
        });

        subscriptionsRef.current.set('notifications', notifSubscription);
      },

      onStompError: (frame) => {
        console.error('[WebSocket] STOMP error:', frame.headers['message']);
        setError('WebSocket connection error');
      },

      onWebSocketError: (event) => {
        console.error('[WebSocket] WebSocket error:', event);
        setError('WebSocket network error');
      },

      onDisconnect: () => {
        console.info('[WebSocket] Disconnected');
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
