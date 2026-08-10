const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

const CONTENT_POLICY_MESSAGE = 'No se permiten imágenes con contenido adulto, pornografía, hentai, violencia, armas, gore o contenido sexual.';

const normalizeUploadError = (message) => {
  const normalized = message || '';
  const policyKeywords = /contenido adulto|pornograf|porno|hentai|violencia|gore|armas|contenido sexual|pol.ticas de contenido|NSFW|VIOLENT/i;

  if (policyKeywords.test(normalized)) return CONTENT_POLICY_MESSAGE;
  return normalized || 'Error al subir foto de perfil';
};

export const getProfileByUserId = async (userId) => {
  const token = localStorage.getItem('token');
  const response = await fetch(`${API_URL}/profiles/user/${userId}`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });
  const data = await response.json();
  if (!response.ok) throw new Error('Error al obtener perfil');
  // Guardar fullName en localStorage para uso posterior
  if (data?.data?.fullName) {
    localStorage.setItem('name', data.data.fullName);
  }
  return data;
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
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) throw new Error(data?.message || 'Error al actualizar perfil');
  return data?.data;
};

export const uploadProfilePicture = async (file) => {
  const token = localStorage.getItem('token');
  const formData = new FormData();
  formData.append('file', file, file.name);
  formData.append('categoryCode', 'PROFILE_PICTURE');

  const response = await fetch(`${API_URL}/storage/upload`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
    body: formData,
  });

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) throw new Error(normalizeUploadError(data?.message));
  return data?.data;
};

export const getProfilePictureUrl = async (storedFileId) => {
  const token = localStorage.getItem('token');
  // Use /url endpoint to get a fresh presigned URL (60-min expiry) instead of
  // the stale static URL stored in the DB, which points to the internal MinIO
  // host and is not accessible from the browser.
  const response = await fetch(`${API_URL}/storage/${storedFileId}/url`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });
  const data = await response.json();
  if (!response.ok) throw new Error(data?.message || 'Error al obtener foto de perfil');
  return data?.data?.publicUrl;
};