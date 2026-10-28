/**
 * Fechas del backend sin zona horaria (LocalDateTime: "2026-10-28T20:00:00.123456").
 *
 * Leídas tal cual quedan en la hora local del teléfono: con el servidor en UTC y el teléfono en
 * Colombia, algo que acaba de pasar aparece 5 horas en el futuro. Leídas siempre como UTC
 * fallarían con un servidor en hora local. Para fechas que ya ocurrieron (envío, lectura) se
 * prueban ambas lecturas y se queda la más reciente que no esté en el futuro.
 *
 * Espejo de TuEventoFrontend/web/src/utils/serverDate.js: las dos apps se construyen por separado y no
 * pueden compartir el módulo. Cualquier cambio aquí debe replicarse allá.
 */

// Margen para relojes del teléfono y del servidor que no coinciden exactamente
const CLOCK_SLACK_MS = 60 * 1000;

const HAS_ZONE = /(Z|[+-]\d{2}:?\d{2})$/i;

/** Las fracciones de más de 3 dígitos no las lee cualquier motor de JS. */
const trimFraction = (text) => text.replace(/(\.\d{3})\d+/, "$1");

/**
 * Milisegundos de una fecha pasada enviada por el servidor, o NaN si no se puede leer.
 */
export function parsePastServerDate(value, now = Date.now()) {
  if (!value) return Number.NaN;
  const text = trimFraction(String(value));
  if (HAS_ZONE.test(text)) return Date.parse(text);

  const candidates = [Date.parse(text), Date.parse(`${text}Z`)].filter((ms) => !Number.isNaN(ms));
  const past = candidates.filter((ms) => ms <= now + CLOCK_SLACK_MS);
  if (past.length > 0) return Math.max(...past);
  return candidates.length > 0 ? Math.min(...candidates) : Number.NaN;
}
