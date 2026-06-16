import { useState, useRef, useCallback } from 'react';
import ElementPalette from '../components/layout-editor/ElementPalette';
import LayoutEditorCanvas from '../components/layout-editor/LayoutEditorCanvas';
import PropertiesPanel from '../components/layout-editor/PropertiesPanel';
import EditorToolbar from '../components/layout-editor/EditorToolbar';
import { generateId } from '../components/layout-editor/layoutEditorUtils';

// ── Elementos de ejemplo precargados ─────────────────────────────────────────
const INITIAL_ELEMENTS = [
  {
    id: generateId(),
    type: 'stage',
    sectionType: null,
    eventSectionId: null,
    seatLayout: null,
    x: 490,
    y: 60,
    width: 220,
    height: 90,
    rotation: 0,
    label: 'Escenario',
    color: '#1E293B',
  },
  {
    id: generateId(),
    type: 'section',
    sectionType: 'VIP',
    eventSectionId: null,
    seatLayout: { rows: 4, cols: 6, seatRadius: 7, gap: 4 },
    x: 100,
    y: 200,
    width: 200,
    height: 150,
    rotation: 0,
    label: 'VIP Izquierda',
    color: '#7C3AED',
  },
  {
    id: generateId(),
    type: 'section',
    sectionType: 'General',
    eventSectionId: null,
    seatLayout: { rows: 6, cols: 8, seatRadius: 7, gap: 4 },
    x: 400,
    y: 220,
    width: 400,
    height: 220,
    rotation: 0,
    label: 'General',
    color: '#16A34A',
  },
  {
    id: generateId(),
    type: 'entrance',
    sectionType: null,
    eventSectionId: null,
    seatLayout: null,
    x: 560,
    y: 700,
    width: 80,
    height: 50,
    rotation: 0,
    label: 'Entrada',
    color: '#15803D',
  },
];

const ZOOM_MIN = 0.2;
const ZOOM_MAX = 4;
const ZOOM_STEP = 0.15;

export default function EventLayoutEditorDemo() {
  const [elements, setElements] = useState(INITIAL_ELEMENTS);
  const [selectedIds, setSelectedIds] = useState([]);
  const [zoom, setZoom] = useState(0.75);
  const containerRef = useRef();

  // ── Selección ─────────────────────────────────────────────────────────────
  const handleSelect = useCallback((ids) => {
    setSelectedIds(Array.isArray(ids) ? ids : [ids]);
  }, []);

  // ── Modificar un elemento ─────────────────────────────────────────────────
  const handleChange = useCallback((updated) => {
    setElements((prev) => prev.map((el) => (el.id === updated.id ? updated : el)));
  }, []);

  // ── Añadir desde paleta ───────────────────────────────────────────────────
  const handleAddElement = useCallback((newElement) => {
    setElements((prev) => [...prev, newElement]);
    setSelectedIds([newElement.id]);
  }, []);

  // ── Eliminar seleccionado ─────────────────────────────────────────────────
  const handleDelete = useCallback(() => {
    setElements((prev) => prev.filter((el) => !selectedIds.includes(el.id)));
    setSelectedIds([]);
  }, [selectedIds]);

  // ── Zoom ──────────────────────────────────────────────────────────────────
  const handleZoomIn  = () => setZoom((z) => Math.min(ZOOM_MAX, +(z + ZOOM_STEP).toFixed(2)));
  const handleZoomOut = () => setZoom((z) => Math.max(ZOOM_MIN, +(z - ZOOM_STEP).toFixed(2)));
  const handleResetZoom = () => setZoom(0.75);

  // Elemento seleccionado (solo uno para el panel de propiedades)
  const selectedElement =
    selectedIds.length === 1
      ? elements.find((el) => el.id === selectedIds[0]) ?? null
      : null;

  return (
    <div
      className="fixed inset-0 flex flex-col bg-background text-textPrimary overflow-hidden"
      style={{ fontFamily: 'Inter, sans-serif' }}
    >
      {/* Header de la página */}
      <div className="flex-shrink-0 bg-surface border-b border-surfaceAlt px-4 py-2 flex items-center gap-3">
        <span className="text-sm font-bold text-textPrimary">Editor de Layout</span>
        <span className="text-[10px] font-bold px-2 py-0.5 rounded-full border border-dashed border-accent text-accent">
          DEMO · Fase 1
        </span>
        <span className="text-textMuted text-xs ml-1">
          Sin conexión al backend — datos en memoria
        </span>
      </div>

      {/* Toolbar */}
      <EditorToolbar
        elements={elements}
        zoom={zoom}
        onZoomIn={handleZoomIn}
        onZoomOut={handleZoomOut}
        onResetZoom={handleResetZoom}
        onClear={() => { setElements([]); setSelectedIds([]); }}
      />

      {/* Cuerpo: paleta + canvas + propiedades */}
      <div className="flex flex-1 min-h-0">
        <ElementPalette onAddElement={handleAddElement} />

        <LayoutEditorCanvas
          elements={elements}
          selectedIds={selectedIds}
          onSelect={handleSelect}
          onChange={handleChange}
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
