const BASE_URL = process.env.EXPO_PUBLIC_API_URL;

export const getActivePalette = async (accessToken) => {
  const response = await fetch(`${BASE_URL}/themes/my-active`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!response.ok) throw new Error("Error fetching theme");
  const json = await response.json();
  return json.data.palette;
};
