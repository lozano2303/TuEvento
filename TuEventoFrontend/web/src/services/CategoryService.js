import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

// Endpoints públicos - sin auth
export const getCategoriesByEvent = async (eventId) => {
  const response = await fetch(`${API_URL}/events/${eventId}/categories`);
  if (!response.ok) throw new Error('Error al obtener categorías');
  return response.json();
};

export const getAllCategories = async () => {
  const response = await fetch(`${API_URL}/categories`);
  if (!response.ok) throw new Error('Error al obtener categorías');
  return response.json();
};

export const getActiveCategories = async () => {
  const response = await fetch(`${API_URL}/categories/active`);
  if (!response.ok) throw new Error('Error al obtener categorías activas');
  return response.json(); // ApiResponse<List<CategoryResponse>>
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