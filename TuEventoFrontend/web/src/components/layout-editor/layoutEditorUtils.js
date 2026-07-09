// ── UUID ──────────────────────────────────────────────────────────────────────
export const generateId = () =>
  `elem-${Math.random().toString(36).slice(2, 9)}-${Date.now().toString(36)}`;

export const generateSectionId = () =>
  `section-${Math.random().toString(36).slice(2, 9)}-${Date.now().toString(36)}`;

// ── Fase 1.12: Migración de polygonPoints al nuevo formato con handles Bézier ──
/**
 * Convierte polygonPoints del formato viejo [[x,y], ...] al nuevo formato
 * [{ x, y, handleIn, handleOut, symmetric }, ...].
 * Si ya tiene el nuevo formato (primer elemento es un objeto con x,y) lo devuelve tal cual.
 * Nunca crashea — devuelve array vacío si la entrada es inválida.
 */
export const migratePolygonPoints = (points) => {
  if (!Array.isArray(points) || points.length === 0) return points ?? [];
  // Detectar si ya tiene el nuevo formato (objeto con propiedad x)
  if (typeof points[0] === 'object' && points[0] !== null && !Array.isArray(points[0]) && 'x' in points[0]) {
    return points; // ya migrado
  }
  // Formato viejo: [[x,y], ...]
  return points.map(([x, y]) => ({
    x,
    y,
    handleIn:  null,
    handleOut: null,
    symmetric: true,
  }));
};

/**
 * Asegura que un elemento de tipo polygon tenga polygonPoints en el nuevo formato.
 * Úsalo en cualquier punto de carga/inicialización.
 */
export const migrateElement = (el) => {
  if (el.shapeMode === 'polygon' && Array.isArray(el.polygonPoints) && el.polygonPoints.length > 0) {
    return { ...el, polygonPoints: migratePolygonPoints(el.polygonPoints) };
  }
  return el;
};

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
    // computePolygonSeatRows calcula las filas desde el espacio real del polígono
    const positions = computePolygonSeatRows(element.polygonPoints, { seatRadius: r, gap: g });
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
    // Generar todas las posiciones posibles y tomar las primeras targetSeats.
    // computePolygonSeatRows ya calcula las filas desde el espacio real del polígono.
    const all = computePolygonSeatRows(element.polygonPoints, { seatRadius, gap });
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
export const CANVAS_MARGIN  = 150;   // respiro alrededor del contenido
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
 * Calcula el bounding box de un array de vértices en formato nuevo [{x,y,...}].
 * Devuelve { minX, minY, maxX, maxY, width, height }.
 */
export const polyBoundingBox = (points) => {
  if (!points || points.length === 0) return { minX: 0, minY: 0, maxX: 0, maxY: 0, width: 0, height: 0 };
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
  for (const pt of points) {
    const x = Array.isArray(pt) ? pt[0] : pt.x;
    const y = Array.isArray(pt) ? pt[1] : pt.y;
    if (x < minX) minX = x;
    if (y < minY) minY = y;
    if (x > maxX) maxX = x;
    if (y > maxY) maxY = y;
  }
  return { minX, minY, maxX, maxY, width: maxX - minX, height: maxY - minY };
};

/**
 * Fase 1.12 Prompt B: Subdivide un segmento Bézier cúbico en N puntos {x,y}.
 *
 * P0 = vértice ancla i, P1 = handleOut de i, P2 = handleIn de i+1, P3 = vértice ancla i+1.
 * Usa la fórmula estándar B(t) = (1-t)³P0 + 3(1-t)²tP1 + 3(1-t)t²P2 + t³P3.
 *
 * @param {{ x,y }} p0  — punto de inicio (ancla i)
 * @param {{ x,y }} p1  — handle de salida de p0
 * @param {{ x,y }} p2  — handle de entrada de p3
 * @param {{ x,y }} p3  — punto de llegada (ancla i+1)
 * @param {number}  n   — número de puntos de subdivisión (sin incluir extremos)
 * @returns {{ x, y }[]}  — n puntos intermedios a lo largo de la curva
 */
function subdivideBezierSegment(p0, p1, p2, p3, n = 16) {
  const pts = [];
  for (let k = 1; k <= n; k++) {
    const t  = k / (n + 1);
    const mt = 1 - t;
    pts.push({
      x: mt * mt * mt * p0.x + 3 * mt * mt * t * p1.x + 3 * mt * t * t * p2.x + t * t * t * p3.x,
      y: mt * mt * mt * p0.y + 3 * mt * mt * t * p1.y + 3 * mt * t * t * p2.y + t * t * t * p3.y,
    });
  }
  return pts;
}

/**
 * Fase 1.12 Prompt B: "Aplana" el contorno de un polígono con curvas Bézier
 * en un array de puntos {x,y} que aproxima fielmente el contorno visual real.
 *
 * Segmentos RECTOS aportan solo sus dos anclas (comportamiento idéntico al anterior).
 * Segmentos CURVOS aportan: ancla i + N puntos de subdivisión + ancla i+1.
 * Los extremos compartidos entre segmentos consecutivos NO se duplican.
 *
 * El resultado es un polígono "denso" que el algoritmo even-odd puede usar
 * directamente — la lógica interna del even-odd no cambia, solo su input.
 *
 * N = 16 puntos por segmento curvo:
 *   • Para curvas de 50–300px de longitud → error < 0.5px respecto a la cúbica real.
 *   • Más que suficiente para que las sillas sigan el contorno sin costuras visibles.
 *   • Para un polígono típico (4-8 curvas) produce ≤ 8 × (16+2) = 144 puntos totales,
 *     completamente manejable para el algoritmo O(n×rows) del even-odd.
 *
 * @param {Array<{x,y,handleIn?,handleOut?,symmetric?}>} points — formato nuevo (migrado)
 * @returns {{ x, y }[]}  — contorno aplanado, listo para polyHorizontalIntersections
 */
export const flattenPolygonForFill = (points) => {
  if (!points || points.length < 3) return points ?? [];

  const SUBDIVISIONS = 16;
  const flat = [];

  for (let i = 0; i < points.length; i++) {
    const curr = points[i];
    const next = points[(i + 1) % points.length];

    // Incluir el vértice ancla actual (sin duplicar el punto de cierre)
    flat.push({ x: curr.x, y: curr.y });

    // Segmento curvo: interpolar con Bézier cúbica
    if (curr.handleOut && next.handleIn) {
      const subdivided = subdivideBezierSegment(
        { x: curr.x,           y: curr.y           },
        { x: curr.handleOut.x, y: curr.handleOut.y },
        { x: next.handleIn.x,  y: next.handleIn.y  },
        { x: next.x,           y: next.y           },
        SUBDIVISIONS,
      );
      for (const pt of subdivided) flat.push(pt);
    }
    // Segmento recto: solo las dos anclas (ya incluida la ancla de inicio arriba)
    // El ancla de llegada (next) se incluirá en la siguiente iteración como curr
  }

  return flat;
};

/**
 * Fase 1.5 / hotfix: algoritmo even-odd que calcula las filas desde el rango
 * vertical REAL del polígono. No usa `rows` como parámetro de densidad fija —
 * en su lugar calcula cuántas filas caben físicamente dado seatRadius y gap.
 *
 * Fase 1.12 Prompt B: antes de calcular intersecciones, aplana el contorno
 * con flattenPolygonForFill para que los segmentos curvos sean seguidos
 * fielmente (subdivisión Bézier con N=16 puntos por segmento curvo).
 * Segmentos rectos no se ven afectados.
 *
 * seatLayout debe tener: { seatRadius, gap } — rows/cols se ignoran aquí.
 * Devuelve TODAS las posiciones que caben físicamente en el polígono.
 * distributeSeats se encarga de recortar al targetSeats deseado.
 */
export const computePolygonSeatRows = (polygonPoints, seatLayout) => {
  if (!polygonPoints || polygonPoints.length < 3 || !seatLayout) return [];

  const { seatRadius, gap } = seatLayout;
  const r = seatRadius ?? 7;
  const g = gap        ?? 4;

  // Fase 1.12 Prompt B: aplanar el contorno con subdivisión Bézier antes de
  // calcular intersecciones. Para polígonos sin curvas esto es equivalente al
  // array de vértices ancla original — sin cambio de comportamiento.
  const flatPoints = flattenPolygonForFill(polygonPoints);

  const bb  = polyBoundingBox(flatPoints);
  const pad = 6;
  const minY = bb.minY + pad;
  const maxY = bb.maxY - pad;
  if (maxY <= minY) return [];

  // Fix: calcular las filas desde el espacio vertical REAL, no desde un `rows` fijo
  const cellH    = r * 2 + g;
  const maxRows  = Math.max(1, Math.floor((maxY - minY) / cellH));
  const seatDiam = r * 2 + g;
  const positions = [];

  for (let row = 0; row < maxRows; row++) {
    // Y del centro de esta fila — en coordenadas relativas al elemento
    const y = minY + (row + 0.5) * ((maxY - minY) / maxRows);

    const xs = polyHorizontalIntersections(flatPoints, y);
    if (xs.length < 2) continue;
    xs.sort((a, b) => a - b);

    // Even-odd: pares consecutivos de intersecciones = segmentos rellenos
    for (let s = 0; s + 1 < xs.length; s += 2) {
      const xLeft  = xs[s]     + pad;
      const xRight = xs[s + 1] - pad;
      const availW = xRight - xLeft;
      if (availW < seatDiam) continue;

      const count = Math.floor((availW + g) / seatDiam);
      if (count <= 0) continue;

      const totalW = count * seatDiam - g;
      const startX = xLeft + (availW - totalW) / 2;
      for (let c = 0; c < count; c++) {
        positions.push({
          x: startX + c * seatDiam + r,
          y,
          r,
        });
      }
    }
  }

  return positions;
};

/**
 * Calcula las intersecciones de una línea horizontal (y = scanY) con
 * los bordes del polígono. Acepta el nuevo formato [{x,y,...}] o {x,y}.
 * Fase 1.12 Prompt B: recibe el contorno aplanado (flatPoints) que ya incluye
 * la subdivisión Bézier — todos los segmentos son tratados como rectos aquí.
 * Devuelve un array de valores X de intersección.
 */
function polyHorizontalIntersections(points, scanY) {
  const xs = [];
  const n  = points.length;
  for (let i = 0; i < n; i++) {
    const a = points[i];
    const b = points[(i + 1) % n];
    const ax = Array.isArray(a) ? a[0] : a.x;
    const ay = Array.isArray(a) ? a[1] : a.y;
    const bx = Array.isArray(b) ? b[0] : b.x;
    const by = Array.isArray(b) ? b[1] : b.y;
    if ((ay <= scanY && by > scanY) || (by <= scanY && ay > scanY)) {
      const t = (scanY - ay) / (by - ay);
      xs.push(ax + t * (bx - ax));
    }
  }
  return xs;
}

/**
 * Aplana el nuevo formato [{x,y,...}] al formato plano [x0,y0,x1,y1,...] que Konva espera.
 * Compatible también con el formato viejo [[x,y],...] como fallback defensivo.
 */
export const flattenPoints = (points) => points.flatMap((pt) =>
  Array.isArray(pt) ? pt : [pt.x, pt.y]
);

/**
 * Calcula el centroide (promedio) de un array de vértices en el nuevo formato [{x,y,...}].
 */
export const polyCentroid = (points) => {
  if (!points || points.length === 0) return { x: 0, y: 0 };
  let sumX = 0, sumY = 0;
  for (const pt of points) {
    sumX += Array.isArray(pt) ? pt[0] : pt.x;
    sumY += Array.isArray(pt) ? pt[1] : pt.y;
  }
  return { x: sumX / points.length, y: sumY / points.length };
};

// ── Formas sugeridas (preset polygons) ───────────────────────────────────────

/**
 * Devuelve un array de polygonPoints en formato nuevo [{x,y,handleIn,handleOut,symmetric}]
 * en coordenadas relativas al elemento (minX=0, minY=0), escalados al bounding box dado.
 *
 * @param {'rect'|'circle'|'semicircle'|'trapezoid'|'triangle'|'hexagon'|'lshape'} preset
 * @param {number} width  — ancho actual del bounding box del elemento
 * @param {number} height — alto actual del bounding box del elemento
 * @returns {Array<{x,y,handleIn,handleOut,symmetric}>}
 */
export function getPresetPolygonPoints(preset, width, height) {
  const w = width;
  const h = height;

  // Helper: punto ancla sin curvas
  const pt = (x, y) => ({ x, y, handleIn: null, handleOut: null, symmetric: true });

  switch (preset) {
    case 'rect':
      return [pt(0, 0), pt(w, 0), pt(w, h), pt(0, h)];

    case 'circle': {
      // 8 puntos cardinales, handles tangentes al elipse inscrito.
      // Magnitud del handle para aproximar un círculo con cúbicas: k = 0.5523 * radio.
      const cx = w / 2, cy = h / 2;
      const rx = w / 2, ry = h / 2;
      const kx = 0.5523 * rx;   // handle horizontal
      const ky = 0.5523 * ry;   // handle vertical

      // Los 8 puntos en ángulos 0°,45°,90°,135°,180°,225°,270°,315°
      // con handles siempre perpendiculares al radio en ese punto.
      // Para los 4 cardinales los handles son puros; para los diagonales
      // los handles se derivan geométricamente para mantener continuidad.
      // Simplificación práctica: usamos solo los 4 cardinales + handles k —
      // visualmente indistinguible de un elipse real.
      return [
        // Derecha (0°)
        { x: cx + rx, y: cy,      handleIn:  { x: cx + rx,  y: cy - ky }, handleOut: { x: cx + rx,  y: cy + ky }, symmetric: true },
        // Abajo (90°)
        { x: cx,      y: cy + ry, handleIn:  { x: cx + kx,  y: cy + ry }, handleOut: { x: cx - kx,  y: cy + ry }, symmetric: true },
        // Izquierda (180°)
        { x: cx - rx, y: cy,      handleIn:  { x: cx - rx,  y: cy + ky }, handleOut: { x: cx - rx,  y: cy - ky }, symmetric: true },
        // Arriba (270°)
        { x: cx,      y: cy - ry, handleIn:  { x: cx - kx,  y: cy - ry }, handleOut: { x: cx + kx,  y: cy - ry }, symmetric: true },
      ];
    }

    case 'semicircle': {
      // Semicírculo: arco superior (media elipse) + base plana inferior.
      // Modelado con los 3 puntos cardinales de la media elipse superior
      // (izquierdo, cima, derecho) usando handles Bézier estándar k=0.5523.
      //
      //   rx = w/2, ry = h/2  →  kx = 0.5523*rx, ky = 0.5523*ry
      //
      // Punto cardinal izquierdo (180°): (0, h/2)
      //   tangente vertical  → handles en ±Y
      //   handleIn  (viene del inferior-izq): (0, h/2 + ky)
      //   handleOut (sale hacia la cima):     (0, h/2 - ky)
      //
      // Punto cardinal superior (270°): (w/2, 0)
      //   tangente horizontal → handles en ±X
      //   handleIn  (viene de la izquierda):  (w/2 - kx, 0)
      //   handleOut (sale hacia la derecha):  (w/2 + kx, 0)
      //
      // Punto cardinal derecho (0°): (w, h/2)
      //   tangente vertical → handles en ±Y
      //   handleIn  (viene de la cima):        (w, h/2 - ky)
      //   handleOut (sale hacia la base):      (w, h/2 + ky)
      //
      // Base plana: (w,h) → (0,h) sin handles (segmento recto).
      const rx = w / 2;
      const ry = h / 2;
      const kx = 0.5523 * rx;
      const ky = 0.5523 * ry;
      const cx = w / 2;
      const cy = h / 2;
      return [
        // Esquina inferior izquierda — base recta, sin handles
        pt(0, h),
        // Cardinal izquierdo del arco (180°)
        { x: 0,  y: cy, handleIn:  { x: 0,       y: cy + ky }, handleOut: { x: 0,       y: cy - ky }, symmetric: true },
        // Cima del arco (270°)
        { x: cx, y: 0,  handleIn:  { x: cx - kx,  y: 0       }, handleOut: { x: cx + kx,  y: 0       }, symmetric: true },
        // Cardinal derecho del arco (0°)
        { x: w,  y: cy, handleIn:  { x: w,        y: cy - ky }, handleOut: { x: w,        y: cy + ky }, symmetric: true },
        // Esquina inferior derecha — base recta, sin handles
        pt(w, h),
      ];
    }

    case 'trapezoid':
      return [
        pt(w * 0.15, 0),
        pt(w * 0.85, 0),
        pt(w, h),
        pt(0, h),
      ];

    case 'triangle':
      return [
        pt(w / 2, 0),
        pt(w, h),
        pt(0, h),
      ];

    case 'hexagon':
      return [
        pt(w / 2,    0),
        pt(w,        h * 0.25),
        pt(w,        h * 0.75),
        pt(w / 2,    h),
        pt(0,        h * 0.75),
        pt(0,        h * 0.25),
      ];

    case 'lshape':
      return [
        pt(0,       0),
        pt(w * 0.5, 0),
        pt(w * 0.5, h * 0.5),
        pt(w,       h * 0.5),
        pt(w,       h),
        pt(0,       h),
      ];

    default:
      return [pt(0, 0), pt(w, 0), pt(w, h), pt(0, h)];
  }
}

// ── AABB rotado ───────────────────────────────────────────────────────────────

/**
 * Devuelve el axis-aligned bounding box (AABB) real de un elemento,
 * teniendo en cuenta su rotación alrededor del centro.
 * Para rotation === 0 equivale exactamente a { x, y, x+w, y+h }.
 */
export function getElementAABB(el) {
  const { x, y, width, height, rotation = 0 } = el;
  if (rotation === 0) return { minX: x, minY: y, maxX: x + width, maxY: y + height };
  const rad = (rotation * Math.PI) / 180;
  const cos = Math.abs(Math.cos(rad));
  const sin = Math.abs(Math.sin(rad));
  const cx  = x + width  / 2;
  const cy  = y + height / 2;
  const hw  = (width  * cos + height * sin) / 2;
  const hh  = (width  * sin + height * cos) / 2;
  return { minX: cx - hw, minY: cy - hh, maxX: cx + hw, maxY: cy + hh };
}

// ── Fase 1.6: Smart guides ────────────────────────────────────────────────────
/**
 * Calcula los 6 bordes de snapping de un elemento (3 verticales + 3 horizontales).
 * Trabaja con coordenadas absolutas del canvas.
 */
export const getElementSnapEdges = (element) => {
  const left    = element.x;
  const right   = element.x + element.width;
  const centerX = element.x + element.width  / 2;
  const top     = element.y;
  const bottom  = element.y + element.height;
  const centerY = element.y + element.height / 2;
  return {
    vertical:   [
      { type: 'left',    value: left    },
      { type: 'centerX', value: centerX },
      { type: 'right',   value: right   },
    ],
    horizontal: [
      { type: 'top',     value: top     },
      { type: 'centerY', value: centerY },
      { type: 'bottom',  value: bottom  },
    ],
  };
};

/**
 * Compara los bordes del elemento activo contra todos los demás y devuelve
 * las mejores guías de alineación (la de menor delta por eje).
 *
 * @param {object}   activeElement  — elemento con posición tentativa actual
 * @param {object[]} otherElements  — elementos a comparar (ya filtrados: sin el activo)
 * @param {number}   threshold      — distancia máxima en px para considerar alineación
 * @returns {{ vertical: Guide|null, horizontal: Guide|null }}
 *          Guide = { position, delta }
 */
export const findSnapGuides = (activeElement, otherElements, threshold = 6) => {
  const activeEdges = getElementSnapEdges(activeElement);
  const vCandidates = [];
  const hCandidates = [];

  for (const other of otherElements) {
    if (other.id === activeElement.id) continue;
    const otherEdges = getElementSnapEdges(other);

    for (const ae of activeEdges.vertical) {
      for (const oe of otherEdges.vertical) {
        const diff = ae.value - oe.value;
        if (Math.abs(diff) <= threshold) {
          vCandidates.push({ position: oe.value, delta: oe.value - ae.value });
        }
      }
    }
    for (const ae of activeEdges.horizontal) {
      for (const oe of otherEdges.horizontal) {
        const diff = ae.value - oe.value;
        if (Math.abs(diff) <= threshold) {
          hCandidates.push({ position: oe.value, delta: oe.value - ae.value });
        }
      }
    }
  }

  const best = (arr) =>
    arr.length === 0 ? null
    : arr.reduce((a, b) => Math.abs(a.delta) <= Math.abs(b.delta) ? a : b);

  return { vertical: best(vCandidates), horizontal: best(hCandidates) };
};

// ── Fase 1.7: Smart guides para vértices individuales ────────────────────────

/**
 * Calcula guías de alineación para un vértice individual que se está arrastrando.
 * Compara contra dos fuentes:
 *   1. Los demás vértices de la misma forma (alineación interna — simetría).
 *   2. Bordes y centros de los demás elementos del canvas (reutiliza getElementSnapEdges).
 *
 * Todas las coordenadas de entrada son ABSOLUTAS (canvas-space).
 *
 * @param {{ x: number, y: number }} activeVertexAbsolute
 *   Posición tentativa del vértice que se arrastra, en coordenadas de canvas.
 *
 * @param {{ x: number, y: number }[]} ownOtherVerticesAbsolute
 *   Resto de vértices de la misma forma (todos excepto el activo), en coordenadas de canvas.
 *
 * @param {object[]} otherElements
 *   Elementos del canvas que no son la sección en edición.
 *
 * @param {number} threshold
 *   Tolerancia en px (default: 6).
 *
 * @returns {{ vertical: VertexGuide|null, horizontal: VertexGuide|null }}
 *   VertexGuide = { position, delta, source: 'vertex' | 'element' }
 *   position — coordenada absoluta donde dibujar la línea guía
 *   delta    — desplazamiento a aplicar para hacer snap exacto
 *   source   — 'vertex' si la guía viene de otro vértice propio, 'element' si viene de elemento externo
 */
export const findVertexSnapGuides = (
  activeVertexAbsolute,
  ownOtherVerticesAbsolute,
  otherElements,
  threshold = 6,
) => {
  const verticalCandidates   = [];
  const horizontalCandidates = [];

  // Fuente 1 — otros vértices de la misma forma
  for (const v of ownOtherVerticesAbsolute) {
    if (Math.abs(v.x - activeVertexAbsolute.x) <= threshold) {
      verticalCandidates.push({
        position: v.x,
        delta:    v.x - activeVertexAbsolute.x,
        source:   'vertex',
        matchY:   v.y,  // Y del vértice con el que coincide → para calcular largo de línea
      });
    }
    if (Math.abs(v.y - activeVertexAbsolute.y) <= threshold) {
      horizontalCandidates.push({
        position: v.y,
        delta:    v.y - activeVertexAbsolute.y,
        source:   'vertex',
        matchX:   v.x,  // X del vértice con el que coincide → para calcular largo de línea
      });
    }
  }

  // Fuente 2 — bordes/centros de otros elementos del canvas
  for (const el of otherElements) {
    const edges = getElementSnapEdges(el);
    for (const edge of edges.vertical) {
      if (Math.abs(edge.value - activeVertexAbsolute.x) <= threshold) {
        // elementEdgeY: Y representativa del borde del elemento para extender la línea hasta él
        const elementEdgeY = edge.type === 'centerX'
          ? el.y + el.height / 2
          : el.y; // para left/right usamos el top del elemento como ancla
        verticalCandidates.push({
          position:     edge.value,
          delta:        edge.value - activeVertexAbsolute.x,
          source:       'element',
          elementEdgeY, // Y hasta donde extender la guía vertical
          elementMinY:  el.y,
          elementMaxY:  el.y + el.height,
        });
      }
    }
    for (const edge of edges.horizontal) {
      if (Math.abs(edge.value - activeVertexAbsolute.y) <= threshold) {
        // elementEdgeX: X representativa del borde del elemento para extender la línea hasta él
        const elementEdgeX = edge.type === 'centerY'
          ? el.x + el.width / 2
          : el.x; // para top/bottom usamos el left del elemento como ancla
        horizontalCandidates.push({
          position:     edge.value,
          delta:        edge.value - activeVertexAbsolute.y,
          source:       'element',
          elementEdgeX, // X hasta donde extender la guía horizontal
          elementMinX:  el.x,
          elementMaxX:  el.x + el.width,
        });
      }
    }
  }

  const best = (arr) =>
    arr.length === 0 ? null
    : arr.reduce((a, b) => Math.abs(a.delta) <= Math.abs(b.delta) ? a : b);

  return {
    vertical:   best(verticalCandidates),
    horizontal: best(horizontalCandidates),
  };
};
