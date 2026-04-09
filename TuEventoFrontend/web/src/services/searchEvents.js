const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export const searchEvents = async (query, filters = {}) => {
  const params = new URLSearchParams();
  
  if (query) params.append('q', query);
  if (filters.category) params.append('category', filters.category);
  if (filters.date) params.append('date', filters.date);
  if (filters.location) params.append('location', filters.location);
  
  const response = await fetch(`${API_URL}/events/search?${params.toString()}`);
  if (!response.ok) throw new Error('Error al buscar eventos');
  return response.json();
};