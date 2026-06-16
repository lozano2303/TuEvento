import { useRef, useState, useCallback, useEffect } from 'react';
import { Stage, Layer, Line, Rect } from 'react-konva';
import SectionElement from './elements/SectionElement';
import InfraElement from './elements/InfraElement';
import { generateId, snapToGrid, rectsIntersect } from './layoutEditorUtils';

const GRID_SIZE = 20;
const CANVAS_W = 1200;
const CANVAS_H = 800;
const ZOOM_MIN = 0.2;
const ZOOM_MAX = 4;
const ZOOM_STEP = 0.1;

// Genera líneas de grilla para el Layer de fondo
function buildGridLines(width, height, step) {
  const lines = [];
  for (let x = 0; x <= width; x += step) {
    lines.push({ key: `v${x}`, points: [x, 0, x, height] });
  }
  for (let y = 0; y <= height; y += step) {
    lines.push({ key: `h${y}`, points: [0, y, width, y] });
  }
  return lines;
}

const GRID_LINES = buildGridLines(CANVAS_W, CANVAS_H, GRID_SIZE);

export default function LayoutEditorCanvas({
  elements,
  selectedIds,
  onSelect,
  onChange,
  onAddElement,
  zoom,
  onZoomChange,
  containerRef,
}) {
  const stageRef = useRef();
  const [stagePos, setStagePos] = useState({ x: 0, y: 0 });

  // Rubber-band selection
  const [selBox, setSelBox] = useState(null);
  const isDraggingCanvas = useRef(false);

  // ── Zoom con rueda del mouse (centrado en cursor) ─────────────────────────
  const handleWheel = useCallback(
    (e) => {
      e.evt.preventDefault();
      const stage = stageRef.current;
      const oldScale = zoom;
      const pointer = stage.getPointerPosition();

      const mousePointTo = {
        x: (pointer.x - stage.x()) / oldScale,
        y: (pointer.y - stage.y()) / oldScale,
      };

      const direction = e.evt.deltaY < 0 ? 1 : -1;
      const newScale = Math.min(
        ZOOM_MAX,
        Math.max(ZOOM_MIN, oldScale + direction * ZOOM_STEP)
      );

      const newPos = {
        x: pointer.x - mousePointTo.x * newScale,
        y: pointer.y - mousePointTo.y * newScale,
      };

      onZoomChange(newScale);
      setStagePos(newPos);
    },
    [zoom, onZoomChange]
  );

  // ── Drag-and-drop desde la paleta ─────────────────────────────────────────
  const handleDrop = useCallback(
    (e) => {
      e.preventDefault();
      const raw = e.dataTransfer.getData('template');
      if (!raw) return;

      const template = JSON.parse(raw);
      const stage = stageRef.current;
      stage.setPointersPositions(e);
      const pos = stage.getPointerPosition();

      // Convertir coordenadas de pantalla a coordenadas del canvas
      const canvasX = snapToGrid((pos.x - stagePos.x) / zoom - template.defaultWidth / 2);
      const canvasY = snapToGrid((pos.y - stagePos.y) / zoom - template.defaultHeight / 2);

      onAddElement({
        id: generateId(),
        type: template.type,
        sectionType: template.sectionType ?? null,
        eventSectionId: null,
        seatLayout: template.seatLayout ?? null,
        x: Math.max(0, canvasX),
        y: Math.max(0, canvasY),
        width: template.defaultWidth,
        height: template.defaultHeight,
        rotation: 0,
        label: template.label,
        color: template.color,
      });
    },
    [zoom, stagePos, onAddElement]
  );

  const handleDragOver = (e) => e.preventDefault();

  // ── Click en fondo → deseleccionar / inicio rubber-band ───────────────────
  const handleStageMouseDown = (e) => {
    if (e.target !== e.target.getStage()) return;
    onSelect([]);
    isDraggingCanvas.current = true;

    const stage = stageRef.current;
    const pos = stage.getRelativePointerPosition();
    setSelBox({ x: pos.x, y: pos.y, width: 0, height: 0, startX: pos.x, startY: pos.y });
  };

  const handleStageMouseMove = (e) => {
    if (!selBox || !isDraggingCanvas.current) return;
    const stage = stageRef.current;
    const pos = stage.getRelativePointerPosition();

    setSelBox((prev) => ({
      ...prev,
      x: Math.min(pos.x, prev.startX),
      y: Math.min(pos.y, prev.startY),
      width: Math.abs(pos.x - prev.startX),
      height: Math.abs(pos.y - prev.startY),
    }));
  };

  const handleStageMouseUp = () => {
    if (selBox && (selBox.width > 5 || selBox.height > 5)) {
      const selected = elements
        .filter((el) => rectsIntersect(selBox, { x: el.x, y: el.y, width: el.width, height: el.height }))
        .map((el) => el.id);
      if (selected.length > 0) onSelect(selected);
    }
    setSelBox(null);
    isDraggingCanvas.current = false;
  };

  return (
    <div
      className="flex-1 overflow-hidden bg-background relative"
      ref={containerRef}
      onDrop={handleDrop}
      onDragOver={handleDragOver}
    >
      <Stage
        ref={stageRef}
        width={containerRef?.current?.clientWidth ?? window.innerWidth - 460}
        height={containerRef?.current?.clientHeight ?? window.innerHeight - 44}
        scaleX={zoom}
        scaleY={zoom}
        x={stagePos.x}
        y={stagePos.y}
        draggable
        onDragEnd={(e) => {
          setStagePos({ x: e.target.x(), y: e.target.y() });
        }}
        onWheel={handleWheel}
        onMouseDown={handleStageMouseDown}
        onMouseMove={handleStageMouseMove}
        onMouseUp={handleStageMouseUp}
      >
        {/* Layer de grilla */}
        <Layer listening={false}>
          <Rect
            x={0}
            y={0}
            width={CANVAS_W}
            height={CANVAS_H}
            fill="var(--color-surface)"
            cornerRadius={8}
          />
          {GRID_LINES.map((l) => (
            <Line
              key={l.key}
              points={l.points}
              stroke="rgba(255,255,255,0.04)"
              strokeWidth={1}
              listening={false}
            />
          ))}
          {/* Borde del canvas */}
          <Rect
            x={0}
            y={0}
            width={CANVAS_W}
            height={CANVAS_H}
            stroke="rgba(124,58,237,0.3)"
            strokeWidth={1}
            fill="transparent"
            cornerRadius={8}
            listening={false}
          />
        </Layer>

        {/* Layer de elementos */}
        <Layer>
          {elements.map((el) => {
            const isSelected = selectedIds.includes(el.id);
            const props = {
              key: el.id,
              element: el,
              isSelected,
              onSelect: () => onSelect([el.id]),
              onChange: (updated) => onChange(updated),
            };
            return el.type === 'section'
              ? <SectionElement {...props} />
              : <InfraElement {...props} />;
          })}

          {/* Rubber-band rect */}
          {selBox && (selBox.width > 2 || selBox.height > 2) && (
            <Rect
              x={selBox.x}
              y={selBox.y}
              width={selBox.width}
              height={selBox.height}
              fill="rgba(124,58,237,0.08)"
              stroke="rgba(124,58,237,0.7)"
              strokeWidth={1}
              dash={[4, 3]}
              listening={false}
            />
          )}
        </Layer>
      </Stage>
    </div>
  );
}
