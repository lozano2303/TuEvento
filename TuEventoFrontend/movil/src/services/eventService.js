import { optionalAuthRequest } from "./httpClient";

const BASE_URL = process.env.EXPO_PUBLIC_API_URL;

/**
 * Construye el sufijo ?language=<code> para contenido dinámico.
 * Español es el idioma fuente: con 'es' o null no se solicita traducción
 * (mismo criterio que la web).
 * @param {string|null} languageCode
 * @returns {string} '' o '?language=<code>'
 */
const langQuery = (languageCode) =>
  languageCode && languageCode !== "es" ? `?language=${languageCode}` : "";

/**
 * GET a un endpoint público de eventos: envía el token si hay sesión (renovándolo si
 * expiró) y devuelve el campo data. Con notFoundAsNull, un 404 devuelve null.
 */
const getPublic = async (path, errorLabel, { notFoundAsNull = false } = {}) => {
  const response = await optionalAuthRequest(`${BASE_URL}${path}`);

  if (!response.ok) {
    if (notFoundAsNull && response.status === 404) return null;
    throw new Error(`Error fetching ${errorLabel}: ${response.status}`);
  }

  const json = await response.json();
  return json.data;
};

/**
 * Obtiene la lista de eventos públicos publicados.
 * Endpoint público - no requiere autenticación, pero si hay token lo envía.
 *
 * @param {string|null} languageCode - código de idioma para traducción dinámica
 * @returns {Promise<Array>} Array de EventSummaryResponse
 */
export const getPublishedEvents = (languageCode = null) =>
  getPublic(`/events/public${langQuery(languageCode)}`, "published events");

/**
 * Obtiene los eventos públicos filtrados por ciudad.
 *
 * @param {number} cityId - ID de la ciudad
 * @returns {Promise<Array>} Array de EventSummaryResponse
 */
export const getPublishedEventsByCity = (cityId, languageCode = null) =>
  getPublic(`/events/public/city/${cityId}${langQuery(languageCode)}`, "events by city");

/**
 * Obtiene los eventos públicos filtrados por categoría.
 *
 * @param {number} categoryId - ID de la categoría
 * @returns {Promise<Array>} Array de EventSummaryResponse
 */
export const getPublishedEventsByCategory = (categoryId, languageCode = null) =>
  getPublic(`/events/public/category/${categoryId}${langQuery(languageCode)}`, "events by category");

/**
 * Obtiene los eventos públicos en un rango de fechas.
 *
 * @param {string} from - Fecha inicio (formato ISO: YYYY-MM-DD)
 * @param {string} to - Fecha fin (formato ISO: YYYY-MM-DD)
 * @returns {Promise<Array>} Array de EventSummaryResponse
 */
export const getPublishedEventsByDateRange = (from, to, languageCode = null) => {
  const langParam = languageCode && languageCode !== "es" ? `&language=${languageCode}` : "";
  return getPublic(`/events/public/date-range?from=${from}&to=${to}${langParam}`, "events by date range");
};

/**
 * Obtiene el detalle completo de un evento.
 *
 * @param {number} eventId - ID del evento
 * @param {string|null} languageCode - código de idioma para traducción dinámica
 * @returns {Promise<Object>} EventResponse (incluye translatedName/translatedDescription/translationGenerating)
 */
export const getEventDetail = (eventId, languageCode = null) =>
  getPublic(`/events/${eventId}${langQuery(languageCode)}`, "event detail");

/**
 * Obtiene las imágenes/media de un evento.
 *
 * @param {number} eventId - ID del evento
 * @returns {Promise<Array>} Array de EventMediaResponse
 */
export const getEventMedia = (eventId) => getPublic(`/events/${eventId}/media`, "event media");

/**
 * Obtiene el layout visual de un evento; null si el evento aún no tiene layout guardado.
 *
 * @param {number} eventId - ID del evento
 * @returns {Promise<Object|null>} EventLayoutResponse { eventLayoutId, eventId, layoutData: string (JSON) }
 */
export const getEventLayout = (eventId) =>
  getPublic(`/events/${eventId}/layout`, "event layout", { notFoundAsNull: true });

/**
 * Obtiene las secciones de un evento con sus precios.
 *
 * @param {number} eventId - ID del evento
 * @returns {Promise<Array>} Array de EventSectionResponse
 */
export const getEventSections = (eventId) =>
  getPublic(`/event-sections/event/${eventId}`, "event sections");
