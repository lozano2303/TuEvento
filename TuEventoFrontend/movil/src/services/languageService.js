const BASE_URL = process.env.EXPO_PUBLIC_API_URL;

/**
 * Lista de idiomas activos del backend — GET /languages (público, sin auth).
 * Cada item incluye al menos { languageId, code }.
 * Espejo de web/src/services/LanguageService.js.
 * @returns {Promise<Array>}
 */
export const getActiveLanguages = async () => {
  const res = await fetch(`${BASE_URL}/languages`);
  if (!res.ok) throw new Error('Error al obtener idiomas');
  const json = await res.json();
  return json.data || [];
};

/**
 * Obtiene un idioma por su código — GET /languages/{code}.
 * @param {string} code
 */
export const getLanguageByCode = async (code) => {
  const res = await fetch(`${BASE_URL}/languages/${code}`);
  if (!res.ok) throw new Error('Error al obtener el idioma');
  const json = await res.json();
  return json.data;
};
