import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/**
 * Obtiene todas las sillas de una sección de evento.
 * Endpoint público — no requiere autenticación.
 * 
 * @param {number} eventSectionId - ID de la sección del evento
 * @returns {Promise<ApiResponse<SeatResponse[]>>}
 */
export const getSeatsBySection = async (eventSectionId) => {
  const res = await fetch(`${API_URL}/seats/section/${eventSectionId}`);
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: `HTTP ${res.status}` }));
    throw new Error(error.message || 'Error al obtener las sillas');
  }
  return res.json();
};

/**
 * Reserva una silla temporalmente (10 minutos).
 * Requiere autenticación — usa httpRequest con auto-refresh de JWT.
 * 
 * La silla debe estar en estado AVAILABLE.
 * Solo usuarios autenticados pueden reservar.
 * 
 * @param {number} seatId - ID de la silla a reservar
 * @returns {Promise<ApiResponse<SeatResponse>>}
 * @throws {Error} Si la silla no está disponible o hay error de red/auth
 */
export const reserveSeat = async (seatId) => {
  const res = await httpRequest(`${API_URL}/seats/${seatId}/reserve`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });

  const data = await res.json();
  
  if (!res.ok) {
    throw new Error(data.message || 'Error al reservar la silla');
  }

  return data;
};

/**
 * Libera una reserva de silla antes de su expiración.
 * Requiere autenticación — usa httpRequest con auto-refresh de JWT.
 * 
 * Solo el usuario que reservó la silla puede liberarla (excepto ADMIN).
 * 
 * @param {number} seatId - ID de la silla a liberar
 * @returns {Promise<ApiResponse<SeatResponse>>}
 * @throws {Error} Si no tienes permiso o la silla no está reservada
 */
export const releaseSeat = async (seatId) => {
  const res = await httpRequest(`${API_URL}/seats/${seatId}/release`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });

  const data = await res.json();
  
  if (!res.ok) {
    throw new Error(data.message || 'Error al liberar la reserva');
  }

  return data;
};
