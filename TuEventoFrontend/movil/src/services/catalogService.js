import { httpRequest } from "./httpClient";

const BASE_URL = process.env.EXPO_PUBLIC_API_URL;

const JSON_HEADERS = { "Content-Type": "application/json" };

// ══════════════════════════════════════════════════════════════════════════════
// CATEGORÍAS
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Lista todas las categorías (en español por defecto).
 * GET /api/v1/categories?language=es
 * @returns {Promise<Array<{categoryId, name, description, dadId, active}>>}
 */
export const getAllCategories = async (language = "es") => {
  const response = await fetch(`${BASE_URL}/categories?language=${language}`);
  if (!response.ok) throw new Error("Error al obtener categorías");
  const json = await response.json();
  return json.data ?? [];
};

/**
 * Lista categorías raíz (para selector de padre).
 * GET /api/v1/categories/root?language=es
 */
export const getRootCategories = async (language = "es") => {
  const response = await fetch(`${BASE_URL}/categories/root?language=${language}`);
  if (!response.ok) throw new Error("Error al obtener categorías principales");
  const json = await response.json();
  return json.data ?? [];
};

/**
 * Crea una categoría.
 * POST /api/v1/categories  { name, description|null, dadId|null }
 */
export const createCategory = async ({ name, description, dadId }) => {
  const response = await httpRequest(`${BASE_URL}/categories`, {
    method: "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify({
      name: name.trim(),
      description: description?.trim() || null,
      dadId: dadId || null,
    }),
  });
  const json = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(json.message || "Error al crear categoría");
  return json.data;
};

/**
 * Actualiza una categoría.
 * PUT /api/v1/categories/{categoryId}  { name, description|null, dadId|null }
 */
export const updateCategory = async (categoryId, { name, description, dadId }) => {
  const response = await httpRequest(`${BASE_URL}/categories/${categoryId}`, {
    method: "PUT",
    headers: JSON_HEADERS,
    body: JSON.stringify({
      name: name.trim(),
      description: description?.trim() || null,
      dadId: dadId || null,
    }),
  });
  const json = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(json.message || "Error al actualizar categoría");
  return json.data;
};

/**
 * Activa o desactiva una categoría.
 * PUT /api/v1/categories/{categoryId}/activate|deactivate
 */
export const toggleCategoryActive = async (categoryId, active) => {
  const endpoint = active ? "deactivate" : "activate";
  const response = await httpRequest(`${BASE_URL}/categories/${categoryId}/${endpoint}`, {
    method: "PUT",
    headers: JSON_HEADERS,
  });
  const json = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(json.message || "Error al cambiar estado de la categoría");
  return json.data;
};

// ══════════════════════════════════════════════════════════════════════════════
// TIPOS DE SECCIÓN
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Lista todos los tipos de sección.
 * GET /api/v1/section-types
 * @returns {Promise<Array<{sectionTypeId, name, active}>>}
 */
export const getAllSectionTypes = async () => {
  const response = await fetch(`${BASE_URL}/section-types`);
  if (!response.ok) throw new Error("Error al obtener tipos de sección");
  const json = await response.json();
  return json.data ?? [];
};

/**
 * Crea un tipo de sección.
 * POST /api/v1/section-types  { name }
 */
export const createSectionType = async (name) => {
  const response = await httpRequest(`${BASE_URL}/section-types`, {
    method: "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify({ name: name.trim() }),
  });
  const json = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(json.message || "Error al crear tipo de sección");
  return json.data;
};

/**
 * Actualiza un tipo de sección.
 * PUT /api/v1/section-types/{sectionTypeId}  { name }
 */
export const updateSectionType = async (sectionTypeId, name) => {
  const response = await httpRequest(`${BASE_URL}/section-types/${sectionTypeId}`, {
    method: "PUT",
    headers: JSON_HEADERS,
    body: JSON.stringify({ name: name.trim() }),
  });
  const json = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(json.message || "Error al actualizar tipo de sección");
  return json.data;
};

/**
 * Activa o desactiva un tipo de sección.
 * PUT /api/v1/section-types/{sectionTypeId}/activate|deactivate
 */
export const toggleSectionTypeActive = async (sectionTypeId, active) => {
  const endpoint = active ? "deactivate" : "activate";
  const response = await httpRequest(`${BASE_URL}/section-types/${sectionTypeId}/${endpoint}`, {
    method: "PUT",
    headers: JSON_HEADERS,
  });
  const json = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(json.message || "Error al cambiar estado del tipo de sección");
  return json.data;
};
