const BASE_URL = process.env.EXPO_PUBLIC_API_URL;

export const getActivePalette = async (accessToken) => {
  const response = await fetch(`${BASE_URL}/themes/my-active`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!response.ok) throw new Error("Error fetching theme");
  const json = await response.json();
  return json.data.palette;
};

export const getThemes = async () => {
  const response = await fetch(`${BASE_URL}/themes`);
  if (!response.ok) throw new Error('Error fetching themes');
  const json = await response.json();
  return json.data;
};

export const activateTheme = async (themeId, accessToken) => {
  const response = await fetch(`${BASE_URL}/themes/activate/${themeId}`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${accessToken}` }
  });
  if (!response.ok) throw new Error('Error activating theme');
  return await response.json();
};
