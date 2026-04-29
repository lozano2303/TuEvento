const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export const getActivePalette = async () => {
  const token = localStorage.getItem('token');
  if (!token) return null;
  const response = await fetch(`${API_URL}/themes/my-active`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) throw new Error('Error fetching theme');
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
  const token = localStorage.getItem('token');
  const response = await fetch(`${API_URL}/themes/activate/${themeId}`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` }
  });
  if (!response.ok) throw new Error('Error activating theme');
  return await response.json();
};
