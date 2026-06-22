import { Circle } from 'react-konva';
import {
  findVertexSnapGuides,
  polyBoundingBox,
} from './layoutEditorUtils';

const VERTEX_RADIUS   = 7;
const MIDPOINT_RADIUS = 5;
const GUIDE_MARGIN    = 10;

/**
 * Calcula la geometría de línea corta { x1,y1,x2,y2 } para una guía de vértice.
 * Extraído de SectionElement (Fase 1.7) y movido aquí al refactorizar a layer propia.
 *
 * @param {{ x, y }} snappedAbs  — posición absoluta del vértice tras aplicar snap
 * @param {object}   guides      — resultado de findVertexSnapGuides
 * @returns {{ vertical: LineGeom|null, horizontal: LineGeom|null }}
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
 * Renderiza los handles de vértices y puntos medios del polígono en edición.
 * Vive en su propia Layer, completamente separado del Group draggable de la sección,
 * por lo que el drag de un vértice nunca puede propagar al Group padre.
 *
 * Todas las coordenadas son ABSOLUTAS (canvas-space), ya que esta Layer
 * no tiene transformación local de ningún Group.
 *
 * Props:
 *   element            — sección en edición (con polygonPoints, x, y)
 *   previewPoints      — [[rx,ry], ...] puntos en tiempo real durante el drag
 *   onVertexDrag       — (idx, absX, absY) => void  — preview en tiempo real
 *   onVertexDragEnd    — (idx, absX, absY) => void  — commit al soltar
 *   onMidpointClick    — (insertAfterIdx) => void
 *   onVertexRightClick — (idx) => void
 *   onVertexGuideChange — ({ vertical, horizontal }) => void
 *   otherElements      — elementos del canvas excepto la sección en edición
 */
export default function VertexEditorOverlay({
  element,
  previewPoints,
  onVertexDrag,
  onVertexDragEnd,
  onMidpointClick,
  onVertexRightClick,
  onVertexGuideChange,
  otherElements,
}) {
  if (!element) return null;

  // Usar previewPoints si está disponible (durante drag), sino los puntos del elemento
  const points = previewPoints ?? element.polygonPoints;
  if (!points || points.length < 3) return null;

  // Convertir de coordenadas relativas al elemento → absolutas del canvas
  const absPoints = points.map(([rx, ry]) => ({
    x: element.x + rx,
    y: element.y + ry,
  }));

  return (
    <>
      {/* ── Puntos medios — insertar vértice ───────────────────────────── */}
      {absPoints.map((pt, i) => {
        const next = absPoints[(i + 1) % absPoints.length];
        const mx   = (pt.x + next.x) / 2;
        const my   = (pt.y + next.y) / 2;
        return (
          <Circle
            key={`mid-${i}`}
            name={`midpoint-handle-${element.id}`}
            x={mx} y={my}
            radius={MIDPOINT_RADIUS}
            fill="rgba(167,139,250,0.45)"
            stroke="rgba(167,139,250,0.85)"
            strokeWidth={1}
            onClick={() => onMidpointClick(i)}
            onTap={() => onMidpointClick(i)}
          />
        );
      })}

      {/* ── Vértices arrastrables ───────────────────────────────────────── */}
      {absPoints.map((pt, i) => (
        <Circle
          key={`v-${i}`}
          name={`vertex-handle-${element.id}`}
          x={pt.x} y={pt.y}
          radius={VERTEX_RADIUS}
          fill="#A78BFA"
          stroke="#ffffff"
          strokeWidth={1.5}
          draggable
          onDragMove={(e) => {
            const node  = e.target;
            const absX  = node.x();
            const absY  = node.y();

            // Otros vértices absolutos (excepto el activo) → para snap interno
            const ownOthers = absPoints
              .filter((_, idx) => idx !== i);

            const guides = findVertexSnapGuides(
              { x: absX, y: absY },
              ownOthers,
              otherElements ?? [],
              6,
            );

            // Aplicar snap al nodo Konva
            if (guides.vertical)   node.x(absX + guides.vertical.delta);
            if (guides.horizontal) node.y(absY + guides.horizontal.delta);

            const snappedAbs = { x: node.x(), y: node.y() };

            // Calcular y emitir geometría de líneas guía
            onVertexGuideChange?.(computeVertexGuideLines(snappedAbs, guides));

            // Notificar posición para preview en tiempo real
            onVertexDrag(i, node.x(), node.y());
          }}
          onDragEnd={(e) => {
            onVertexGuideChange?.({ vertical: null, horizontal: null });
            onVertexDragEnd(i, e.target.x(), e.target.y());
          }}
          onContextMenu={(e) => {
            e.evt.preventDefault();
            onVertexRightClick(i);
          }}
        />
      ))}
    </>
  );
}
