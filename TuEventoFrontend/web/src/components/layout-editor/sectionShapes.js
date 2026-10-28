// ── Catálogo de formas base para secciones ───────────────────────────────────
// Cada forma se define en coordenadas unitarias (cualquier escala) con anclas
// rectas o arcos Bézier; fitPoints la lleva al tamaño real del elemento.
// El resultado usa el mismo formato de vértices que el editor de vértices
// ({ x, y, handleIn, handleOut, symmetric }), así que toda forma se puede
// ajustar después a mano.

import { computeMaxSeats, flattenPolygonForFill, polyBoundingBox, snapToGrid } from './layoutEditorUtils';

/** Máximo de sillas por sección lógica; el backend rechaza más (MAX_SEATS_PER_SECTION). */
export const MAX_SECTION_SEATS = 200_000;

/**
 * Tamaños de silla. Con muchas sillas conviene uno menor: la sección ocupa mucho menos
 * en el mapa. El selector de asientos pagina por fila y columna, así que no cambia.
 */
export const SEAT_SIZES = [
  { id: 'normal',  label: 'Normal',   seatRadius: 8, gap: 4 },
  { id: 'compact', label: 'Compacta', seatRadius: 5, gap: 3 },
  { id: 'mini',    label: 'Mini',     seatRadius: 3, gap: 2 },
];

/** Tamaño sugerido según la cantidad de sillas de la sección. */
export const suggestedSeatSize = (seats) => {
  if (seats > 10_000) return 'mini';
  if (seats > 1_000) return 'compact';
  return 'normal';
};

export const getSeatSize = (id) => SEAT_SIZES.find((s) => s.id === id) ?? SEAT_SIZES[0];

/** Tamaño del catálogo que corresponde a un seatLayout (o el más parecido por radio). */
export const seatSizeOf = (seatLayout) => {
  const radius = seatLayout?.seatRadius ?? SEAT_SIZES[0].seatRadius;
  const distance = (size) => Math.abs(size.seatRadius - radius);
  return SEAT_SIZES.reduce((best, size) => (distance(size) < distance(best) ? size : best), SEAT_SIZES[0]);
};

const MIN_WIDTH  = 80;
const MIN_HEIGHT = 60;

const anchor = (x, y) => ({ x, y, handleIn: null, handleOut: null, symmetric: true });

/**
 * Arco elíptico de `from` a `to` (radianes, sentido horario en pantalla) como anclas
 * con handles Bézier, en tramos de hasta 90°. La primera ancla no tiene handleIn y
 * la última no tiene handleOut, para que los segmentos vecinos queden rectos.
 */
function arc(cx, cy, rx, ry, from, to) {
  const steps = Math.max(1, Math.ceil(Math.abs(to - from) / (Math.PI / 2) - 1e-9));
  const delta = (to - from) / steps;
  const k     = (4 / 3) * Math.tan(delta / 4);
  const pts   = [];
  for (let i = 0; i <= steps; i++) {
    const a  = from + delta * i;
    const x  = cx + rx * Math.cos(a);
    const y  = cy + ry * Math.sin(a);
    const tx = -rx * Math.sin(a) * k;
    const ty =  ry * Math.cos(a) * k;
    pts.push({
      x, y,
      handleIn:  i > 0     ? { x: x - tx, y: y - ty } : null,
      handleOut: i < steps ? { x: x + tx, y: y + ty } : null,
      symmetric: i > 0 && i < steps,
    });
  }
  return pts;
}

/** Elipse cerrada: el arco completo sin el ancla final repetida. */
function ellipse(cx, cy, rx, ry) {
  const pts  = arc(cx, cy, rx, ry, 0, Math.PI * 2);
  const last = pts.pop();
  pts[0] = { ...pts[0], handleIn: last.handleIn, symmetric: true };
  return pts;
}

/** Polígono regular de n lados con un vértice arriba. */
function regular(n) {
  return Array.from({ length: n }, (_, i) => {
    const a = -Math.PI / 2 + (i * 2 * Math.PI) / n;
    return anchor(Math.cos(a), Math.sin(a));
  });
}

const poly = (coords) => coords.map(([x, y]) => anchor(x, y));

const HALF_PI = Math.PI / 2;

function roundedRect() {
  const r = 0.18;
  return [
    ...arc(1 - r, r,     r, r, -HALF_PI, 0),
    ...arc(1 - r, 1 - r, r, r, 0, HALF_PI),
    ...arc(r,     1 - r, r, r, HALF_PI, Math.PI),
    ...arc(r,     r,     r, r, Math.PI, Math.PI + HALF_PI),
  ];
}

function horseshoe() {
  return [
    anchor(1, 0),
    ...arc(0.5, 0.5, 0.5, 0.5, 0, Math.PI),
    anchor(0, 0),
    anchor(0.25, 0),
    ...arc(0.5, 0.5, 0.25, 0.25, Math.PI, 0),
    anchor(0.75, 0),
  ];
}

// El anillo se cierra con una costura recta: el contorno exterior en un sentido y el
// interior en el contrario, así el relleno (y el cálculo de sillas) deja el hueco vacío.
const ring = () => [...arc(0, 0, 1, 1, 0, Math.PI * 2), ...arc(0, 0, 0.55, 0.55, Math.PI * 2, 0)];

const grandstandArc = () => [
  ...arc(0, 0, 1, 1, (7 * Math.PI) / 6, (11 * Math.PI) / 6),
  ...arc(0, 0, 0.55, 0.55, (11 * Math.PI) / 6, (7 * Math.PI) / 6),
];

/**
 * Catálogo de formas base. `aspect` es la relación ancho/alto con la que se crea la
 * sección; `rect` marca la única forma que no es polígono (shapeMode 'rect').
 */
const ALL_SHAPES = [
  { id: 'rect',              label: 'Rectángulo',            aspect: 4 / 3, rect: true },
  { id: 'roundedRect',       label: 'Rectángulo redondeado', aspect: 4 / 3, unit: roundedRect },
  { id: 'circle',            label: 'Círculo',               aspect: 1,     unit: () => ellipse(0, 0, 1, 1) },
  { id: 'oval',              label: 'Óvalo',                 aspect: 2,     unit: () => ellipse(0, 0, 1, 1) },
  { id: 'semicircle',        label: 'Semicírculo',           aspect: 2,     unit: () => arc(0, 0, 1, 1, Math.PI, Math.PI * 2) },
  { id: 'quarterCircle',     label: 'Cuarto de círculo',     aspect: 1,     unit: () => [...arc(0, 1, 1, 1, -HALF_PI, 0), anchor(0, 1)] },
  { id: 'grandstandArc',     label: 'Arco de tribuna',       aspect: 2.5,   unit: grandstandArc },
  { id: 'ring',              label: 'Anillo',                aspect: 1,     unit: ring, hollow: true },
  { id: 'horseshoe',         label: 'Herradura',             aspect: 1,     unit: horseshoe, hollow: true },
  { id: 'trapezoid',         label: 'Trapecio',              aspect: 1.6,   unit: () => poly([[0.15, 0], [0.85, 0], [1, 1], [0, 1]]) },
  { id: 'invertedTrapezoid', label: 'Trapecio invertido',    aspect: 1.6,   unit: () => poly([[0, 0], [1, 0], [0.85, 1], [0.15, 1]]) },
  { id: 'parallelogram',     label: 'Paralelogramo',         aspect: 1.6,   unit: () => poly([[0.2, 0], [1, 0], [0.8, 1], [0, 1]]) },
  { id: 'rhombus',           label: 'Rombo',                 aspect: 1.3,   unit: () => poly([[0.5, 0], [1, 0.5], [0.5, 1], [0, 0.5]]) },
  { id: 'triangle',          label: 'Triángulo',             aspect: 1.2,   unit: () => poly([[0.5, 0], [1, 1], [0, 1]]) },
  { id: 'rightTriangle',     label: 'Triángulo rectángulo',  aspect: 1.2,   unit: () => poly([[0, 0], [1, 1], [0, 1]]) },
  { id: 'pentagon',          label: 'Pentágono',             aspect: 1,     unit: () => regular(5) },
  { id: 'hexagon',           label: 'Hexágono',              aspect: 1.15,  unit: () => regular(6) },
  { id: 'octagon',           label: 'Octágono',              aspect: 1,     unit: () => regular(8) },
  { id: 'lshape',            label: 'Forma L',               aspect: 1,     unit: () => poly([[0, 0], [0.5, 0], [0.5, 0.5], [1, 0.5], [1, 1], [0, 1]]) },
  { id: 'ushape',            label: 'Forma U',               aspect: 1.2,   unit: () => poly([[0, 0], [0.3, 0], [0.3, 0.6], [0.7, 0.6], [0.7, 0], [1, 0], [1, 1], [0, 1]]), hollow: true },
  { id: 'tshape',            label: 'Forma T',               aspect: 1,     unit: () => poly([[0, 0], [1, 0], [1, 0.35], [0.65, 0.35], [0.65, 1], [0.35, 1], [0.35, 0.35], [0, 0.35]]) },
  { id: 'cross',             label: 'Cruz',                  aspect: 1,     unit: () => poly([[0.33, 0], [0.67, 0], [0.67, 0.33], [1, 0.33], [1, 0.67], [0.67, 0.67], [0.67, 1], [0.33, 1], [0.33, 0.67], [0, 0.67], [0, 0.33], [0.33, 0.33]]) },
];

/**
 * Formas que se ofrecen al crear o cambiar una sección. Las que tienen un hueco o el centro
 * vacío (`hollow`) no se ofrecen; se mantienen solo para que los layouts guardados con ellas
 * se sigan resolviendo.
 */
export const SECTION_SHAPES = ALL_SHAPES.filter((shape) => !shape.hollow);

const SHAPES_BY_ID = Object.fromEntries(ALL_SHAPES.map((s) => [s.id, s]));

export const getSectionShape = (id) => SHAPES_BY_ID[id] ?? SHAPES_BY_ID.rect;

const scalePoint = (p, fx, fy) => (p ? { x: fx(p.x), y: fy(p.y) } : null);

/**
 * Escala los puntos para que su contorno real (curvas incluidas) ocupe exactamente
 * el rectángulo [0, width] × [0, height].
 */
function fitPoints(points, width, height) {
  const bb = polyBoundingBox(flattenPolygonForFill(points));
  const fx = (x) => ((x - bb.minX) / (bb.width  || 1)) * width;
  const fy = (y) => ((y - bb.minY) / (bb.height || 1)) * height;
  return points.map((p) => ({
    ...p,
    x: fx(p.x),
    y: fy(p.y),
    handleIn:  scalePoint(p.handleIn, fx, fy),
    handleOut: scalePoint(p.handleOut, fx, fy),
  }));
}

/** Vértices de la forma escalados a width × height (el rectángulo también, para los presets). */
export function getShapePoints(id, width, height) {
  const shape = getSectionShape(id);
  if (shape.rect) return fitPoints(poly([[0, 0], [1, 0], [1, 1], [0, 1]]), width, height);
  return fitPoints(shape.unit(), width, height);
}

/** Geometría de una sección con esa forma: { shapeMode, polygonPoints, width, height }. */
export function buildShapeGeometry(id, width, height) {
  const shape = getSectionShape(id);
  return {
    shapeMode:     shape.rect ? 'rect' : 'polygon',
    polygonPoints: shape.rect ? null : getShapePoints(id, width, height),
    width,
    height,
  };
}

/** Atributo `d` de un <path> SVG con la forma centrada en una caja de size × size. */
export function getShapeIconPath(id, size = 40, padding = 4) {
  const { aspect } = getSectionShape(id);
  const box = size - padding * 2;
  const w   = aspect >= 1 ? box : box * aspect;
  const h   = aspect >= 1 ? box / aspect : box;
  const ox  = (size - w) / 2;
  const oy  = (size - h) / 2;
  const pts = getShapePoints(id, w, h);
  const f   = (p) => `${(p.x + ox).toFixed(1)},${(p.y + oy).toFixed(1)}`;
  const segments = pts.map((curr, i) => {
    const next = pts[(i + 1) % pts.length];
    return curr.handleOut && next.handleIn
      ? `C${f(curr.handleOut)} ${f(next.handleIn)} ${f(next)}`
      : `L${f(next)}`;
  });
  return `M${f(pts[0])} ${segments.join(' ')} Z`;
}

const seatsThatFit = (id, width, height, seatRadius, gap) =>
  computeMaxSeats(buildShapeGeometry(id, width, height), seatRadius, gap);

/**
 * Tamaño mínimo (múltiplo de la grilla de 10 px) en el que caben `targetSeats` sillas
 * con la forma elegida, respetando su relación de aspecto. El tamaño de cada silla no
 * cambia: una sección con más sillas simplemente es más grande.
 *
 * @returns {{ shapeMode, polygonPoints, width, height }}
 */
export function sizeSectionForSeats(id, targetSeats, { seatRadius, gap }) {
  const { aspect } = getSectionShape(id);
  const target = Math.min(MAX_SECTION_SEATS, Math.max(1, Math.round(targetSeats)));
  const dims   = (scale) => ({
    width:  Math.max(MIN_WIDTH,  snapToGrid(scale * aspect + 5)),
    height: Math.max(MIN_HEIGHT, snapToGrid(scale + 5)),
  });
  const fits = (scale) => {
    const { width, height } = dims(scale);
    return seatsThatFit(id, width, height, seatRadius, gap) >= target;
  };

  // Crecer hasta que quepan y luego acotar con búsqueda binaria
  const cell = seatRadius * 2 + gap;
  let lo = 0;
  let hi = cell * Math.sqrt(target / aspect);
  for (let i = 0; i < 40 && !fits(hi); i++) { lo = hi; hi *= 1.5; }
  for (let i = 0; i < 14 && hi - lo > 1; i++) {
    const mid = (lo + hi) / 2;
    if (fits(mid)) hi = mid; else lo = mid;
  }

  const { width, height } = dims(hi);
  return buildShapeGeometry(id, width, height);
}
