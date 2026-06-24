import { useState, useRef, useCallback, useEffect, useMemo } from 'react';
import ElementPalette from '../components/layout-editor/ElementPalette';
import LayoutEditorCanvas from '../components/layout-editor/LayoutEditorCanvas';
import PropertiesPanel from '../components/layout-editor/PropertiesPanel';
import EditorToolbar from '../components/layout-editor/EditorToolbar';
import { generateId } from '../components/layout-editor/layoutEditorUtils';

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

// ── Constantes de canvas ──────────────────────────────────────────────────────
const CANVAS_MIN_W  = 1200;
const CANVAS_MIN_H  = 800;
const CANVAS_MARGIN = 150;
const CANVAS_DEFAULT_MANUAL = { width: CANVAS_MIN_W, height: CANVAS_MIN_H };

const ZOOM_MIN  = 0.2;
const ZOOM_MAX  = 4;
const ZOOM_STEP = 0.15;

/**
 * Normaliza las posiciones de los elementos para que ninguno tenga
 * coords menores que CANVAS_MARGIN.
 * Solo se llama en dragEnd, nunca durante el drag.
 */
function normalizePositions(elements) {
  if (elements.length === 0) return elements;
  const minX = Math.min(...elements.map((el) => el.x));
  const minY = Math.min(...elements.map((el) => el.y));
  const offsetX = minX < CANVAS_MARGIN ? CANVAS_MARGIN - minX : 0;
  const offsetY = minY < CANVAS_MARGIN ? CANVAS_MARGIN - minY : 0;
  if (offsetX === 0 && offsetY === 0) return elements;
  return elements.map((el) => ({ ...el, x: el.x + offsetX, y: el.y + offsetY }));
}

export default function EventLayoutEditorDemo() {
  const [elements,         setElements]         = useState(INITIAL_ELEMENTS);
  const [selectedIds,      setSelectedIds]      = useState([]);
  const [zoom,             setZoom]             = useState(0.75);
  const [editingPolygonId, setEditingPolygonId] = useState(null);
  // Fase 1.11: tamaño manual del canvas (resize por el usuario).
  // El canvas real = max(manualCanvasSize, bboxDeElementos + margen).
  const [manualCanvasSize, setManualCanvasSize] = useState(CANVAS_DEFAULT_MANUAL);
  const containerRef = useRef();

  // ── Fase 1.11: canvasSize derivado — siempre envuelve a los elementos ─────
  const canvasSize = useMemo(() => {
    if (elements.length === 0) {
      return { width: manualCanvasSize.width, height: manualCanvasSize.height };
    }
    const maxX = Math.max(...elements.map((el) => el.x + el.width));
    const maxY = Math.max(...elements.map((el) => el.y + el.height));
    return {
      width:  Math.max(manualCanvasSize.width,  CANVAS_MIN_W, maxX + CANVAS_MARGIN),
      height: Math.max(manualCanvasSize.height, CANVAS_MIN_H, maxY + CANVAS_MARGIN),
    };
  }, [elements, manualCanvasSize]);

  // ── Fase 1.3: salir del modo edición si se cambia la selección ────────────
  useEffect(() => {
    if (editingPolygonId && !selectedIds.includes(editingPolygonId)) {
      setEditingPolygonId(null);
    }
  }, [selectedIds, editingPolygonId]);

  // ── Selección ─────────────────────────────────────────────────────────────
  const handleSelect = useCallback((ids) => {
    setSelectedIds(Array.isArray(ids) ? ids : [ids]);
  }, []);

  // ── Modificar elemento ────────────────────────────────────────────────────
  // Sin reconcileCanvas ni setCanvasSize — canvasSize se deriva automáticamente.
  const handleChange = useCallback((updated) => {
    setElements((prev) => prev.map((el) => (el.id === updated.id ? updated : el)));
  }, []);

  // ── Drag grupal ───────────────────────────────────────────────────────────
  const handleGroupDragEnd = useCallback((updatedElements) => {
    setElements((prev) => {
      const merged = prev.map((el) => {
        const u = updatedElements.find((ue) => ue.id === el.id);
        return u ?? el;
      });
      // Normalizar coords negativas al soltar
      return normalizePositions(merged);
    });
  }, []);

  // ── Paleta ────────────────────────────────────────────────────────────────
  const handleAddElement = useCallback((newElement) => {
    setElements((prev) => [...prev, newElement]);
    setSelectedIds([newElement.id]);
  }, []);

  // ── Eliminar ──────────────────────────────────────────────────────────────
  const handleDelete = useCallback(() => {
    setEditingPolygonId(null);
    setElements((prev) => prev.filter((el) => !selectedIds.includes(el.id)));
    setSelectedIds([]);
  }, [selectedIds]);

  // ── Fase 1.3: edición de vértices ─────────────────────────────────────────
  const handleStartVertexEdit = useCallback((elementId) => {
    setEditingPolygonId(elementId);
    setSelectedIds([elementId]);
  }, []);

  const handleEndVertexEdit = useCallback(() => {
    setEditingPolygonId(null);
  }, []);

  // ── Formas sugeridas (preset) ─────────────────────────────────────────────
  // onApplyPreset se pasa a PropertiesPanel y delega a LayoutEditorCanvas
  // via una ref para no recrear el callback en cada render.
  const applyPresetRef = useRef(null);
  const handleApplyPreset = useCallback((newPolygonPoints) => {
    applyPresetRef.current?.(newPolygonPoints);
  }, []);

  // ── Zoom ──────────────────────────────────────────────────────────────────
  const handleZoomIn    = () => setZoom((z) => Math.min(ZOOM_MAX, +(z + ZOOM_STEP).toFixed(2)));
  const handleZoomOut   = () => setZoom((z) => Math.max(ZOOM_MIN, +(z - ZOOM_STEP).toFixed(2)));
  const handleResetZoom = () => setZoom(0.75);

  const selectedElement =
    selectedIds.length === 1
      ? elements.find((el) => el.id === selectedIds[0]) ?? null
      : null;

  const isEditingVertices = editingPolygonId !== null;

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
        {isEditingVertices && (
          <span className="text-[10px] font-bold text-accent border border-accent/40 px-2 py-0.5 rounded-full">
            ✏ Editando vértices — Esc para salir
          </span>
        )}
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
        onClear={() => {
          setElements([]);
          setSelectedIds([]);
          setManualCanvasSize(CANVAS_DEFAULT_MANUAL);
          setEditingPolygonId(null);
        }}
      />

      <div className="flex flex-1 min-h-0">
        <ElementPalette onAddElement={handleAddElement} />

        <LayoutEditorCanvas
          elements={elements}
          selectedIds={selectedIds}
          canvasSize={canvasSize}
          onCanvasSizeChange={setManualCanvasSize}
          editingPolygonId={editingPolygonId}
          onSelect={handleSelect}
          onChange={handleChange}
          onGroupDragEnd={handleGroupDragEnd}
          onStartVertexEdit={handleStartVertexEdit}
          onEndVertexEdit={handleEndVertexEdit}
          onAddElement={handleAddElement}
          zoom={zoom}
          onZoomChange={setZoom}
          containerRef={containerRef}
          onNormalizePositions={(els) => setElements(normalizePositions(els))}
          onRegisterApplyPreset={(fn) => { applyPresetRef.current = fn; }}
        />

        <PropertiesPanel
          element={selectedElement}
          onChange={handleChange}
          onDelete={handleDelete}
          isEditingVertices={isEditingVertices}
          onStartVertexEdit={() => selectedElement && handleStartVertexEdit(selectedElement.id)}
          onEndVertexEdit={handleEndVertexEdit}
          canvasSize={canvasSize}
          onCanvasSizeChange={setManualCanvasSize}
          onApplyPreset={handleApplyPreset}
        />
      </div>
    </div>
  );
}
