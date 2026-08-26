import AsyncStorage from "@react-native-async-storage/async-storage";

const BASE_URL = process.env.EXPO_PUBLIC_API_URL;

/**
 * Obtiene todas las sillas de una sección de evento.
 * Endpoint público - no requiere autenticación.
 * 
 * @param {number} eventSectionId - ID de la sección del evento
 * @returns {Promise<Array>} Array de SeatResponse
 */
export const getSeatsBySection = async (eventSectionId) => {
  const token = await AsyncStorage.getItem("accessToken");
  
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}/seats/section/${eventSectionId}`, { headers });
  
  if (!response.ok) {
    throw new Error(`Error fetching seats: ${response.status}`);
  }
  
  const json = await response.json();
  return json.data; // Array de SeatResponse
};

/**
 * Reserva una silla temporalmente (10 minutos).
 * Requiere autenticación.
 * 
 * La silla debe estar en estado AVAILABLE.
 * Solo usuarios autenticados pueden reservar.
 * 
 * @param {number} seatId - ID de la silla a reservar
 * @returns {Promise<Object>} SeatResponse con reservedBy y reservedUntil poblados
 * @throws {Error} Si la silla no está disponible o hay error de red/auth
 */
export const reserveSeat = async (seatId) => {
  const token = await AsyncStorage.getItem("accessToken");
  
  if (!token) {
    throw new Error("Debes iniciar sesión para reservar sillas");
  }

  const response = await fetch(`${BASE_URL}/seats/${seatId}/reserve`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${token}`,
      "Content-Type": "application/json",
    },
  });
  
  const json = await response.json();
  
  if (!response.ok) {
    // Manejar errores específicos del backend
    if (response.status === 404) {
      throw new Error("La silla no existe");
    }
    if (response.status === 400 && json.message?.includes("SEAT_NOT_AVAILABLE")) {
      throw new Error("La silla no está disponible");
    }
    throw new Error(json.message || "Error al reservar la silla");
  }
  
  return json.data;
};

/**
 * Libera una reserva de silla antes de su expiración.
 * Requiere autenticación.
 * 
 * Solo el usuario que reservó la silla puede liberarla (excepto ADMIN).
 * 
 * @param {number} seatId - ID de la silla a liberar
 * @returns {Promise<Object>} SeatResponse con reservedBy y reservedUntil en null
 * @throws {Error} Si no tienes permiso o la silla no está reservada
 */
export const releaseSeat = async (seatId) => {
  const token = await AsyncStorage.getItem("accessToken");
  
  if (!token) {
    throw new Error("Debes iniciar sesión para liberar reservas");
  }

  const response = await fetch(`${BASE_URL}/seats/${seatId}/release`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${token}`,
      "Content-Type": "application/json",
    },
  });
  
  const json = await response.json();
  
  if (!response.ok) {
    // Manejar errores específicos del backend
    if (response.status === 404) {
      throw new Error("La silla no existe");
    }
    if (response.status === 400 && json.message?.includes("SEAT_NOT_RESERVED")) {
      throw new Error("La silla no está reservada");
    }
    if (response.status === 403) {
      throw new Error("No puedes liberar la reserva de otro usuario");
    }
    throw new Error(json.message || "Error al liberar la reserva");
  }
  
  return json.data;
};
