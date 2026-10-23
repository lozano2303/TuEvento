import { useState, useRef, useCallback, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, ChevronRight, ArrowLeft } from 'lucide-react';
import ElementPalette from '../components/layout-editor/ElementPalette';
import LayoutEditorCanvas from '../components/layout-editor/LayoutEditorCanvas';
import PropertiesPanel from '../components/layout-editor/PropertiesPanel';
import EditorToolbar from '../components/layout-editor/EditorToolbar';
import SectionsList from '../components/layout-editor/SectionsList';
import HelpModal from '../components/layout-editor/HelpModal';
import BackButton from '../components/common/BackButton';
import {
  generateId,
  generateSectionId,
  getElementAABB,
  CANVAS_MARGIN,
  serializeLayout,
  groupBy,
} from '../components/layout-editor/layoutEditorUtils';
import * as EventService        from '../services/EventService';
import * as SectionTypeService  from '../services/SectionTypeService';
import * as EventSectionService from '../services/EventSectionService';
import * as LayoutService       from '../services/LayoutService';

// ── Constantes de canvas ──────────────────────────────────────────────────────
const CANVAS_MIN_W          = 1200;
const CANVAS_MIN_H          = 800;
const CANVAS_DEFAULT_MANUAL = { width: CANVAS_MIN_W, height: CANVAS_MIN_H };
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

export default function EventLayoutEditor() {
  const { eventId } = useParams();
  const navigate    = useNavigate();

  // ── Estado backend ────────────────────────────────────────────────────────
  const [sectionTypes,  setSectionTypes]  = useState(null);
  const [maxSeats,      setMaxSeats]      = useState(null);
  const [eventStatus,   setEventStatus]   = useState(null); // 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED'
  const [eventOwnerId,  setEventOwnerId]  = useState(null);
  const [isSaving,      setIsSaving]      = useState(false);
  const [saveMsg,       setSaveMsg]       = useState(null);
  const [loadError,     setLoadError]     = useState(null);

  // El layout solo es editable cuando el evento está en DRAFT
  const isReadOnly = eventStatus !== null && eventStatus !== 'DRAFT';

  // ── Estado del editor ─────────────────────────────────────────────────────
  const [elements,          setElements]          = useState([]);
  const [selectedIds,       setSelectedIds]       = useState([]);
  const [zoom,              setZoom]              = useState(0.75);
  const [editingPolygonId,  setEditingPolygonId]  = useState(null);
  const [manualCanvasSize,  setManualCanvasSize]  = useState(CANVAS_DEFAULT_MANUAL);
  const [userResizedCanvas, setUserResizedCanvas] = useState(false);
  const containerRef = useRef();

  const [history,   setHistory]   = useState([]);
  const [future,    setFuture]    = useState([]);
  const [clipboard, setClipboard] = useState(null);
  const [helpOpen,  setHelpOpen]  = useState(false);
  const [isLeftPanelOpen,  setIsLeftPanelOpen]  = useState(true);
  const [isRightPanelOpen, setIsRightPanelOpen] = useState(true);

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

  // ── Carga inicial ─────────────────────────────────────────────────────────
  useEffect(() => {
    if (!eventId) { setLoadError('No se especificó un evento'); return; }
    (async () => {
      try {
        const eventRes = await EventService.getEventById(eventId);
        const eventData = eventRes.data;
        setMaxSeats(eventData?.availableSeats ?? null);
        setEventStatus(eventData?.status ?? null);
        setEventOwnerId(eventData?.userId ?? null);

        // Verificar propiedad del evento (solo UX, backend valida de verdad)
        const currentUserId = localStorage.getItem('userID');
        const currentRole = localStorage.getItem('role');
        if (eventData?.userId && currentUserId && currentRole !== 'ADMIN') {
          if (String(eventData.userId) !== String(currentUserId)) {
            setLoadError('No tienes permiso para editar el layout de este evento');
            return;
          }
        }

        const stRes = await SectionTypeService.getAllSectionTypes();
        setSectionTypes(stRes.data ?? []);

        const esRes = await EventSectionService.getByEvent(eventId);
        const existingBackendIds = new Set((esRes.data ?? []).map((s) => s.eventSectionId));

        const layoutRes = await LayoutService.getLayout(eventId);
        if (layoutRes?.data?.layoutData) {
          const parsed = JSON.parse(layoutRes.data.layoutData);
          const loaded = (parsed.elements ?? []).map((el) => {
            if (el.type === 'section' && el.backendSectionId != null
                && !existingBackendIds.has(el.backendSectionId)) {
              return { ...el, backendSectionId: null };
            }
            return el;
          });
          setElements(loaded);
          if (parsed.canvasWidth && parsed.canvasHeight) {
            setManualCanvasSize({ width: parsed.canvasWidth, height: parsed.canvasHeight });
          }
        }
      } catch (err) {
        console.error('[LayoutEditor] Error al cargar:', err);
        setLoadError(err.message);
      }
    })();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [eventId]);

  // ── Salida de error con botón volver ──────────────────────────────────────
  if (loadError) {
    return (
      <div className="flex flex-col items-center justify-center h-screen gap-4 bg-background text-textPrimary">
        <p className="text-textMuted text-sm">No se pudo cargar este evento.</p>
        <p className="text-[11px] text-red-400 font-mono max-w-sm text-center">{loadError}</p>
        <button
          onClick={() => navigate('/events/manage')}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-surfaceAlt text-textSecondary hover:text-textPrimary transition-colors text-sm"
        >
          <ArrowLeft className="w-4 h-4" /> Volver a mis eventos
        </button>
      </div>
    );
  }

  // ── Handlers ──────────────────────────────────────────────────────────────
  const handleSelect = useCallback((ids) => setSelectedIds(Array.isArray(ids) ? ids : [ids]), []);

  const handleChange = useCallback((updated) => {
    setElements((prev) => {
      setHistory((h) => [...h.slice(-(MAX_HISTORY - 1)), prev]);
      setFuture([]);
      return prev.map((el) => {
        if (el.id === updated.id) return updated;
        if (updated.eventSectionId && el.eventSectionId === updated.eventSectionId) {
          const patch = {};
          if (el.color !== updated.color) patch.color = updated.color;
          if (el.price !== updated.price) patch.price = updated.price;
          if (Object.keys(patch).length > 0) return { ...el, ...patch };
        }
        return el;
      });
    });
  }, []);

  const handleGroupDragEnd = useCallback((updatedElements) => {
    setElements((prev) => normalizePositions(prev.map((el) => {
      const u = updatedElements.find((ue) => ue.id === el.id);
      return u ?? el;
    })));
  }, []);

  const handleAddElement = useCallback((newElement) => {
    const el = newElement.type === 'section' && !newElement.eventSectionId
      ? { ...newElement, eventSectionId: generateSectionId() }
      : newElement;
    setElements((prev) => normalizePositions([...prev, el]));
    setSelectedIds([el.id]);
  }, []);

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

  const handleStartVertexEdit = useCallback((id) => { setEditingPolygonId(id); setSelectedIds([id]); }, []);
  const handleEndVertexEdit   = useCallback(() => setEditingPolygonId(null), []);

  const applyPresetRef  = useRef(null);
  const handleApplyPreset = useCallback((pts) => { applyPresetRef.current?.(pts); }, []);

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

  // ── Atajos de teclado ─────────────────────────────────────────────────────
  useEffect(() => {
    const handler = (e) => {
      const tag = document.activeElement?.tagName;
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return;

      if (e.ctrlKey && e.key === 'c') {
        if (selectedIds.length === 1) {
          const el = elements.find((x) => x.id === selectedIds[0]);
          if (el?.type === 'section') { e.preventDefault(); setClipboard(el); }
        }
        return;
      }
      if (e.ctrlKey && e.key === 'v') {
        if (!clipboard) return;
        e.preventDefault();
        const siblings  = elements.filter((x) => x.eventSectionId === clipboard.eventSectionId);
        const baseLabel = clipboard.label.replace(/ \d+$/, '');
        const newEl = {
          ...clipboard, id: generateId(),
          eventSectionId:  clipboard.eventSectionId,
          backendSectionId: clipboard.backendSectionId,
          label: `${baseLabel} ${siblings.length + 1}`,
          x: clipboard.x + siblings.length * 20,
          y: clipboard.y + siblings.length * 20,
        };
        setHistory((h) => [...h.slice(-(MAX_HISTORY - 1)), elements]);
        setFuture([]);
        setElements((prev) => [...prev, newEl]);
        setSelectedIds([newEl.id]);
        return;
      }
      if (e.ctrlKey && !e.shiftKey && e.key === 'z') { e.preventDefault(); handleUndo(); return; }
      if ((e.ctrlKey && e.shiftKey && e.key === 'z') || (e.ctrlKey && e.key === 'y')) { e.preventDefault(); handleRedo(); return; }
      if ((e.key === 'Delete' || e.key === 'Backspace') && selectedIds.length > 0) { e.preventDefault(); handleDeleteSelected(); return; }
      if (e.key === 'Escape' && selectedIds.length > 0 && !editingPolygonId) { e.preventDefault(); setSelectedIds([]); return; }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [handleUndo, handleRedo, selectedIds, handleDeleteSelected, editingPolygonId, clipboard, elements]);

  useEffect(() => {
    if (editingPolygonId && !selectedIds.includes(editingPolygonId)) setEditingPolygonId(null);
  }, [selectedIds, editingPolygonId]);

  // ── Derived ───────────────────────────────────────────────────────────────
  const occupiedSectionCounts = useMemo(() => {
    const counts = {};
    for (const el of elements)
      if (el.type === 'section' && el.sectionType)
        counts[el.sectionType] = (counts[el.sectionType] ?? 0) + 1;
    return counts;
  }, [elements]);

  const totalSeats     = useMemo(() =>
    elements.filter((el) => el.type === 'section').reduce((s, el) => s + (el.seatLayout?.targetSeats ?? 0), 0),
    [elements]);
  const isOverCapacity = maxSeats != null && totalSeats > maxSeats;

  // ── handleSave ────────────────────────────────────────────────────────────
  const handleSave = useCallback(async () => {
    if (isReadOnly) {
      setSaveMsg('El layout solo se puede editar cuando el evento está en DRAFT');
      setTimeout(() => setSaveMsg(null), 4000);
      return;
    }
    if (isOverCapacity) {
      setSaveMsg('Excede el aforo configurado para este evento — ajusta las sillas antes de guardar');
      setTimeout(() => setSaveMsg(null), 4000);
      return;
    }
    setIsSaving(true);
    try {
      const groups = groupBy(elements.filter((e) => e.type === 'section'), 'eventSectionId');
      console.log('[handleSave] groups:', JSON.stringify(
        Object.entries(groups).map(([id, g]) => ({
          eventSectionId: id, count: g.length,
          sectionTypeId: g[0]?.sectionTypeId, backendSectionId: g[0]?.backendSectionId,
          price: g[0]?.price,
          capacity: g.reduce((s, el) => s + (el.seatLayout?.targetSeats ?? 0), 0),
        })), null, 2));

      const updatedElements = [...elements];
      for (const [, group] of Object.entries(groups)) {
        const capacity = group.reduce((s, el) => s + (el.seatLayout?.targetSeats ?? 0), 0);
        const price    = group[0].price ?? 0;
        const existingBackendId = group[0].backendSectionId;

        let backendSection;
        if (existingBackendId) {
          backendSection = (await EventSectionService.update(existingBackendId, { capacity, price })).data;
        } else {
          backendSection = (await EventSectionService.create({
            eventId: Number(eventId), sectionTypeId: group[0].sectionTypeId, capacity, price,
          })).data;
        }
        group.forEach((el) => {
          const idx = updatedElements.findIndex((u) => u.id === el.id);
          if (idx !== -1)
            updatedElements[idx] = { ...updatedElements[idx], backendSectionId: backendSection.eventSectionId };
        });
      }

      const currentBackendIds = updatedElements
        .filter((e) => e.type === 'section' && e.backendSectionId != null)
        .map((e) => e.backendSectionId);
      const existingRes = await EventSectionService.getByEvent(eventId);
      for (const orphan of (existingRes.data ?? []).filter((s) => !currentBackendIds.includes(s.eventSectionId)))
        await EventSectionService.remove(orphan.eventSectionId);

      setElements(updatedElements);
      await LayoutService.saveLayout(eventId, serializeLayout(updatedElements, canvasSize?.width, canvasSize?.height));
      setSaveMsg('✓ Guardado y sincronizado correctamente');
    } catch (err) {
      console.error('[LayoutEditor] Error al guardar:', err);
      setSaveMsg(`Error al guardar: ${err.message}`);
    } finally {
      setIsSaving(false);
      setTimeout(() => setSaveMsg(null), 4000);
    }
  }, [eventId, isReadOnly, isOverCapacity, elements, canvasSize]);

  const selectedElement   = selectedIds.length === 1 ? elements.find((el) => el.id === selectedIds[0]) ?? null : null;

  const handleAddSubSection = useCallback(() => {
    if (!selectedElement || selectedElement.type !== 'section') return;
    
    // Reutilizar la lógica existente de Ctrl+V (duplicación)
    const siblings = elements.filter((x) => x.eventSectionId === selectedElement.eventSectionId);
    const baseLabel = selectedElement.label.replace(/ \d+$/, '');
    const newEl = {
      ...selectedElement, 
      id: generateId(),
      eventSectionId: selectedElement.eventSectionId,  // Mantener mismo ID lógico
      backendSectionId: selectedElement.backendSectionId, // Mantener mismo backend ID
      label: `${baseLabel} ${siblings.length + 1}`,
      x: selectedElement.x + siblings.length * 20,
      y: selectedElement.y + siblings.length * 20,
    };
    
    setHistory((h) => [...h.slice(-(MAX_HISTORY - 1)), elements]);
    setFuture([]);
    setElements((prev) => [...prev, newEl]);
    setSelectedIds([newEl.id]);
  }, [selectedElement, elements]);
  const isEditingVertices = editingPolygonId !== null;

  return (
    <div className="fixed inset-0 flex flex-col bg-background text-textPrimary overflow-hidden" style={{ fontFamily: 'Inter, sans-serif' }}>

      {/* Topbar */}
      <div className="flex-shrink-0 bg-surface border-b border-surfaceAlt px-4 py-2 flex items-center gap-3">
        <BackButton fallback="/events/manage" label="Mis eventos" />
        <div className="w-px h-4 bg-surfaceAlt" />
        <span className="text-sm font-bold text-textPrimary">Editor de Layout</span>
        <span className="text-[10px] font-bold px-2 py-0.5 rounded-full border border-dashed border-primary text-primary">
          Evento #{eventId}
        </span>
        {isEditingVertices && (
          <span className="text-[10px] font-bold text-accent border border-accent/40 px-2 py-0.5 rounded-full">
            ✏ Editando vértices — Esc para salir
          </span>
        )}
        {isReadOnly && (
          <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-yellow-500/15 text-yellow-400 border border-yellow-400/30">
            Solo lectura — el evento está {eventStatus}
          </span>
        )}
        {selectedElement?.type === 'section' && !isReadOnly && (
          <button
            onClick={handleAddSubSection}
            className="ml-auto text-[10px] font-semibold px-3 py-1.5 rounded-lg bg-primary/10 text-primary border border-primary/30 hover:bg-primary/20 hover:border-primary/50 transition-colors flex items-center gap-1.5"
            title="Agregar sub-sección vinculada"
          >
            <span className="text-sm">+</span>
            <span>Agregar sub-sección</span>
          </button>
        )}
        <span className={`text-[10px] text-textMuted ${selectedElement?.type === 'section' && !isReadOnly ? '' : 'ml-auto'}`}>
          Canvas {canvasSize.width}×{canvasSize.height}
          {selectedIds.length > 1 && ` · ${selectedIds.length} seleccionados`}
        </span>
      </div>

      <EditorToolbar
        elements={elements} canvasSize={canvasSize}
        canUndo={history.length > 0} canRedo={future.length > 0}
        onUndo={handleUndo} onRedo={handleRedo}
        onOpenHelp={() => setHelpOpen(true)}
        onSave={handleSave} isSaving={isSaving} maxSeats={maxSeats} isReadOnly={isReadOnly}
        onClear={() => {
          setElements([]); setSelectedIds([]); setManualCanvasSize(CANVAS_DEFAULT_MANUAL);
          setUserResizedCanvas(false); setEditingPolygonId(null);
          setHistory([]); setFuture([]); setClipboard(null);
        }}
      />

      {saveMsg && (
        <div className="fixed bottom-4 left-1/2 -translate-x-1/2 z-50 bg-surface border border-accent/40
                        text-textPrimary text-xs px-4 py-2.5 rounded-xl shadow-lg shadow-primary/20
                        flex items-center gap-2 max-w-sm text-center pointer-events-none">
          <span>💾</span><span>{saveMsg}</span>
        </div>
      )}

      <div className="flex flex-1 min-h-0">
        {/* Panel izquierdo */}
        <div className="relative flex-shrink-0">
          <div className={`overflow-hidden transition-[max-width] duration-200 ease-in-out h-full ${isLeftPanelOpen ? 'max-w-[220px]' : 'max-w-0'}`}>
            <ElementPalette onAddElement={handleAddElement} occupiedSectionCounts={occupiedSectionCounts} sectionTypes={sectionTypes} />
          </div>
          <button onClick={() => setIsLeftPanelOpen((v) => !v)}
            className="absolute top-1/2 -right-3 -translate-y-1/2 z-30 w-6 h-6 rounded-full bg-surface border border-surfaceAlt shadow-md flex items-center justify-center text-textMuted hover:text-textPrimary hover:bg-surfaceAlt transition-colors">
            {isLeftPanelOpen ? <ChevronLeft className="w-3.5 h-3.5" /> : <ChevronRight className="w-3.5 h-3.5" />}
          </button>
        </div>

        <LayoutEditorCanvas
          elements={elements} selectedIds={selectedIds} canvasSize={canvasSize} canvasSizeRef={canvasSizeRef}
          onCanvasSizeChange={handleCanvasSizeChange} editingPolygonId={editingPolygonId}
          onSelect={handleSelect} onChange={handleChange} onGroupDragEnd={handleGroupDragEnd}
          onStartVertexEdit={handleStartVertexEdit} onEndVertexEdit={handleEndVertexEdit}
          onAddElement={handleAddElement} zoom={zoom} onZoomChange={setZoom}
          containerRef={containerRef} onRegisterApplyPreset={(fn) => { applyPresetRef.current = fn; }}
        />

        {/* Panel derecho */}
        <div className="relative flex-shrink-0 h-full">
          <div className={`flex flex-col h-full overflow-hidden transition-[max-width] duration-200 ease-in-out ${isRightPanelOpen ? 'max-w-[240px]' : 'max-w-0'}`}>
            <PropertiesPanel
              element={selectedElement} elements={elements} onChange={handleChange} onDelete={handleDelete}
              isEditingVertices={isEditingVertices}
              onStartVertexEdit={() => selectedElement && handleStartVertexEdit(selectedElement.id)}
              onEndVertexEdit={handleEndVertexEdit} canvasSize={canvasSize}
              onCanvasSizeChange={handleCanvasSizeChange} onApplyPreset={handleApplyPreset}
            />
            <div className="flex-1 overflow-y-auto border-t border-surfaceAlt min-h-0 w-[240px] bg-surface">
              <SectionsList elements={elements} selectedIds={selectedIds} onSelect={(id) => setSelectedIds([id])} />
            </div>
          </div>
          <button onClick={() => setIsRightPanelOpen((v) => !v)}
            className="absolute top-1/2 -left-3 -translate-y-1/2 z-30 w-6 h-6 rounded-full bg-surface border border-surfaceAlt shadow-md flex items-center justify-center text-textMuted hover:text-textPrimary hover:bg-surfaceAlt transition-colors">
            {isRightPanelOpen ? <ChevronRight className="w-3.5 h-3.5" /> : <ChevronLeft className="w-3.5 h-3.5" />}
          </button>
        </div>
      </div>

      <HelpModal isOpen={helpOpen} onClose={() => setHelpOpen(false)} />
    </div>
  );
}
