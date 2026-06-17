import { useRef, useState, useCallback, useMemo } from 'react';
import { Stage, Layer, Line, Rect } from 'react-konva';
import SectionElement from './elements/SectionElement';
import InfraElement from './elements/InfraElement';
import { generateId, snapToGrid, rectsIntersect } from './layoutEditorUtils';

const GRID_SIZE        = 20;
const ZOOM_MIN         = 0.2;
const ZOOM_MAX         = 4;
const ZOOM_STEP        = 0.1;
const EXPAND_INCREMENT = 200;

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
  editingPolygonId,    // Fase 1.3: id del elemento en modo edición de vértices, o null
  onSelect,
  onChange,
  onExpandCanvas,
  onGroupDragEnd,
  onStartVertexEdit,   // Fase 1.3: (elementId) => void
  onEndVertexEdit,     // Fase 1.3: () => void
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

  // Fix 5: estado del drag grupal — solo en refs para no re-renderizar en onDragMove
  const groupDragState = useRef({
    active:       false,
    leaderId:     null,
    startPositions: {},   // { [id]: { x, y } } — posiciones al iniciar el drag
    leaderStart:  null,   // { x, y } del líder al iniciar
  });
  // Posiciones "en vuelo" de los seguidores durante onDragMove
  const [followerPositions, setFollowerPositions] = useState({});

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
      const newScale  = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, oldScale + direction * ZOOM_STEP));
      const newPos    = {
        x: pointer.x - mousePointTo.x * newScale,
        y: pointer.y - mousePointTo.y * newScale,
      };
      onZoomChange(newScale);
      setStagePos(newPos);
    },
    [zoom, onZoomChange]
  );

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

  // ── Fix 1 + 2: handlers del Stage ────────────────────────────────────────
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
    if (!isLeftClick || !isOverStage) return;
    if (ctrlOrCmd) {
      isRubberBand.current = true;
      const pos = stageRef.current.getRelativePointerPosition();
      setSelBox({ x: pos.x, y: pos.y, width: 0, height: 0, startX: pos.x, startY: pos.y });
    } else {
      // Fase 1.3: click en el fondo mientras se editan vértices → salir del modo edición
      // El componente padre maneja esto via onSelect([]) que dispara onEndVertexEdit si
      // está activo — no necesitamos un callback extra aquí porque EventLayoutEditorDemo
      // escucha el cambio de selectedIds y cancela editingPolygonId.
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

  // ────────────────────────────────────────────────────────────────────────────
  // Fix 5: callbacks de drag grupal
  // ────────────────────────────────────────────────────────────────────────────

  /**
   * Al iniciar el drag de un elemento que pertenece a una selección múltiple,
   * guardamos las posiciones iniciales de todos los seleccionados.
   */
  const handleGroupDragStart = useCallback(
    (leaderId, startPos) => {
      if (selectedIds.length <= 1) return; // drag individual, no hacer nada aquí

      const startPositions = {};
      for (const el of elements) {
        if (selectedIds.includes(el.id)) {
          startPositions[el.id] = { x: el.x, y: el.y };
        }
      }

      groupDragState.current = {
        active:         true,
        leaderId,
        startPositions,
        leaderStart:    startPos,
      };
      // Inicializar followerPositions con las posiciones actuales
      setFollowerPositions({ ...startPositions });
    },
    [elements, selectedIds]
  );

  /**
   * Durante el drag, calculamos el delta del líder y aplicamos el mismo delta
   * a los seguidores — actualiza React state para moverlos visualmente.
   */
  const handleGroupDragMove = useCallback(
    (leaderId, currentPos) => {
      const state = groupDragState.current;
      if (!state.active || state.leaderId !== leaderId) return;

      const deltaX = currentPos.x - state.leaderStart.x;
      const deltaY = currentPos.y - state.leaderStart.y;

      const newPositions = {};
      for (const id of selectedIds) {
        if (id === leaderId) continue; // el líder lo maneja Konva directamente
        newPositions[id] = {
          x: state.startPositions[id].x + deltaX,
          y: state.startPositions[id].y + deltaY,
        };
      }
      setFollowerPositions(newPositions);
    },
    [selectedIds]
  );

  /**
   * Al soltar, aplicamos snapping a todos, llamamos onGroupDragEnd en el padre
   * para que actualice el estado canónico y recalcule el canvas (Fix 7).
   */
  const handleGroupDragEnd = useCallback(
    (leaderId, finalPos) => {
      const state = groupDragState.current;

      if (!state.active || state.leaderId !== leaderId) {
        // Drag individual — delegar al handler normal
        const el = elements.find((e) => e.id === leaderId);
        if (el) {
          handleElementChange({ ...el, x: snapToGrid(finalPos.x), y: snapToGrid(finalPos.y) });
        }
        return;
      }

      // Drag grupal: calcular posiciones finales con snapping para todos
      const deltaX = finalPos.x - state.leaderStart.x;
      const deltaY = finalPos.y - state.leaderStart.y;

      const updatedElements = elements.map((el) => {
        if (!selectedIds.includes(el.id)) return el;
        const startPos = state.startPositions[el.id];
        return {
          ...el,
          x: snapToGrid(startPos.x + deltaX),
          y: snapToGrid(startPos.y + deltaY),
        };
      });

      groupDragState.current = { active: false, leaderId: null, startPositions: {}, leaderStart: null };
      setFollowerPositions({});

      // Notificar al padre con todos los elementos actualizados (Fix 7)
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

  // ── Dimensiones del viewport ──────────────────────────────────────────────
  const stageW = containerRef?.current?.clientWidth  ?? window.innerWidth  - 460;
  const stageH = containerRef?.current?.clientHeight ?? window.innerHeight - 88;

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
            const isSelected = selectedIds.includes(el.id);
            const isMulti    = selectedIds.length > 1 && isSelected;

            // Fix 5: si hay drag grupal activo, los seguidores usan posición en vuelo
            const followerPos = followerPositions[el.id];
            const displayEl = (isMulti && followerPos && el.id !== groupDragState.current.leaderId)
              ? { ...el, x: followerPos.x, y: followerPos.y }
              : el;

            const sharedProps = {
              key:              el.id,
              element:          displayEl,
              isSelected,
              isEditingVertices: editingPolygonId === el.id,  // Fase 1.3
              onSelect:         () => onSelect([el.id]),
              onChange:         handleElementChange,
              onGroupDragStart: isMulti ? handleGroupDragStart : undefined,
              onGroupDragMove:  isMulti ? handleGroupDragMove  : undefined,
              onGroupDragEnd:   isMulti ? handleGroupDragEnd   : undefined,
              // Fase 1.3: solo secciones usan estos callbacks
              onStartVertexEdit: el.type === 'section'
                ? () => onStartVertexEdit?.(el.id)
                : undefined,
              onEndVertexEdit: el.type === 'section'
                ? () => onEndVertexEdit?.()
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

      <div className="absolute bottom-2 right-2 text-[10px] text-textMuted pointer-events-none select-none space-y-0.5">
        <div>🖱 Rueda → zoom</div>
        <div>🖱 Derecho + arrastrar → mover canvas</div>
        <div>⌨ Ctrl + arrastrar fondo → selección múltiple</div>
      </div>
    </div>
  );
}
