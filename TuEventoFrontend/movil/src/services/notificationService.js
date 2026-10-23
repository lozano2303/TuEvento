import AsyncStorage from "@react-native-async-storage/async-storage";

const BASE_URL = process.env.EXPO_PUBLIC_API_URL;

async function authHeaders() {
  const token = await AsyncStorage.getItem("accessToken");
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

/**
 * Obtiene las notificaciones del usuario autenticado.
 * GET /api/v1/notifications/me?page=0&size=20&unreadOnly=false
 * @returns {Promise<Object>} { data: { content: [], ... } }
 */
export const getUserNotifications = async ({ unreadOnly = false, page = 0, size = 20 } = {}) => {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
    unreadOnly: unreadOnly.toString(),
  });
  const response = await fetch(`${BASE_URL}/notifications/me?${params}`, {
    headers: await authHeaders(),
  });
  const json = await response.json();
  if (!response.ok) throw new Error(json.message || "Error al cargar notificaciones");
  return json;
};

/**
 * Marca una notificación como leída.
 * PATCH /api/v1/notifications/{notificationUserId}/read
 */
export const markAsRead = async (notificationUserId) => {
  const response = await fetch(`${BASE_URL}/notifications/${notificationUserId}/read`, {
    method: "PATCH",
    headers: await authHeaders(),
  });
  const json = await response.json();
  if (!response.ok) throw new Error(json.message || "Error al marcar como leída");
  return json;
};

/**
 * Marca todas las notificaciones del usuario como leídas.
 * PATCH /api/v1/notifications/me/read-all
 */
export const markAllAsRead = async () => {
  const response = await fetch(`${BASE_URL}/notifications/me/read-all`, {
    method: "PATCH",
    headers: await authHeaders(),
  });
  const json = await response.json();
  if (!response.ok) throw new Error(json.message || "Error al marcar todas como leídas");
  return json;
};
