/**
 * Bus mínimo para los eventos de sesión del WebSocket (/topic/users/{userId}/session).
 *
 * NotificationContext es el único que mantiene la conexión STOMP y reemite aquí cada
 * mensaje; ThemeContext y LanguageContext se suscriben sin depender del orden de los
 * providers. Tipos: THEME_UPDATED, LANGUAGE_UPDATED { languageId }, ROLE_UPDATED { role }
 * y ROLE_REVOKED { newRole } (este último llega por /topic/role-change/{userId}).
 */
const listeners = new Set();

/** Registra un listener; devuelve la función para quitarlo (apta para useEffect). */
export function onSessionEvent(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function emitSessionEvent(event) {
  listeners.forEach((listener) => {
    try {
      listener(event);
    } catch {
      // un listener roto no debe impedir que los demás reciban el evento
    }
  });
}
