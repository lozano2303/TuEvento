/**
 * Traducciones de notificaciones (misma lógica que la web).
 *
 * Espejo de TuEventoFrontend/web/src/utils/notificationI18n.js: las dos apps se construyen por
 * separado y no pueden compartir el módulo. Cualquier cambio aquí debe replicarse allá.
 *
 * El backend crea cada notificación en español y la traduce en segundo plano
 * (LibreTranslate, un job cada ~15 s); la API devuelve las traducciones junto a
 * cada notificación con el código base del idioma ("en", "pt", "fr").
 */
import { parsePastServerDate } from "./serverDate";

export const SOURCE_LANGUAGE = "es";

/** Ventana durante la cual una notificación reciente sin traducción se sigue consultando. */
export const TRANSLATION_WAIT_MS = 3 * 60 * 1000;

/** Intervalo de consulta mientras haya notificaciones esperando traducción. */
export const TRANSLATION_POLL_MS = 5000;

/** Código base del idioma de la UI ("en-US" → "en"), que es como llegan las traducciones. */
export function baseLanguage(i18n) {
  return (i18n?.resolvedLanguage || i18n?.language || SOURCE_LANGUAGE).split("-")[0].toLowerCase();
}

function findTranslation(notification, language) {
  if (!notification || language === SOURCE_LANGUAGE) return null;
  return notification.translations?.find(
    (t) => t.languageCode?.split("-")[0].toLowerCase() === language && t.status === "PUBLISHED"
  ) ?? null;
}

/** Asunto y cuerpo en el idioma indicado, con el original como respaldo. */
export function localizeNotification(notification, language) {
  const translation = findTranslation(notification, language);
  return {
    subject: translation?.translatedSubject || notification?.subject,
    body: translation?.translatedBody || notification?.body,
  };
}

/** true si la notificación es reciente y todavía no llegó su traducción al idioma indicado. */
export function isAwaitingTranslation(notification, language, now = Date.now()) {
  if (!notification || language === SOURCE_LANGUAGE || findTranslation(notification, language)) {
    return false;
  }
  // sentAt llega sin zona horaria: leído como hora local, un servidor en UTC lo dejaría 5 h en
  // el futuro y la notificación esperaría traducción (y se consultaría) todo ese tiempo
  const sentAt = parsePastServerDate(notification.sentAt, now);
  return Number.isFinite(sentAt) && now - sentAt < TRANSLATION_WAIT_MS;
}

/**
 * Copia las traducciones recién llegadas sobre la lista actual sin pisar el resto
 * de campos (p. ej. readAt marcado localmente). Misma referencia si nada cambió.
 */
export function mergeTranslations(current, fresh) {
  const byId = new Map((fresh || []).map((n) => [n.notificationUserId, n]));
  let changed = false;
  const merged = current.map((n) => {
    const updated = byId.get(n.notificationUserId);
    if (!updated || JSON.stringify(updated.translations ?? []) === JSON.stringify(n.translations ?? [])) {
      return n;
    }
    changed = true;
    return { ...n, translations: updated.translations };
  });
  return changed ? merged : current;
}
