import AsyncStorage from "@react-native-async-storage/async-storage";

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
 * Obtiene la lista de eventos públicos publicados.
 * Endpoint público - no requiere autenticación, pero si hay token lo envía.
 *
 * @param {string|null} languageCode - código de idioma para traducción dinámica
 * @returns {Promise<Array>} Array de EventSummaryResponse
 */
export const getPublishedEvents = async (languageCode = null) => {
  const token = await AsyncStorage.getItem("accessToken");
  
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}/events/public${langQuery(languageCode)}`, { headers });
  
  if (!response.ok) {
    throw new Error(`Error fetching published events: ${response.status}`);
  }
  
  const json = await response.json();
  return json.data; // Array de EventSummaryResponse
};

/**
 * Obtiene los eventos públicos filtrados por ciudad.
 * 
 * @param {number} cityId - ID de la ciudad
 * @returns {Promise<Array>} Array de EventSummaryResponse
 */
export const getPublishedEventsByCity = async (cityId, languageCode = null) => {
  const token = await AsyncStorage.getItem("accessToken");
  
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}/events/public/city/${cityId}${langQuery(languageCode)}`, { headers });
  
  if (!response.ok) {
    throw new Error(`Error fetching events by city: ${response.status}`);
  }
  
  const json = await response.json();
  return json.data;
};

/**
 * Obtiene los eventos públicos filtrados por categoría.
 * 
 * @param {number} categoryId - ID de la categoría
 * @returns {Promise<Array>} Array de EventSummaryResponse
 */
export const getPublishedEventsByCategory = async (categoryId, languageCode = null) => {
  const token = await AsyncStorage.getItem("accessToken");
  
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}/events/public/category/${categoryId}${langQuery(languageCode)}`, { headers });
  
  if (!response.ok) {
    throw new Error(`Error fetching events by category: ${response.status}`);
  }
  
  const json = await response.json();
  return json.data;
};

/**
 * Obtiene los eventos públicos en un rango de fechas.
 * 
 * @param {string} from - Fecha inicio (formato ISO: YYYY-MM-DD)
 * @param {string} to - Fecha fin (formato ISO: YYYY-MM-DD)
 * @returns {Promise<Array>} Array de EventSummaryResponse
 */
export const getPublishedEventsByDateRange = async (from, to, languageCode = null) => {
  const token = await AsyncStorage.getItem("accessToken");
  
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const langParam = languageCode && languageCode !== "es" ? `&language=${languageCode}` : "";
  const response = await fetch(
    `${BASE_URL}/events/public/date-range?from=${from}&to=${to}${langParam}`,
    { headers }
  );
  
  if (!response.ok) {
    throw new Error(`Error fetching events by date range: ${response.status}`);
  }
  
  const json = await response.json();
  return json.data;
};

/**
 * Obtiene el detalle completo de un evento.
 *
 * @param {number} eventId - ID del evento
 * @param {string|null} languageCode - código de idioma para traducción dinámica
 * @returns {Promise<Object>} EventResponse (incluye translatedName/translatedDescription/translationGenerating)
 */
export const getEventDetail = async (eventId, languageCode = null) => {
  const token = await AsyncStorage.getItem("accessToken");
  
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}/events/${eventId}${langQuery(languageCode)}`, { headers });
  
  if (!response.ok) {
    throw new Error(`Error fetching event detail: ${response.status}`);
  }
  
  const json = await response.json();
  return json.data;
};

/**
 * Obtiene las imágenes/media de un evento.
 * 
 * @param {number} eventId - ID del evento
 * @returns {Promise<Array>} Array de EventMediaResponse
 */
export const getEventMedia = async (eventId) => {
  const token = await AsyncStorage.getItem("accessToken");
  
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}/events/${eventId}/media`, { headers });
  
  if (!response.ok) {
    throw new Error(`Error fetching event media: ${response.status}`);
  }
  
  const json = await response.json();
  return json.data;
};

/**
 * Obtiene el layout visual de un evento.
 * 
 * @param {number} eventId - ID del evento
 * @returns {Promise<Object>} EventLayoutResponse { eventLayoutId, eventId, layoutData: string }
 */
export const getEventLayout = async (eventId) => {
  const token = await AsyncStorage.getItem("accessToken");
  
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}/events/${eventId}/layout`, { headers });
  
  if (!response.ok) {
    if (response.status === 404) {
      // El evento no tiene layout guardado todavía
      return null;
    }
    throw new Error(`Error fetching event layout: ${response.status}`);
  }
  
  const json = await response.json();
  return json.data; // { eventLayoutId, eventId, layoutData: string (JSON) }
};

/**
 * Obtiene las secciones de un evento con sus precios.
 * 
 * @param {number} eventId - ID del evento
 * @returns {Promise<Array>} Array de EventSectionResponse
 */
export const getEventSections = async (eventId) => {
  const token = await AsyncStorage.getItem("accessToken");
  
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}/event-sections/event/${eventId}`, { headers });
  
  if (!response.ok) {
    throw new Error(`Error fetching event sections: ${response.status}`);
  }
  
  const json = await response.json();
  return json.data;
};
