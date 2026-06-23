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
 *  Puntos medios (círculos semitransparentes, en mitad de cada segmento)
 *    • Click simple       — inserta un vértice nuevo en ese punto.
 *    • Alt + arrastrar    — convierte ese segmento en una curva Bézier cúbica.
 *                           Los dos handles aparecen a 1/3 y 2/3 del segmento
 *                           y se desplazan juntos simétricamente mientras arrastras.
 *
 *  Handles de control (círculos pequeños huecos, conectados al ancla por línea punteada)
 *    • Arrastrar          — ajusta la curvatura del segmento adyacente.
 *                           Si el vértice es simétrico (por defecto), el handle
 *                           opuesto se mueve en espejo automáticamente → curva suave.
 *    • Click derecho      — alterna entre simétrico e independiente para ese vértice.
 *                           En modo independiente, cada handle se mueve por separado
 *                           → esquina con curvatura distinta en cada lado.
 *
 * ── Notas de implementación ─────────────────────────────────────────────────
 *
 *  • Esta Layer vive separada del Group draggable de la sección (Fase 1.8),
 *    por lo que el drag de un handle/vértice nunca propaga al Group padre.
 *  • Todas las coordenadas aquí son ABSOLUTAS (canvas-space). La conversión
 *    relativo↔absoluto se hace con element.x/element.y al subir/bajar callbacks.
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

  // ── Ref para el drag de Alt+midpoint ────────────────────────────────────
  // (necesitamos guardar el estado del drag de curvatura entre eventos)
  // Se usa un ref global en el componente para no perder el estado entre renders.
  // El patrón funciona porque los Konva event handlers son síncrónos en un mismo frame.

  return (
    <>
      {/* ── Líneas "bigotes" de handles Bézier ─────────────────────────── */}
      {absPoints.map((pt, i) => (
        <BezierWhiskers key={`whiskers-${i}`} pt={pt} />
      ))}

      {/* ── Puntos medios — insertar vértice o Alt+drag para curvar ──────── */}
      {/* Renderizados ANTES que los handles para que los handles queden      */}
      {/* en z-order superior y ganen el hit-test cuando se superponen.       */}
      {absPoints.map((pt, i) => {
        const next = absPoints[(i + 1) % absPoints.length];

        // Si el segmento ya es curvo (ambos handles existen), omitir el midpoint:
        // evita que compita con los handles de Bézier en hit-testing y z-order.
        // El flujo de Alt+drag para CREAR la curva solo aplica a segmentos rectos,
        // así que este midpoint no hace falta una vez que la curva ya existe.
        const segmentIsCurved = pt.handleOut !== null && next.handleIn !== null;
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
          onHandleDrag={onHandleDrag}
          onHandleDragEnd={onHandleDragEnd}
          onHandleRightClick={onHandleRightClick}
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

/** Círculos arrastrables de los handles de control Bézier. */
function BezierHandles({ pt, vertexIdx, onHandleDrag, onHandleDragEnd, onHandleRightClick }) {
  const renderHandle = (absHandle, side) => (
    // key no tiene efecto aquí (no es array de JSX) pero lo dejamos para claridad.
    // El key real lo pone el padre en <BezierHandles key={...} />.
    <Circle
      x={absHandle.x}
      y={absHandle.y}
      radius={HANDLE_RADIUS}
      fill="transparent"
      stroke="#A78BFA"
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
        onHandleRightClick?.(vertexIdx);
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
        const node    = e.target;
        const currX   = node.x();
        const currY   = node.y();
        const dx      = currX - dragStartRef.current.x;
        const dy      = currY - dragStartRef.current.y;

        // Calcular handles a 1/3 y 2/3 desplazados por el delta del drag
        // Punto ancla i (pt) y ancla siguiente (next)
        const baseHOut = {
          x: pt.absX   + (next.absX - pt.absX)   * (1 / 3) + dx,
          y: pt.absY   + (next.absY - pt.absY)   * (1 / 3) + dy,
        };
        const baseHIn  = {
          x: next.absX - (next.absX - pt.absX) * (1 / 3) + dx,
          y: next.absY - (next.absY - pt.absY) * (1 / 3) + dy,
        };

        onSegmentCurve?.(segmentIdx, baseHOut, baseHIn);
      }}
      onDragEnd={(e) => {
        if (!altDragRef.current) {
          // Era un click (drag cancelado), no hacer nada especial
          return;
        }
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
