const BASE_URL = `${process.env.EXPO_PUBLIC_API_URL}/auth`;

export const authService = {
  async login(email, password) {
    const res = await fetch(`${BASE_URL}/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });
    const json = await res.json();
    if (!json.success) throw new Error(json.message || "Error al iniciar sesión");
    return json.data; // { accessToken, refreshToken, userId, alias }
  },

  async register(fullName, email, password) {
    const res = await fetch(`${BASE_URL}/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ fullName, email, password }),
    });
    const json = await res.json();
    if (!json.success) throw new Error(json.message || "Error al registrarse");
    return json.data; // { userId, alias, email }
  },
};
