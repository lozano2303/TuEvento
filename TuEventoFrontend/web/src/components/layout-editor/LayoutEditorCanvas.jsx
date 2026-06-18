import { useRef, useState, useCallback, useMemo } from 'react';
import { Stage, Layer, Line, Rect } from 'react-konva';
import SectionElement from './elements/SectionElement';
import InfraElement from './elements/InfraElement';
import {
  generateId, snapToGrid, rectsIntersect,
  findSnapGuides,
} from './layoutEditorUtils';

const GRID_SIZE        = 20;
const ZOOM_MIN         = 0.2;
const ZOOM_MAX         = 3;
const ZOOM_STEP        = 0.1;
const EXPAND_INCREMENT = 200;
const FIT_MARGIN       = 40;
const SNAP_THRESHOLD   = 6;   // px — tolerancia para smart guides
const GUIDE_COLOR      = '#FF4D8F'; // magenta — estándar Figma/Canva
const GUIDE_EXTENT     = 10000;     // longitud de las líneas guía en px de canvas

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

  // Fix B: ref al commitAndExit del SectionElement en edición
  const commitVertexEditRef = useRef(null);

  // Fase 1.6: smart guides — posición de las líneas guía activas (coord. canvas)
  const [activeGuides, setActiveGuides] = useState({ vertical: null, horizontal: null });

  const gridLines = useMemo(
    () => buildGridLines(canvasSize.width, canvasSize.height, GRID_SIZE),
    [canvasSize.width, canvasSize.height]
  );

  // ── Zoom ─────────────────────────────────────────────────────────────────
  const handleWheel = useCallback((e) => {
    e.evt.preventDefault();
    const stage = stageRef.current;
    const oldScale = zoom;
    const pointer  = stage.getPointerPosition();
    const mpt = { x: (pointer.x - stage.x()) / oldScale, y: (pointer.y - stage.y()) / oldScale };
    const dir = e.evt.deltaY < 0 ? 1 : -1;
    const ns  = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, +(oldScale + dir * ZOOM_STEP).toFixed(2)));
    onZoomChange(ns);
    setStagePos({ x: pointer.x - mpt.x * ns, y: pointer.y - mpt.y * ns });
  }, [zoom, onZoomChange]);

  const handleZoomIn = useCallback(() => {
    const next = Math.min(ZOOM_MAX, +(zoom + ZOOM_STEP).toFixed(2));
    onZoomChange(next);
    const sw = containerRef?.current?.clientWidth  ?? 800;
    const sh = containerRef?.current?.clientHeight ?? 600;
    setStagePos((pos) => {
      const mpt = { x: (sw / 2 - pos.x) / zoom, y: (sh / 2 - pos.y) / zoom };
      return { x: sw / 2 - mpt.x * next, y: sh / 2 - mpt.y * next };
    });
  }, [zoom, onZoomChange, containerRef]);

  const handleZoomOut = useCallback(() => {
    const next = Math.max(ZOOM_MIN, +(zoom - ZOOM_STEP).toFixed(2));
    onZoomChange(next);
    const sw = containerRef?.current?.clientWidth  ?? 800;
    const sh = containerRef?.current?.clientHeight ?? 600;
    setStagePos((pos) => {
      const mpt = { x: (sw / 2 - pos.x) / zoom, y: (sh / 2 - pos.y) / zoom };
      return { x: sw / 2 - mpt.x * next, y: sh / 2 - mpt.y * next };
    });
  }, [zoom, onZoomChange, containerRef]);

  const handleFit = useCallback(() => {
    const sw = containerRef?.current?.clientWidth  ?? 800;
    const sh = containerRef?.current?.clientHeight ?? 600;
    if (elements.length === 0) { onZoomChange(1); setStagePos({ x: 0, y: 0 }); return; }
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    for (const el of elements) {
      minX = Math.min(minX, el.x); minY = Math.min(minY, el.y);
      maxX = Math.max(maxX, el.x + el.width); maxY = Math.max(maxY, el.y + el.height);
    }
    const ns = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN,
      +(Math.min((sw - FIT_MARGIN * 2) / (maxX - minX), (sh - FIT_MARGIN * 2) / (maxY - minY))).toFixed(2)));
    onZoomChange(ns);
    setStagePos({ x: (sw - (maxX - minX) * ns) / 2 - minX * ns, y: (sh - (maxY - minY) * ns) / 2 - minY * ns });
  }, [elements, onZoomChange, containerRef]);

  // ── Drop ─────────────────────────────────────────────────────────────────
  const handleDrop = useCallback((e) => {
    e.preventDefault();
    const raw = e.dataTransfer.getData('template');
    if (!raw) return;
    const template = JSON.parse(raw);
    const stage = stageRef.current;
    stage.setPointersPositions(e);
    const pos = stage.getPointerPosition();
    const cx = snapToGrid((pos.x - stagePos.x) / zoom - template.defaultWidth  / 2);
    const cy = snapToGrid((pos.y - stagePos.y) / zoom - template.defaultHeight / 2);
    onAddElement({
      id: generateId(), type: template.type,
      sectionType: template.sectionType ?? null, eventSectionId: null,
      seatLayout: template.seatLayout ?? null,
      x: Math.max(0, cx), y: Math.max(0, cy),
      width: template.defaultWidth, height: template.defaultHeight,
      rotation: 0, label: template.label, color: template.color,
    });
  }, [zoom, stagePos, onAddElement]);
  const handleDragOver = (e) => e.preventDefault();

  // ── Stage mouse handlers ──────────────────────────────────────────────────
  const handleStageMouseDown = (e) => {
    const isRight = e.evt.button === 2;
    const isLeft  = e.evt.button === 0;
    const isOver  = e.target === e.target.getStage();
    const ctrl    = e.evt.ctrlKey || e.evt.metaKey;

    if (isRight) {
      e.evt.preventDefault();
      panState.current = { active: true, startPointer: { x: e.evt.clientX, y: e.evt.clientY }, startStagePos: { ...stagePos } };
      return;
    }
    if (!isLeft) return;

    if (editingPolygonId) {
      const name = e.target?.name?.() ?? e.target?.attrs?.name ?? '';
      if (!name.startsWith('vertex-handle-') && !name.startsWith('midpoint-handle-') && !name.startsWith('polygon-shape-')) {
        commitVertexEditRef.current?.();
        onEndVertexEdit?.();
      }
      return;
    }
    if (!isOver) return;
    if (ctrl && !editingPolygonId) {
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
      const np = { x: panState.current.startStagePos.x + dx, y: panState.current.startStagePos.y + dy };
      setStagePos(np); stageRef.current.position(np); stageRef.current.batchDraw();
      return;
    }
    if (!isRubberBand.current || !selBox) return;
    const pos = stageRef.current.getRelativePointerPosition();
    setSelBox((prev) => ({
      ...prev,
      x: Math.min(pos.x, prev.startX), y: Math.min(pos.y, prev.startY),
      width: Math.abs(pos.x - prev.startX), height: Math.abs(pos.y - prev.startY),
    }));
  };

  const handleStageMouseUp = () => {
    if (panState.current.active) { panState.current.active = false; return; }
    if (isRubberBand.current && selBox) {
      if (selBox.width > 5 || selBox.height > 5) {
        const sel = elements.filter((el) => rectsIntersect(selBox, { x: el.x, y: el.y, width: el.width, height: el.height })).map((el) => el.id);
        onSelect(sel.length > 0 ? sel : []);
      }
    }
    setSelBox(null); isRubberBand.current = false;
  };

  // ── Fix 5 + Fase 1.6: drag grupal con smart guides ────────────────────────
  const handleGroupDragStart = useCallback((leaderId, startPos) => {
    if (selectedIds.length <= 1) return;
    const sp = {};
    for (const el of elements) { if (selectedIds.includes(el.id)) sp[el.id] = { x: el.x, y: el.y }; }
    groupDragState.current = { active: true, leaderId, startPositions: sp, leaderStart: startPos };
    setFollowerPositions({ ...sp });
  }, [elements, selectedIds]);

  const handleGroupDragMove = useCallback((leaderId, currentPos) => {
    const state = groupDragState.current;
    if (!state.active || state.leaderId !== leaderId) return;

    const rawDx = currentPos.x - state.leaderStart.x;
    const rawDy = currentPos.y - state.leaderStart.y;

    // Fase 1.6: calcular bounding box del grupo en la posición tentativa
    // y buscar smart guides contra los elementos que NO están en la selección
    const outsiders = elements.filter((el) => !selectedIds.includes(el.id));
    if (outsiders.length > 0) {
      // Bounding box del grupo con el desplazamiento tentativo
      let gMinX = Infinity, gMinY = Infinity, gMaxX = -Infinity, gMaxY = -Infinity;
      for (const id of selectedIds) {
        const el = elements.find((e) => e.id === id);
        if (!el) continue;
        const nx = el.x + rawDx, ny = el.y + rawDy;
        gMinX = Math.min(gMinX, nx); gMinY = Math.min(gMinY, ny);
        gMaxX = Math.max(gMaxX, nx + el.width); gMaxY = Math.max(gMaxY, ny + el.height);
      }
      const groupProxy = { id: '__group__', x: gMinX, y: gMinY, width: gMaxX - gMinX, height: gMaxY - gMinY };
      const guides = findSnapGuides(groupProxy, outsiders, SNAP_THRESHOLD);

      // Ajustar el delta con el snap de smart guide (prioridad sobre snap de grilla)
      const finalDx = guides.vertical   ? rawDx + guides.vertical.delta   : rawDx;
      const finalDy = guides.horizontal ? rawDy + guides.horizontal.delta : rawDy;

      setActiveGuides({
        vertical:   guides.vertical   ? guides.vertical.position   : null,
        horizontal: guides.horizontal ? guides.horizontal.position : null,
      });

      const newPositions = {};
      for (const id of selectedIds) {
        if (id === leaderId) continue;
        newPositions[id] = { x: state.startPositions[id].x + finalDx, y: state.startPositions[id].y + finalDy };
      }
      setFollowerPositions(newPositions);
      return;
    }

    // Sin outsiders: sólo mover seguidores
    const newPositions = {};
    for (const id of selectedIds) {
      if (id === leaderId) continue;
      newPositions[id] = { x: state.startPositions[id].x + rawDx, y: state.startPositions[id].y + rawDy };
    }
    setFollowerPositions(newPositions);
    setActiveGuides({ vertical: null, horizontal: null });
  }, [elements, selectedIds]);

  const handleGroupDragEnd = useCallback((leaderId, finalPos) => {
    setActiveGuides({ vertical: null, horizontal: null });
    const state = groupDragState.current;
    if (!state.active || state.leaderId !== leaderId) {
      const el = elements.find((e) => e.id === leaderId);
      if (el) handleElementChange({ ...el, x: snapToGrid(finalPos.x), y: snapToGrid(finalPos.y) });
      return;
    }
    const dx = finalPos.x - state.leaderStart.x;
    const dy = finalPos.y - state.leaderStart.y;
    const updated = elements.map((el) => {
      if (!selectedIds.includes(el.id)) return el;
      const sp = state.startPositions[el.id];
      return { ...el, x: snapToGrid(sp.x + dx), y: snapToGrid(sp.y + dy) };
    });
    groupDragState.current = { active: false, leaderId: null, startPositions: {}, leaderStart: null };
    setFollowerPositions({});
    if (onGroupDragEnd) onGroupDragEnd(updated);
  }, [elements, selectedIds, onGroupDragEnd]); // eslint-disable-line react-hooks/exhaustive-deps

  // ── Fase 1.6: drag individual con smart guides ────────────────────────────
  const handleElementDragMove = useCallback((elementId, tentativePos) => {
    const el = elements.find((e) => e.id === elementId);
    if (!el) return;
    const tentative = { ...el, x: tentativePos.x, y: tentativePos.y };
    const outsiders  = elements.filter((e) => e.id !== elementId);
    const guides     = findSnapGuides(tentative, outsiders, SNAP_THRESHOLD);
    setActiveGuides({
      vertical:   guides.vertical   ? guides.vertical.position   : null,
      horizontal: guides.horizontal ? guides.horizontal.position : null,
    });
    // devolver el delta para que el componente hijo pueda aplicarlo
    return { dx: guides.vertical?.delta ?? 0, dy: guides.horizontal?.delta ?? 0 };
  }, [elements]);

  const handleElementDragEnd = useCallback((updated) => {
    setActiveGuides({ vertical: null, horizontal: null });
    handleElementChange(updated);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // ── Fix 3: expandir canvas ────────────────────────────────────────────────
  const handleElementChange = useCallback((updated) => {
    const right = updated.x + updated.width, bottom = updated.y + updated.height;
    let nW = canvasSize.width, nH = canvasSize.height, ox = 0, oy = 0;
    if (right  > nW) nW = right  + EXPAND_INCREMENT;
    if (bottom > nH) nH = bottom + EXPAND_INCREMENT;
    if (updated.x < 0) { ox = Math.ceil(-updated.x / EXPAND_INCREMENT) * EXPAND_INCREMENT; nW += ox; }
    if (updated.y < 0) { oy = Math.ceil(-updated.y / EXPAND_INCREMENT) * EXPAND_INCREMENT; nH += oy; }
    const exp = nW !== canvasSize.width || nH !== canvasSize.height;
    if (exp) {
      onExpandCanvas({ updatedElement: { ...updated, x: updated.x + ox, y: updated.y + oy }, offsetDelta: { x: ox, y: oy }, newCanvasSize: { width: nW, height: nH } });
    } else {
      onChange(updated);
    }
  }, [canvasSize, onChange, onExpandCanvas]);

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
        width={stageW} height={stageH}
        scaleX={zoom} scaleY={zoom}
        x={stagePos.x} y={stagePos.y}
        draggable={false}
        onWheel={handleWheel}
        onMouseDown={handleStageMouseDown}
        onMouseMove={handleStageMouseMove}
        onMouseUp={handleStageMouseUp}
      >
        {/* Layer de grilla */}
        <Layer listening={false}>
          <Rect x={0} y={0} width={canvasSize.width} height={canvasSize.height} fill="var(--color-surface)" cornerRadius={8} />
          {gridLines.map((l) => (
            <Line key={l.key} points={l.points} stroke="rgba(255,255,255,0.04)" strokeWidth={1} listening={false} />
          ))}
          <Rect x={0} y={0} width={canvasSize.width} height={canvasSize.height} stroke="rgba(124,58,237,0.3)" strokeWidth={1} fill="transparent" cornerRadius={8} listening={false} />
        </Layer>

        {/* Layer de elementos */}
        <Layer>
          {elements.map((el) => {
            const isSelected    = selectedIds.includes(el.id);
            const isMulti       = selectedIds.length > 1 && isSelected;
            const followerPos   = followerPositions[el.id];
            const isEditingThis = editingPolygonId === el.id;

            const displayEl = (isMulti && followerPos && el.id !== groupDragState.current.leaderId)
              ? { ...el, x: followerPos.x, y: followerPos.y }
              : el;

            const sharedProps = {
              key:              el.id,
              element:          displayEl,
              isSelected,
              isEditingVertices: isEditingThis,
              onSelect:         () => onSelect([el.id]),
              onChange:         handleElementChange,
              onDragMove:       handleElementDragMove,    // Fase 1.6
              onDragEnd:        handleElementDragEnd,     // Fase 1.6
              onGroupDragStart: isMulti ? handleGroupDragStart : undefined,
              onGroupDragMove:  isMulti ? handleGroupDragMove  : undefined,
              onGroupDragEnd:   isMulti ? handleGroupDragEnd   : undefined,
              onStartVertexEdit: el.type === 'section' ? () => onStartVertexEdit?.(el.id) : undefined,
              onEndVertexEdit:   el.type === 'section' ? () => onEndVertexEdit?.()         : undefined,
              onSaveVertexEdit:  isEditingThis ? (fn) => { commitVertexEditRef.current = fn; } : undefined,
            };

            return el.type === 'section'
              ? <SectionElement {...sharedProps} />
              : <InfraElement   {...sharedProps} />;
          })}

          {/* Rubber-band */}
          {selBox && (selBox.width > 2 || selBox.height > 2) && (
            <Rect x={selBox.x} y={selBox.y} width={selBox.width} height={selBox.height}
              fill="rgba(124,58,237,0.08)" stroke="rgba(124,58,237,0.7)"
              strokeWidth={1} dash={[4, 3]} listening={false} />
          )}
        </Layer>

        {/* Fase 1.6: Layer de smart guides — siempre encima */}
        <Layer listening={false}>
          {activeGuides.vertical !== null && (
            <Line
              points={[activeGuides.vertical, -GUIDE_EXTENT, activeGuides.vertical, GUIDE_EXTENT]}
              stroke={GUIDE_COLOR} strokeWidth={1} dash={[4, 4]} listening={false}
            />
          )}
          {activeGuides.horizontal !== null && (
            <Line
              points={[-GUIDE_EXTENT, activeGuides.horizontal, GUIDE_EXTENT, activeGuides.horizontal]}
              stroke={GUIDE_COLOR} strokeWidth={1} dash={[4, 4]} listening={false}
            />
          )}
        </Layer>
      </Stage>

      {/* Controles de zoom flotantes */}
      <div className="absolute bottom-4 right-4 z-10 flex items-center gap-1 bg-surface border border-surfaceAlt rounded-lg px-2 py-1 shadow-lg shadow-black/30">
        <button onClick={handleZoomOut} className="w-6 h-6 flex items-center justify-center rounded text-textSecondary hover:text-textPrimary hover:bg-surfaceAlt transition-colors text-sm font-bold" title="Alejar">−</button>
        <span className="text-xs font-mono text-textSecondary w-10 text-center select-none">{Math.round(zoom * 100)}%</span>
        <button onClick={handleZoomIn} className="w-6 h-6 flex items-center justify-center rounded text-textSecondary hover:text-textPrimary hover:bg-surfaceAlt transition-colors text-sm font-bold" title="Acercar">+</button>
        <div className="w-px h-4 bg-surfaceAlt mx-0.5" />
        <button onClick={handleFit} className="w-6 h-6 flex items-center justify-center rounded text-textSecondary hover:text-textPrimary hover:bg-surfaceAlt transition-colors text-sm" title="Ajustar al canvas">⊡</button>
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
          <div><kbd className="bg-surfaceAlt px-1 rounded text-textMuted">Esc</kbd> → descartar</div>
        </div>
      )}
    </div>
  );
}
