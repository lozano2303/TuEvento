const BASE_URL = `${process.env.EXPO_PUBLIC_API_URL}/auth`;

export const authService = {
  async login(email, password) {
    const res = await fetch(`${BASE_URL}/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });
    const json = await res.json();
    if (!json.success) throw new Error(json.message);
    return json.data; // { accessToken, refreshToken, userId, alias }
  },

  async oauthLogin(provider, code) {
    const res = await fetch(`${BASE_URL}/oauth/${provider}/callback?code=${code}`, {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    });
    const json = await res.json();
    if (!json.success) throw new Error(json.message);
    return json.data; // { accessToken, refreshToken, userId, alias }
  },

  async register(fullName, email, password) {
    const res = await fetch(`${BASE_URL}/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ fullName, email, password }),
    });
    const json = await res.json();
    if (!json.success) throw new Error(json.message);
    return json.data; // { userId, alias, email }
  },

  async activateAccount(email, activationCode) {
    const res = await fetch(`${BASE_URL}/activate`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, activationCode }),
    });
    const json = await res.json();
    if (!json.success) throw new Error(json.message);
    return json.data;
  },

  async recoverPassword(email) {
    const res = await fetch(`${BASE_URL}/recover-password`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email }),
    });
    const json = await res.json();
    if (!json.success) throw new Error(json.message);
    return json.data;
  },

  async resetPassword(email, code, newPassword, confirmPassword) {
    const res = await fetch(`${BASE_URL}/reset-password`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, code, newPassword, confirmPassword }),
    });
    const json = await res.json();
    if (!json.success) throw new Error(json.message);
    return json.data;
  },
};
