const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export const getProfileByUserId = async (userId) => {
  const token = localStorage.getItem('token');
  const response = await fetch(`${API_URL}/profiles/user/${userId}`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });
  if (!response.ok) throw new Error('Error al obtener perfil');
  return response.json();
};

export const createProfile = async (profileData) => {
  const token = localStorage.getItem('token');
  const response = await fetch(`${API_URL}/profiles`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(profileData),
  });
  if (!response.ok) throw new Error('Error al crear perfil');
  return response.json();
};

export const updateProfile = async (profileId, profileData) => {
  const token = localStorage.getItem('token');
  const response = await fetch(`${API_URL}/profiles/${profileId}`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(profileData),
  });
  if (!response.ok) throw new Error('Error al actualizar perfil');
  return response.json();
};