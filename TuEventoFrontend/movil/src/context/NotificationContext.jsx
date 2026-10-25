import { createContext, useContext, useState, useEffect, useCallback, useRef } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useAuth } from "./AuthContext";
import {
  getUserNotifications,
  markAsRead,
  markAllAsRead,
} from "../services/notificationService";
import { getAdminPendingCounts } from "../services/adminService";
import { getValidAccessToken, refreshAccessToken } from "../services/httpClient";
import { emitSessionEvent } from "../utils/sessionEvents";
import { EVENT_CANCELLED_TOPIC, emitEventCancelled } from "../utils/eventCancellation";
import { jwtDecode } from "jwt-decode";

const NotificationContext = createContext(null);

const EMPTY_ADMIN_PENDING = { organizerRequests: 0, eventsInReview: 0 };

function getWsBaseUrl() {
  const apiUrl = process.env.EXPO_PUBLIC_API_URL || "http://localhost:8080/api/v1";
  return apiUrl.replace("/api/v1", "");
}

export function NotificationProvider({ children }) {
  const { user, setUser } = useAuth(); // ← depende de AuthContext para saber cuándo hay sesión

  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount]     = useState(0);
  const [loading, setLoading]             = useState(false);
  // Pendientes del admin (punto rojo en Perfil): solo se cargan para rol ADMIN
  const [adminPending, setAdminPending]   = useState(EMPTY_ADMIN_PENDING);
  const isAdmin = user?.role === "ADMIN";

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

  // ── Pendientes del admin (REST) — carga inicial, reconexión y foco de Perfil ─
  const refreshAdminPending = useCallback(async () => {
    try {
      setAdminPending(await getAdminPendingCounts());
    } catch (err) {
      console.warn("[NotificationContext] Error loading admin pending:", err.message);
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

  // ── ROLE_UPDATED: el refresh relee el rol desde la BD (organizador aprobado o revocado) ─
  const applyRoleUpdate = useCallback(async () => {
    try {
      const token = await refreshAccessToken();
      const { role } = jwtDecode(token);
      if (role) setUser((prev) => (prev ? { ...prev, role } : prev));
    } catch (err) {
      console.warn("[NotificationContext] Error refreshing role:", err.message);
    }
  }, [setUser]);

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
      setAdminPending(EMPTY_ADMIN_PENDING);
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
      let token = await getValidAccessToken();
      if (!token) return;

      const WS_BASE = getWsBaseUrl();

      const handleNew = (notification) => {
        setNotifications((prev) => [notification, ...prev]);
        setUnreadCount((p) => p + 1);
      };

      const client = new Client({
        // Antes de cada (re)conexión se obtiene un token vigente: el JWT dura 15 min y,
        // si la conexión se cae, reintentar con el token original fallaría para siempre.
        beforeConnect: async () => {
          token = (await getValidAccessToken()) ?? token;
        },
        webSocketFactory: () => new SockJS(`${WS_BASE}/ws?token=${token}`),
        onConnect: () => {
          const sub = client.subscribe(
            `/topic/notifications/${userId}`,
            (msg) => {
              try { handleNew(JSON.parse(msg.body)); } catch { /* silencioso */ }
            }
          );
          subscriptionsRef.current.set("notifications", sub);

          // Eventos de sesión: tema, idioma y rol cambiados (en vivo, sin polling)
          const sessionSub = client.subscribe(`/topic/users/${userId}/session`, (msg) => {
            let event;
            try { event = JSON.parse(msg.body); } catch { return; }
            if (event?.type === "ROLE_UPDATED") void applyRoleUpdate();
            emitSessionEvent(event);
          });
          subscriptionsRef.current.set("session", sessionSub);

          // Rol de organizador revocado por un admin: renovar el JWT para tomar el rol USER
          const roleSub = client.subscribe(`/topic/role-change/${userId}`, (msg) => {
            let event;
            try { event = JSON.parse(msg.body); } catch { return; }
            if (event?.type === "ROLE_REVOKED") void applyRoleUpdate();
            emitSessionEvent(event);
          });
          subscriptionsRef.current.set("roleChange", roleSub);

          // Eventos cancelados: los listados abiertos los quitan sin recargar
          const cancelledSub = client.subscribe(EVENT_CANCELLED_TOPIC, (msg) => {
            let event;
            try { event = JSON.parse(msg.body); } catch { return; }
            if (event?.type === "EVENT_CANCELLED" && event.eventId != null) emitEventCancelled(event.eventId);
          });
          subscriptionsRef.current.set("eventsCancelled", cancelledSub);

          // Admin: contadores de solicitudes y eventos en revisión en vivo
          if (isAdmin) {
            const adminSub = client.subscribe("/topic/admin/pending-counts", (msg) => {
              try { setAdminPending(JSON.parse(msg.body)); } catch { /* silencioso */ }
            });
            subscriptionsRef.current.set("adminPending", adminSub);
            // Tras una reconexión pudo cambiar algo mientras no había conexión
            refreshAdminPending();
          }
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

    // Cargar notificaciones (+ pendientes del admin) y conectar WS
    loadNotifications();
    if (isAdmin) {
      refreshAdminPending();
    } else {
      setAdminPending(EMPTY_ADMIN_PENDING);
    }
    void connect();

    return () => {
      // Solo limpiar el WS, no el estado (evita parpadeo en re-renders)
      wsClientRef.current?.deactivate();
      wsClientRef.current = null;
      // Si el efecto se re-ejecuta (p. ej. cambia el rol), debe poder reconectar
      connectedUserRef.current = null;
      subscriptionsRef.current.clear();
    };
  }, [user?.userId, isAdmin, loadNotifications, refreshAdminPending, applyRoleUpdate]);

  const value = {
    notifications,
    unreadCount,
    loading,
    adminPending,
    adminPendingTotal: adminPending.organizerRequests + adminPending.eventsInReview,
    refreshAdminPending,
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
