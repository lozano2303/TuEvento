import {
  TRANSLATION_WAIT_MS,
  baseLanguage,
  isAwaitingTranslation,
  localizeNotification,
  mergeTranslations,
} from "../../src/utils/notificationI18n";

const published = {
  notificationUserId: 3,
  subject: "Pago aprobado",
  body: "Tu pago fue aprobado",
  translations: [
    { languageCode: "fr", status: "PUBLISHED", translatedSubject: "Paiement approuvé", translatedBody: "OK" },
  ],
};

describe("notificationI18n (móvil)", () => {
  it("usa el código base del idioma", () => {
    expect(baseLanguage({ language: "fr-CA" })).toBe("fr");
  });

  it("devuelve la traducción publicada o el original", () => {
    expect(localizeNotification(published, "fr").subject).toBe("Paiement approuvé");
    expect(localizeNotification(published, "en").subject).toBe("Pago aprobado");
  });

  it("solo copia traducciones cuando cambian", () => {
    const current = [{ ...published, translations: [] }];
    expect(mergeTranslations(current, [published])[0].translations).toHaveLength(1);
    expect(mergeTranslations([published], [published])).toEqual([published]);
  });
});

describe("isAwaitingTranslation con sentAt sin zona horaria", () => {
  const NOW = Date.parse("2026-10-28T20:00:00Z");
  /** Como lo envía un servidor en UTC: LocalDateTime sin zona. */
  const naiveUtc = (ms) => new Date(ms).toISOString().replace("Z", "");
  const pending = (sentAt) => ({ ...published, sentAt, translations: [] });

  it("espera la traducción de una notificación reciente", () => {
    expect(isAwaitingTranslation(pending(naiveUtc(NOW - 60 * 1000)), "en", NOW)).toBe(true);
  });

  it("deja de esperar pasados unos minutos", () => {
    expect(isAwaitingTranslation(pending(naiveUtc(NOW - TRANSLATION_WAIT_MS - 1000)), "en", NOW)).toBe(false);
  });

  it("no espera en español ni si ya hay traducción", () => {
    expect(isAwaitingTranslation(pending(naiveUtc(NOW)), "es", NOW)).toBe(false);
    expect(isAwaitingTranslation({ ...published, sentAt: naiveUtc(NOW) }, "fr", NOW)).toBe(false);
  });
});
