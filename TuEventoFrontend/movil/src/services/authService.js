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
    // Pass response_format=json so the backend returns a JSON payload instead
    // of the browser 302 redirect it issues for web clients.  The mobile app
    // handles its own navigation after receiving the LoginResponse.
    const res = await fetch(
      `${BASE_URL}/oauth/${provider}/callback?code=${encodeURIComponent(code)}&response_format=json`,
      {
        method: "GET",
        headers: { "Content-Type": "application/json" },
      }
    );
    const json = await res.json();
    if (!res.ok || !json.success) throw new Error(json.message || "OAuth login failed");
    // json.data shape: { accessToken, refreshToken, userId, alias, role, needsOnboarding, profileId }
    return json.data;
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
