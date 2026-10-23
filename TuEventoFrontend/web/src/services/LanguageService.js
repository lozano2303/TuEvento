/**
 * LanguageService — manejo de idiomas del sistema.
 */
import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/**
 * Obtiene todos los idiomas activos del sistema.
 * GET /languages — sin auth requerida (público)
 */
export const getActiveLanguages = async () => {
  const response = await fetch(`${API_URL}/languages`);
  if (!response.ok) throw new Error('Error al obtener idiomas');
  const data = await response.json();
  return data?.data || [];
};

/**
 * Obtiene un idioma por su código (ej: "es", "en").
 * GET /languages/{code} — sin auth
 */
export const getLanguageByCode = async (code) => {
  const response = await fetch(`${API_URL}/languages/${code}`);
  if (!response.ok) throw new Error('Error al obtener idioma');
  const data = await response.json();
  return data?.data;
};
