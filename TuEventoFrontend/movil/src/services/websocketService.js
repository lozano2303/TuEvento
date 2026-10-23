import AsyncStorage from '@react-native-async-storage/async-storage';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

/**
 * Deriva la URL base desde EXPO_PUBLIC_API_URL.
 * Ejemplo: http://10.3.232.220:8080/api/v1 → http://10.3.232.220:8080
 */
function getBaseURL() {
  const apiUrl = process.env.EXPO_PUBLIC_API_URL || 'http://localhost:8080/api/v1';
  // Quitar /api/v1 para obtener la URL base
  return apiUrl.replace('/api/v1', '');
}

/**
 * Conecta al WebSocket de actualizaciones de sillas para un evento específico.
 * 
 * Usa SockJS + STOMP (mismo patrón que la web) porque WebSocket nativo
 * de React Native no es 100% compatible con Spring WebSocket + STOMP.
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
 *     changedBy: number | null,
 *     reservedUntil: string | null
 *   }
 * 
 * @returns {Promise<Client | null>} Cliente STOMP para desconectar en cleanup, o null si no hay token
 */
export async function connectSeatSocket(eventId, onSeatUpdate) {
  const token = await AsyncStorage.getItem('accessToken');

  if (!token) {
    console.info('[WebSocket] No token found — skipping WebSocket connection.');
    return null;
  }

  const baseUrl = getBaseURL();

  const client = new Client({
    // SockJS como factory de transporte (igual que la web)
    webSocketFactory: () => new SockJS(`${baseUrl}/ws?token=${token}`),

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

    // Heartbeat para detectar conexiones muertas
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  });

  // Activar la conexión
  client.activate();

  return client;
}

/**
 * Desconecta un cliente WebSocket de forma limpia.
 * 
 * @param {Client | null} client - Cliente STOMP a desconectar
 */
export function disconnectSeatSocket(client) {
  if (client && client.active) {
    client.deactivate();
    console.info('[WebSocket] Client deactivated');
  }
}
