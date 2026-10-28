import { httpRequest } from "./httpClient";

const BASE_URL = `${process.env.EXPO_PUBLIC_API_URL}/profiles`;

// El parámetro accessToken se conserva por compatibilidad: httpRequest usa siempre el token
// guardado y lo renueva si expiró.
export const profileService = {
  async getByUserId(userId, _accessToken, languageCode = null) {
    const langQ = languageCode && languageCode !== "es" ? `?language=${languageCode}` : "";
    const res = await httpRequest(`${BASE_URL}/user/${userId}${langQ}`, {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    });
    const json = await res.json();
    if (!json.success) throw new Error(json.message || "Error al obtener perfil");
    return json.data; // { profileId, userId, fullName, bio, storedFileId, preferredLanguageId, translatedBio, ... }
  },

  async updateProfile(profileId, body, _accessToken) {
    const res = await httpRequest(`${BASE_URL}/${profileId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    const json = await res.json();
    if (!json.success) throw new Error(json.message || "Error al actualizar perfil");
    return json.data;
  },

  // Desactiva la cuenta del usuario autenticado.
  // DELETE /users/me con { password } en el body.
  async deactivateAccount(password, _accessToken) {
    const url = `${process.env.EXPO_PUBLIC_API_URL}/users/me`;
    const res = await httpRequest(url, {
      method: "DELETE",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ password }),
    });
    const json = await res.json().catch(() => ({}));
    if (!res.ok || json.success === false) {
      throw new Error(json.message || "No se pudo desactivar la cuenta.");
    }
    return json;
  },
};
