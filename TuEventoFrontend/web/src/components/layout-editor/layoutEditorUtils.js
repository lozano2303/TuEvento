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
      sectionType:    el.sectionType,
      eventSectionId: null,
      seatLayout:     el.seatLayout,
      shapeMode:      el.shapeMode ?? 'rect',
      polygonPoints:  el.polygonPoints ?? null,
    }),
    x: el.x,
    y: el.y,
    width:    el.width,
    height:   el.height,
    rotation: el.rotation ?? 0,
    label:    el.label,
    color:    el.color,
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

// ── Tamaño mínimo proporcional a la grilla de sillas (Fix 4 / Fase 1.5) ──────
/**
 * Fase 1.5: ya no depende de rows/cols. Devuelve un tamaño mínimo fijo
 * para que la sección siempre tenga espacio legible.
 */
export const computeMinSectionSize = (_seatLayout, _padding, _labelSpace) => {
  return { width: 80, height: 60 };
};

// ── Fase 1.5: migrar seatLayout legacy (rows/cols → targetSeats) ─────────────
/**
 * Si el seatLayout tiene rows/cols (formato antiguo), convierte a targetSeats.
 * Siempre retorna un objeto con targetSeats, seatRadius, gap — sin rows/cols.
 */
export const normalizeSeatLayout = (sl) => {
  if (!sl) return null;
  if (sl.targetSeats !== undefined) return sl;
  // Migración automática
  return {
    targetSeats: (sl.rows ?? 1) * (sl.cols ?? 1),
    seatRadius:  sl.seatRadius ?? 7,
    gap:         sl.gap        ?? 4,
  };
};

// ── Fase 1.5: capacidad máxima según forma y tamaño ──────────────────────────
/**
 * Cuántas sillas caben físicamente en una sección dada su forma y tamaño.
 * Para rect: deriva rows/cols del tamaño disponible.
 * Para polygon: usa computePolygonSeatRows con una densidad máxima.
 */
export const computeMaxSeats = (element, seatRadius, gap) => {
  const r = seatRadius ?? 7;
  const g = gap        ?? 4;
  const shapeMode = element.shapeMode ?? 'rect';
  const cellSize  = r * 2 + g;
  const LABEL_H   = 20;
  const PADDING   = 12;

  if (shapeMode === 'polygon' && element.polygonPoints) {
    // Llenado máximo del polígono con densidad alta (999 filas = sin límite de filas)
    const positions = computePolygonSeatRows(
      element.polygonPoints,
      { rows: 999, cols: 9999, seatRadius: r, gap: g }
    );
    return positions.length;
  }

  // Rect
  const cols = Math.floor((element.width  - PADDING * 2 + g) / cellSize);
  const rows = Math.floor((element.height - PADDING * 2 - LABEL_H + g) / cellSize);
  return Math.max(0, cols * rows);
};

// ── Fase 1.5: distribuir sillas a partir de targetSeats ───────────────────────
/**
 * Calcula las posiciones de las sillas para una sección, dado targetSeats.
 * - Si targetSeats > maxSeats: dibuja solo maxSeats (las que caben).
 * - Devuelve array de { x, y, r }.
 */
export const distributeSeats = (element) => {
  const sl = normalizeSeatLayout(element.seatLayout);
  if (!sl) return [];

  const { targetSeats, seatRadius, gap } = sl;
  const shapeMode = element.shapeMode ?? 'rect';
  const PADDING   = 12;
  const LABEL_H   = 20;
  const cellSize  = seatRadius * 2 + gap;

  if (shapeMode === 'polygon' && element.polygonPoints) {
    // Generar todas las posiciones posibles y tomar las primeras targetSeats
    const all = computePolygonSeatRows(
      element.polygonPoints,
      { rows: 999, cols: 9999, seatRadius, gap }
    );
    return all.slice(0, targetSeats);
  }

  // Rect: derivar rows/cols óptimos para targetSeats dadas las dimensiones
  const availW     = element.width  - PADDING * 2;
  const availH     = element.height - PADDING * 2 - LABEL_H;
  const maxCols    = Math.max(1, Math.floor((availW + gap) / cellSize));
  const maxRows    = Math.max(1, Math.floor((availH + gap) / cellSize));
  const maxSeats   = maxCols * maxRows;
  const actual     = Math.min(targetSeats, maxSeats);

  if (actual <= 0) return [];

  // Calcular cols óptimas para la relación de aspecto
  const aspectRatio = availW / Math.max(1, availH);
  let cols = Math.min(maxCols, Math.max(1, Math.round(Math.sqrt(actual * aspectRatio))));
  let rows = Math.min(maxRows, Math.ceil(actual / cols));
  // Ajustar si rows*cols > maxSeats
  while (rows * cols > maxSeats && rows > 1) rows--;
  while (rows * cols > maxSeats && cols > 1) cols--;

  const cellW = availW / cols;
  const cellH = availH / rows;
  const positions = [];
  let count = 0;

  outer:
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      if (count >= actual) break outer;
      positions.push({
        x: PADDING + c * cellW + cellW / 2,
        y: PADDING + LABEL_H / 2 + r * cellH + cellH / 2,
        r: Math.max(2, Math.min(seatRadius, (Math.min(cellW, cellH) - gap) / 2)),
      });
      count++;
    }
  }
  return positions;
};

// ── Intersección para rubber-band selection ───────────────────────────────────
export const rectsIntersect = (a, b) => {
  const ax2 = a.x + a.width;
  const ay2 = a.y + a.height;
  const bx2 = b.x + b.width;
  const by2 = b.y + b.height;
  return a.x < bx2 && ax2 > b.x && a.y < by2 && ay2 > b.y;
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

// ── Fase 1.3: utilidades para polígonos ──────────────────────────────────────

/**
 * Calcula el bounding box de un array de vértices [[x,y], ...].
 * Devuelve { minX, minY, maxX, maxY, width, height }.
 */
export const polyBoundingBox = (points) => {
  if (!points || points.length === 0) return { minX: 0, minY: 0, maxX: 0, maxY: 0, width: 0, height: 0 };
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
  for (const [x, y] of points) {
    if (x < minX) minX = x;
    if (y < minY) minY = y;
    if (x > maxX) maxX = x;
    if (y > maxY) maxY = y;
  }
  return { minX, minY, maxX, maxY, width: maxX - minX, height: maxY - minY };
};

/**
 * Fix C: algoritmo even-odd correcto para polígonos cóncavos.
 * Para cada fila, encuentra TODOS los segmentos de intersección (pares de Xs)
 * y coloca sillas dentro de cada segmento válido — no solo el más ancho.
 */
export const computePolygonSeatRows = (polygonPoints, seatLayout) => {
  if (!polygonPoints || polygonPoints.length < 3 || !seatLayout) return [];
  const { rows, cols, seatRadius, gap } = seatLayout;
  const bb  = polyBoundingBox(polygonPoints);
  const pad = 6;
  const minY = bb.minY + pad;
  const maxY = bb.maxY - pad;
  if (maxY <= minY) return [];

  const rowH     = (maxY - minY) / rows;
  const seatDiam = seatRadius * 2 + gap;
  const positions = [];

  for (let r = 0; r < rows; r++) {
    const y  = minY + (r + 0.5) * rowH;
    const xs = polyHorizontalIntersections(polygonPoints, y);
    if (xs.length < 2) continue;

    xs.sort((a, b) => a - b);

    // Even-odd: pares de intersecciones forman segmentos rellenos
    for (let s = 0; s + 1 < xs.length; s += 2) {
      const xLeft  = xs[s]     + pad;
      const xRight = xs[s + 1] - pad;
      const availW = xRight - xLeft;
      if (availW < seatDiam) continue;

      // Cuántos asientos caben en este segmento (sin exceder cols)
      const count = Math.min(cols, Math.floor((availW + gap) / seatDiam));
      if (count <= 0) continue;

      const totalW = count * seatDiam - gap;
      const startX = xLeft + (availW - totalW) / 2;
      for (let c = 0; c < count; c++) {
        positions.push({
          x: startX + c * seatDiam + seatRadius,
          y,
          r: seatRadius,
        });
      }
    }
  }

  return positions;
};

/**
 * Calcula las intersecciones de una línea horizontal (y = scanY) con
 * los bordes del polígono definido por `points` ([[x,y], ...]).
 * Devuelve un array de valores X de intersección.
 */
function polyHorizontalIntersections(points, scanY) {
  const xs = [];
  const n  = points.length;
  for (let i = 0; i < n; i++) {
    const [ax, ay] = points[i];
    const [bx, by] = points[(i + 1) % n];
    if ((ay <= scanY && by > scanY) || (by <= scanY && ay > scanY)) {
      const t = (scanY - ay) / (by - ay);
      xs.push(ax + t * (bx - ax));
    }
  }
  return xs;
}

/**
 * Aplana [[x0,y0],[x1,y1],...] al formato plano [x0,y0,x1,y1,...] que Konva espera.
 */
export const flattenPoints = (points) => points.flatMap(([x, y]) => [x, y]);

/**
 * Calcula el centroide (promedio) de un array de vértices.
 */
export const polyCentroid = (points) => {
  if (!points || points.length === 0) return { x: 0, y: 0 };
  const sumX = points.reduce((s, [x]) => s + x, 0);
  const sumY = points.reduce((s, [, y]) => s + y, 0);
  return { x: sumX / points.length, y: sumY / points.length };
};
