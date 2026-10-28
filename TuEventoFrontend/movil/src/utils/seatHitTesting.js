import { flattenPolygonForFill, toElementLocal } from "./layoutUtils";

const SEAT_TOLERANCE = 8; // Píxeles de tolerancia para hacer más fácil tocar con el dedo
const SECTION_TOLERANCE = 6;

/**
 * Encuentra la silla tocada entre las que están dibujadas (la página visible).
 *
 * @param {number} tapX - Coordenada X del tap en pantalla
 * @param {number} tapY - Coordenada Y del tap en pantalla
 * @param {Array<{pos: {x, y, r}, seatId: number}>} visibleSeats - Sillas de la página actual
 * @param {Object} element - Elemento de sección con propiedades {x, y, ...}
 * @param {number} scale - Factor de escala aplicado al canvas
 * @param {number} offsetX - Offset X del canvas (para centrado)
 * @param {number} offsetY - Offset Y del canvas (para centrado)
 * @param {boolean} [rotated=false] - Si la sección se dibuja con su rotación
 * @returns {number | null} seatId de la silla tocada, o null si no se tocó ninguna
 */
export function findSeatAt(tapX, tapY, visibleSeats, element, scale, offsetX, offsetY, rotated = false) {
  const layoutX = (tapX - offsetX) / scale;
  const layoutY = (tapY - offsetY) / scale;
  const local = toElementLocal(layoutX, layoutY, rotated ? element : { ...element, rotation: 0 });
  const tolerance = SEAT_TOLERANCE / scale;

  for (const { pos, seatId } of visibleSeats) {
    const dx = local.x - pos.x;
    const dy = local.y - pos.y;
    if (dx * dx + dy * dy <= (pos.r + tolerance) ** 2) {
      return seatId;
    }
  }
  return null;
}

/** Punto dentro de un polígono (ray casting). */
function pointInPolygon(x, y, points) {
  let inside = false;
  for (let i = 0, j = points.length - 1; i < points.length; j = i++) {
    const xi = points[i].x, yi = points[i].y;
    const xj = points[j].x, yj = points[j].y;
    if ((yi > y) !== (yj > y) && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi) {
      inside = !inside;
    }
  }
  return inside;
}

/**
 * Encuentra la sección tocada en la vista general. Recorre de la última a la primera
 * porque las últimas se dibujan encima.
 *
 * @param {Array} elements - Elementos de sección dibujados, en orden de dibujo
 * @returns {Object | null} El elemento tocado (con su backendSectionId), o null
 */
export function findSectionAt(tapX, tapY, elements, scale, offsetX, offsetY) {
  // Coordenadas del layout correspondientes al tap
  const x = (tapX - offsetX) / scale;
  const y = (tapY - offsetY) / scale;
  const tolerance = SECTION_TOLERANCE / scale;

  for (let i = elements.length - 1; i >= 0; i--) {
    const el = elements[i];
    if (!el.backendSectionId) continue;

    // La vista general dibuja cada sección con su rotación del editor
    const local = toElementLocal(x, y, el);
    if ((el.shapeMode ?? "rect") === "polygon" && el.polygonPoints) {
      const points = flattenPolygonForFill(el.polygonPoints);
      if (points.length >= 3 && pointInPolygon(local.x, local.y, points)) return el;
    } else if (
      local.x >= -tolerance && local.x <= el.width + tolerance &&
      local.y >= -tolerance && local.y <= el.height + tolerance
    ) {
      return el;
    }
  }
  return null;
}
