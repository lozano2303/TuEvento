import { useState, useRef, useCallback, useEffect, useMemo } from 'react';
import ElementPalette from '../components/layout-editor/ElementPalette';
import LayoutEditorCanvas from '../components/layout-editor/LayoutEditorCanvas';
import PropertiesPanel from '../components/layout-editor/PropertiesPanel';
import EditorToolbar from '../components/layout-editor/EditorToolbar';
import { generateId, getElementAABB, CANVAS_MARGIN } from '../components/layout-editor/layoutEditorUtils';

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
const CANVAS_DEFAULT_MANUAL = { width: CANVAS_MIN_W, height: CANVAS_MIN_H };

const ZOOM_MIN  = 0.2;
const ZOOM_MAX  = 4;
const ZOOM_STEP = 0.15;

/**
 * Normaliza las posiciones de los elementos para que ningún AABB rotado
 * tenga minX/minY menores que CANVAS_MARGIN.
 * Usa getElementAABB para detectar elementos rotados que salen por
 * el borde superior/izquierdo.
 * Solo se llama en dragEnd / transformEnd, nunca durante el drag.
 */
function normalizePositions(els) {
  if (els.length === 0) return els;
  const aabbs  = els.map(getElementAABB);
  const minX   = Math.min(...aabbs.map((b) => b.minX));
  const minY   = Math.min(...aabbs.map((b) => b.minY));
  if (minX >= CANVAS_MARGIN && minY >= CANVAS_MARGIN) return els;
  const offsetX = minX < CANVAS_MARGIN ? CANVAS_MARGIN - minX : 0;
  const offsetY = minY < CANVAS_MARGIN ? CANVAS_MARGIN - minY : 0;
  return els.map((el) => ({ ...el, x: el.x + offsetX, y: el.y + offsetY }));
}

const MAX_HISTORY = 15;

export default function EventLayoutEditorDemo() {
  const [elements,         setElements]         = useState(INITIAL_ELEMENTS);
  const [selectedIds,      setSelectedIds]      = useState([]);
  const [zoom,             setZoom]             = useState(0.75);
  const [editingPolygonId, setEditingPolygonId] = useState(null);
  // Fase 1.11: tamaño manual del canvas (resize por el usuario).
  // El canvas real = auto (bbox elementos) a menos que el usuario haya
  // arrastrado el handle de resize — en ese caso se respeta como mínimo.
  const [manualCanvasSize,  setManualCanvasSize]  = useState(CANVAS_DEFAULT_MANUAL);
  const [userResizedCanvas, setUserResizedCanvas] = useState(false);
  const containerRef = useRef();

  // ── Undo / Redo ───────────────────────────────────────────────────────────
  const [history, setHistory] = useState([]);   // snapshots anteriores de elements
  const [future,  setFuture]  = useState([]);   // snapshots para redo

  // ── Fase 1.11: canvasSize derivado — siempre envuelve a los elementos ─────
  // Solo crece hacia abajo/derecha. dragBoundFunc en los elementos impide
  // que salgan por arriba/izquierda, por lo que manualCanvasSize ya no se
  // usa como piso automático — solo actúa si el usuario arrastró el handle.
  const canvasSize = useMemo(() => {
    if (elements.length === 0)
      return { width: CANVAS_MIN_W, height: CANVAS_MIN_H };
    const aabbs = elements.map(getElementAABB);
    const maxX  = Math.max(...aabbs.map((b) => b.maxX));
    const maxY  = Math.max(...aabbs.map((b) => b.maxY));
    return {
      width:  Math.max(CANVAS_MIN_W, maxX + CANVAS_MARGIN),
      height: Math.max(CANVAS_MIN_H, maxY + CANVAS_MARGIN),
    };
  }, [elements]);

  // Ref estable para canvasSize — dragBoundFunc lo lee para evitar closures stale.
  const canvasSizeRef = useRef(canvasSize);
  useEffect(() => { canvasSizeRef.current = canvasSize; }, [canvasSize]);

  // Callback para resize manual del canvas — activa el flag de override.
  const handleCanvasSizeChange = useCallback((size) => {
    setManualCanvasSize(size);
    setUserResizedCanvas(true);
  }, []);

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
  // Empuja al historial. dragBoundFunc impide que los elementos salgan por
  // cualquier borde durante el drag — normalizePositions ya no es necesaria aquí.
  const handleChange = useCallback((updated) => {
    setElements((prev) => {
      setHistory((h) => [...h.slice(-(MAX_HISTORY - 1)), prev]);
      setFuture([]);
      return prev.map((el) => (el.id === updated.id ? updated : el));
    });
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
  // normalizePositions aquí cubre el caso de drop cerca del borde superior/izquierdo.
  const handleAddElement = useCallback((newElement) => {
    setElements((prev) => normalizePositions([...prev, newElement]));
    setSelectedIds([newElement.id]);
  }, []);

  // ── Eliminar ──────────────────────────────────────────────────────────────
  const handleDelete = useCallback(() => {
    setEditingPolygonId(null);
    setElements((prev) => {
      setHistory((h) => [...h.slice(-(MAX_HISTORY - 1)), prev]);
      setFuture([]);
      return prev.filter((el) => !selectedIds.includes(el.id));
    });
    setSelectedIds([]);
  }, [selectedIds]);

  // ── Eliminar seleccionados por teclado ────────────────────────────────────
  const handleDeleteSelected = useCallback(() => {
    if (selectedIds.length === 0) return;
    setEditingPolygonId(null);
    setElements((prev) => {
      setHistory((h) => [...h.slice(-(MAX_HISTORY - 1)), prev]);
      setFuture([]);
      return prev.filter((el) => !selectedIds.includes(el.id));
    });
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

  // ── Undo ──────────────────────────────────────────────────────────────────
  const handleUndo = useCallback(() => {
    setHistory((h) => {
      if (h.length === 0) return h;
      const prev = h[h.length - 1];
      setFuture((f) => [elements, ...f.slice(0, MAX_HISTORY - 1)]);
      setElements(prev);
      return h.slice(0, -1);
    });
  }, [elements]);

  // ── Redo ──────────────────────────────────────────────────────────────────
  const handleRedo = useCallback(() => {
    setFuture((f) => {
      if (f.length === 0) return f;
      const next = f[0];
      setHistory((h) => [...h.slice(-(MAX_HISTORY - 1)), elements]);
      setElements(next);
      return f.slice(1);
    });
  }, [elements]);

  // ── Atajos de teclado globales ─────────────────────────────────────────────
  useEffect(() => {
    const handler = (e) => {
      const tag = document.activeElement?.tagName;
      const isInput = tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT';
      if (isInput) return;

      // Undo
      if (e.ctrlKey && !e.shiftKey && e.key === 'z') {
        e.preventDefault();
        handleUndo();
        return;
      }
      // Redo
      if ((e.ctrlKey && e.shiftKey && e.key === 'z') || (e.ctrlKey && e.key === 'y')) {
        e.preventDefault();
        handleRedo();
        return;
      }
      // Eliminar seleccionados
      if ((e.key === 'Delete' || e.key === 'Backspace') && selectedIds.length > 0) {
        e.preventDefault();
        handleDeleteSelected();
        return;
      }
      // Deseleccionar
      if (e.key === 'Escape' && selectedIds.length > 0 && !editingPolygonId) {
        e.preventDefault();
        setSelectedIds([]);
        return;
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [handleUndo, handleRedo, selectedIds, handleDeleteSelected, editingPolygonId]);

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
        canUndo={history.length > 0}
        canRedo={future.length > 0}
        onUndo={handleUndo}
        onRedo={handleRedo}
        onClear={() => {
          setElements([]);
          setSelectedIds([]);
          setManualCanvasSize(CANVAS_DEFAULT_MANUAL);
          setUserResizedCanvas(false);
          setEditingPolygonId(null);
          setHistory([]);
          setFuture([]);
        }}
      />

      <div className="flex flex-1 min-h-0">
        <ElementPalette onAddElement={handleAddElement} />

        <LayoutEditorCanvas
          elements={elements}
          selectedIds={selectedIds}
          canvasSize={canvasSize}
          canvasSizeRef={canvasSizeRef}
          onCanvasSizeChange={handleCanvasSizeChange}
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
          onCanvasSizeChange={handleCanvasSizeChange}
          onApplyPreset={handleApplyPreset}
        />
      </div>
    </div>
  );
}
