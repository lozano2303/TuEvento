/**
 * Bus mínimo para el aviso global de "evento cancelado" (/topic/events/cancelled).
 *
 * NotificationContext mantiene la única conexión STOMP y reemite aquí cada aviso;
 * los listados de eventos se suscriben para quitar el evento sin recargar.
 */
export const EVENT_CANCELLED_TOPIC = "/topic/events/cancelled";

const listeners = new Set();

/** Registra un listener que recibe el eventId cancelado; devuelve la función para quitarlo. */
export function onEventCancelled(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function emitEventCancelled(eventId) {
  listeners.forEach((listener) => {
    try {
      listener(eventId);
    } catch {
      // un listener roto no debe impedir que los demás reciban el aviso
    }
  });
}

/** Quita de una lista de eventos el que tenga el eventId indicado. */
export function withoutEvent(list, eventId) {
  return Array.isArray(list) ? list.filter((e) => String(e?.eventId) !== String(eventId)) : list;
}
