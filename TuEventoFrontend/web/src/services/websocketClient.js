import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

/**
 * URL base del WebSocket.
 * Usa la misma base que la API pero sin el prefijo /api/v1.
 * SockJS negocia el transporte automáticamente (WebSocket, polling, streaming).
 */
const WS_BASE_URL = import.meta.env.VITE_API_URL 
  ? import.meta.env.VITE_API_URL.replace('/api/v1', '')
  : 'http://localhost:8080';

/**
 * Conecta al WebSocket de actualizaciones de sillas para un evento específico.
 * 
 * Estrategia de autenticación:
 * - El token JWT se envía como query parameter en el handshake inicial
 * - Si no hay token (usuario no autenticado), NO se conecta el WebSocket
 * - Los usuarios anónimos pueden ver las sillas pero sin actualizaciones en tiempo real
 * 
 * Características:
 * - Usa SockJS como capa de transporte (fallback automático si WebSocket nativo falla)
 * - Reconexión automática cada 5 segundos si se pierde la conexión
 * - Suscripción a /topic/events/{eventId}/seats para recibir cambios de estado
 * 
 * @param {number} eventId - ID del evento a monitorear
 * @param {function} onSeatUpdate - Callback que recibe el evento SeatStatusChangedEvent
 *   Estructura del evento:
 *   {
 *     seatId: number,
 *     eventSectionId: number,
 *     eventId: number,
 *     oldStatus: 'AVAILABLE' | 'RESERVED' | 'SOLD' | 'COURTESY',
 *     newStatus: 'AVAILABLE' | 'RESERVED' | 'SOLD' | 'COURTESY',
 *     changedBy: number | null,  // null = cambio automático del sistema
 *     reservedUntil: string | null  // ISO timestamp cuando expira la reserva
 *   }
 * 
 * @returns {Client | null} Cliente STOMP para desconectar en cleanup, o null si no hay token
 * 
 * @example
 * const client = connectSeatSocket(123, (event) => {
 *   console.log(`Silla ${event.seatId}: ${event.oldStatus} → ${event.newStatus}`);
 *   if (event.changedBy === null) {
 *     console.log('Cambio automático (expiración de reserva)');
 *   }
 * });
 * 
 * // En cleanup (useEffect o componentWillUnmount):
 * if (client) {
 *   client.deactivate();
 * }
 */
export function connectSeatSocket(eventId, onSeatUpdate) {
  const token = localStorage.getItem('token');

  // Si no hay token, no conectar WebSocket
  // Los usuarios anónimos verán el estado actual pero sin actualizaciones en vivo
  if (!token) {
    console.info('[WebSocket] No token found — skipping WebSocket connection. User will see static seat state.');
    return null;
  }

  const client = new Client({
    // SockJS como factory de transporte (fallback automático)
    webSocketFactory: () => new SockJS(`${WS_BASE_URL}/ws?token=${token}`),

    // Callback de conexión exitosa
    onConnect: (frame) => {
      console.info(`[WebSocket] Connected to seat updates for event ${eventId}`);
      
      // Suscribirse al topic de sillas del evento
      client.subscribe(`/topic/events/${eventId}/seats`, (message) => {
        try {
          const event = JSON.parse(message.body);
          onSeatUpdate(event);
        } catch (err) {
          console.error('[WebSocket] Error parsing seat update:', err);
        }
      });
    },

    // Callback de error de conexión
    onStompError: (frame) => {
      console.error('[WebSocket] STOMP error:', frame.headers['message']);
      console.error('[WebSocket] Additional details:', frame.body);
    },

    // Callback de error de WebSocket (transporte)
    onWebSocketError: (event) => {
      console.error('[WebSocket] WebSocket error:', event);
    },

    // Callback de cierre de conexión
    onDisconnect: () => {
      console.info('[WebSocket] Disconnected from seat updates');
    },

    // Reconexión automática cada 5 segundos si se pierde la conexión
    reconnectDelay: 5000,

    // Heartbeat para detectar conexiones muertas (ping cada 10s, timeout 10s)
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,

    // Debug logging (solo en desarrollo)
    debug: import.meta.env.DEV 
      ? (msg) => console.debug('[WebSocket Debug]', msg)
      : undefined,
  });

  // Activar la conexión
  client.activate();

  return client;
}

/**
 * Desconecta un cliente WebSocket de forma limpia.
 * 
 * @param {Client} client - Cliente STOMP a desconectar
 */
export function disconnectSeatSocket(client) {
  if (client && client.active) {
    client.deactivate();
    console.info('[WebSocket] Client deactivated');
  }
}
