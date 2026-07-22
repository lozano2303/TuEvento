/**
 * Servicio de geocodificación usando Nominatim (OpenStreetMap).
 *
 * Política de uso: máximo 1 request/segundo. Para proyectos en producción
 * con tráfico real se recomienda usar un proxy propio o un proveedor con
 * API key (Mapbox, HERE, etc.) para evitar bloqueos por abuso.
 * En contexto académico el uso esporádico desde el navegador es aceptable;
 * el Referer que el navegador envía automáticamente sirve como identificación.
 */

const NOMINATIM_URL = 'https://nominatim.openstreetmap.org';

/**
 * Geocodifica una ciudad para obtener sus coordenadas y bbox.
 * Devuelve { lat, lng, boundingbox } o null si no se encontró.
 * boundingbox: [south, north, west, east] como strings.
 */
export const geocodeCity = async (cityName, departmentName) => {
  try {
    const query = `${cityName}, ${departmentName}, Colombia`;
    const url   = `${NOMINATIM_URL}/search?q=${encodeURIComponent(query)}&format=json&limit=1`;
    const res   = await fetch(url);
    if (!res.ok) return null;
    const results = await res.json();
    if (!results.length) return null;
    const r = results[0];
    return {
      lat:         parseFloat(r.lat),
      lng:         parseFloat(r.lon),
      boundingbox: r.boundingbox,   // [south, north, west, east]
    };
  } catch {
    return null;
  }
};

/**
 * Busca una dirección escrita y la sesga a la bbox de la ciudad si se provee.
 * Devuelve { lat, lng, displayName } o null si no se encontró.
 */
export const searchAddress = async (query, cityBoundingbox) => {
  let url = `${NOMINATIM_URL}/search?q=${encodeURIComponent(query)}&format=json&limit=1`;
  if (cityBoundingbox) {
    const [south, north, west, east] = cityBoundingbox;
    url += `&viewbox=${west},${north},${east},${south}&bounded=1`;
  }
  const res = await fetch(url);
  if (!res.ok) throw new Error('Error al buscar la dirección');
  const results = await res.json();
  if (!results.length) return null;
  const r = results[0];
  return { lat: parseFloat(r.lat), lng: parseFloat(r.lon), displayName: r.display_name };
};

/**
 * Geocodificación inversa: coordenadas → dirección legible.
 * Devuelve el display_name como string o null si falla.
 */
export const reverseGeocode = async (lat, lng) => {
  try {
    const url    = `${NOMINATIM_URL}/reverse?lat=${lat}&lon=${lng}&format=json`;
    const res    = await fetch(url);
    if (!res.ok) return null;
    const result = await res.json();
    return result.display_name ?? null;
  } catch {
    return null;
  }
};
