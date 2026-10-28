import { normalizeReservedUntil, SEAT_HOLD_MS } from "../../src/utils/seatHold";

const NOW = Date.parse("2026-10-28T20:00:00Z");
const minutesLeft = (iso) => (Date.parse(iso) - NOW) / 60000;

/** Fecha sin zona como la envía el backend (LocalDateTime), con la hora de pared de `ms` en UTC. */
const naiveUtc = (ms) => new Date(ms).toISOString().replace("Z", "");

describe("normalizeReservedUntil", () => {
  it("sin bloqueo devuelve null", () => {
    expect(normalizeReservedUntil(null, NOW)).toBeNull();
    expect(normalizeReservedUntil(undefined, NOW)).toBeNull();
  });

  it("respeta una fecha con zona dentro de los 10 minutos", () => {
    expect(minutesLeft(normalizeReservedUntil("2026-10-28T20:07:30Z", NOW))).toBeCloseTo(7.5);
  });

  it("un servidor en UTC sin zona da los 10 minutos, no horas de más", () => {
    const iso = normalizeReservedUntil(`${naiveUtc(NOW + SEAT_HOLD_MS)}123456`, NOW);
    expect(minutesLeft(iso)).toBeGreaterThan(9.9);
    expect(minutesLeft(iso)).toBeLessThanOrEqual(10);
  });

  it("nunca pasa de 10 minutos aunque la fecha recibida sea posterior", () => {
    expect(minutesLeft(normalizeReservedUntil("2026-10-29T03:00:00Z", NOW))).toBe(10);
  });

  it("una fecha ilegible cuenta el bloqueo completo", () => {
    expect(minutesLeft(normalizeReservedUntil("no es fecha", NOW))).toBe(10);
  });

  it("una reserva ya vencida sigue vencida", () => {
    expect(minutesLeft(normalizeReservedUntil("2026-10-28T19:50:00Z", NOW))).toBeLessThan(0);
  });
});
