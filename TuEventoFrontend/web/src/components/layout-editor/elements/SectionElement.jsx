import { useRef, useEffect } from 'react';
import { Group, Rect, Shape, Circle, Text, Transformer } from 'react-konva';
import {
  computeMinSectionSize,
  distributeSeats,
  polyCentroid,
  snapToGrid,
  migratePolygonPoints,
} from '../layoutEditorUtils';

const VERTEX_RADIUS = 7; // eslint-disable-line no-unused-vars — reservado por si se necesita

/**
 * Construye el path del polígono en el contexto 2D de Konva.
 * Compartida entre sceneFunc y hitFunc del <Shape> para garantizar que
 * el hit-test siga exactamente el contorno visual — incluyendo curvas Bézier.
 * Si algún punto tiene handleOut y el siguiente tiene handleIn → bezierCurveTo.
 * En cualquier otro caso → lineTo (segmento recto).
 *
 * @param {CanvasRenderingContext2D} context
 * @param {Array<{x,y,handleIn,handleOut}>} points  — formato nuevo (migrado)
 */
function drawPolygonPath(context, points) {
  if (!points || points.length < 2) return;
  context.beginPath();
  context.moveTo(points[0].x, points[0].y);
  for (let i = 0; i < points.length; i++) {
    const curr = points[i];
    const next = points[(i + 1) % points.length];
    if (curr.handleOut && next.handleIn) {
      context.bezierCurveTo(
        curr.handleOut.x, curr.handleOut.y,
        next.handleIn.x,  next.handleIn.y,
        next.x,           next.y,
      );
    } else {
      context.lineTo(next.x, next.y);
    }
  }
  context.closePath();
}

/**
 * Calcula el bounding box visual del polígono incluyendo los puntos de control
 * Bézier (handleIn/handleOut). Los puntos de control de una cúbica siempre
 * contienen a la curva real, así que este BB es una cota segura para el
 * Transformer — sin necesidad de tesela la curva punto a punto.
 *
 * @param {Array<{x,y,handleIn?,handleOut?}>} points — formato nuevo (migrado)
 * @returns {{ minX, minY, maxX, maxY, width, height }}
 */
function computePolygonVisualBB(points) {
  if (!points || points.length === 0) {
    return { minX: 0, minY: 0, maxX: 0, maxY: 0, width: 0, height: 0 };
  }
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
  const expand = (x, y) => {
    if (x < minX) minX = x; if (x > maxX) maxX = x;
    if (y < minY) minY = y; if (y > maxY) maxY = y;
  };
  for (const pt of points) {
    expand(pt.x, pt.y);
    if (pt.handleIn)  expand(pt.handleIn.x,  pt.handleIn.y);
    if (pt.handleOut) expand(pt.handleOut.x, pt.handleOut.y);
  }
  return { minX, minY, maxX, maxY, width: maxX - minX, height: maxY - minY };
}

function darkenHex(hex, amount = 40) {
  const num = parseInt((hex || '#000000').replace('#', ''), 16);
  const r   = Math.max(0, (num >> 16)         - amount);
  const g   = Math.max(0, ((num >> 8) & 0xff) - amount);
  const b   = Math.max(0, (num & 0xff)         - amount);
  return `rgb(${r},${g},${b})`;
}

/**
 * Renderiza una sección del layout (rectángulo o polígono libre).
 *
 * Fase 1.8: los handles de vértices se movieron a VertexEditorOverlay
 * (layer independiente). Este componente ya no gestiona edición de vértices —
 * solo notifica al padre via onEnterVertexEdit cuando el usuario hace doble-click.
 *
 * Props:
 *   element           — objeto de sección
 *   isSelected        — bool
 *   isEditingVertices — bool: cuando true, la sección muestra borde de edición
 *                       pero los handles no viven aquí
 *   previewPoints     — [[rx,ry], ...] | null: puntos en tiempo real durante drag
 *                       de vértices (calculados en LayoutEditorCanvas)
 *   onSelect          — () => void
 *   onChange          — (updatedElement) => void
 *   onDragMove        — (id, { x, y }) => { dx, dy }   (Fase 1.6 smart guides)
 *   onDragEnd         — (updatedElement) => void         (Fase 1.6 smart guides)
 *   onGroupDragStart  — multi-drag
 *   onGroupDragMove   — multi-drag
 *   onGroupDragEnd    — multi-drag
 *   onEnterVertexEdit — () => void: doble-click activa modo edición en el padre
 */
export default function SectionElement({
  element,
  isSelected,
  isEditingVertices,
  previewPoints,
  onSelect,
  onChange,
  onDragMove,
  onDragEnd,
  onGroupDragStart,
  onGroupDragMove,
  onGroupDragEnd,
  onEnterVertexEdit,
}) {
  const groupRef = useRef();
  const trRef    = useRef();

  const shapeMode = element.shapeMode ?? 'rect';
  const minSize   = computeMinSectionSize(element.seatLayout);

  // Puntos de trabajo: previewPoints durante edición (tiempo real), luego los del elemento.
  // Declarado ANTES del useEffect del Transformer para que pueda listarse como dependencia.
  const workPoints = (() => {
    const raw = (isEditingVertices && previewPoints) ? previewPoints : element.polygonPoints;
    return raw ? migratePolygonPoints(raw) : raw;
  })();

  // BB visual real del polígono, incluyendo puntos de control Bézier.
  // El Transformer usa estas dimensiones para no quedarse "congelado" al curvar.
  const visualBB = (shapeMode === 'polygon' && workPoints)
    ? computePolygonVisualBB(workPoints)
    : null;
  const groupWidth  = visualBB ? Math.max(minSize.width,  visualBB.width)  : element.width;
  const groupHeight = visualBB ? Math.max(minSize.height, visualBB.height) : element.height;

  // ── Transformer ───────────────────────────────────────────────────────────
  // Se fuerza forceUpdate() cuando workPoints cambia (curvar segmento, mover
  // handle, mover vértice) para que el cuadro siga la geometría real.
  useEffect(() => {
    if (isSelected && !isEditingVertices && trRef.current && groupRef.current) {
      trRef.current.nodes([groupRef.current]);
      trRef.current.forceUpdate();
      trRef.current.getLayer()?.batchDraw();
    } else if (trRef.current) {
      trRef.current.nodes([]);
      trRef.current.getLayer()?.batchDraw();
    }
  }, [isSelected, isEditingVertices, workPoints]); // workPoints → re-evalúa al curvar/mover handles

  const seatPositions = (() => {
    if ((element.shapeMode ?? 'rect') === 'polygon' && workPoints) {
      return distributeSeats({ ...element, polygonPoints: workPoints });
    }
    return distributeSeats(element);
  })();

  const labelCenter = shapeMode === 'polygon' && workPoints
    ? polyCentroid(workPoints)
    : { x: element.width / 2, y: element.height / 2 };

  // ── Drag ──────────────────────────────────────────────────────────────────
  const handleDragStart = (e) => {
    // Durante edición de vértices el Group no debería ser draggable,
    // pero si por algún edge case llega aquí, cancelarlo.
    if (isEditingVertices) { e.target.stopDrag(); return; }
    if (onGroupDragStart) onGroupDragStart(element.id, { x: e.target.x(), y: e.target.y() });
  };

  const handleDragMove = (e) => {
    if (isEditingVertices) return;
    if (onGroupDragMove) {
      onGroupDragMove(element.id, { x: e.target.x(), y: e.target.y() });
    } else if (onDragMove) {
      const result = onDragMove(element.id, { x: e.target.x(), y: e.target.y() });
      if (result && (result.dx !== 0 || result.dy !== 0)) {
        e.target.x(e.target.x() + result.dx);
        e.target.y(e.target.y() + result.dy);
      }
    }
  };

  const handleDragEnd = (e) => {
    if (isEditingVertices) return;
    if (onGroupDragEnd) {
      onGroupDragEnd(element.id, { x: e.target.x(), y: e.target.y() });
    } else if (onDragEnd) {
      onDragEnd({ ...element, x: snapToGrid(e.target.x()), y: snapToGrid(e.target.y()) });
    } else {
      onChange({ ...element, x: snapToGrid(e.target.x()), y: snapToGrid(e.target.y()) });
    }
  };

  // ── Transformer end ───────────────────────────────────────────────────────
  const handleTransformEnd = () => {
    const node   = groupRef.current;
    const scaleX = node.scaleX();
    const scaleY = node.scaleY();
    node.scaleX(1); node.scaleY(1);
    const newW = snapToGrid(Math.max(minSize.width,  node.width()  * scaleX));
    const newH = snapToGrid(Math.max(minSize.height, node.height() * scaleY));
    let patch = {
      x: snapToGrid(node.x()), y: snapToGrid(node.y()),
      width: newW, height: newH, rotation: node.rotation(),
    };
    if (shapeMode === 'polygon' && element.polygonPoints) {
      const pts = migratePolygonPoints(element.polygonPoints);
      // Usar BB visual (incluye handles) para que el escalado no deje handles fuera
      const bb  = computePolygonVisualBB(pts);
      const ratioX = bb.width  > 0 ? newW / bb.width  : 1;
      const ratioY = bb.height > 0 ? newH / bb.height : 1;
      patch.polygonPoints = pts.map((pt) => ({
        ...pt,
        x: bb.minX + (pt.x - bb.minX) * ratioX,
        y: bb.minY + (pt.y - bb.minY) * ratioY,
        handleIn:  pt.handleIn  ? { x: bb.minX + (pt.handleIn.x  - bb.minX) * ratioX, y: bb.minY + (pt.handleIn.y  - bb.minY) * ratioY } : null,
        handleOut: pt.handleOut ? { x: bb.minX + (pt.handleOut.x - bb.minX) * ratioX, y: bb.minY + (pt.handleOut.y - bb.minY) * ratioY } : null,
      }));
    }
    onChange({ ...element, ...patch });
  };

  const handleDoubleClick = () => {
    if (shapeMode === 'polygon' && onEnterVertexEdit) onEnterVertexEdit();
  };

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <>
      <Group
        ref={groupRef}
        x={element.x}
        y={element.y}
        width={groupWidth}
        height={groupHeight}
        rotation={element.rotation ?? 0}
        // Fase 1.8: el Group es SIEMPRE draggable — los vértices ya no viven aquí,
        // así que no hay conflicto estructural de drag.
        draggable={!isEditingVertices}
        onClick={isEditingVertices ? undefined : onSelect}
        onTap={isEditingVertices ? undefined : onSelect}
        onDblClick={handleDoubleClick}
        onDblTap={handleDoubleClick}
        onDragStart={handleDragStart}
        onDragMove={handleDragMove}
        onDragEnd={handleDragEnd}
        onTransformEnd={handleTransformEnd}
      >
        {/* ── Fondo ────────────────────────────────────────────────────── */}
        {shapeMode === 'polygon' && workPoints ? (
          <>
            {/* Rect invisible que fija las dimensiones del Group para que
                el Transformer pueda calcular getClientRect() correctamente.
                <Shape> con sceneFunc no tiene dimensiones implícitas en Konva —
                el Transformer usaría solo el bbox de sillas+label sin esto. */}
            <Rect
              x={visualBB ? visualBB.minX : 0}
              y={visualBB ? visualBB.minY : 0}
              width={visualBB ? visualBB.width : groupWidth}
              height={visualBB ? visualBB.height : groupHeight}
              fill="transparent"
              stroke="transparent"
              strokeWidth={0}
              listening={false}
              perfectDrawEnabled={false}
            />
            <Shape
              name={`polygon-shape-${element.id}`}
              sceneFunc={(context, shape) => {
                drawPolygonPath(context, workPoints);
                context.fillStrokeShape(shape);
              }}
              hitFunc={(context, shape) => {
                // Mismo path que sceneFunc — garantiza que el hit-test siga el
                // contorno real (incluidas curvas Bézier), no el bounding box.
                drawPolygonPath(context, workPoints);
                context.fillStrokeShape(shape);
              }}
              fill={element.color}
              opacity={0.85}
              stroke={isSelected || isEditingVertices ? '#ffffff' : darkenHex(element.color)}
              strokeWidth={isSelected || isEditingVertices ? 2 : 1}
              listening={true}
            />
          </>
        ) : (
          <Rect
            width={element.width}
            height={element.height}
            fill={element.color}
            opacity={0.85}
            cornerRadius={6}
            stroke={isSelected ? '#ffffff' : 'rgba(255,255,255,0.3)'}
            strokeWidth={isSelected ? 2 : 1}
          />
        )}

        {/* ── Sillas ───────────────────────────────────────────────────── */}
        {!isEditingVertices && seatPositions.map((pos, i) => (
          <Circle
            key={i}
            x={pos.x} y={pos.y} radius={pos.r}
            fill="rgba(255,255,255,0.75)"
            stroke="rgba(255,255,255,0.4)"
            strokeWidth={0.5}
            listening={false}
          />
        ))}

        {/* ── Label ────────────────────────────────────────────────────── */}
        <Text
          x={labelCenter.x - element.width / 2}
          y={labelCenter.y - 8}
          width={element.width}
          text={element.label}
          fontSize={13}
          fontStyle="bold"
          fill="#ffffff"
          align="center"
          listening={false}
        />
      </Group>

      {/* Transformer — solo cuando está seleccionado y fuera del modo edición */}
      {isSelected && !isEditingVertices && (
        <Transformer
          ref={trRef}
          rotateEnabled
          enabledAnchors={[
            'top-left', 'top-right', 'bottom-left', 'bottom-right',
            'middle-left', 'middle-right', 'top-center', 'bottom-center',
          ]}
          boundBoxFunc={(oldBox, newBox) => {
            if (newBox.width < minSize.width || newBox.height < minSize.height) return oldBox;
            return newBox;
          }}
        />
      )}
    </>
  );
}
