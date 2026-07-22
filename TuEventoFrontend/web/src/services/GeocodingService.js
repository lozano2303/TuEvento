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
 * Construye una dirección corta y legible a partir del objeto address de Nominatim.
 * Resultado típico en Colombia: "Calle 1B, Las Acacias" (house_number casi nunca
 * aparece en datos OSM colombianos — es una limitación del catastro, no del código).
 * Devuelve null si no hay suficiente información estructurada.
 */
function buildShortAddress(address) {
  if (!address) return null;

  const road         = address.road ?? address.pedestrian ?? address.footway ?? '';
  const houseNumber  = address.house_number ?? '';
  const neighbourhood = address.suburb ?? address.neighbourhood ?? address.quarter ?? '';

  let street = road;
  if (houseNumber) street += ` # ${houseNumber}`;

  const parts = [street.trim(), neighbourhood].filter(Boolean);
  if (parts.length === 0) return null;

  // Truncar a 200 chars para respetar @Size(max=200) del backend
  return parts.join(', ').slice(0, 200);
}

/**
 * Geocodificación inversa: coordenadas → dirección corta.
 * Pide addressdetails=1 para obtener datos estructurados y construir una
 * dirección compacta en vez del display_name crudo de 100+ caracteres.
 * Devuelve string o null si Nominatim no tiene datos suficientes en ese punto.
 */
export const reverseGeocode = async (lat, lng) => {
  try {
    const url    = `${NOMINATIM_URL}/reverse?lat=${lat}&lon=${lng}&format=json&addressdetails=1`;
    const res    = await fetch(url);
    if (!res.ok) return null;
    const result = await res.json();
    return buildShortAddress(result.address);
  } catch {
    return null;
  }
};
