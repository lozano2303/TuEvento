const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/**
 * Obtiene el layout visual de un evento.
 * Devuelve null si el evento aún no tiene layout guardado (404).
 * ApiResponse<{ eventLayoutId, eventId, layoutData: string (JSON) }>
 */
export const getLayout = async (eventId) => {
  const res = await fetch(`${API_URL}/events/${eventId}/layout`);
  if (res.status === 404) return null;
  if (!res.ok) {
    // Logear el status real para diagnóstico — si ves 403 es auth, 500 es bug del controller
    console.error(`[LayoutService.getLayout] status ${res.status} para eventId=${eventId}`);
    throw new Error(`Error al obtener el layout (HTTP ${res.status})`);
  }
  return res.json();
};

/**
 * Crea o actualiza el layout visual de un evento — requiere ORGANIZER.
 * layoutDataObject se serializa a string JSON antes de enviar
 * porque el backend espera { layoutData: string }.
 */
export const saveLayout = async (eventId, layoutDataObject) => {
  const token = localStorage.getItem('token');
  const res = await fetch(`${API_URL}/events/${eventId}/layout`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
    body: JSON.stringify({ layoutData: JSON.stringify(layoutDataObject) }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.message || 'Error al guardar el layout');
  return data; // ApiResponse<EventLayoutResponse>
};
