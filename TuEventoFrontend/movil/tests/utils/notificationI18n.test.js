import { baseLanguage, localizeNotification, mergeTranslations } from "../../src/utils/notificationI18n";

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
