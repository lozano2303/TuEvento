const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

// Todos los endpoints de geolocation son públicos — sin Authorization header.

export const getDepartments = async () => {
  const res = await fetch(`${API_URL}/geolocation/departments`);
  if (!res.ok) throw new Error('Error al obtener departamentos');
  return res.json(); // ApiResponse<List<DepartmentResponse>>
};

export const getCitiesByDepartment = async (departmentId) => {
  const res = await fetch(`${API_URL}/geolocation/departments/${departmentId}/cities`);
  if (!res.ok) throw new Error('Error al obtener ciudades');
  return res.json(); // ApiResponse<List<CityResponse>>
};

export const getSitesByCity = async (cityId) => {
  const res = await fetch(`${API_URL}/geolocation/cities/${cityId}/sites`);
  if (!res.ok) throw new Error('Error al obtener sedes');
  return res.json(); // ApiResponse<List<SiteResponse>>
};

/** Obtiene una sede por ID — público, sin auth. */
export const getSiteById = async (siteId) => {
  const res = await fetch(`${API_URL}/geolocation/sites/${siteId}`);
  if (!res.ok) throw new Error('Error al obtener la sede');
  return res.json(); // ApiResponse<SiteResponse>
};

// Crear nueva sede — requiere auth (ORGANIZER o ADMIN)
export const createSite = async ({ cityId, name, address, capacity, latitude, longitude }) => {
  const token = localStorage.getItem('token');
  const res = await fetch(`${API_URL}/geolocation/sites`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify({ cityId, name, address, capacity, latitude, longitude }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.message || 'Error al crear la sede');
  return data; // ApiResponse<SiteResponse>
};
