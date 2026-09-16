import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

const normalizeUploadError = (message) => {
  const normalized = message || '';

  // El backend envía "Tu imagen no cumple con nuestras políticas de contenido."
  // para cualquier violación NSFW/gore (ImagePolicyViolationException).
  // Cualquier variante que contenga "políticas de contenido" o palabras clave
  // de contenido inapropiado se pasa tal cual al usuario — ya viene redactado
  // de forma clara desde el backend. No lo sobreescribimos con un mensaje
  // diferente para evitar inconsistencias.
  const policyKeywords = /pol.ticas de contenido|contenido adulto|pornograf|porno|hentai|violencia|gore|armas|contenido sexual|NSFW|VIOLENT/i;

  if (policyKeywords.test(normalized)) return normalized;
  return normalized || 'Error al subir foto de perfil';
};

export const getProfileByUserId = async (userId) => {
  const response = await httpRequest(`${API_URL}/profiles/user/${userId}`, {
    headers: {
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
  const response = await httpRequest(`${API_URL}/profiles`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(profileData),
  });
  if (!response.ok) throw new Error('Error al crear perfil');
  return response.json();
};

export const updateProfile = async (profileId, profileData) => {
  const response = await httpRequest(`${API_URL}/profiles/${profileId}`, {
    method: 'PUT',
    headers: {
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
  const formData = new FormData();
  formData.append('file', file, file.name);
  formData.append('categoryCode', 'PROFILE_PICTURE');

  const response = await httpRequest(`${API_URL}/storage/upload`, {
    method: 'POST',
    body: formData,
  });

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) throw new Error(normalizeUploadError(data?.message));
  return data?.data;
};

export const getProfilePictureUrl = async (storedFileId) => {
  // Use /url endpoint to get a fresh presigned URL (60-min expiry) instead of
  // the stale static URL stored in the DB, which points to the internal MinIO
  // host and is not accessible from the browser.
  const response = await httpRequest(`${API_URL}/storage/${storedFileId}/url`, {
    headers: {
      'Content-Type': 'application/json',
    },
  });
  const data = await response.json();
  if (!response.ok) throw new Error(data?.message || 'Error al obtener foto de perfil');
  return data?.data?.publicUrl;
};

// Traduce los mensajes de error del backend (en inglés) a español para el
// flujo de desactivación de cuenta. El backend lanza BusinessException con
// mensajes en inglés porque el dominio no tiene i18n; la traducción la hace
// esta capa de servicio, que es el punto natural de adaptación UI↔backend.
const normalizeDeactivateError = (message) => {
  const m = (message || '').toLowerCase();
  if (m.includes('password is incorrect') || m.includes('invalid_password') || m.includes('wrong password')) {
    return 'Contraseña incorrecta. Verifica e inténtalo de nuevo.';
  }
  if (m.includes('already inactive') || m.includes('account_already_inactive')) {
    return 'Esta cuenta ya está desactivada.';
  }
  if (m.includes('account not found') || m.includes('account_deleted') || m.includes('user not found')) {
    return 'No se encontró la cuenta. Contacta soporte.';
  }
  return message || 'No se pudo desactivar la cuenta. Intenta de nuevo.';
};

export const deactivateAccount = async (password) => {
  const response = await httpRequest(`${API_URL}/users/me`, {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ password }),
  });
  const data = await response.json();
  if (!response.ok) throw new Error(normalizeDeactivateError(data?.message));
  return data;
};
