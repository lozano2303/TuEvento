import { parsePastServerDate } from "../../src/utils/serverDate";

const NOW = Date.parse("2026-10-28T20:00:00Z");
/** Fecha sin zona con la hora de pared de `ms` en UTC (servidor en UTC). */
const naiveUtc = (ms) => new Date(ms).toISOString().replace("Z", "");

describe("parsePastServerDate", () => {
  it("respeta las fechas que traen zona", () => {
    expect(parsePastServerDate("2026-10-28T19:58:00Z", NOW)).toBe(Date.parse("2026-10-28T19:58:00Z"));
    expect(parsePastServerDate("2026-10-28T14:58:00-05:00", NOW)).toBe(Date.parse("2026-10-28T19:58:00Z"));
  });

  it("una fecha de un servidor en UTC sin zona no queda en el futuro", () => {
    const twoMinutesAgo = NOW - 2 * 60 * 1000;
    expect(parsePastServerDate(`${naiveUtc(twoMinutesAgo)}123`, NOW)).toBe(twoMinutesAgo);
  });

  it("devuelve NaN si no hay fecha o no se puede leer", () => {
    expect(parsePastServerDate(null, NOW)).toBeNaN();
    expect(parsePastServerDate("no es fecha", NOW)).toBeNaN();
  });

  it("una fecha realmente futura se devuelve tal cual (la más cercana)", () => {
    const inOneDay = NOW + 24 * 60 * 60 * 1000;
    expect(parsePastServerDate(new Date(inOneDay).toISOString(), NOW)).toBe(inOneDay);
  });
});
