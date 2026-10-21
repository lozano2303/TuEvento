import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/**
 * Servicio para gestionar traducciones de diferentes entidades
 */

// ========== EVENT TRANSLATIONS ==========

/**
 * Obtiene todas las traducciones de eventos para un idioma específico
 * @param {string} languageCode - Código del idioma (es, en, fr, pt)
 * @param {string} filter - Filtro: 'all', 'needs-review', 'translated', 'not-translated'
 * @returns {Promise} Lista de eventos con sus traducciones
 */
export const getEventTranslations = async (languageCode, filter = 'all') => {
  const response = await httpRequest(`${API_URL}/admin/event-translations/${languageCode}?filter=${filter}`);
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Error al obtener traducciones' }));
    throw new Error(error.message || 'Error al obtener traducciones de eventos');
  }
  return response.json();
};

/**
 * Actualiza o crea la traducción de un evento
 * @param {number} eventId - ID del evento
 * @param {number} languageId - ID del idioma
 * @param {object} translationData - Datos de la traducción
 * @returns {Promise}
 */
export const updateEventTranslation = async (eventId, languageId, translationData) => {
  const response = await httpRequest(
    `${API_URL}/admin/event-translations/${eventId}/language/${languageId}`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(translationData),
    }
  );
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Error al actualizar traducción' }));
    throw new Error(error.message || 'Error al actualizar traducción');
  }
  return response.json();
};

/**
 * Alias de updateEventTranslation (PUT crea o actualiza)
 */
export const createEventTranslation = updateEventTranslation;

// ========== CATEGORY TRANSLATIONS ==========

/**
 * Obtiene todas las traducciones de categorías para un idioma específico
 * @param {string} languageCode - Código del idioma (es, en, fr, pt)
 * @param {string} filter - Filtro: 'all', 'needs-review', 'translated', 'not-translated'
 * @returns {Promise} Lista de categorías con sus traducciones
 */
export const getCategoryTranslations = async (languageCode, filter = 'all') => {
  const response = await httpRequest(`${API_URL}/admin/category-translations/${languageCode}?filter=${filter}`);
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Error al obtener traducciones' }));
    throw new Error(error.message || 'Error al obtener traducciones de categorías');
  }
  return response.json();
};

/**
 * Actualiza o crea la traducción de una categoría
 * @param {number} categoryId - ID de la categoría
 * @param {number} languageId - ID del idioma
 * @param {object} translationData - Datos de la traducción
 * @returns {Promise}
 */
export const updateCategoryTranslation = async (categoryId, languageId, translationData) => {
  const response = await httpRequest(
    `${API_URL}/admin/category-translations/${categoryId}/language/${languageId}`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(translationData),
    }
  );
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Error al actualizar traducción' }));
    throw new Error(error.message || 'Error al actualizar traducción');
  }
  return response.json();
};

/**
 * Alias de updateCategoryTranslation (PUT crea o actualiza)
 */
export const createCategoryTranslation = updateCategoryTranslation;

// ========== SECTION TYPE TRANSLATIONS ==========

/**
 * Obtiene todas las traducciones de tipos de sección para un idioma específico
 * @param {string} languageCode - Código del idioma (es, en, fr, pt)
 * @param {string} filter - Filtro: 'all', 'needs-review', 'translated', 'not-translated'
 * @returns {Promise} Lista de tipos de sección con sus traducciones
 */
export const getSectionTypeTranslations = async (languageCode, filter = 'all') => {
  const response = await httpRequest(`${API_URL}/admin/section-type-translations/${languageCode}?filter=${filter}`);
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Error al obtener traducciones' }));
    throw new Error(error.message || 'Error al obtener traducciones de tipos de sección');
  }
  return response.json();
};

/**
 * Actualiza o crea la traducción de un tipo de sección
 * @param {number} sectionTypeId - ID del tipo de sección
 * @param {number} languageId - ID del idioma
 * @param {object} translationData - Datos de la traducción
 * @returns {Promise}
 */
export const updateSectionTypeTranslation = async (sectionTypeId, languageId, translationData) => {
  const response = await httpRequest(
    `${API_URL}/admin/section-type-translations/${sectionTypeId}/language/${languageId}`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(translationData),
    }
  );
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Error al actualizar traducción' }));
    throw new Error(error.message || 'Error al actualizar traducción');
  }
  return response.json();
};

/**
 * Alias de updateSectionTypeTranslation (PUT crea o actualiza)
 */
export const createSectionTypeTranslation = updateSectionTypeTranslation;

// ========== COMMENT TRANSLATIONS ==========

/**
 * Obtiene todas las traducciones de comentarios para un idioma y evento específico
 * @param {string} languageCode - Código del idioma (es, en, fr, pt)
 * @param {number} eventId - ID del evento
 * @param {string} filter - Filtro: 'all', 'needs-review', 'translated', 'not-translated'
 * @returns {Promise} Lista de comentarios con sus traducciones
 */
export const getCommentTranslations = async (languageCode, eventId, filter = 'all') => {
  const response = await httpRequest(`${API_URL}/admin/comment-translations/${languageCode}?eventId=${eventId}&filter=${filter}`);
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Error al obtener traducciones' }));
    throw new Error(error.message || 'Error al obtener traducciones de comentarios');
  }
  return response.json();
};

/**
 * Actualiza o crea la traducción de un comentario
 * @param {number} ratingId - ID del comentario
 * @param {number} languageId - ID del idioma
 * @param {object} translationData - Datos de la traducción
 * @returns {Promise}
 */
export const updateCommentTranslation = async (ratingId, languageId, translationData) => {
  const response = await httpRequest(
    `${API_URL}/admin/comment-translations/${ratingId}/language/${languageId}`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(translationData),
    }
  );
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Error al actualizar traducción' }));
    throw new Error(error.message || 'Error al actualizar traducción');
  }
  return response.json();
};

/**
 * Alias de updateCommentTranslation (PUT crea o actualiza)
 */
export const createCommentTranslation = updateCommentTranslation;

// ========== PROFILE TRANSLATIONS ==========

/**
 * Obtiene todas las traducciones de perfiles para un idioma
 * @param {string} languageCode - Código del idioma (es, en, fr, pt)
 * @param {string} filter - Filtro: 'all', 'needs-review', 'translated', 'not-translated'
 * @returns {Promise} Lista de perfiles con sus traducciones
 */
export const getProfileTranslations = async (languageCode, filter = 'all') => {
  const response = await httpRequest(`${API_URL}/admin/profile-translations/${languageCode}?filter=${filter}`);
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Error al obtener traducciones' }));
    throw new Error(error.message || 'Error al obtener traducciones de perfiles');
  }
  return response.json();
};

/**
 * Actualiza o crea la traducción de un perfil
 * @param {number} profileId - ID del perfil
 * @param {number} languageId - ID del idioma
 * @param {object} translationData - Datos de la traducción
 * @returns {Promise}
 */
export const updateProfileTranslation = async (profileId, languageId, translationData) => {
  const response = await httpRequest(
    `${API_URL}/admin/profile-translations/${profileId}/language/${languageId}`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(translationData),
    }
  );
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Error al actualizar traducción' }));
    throw new Error(error.message || 'Error al actualizar traducción');
  }
  return response.json();
};

/**
 * Alias de updateProfileTranslation (PUT crea o actualiza)
 */
export const createProfileTranslation = updateProfileTranslation;

// ========== LANGUAGE UTILS ==========

/**
 * Obtiene información de los idiomas disponibles
 * @returns {Promise} Lista de idiomas
 */
export const getLanguages = async () => {
  const response = await httpRequest(`${API_URL}/languages`);
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Error al obtener idiomas' }));
    throw new Error(error.message || 'Error al obtener idiomas');
  }
  return response.json();
};

/**
 * Mapeo de códigos de idioma a IDs (según la base de datos)
 * ES=1, EN=2, FR=3, PT=4
 */
export const LANGUAGE_MAP = {
  es: 1,
  en: 2,
  fr: 3,
  pt: 4,
};

/**
 * Obtiene el ID de un idioma por su código
 * @param {string} code - Código del idioma (es, en, fr, pt)
 * @returns {number} ID del idioma
 */
export const getLanguageId = (code) => {
  return LANGUAGE_MAP[code?.toLowerCase()] || 1;
};
