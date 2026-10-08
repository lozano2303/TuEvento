import { createContext, useContext, useState, useEffect, useCallback, useRef } from "react";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useAuth } from "./AuthContext";
import {
  getUserNotifications,
  markAsRead,
  markAllAsRead,
} from "../services/notificationService";

const NotificationContext = createContext(null);

function getWsBaseUrl() {
  const apiUrl = process.env.EXPO_PUBLIC_API_URL || "http://localhost:8080/api/v1";
  return apiUrl.replace("/api/v1", "");
}

export function NotificationProvider({ children }) {
  const { user } = useAuth(); // ← depende de AuthContext para saber cuándo hay sesión

  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount]     = useState(0);
  const [loading, setLoading]             = useState(false);

  const wsClientRef      = useRef(null);
  const connectedUserRef = useRef(null); // userId actualmente conectado
  const subscriptionsRef = useRef(new Map());
  const notificationsRef = useRef(notifications);

  useEffect(() => {
    notificationsRef.current = notifications;
  }, [notifications]);

  // ── Cargar notificaciones ─────────────────────────────────────────────────
  const loadNotifications = useCallback(async () => {
    try {
      setLoading(true);
      const response = await getUserNotifications();
      const list = response.data?.content ?? [];
      setNotifications(list);
      setUnreadCount(list.filter((n) => !n.readAt).length);
    } catch (err) {
      console.warn("[NotificationContext] Error loading:", err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  // ── Marcar una como leída ─────────────────────────────────────────────────
  const markNotificationAsRead = useCallback(async (notificationUserId) => {
    try {
      const was = notificationsRef.current.find(
        (n) => n.notificationUserId === notificationUserId && !n.readAt
      );
      await markAsRead(notificationUserId);
      setNotifications((prev) =>
        prev.map((n) =>
          n.notificationUserId === notificationUserId
            ? { ...n, readAt: new Date().toISOString() }
            : n
        )
      );
      if (was) setUnreadCount((p) => Math.max(0, p - 1));
    } catch (err) {
      console.warn("[NotificationContext] Error marking as read:", err.message);
    }
  }, []);

  // ── Marcar todas como leídas ──────────────────────────────────────────────
  const markAllNotificationsAsRead = useCallback(async () => {
    try {
      await markAllAsRead();
      const now = new Date().toISOString();
      setNotifications((prev) => prev.map((n) => ({ ...n, readAt: n.readAt || now })));
      setUnreadCount(0);
    } catch (err) {
      console.warn("[NotificationContext] Error marking all as read:", err.message);
    }
  }, []);

  // ── Conectar/desconectar según cambio de sesión ───────────────────────────
  useEffect(() => {
    const userId = user?.userId ? String(user.userId) : null;

    // Sin usuario → limpiar todo
    if (!userId) {
      wsClientRef.current?.deactivate();
      wsClientRef.current = null;
      connectedUserRef.current = null;
      subscriptionsRef.current.clear();
      setNotifications([]);
      setUnreadCount(0);
      return;
    }

    // Ya conectado para este mismo usuario → no reconectar
    if (connectedUserRef.current === userId) return;

    // Desconectar sesión anterior si existe
    wsClientRef.current?.deactivate();
    wsClientRef.current = null;
    subscriptionsRef.current.clear();
    connectedUserRef.current = userId;

    const connect = async () => {
      const token = await AsyncStorage.getItem("accessToken");
      if (!token) return;

      const WS_BASE = getWsBaseUrl();

      const handleNew = (notification) => {
        setNotifications((prev) => [notification, ...prev]);
        setUnreadCount((p) => p + 1);
      };

      const client = new Client({
        webSocketFactory: () => new SockJS(`${WS_BASE}/ws?token=${token}`),
        onConnect: () => {
          const sub = client.subscribe(
            `/topic/notifications/${userId}`,
            (msg) => {
              try { handleNew(JSON.parse(msg.body)); } catch { /* silencioso */ }
            }
          );
          subscriptionsRef.current.set("notifications", sub);
        },
        onStompError:    (f) => console.warn("[WS] STOMP error:", f.headers["message"]),
        onWebSocketError:(e) => console.warn("[WS] error:", e),
        onDisconnect:    ()  => subscriptionsRef.current.clear(),
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
      });

      client.activate();
      wsClientRef.current = client;
    };

    // Cargar notificaciones + conectar WS
    loadNotifications();
    connect();

    return () => {
      // Solo limpiar el WS, no el estado (evita parpadeo en re-renders)
      wsClientRef.current?.deactivate();
      wsClientRef.current = null;
      subscriptionsRef.current.clear();
    };
  }, [user?.userId, loadNotifications]);

  const value = {
    notifications,
    unreadCount,
    loading,
    markAsRead: markNotificationAsRead,
    markAllAsRead: markAllNotificationsAsRead,
    refresh: loadNotifications,
  };

  return (
    <NotificationContext.Provider value={value}>
      {children}
    </NotificationContext.Provider>
  );
}

export function useNotifications() {
  const ctx = useContext(NotificationContext);
  if (!ctx) throw new Error("useNotifications must be used within NotificationProvider");
  return ctx;
}
