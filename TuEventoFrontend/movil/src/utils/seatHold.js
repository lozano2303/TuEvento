/** Las sillas quedan bloqueadas 10 minutos al reservarlas (RESERVATION_TTL_MINUTES en el backend). */
export const SEAT_HOLD_MS = 10 * 60 * 1000;

// Margen para relojes del teléfono y del servidor que no coinciden exactamente
const CLOCK_SLACK_MS = 60 * 1000;

const HAS_ZONE = /(Z|[+-]\d{2}:?\d{2})$/i;

/** Milisegundos de una fecha ISO; las fracciones de más de 3 dígitos no las lee cualquier motor. */
function parseIso(value) {
  return Date.parse(value.replace(/(\.\d{3})\d+/, "$1"));
}

/**
 * Fin del bloqueo de una silla como ISO en UTC, nunca a más de 10 minutos de `now`.
 *
 * El backend envía `reservedUntil` sin zona horaria (LocalDateTime con la hora del servidor).
 * Leído como hora local, un servidor en UTC y un teléfono en Colombia dan 5 horas de más; leído
 * siempre como UTC fallaría con un servidor en hora local. Se prueban ambas lecturas y se queda la
 * que cabe en el bloqueo de 10 minutos.
 *
 * @returns {string|null} null si no hay bloqueo
 */
export function normalizeReservedUntil(value, now = Date.now()) {
  if (!value) return null;
  const text = String(value);
  const latest = now + SEAT_HOLD_MS;

  const candidates = HAS_ZONE.test(text)
    ? [parseIso(text)]
    : [parseIso(text), parseIso(`${text.replace(/(\.\d{3})\d+/, "$1")}Z`)];
  const plausible = candidates.filter((ms) => !Number.isNaN(ms) && ms <= latest + CLOCK_SLACK_MS);

  const deadline = plausible.length > 0 ? Math.min(Math.max(...plausible), latest) : latest;
  return new Date(deadline).toISOString();
}
