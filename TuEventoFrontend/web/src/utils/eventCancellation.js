/**
 * Aviso global de "evento cancelado".
 *
 * NotificationContext es el único que mantiene la conexión STOMP: al llegar un mensaje
 * de /topic/events/cancelled lo reemite como evento del navegador, y los listados de
 * eventos lo escuchan para quitar el evento sin recargar (sin depender de cuándo
 * conecta el WebSocket ni del orden de los providers).
 */
export const EVENT_CANCELLED_TOPIC = '/topic/events/cancelled';

const DOM_EVENT = 'tuevento:event-cancelled';

export function emitEventCancelled(eventId) {
  window.dispatchEvent(new CustomEvent(DOM_EVENT, { detail: { eventId } }));
}

/** Registra un listener que recibe el eventId cancelado; devuelve la función para quitarlo. */
export function onEventCancelled(listener) {
  const handler = (e) => listener(e.detail?.eventId);
  window.addEventListener(DOM_EVENT, handler);
  return () => window.removeEventListener(DOM_EVENT, handler);
}

/** Quita de una lista de eventos el que tenga el eventId indicado. */
export function withoutEvent(list, eventId) {
  return Array.isArray(list) ? list.filter((e) => String(e?.eventId) !== String(eventId)) : list;
}
