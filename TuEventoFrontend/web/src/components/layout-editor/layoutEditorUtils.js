// ── UUID ──────────────────────────────────────────────────────────────────────
export const generateId = () =>
  `elem-${Math.random().toString(36).slice(2, 9)}-${Date.now().toString(36)}`;

// ── Snapping ─────────────────────────────────────────────────────────────────
export const snapToGrid = (value, gridSize = 10) =>
  Math.round(value / gridSize) * gridSize;

// ── Serialización ─────────────────────────────────────────────────────────────
/**
 * Convierte el estado del editor al objeto layoutData que se enviará al backend.
 * Solo incluye los campos que el backend espera — descarta helpers internos.
 */
export const serializeLayout = (elements, canvasWidth = 1200, canvasHeight = 800) => ({
  canvasWidth,
  canvasHeight,
  elements: elements.map((el) => ({
    id: el.id,
    type: el.type,
    ...(el.type === 'section' && {
      sectionType: el.sectionType,
      eventSectionId: null,       // siempre null en Fase 1
      seatLayout: el.seatLayout,
    }),
    x: el.x,
    y: el.y,
    width: el.width,
    height: el.height,
    rotation: el.rotation ?? 0,
    label: el.label,
    color: el.color,
  })),
});

// ── Cálculo de grilla de sillas ───────────────────────────────────────────────
/**
 * Devuelve un array de posiciones {x, y} para cada silla dentro de un rect.
 * Si el espacio por silla < minCellPx, retorna [] para que el componente
 * muestre el badge en lugar de la grilla.
 */
export const computeSeatPositions = (
  rectWidth,
  rectHeight,
  seatLayout,
  padding = 12
) => {
  if (!seatLayout) return [];
  const { rows, cols, seatRadius, gap } = seatLayout;

  const availW = rectWidth - padding * 2;
  const availH = rectHeight - padding * 2;
  const cellW = availW / cols;
  const cellH = availH / rows;

  // Si el espacio es tan pequeño que las sillas se solapan, aun así dibujamos
  // (el recálculo en PropertiesPanel evita que llegue a ese estado)
  const positions = [];
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      positions.push({
        x: padding + c * cellW + cellW / 2,
        y: padding + r * cellH + cellH / 2,
        r: Math.max(2, Math.min(seatRadius, (Math.min(cellW, cellH) - gap) / 2)),
      });
    }
  }
  return positions;
};

// ── Tamaño mínimo proporcional a la grilla de sillas (Fix 4) ─────────────────
/**
 * Calcula el width/height mínimo para que la grilla de sillas sea legible.
 * Fórmula: dimension = cells * (seatRadius * 2 + gap) + padding * 2 + labelSpace
 */
export const computeSectionSize = (seatLayout, labelSpace = 28, padding = 12) => {
  if (!seatLayout) return null;
  const { rows, cols, seatRadius = 7, gap = 4 } = seatLayout;
  const cellSize = seatRadius * 2 + gap;
  return {
    width: Math.round(cols * cellSize + padding * 2),
    height: Math.round(rows * cellSize + padding * 2 + labelSpace),
  };
};

// ── Intersección para rubber-band selection ───────────────────────────────────
export const rectsIntersect = (a, b) => {
  const ax2 = a.x + a.width;
  const ay2 = a.y + a.height;
  const bx2 = b.x + b.width;
  const by2 = b.y + b.height;
  return a.x < bx2 && ax2 > b.x && a.y < by2 && ay2 > b.y;
};

// ── Fix 6: tamaño mínimo de sección para Transformer ─────────────────────────
/**
 * Calcula el tamaño mínimo absoluto de una sección con seatLayout,
 * usando los valores mínimos de seatRadius y gap que aún producen
 * sillas reconocibles como círculos.
 */
export const computeMinSectionSize = (seatLayout, padding = 12, labelSpace = 28) => {
  if (!seatLayout) return { width: 60, height: 40 };
  const MIN_RADIUS = 4;
  const MIN_GAP    = 2;
  const { rows, cols } = seatLayout;
  const cellSize = MIN_RADIUS * 2 + MIN_GAP;
  return {
    width:  Math.max(60, Math.round(cols * cellSize + padding * 2)),
    height: Math.max(40, Math.round(rows * cellSize + padding * 2 + labelSpace)),
  };
};

// ── Fix 7: recalcular canvas a partir del bounding box de los elementos ───────
const CANVAS_MIN_W   = 1200;
const CANVAS_MIN_H   = 800;
const CANVAS_MARGIN  = 150;   // respiro alrededor del contenido
const SHRINK_THRESHOLD = 150; // solo contraer si hay más de esto de espacio vacío

/**
 * Dado el array de elementos y el canvasSize actual, devuelve el nuevo
 * canvasSize ajustado al contenido real (más margen), con un mínimo absoluto.
 * También devuelve offsetDelta si hay que reubicar elementos para que ninguno
 * quede con coordenadas negativas.
 */
export const computeCanvasForElements = (elements, currentCanvas) => {
  if (elements.length === 0) {
    return {
      newCanvasSize: { width: CANVAS_MIN_W, height: CANVAS_MIN_H },
      offsetDelta:   { x: 0, y: 0 },
    };
  }

  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
  for (const el of elements) {
    minX = Math.min(minX, el.x);
    minY = Math.min(minY, el.y);
    maxX = Math.max(maxX, el.x + el.width);
    maxY = Math.max(maxY, el.y + el.height);
  }

  // Si algún elemento tiene coordenada negativa, calculamos offset para
  // traerlos a positivo (con margen)
  const offsetX = minX < 0 ? Math.abs(minX) + CANVAS_MARGIN : 0;
  const offsetY = minY < 0 ? Math.abs(minY) + CANVAS_MARGIN : 0;

  const contentW = (maxX + offsetX) + CANVAS_MARGIN;
  const contentH = (maxY + offsetY) + CANVAS_MARGIN;

  const newW = Math.max(CANVAS_MIN_W, Math.ceil(contentW));
  const newH = Math.max(CANVAS_MIN_H, Math.ceil(contentH));

  // Solo contraer si la diferencia es mayor al umbral (evita micro-cambios constantes)
  const finalW = (currentCanvas.width - newW > SHRINK_THRESHOLD || newW > currentCanvas.width)
    ? newW
    : currentCanvas.width;
  const finalH = (currentCanvas.height - newH > SHRINK_THRESHOLD || newH > currentCanvas.height)
    ? newH
    : currentCanvas.height;

  return {
    newCanvasSize: { width: finalW, height: finalH },
    offsetDelta:   { x: offsetX, y: offsetY },
  };
};
