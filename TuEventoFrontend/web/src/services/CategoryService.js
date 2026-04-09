const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

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

export const addCategoryToEvent = async (eventId, categoryId) => {
  const token = localStorage.getItem('token');
  const response = await fetch(`${API_URL}/events/${eventId}/categories`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ categoryId }),
  });
  if (!response.ok) throw new Error('Error al añadir categoría');
  return response.json();
};

export const removeCategoryFromEvent = async (eventId, categoryId) => {
  const token = localStorage.getItem('token');
  const response = await fetch(`${API_URL}/events/${eventId}/categories/${categoryId}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
  if (!response.ok) throw new Error('Error al eliminar categoría');
  return response.json();
};