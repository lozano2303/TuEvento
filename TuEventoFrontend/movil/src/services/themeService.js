import AsyncStorage from "@react-native-async-storage/async-storage";

const BASE_URL = process.env.EXPO_PUBLIC_API_URL;

export const getActivePalette = async () => {
  const token = await AsyncStorage.getItem("accessToken");
  if (!token) return null;
  const response = await fetch(`${BASE_URL}/themes/my-active`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) throw new Error(`Error fetching active theme: ${response.status}`);
  const json = await response.json();
  return json.data; // { themeId, themeName, userThemeId, palette }
};

export const getThemes = async () => {
  const response = await fetch(`${BASE_URL}/themes`);
  if (!response.ok) throw new Error(`Error fetching themes: ${response.status}`);
  const json = await response.json();
  return json.data;
};

export const activateTheme = async (themeId) => {
  const token = await AsyncStorage.getItem("accessToken");

  const response = await fetch(`${BASE_URL}/themes/activate/${themeId}`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!response.ok) throw new Error(`Error activating theme: ${response.status}`);
  return await response.json();
};

export const customizeTheme = async (property, value, accessToken) => {
  const response = await fetch(`${BASE_URL}/themes/my-active/customize`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ property, value }),
  });
  if (!response.ok) throw new Error(`Error customizing theme: ${response.status}`);
  const json = await response.json();
  return json.data; // paleta resuelta completa
};

export const resetCustomization = async (property, accessToken) => {
  const response = await fetch(`${BASE_URL}/themes/my-active/customize/${property}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!response.ok) throw new Error(`Error resetting customization: ${response.status}`);
  const json = await response.json();
  return json.data;
};

export const getCustomizationLog = async (accessToken) => {
  const response = await fetch(`${BASE_URL}/themes/my-active/log`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!response.ok) throw new Error(`Error fetching log: ${response.status}`);
  const json = await response.json();
  return json.data;
};
