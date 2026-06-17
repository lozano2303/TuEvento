import React, { useRef, useEffect, useState, useCallback } from 'react';
import { Group, Rect, Line, Circle, Text, Transformer } from 'react-konva';
import {
  computeSeatPositions,
  computeMinSectionSize,
  computePolygonSeatRows,
  flattenPoints,
  polyCentroid,
  polyBoundingBox,
  snapToGrid,
} from '../layoutEditorUtils';

const VERTEX_RADIUS   = 7;
const MIDPOINT_RADIUS = 5;

function darkenHex(hex, amount = 40) {
  const num = parseInt((hex || '#000000').replace('#', ''), 16);
  const r   = Math.max(0, (num >> 16)        - amount);
  const g   = Math.max(0, ((num >> 8) & 0xff) - amount);
  const b   = Math.max(0, (num & 0xff)        - amount);
  return `rgb(${r},${g},${b})`;
}

export default function SectionElement({
  element,
  isSelected,
  isEditingVertices,  // Fase 1.3
  onSelect,
  onChange,
  onGroupDragStart,
  onGroupDragMove,
  onGroupDragEnd,
  onStartVertexEdit,  // Fase 1.3: () => void
  onEndVertexEdit,    // Fase 1.3: () => void
}) {
  const groupRef = useRef();
  const trRef    = useRef();
  const [localPoints, setLocalPoints] = useState(null);

  // Sincronizar localPoints al entrar/salir del modo edición
  useEffect(() => {
    if (isEditingVertices && element.polygonPoints) {
      setLocalPoints(element.polygonPoints.map((p) => [...p]));
    } else {
      setLocalPoints(null);
    }
  }, [isEditingVertices]); // eslint-disable-line react-hooks/exhaustive-deps

  // Transformer: adjuntar solo cuando seleccionado y fuera de modo edición
  useEffect(() => {
    if (isSelected && !isEditingVertices && trRef.current && groupRef.current) {
      trRef.current.nodes([groupRef.current]);
      trRef.current.getLayer()?.batchDraw();
    } else if (trRef.current) {
      trRef.current.nodes([]);
      trRef.current.getLayer()?.batchDraw();
    }
  }, [isSelected, isEditingVertices]);

  // Escape → confirmar y salir
  useEffect(() => {
    if (!isEditingVertices) return;
    const handler = (e) => { if (e.key === 'Escape') commitAndExit(); };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [isEditingVertices, localPoints]); // eslint-disable-line react-hooks/exhaustive-deps

  const shapeMode = element.shapeMode ?? 'rect';
  const minSize   = computeMinSectionSize(element.seatLayout);

  // Sillas: polígono usa el algoritmo de escaneo horizontal, rect usa la grilla simple
  const seatPositions = shapeMode === 'polygon' && element.polygonPoints
    ? computePolygonSeatRows(element.polygonPoints, element.seatLayout)
    : computeSeatPositions(element.width, element.height, element.seatLayout);

  // Centroide para el label
  const labelCenter = shapeMode === 'polygon' && element.polygonPoints
    ? polyCentroid(element.polygonPoints)
    : { x: element.width / 2, y: element.height / 2 };

  // ── Drag ──────────────────────────────────────────────────────────────────
  const handleDragStart = (e) => {
    if (onGroupDragStart) onGroupDragStart(element.id, { x: e.target.x(), y: e.target.y() });
  };
  const handleDragMove = (e) => {
    if (onGroupDragMove) onGroupDragMove(element.id, { x: e.target.x(), y: e.target.y() });
  };
  const handleDragEnd = (e) => {
    if (onGroupDragEnd) {
      onGroupDragEnd(element.id, { x: e.target.x(), y: e.target.y() });
    } else {
      onChange({ ...element, x: snapToGrid(e.target.x()), y: snapToGrid(e.target.y()) });
    }
  };

  // ── Transformer end ───────────────────────────────────────────────────────
  const handleTransformEnd = () => {
    const node   = groupRef.current;
    const scaleX = node.scaleX();
    const scaleY = node.scaleY();
    node.scaleX(1);
    node.scaleY(1);

    const newW = snapToGrid(Math.max(minSize.width,  node.width()  * scaleX));
    const newH = snapToGrid(Math.max(minSize.height, node.height() * scaleY));
    let patch  = { x: snapToGrid(node.x()), y: snapToGrid(node.y()), width: newW, height: newH, rotation: node.rotation() };

    // Polígono: escalar vértices al nuevo bounding box
    if (shapeMode === 'polygon' && element.polygonPoints) {
      const bb     = polyBoundingBox(element.polygonPoints);
      const ratioX = bb.width  > 0 ? newW / bb.width  : 1;
      const ratioY = bb.height > 0 ? newH / bb.height : 1;
      patch.polygonPoints = element.polygonPoints.map(([px, py]) => [
        bb.minX + (px - bb.minX) * ratioX,
        bb.minY + (py - bb.minY) * ratioY,
      ]);
    }
    onChange({ ...element, ...patch });
  };

  // ── Doble-click → entrar en modo edición de vértices ─────────────────────
  const handleDoubleClick = () => {
    if (shapeMode === 'polygon' && onStartVertexEdit) onStartVertexEdit();
  };

  // ── Edición de vértices ───────────────────────────────────────────────────
  const commitAndExit = useCallback(() => {
    if (localPoints) {
      const bb = polyBoundingBox(localPoints);
      onChange({
        ...element,
        polygonPoints: localPoints,
        width:  Math.max(minSize.width,  Math.round(bb.width)),
        height: Math.max(minSize.height, Math.round(bb.height)),
      });
    }
    onEndVertexEdit?.();
  }, [localPoints, element, onChange, onEndVertexEdit, minSize]);

  const handleVertexDragMove = (idx, e) => {
    const node = e.target;
    setLocalPoints((prev) => {
      const next = prev.map((p) => [...p]);
      next[idx] = [node.x(), node.y()];
      return next;
    });
  };

  const handleMidpointClick = (idx) => {
    setLocalPoints((prev) => {
      const next = [...prev];
      const a    = prev[idx];
      const b    = prev[(idx + 1) % prev.length];
      next.splice(idx + 1, 0, [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2]);
      return next;
    });
  };

  const handleVertexRightClick = (idx, e) => {
    e.evt.preventDefault();
    setLocalPoints((prev) => {
      if (prev.length <= 3) return prev;
      return prev.filter((_, i) => i !== idx);
    });
  };

  const workPoints = (isEditingVertices && localPoints) ? localPoints : element.polygonPoints;

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <>
      <Group
        ref={groupRef}
        x={element.x}
        y={element.y}
        width={element.width}
        height={element.height}
        rotation={element.rotation ?? 0}
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
        {/* ── Fondo ──────────────────────────────────────────────────────── */}
        {shapeMode === 'polygon' && workPoints ? (
          <Line
            points={flattenPoints(workPoints)}
            closed
            fill={element.color}
            opacity={0.85}
            stroke={isSelected || isEditingVertices ? '#ffffff' : darkenHex(element.color)}
            strokeWidth={isSelected || isEditingVertices ? 2 : 1}
          />
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

        {/* ── Sillas (ocultas durante edición de vértices) ──────────────── */}
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

        {/* ── Label ─────────────────────────────────────────────────────── */}
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

        {/* ── Controles de vértices (modo edición) ──────────────────────── */}
        {isEditingVertices && workPoints && workPoints.map((pt, idx) => {
          const nextPt = workPoints[(idx + 1) % workPoints.length];
          const midX   = (pt[0] + nextPt[0]) / 2;
          const midY   = (pt[1] + nextPt[1]) / 2;
          return (
            <React.Fragment key={`v-${idx}`}>
              {/* Punto medio: añadir vértice */}
              <Circle
                x={midX} y={midY}
                radius={MIDPOINT_RADIUS}
                fill="rgba(167,139,250,0.45)"
                stroke="rgba(167,139,250,0.85)"
                strokeWidth={1}
                onClick={() => handleMidpointClick(idx)}
                onTap={() => handleMidpointClick(idx)}
              />
              {/* Vértice arrastrable */}
              <Circle
                x={pt[0]} y={pt[1]}
                radius={VERTEX_RADIUS}
                fill="#A78BFA"
                stroke="#ffffff"
                strokeWidth={1.5}
                draggable
                onDragMove={(e) => handleVertexDragMove(idx, e)}
                onContextMenu={(e) => handleVertexRightClick(idx, e)}
              />
            </React.Fragment>
          );
        })}
      </Group>

      {/* Transformer: visible solo cuando seleccionado y fuera de edición */}
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

      {/* Botón flotante "Listo" durante edición de vértices */}
      {isEditingVertices && (
        <Group
          x={element.x + element.width / 2 - 36}
          y={element.y - 32}
          onClick={commitAndExit}
          onTap={commitAndExit}
        >
          <Rect width={72} height={24} fill="#7C3AED" cornerRadius={6} />
          <Text
            x={0} y={5} width={72}
            text="✓ Listo"
            fontSize={11} fontStyle="bold"
            fill="#ffffff" align="center"
            listening={false}
          />
        </Group>
      )}
    </>
  );
}
