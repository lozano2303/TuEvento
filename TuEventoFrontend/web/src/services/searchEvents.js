const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/**
 * Busca eventos publicados por texto y/o fecha exacta.
 * @param {string|null} q    - Texto a buscar en el nombre del evento (null = todos)
 * @param {string|null} date - Fecha en formato YYYY-MM-DD (null = sin filtro de fecha)
 * @returns {{ success: boolean, data: Array, message?: string }}
 */
export const searchEvents = async (q, date) => {
  try {
    const params = new URLSearchParams();

    if (q && q.trim()) params.append('q', q.trim());
    if (date)          params.append('date', date);

    const response = await fetch(
      `${API_URL}/events/public/search?${params.toString()}`
    );

    if (!response.ok) {
      const errorBody = await response.json().catch(() => ({}));
      return {
        success: false,
        message: errorBody?.message || `Error ${response.status} al buscar eventos`,
        data: [],
      };
    }

    const body = await response.json();
    // El backend devuelve ApiResponse<List<EventSummaryResponse>>
    // { status, message, data }
    return {
      success: true,
      data: body.data ?? [],
      message: body.message,
    };
  } catch (err) {
    console.error('searchEvents error:', err);
    return { success: false, message: 'Error de conexión', data: [] };
  }
};
