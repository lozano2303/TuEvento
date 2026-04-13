const BASE_URL = `${process.env.EXPO_PUBLIC_API_URL}/profiles`;

export const profileService = {
  async getByUserId(userId, accessToken) {
    const res = await fetch(`${BASE_URL}/user/${userId}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${accessToken}`,
      },
    });
    const json = await res.json();
    if (!json.success) throw new Error(json.message || "Error al obtener perfil");
    return json.data; // { profileId, userId, fullName, bio, ... }
  },
};
