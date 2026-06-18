import { useRef, useState, useCallback, useMemo } from 'react';
import { Stage, Layer, Line, Rect } from 'react-konva';
import SectionElement from './elements/SectionElement';
import InfraElement from './elements/InfraElement';
import { generateId, snapToGrid, rectsIntersect } from './layoutEditorUtils';

const GRID_SIZE        = 20;
const ZOOM_MIN         = 0.2;
const ZOOM_MAX         = 3;
const ZOOM_STEP        = 0.1;
const EXPAND_INCREMENT = 200;
const FIT_MARGIN       = 40;   // px de margen para el fit/auto zoom

function buildGridLines(width, height, step) {
  const lines = [];
  for (let x = 0; x <= width; x += step)
    lines.push({ key: `v${x}`, points: [x, 0, x, height] });
  for (let y = 0; y <= height; y += step)
    lines.push({ key: `h${y}`, points: [0, y, width, y] });
  return lines;
}

export default function LayoutEditorCanvas({
  elements,
  selectedIds,
  canvasSize,
  editingPolygonId,
  onSelect,
  onChange,
  onExpandCanvas,
  onGroupDragEnd,
  onStartVertexEdit,
  onEndVertexEdit,
  onAddElement,
  zoom,
  onZoomChange,
  containerRef,
}) {
  const stageRef = useRef();
  const [stagePos, setStagePos] = useState({ x: 0, y: 0 });

  // Fix 1: pan manual con click derecho
  const panState = useRef({ active: false, startPointer: null, startStagePos: null });

  // Fix 2: rubber-band solo con Ctrl
  const [selBox, setSelBox] = useState(null);
  const isRubberBand = useRef(false);

  // Fix 5: drag grupal
  const groupDragState = useRef({
    active: false, leaderId: null, startPositions: {}, leaderStart: null,
  });
  const [followerPositions, setFollowerPositions] = useState({});

  // Fix B: ref al callback commitAndExit del SectionElement activo en edición
  // SectionElement lo registra via onSaveVertexEdit cuando cambia
  const commitVertexEditRef = useRef(null);

  const gridLines = useMemo(
    () => buildGridLines(canvasSize.width, canvasSize.height, GRID_SIZE),
    [canvasSize.width, canvasSize.height]
  );

  // ── Zoom centrado en cursor ───────────────────────────────────────────────
  const handleWheel = useCallback(
    (e) => {
      e.evt.preventDefault();
      const stage    = stageRef.current;
      const oldScale = zoom;
      const pointer  = stage.getPointerPosition();
      const mousePointTo = {
        x: (pointer.x - stage.x()) / oldScale,
        y: (pointer.y - stage.y()) / oldScale,
      };
      const direction = e.evt.deltaY < 0 ? 1 : -1;
      const newScale  = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, +(oldScale + direction * ZOOM_STEP).toFixed(2)));
      const newPos    = {
        x: pointer.x - mousePointTo.x * newScale,
        y: pointer.y - mousePointTo.y * newScale,
      };
      onZoomChange(newScale);
      setStagePos(newPos);
    },
    [zoom, onZoomChange]
  );

  // ── Fix D2: botones de zoom ───────────────────────────────────────────────
  const handleZoomIn = useCallback(() => {
    const next = Math.min(ZOOM_MAX, +(zoom + ZOOM_STEP).toFixed(2));
    onZoomChange(next);
    // Ajustar posición para que el zoom quede centrado en el viewport
    const stageW = containerRef?.current?.clientWidth  ?? 800;
    const stageH = containerRef?.current?.clientHeight ?? 600;
    setStagePos((pos) => {
      const cx = stageW / 2;
      const cy = stageH / 2;
      const mousePointTo = { x: (cx - pos.x) / zoom, y: (cy - pos.y) / zoom };
      return { x: cx - mousePointTo.x * next, y: cy - mousePointTo.y * next };
    });
  }, [zoom, onZoomChange, containerRef]);

  const handleZoomOut = useCallback(() => {
    const next = Math.max(ZOOM_MIN, +(zoom - ZOOM_STEP).toFixed(2));
    onZoomChange(next);
    const stageW = containerRef?.current?.clientWidth  ?? 800;
    const stageH = containerRef?.current?.clientHeight ?? 600;
    setStagePos((pos) => {
      const cx = stageW / 2;
      const cy = stageH / 2;
      const mousePointTo = { x: (cx - pos.x) / zoom, y: (cy - pos.y) / zoom };
      return { x: cx - mousePointTo.x * next, y: cy - mousePointTo.y * next };
    });
  }, [zoom, onZoomChange, containerRef]);

  const handleFit = useCallback(() => {
    const stageW = containerRef?.current?.clientWidth  ?? 800;
    const stageH = containerRef?.current?.clientHeight ?? 600;

    if (elements.length === 0) {
      onZoomChange(1);
      setStagePos({ x: 0, y: 0 });
      return;
    }

    // Bounding box de todos los elementos
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    for (const el of elements) {
      minX = Math.min(minX, el.x);
      minY = Math.min(minY, el.y);
      maxX = Math.max(maxX, el.x + el.width);
      maxY = Math.max(maxY, el.y + el.height);
    }
    const contentW = maxX - minX;
    const contentH = maxY - minY;

    const scaleX = (stageW - FIT_MARGIN * 2) / contentW;
    const scaleY = (stageH - FIT_MARGIN * 2) / contentH;
    const newScale = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, +(Math.min(scaleX, scaleY)).toFixed(2)));

    // Centrar el contenido en el viewport
    const newX = (stageW - contentW * newScale) / 2 - minX * newScale;
    const newY = (stageH - contentH * newScale) / 2 - minY * newScale;

    onZoomChange(newScale);
    setStagePos({ x: newX, y: newY });
  }, [elements, onZoomChange, containerRef]);

  // ── Drop desde la paleta ──────────────────────────────────────────────────
  const handleDrop = useCallback(
    (e) => {
      e.preventDefault();
      const raw = e.dataTransfer.getData('template');
      if (!raw) return;
      const template = JSON.parse(raw);
      const stage    = stageRef.current;
      stage.setPointersPositions(e);
      const pos = stage.getPointerPosition();
      const canvasX = snapToGrid((pos.x - stagePos.x) / zoom - template.defaultWidth  / 2);
      const canvasY = snapToGrid((pos.y - stagePos.y) / zoom - template.defaultHeight / 2);
      onAddElement({
        id: generateId(),
        type:           template.type,
        sectionType:    template.sectionType ?? null,
        eventSectionId: null,
        seatLayout:     template.seatLayout  ?? null,
        x:      Math.max(0, canvasX),
        y:      Math.max(0, canvasY),
        width:  template.defaultWidth,
        height: template.defaultHeight,
        rotation: 0,
        label:  template.label,
        color:  template.color,
      });
    },
    [zoom, stagePos, onAddElement]
  );
  const handleDragOver = (e) => e.preventDefault();

  // ── Fix 1 + 2 + B: handlers del Stage ────────────────────────────────────
  const handleStageMouseDown = (e) => {
    const isRightClick = e.evt.button === 2;
    const isLeftClick  = e.evt.button === 0;
    const isOverStage  = e.target === e.target.getStage();
    const ctrlOrCmd    = e.evt.ctrlKey || e.evt.metaKey;

    if (isRightClick) {
      e.evt.preventDefault();
      panState.current = {
        active:        true,
        startPointer:  { x: e.evt.clientX, y: e.evt.clientY },
        startStagePos: { ...stagePos },
      };
      return;
    }

    if (!isLeftClick) return;

    // Fix B: si hay edición de vértices activa y el click cayó fuera del Stage
    // (sobre otro elemento), igualmente hacemos commit y salimos
    if (editingPolygonId) {
      commitVertexEditRef.current?.();
      onEndVertexEdit?.();
      return;
    }

    if (!isOverStage) return;

    // Fix B: deshabilitar rubber-band durante edición de vértices
    if (ctrlOrCmd && !editingPolygonId) {
      isRubberBand.current = true;
      const pos = stageRef.current.getRelativePointerPosition();
      setSelBox({ x: pos.x, y: pos.y, width: 0, height: 0, startX: pos.x, startY: pos.y });
    } else if (!editingPolygonId) {
      onSelect([]);
    }
  };

  const handleStageMouseMove = (e) => {
    if (panState.current.active) {
      const dx = e.evt.clientX - panState.current.startPointer.x;
      const dy = e.evt.clientY - panState.current.startPointer.y;
      const newPos = {
        x: panState.current.startStagePos.x + dx,
        y: panState.current.startStagePos.y + dy,
      };
      setStagePos(newPos);
      stageRef.current.position(newPos);
      stageRef.current.batchDraw();
      return;
    }
    if (!isRubberBand.current || !selBox) return;
    const pos = stageRef.current.getRelativePointerPosition();
    setSelBox((prev) => ({
      ...prev,
      x:      Math.min(pos.x, prev.startX),
      y:      Math.min(pos.y, prev.startY),
      width:  Math.abs(pos.x - prev.startX),
      height: Math.abs(pos.y - prev.startY),
    }));
  };

  const handleStageMouseUp = () => {
    if (panState.current.active) {
      panState.current.active = false;
      return;
    }
    if (isRubberBand.current && selBox) {
      if (selBox.width > 5 || selBox.height > 5) {
        const selected = elements
          .filter((el) =>
            rectsIntersect(selBox, { x: el.x, y: el.y, width: el.width, height: el.height })
          )
          .map((el) => el.id);
        onSelect(selected.length > 0 ? selected : []);
      }
    }
    setSelBox(null);
    isRubberBand.current = false;
  };

  // ── Fix 5: drag grupal ────────────────────────────────────────────────────
  const handleGroupDragStart = useCallback(
    (leaderId, startPos) => {
      if (selectedIds.length <= 1) return;
      const startPositions = {};
      for (const el of elements) {
        if (selectedIds.includes(el.id)) startPositions[el.id] = { x: el.x, y: el.y };
      }
      groupDragState.current = { active: true, leaderId, startPositions, leaderStart: startPos };
      setFollowerPositions({ ...startPositions });
    },
    [elements, selectedIds]
  );

  const handleGroupDragMove = useCallback(
    (leaderId, currentPos) => {
      const state = groupDragState.current;
      if (!state.active || state.leaderId !== leaderId) return;
      const deltaX = currentPos.x - state.leaderStart.x;
      const deltaY = currentPos.y - state.leaderStart.y;
      const newPositions = {};
      for (const id of selectedIds) {
        if (id === leaderId) continue;
        newPositions[id] = {
          x: state.startPositions[id].x + deltaX,
          y: state.startPositions[id].y + deltaY,
        };
      }
      setFollowerPositions(newPositions);
    },
    [selectedIds]
  );

  const handleGroupDragEnd = useCallback(
    (leaderId, finalPos) => {
      const state = groupDragState.current;
      if (!state.active || state.leaderId !== leaderId) {
        const el = elements.find((e) => e.id === leaderId);
        if (el) handleElementChange({ ...el, x: snapToGrid(finalPos.x), y: snapToGrid(finalPos.y) });
        return;
      }
      const deltaX = finalPos.x - state.leaderStart.x;
      const deltaY = finalPos.y - state.leaderStart.y;
      const updatedElements = elements.map((el) => {
        if (!selectedIds.includes(el.id)) return el;
        const sp = state.startPositions[el.id];
        return { ...el, x: snapToGrid(sp.x + deltaX), y: snapToGrid(sp.y + deltaY) };
      });
      groupDragState.current = { active: false, leaderId: null, startPositions: {}, leaderStart: null };
      setFollowerPositions({});
      if (onGroupDragEnd) onGroupDragEnd(updatedElements);
    },
    [elements, selectedIds, onGroupDragEnd] // eslint-disable-line react-hooks/exhaustive-deps
  );

  // ── Fix 3: expandir canvas ────────────────────────────────────────────────
  const handleElementChange = useCallback(
    (updated) => {
      const right  = updated.x + updated.width;
      const bottom = updated.y + updated.height;
      let newW = canvasSize.width, newH = canvasSize.height;
      let offsetX = 0, offsetY = 0;
      if (right  > newW) newW = right  + EXPAND_INCREMENT;
      if (bottom > newH) newH = bottom + EXPAND_INCREMENT;
      if (updated.x < 0) { offsetX = Math.ceil(-updated.x / EXPAND_INCREMENT) * EXPAND_INCREMENT; newW += offsetX; }
      if (updated.y < 0) { offsetY = Math.ceil(-updated.y / EXPAND_INCREMENT) * EXPAND_INCREMENT; newH += offsetY; }
      const needsExpansion = newW !== canvasSize.width || newH !== canvasSize.height;
      if (needsExpansion) {
        onExpandCanvas({
          updatedElement: { ...updated, x: updated.x + offsetX, y: updated.y + offsetY },
          offsetDelta:    { x: offsetX, y: offsetY },
          newCanvasSize:  { width: newW, height: newH },
        });
      } else {
        onChange(updated);
      }
    },
    [canvasSize, onChange, onExpandCanvas]
  );

  const stageW = containerRef?.current?.clientWidth  ?? window.innerWidth  - 460;
  const stageH = containerRef?.current?.clientHeight ?? window.innerHeight - 88;

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div
      className="flex-1 overflow-hidden bg-background relative"
      ref={containerRef}
      onDrop={handleDrop}
      onDragOver={handleDragOver}
      onContextMenu={(e) => e.preventDefault()}
    >
      <Stage
        ref={stageRef}
        width={stageW}
        height={stageH}
        scaleX={zoom}
        scaleY={zoom}
        x={stagePos.x}
        y={stagePos.y}
        draggable={false}
        onWheel={handleWheel}
        onMouseDown={handleStageMouseDown}
        onMouseMove={handleStageMouseMove}
        onMouseUp={handleStageMouseUp}
      >
        <Layer listening={false}>
          <Rect x={0} y={0} width={canvasSize.width} height={canvasSize.height}
            fill="var(--color-surface)" cornerRadius={8} />
          {gridLines.map((l) => (
            <Line key={l.key} points={l.points}
              stroke="rgba(255,255,255,0.04)" strokeWidth={1} listening={false} />
          ))}
          <Rect x={0} y={0} width={canvasSize.width} height={canvasSize.height}
            stroke="rgba(124,58,237,0.3)" strokeWidth={1} fill="transparent"
            cornerRadius={8} listening={false} />
        </Layer>

        <Layer>
          {elements.map((el) => {
            const isSelected  = selectedIds.includes(el.id);
            const isMulti     = selectedIds.length > 1 && isSelected;
            const followerPos = followerPositions[el.id];
            const displayEl   = (isMulti && followerPos && el.id !== groupDragState.current.leaderId)
              ? { ...el, x: followerPos.x, y: followerPos.y }
              : el;

            const isEditingThisEl = editingPolygonId === el.id;

            const sharedProps = {
              key:              el.id,
              element:          displayEl,
              isSelected,
              isEditingVertices: isEditingThisEl,
              onSelect:         () => onSelect([el.id]),
              onChange:         handleElementChange,
              onGroupDragStart: isMulti ? handleGroupDragStart : undefined,
              onGroupDragMove:  isMulti ? handleGroupDragMove  : undefined,
              onGroupDragEnd:   isMulti ? handleGroupDragEnd   : undefined,
              onStartVertexEdit: el.type === 'section'
                ? () => onStartVertexEdit?.(el.id) : undefined,
              onEndVertexEdit: el.type === 'section'
                ? () => onEndVertexEdit?.() : undefined,
              // Fix B: el SectionElement en edición registra su commitAndExit aquí
              onSaveVertexEdit: isEditingThisEl
                ? (fn) => { commitVertexEditRef.current = fn; }
                : undefined,
            };

            return el.type === 'section'
              ? <SectionElement {...sharedProps} />
              : <InfraElement   {...sharedProps} />;
          })}

          {selBox && (selBox.width > 2 || selBox.height > 2) && (
            <Rect
              x={selBox.x} y={selBox.y}
              width={selBox.width} height={selBox.height}
              fill="rgba(124,58,237,0.08)"
              stroke="rgba(124,58,237,0.7)"
              strokeWidth={1} dash={[4, 3]} listening={false}
            />
          )}
        </Layer>
      </Stage>

      {/* Fix D2: controles de zoom flotantes */}
      <div className="absolute bottom-4 right-4 z-10 flex items-center gap-1
                      bg-surface border border-surfaceAlt rounded-lg px-2 py-1 shadow-lg
                      shadow-black/30">
        <button
          onClick={handleZoomOut}
          className="w-6 h-6 flex items-center justify-center rounded text-textSecondary
                     hover:text-textPrimary hover:bg-surfaceAlt transition-colors text-sm font-bold"
          title="Alejar (−)"
        >−</button>

        <span className="text-xs font-mono text-textSecondary w-10 text-center select-none">
          {Math.round(zoom * 100)}%
        </span>

        <button
          onClick={handleZoomIn}
          className="w-6 h-6 flex items-center justify-center rounded text-textSecondary
                     hover:text-textPrimary hover:bg-surfaceAlt transition-colors text-sm font-bold"
          title="Acercar (+)"
        >+</button>

        <div className="w-px h-4 bg-surfaceAlt mx-0.5" />

        <button
          onClick={handleFit}
          className="w-6 h-6 flex items-center justify-center rounded text-textSecondary
                     hover:text-textPrimary hover:bg-surfaceAlt transition-colors text-sm"
          title="Ajustar todo al área visible"
        >⊡</button>
      </div>

      {/* Hint */}
      {!editingPolygonId && (
        <div className="absolute bottom-4 left-4 text-[10px] text-textMuted pointer-events-none select-none space-y-0.5">
          <div>🖱 Rueda → zoom · Derecho+drag → pan</div>
          <div>⌨ Ctrl+drag fondo → selección múltiple</div>
        </div>
      )}
      {editingPolygonId && (
        <div className="absolute bottom-4 left-4 text-[10px] text-accent pointer-events-none select-none space-y-0.5">
          <div>✏ Click fuera o <kbd className="bg-surfaceAlt px-1 rounded text-textMuted">Listo</kbd> → guardar</div>
          <div><kbd className="bg-surfaceAlt px-1 rounded text-textMuted">Esc</kbd> → descartar cambios</div>
          <div>Click derecho en vértice → eliminar</div>
        </div>
      )}
    </div>
  );
}
