import { httpRequest } from "./httpClient";

const BASE_URL = process.env.EXPO_PUBLIC_API_URL;

/** IDs de idioma en la BD (mismo mapa que la web): ES=1, EN=2, FR=3, PT=4. */
export const LANGUAGE_IDS = { es: 1, en: 2, fr: 3, pt: 4 };

/**
 * Ruta del backend por módulo: GET /admin/{path}/{lang}?filter= y
 * PUT /admin/{path}/{id}/language/{languageId} (crea o actualiza).
 */
const MODULE_PATHS = {
  events:       "event-translations",
  categories:   "category-translations",
  sectionTypes: "section-type-translations",
  comments:     "comment-translations",
  profiles:     "profile-translations",
};

/**
 * Lista las entidades de un módulo con su traducción al idioma dado.
 * @param {string} module - events | categories | sectionTypes | comments | profiles
 * @param {string} languageCode - en | fr | pt
 * @param {string} filter - all | needs-review | translated | not-translated
 * @param {number} [eventId] - obligatorio para comments
 */
export const getTranslations = async (module, languageCode, filter = "all", eventId = null) => {
  const eventQuery = module === "comments" ? `&eventId=${eventId}` : "";
  const response = await httpRequest(
    `${BASE_URL}/admin/${MODULE_PATHS[module]}/${languageCode}?filter=${filter}${eventQuery}`
  );
  if (!response.ok) {
    const err = await response.json().catch(() => ({}));
    throw new Error(err.message || `Error al obtener traducciones: ${response.status}`);
  }
  // Eventos, categorías y tipos de sección vienen en ApiResponse ({ data: [...] });
  // comentarios y perfiles devuelven el array directo
  const json = await response.json();
  return Array.isArray(json) ? json : (json.data ?? []);
};

/** Guarda (crea o actualiza) la traducción de una entidad. */
export const saveTranslation = async (module, entityId, languageId, data) => {
  const response = await httpRequest(
    `${BASE_URL}/admin/${MODULE_PATHS[module]}/${entityId}/language/${languageId}`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    }
  );
  if (!response.ok) {
    const err = await response.json().catch(() => ({}));
    throw new Error(err.message || `Error al guardar traducción: ${response.status}`);
  }
  return response.json();
};
