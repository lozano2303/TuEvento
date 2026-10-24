// Helpers de presentación de eventos compartidos por Home, Eventos y Detalle.

const LOCALES = { es: "es-CO", en: "en-US", pt: "pt-BR", fr: "fr-FR" };

/** Locale de Intl para el idioma activo de la app. */
export const localeFor = (language) => LOCALES[language] ?? LOCALES.es;

/** Nombre a mostrar: la traducción del backend si el idioma no es español y existe. */
export const displayEventName = (event, language) =>
  language !== "es" && event?.translatedName ? event.translatedName : event?.eventName;

/**
 * Convierte un LocalDate ("2026-11-02") en Date local. new Date("2026-11-02") lo
 * interpreta como UTC y en Colombia se mostraría el día anterior.
 */
export const parseLocalDate = (value) => {
  if (!value) return null;
  const str = String(value);
  return new Date(/^\d{4}-\d{2}-\d{2}$/.test(str) ? `${str}T00:00:00` : str);
};

/** Fecha corta en el idioma activo; si hay fecha de fin distinta, muestra el rango. */
export const formatEventDate = (startDate, finishDate, language, withYear = false) => {
  const start = parseLocalDate(startDate);
  if (!start) return null;
  const options = { day: "numeric", month: "short", ...(withYear ? { year: "numeric" } : {}) };
  const locale = localeFor(language);
  const startStr = start.toLocaleDateString(locale, options);
  const finish = parseLocalDate(finishDate);
  if (finish && finish.getTime() !== start.getTime()) {
    return `${startStr} - ${finish.toLocaleDateString(locale, options)}`;
  }
  return startStr;
};

/** Reemplaza localhost por el host de MinIO accesible desde el dispositivo. */
export const resolveCoverUrl = (url) =>
  url ? url.replace("localhost", process.env.EXPO_PUBLIC_MINIO_HOST ?? "localhost") : null;
