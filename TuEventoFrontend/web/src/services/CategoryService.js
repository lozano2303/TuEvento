import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

// Endpoints públicos - sin auth
export const getCategoriesByEvent = async (eventId) => {
  const response = await fetch(`${API_URL}/events/${eventId}/categories`);
  if (!response.ok) throw new Error('Error al obtener categorías');
  return response.json();
};

export const getAllCategories = async (languageCode = null) => {
  const url = languageCode 
    ? `${API_URL}/categories?language=${languageCode}`
    : `${API_URL}/categories`;
  const response = await fetch(url);
  if (!response.ok) throw new Error('Error al obtener categorías');
  return response.json();
};

export const getActiveCategories = async (languageCode = null) => {
  const url = languageCode 
    ? `${API_URL}/categories/active?language=${languageCode}`
    : `${API_URL}/categories/active`;
  const response = await fetch(url);
  if (!response.ok) throw new Error('Error al obtener categorías activas');
  return response.json(); // ApiResponse<List<CategoryResponse>>
};

export const getCategoryById = async (categoryId, languageCode = null) => {
  const url = languageCode 
    ? `${API_URL}/categories/${categoryId}?language=${languageCode}`
    : `${API_URL}/categories/${categoryId}`;
  const response = await fetch(url);
  if (!response.ok) throw new Error('Error al obtener categoría');
  return response.json();
};

export const getRootCategories = async (languageCode = null) => {
  const url = languageCode 
    ? `${API_URL}/categories/root?language=${languageCode}`
    : `${API_URL}/categories/root`;
  const response = await fetch(url);
  if (!response.ok) throw new Error('Error al obtener categorías raíz');
  return response.json();
};

export const getSubcategories = async (parentId, languageCode = null) => {
  const url = languageCode 
    ? `${API_URL}/categories/${parentId}/subcategories?language=${languageCode}`
    : `${API_URL}/categories/${parentId}/subcategories`;
  const response = await fetch(url);
  if (!response.ok) throw new Error('Error al obtener subcategorías');
  return response.json();
};

// Endpoints con auth - ORGANIZER/ADMIN
export const addCategoryToEvent = async (eventId, categoryId) => {
  const response = await httpRequest(`${API_URL}/events/${eventId}/categories`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ categoryId }),
  });
  if (!response.ok) throw new Error('Error al añadir categoría');
  return response.json();
};

export const removeCategoryFromEvent = async (eventId, categoryId) => {
  const response = await httpRequest(`${API_URL}/events/${eventId}/categories/${categoryId}`, {
    method: 'DELETE',
  });
  if (!response.ok) throw new Error('Error al eliminar categoría');
  return response.json();
};