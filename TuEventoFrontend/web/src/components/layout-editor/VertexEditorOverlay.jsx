import { useRef } from 'react';
import { Circle, Line } from 'react-konva';
import {
  findVertexSnapGuides,
  migratePolygonPoints,
} from './layoutEditorUtils';

const VERTEX_RADIUS   = 7;
const MIDPOINT_RADIUS = 5;
const HANDLE_RADIUS   = 5;
const GUIDE_MARGIN    = 10;

/**
 * VertexEditorOverlay — handles de edición del polígono libre.
 *
 * ── Interacciones disponibles ───────────────────────────────────────────────
 *
 *  Vértices ancla (círculos morados sólidos)
 *    • Arrastrar          — mueve el vértice; snap a otros vértices y bordes.
 *    • Click derecho      — elimina el vértice (mínimo 3 vértices).
 *
 *  Puntos medios (círculos semitransparentes, en mitad de cada segmento RECTO)
 *    • Click simple       — inserta un vértice nuevo en ese punto.
 *    • Alt + arrastrar    — convierte ese segmento en una curva Bézier cúbica.
 *                           Los dos handles aparecen a 1/3 y 2/3 del segmento
 *                           y se desplazan juntos simétricamente mientras arrastras.
 *    • El midpoint desaparece una vez que el segmento ya tiene curva.
 *
 *  Handles de control (círculos pequeños huecos, conectados al ancla por línea punteada)
 *    • Arrastrar          — ajusta la curvatura del segmento adyacente.
 *                           Si el vértice es simétrico (por defecto), el handle
 *                           opuesto se mueve en espejo automáticamente → curva suave.
 *    • Click derecho      — alterna entre simétrico e independiente para ese vértice.
 *    • Shift + click derecho — elimina la curva del segmento adyacente, devuelve
 *                           el segmento a recto y hace que el midpoint reaparezca.
 *
 * ── Notas de implementación ─────────────────────────────────────────────────
 *
 *  • Esta Layer vive separada del Group draggable de la sección (Fase 1.8),
 *    por lo que el drag de un handle/vértice nunca propaga al Group padre.
 *  • Todas las coordenadas aquí son ABSOLUTAS (canvas-space). La conversión
 *    relativo↔absoluto se hace con element.x/element.y al subir/bajar callbacks.
 *  • Los handles llevan name="bezier-handle-{id}" para que el filtro de
 *    handleStageMouseDown en LayoutEditorCanvas los reconozca como clicks
 *    internos y no expulse al usuario del modo edición al iniciar un drag.
 *  • El algoritmo de relleno de sillas (Prompt B) no usa handleIn/handleOut —
 *    trabaja solo con vértices ancla hasta que Prompt B lo corrija.
 */

/**
 * Calcula la geometría de línea corta { x1,y1,x2,y2 } para una guía de vértice.
 */
function computeVertexGuideLines(snappedAbs, guides) {
  let verticalLine   = null;
  let horizontalLine = null;

  if (guides.vertical) {
    const g = guides.vertical;
    if (g.source === 'vertex') {
      const minY = Math.min(snappedAbs.y, g.matchY);
      const maxY = Math.max(snappedAbs.y, g.matchY);
      verticalLine = {
        x1: g.position, y1: minY - GUIDE_MARGIN,
        x2: g.position, y2: maxY + GUIDE_MARGIN,
      };
    } else {
      const minY = Math.min(snappedAbs.y, g.elementMinY);
      const maxY = Math.max(snappedAbs.y, g.elementMaxY);
      verticalLine = {
        x1: g.position, y1: minY - GUIDE_MARGIN,
        x2: g.position, y2: maxY + GUIDE_MARGIN,
      };
    }
  }

  if (guides.horizontal) {
    const g = guides.horizontal;
    if (g.source === 'vertex') {
      const minX = Math.min(snappedAbs.x, g.matchX);
      const maxX = Math.max(snappedAbs.x, g.matchX);
      horizontalLine = {
        x1: minX - GUIDE_MARGIN, y1: g.position,
        x2: maxX + GUIDE_MARGIN, y2: g.position,
      };
    } else {
      const minX = Math.min(snappedAbs.x, g.elementMinX);
      const maxX = Math.max(snappedAbs.x, g.elementMaxX);
      horizontalLine = {
        x1: minX - GUIDE_MARGIN, y1: g.position,
        x2: maxX + GUIDE_MARGIN, y2: g.position,
      };
    }
  }

  return { vertical: verticalLine, horizontal: horizontalLine };
}

/**
 * Renderiza los handles de vértices, puntos medios, y handles de Bézier
 * del polígono en edición.
 *
 * Vive en su propia Layer — separada del Group draggable de la sección.
 * Todas las coordenadas son ABSOLUTAS (canvas-space).
 *
 * Props:
 *   element             — sección en edición (con polygonPoints en nuevo formato, x, y)
 *   previewPoints       — [{x,y,handleIn,handleOut,symmetric},...] en tiempo real durante drag
 *   onVertexDrag        — (idx, absX, absY) => void
 *   onVertexDragEnd     — (idx, absX, absY) => void
 *   onMidpointClick     — (insertAfterIdx) => void
 *   onSegmentCurve      — (segmentIdx, handleOutAbs, handleInAbs) => void
 *   onVertexRightClick  — (idx) => void
 *   onHandleDrag        — (vertexIdx, side, absX, absY) => void  — 'in' | 'out'
 *   onHandleDragEnd     — (vertexIdx, side, absX, absY) => void
 *   onHandleRightClick  — (vertexIdx) => void  — toggle symmetric
 *   onClearCurve        — (vertexIdx, side) => void  — Shift+click derecho → eliminar curva
 *   onVertexGuideChange — ({ vertical, horizontal }) => void
 *   otherElements       — elementos del canvas excepto la sección en edición
 */
export default function VertexEditorOverlay({
  element,
  previewPoints,
  onVertexDrag,
  onVertexDragEnd,
  onMidpointClick,
  onSegmentCurve,
  onVertexRightClick,
  onHandleDrag,
  onHandleDragEnd,
  onHandleRightClick,
  onClearCurve,
  onVertexGuideChange,
  otherElements,
}) {
  if (!element) return null;

  // Usar previewPoints si está disponible, sino los puntos del elemento — migrar siempre
  const rawPoints = previewPoints ?? element.polygonPoints;
  const points = rawPoints ? migratePolygonPoints(rawPoints) : null;
  if (!points || points.length < 3) return null;

  // Convertir vértices ancla de coordenadas relativas → absolutas del canvas
  const absPoints = points.map((pt) => ({
    ...pt,
    // Posición absoluta del ancla
    absX: element.x + pt.x,
    absY: element.y + pt.y,
    // Handles absolutos (si existen)
    absHandleIn:  pt.handleIn  ? { x: element.x + pt.handleIn.x,  y: element.y + pt.handleIn.y  } : null,
    absHandleOut: pt.handleOut ? { x: element.x + pt.handleOut.x, y: element.y + pt.handleOut.y } : null,
  }));

  return (
    <>
      {/* ── Líneas "bigotes" de handles Bézier ─────────────────────────── */}
      {absPoints.map((pt, i) => (
        <BezierWhiskers key={`whiskers-${i}`} pt={pt} />
      ))}

      {/* ── Puntos medios — insertar vértice o Alt+drag para curvar ──────── */}
      {/* Renderizados ANTES que los handles Bézier para que los handles      */}
      {/* queden en z-order superior y ganen el hit-test si se superponen.   */}
      {absPoints.map((pt, i) => {
        const next = absPoints[(i + 1) % absPoints.length];

        // Un segmento es curvo si Y SOLO SI el vértice de salida (pt) tiene
        // handleOut no-nulo Y el vértice de llegada (next) tiene handleIn no-nulo.
        // Se usa ?? null para tratar undefined igual que null (defensivo).
        const ptHandleOut  = pt.handleOut  ?? null;
        const nextHandleIn = next.handleIn ?? null;
        const segmentIsCurved = ptHandleOut !== null && nextHandleIn !== null;

        // Segmento curvo → omitir midpoint (ya existe la curva, no tiene sentido
        // insertar un vértice en el medio ni crear otra curva encima).
        if (segmentIsCurved) return null;

        const mx = (pt.absX + next.absX) / 2;
        const my = (pt.absY + next.absY) / 2;
        return (
          <MidpointHandle
            key={`mid-${i}`}
            mx={mx} my={my}
            segmentIdx={i}
            pt={pt} next={next}
            elementId={element.id}
            onMidpointClick={onMidpointClick}
            onSegmentCurve={onSegmentCurve}
          />
        );
      })}

      {/* ── Handles de control Bézier ───────────────────────────────────── */}
      {/* Renderizados DESPUÉS de midpoints → z-order superior → capturan   */}
      {/* los clicks/drags antes que cualquier midpoint adyacente.           */}
      {absPoints.map((pt, i) => (
        <BezierHandles
          key={`handles-${i}`}
          pt={pt}
          vertexIdx={i}
          elementId={element.id}
          onHandleDrag={onHandleDrag}
          onHandleDragEnd={onHandleDragEnd}
          onHandleRightClick={onHandleRightClick}
          onClearCurve={onClearCurve}
        />
      ))}

      {/* ── Vértices ancla arrastrables ─────────────────────────────────── */}
      {/* Encima de todo — siempre tienen prioridad de click sobre handles   */}
      {/* y midpoints cuando se superponen en esquinas.                      */}
      {absPoints.map((pt, i) => (
        <AnchorVertex
          key={`v-${i}`}
          pt={pt}
          vertexIdx={i}
          elementId={element.id}
          absPoints={absPoints}
          otherElements={otherElements}
          onVertexDrag={onVertexDrag}
          onVertexDragEnd={onVertexDragEnd}
          onVertexRightClick={onVertexRightClick}
          onVertexGuideChange={onVertexGuideChange}
        />
      ))}
    </>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-componentes internos
// ─────────────────────────────────────────────────────────────────────────────

/** Líneas finas "bigotes" que conectan cada vértice con sus handles. */
function BezierWhiskers({ pt }) {
  return (
    <>
      {pt.absHandleIn && (
        <Line
          points={[pt.absX, pt.absY, pt.absHandleIn.x, pt.absHandleIn.y]}
          stroke="rgba(167,139,250,0.45)"
          strokeWidth={1}
          dash={[3, 3]}
          listening={false}
        />
      )}
      {pt.absHandleOut && (
        <Line
          points={[pt.absX, pt.absY, pt.absHandleOut.x, pt.absHandleOut.y]}
          stroke="rgba(167,139,250,0.45)"
          strokeWidth={1}
          dash={[3, 3]}
          listening={false}
        />
      )}
    </>
  );
}

/**
 * Círculos arrastrables de los handles de control Bézier.
 *
 * Señal visual del estado simétrico/independiente (prop `symmetric` del vértice):
 *   • Simétrico  (default): hueco, stroke morado — mismo estilo base que siempre tuvo.
 *   • Independiente:        relleno ámbar tenue + stroke ámbar — claramente distinto,
 *                           sin confundirse con vértices (morado sólido), midpoints
 *                           (morado semitransparente) ni guías (rosa/violeta).
 *
 * Llevan name="bezier-handle-{elementId}" para que handleStageMouseDown
 * en LayoutEditorCanvas los reconozca como clicks internos al modo edición
 * y NO expulse al usuario cuando inicia el drag de un handle.
 */
function BezierHandles({ pt, vertexIdx, elementId, onHandleDrag, onHandleDragEnd, onHandleRightClick, onClearCurve }) {
  // El estado simétrico/independiente vive en el vértice ancla, no en el handle.
  // Ambos handles (handleIn y handleOut) del mismo vértice comparten el mismo estado.
  const isSymmetric = pt.symmetric !== false; // true por defecto si undefined

  const fillColor   = isSymmetric ? 'transparent'           : 'rgba(245,158,11,0.15)';
  const strokeColor = isSymmetric ? '#A78BFA'               : '#F59E0B';

  const renderHandle = (absHandle, side) => (
    <Circle
      name={`bezier-handle-${elementId}`}
      x={absHandle.x}
      y={absHandle.y}
      radius={HANDLE_RADIUS}
      fill={fillColor}
      stroke={strokeColor}
      strokeWidth={1.5}
      draggable
      onDragMove={(e) => {
        const node = e.target;
        onHandleDrag?.(vertexIdx, side, node.x(), node.y());
      }}
      onDragEnd={(e) => {
        onHandleDragEnd?.(vertexIdx, side, e.target.x(), e.target.y());
      }}
      onContextMenu={(e) => {
        e.evt.preventDefault();
        if (e.evt.shiftKey) {
          // Shift+click derecho → eliminar la curva del segmento adyacente
          onClearCurve?.(vertexIdx, side);
        } else {
          // Click derecho simple → toggle simétrico/independiente
          onHandleRightClick?.(vertexIdx);
        }
      }}
    />
  );

  return (
    <>
      {pt.absHandleIn  && renderHandle(pt.absHandleIn,  'in')}
      {pt.absHandleOut && renderHandle(pt.absHandleOut, 'out')}
    </>
  );
}

/**
 * Punto medio de un segmento.
 * Solo se renderiza para segmentos RECTOS (sin curva).
 * - Click simple → insertar vértice (onMidpointClick)
 * - Alt + DragStart → curvar el segmento (onSegmentCurve)
 */
function MidpointHandle({ mx, my, segmentIdx, pt, next, elementId, onMidpointClick, onSegmentCurve }) {
  // Guardamos si el drag fue iniciado con Alt
  const altDragRef = useRef(false);
  // Posición inicial del midpoint al empezar el drag (para calcular deltas)
  const dragStartRef = useRef({ x: 0, y: 0 });

  return (
    <Circle
      x={mx} y={my}
      radius={MIDPOINT_RADIUS}
      name={`midpoint-handle-${elementId}`}
      fill="rgba(167,139,250,0.45)"
      stroke="rgba(167,139,250,0.85)"
      strokeWidth={1}
      draggable
      onDragStart={(e) => {
        altDragRef.current = e.evt.altKey;
        dragStartRef.current = { x: e.target.x(), y: e.target.y() };
        if (!e.evt.altKey) {
          // Sin Alt: no queremos drag — cancelamos inmediatamente
          e.target.stopDrag();
        }
      }}
      onDragMove={(e) => {
        if (!altDragRef.current) return;
        const node  = e.target;
        const currX = node.x();
        const currY = node.y();
        const dx    = currX - dragStartRef.current.x;
        const dy    = currY - dragStartRef.current.y;

        // Calcular handles a 1/3 y 2/3 desplazados por el delta del drag.
        // pt.absX/absY y next.absX/absY son las posiciones absolutas de los
        // vértices ancla del segmento (ya calculadas en el componente padre).
        const baseHOut = {
          x: pt.absX   + (next.absX - pt.absX)   * (1 / 3) + dx,
          y: pt.absY   + (next.absY - pt.absY)   * (1 / 3) + dy,
        };
        const baseHIn = {
          x: next.absX - (next.absX - pt.absX) * (1 / 3) + dx,
          y: next.absY - (next.absY - pt.absY) * (1 / 3) + dy,
        };

        onSegmentCurve?.(segmentIdx, baseHOut, baseHIn);
      }}
      onDragEnd={() => {
        altDragRef.current = false;
        // La curva ya quedó persistida por onDragMove — no hace falta re-emitir
      }}
      onClick={(e) => {
        // Solo insertar si no fue un alt-drag
        if (!e.evt.altKey) onMidpointClick(segmentIdx);
      }}
      onTap={() => onMidpointClick(segmentIdx)}
    />
  );
}

/** Vértice ancla arrastrable con snap y smart guides. */
function AnchorVertex({ pt, vertexIdx, elementId, absPoints, otherElements, onVertexDrag, onVertexDragEnd, onVertexRightClick, onVertexGuideChange }) {
  return (
    <Circle
      x={pt.absX} y={pt.absY}
      radius={VERTEX_RADIUS}
      name={`vertex-handle-${elementId}`}
      fill="#A78BFA"
      stroke="#ffffff"
      strokeWidth={1.5}
      draggable
      onDragMove={(e) => {
        const node = e.target;
        const absX = node.x();
        const absY = node.y();

        // Otros vértices absolutos (excepto el activo) → para snap interno
        const ownOthers = absPoints
          .filter((_, idx) => idx !== vertexIdx)
          .map((p) => ({ x: p.absX, y: p.absY }));

        const guides = findVertexSnapGuides(
          { x: absX, y: absY },
          ownOthers,
          otherElements ?? [],
          6,
        );

        if (guides.vertical)   node.x(absX + guides.vertical.delta);
        if (guides.horizontal) node.y(absY + guides.horizontal.delta);

        const snappedAbs = { x: node.x(), y: node.y() };
        onVertexGuideChange?.(computeVertexGuideLines(snappedAbs, guides));
        onVertexDrag(vertexIdx, node.x(), node.y());
      }}
      onDragEnd={(e) => {
        onVertexGuideChange?.({ vertical: null, horizontal: null });
        onVertexDragEnd(vertexIdx, e.target.x(), e.target.y());
      }}
      onContextMenu={(e) => {
        e.evt.preventDefault();
        onVertexRightClick(vertexIdx);
      }}
    />
  );
}
