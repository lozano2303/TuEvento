import AsyncStorage from "@react-native-async-storage/async-storage";

const BASE_URL = process.env.EXPO_PUBLIC_API_URL;

/**
 * Obtiene la lista de eventos públicos publicados.
 * Endpoint público - no requiere autenticación, pero si hay token lo envía.
 * 
 * @returns {Promise<Array>} Array de EventSummaryResponse
 */
export const getPublishedEvents = async () => {
  const token = await AsyncStorage.getItem("accessToken");
  
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}/events/public`, { headers });
  
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
export const getPublishedEventsByCity = async (cityId) => {
  const token = await AsyncStorage.getItem("accessToken");
  
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}/events/public/city/${cityId}`, { headers });
  
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
export const getPublishedEventsByCategory = async (categoryId) => {
  const token = await AsyncStorage.getItem("accessToken");
  
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}/events/public/category/${categoryId}`, { headers });
  
  if (!response.ok) {
    throw new Error(`Error fetching events by category: ${response.status}`);
  }
  
  const json = await response.json();
  return json.data;
};

/**
 * Obtiene el detalle completo de un evento por su ID.
 * 
 * @param {number} eventId - ID del evento
 * @returns {Promise<Object>} EventResponse
 */
export const getEventDetail = async (eventId) => {
  const token = await AsyncStorage.getItem("accessToken");
  
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}/events/${eventId}`, { headers });
  
  if (!response.ok) {
    throw new Error(`Error fetching event detail: ${response.status}`);
  }
  
  const json = await response.json();
  return json.data; // EventResponse
};

/**
 * Obtiene las imágenes/medios de un evento.
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
  return json.data; // Array de EventMediaResponse
};
