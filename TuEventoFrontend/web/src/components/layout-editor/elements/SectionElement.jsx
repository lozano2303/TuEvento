import { useRef, useEffect, useMemo, useState, useCallback } from 'react';
import { Group, Rect, Shape, Text, Transformer } from 'react-konva';
import {
  computeMinSectionSize,
  distributeSeats,
  polyCentroid,
  snapToGrid,
  migratePolygonPoints,
  getElementAABB,
  CANVAS_MARGIN,
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
  canvasSizeRef,
}) {
  const groupRef = useRef();
  const trRef    = useRef();

  const shapeMode = element.shapeMode ?? 'rect';
  const minSize   = computeMinSectionSize(element.seatLayout);

  // Puntos de trabajo: previewPoints durante edición (tiempo real), luego los del elemento.
  // useMemo evita recalcular migratePolygonPoints en renders que no cambian los puntos
  // (p.ej. cambios de color, label, etc. que re-renderizan SectionElement por otras razones).
  const workPoints = useMemo(() => {
    const raw = (isEditingVertices && previewPoints) ? previewPoints : element.polygonPoints;
    return raw ? migratePolygonPoints(raw) : raw;
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isEditingVertices, previewPoints, element.polygonPoints]);

  // BB visual real del polígono, incluyendo puntos de control Bézier.
  // useMemo: solo recalcular cuando workPoints cambia, no en cada render.
  const visualBB = useMemo(() => {
    if (shapeMode !== 'polygon' || !workPoints) return null;
    return computePolygonVisualBB(workPoints);
  }, [shapeMode, workPoints]);
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

  const seatPositions = useMemo(() => {
    if ((element.shapeMode ?? 'rect') === 'polygon' && workPoints) {
      return distributeSeats({ ...element, polygonPoints: workPoints });
    }
    return distributeSeats(element);
  // distributeSeats depende de element.seatLayout y la geometría — workPoints captura la geometría
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [element.seatLayout, element.shapeMode, element.width, element.height, workPoints]);

  const labelCenter = useMemo(() => {
    if (shapeMode === 'polygon' && workPoints) return polyCentroid(workPoints);
    return { x: element.width / 2, y: element.height / 2 };
  }, [shapeMode, workPoints, element.width, element.height]);

  // ── Label flotante — posición en canvas-space (fuera del Group rotado) ────
  // Calculamos el AABB del grupo en coordenadas del Layer usando getClientRect.
  // Usamos estado para actualizarlo en tiempo real durante el drag.
  const [labelPos, setLabelPos] = useState({ x: element.x + element.width / 2, y: element.y - 18 });

  const updateLabelPos = useCallback(() => {
    const node = groupRef.current;
    if (!node) return;
    const r = node.getClientRect({ relativeTo: node.getLayer() });
    // Clamp: el label nunca queda por encima del margen mínimo del canvas
    const clampedY = Math.max(CANVAS_MARGIN / 2, r.y - 18);
    setLabelPos({ x: r.x + r.width / 2, y: clampedY });
  }, []);

  // Sync cuando la geometría del elemento cambia — no en cada render arbitrario
  // (sin deps correría tras setLabelPos → loop infinito de renders)
  useEffect(() => {
    updateLabelPos();
  }, [updateLabelPos, element.x, element.y, element.width, element.height, element.rotation, workPoints]);

  const handleDragStart = (e) => {
    // Durante edición de vértices el Group no debería ser draggable,
    // pero si por algún edge case llega aquí, cancelarlo.
    if (isEditingVertices) { e.target.stopDrag(); return; }
    if (onGroupDragStart) onGroupDragStart(element.id, { x: e.target.x(), y: e.target.y() });
  };

  const handleDragMove = (e) => {
    if (isEditingVertices) return;
    const node = e.target;
    const cs   = canvasSizeRef?.current ?? { width: 9999, height: 9999 };

    // getClientRect devuelve el AABB visual real en canvas-space (Layer coords)
    // sin ambigüedad sobre el eje de rotación de Konva.
    const clamp = (px, py) => {
      // Movemos el nodo temporalmente y leemos su AABB real
      const origX = node.x(), origY = node.y();
      node.x(px); node.y(py);
      const r = node.getClientRect({ relativeTo: node.getLayer() });
      node.x(origX); node.y(origY);
      let ox = px, oy = py;
      if (r.x < 0)                      ox += -r.x;
      if (r.y < 0)                      oy += -r.y;
      if (r.x + r.width  > cs.width)    ox -= (r.x + r.width)  - cs.width;
      if (r.y + r.height > cs.height)   oy -= (r.y + r.height) - cs.height;
      return { x: ox, y: oy };
    };

    const raw = { x: node.x(), y: node.y() };
    const clamped = clamp(raw.x, raw.y);
    if (clamped.x !== raw.x) node.x(clamped.x);
    if (clamped.y !== raw.y) node.y(clamped.y);
    updateLabelPos();

    if (onGroupDragMove) {
      onGroupDragMove(element.id, { x: clamped.x, y: clamped.y });
    } else if (onDragMove) {
      const result = onDragMove(element.id, { x: clamped.x, y: clamped.y });
      if (result && (result.dx !== 0 || result.dy !== 0)) {
        const afterSnap = clamp(node.x() + result.dx, node.y() + result.dy);
        node.x(afterSnap.x);
        node.y(afterSnap.y);
      }
    }
  };

  const handleDragEnd = (e) => {
    if (isEditingVertices) return;
    const node = e.target;
    const cs   = canvasSizeRef?.current ?? { width: 9999, height: 9999 };
    const r    = node.getClientRect({ relativeTo: node.getLayer() });
    let fx = node.x(), fy = node.y();
    if (r.x < 0)                    fx += -r.x;
    if (r.y < 0)                    fy += -r.y;
    if (r.x + r.width  > cs.width)  fx -= (r.x + r.width)  - cs.width;
    if (r.y + r.height > cs.height) fy -= (r.y + r.height) - cs.height;
    const finalX = snapToGrid(fx);
    const finalY = snapToGrid(fy);
    if (onGroupDragEnd) {
      onGroupDragEnd(element.id, { x: finalX, y: finalY });
    } else if (onDragEnd) {
      onDragEnd({ ...element, x: finalX, y: finalY });
    } else {
      onChange({ ...element, x: finalX, y: finalY });
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
    const cs   = canvasSizeRef?.current ?? { width: 9999, height: 9999 };
    // Usar getClientRect para el AABB real en canvas-space (sin ambigüedad de eje)
    const r    = node.getClientRect({ relativeTo: node.getLayer() });
    let px = node.x(), py = node.y();
    if (r.x < 0)                    px += -r.x;
    if (r.y < 0)                    py += -r.y;
    if (r.x + r.width  > cs.width)  px -= (r.x + r.width)  - cs.width;
    if (r.y + r.height > cs.height) py -= (r.y + r.height) - cs.height;
    let patch = {
      x: snapToGrid(px), y: snapToGrid(py),
      width: newW, height: newH, rotation: node.rotation(),
    };
    if (shapeMode === 'polygon' && element.polygonPoints) {
      const pts = migratePolygonPoints(element.polygonPoints);
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

        {/* ── Sillas — un solo <Shape> con sceneFunc batcheado ────────────
             Un único beginPath + fill() + stroke() para todas las sillas,
             independientemente de cuántas haya. Sin nodos Konva individuales. */}
        {!isEditingVertices && seatPositions.length > 0 && (
          <Shape
            sceneFunc={(ctx) => {
              ctx.beginPath();
              for (const pos of seatPositions) {
                // moveTo obliga a que cada arco sea un subpath independiente,
                // evitando que stroke() dibuje líneas entre sillas consecutivas.
                ctx.moveTo(pos.x + pos.r, pos.y);
                ctx.arc(pos.x, pos.y, pos.r, 0, Math.PI * 2);
              }
              ctx.fillStyle = 'rgba(255,255,255,0.75)';
              ctx.fill();
              ctx.strokeStyle = 'rgba(255,255,255,0.4)';
              ctx.lineWidth = 0.5;
              ctx.stroke();
            }}
            listening={false}
            perfectDrawEnabled={false}
          />
        )}
      </Group>

      {/* ── Label flotante — fuera del Group, no rota con el elemento ───── */}
      <Text
        x={labelPos.x}
        y={labelPos.y}
        text={element.label}
        fontSize={12}
        fontStyle="bold"
        fill="#ffffff"
        align="center"
        offsetX={0}
        shadowColor="black"
        shadowBlur={4}
        shadowOpacity={0.8}
        listening={false}
        perfectDrawEnabled={false}
      />

      {/* Transformer — solo cuando está seleccionado y fuera del modo edición */}
      {isSelected && !isEditingVertices && (
        <Transformer
          ref={trRef}
          rotateEnabled
          rotationSnaps={[0, 45, 90, 135, 180, 225, 270, 315]}
          rotationSnapTolerance={10}
          rotateAnchorOffset={28}
          rotateAnchorCursor="grab"
          enabledAnchors={[
            'top-left', 'top-right', 'bottom-left', 'bottom-right',
            'middle-left', 'middle-right', 'top-center', 'bottom-center',
          ]}
          anchorStyleFunc={(anchor) => {
            if (anchor.hasName('rotater')) {
              anchor.cornerRadius(10);
              anchor.fill('#818cf8');
              anchor.stroke('#6366f1');
              anchor.strokeWidth(2);
              anchor.width(18);
              anchor.height(18);
              anchor.offsetX(9);
              anchor.offsetY(9);
            }
          }}
          boundBoxFunc={(oldBox, newBox) => {
            if (newBox.width < minSize.width || newBox.height < minSize.height) return oldBox;
            return newBox;
          }}
        />
      )}
    </>
  );
}
