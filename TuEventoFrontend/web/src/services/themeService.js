import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export const getActivePalette = async () => {
  const response = await httpRequest(`${API_URL}/themes/my-active`, {
    headers: { 'Content-Type': 'application/json' },
  });
  if (!response.ok) throw new Error(`Error fetching theme: ${response.status}`);
  const json = await response.json();
  return json.data; // retorna { themeId, themeName, userThemeId, palette }
};

export const getThemes = async () => {
  const response = await fetch(`${API_URL}/themes`);
  if (!response.ok) throw new Error('Error fetching themes');
  const json = await response.json();
  return json.data;
};

export const activateTheme = async (themeId) => {
  const response = await httpRequest(`${API_URL}/themes/activate/${themeId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  });
  if (!response.ok) throw new Error('Error activating theme');
  return await response.json();
};
