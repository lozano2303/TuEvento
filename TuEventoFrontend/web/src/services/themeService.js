import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

// ── GET /themes  — public, no auth needed ─────────────────────────────────────
export const getThemes = async () => {
  const response = await fetch(`${API_URL}/themes`);
  if (!response.ok) throw new Error('Error fetching themes');
  const json = await response.json();
  return json.data;
};

// ── POST /themes/activate/:themeId ────────────────────────────────────────────
export const activateTheme = async (themeId) => {
  const response = await httpRequest(`${API_URL}/themes/activate/${themeId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });
  if (!response.ok) throw new Error('Error activating theme');
  return await response.json();
};

// ── GET /themes/my-active  — returns { themeId, themeName, userThemeId, palette } ─
export const getActivePalette = async () => {
  const response = await httpRequest(`${API_URL}/themes/my-active`, {
    headers: { 'Content-Type': 'application/json' },
  });
  if (!response.ok) throw new Error(`Error fetching theme: ${response.status}`);
  const json = await response.json();
  return json.data;
};

// ── PUT /themes/my-active/customize  — body: { property, value } ──────────────
// Returns the full resolved palette after applying the override.
// value must be a valid hex colour (#RGB, #RRGGBB, #RRGGBBAA) or rgb/rgba().
export const customizeTheme = async (property, value) => {
  const response = await httpRequest(`${API_URL}/themes/my-active/customize`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ property, value }),
  });
  if (!response.ok) throw new Error(`Error customizing theme: ${response.status}`);
  const json = await response.json();
  return json.data; // ResolvedPaletteResponse: { themeId, themeName, userThemeId, palette }
};

// ── DELETE /themes/my-active/customize/:property  — reset one token to base ───
export const resetCustomization = async (property) => {
  const response = await httpRequest(`${API_URL}/themes/my-active/customize/${property}`, {
    method: 'DELETE',
  });
  if (!response.ok) throw new Error(`Error resetting customization: ${response.status}`);
  const json = await response.json();
  return json.data;
};

// ── GET /themes/my-active/log  — returns ThemeLogResponse[] ──────────────────
export const getCustomizationLog = async () => {
  const response = await httpRequest(`${API_URL}/themes/my-active/log`);
  if (!response.ok) throw new Error(`Error fetching log: ${response.status}`);
  const json = await response.json();
  return json.data;
};
