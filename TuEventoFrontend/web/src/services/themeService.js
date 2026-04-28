const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export const getActivePalette = async () => {
  const token = localStorage.getItem('token');
  if (!token) return null;
  const response = await fetch(`${API_URL}/themes/my-active`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) throw new Error('Error fetching theme');
  const json = await response.json();
  return json.data.palette;
};
