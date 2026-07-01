import { useState, useRef, useCallback, useEffect, useMemo } from 'react';
import ElementPalette from '../components/layout-editor/ElementPalette';
import LayoutEditorCanvas from '../components/layout-editor/LayoutEditorCanvas';
import PropertiesPanel from '../components/layout-editor/PropertiesPanel';
import EditorToolbar from '../components/layout-editor/EditorToolbar';
import SectionsList from '../components/layout-editor/SectionsList';
import { generateId, generateSectionId, getElementAABB, CANVAS_MARGIN } from '../components/layout-editor/layoutEditorUtils';

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
    eventSectionId: generateSectionId(),
    seatLayout: { rows: 4, cols: 6, seatRadius: 7, gap: 4 },
    x: 100, y: 200, width: 216, height: 140,
    rotation: 0, label: 'VIP Izquierda', color: '#7C3AED',
  },
  {
    id: generateId(),
    type: 'section',
    sectionType: 'General',
    eventSectionId: generateSectionId(),
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
const CANVAS_MIN_W          = 1200;
const CANVAS_MIN_H          = 800;
const CANVAS_DEFAULT_MANUAL = { width: CANVAS_MIN_W, height: CANVAS_MIN_H };
const ZOOM_MIN  = 0.2;
const ZOOM_MAX  = 4;
const ZOOM_STEP = 0.15;
const MAX_HISTORY = 15;

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

export default function EventLayoutEditorDemo() {
  const [elements,         setElements]         = useState(INITIAL_ELEMENTS);
  const [selectedIds,      setSelectedIds]      = useState([]);
  const [zoom,             setZoom]             = useState(0.75);
  const [editingPolygonId, setEditingPolygonId] = useState(null);
  const [manualCanvasSize, setManualCanvasSize] = useState(CANVAS_DEFAULT_MANUAL);
  const [userResizedCanvas, setUserResizedCanvas] = useState(false);
  const containerRef = useRef();

  // ── Undo / Redo ───────────────────────────────────────────────────────────
  const [history,   setHistory]   = useState([]);
  const [future,    setFuture]    = useState([]);
  const [clipboard, setClipboard] = useState(null);

  // ── canvasSize derivado ───────────────────────────────────────────────────
  const canvasSize = useMemo(() => {
    if (elements.length === 0)
      return { width: manualCanvasSize.width, height: manualCanvasSize.height };
    const aabbs = elements.map(getElementAABB);
    const maxX  = Math.max(...aabbs.map((b) => b.maxX));
    const maxY  = Math.max(...aabbs.map((b) => b.maxY));
    const autoW = Math.max(CANVAS_MIN_W, maxX + CANVAS_MARGIN);
    const autoH = Math.max(CANVAS_MIN_H, maxY + CANVAS_MARGIN);
    return {
      width:  userResizedCanvas ? Math.max(manualCanvasSize.width,  autoW) : autoW,
      height: userResizedCanvas ? Math.max(manualCanvasSize.height, autoH) : autoH,
    };
  }, [elements, manualCanvasSize, userResizedCanvas]);

  const canvasSizeRef = useRef(canvasSize);
  useEffect(() => { canvasSizeRef.current = canvasSize; }, [canvasSize]);

  const handleCanvasSizeChange = useCallback((size) => {
    setManualCanvasSize(size);
    setUserResizedCanvas(true);
  }, []);

  // ── Salir del modo edición si cambia la selección ─────────────────────────
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
      return normalizePositions(merged);
    });
  }, []);

  // ── Paleta — asigna eventSectionId a nuevas secciones ────────────────────
  const handleAddElement = useCallback((newElement) => {
    const el = newElement.type === 'section' && !newElement.eventSectionId
      ? { ...newElement, eventSectionId: generateSectionId() }
      : newElement;
    setElements((prev) => normalizePositions([...prev, el]));
    setSelectedIds([el.id]);
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

  // ── Edición de vértices ───────────────────────────────────────────────────
  const handleStartVertexEdit = useCallback((elementId) => {
    setEditingPolygonId(elementId);
    setSelectedIds([elementId]);
  }, []);

  const handleEndVertexEdit = useCallback(() => {
    setEditingPolygonId(null);
  }, []);

  // ── Formas sugeridas (preset) ─────────────────────────────────────────────
  const applyPresetRef = useRef(null);
  const handleApplyPreset = useCallback((newPolygonPoints) => {
    applyPresetRef.current?.(newPolygonPoints);
  }, []);

  // ── Undo / Redo ───────────────────────────────────────────────────────────
  const handleUndo = useCallback(() => {
    setHistory((h) => {
      if (h.length === 0) return h;
      const prev = h[h.length - 1];
      setFuture((f) => [elements, ...f.slice(0, MAX_HISTORY - 1)]);
      setElements(prev);
      return h.slice(0, -1);
    });
  }, [elements]);

  const handleRedo = useCallback(() => {
    setFuture((f) => {
      if (f.length === 0) return f;
      const next = f[0];
      setHistory((h) => [...h.slice(-(MAX_HISTORY - 1)), elements]);
      setElements(next);
      return f.slice(1);
    });
  }, [elements]);

  // ── Atajos de teclado globales ────────────────────────────────────────────
  useEffect(() => {
    const handler = (e) => {
      const tag     = document.activeElement?.tagName;
      const isInput = tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT';
      if (isInput) return;

      // Ctrl+C — copiar sección seleccionada
      if (e.ctrlKey && e.key === 'c') {
        if (selectedIds.length === 1) {
          const el = elements.find((x) => x.id === selectedIds[0]);
          if (el?.type === 'section') {
            e.preventDefault();
            setClipboard(el);
          }
        }
        return;
      }
      // Ctrl+V — pegar con mismo eventSectionId, offset 20px
      if (e.ctrlKey && e.key === 'v') {
        if (!clipboard) return;
        e.preventDefault();
        const siblings  = elements.filter((x) => x.eventSectionId === clipboard.eventSectionId);
        const suffix    = siblings.length + 1;
        const baseLabel = clipboard.label.replace(/ \d+$/, '');
        const newEl = {
          ...clipboard,
          id:             generateId(),
          eventSectionId: clipboard.eventSectionId,
          label:          `${baseLabel} ${suffix}`,
          x:              clipboard.x + 20,
          y:              clipboard.y + 20,
        };
        setHistory((h) => [...h.slice(-(MAX_HISTORY - 1)), elements]);
        setFuture([]);
        setElements((prev) => [...prev, newEl]);
        setSelectedIds([newEl.id]);
        return;
      }
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
      // Delete
      if ((e.key === 'Delete' || e.key === 'Backspace') && selectedIds.length > 0) {
        e.preventDefault();
        handleDeleteSelected();
        return;
      }
      // Escape
      if (e.key === 'Escape' && selectedIds.length > 0 && !editingPolygonId) {
        e.preventDefault();
        setSelectedIds([]);
        return;
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [handleUndo, handleRedo, selectedIds, handleDeleteSelected, editingPolygonId, clipboard, elements]);

  // ── Zoom ──────────────────────────────────────────────────────────────────
  const handleZoomIn    = () => setZoom((z) => Math.min(ZOOM_MAX, +(z + ZOOM_STEP).toFixed(2)));
  const handleZoomOut   = () => setZoom((z) => Math.max(ZOOM_MIN, +(z - ZOOM_STEP).toFixed(2)));
  const handleResetZoom = () => setZoom(0.75);

  // ── Conteo de secciones por tipo (para badge en paleta) ─────────────────
  const occupiedSectionCounts = useMemo(() => {
    const counts = {};
    for (const el of elements) {
      if (el.type === 'section' && el.sectionType) {
        counts[el.sectionType] = (counts[el.sectionType] ?? 0) + 1;
      }
    }
    return counts;
  }, [elements]);

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
      {/* ── Topbar ──────────────────────────────────────────────────────── */}
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
          setClipboard(null);
        }}
      />

      <div className="flex flex-1 min-h-0">
        <ElementPalette
          onAddElement={handleAddElement}
          occupiedSectionCounts={occupiedSectionCounts}
        />

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

        <div className="flex flex-col flex-shrink-0 h-full overflow-hidden">
          <PropertiesPanel
            element={selectedElement}
            elements={elements}
            onChange={handleChange}
            onDelete={handleDelete}
            isEditingVertices={isEditingVertices}
            onStartVertexEdit={() => selectedElement && handleStartVertexEdit(selectedElement.id)}
            onEndVertexEdit={handleEndVertexEdit}
            canvasSize={canvasSize}
            onCanvasSizeChange={handleCanvasSizeChange}
            onApplyPreset={handleApplyPreset}
          />
          <div className="flex-1 overflow-y-auto border-t border-surfaceAlt min-h-0 w-[240px] bg-surface">
            <SectionsList
              elements={elements}
              selectedIds={selectedIds}
              onSelect={(id) => setSelectedIds([id])}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
