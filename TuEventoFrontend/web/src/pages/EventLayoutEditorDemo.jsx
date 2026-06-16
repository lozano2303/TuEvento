import { useState, useRef, useCallback } from 'react';
import ElementPalette from '../components/layout-editor/ElementPalette';
import LayoutEditorCanvas from '../components/layout-editor/LayoutEditorCanvas';
import PropertiesPanel from '../components/layout-editor/PropertiesPanel';
import EditorToolbar from '../components/layout-editor/EditorToolbar';
import { generateId, computeCanvasForElements } from '../components/layout-editor/layoutEditorUtils';

const INITIAL_ELEMENTS = [
  {
    id: generateId(),
    type: 'stage',
    sectionType: null,
    eventSectionId: null,
    seatLayout: null,
    x: 490, y: 60, width: 220, height: 90,
    rotation: 0, label: 'Escenario', color: '#1E293B',
  },
  {
    id: generateId(),
    type: 'section',
    sectionType: 'VIP',
    eventSectionId: null,
    seatLayout: { rows: 4, cols: 6, seatRadius: 7, gap: 4 },
    x: 100, y: 200, width: 216, height: 140,
    rotation: 0, label: 'VIP Izquierda', color: '#7C3AED',
  },
  {
    id: generateId(),
    type: 'section',
    sectionType: 'General',
    eventSectionId: null,
    seatLayout: { rows: 6, cols: 8, seatRadius: 7, gap: 4 },
    x: 400, y: 220, width: 264, height: 168,
    rotation: 0, label: 'General', color: '#16A34A',
  },
  {
    id: generateId(),
    type: 'entrance',
    sectionType: null,
    eventSectionId: null,
    seatLayout: null,
    x: 560, y: 700, width: 80, height: 50,
    rotation: 0, label: 'Entrada', color: '#15803D',
  },
];

const CANVAS_DEFAULT = { width: 1200, height: 800 };
const ZOOM_MIN  = 0.2;
const ZOOM_MAX  = 4;
const ZOOM_STEP = 0.15;

// ── Helper: recalcula canvasSize tras cualquier movimiento (Fix 7) ────────────
function reconcileCanvas(nextElements, currentCanvas) {
  const { newCanvasSize, offsetDelta } = computeCanvasForElements(nextElements, currentCanvas);
  const needsOffset = offsetDelta.x !== 0 || offsetDelta.y !== 0;
  const adjustedElements = needsOffset
    ? nextElements.map((el) => ({ ...el, x: el.x + offsetDelta.x, y: el.y + offsetDelta.y }))
    : nextElements;
  return { adjustedElements, newCanvasSize };
}

export default function EventLayoutEditorDemo() {
  const [elements,    setElements]    = useState(INITIAL_ELEMENTS);
  const [selectedIds, setSelectedIds] = useState([]);
  const [zoom,        setZoom]        = useState(0.75);
  const [canvasSize,  setCanvasSize]  = useState(CANVAS_DEFAULT);
  const containerRef = useRef();

  // ── Selección ─────────────────────────────────────────────────────────────
  const handleSelect = useCallback((ids) => {
    setSelectedIds(Array.isArray(ids) ? ids : [ids]);
  }, []);

  // ── Modificar un elemento (drag individual, transform, props panel) ───────
  // Fix 7: después de cada cambio, recalcular el canvas
  const handleChange = useCallback((updated) => {
    setElements((prev) => {
      const next = prev.map((el) => (el.id === updated.id ? updated : el));
      const { adjustedElements, newCanvasSize } = reconcileCanvas(next, canvasSize);
      // Si el canvas cambió, actualizarlo en el mismo ciclo mediante un
      // setTimeout(0) no es ideal — usamos un ref para pasarlo afuera
      if (
        newCanvasSize.width  !== canvasSize.width ||
        newCanvasSize.height !== canvasSize.height
      ) {
        // Programar la actualización de canvasSize fuera del setState de elements
        queueMicrotask(() => setCanvasSize(newCanvasSize));
      }
      return adjustedElements;
    });
  }, [canvasSize]);

  // ── Fix 5 + 7: drag grupal finalizado en el canvas ────────────────────────
  // Recibe el array completo de elementos con las posiciones ya actualizadas
  const handleGroupDragEnd = useCallback((updatedElements) => {
    setElements((prev) => {
      // Mezclar: los elementos del grupo con sus nuevas posiciones, los demás intactos
      const idSet = new Set(updatedElements.map((e) => e.id));
      const merged = prev.map((el) => {
        const u = updatedElements.find((ue) => ue.id === el.id);
        return u ?? el;
      });
      const { adjustedElements, newCanvasSize } = reconcileCanvas(merged, canvasSize);
      if (
        newCanvasSize.width  !== canvasSize.width ||
        newCanvasSize.height !== canvasSize.height
      ) {
        queueMicrotask(() => setCanvasSize(newCanvasSize));
      }
      return adjustedElements;
    });
  }, [canvasSize]);

  // ── Fix 3: expansión de canvas por desbordamiento ─────────────────────────
  const handleExpandCanvas = useCallback(({ updatedElement, offsetDelta, newCanvasSize }) => {
    setElements((prev) =>
      prev.map((el) => {
        const base = el.id === updatedElement.id ? updatedElement : el;
        return { ...base, x: base.x + offsetDelta.x, y: base.y + offsetDelta.y };
      })
    );
    setCanvasSize(newCanvasSize);
  }, []);

  // ── Añadir desde paleta ───────────────────────────────────────────────────
  const handleAddElement = useCallback((newElement) => {
    setElements((prev) => [...prev, newElement]);
    setSelectedIds([newElement.id]);
  }, []);

  // ── Eliminar seleccionado ─────────────────────────────────────────────────
  const handleDelete = useCallback(() => {
    setElements((prev) => {
      const next = prev.filter((el) => !selectedIds.includes(el.id));
      const { adjustedElements, newCanvasSize } = reconcileCanvas(next, canvasSize);
      queueMicrotask(() => setCanvasSize(newCanvasSize));
      return adjustedElements;
    });
    setSelectedIds([]);
  }, [selectedIds, canvasSize]);

  // ── Zoom ──────────────────────────────────────────────────────────────────
  const handleZoomIn    = () => setZoom((z) => Math.min(ZOOM_MAX, +(z + ZOOM_STEP).toFixed(2)));
  const handleZoomOut   = () => setZoom((z) => Math.max(ZOOM_MIN, +(z - ZOOM_STEP).toFixed(2)));
  const handleResetZoom = () => setZoom(0.75);

  const selectedElement =
    selectedIds.length === 1
      ? elements.find((el) => el.id === selectedIds[0]) ?? null
      : null;

  return (
    <div
      className="fixed inset-0 flex flex-col bg-background text-textPrimary overflow-hidden"
      style={{ fontFamily: 'Inter, sans-serif' }}
    >
      <div className="flex-shrink-0 bg-surface border-b border-surfaceAlt px-4 py-2 flex items-center gap-3">
        <span className="text-sm font-bold text-textPrimary">Editor de Layout</span>
        <span className="text-[10px] font-bold px-2 py-0.5 rounded-full border border-dashed border-accent text-accent">
          DEMO · Fase 1
        </span>
        <span className="text-textMuted text-xs ml-1">Sin conexión al backend — datos en memoria</span>
        <span className="ml-auto text-[10px] text-textMuted">
          Canvas {canvasSize.width}×{canvasSize.height}
          {selectedIds.length > 1 && ` · ${selectedIds.length} seleccionados`}
        </span>
      </div>

      <EditorToolbar
        elements={elements}
        canvasSize={canvasSize}
        zoom={zoom}
        onZoomIn={handleZoomIn}
        onZoomOut={handleZoomOut}
        onResetZoom={handleResetZoom}
        onClear={() => { setElements([]); setSelectedIds([]); setCanvasSize(CANVAS_DEFAULT); }}
      />

      <div className="flex flex-1 min-h-0">
        <ElementPalette onAddElement={handleAddElement} />

        <LayoutEditorCanvas
          elements={elements}
          selectedIds={selectedIds}
          canvasSize={canvasSize}
          onSelect={handleSelect}
          onChange={handleChange}
          onExpandCanvas={handleExpandCanvas}
          onGroupDragEnd={handleGroupDragEnd}
          onAddElement={handleAddElement}
          zoom={zoom}
          onZoomChange={setZoom}
          containerRef={containerRef}
        />

        <PropertiesPanel
          element={selectedElement}
          onChange={handleChange}
          onDelete={handleDelete}
        />
      </div>
    </div>
  );
}
