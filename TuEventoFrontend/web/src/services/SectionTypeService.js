const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/** Lista todos los tipos de sección disponibles — público, sin auth. */
export const getAllSectionTypes = async () => {
  const res = await fetch(`${API_URL}/section-types`);
  if (!res.ok) throw new Error('Error al obtener tipos de sección');
  return res.json(); // ApiResponse<List<{ sectionTypeId, name }>>
};
