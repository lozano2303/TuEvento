/**
 * Utilidades para procesar layout_data del evento.
 * Portado de layoutEditorUtils.js del web con adaptaciones para RN.
 */

/**
 * Migra polygonPoints del formato viejo [[x,y], ...] al nuevo formato con handles Bézier.
 */
export const migratePolygonPoints = (points) => {
  if (!Array.isArray(points) || points.length === 0) return points ?? [];
  if (typeof points[0] === 'object' && points[0] !== null && !Array.isArray(points[0]) && 'x' in points[0]) {
    return points; // ya migrado
  }
  return points.map(([x, y]) => ({
    x,
    y,
    handleIn: null,
    handleOut: null,
    symmetric: true,
  }));
};

/**
 * Asegura que un elemento de tipo polygon tenga polygonPoints en el nuevo formato.
 */
export const migrateElement = (el) => {
  if (el.shapeMode === 'polygon' && Array.isArray(el.polygonPoints) && el.polygonPoints.length > 0) {
    return { ...el, polygonPoints: migratePolygonPoints(el.polygonPoints) };
  }
  return el;
};

/**
 * Normaliza seatLayout de formato legacy (rows/cols) a nuevo (targetSeats).
 */
export const normalizeSeatLayout = (sl) => {
  if (!sl) return null;
  if (sl.targetSeats !== undefined) return sl;
  // Migración automática
  return {
    targetSeats: (sl.rows ?? 1) * (sl.cols ?? 1),
    seatRadius: sl.seatRadius ?? 7,
    gap: sl.gap ?? 4,
  };
};

/**
 * Calcula el bounding box de un array de vértices.
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
 * Subdivide un segmento Bézier cúbico en N puntos.
 */
function subdivideBezierSegment(p0, p1, p2, p3, n = 16) {
  const pts = [];
  for (let k = 1; k <= n; k++) {
    const t = k / (n + 1);
    const mt = 1 - t;
    pts.push({
      x: mt * mt * mt * p0.x + 3 * mt * mt * t * p1.x + 3 * mt * t * t * p2.x + t * t * t * p3.x,
      y: mt * mt * mt * p0.y + 3 * mt * mt * t * p1.y + 3 * mt * t * t * p2.y + t * t * t * p3.y,
    });
  }
  return pts;
}

/**
 * Aplana el contorno de un polígono con curvas Bézier en puntos {x,y}.
 */
export const flattenPolygonForFill = (points) => {
  if (!points || points.length < 3) return points ?? [];

  const SUBDIVISIONS = 16;
  const flat = [];

  for (let i = 0; i < points.length; i++) {
    const curr = points[i];
    const next = points[(i + 1) % points.length];

    flat.push({ x: curr.x, y: curr.y });

    if (curr.handleOut && next.handleIn) {
      const subdivided = subdivideBezierSegment(
        { x: curr.x, y: curr.y },
        { x: curr.handleOut.x, y: curr.handleOut.y },
        { x: next.handleIn.x, y: next.handleIn.y },
        { x: next.x, y: next.y },
        SUBDIVISIONS
      );
      for (const pt of subdivided) flat.push(pt);
    }
  }

  return flat;
};

/**
 * Calcula intersecciones horizontales para algoritmo even-odd.
 */
function polyHorizontalIntersections(points, scanY) {
  const xs = [];
  const n = points.length;
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
 * Calcula posiciones de sillas en un polígono usando algoritmo even-odd.
 */
export const computePolygonSeatRows = (polygonPoints, seatLayout) => {
  if (!polygonPoints || polygonPoints.length < 3 || !seatLayout) return [];

  const { seatRadius, gap } = seatLayout;
  const r = seatRadius ?? 7;
  const g = gap ?? 4;

  const flatPoints = flattenPolygonForFill(polygonPoints);
  const bb = polyBoundingBox(flatPoints);
  const pad = 6;
  const minY = bb.minY + pad;
  const maxY = bb.maxY - pad;
  if (maxY <= minY) return [];

  const cellH = r * 2 + g;
  const maxRows = Math.max(1, Math.floor((maxY - minY) / cellH));
  const seatDiam = r * 2 + g;
  const positions = [];

  for (let row = 0; row < maxRows; row++) {
    const y = minY + (row + 0.5) * ((maxY - minY) / maxRows);

    const xs = polyHorizontalIntersections(flatPoints, y);
    if (xs.length < 2) continue;
    xs.sort((a, b) => a - b);

    for (let s = 0; s + 1 < xs.length; s += 2) {
      const xLeft = xs[s] + pad;
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
 * Asigna fila y columna a las sillas de un polígono (vienen fila por fila, de arriba
 * abajo y de izquierda a derecha, como las genera computePolygonSeatRows).
 * Espejo de indexPolygonSeats en web/src/components/layout-editor/layoutEditorUtils.js.
 *
 * La columna es FÍSICA: se calcula con la posición X, no con el orden dentro de la fila.
 * En formas con huecos (herradura, U, anillo) las sillas a ambos lados del hueco quedan
 * en columnas separadas, y una página de 10 × 10 nunca junta los dos lados.
 * El orden de las posiciones no cambia: el índice de cada una sigue siendo el de su silla.
 *
 * @returns {{ positions, rowStructure }} rowStructure[fila] = columnas que ocupa la fila
 */
export function indexPolygonSeats(positions, cellSize) {
  if (positions.length === 0) return { positions: [], rowStructure: [] };
  // reduce y no Math.min(...): con cientos de miles de sillas el spread desborda la pila
  const minX      = positions.reduce((min, p) => Math.min(min, p.x), Infinity);
  const tolerance = cellSize * 0.1;
  const rowStructure = [];
  let rowIndex = -1;
  let rowY     = null;
  const indexed = positions.map((pos) => {
    if (rowY === null || Math.abs(pos.y - rowY) > tolerance) {
      rowIndex++;
      rowY = pos.y;
      rowStructure.push(0);
    }
    const colIndex = Math.round((pos.x - minX) / cellSize);
    rowStructure[rowIndex] = Math.max(rowStructure[rowIndex], colIndex + 1);
    return { ...pos, rowIndex, colIndex };
  });
  return { positions: indexed, rowStructure };
}

/**
 * Distribuye sillas en una sección rectangular.
 */
export const distributeSeatsRect = (element) => {
  const sl = normalizeSeatLayout(element.seatLayout);
  if (!sl) return [];

  const { targetSeats, seatRadius, gap } = sl;
  const PADDING = 12;
  const LABEL_H = 20;
  const cellSize = seatRadius * 2 + gap;

  const availW = element.width - PADDING * 2;
  const availH = element.height - PADDING * 2 - LABEL_H;
  const maxCols = Math.max(1, Math.floor((availW + gap) / cellSize));
  const maxRows = Math.max(1, Math.floor((availH + gap) / cellSize));
  const maxSeats = maxCols * maxRows;
  const actual = Math.min(targetSeats, maxSeats);

  if (actual <= 0) return [];

  const aspectRatio = availW / Math.max(1, availH);
  let cols = Math.min(maxCols, Math.max(1, Math.round(Math.sqrt(actual * aspectRatio))));
  let rows = Math.min(maxRows, Math.ceil(actual / cols));

  while (rows * cols > maxSeats && rows > 1) rows--;
  while (rows * cols > maxSeats && cols > 1) cols--;

  const cellW = availW / cols;
  const cellH = availH / rows;
  const positions = [];
  let count = 0;

  outer: for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      if (count >= actual) break outer;
      positions.push({
        x: PADDING + c * cellW + cellW / 2,
        y: PADDING + LABEL_H / 2 + r * cellH + cellH / 2,
        r: Math.max(2, Math.min(seatRadius, (Math.min(cellW, cellH) - gap) / 2)),
        rowIndex: r,
        colIndex: c,
      });
      count++;
    }
  }

  // Estructura uniforme: todas las filas tienen `cols` sillas salvo la última
  const rowStructure = Array(rows).fill(cols);
  const seatsInLastRow = actual - (rows - 1) * cols;
  if (seatsInLastRow < cols && rows > 0) {
    rowStructure[rows - 1] = seatsInLastRow;
  }

  return {
    positions,
    rowStructure,
    isUniformGrid: true,
  };
};

/**
 * Distribuye sillas en una sección (rect o polygon).
 */
export const distributeSeats = (element) => {
  const shapeMode = element.shapeMode ?? 'rect';

  if (shapeMode === 'polygon' && element.polygonPoints) {
    const sl = normalizeSeatLayout(element.seatLayout);
    if (!sl) return { positions: [], rowStructure: [], isUniformGrid: false };
    const allPositions = computePolygonSeatRows(element.polygonPoints, sl);
    return {
      ...indexPolygonSeats(allPositions.slice(0, sl.targetSeats), sl.seatRadius * 2 + sl.gap),
      isUniformGrid: false,
    };
  }

  return distributeSeatsRect(element);
};

/**
 * Devuelve el AABB de un elemento (considerando rotación).
 */
export function getElementAABB(el) {
  const { x, y, width, height, rotation = 0 } = el;
  if (rotation === 0) return { minX: x, minY: y, maxX: x + width, maxY: y + height };
  
  const rad = (rotation * Math.PI) / 180;
  const cos = Math.abs(Math.cos(rad));
  const sin = Math.abs(Math.sin(rad));
  const cx = x + width / 2;
  const cy = y + height / 2;
  const hw = (width * cos + height * sin) / 2;
  const hh = (width * sin + height * cos) / 2;
  return { minX: cx - hw, minY: cy - hh, maxX: cx + hw, maxY: cy + hh };
}

/**
 * Calcula el AABB total de todos los elementos.
 */
export function computeTotalAABB(elements) {
  if (elements.length === 0) return { minX: 0, minY: 0, maxX: 1200, maxY: 800 };

  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
  for (const el of elements) {
    const aabb = getElementAABB(el);
    if (aabb.minX < minX) minX = aabb.minX;
    if (aabb.minY < minY) minY = aabb.minY;
    if (aabb.maxX > maxX) maxX = aabb.maxX;
    if (aabb.maxY > maxY) maxY = aabb.maxY;
  }

  return { minX, minY, maxX, maxY };
}

/** Filas (ordenadas) que tienen al menos una silla en la ventana de columnas `colPage`. */
export function rowsInColumnWindow(seatPositions, colPage, pageSize = 10) {
  const colStart = colPage * pageSize;
  const rows = new Set();
  for (const pos of seatPositions) {
    if (pos.colIndex >= colStart && pos.colIndex < colStart + pageSize) rows.add(pos.rowIndex);
  }
  return [...rows].sort((a, b) => a - b);
}

/**
 * Sillas de una página de la vista enfocada: la ventana de 10 columnas `colPage` y,
 * entre las filas que tienen sillas en esa ventana, el bloque de 10 filas `rowPage`.
 * Filtra por la fila y columna de cada silla, así que respeta los huecos de la forma.
 * Espejo de filterSeatsByPage en web/src/components/layout-editor/layoutEditorUtils.js.
 *
 * @returns {Array<{pos, realIndex}>} realIndex = índice de la silla dentro de la sección
 */
export function filterSeatsByPage(seatPositions, colPage, rowPage, pageSize = 10) {
  if (!seatPositions || seatPositions.length === 0) return [];
  const colStart = colPage * pageSize;
  const pageRows = new Set(rowsInColumnWindow(seatPositions, colPage, pageSize)
    .slice(rowPage * pageSize, (rowPage + 1) * pageSize));
  const page = [];
  seatPositions.forEach((pos, realIndex) => {
    const inWindow = pos.colIndex >= colStart && pos.colIndex < colStart + pageSize;
    if (inWindow && pageRows.has(pos.rowIndex)) page.push({ pos, realIndex });
  });
  return page;
}
