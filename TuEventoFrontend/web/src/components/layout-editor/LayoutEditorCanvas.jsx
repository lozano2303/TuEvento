import { useRef, useState, useCallback, useMemo, useEffect} from 'react';
import { Stage, Layer, Line, Rect } from 'react-konva';
import SectionElement from './elements/SectionElement';
import InfraElement from './elements/InfraElement';
import VertexEditorOverlay from './VertexEditorOverlay';
import {
  generateId, snapToGrid, rectsIntersect,
  findSnapGuides, migratePolygonPoints, getElementAABB,
} from './layoutEditorUtils';

const GRID_SIZE          = 20;
const ZOOM_MIN           = 0.2;
const ZOOM_MAX           = 3;
const ZOOM_STEP          = 0.1;
const FIT_MARGIN         = 40;
const SNAP_THRESHOLD     = 6;
const GUIDE_COLOR        = '#FF4D8F';
const GUIDE_COLOR_VERTEX = '#9B6BFF';
const GUIDE_EXTENT       = 10000;

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
  canvasSizeRef,
  onCanvasSizeChange,
  editingPolygonId,
  onSelect,
  onChange,
  onGroupDragEnd,
  onStartVertexEdit,
  onEndVertexEdit,
  onAddElement,
  onRegisterApplyPreset, // Formas sugeridas: registra handleApplyPreset en el padre
  zoom,
  onZoomChange,
  containerRef,
}) {
  const stageRef = useRef();
  const [stagePos, setStagePos] = useState({ x: 0, y: 0 });

  // Refs estables para zoom y stagePos — usados en dragBoundFunc para evitar closures stale
  const zoomRef     = useRef(zoom);
  const stagePosRef = useRef(stagePos);
  const stageRefForBound = stageRef; // alias para pasar a elementos
  zoomRef.current     = zoom;
  stagePosRef.current = stagePos;

  // Pan (click derecho)
  const panState = useRef({ active: false, startPointer: null, startStagePos: null });

  // Rubber-band (Ctrl+drag)
  const [selBox, setSelBox] = useState(null);
  const isRubberBand = useRef(false);

  // Multi-drag
  const groupDragState = useRef({ active: false, leaderId: null, startPositions: {}, leaderStart: null });
  const [followerPositions, setFollowerPositions] = useState({});

  // Fase 1.10: handle de resize del canvas
  const startCanvasResize = useCallback((e) => {
    if (!onCanvasSizeChange) return;
    e.preventDefault();
    const startX = e.clientX;
    const startY = e.clientY;
    const startW = canvasSize.width;
    const startH = canvasSize.height;
    const onMouseMove = (ev) => {
      onCanvasSizeChange({
        width:  Math.max(800,  startW + (ev.clientX - startX)),
        height: Math.max(600, startH + (ev.clientY - startY)),
      });
    };
    const onMouseUp = () => {
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
    };
    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  }, [canvasSize, onCanvasSizeChange]);

  // Fase 1.6: smart guides globales
  const [activeGuides, setActiveGuides] = useState({ vertical: null, horizontal: null });

  // Fase 1.7/1.8: guías de vértices — líneas cortas { x1,y1,x2,y2 }
  const [activeVertexGuides, setActiveVertexGuides] = useState({ vertical: null, horizontal: null });

  // Fase 1.8: estado local de preview de vértices mientras se arrastra
  // { points: [[rx,ry],...] } — solo se usa como previewPoints en SectionElement y VertexEditorOverlay
  const [vertexPreview, setVertexPreview] = useState(null);

  // Fase 1.8: snapshot para revertir con Escape
  const vertexSnapshotRef = useRef(null);

  const gridLines = useMemo(
    () => buildGridLines(canvasSize.width, canvasSize.height, GRID_SIZE),
    [canvasSize.width, canvasSize.height],
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

  // ── Drop — Fix 2: compensar zoom y pan para posición correcta ───────────
  const handleDrop = useCallback((e) => {
    e.preventDefault();
    const raw = e.dataTransfer.getData('template');
    if (!raw) return;
    const template = JSON.parse(raw);
    const stage = stageRef.current;
    if (!stage) return;

    // Leer posición y escala directamente del nodo Konva (evita stale closure)
    const scale    = stage.scaleX();
    const pos      = stage.position();
    const rect     = stage.container().getBoundingClientRect();

    // Coordenadas del puntero relativas al contenedor DOM del Stage
    const pointerX = e.clientX - rect.left;
    const pointerY = e.clientY - rect.top;

    // Convertir a coordenadas del canvas compensando zoom y pan
    const canvasX = (pointerX - pos.x) / scale;
    const canvasY = (pointerY - pos.y) / scale;

    const cx = snapToGrid(canvasX - template.defaultWidth  / 2);
    const cy = snapToGrid(canvasY - template.defaultHeight / 2);

    onAddElement({
      id:               generateId(),
      type:             template.type,
      sectionType:      template.sectionType      ?? null,
      sectionTypeId:    template.sectionTypeId    ?? null, // ← campo nuevo (entero backend)
      backendSectionId: template.backendSectionId ?? null, // ← campo nuevo
      price:            template.price            ?? null, // ← campo nuevo
      eventSectionId:   null,
      seatLayout:       template.seatLayout ?? null,
      x:                Math.max(0, cx),
      y:                Math.max(0, cy),
      width:            template.defaultWidth,
      height:           template.defaultHeight,
      rotation:         0,
      label:            template.label,
      color:            template.color,
    });
  }, [onAddElement]);
  const handleDragOver = (e) => e.preventDefault();

  // ── Fase 1.8: Vertex edit — commit / discard ──────────────────────────────
  const commitVertexEdit = useCallback(() => {
    if (!editingPolygonId || !vertexPreview) { onEndVertexEdit?.(); return; }
    const el = elements.find((e) => e.id === editingPolygonId);
    if (!el) { onEndVertexEdit?.(); return; }
    const migrated = migratePolygonPoints(vertexPreview);
    const patch = normalizePoints(migrated, el.x, el.y);
    onChange({ ...el, ...patch });
    setVertexPreview(null);
    vertexPreviewRef.current = null;
    vertexSnapshotRef.current = null;
    onEndVertexEdit?.();
  }, [editingPolygonId, vertexPreview, elements, onChange, onEndVertexEdit]);

  const discardVertexEdit = useCallback(() => {
    if (editingPolygonId && vertexSnapshotRef.current) {
      const el = elements.find((e) => e.id === editingPolygonId);
      if (el) onChange({ ...el, polygonPoints: vertexSnapshotRef.current });
    }
    setVertexPreview(null);
    vertexPreviewRef.current = null;
    vertexSnapshotRef.current = null;
    onEndVertexEdit?.();
  }, [editingPolygonId, elements, onChange, onEndVertexEdit]);

  /**
   * Aplica un preset de forma al elemento en edición.
   * Actualiza preview, ref y snapshot — el usuario sigue en modo edición
   * y Escape revertirá al preset (no a la forma anterior al preset).
   */
  const handleApplyPreset = useCallback((newPolygonPoints) => {
    if (!editingPolygonId) return;
    const el = elements.find((e) => e.id === editingPolygonId);
    if (!el) return;
    const copy = newPolygonPoints.map((p) => ({ ...p }));
    // Actualizar snapshot: Escape ahora revierte al preset recién aplicado
    vertexSnapshotRef.current = copy;
    vertexPreviewRef.current  = copy;
    setVertexPreview(copy);
    // Persistir en el estado del elemento (shapeMode puede ya ser 'polygon')
    onChange({ ...el, shapeMode: 'polygon', polygonPoints: copy });
  }, [editingPolygonId, elements, onChange]);

  // Registrar el handler en el padre para que PropertiesPanel lo invoque
  useEffect(() => {
    onRegisterApplyPreset?.(handleApplyPreset);
  }, [handleApplyPreset, onRegisterApplyPreset]);

  // Inicializar preview y snapshot al entrar/salir del modo edición.
  // useEffect garantiza que la inicialización ocurre DESPUÉS de que React
  // commitea el render, eliminando la carrera entre commitVertexEdit de la
  // sesión anterior y setVertexPreview de la nueva.
  useEffect(() => {
    if (editingPolygonId) {
      const el = elements.find((e) => e.id === editingPolygonId);
      if (el?.polygonPoints) {
        const migrated = migratePolygonPoints(el.polygonPoints);
        const copy = migrated.map((p) => ({ ...p }));
        vertexSnapshotRef.current = copy;
        vertexPreviewRef.current  = copy;
        setVertexPreview(copy);
      }
    } else {
      setVertexPreview(null);
      vertexPreviewRef.current  = null;
      vertexSnapshotRef.current = null;
    }
  }, [editingPolygonId]); // eslint-disable-line react-hooks/exhaustive-deps

  // ── Fase 1.8: Escape → descartar cambios de vértices ─────────────────────
  // Usamos un ref para que el listener tenga siempre la versión más reciente
  // sin necesidad de re-registrar con cada render.
  const discardRef = useRef(discardVertexEdit);
  discardRef.current = discardVertexEdit;
  const commitRef = useRef(commitVertexEdit);
  commitRef.current = commitVertexEdit;
  const editingRef = useRef(editingPolygonId);
  editingRef.current = editingPolygonId;

  // El listener se registra una sola vez al montar el componente.
  useCallback(() => {}, []); // eslint lint-hint: no-op para satisfacer orden de hooks
  // Registrar Escape/Enter como efecto de montaje usando ref estables:
  const escapeRegistered = useRef(false);
  if (!escapeRegistered.current) {
    escapeRegistered.current = true;
    if (typeof window !== 'undefined') {
      window.addEventListener('keydown', (e) => {
        if (!editingRef.current) return;
        if (e.key === 'Escape') discardRef.current();
        if (e.key === 'Enter') { e.preventDefault(); commitRef.current(); }
      });
    }
  }

  // ── Fase 1.8/1.9: Handlers de vértices ───────────────────────────────────

  /**
   * Normaliza polygonPoints para que siempre minX=0, minY=0
   * y absorbe el desplazamiento en el.x / el.y.
   * Fase 1.12: también traslada handleIn/handleOut restando el mismo offset
   * para que las curvas permanezcan alineadas con sus anclas tras normalizar.
   * Trabaja con el nuevo formato [{x,y,handleIn,handleOut,symmetric},...].
   */
  function normalizePoints(pts, elX, elY) {
    const migrated = migratePolygonPoints(pts);
    const xs   = migrated.map((p) => p.x);
    const ys   = migrated.map((p) => p.y);
    const minX = Math.min(...xs);
    const minY = Math.min(...ys);
    const normalized = migrated.map((p) => ({
      ...p,
      x: p.x - minX,
      y: p.y - minY,
      handleIn:  p.handleIn  ? { x: p.handleIn.x  - minX, y: p.handleIn.y  - minY } : null,
      handleOut: p.handleOut ? { x: p.handleOut.x - minX, y: p.handleOut.y - minY } : null,
    }));
    return {
      polygonPoints: normalized,
      x: elX + minX,
      y: elY + minY,
      width:  Math.max(80, Math.max(...normalized.map((p) => p.x))),
      height: Math.max(60, Math.max(...normalized.map((p) => p.y))),
    };
  }

  // Ref que siempre contiene el valor más reciente de vertexPreview.
  // Permite leer el preview actual en dragEnd sin depender de closures stale
  // ni meter efectos secundarios dentro de updaters de setState.
  const vertexPreviewRef = useRef(null);

  const handleVertexDrag = useCallback((vertexIdx, absX, absY) => {
    const el = elements.find((e) => e.id === editingPolygonId);
    if (!el) return;
    // abs→local: inversa de la rotación del Group
    const rad = (el.rotation ?? 0) * Math.PI / 180;
    const cosR = Math.cos(rad), sinR = Math.sin(rad);
    const toLocal = (ax, ay) => {
      const dx = ax - el.x, dy = ay - el.y;
      return { x: dx * cosR + dy * sinR, y: -dx * sinR + dy * cosR };
    };
    const local = toLocal(absX, absY);
    setVertexPreview((prev) => {
      const pts = migratePolygonPoints(prev ?? el.polygonPoints);
      const next = pts.map((p, i) =>
        i === vertexIdx
          ? { ...p, x: local.x, y: local.y }
          : { ...p },
      );
      vertexPreviewRef.current = next;
      return next;
    });
  }, [elements, editingPolygonId]);

  const handleVertexDragEnd = useCallback((vertexIdx, absX, absY) => {
    const el = elements.find((e) => e.id === editingPolygonId);
    if (!el) return;
    const rad = (el.rotation ?? 0) * Math.PI / 180;
    const cosR = Math.cos(rad), sinR = Math.sin(rad);
    const toLocal = (ax, ay) => {
      const dx = ax - el.x, dy = ay - el.y;
      return { x: dx * cosR + dy * sinR, y: -dx * sinR + dy * cosR };
    };
    const local = toLocal(absX, absY);
    // Calcular el estado final con la posición del dragEnd
    const pts = migratePolygonPoints(vertexPreviewRef.current ?? el.polygonPoints);
    const raw = pts.map((p, i) =>
      i === vertexIdx
        ? { ...p, x: local.x, y: local.y }
        : { ...p },
    );
    const patch = normalizePoints(raw, el.x, el.y);
    // setState y onChange en el cuerpo del callback, nunca dentro de un updater
    setVertexPreview(patch.polygonPoints);
    vertexPreviewRef.current = patch.polygonPoints;
    onChange({ ...el, ...patch });
  }, [elements, editingPolygonId, onChange]);

  const handleMidpointClick = useCallback((insertAfterIdx) => {
    const el = elements.find((e) => e.id === editingPolygonId);
    if (!el) return;
    setVertexPreview((prev) => {
      const pts  = migratePolygonPoints(prev ?? el.polygonPoints);
      const a    = pts[insertAfterIdx];
      const b    = pts[(insertAfterIdx + 1) % pts.length];
      const newPt = {
        x: (a.x + b.x) / 2,
        y: (a.y + b.y) / 2,
        handleIn:  null,
        handleOut: null,
        symmetric: true,
      };
      const next = [
        ...pts.slice(0, insertAfterIdx + 1),
        newPt,
        ...pts.slice(insertAfterIdx + 1),
      ];
      vertexPreviewRef.current = next;
      return next;
    });
  }, [elements, editingPolygonId]);

  const handleVertexRightClick = useCallback((vertexIdx) => {
    setVertexPreview((prev) => {
      if (!prev || prev.length <= 3) return prev;
      const next = prev.filter((_, i) => i !== vertexIdx);
      vertexPreviewRef.current = next;
      return next;
    });
  }, []);

  const handleVertexGuideChange = useCallback(({ vertical, horizontal }) => {
    setActiveVertexGuides({ vertical: vertical ?? null, horizontal: horizontal ?? null });
  }, []);

  // ── Fase 1.12: handlers de handles Bézier ────────────────────────────────

  /**
   * Callback del Alt+drag sobre punto medio — crea/actualiza la curva del segmento i.
   * handleOutAbs y handleInAbs ya vienen en coordenadas absolutas del canvas.
   */
  const handleSegmentCurve = useCallback((segmentIdx, handleOutAbs, handleInAbs) => {
    const el = elements.find((e) => e.id === editingPolygonId);
    if (!el) return;
    const rad = (el.rotation ?? 0) * Math.PI / 180;
    const cosR = Math.cos(rad), sinR = Math.sin(rad);
    const toLocal = (ax, ay) => {
      const dx = ax - el.x, dy = ay - el.y;
      return { x: dx * cosR + dy * sinR, y: -dx * sinR + dy * cosR };
    };
    const pts     = migratePolygonPoints(vertexPreviewRef.current ?? el.polygonPoints);
    const nextIdx = (segmentIdx + 1) % pts.length;
    const locOut  = toLocal(handleOutAbs.x, handleOutAbs.y);
    const locIn   = toLocal(handleInAbs.x,  handleInAbs.y);
    const updated = pts.map((p, i) => {
      if (i === segmentIdx) return { ...p, handleOut: locOut };
      if (i === nextIdx)    return { ...p, handleIn:  locIn  };
      return p;
    });
    vertexPreviewRef.current = updated;
    setVertexPreview(updated);
    onChange({ ...el, polygonPoints: updated });
  }, [elements, editingPolygonId, onChange]);

  /**
   * Preview en tiempo real del drag de un handle de control.
   * side: 'in' | 'out'
   * Solo actualiza el preview visual — no persiste (eso lo hace handleHandleDragEnd).
   */
  const handleHandleDrag = useCallback((vertexIdx, side, absX, absY) => {
    const el = elements.find((e) => e.id === editingPolygonId);
    if (!el) return;
    const rad = (el.rotation ?? 0) * Math.PI / 180;
    const cosR = Math.cos(rad), sinR = Math.sin(rad);
    const toLocal = (ax, ay) => {
      const dx = ax - el.x, dy = ay - el.y;
      return { x: dx * cosR + dy * sinR, y: -dx * sinR + dy * cosR };
    };
    const loc = toLocal(absX, absY);
    setVertexPreview((prev) => {
      const pts = migratePolygonPoints(prev ?? el.polygonPoints);
      const next = pts.map((p, i) => {
        if (i !== vertexIdx) return p;
        if (side === 'out') {
          const updated = { ...p, handleOut: { x: loc.x, y: loc.y } };
          if (p.symmetric && p.handleIn !== null) {
            updated.handleIn = { x: p.x - (loc.x - p.x), y: p.y - (loc.y - p.y) };
          }
          return updated;
        } else {
          const updated = { ...p, handleIn: { x: loc.x, y: loc.y } };
          if (p.symmetric && p.handleOut !== null) {
            updated.handleOut = { x: p.x - (loc.x - p.x), y: p.y - (loc.y - p.y) };
          }
          return updated;
        }
      });
      vertexPreviewRef.current = next;
      return next;
    });
  }, [elements, editingPolygonId]);

  /**
   * Commit del drag de un handle — normaliza y persiste.
   * onChange se llama en el cuerpo del callback, NUNCA dentro de un updater.
   */
  const handleHandleDragEnd = useCallback((vertexIdx, side, absX, absY) => {
    const el = elements.find((e) => e.id === editingPolygonId);
    if (!el) return;
    const rad = (el.rotation ?? 0) * Math.PI / 180;
    const cosR = Math.cos(rad), sinR = Math.sin(rad);
    const toLocal = (ax, ay) => {
      const dx = ax - el.x, dy = ay - el.y;
      return { x: dx * cosR + dy * sinR, y: -dx * sinR + dy * cosR };
    };
    const loc = toLocal(absX, absY);
    const pts = migratePolygonPoints(vertexPreviewRef.current ?? el.polygonPoints);
    const raw = pts.map((p, i) => {
      if (i !== vertexIdx) return p;
      if (side === 'out') {
        const updated = { ...p, handleOut: { x: loc.x, y: loc.y } };
        if (p.symmetric && p.handleIn !== null) {
          updated.handleIn = { x: p.x - (loc.x - p.x), y: p.y - (loc.y - p.y) };
        }
        return updated;
      } else {
        const updated = { ...p, handleIn: { x: loc.x, y: loc.y } };
        if (p.symmetric && p.handleOut !== null) {
          updated.handleOut = { x: p.x - (loc.x - p.x), y: p.y - (loc.y - p.y) };
        }
        return updated;
      }
    });
    const patch = normalizePoints(raw, el.x, el.y);
    setVertexPreview(patch.polygonPoints);
    vertexPreviewRef.current = patch.polygonPoints;
    onChange({ ...el, ...patch });
  }, [elements, editingPolygonId, onChange]);

  /**
   * Click derecho sobre un handle — alterna symmetric para ese vértice.
   */
  const handleHandleRightClick = useCallback((vertexIdx) => {
    const el = elements.find((e) => e.id === editingPolygonId);
    if (!el) return;
    const pts = migratePolygonPoints(vertexPreviewRef.current ?? el.polygonPoints);
    const updated = pts.map((p, i) =>
      i === vertexIdx ? { ...p, symmetric: !p.symmetric } : p,
    );
    vertexPreviewRef.current = updated;
    setVertexPreview(updated);
    onChange({ ...el, polygonPoints: updated });
  }, [elements, editingPolygonId, onChange]);

  /**
   * Shift+click derecho sobre un handle — elimina la curva del segmento adyacente.
   */
  const handleClearCurve = useCallback((vertexIdx, side) => {
    const el = elements.find((e) => e.id === editingPolygonId);
    if (!el) return;
    const pts = migratePolygonPoints(vertexPreviewRef.current ?? el.polygonPoints);
    const n   = pts.length;
    let updated;
    if (side === 'out') {
      const nextIdx = (vertexIdx + 1) % n;
      updated = pts.map((p, i) => {
        if (i === vertexIdx) return { ...p, handleOut: null };
        if (i === nextIdx)   return { ...p, handleIn:  null };
        return p;
      });
    } else {
      const prevIdx = (vertexIdx - 1 + n) % n;
      updated = pts.map((p, i) => {
        if (i === vertexIdx) return { ...p, handleIn:  null };
        if (i === prevIdx)   return { ...p, handleOut: null };
        return p;
      });
    }
    vertexPreviewRef.current = updated;
    setVertexPreview(updated);
    onChange({ ...el, polygonPoints: updated });
  }, [elements, editingPolygonId, onChange]);

  // ── Stage mouse handlers ──────────────────────────────────────────────────
  const handleStageMouseDown = (e) => {
    const isRight = e.evt.button === 2;
    const isLeft  = e.evt.button === 0;
    const isOver  = e.target === e.target.getStage();
    const ctrl    = e.evt.ctrlKey || e.evt.metaKey;

    if (isRight) {
      e.evt.preventDefault();
      panState.current = { active: true, startPointer: { x: e.evt.clientX, y: e.evt.clientY }, startStagePos: { ...stagePos } };
      if (stageRef.current) stageRef.current.container().style.cursor = 'grabbing';
      return;
    }
    if (!isLeft) return;

    if (editingPolygonId) {
      const name = e.target?.name?.() ?? e.target?.attrs?.name ?? '';
      // Clicks sobre handles de vértices, midpoints, handles de curva o la propia forma: no salir
      if (
        name.startsWith('vertex-handle-') ||
        name.startsWith('midpoint-handle-') ||
        name.startsWith('bezier-handle-') ||
        name.startsWith('polygon-shape-')
      ) return;

      // Opción 1: point-in-polygon real via Konva intersects().
      // Si el click cayó dentro del contorno visual real de la sección en edición
      // (incluyendo curvas Bézier, gracias al hitFunc ya corregido), no salimos.
      // Esto cubre el caso de clicks en la "zona vacía" que quedó entre el bbox
      // original y el nuevo contorno curvo.
      const shapeNode = stageRef.current?.findOne(
        `[name=polygon-shape-${editingPolygonId}]`
      );
      if (shapeNode) {
        const stage   = stageRef.current;
        const pointer = stage.getPointerPosition();
        // getPointerPosition devuelve coords de pantalla; hay que convertir
        // a coords del canvas (compensar zoom y pan del Stage).
        const transform = stage.getAbsoluteTransform().copy().invert();
        const canvasPos = transform.point(pointer);
        // intersects() usa el hitFunc del nodo — el mismo que ya dibuja la curva real.
        if (shapeNode.intersects(canvasPos)) return;
      }

      // Click realmente fuera del contorno → commit y salir
      commitVertexEdit();
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
    if (panState.current.active) {
      panState.current.active = false;
      if (stageRef.current) stageRef.current.container().style.cursor = 'default';
      return;
    }
    if (isRubberBand.current && selBox) {
      if (selBox.width > 5 || selBox.height > 5) {
        const sel = elements
          .filter((el) => {
            const aabb = getElementAABB(el);
            return rectsIntersect(selBox, { x: aabb.minX, y: aabb.minY, width: aabb.maxX - aabb.minX, height: aabb.maxY - aabb.minY });
          })
          .map((el) => el.id);
        onSelect(sel.length > 0 ? sel : []);
      }
    }
    setSelBox(null); isRubberBand.current = false;
  };

  // ── Multi-drag (Fase 1.6) ─────────────────────────────────────────────────
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
    const outsiders = elements.filter((el) => !selectedIds.includes(el.id));
    if (outsiders.length > 0) {
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

  // ── Drag individual con smart guides (Fase 1.6) ───────────────────────────
  const handleElementDragMove = useCallback((elementId, tentativePos) => {
    const el = elements.find((e) => e.id === elementId);
    if (!el) return;
    const tentative = { ...el, x: tentativePos.x, y: tentativePos.y };
    const outsiders = elements.filter((e) => e.id !== elementId);
    const guides    = findSnapGuides(tentative, outsiders, SNAP_THRESHOLD);
    setActiveGuides({
      vertical:   guides.vertical   ? guides.vertical.position   : null,
      horizontal: guides.horizontal ? guides.horizontal.position : null,
    });
    return { dx: guides.vertical?.delta ?? 0, dy: guides.horizontal?.delta ?? 0 };
  }, [elements]);

  const handleElementDragEnd = useCallback((updated) => {
    setActiveGuides({ vertical: null, horizontal: null });
    setActiveVertexGuides({ vertical: null, horizontal: null });
    onChange(updated);
  }, [onChange]);

  // ── Fase 1.11: handleElementChange simplificado ───────────────────────────
  // canvasSize ya es un valor derivado en el padre — no hay que expandir aquí.
  const handleElementChange = useCallback((updated) => {
    onChange(updated);
  }, [onChange]);

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
        {/* ── Layer de grilla ──────────────────────────────────────────── */}
        <Layer listening={false}>
          <Rect x={0} y={0} width={canvasSize.width} height={canvasSize.height} fill="var(--color-surface)" cornerRadius={8} />
          {gridLines.map((l) => (
            <Line key={l.key} points={l.points} stroke="rgba(255,255,255,0.04)" strokeWidth={1} listening={false} />
          ))}
          <Rect x={0} y={0} width={canvasSize.width} height={canvasSize.height} stroke="rgba(124,58,237,0.3)" strokeWidth={1} fill="transparent" cornerRadius={8} listening={false} />
        </Layer>

        {/* ── Layer de elementos ───────────────────────────────────────── */}
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
              element:   displayEl,
              isSelected,
              onSelect:  () => onSelect([el.id]),
              onChange:  handleElementChange,
              onDragMove:       handleElementDragMove,
              onDragEnd:        handleElementDragEnd,
              onGroupDragStart: isMulti ? handleGroupDragStart : undefined,
              onGroupDragMove:  isMulti ? handleGroupDragMove  : undefined,
              onGroupDragEnd:   isMulti ? handleGroupDragEnd   : undefined,
              canvasSize,
              canvasSizeRef,
            };

            if (el.type === 'section') {
              return (
                <SectionElement
                  key={el.id}
                  {...sharedProps}
                  isEditingVertices={isEditingThis}
                  previewPoints={isEditingThis ? vertexPreview : null}
                  onEnterVertexEdit={() => onStartVertexEdit?.(el.id)}
                />
              );
            }
            return <InfraElement key={el.id} {...sharedProps} />;
          })}

          {/* Rubber-band */}
          {selBox && (selBox.width > 2 || selBox.height > 2) && (
            <Rect x={selBox.x} y={selBox.y} width={selBox.width} height={selBox.height}
              fill="rgba(124,58,237,0.08)" stroke="rgba(124,58,237,0.7)"
              strokeWidth={1} dash={[4, 3]} listening={false} />
          )}
        </Layer>

        {/* ── Layer de smart guides globales (Fase 1.6) ────────────────── */}
        <Layer listening={false}>
          {activeGuides.vertical !== null && (
            <Line points={[activeGuides.vertical, -GUIDE_EXTENT, activeGuides.vertical, GUIDE_EXTENT]}
              stroke={GUIDE_COLOR} strokeWidth={1} dash={[4, 4]} listening={false} />
          )}
          {activeGuides.horizontal !== null && (
            <Line points={[-GUIDE_EXTENT, activeGuides.horizontal, GUIDE_EXTENT, activeGuides.horizontal]}
              stroke={GUIDE_COLOR} strokeWidth={1} dash={[4, 4]} listening={false} />
          )}
          {/* Guías de vértices — líneas cortas (Fase 1.7/1.8) */}
          {activeVertexGuides.vertical !== null && (() => {
            const g = activeVertexGuides.vertical;
            return <Line points={[g.x1, g.y1, g.x2, g.y2]} stroke={GUIDE_COLOR_VERTEX} strokeWidth={1.5} dash={[3, 3]} listening={false} />;
          })()}
          {activeVertexGuides.horizontal !== null && (() => {
            const g = activeVertexGuides.horizontal;
            return <Line points={[g.x1, g.y1, g.x2, g.y2]} stroke={GUIDE_COLOR_VERTEX} strokeWidth={1.5} dash={[3, 3]} listening={false} />;
          })()}
        </Layer>

        {/* ── Layer de edición de vértices (Fase 1.8) — separada del Group ── */}
        <Layer>
          {editingPolygonId !== null && (() => {
            const el = elements.find((e) => e.id === editingPolygonId);
            if (!el) return null;
            return (
              <VertexEditorOverlay
                element={{ ...el, polygonPoints: vertexPreview ?? el.polygonPoints }}
                previewPoints={vertexPreview}
                onVertexDrag={handleVertexDrag}
                onVertexDragEnd={handleVertexDragEnd}
                onMidpointClick={handleMidpointClick}
                onSegmentCurve={handleSegmentCurve}
                onVertexRightClick={handleVertexRightClick}
                onHandleDrag={handleHandleDrag}
                onHandleDragEnd={handleHandleDragEnd}
                onHandleRightClick={handleHandleRightClick}
                onClearCurve={handleClearCurve}
                onVertexGuideChange={handleVertexGuideChange}
                otherElements={elements.filter((e) => e.id !== editingPolygonId)}
              />
            );
          })()}
        </Layer>
      </Stage>

      {/* ── Controles de zoom — esquina superior izquierda del canvas ──── */}
      <div className="absolute top-4 left-4 z-20 flex items-center gap-1 bg-surface border border-surfaceAlt rounded-lg px-2 py-1.5 shadow-xl shadow-black/40 select-none">
        <button
          onClick={handleZoomOut}
          className="w-7 h-7 flex items-center justify-center rounded text-textSecondary hover:text-textPrimary hover:bg-surfaceAlt transition-colors text-base font-bold"
          title="Reducir zoom (−)"
        >−</button>
        <span className="text-xs font-mono text-textPrimary w-12 text-center tabular-nums">
          {Math.round(zoom * 100)}%
        </span>
        <button
          onClick={handleZoomIn}
          className="w-7 h-7 flex items-center justify-center rounded text-textSecondary hover:text-textPrimary hover:bg-surfaceAlt transition-colors text-base font-bold"
          title="Aumentar zoom (+)"
        >+</button>
        <div className="w-px h-4 bg-surfaceAlt mx-1" />
        <button
          onClick={handleFit}
          className="w-7 h-7 flex items-center justify-center rounded text-textSecondary hover:text-textPrimary hover:bg-surfaceAlt transition-colors text-sm"
          title="Ajustar todo al viewport (⊡)"
        >⊡</button>
      </div>

      {/* ── Handle de resize del canvas (Fase 1.10) ─────────────────────── */}
      {onCanvasSizeChange && (() => {
        // Calcular posición en pantalla de la esquina inferior derecha del canvas
        const handleSize = 18;
        const cornerCanvasX = canvasSize.width;
        const cornerCanvasY = canvasSize.height;
        // Convertir a coordenadas del Stage (pantalla relativa al contenedor)
        const cornerScreenX = cornerCanvasX * zoom + stagePos.x - handleSize;
        const cornerScreenY = cornerCanvasY * zoom + stagePos.y - handleSize;
        return (
          <div
            onMouseDown={startCanvasResize}
            title="Arrastra para redimensionar el canvas"
            style={{
              position:   'absolute',
              left:       `${cornerScreenX}px`,
              top:        `${cornerScreenY}px`,
              width:      `${handleSize}px`,
              height:     `${handleSize}px`,
              cursor:     'se-resize',
              zIndex:     25,
              background: 'var(--color-surface)',
              border:     '1px solid var(--color-surfaceAlt)',
              borderRadius: '3px 0 0 0',
              display:    'flex',
              alignItems: 'center',
              justifyContent: 'center',
              userSelect: 'none',
            }}
          >
            <svg width="10" height="10" viewBox="0 0 10 10" style={{ color: 'var(--color-textMuted)' }}>
              <line x1="2" y1="10" x2="10" y2="2" stroke="currentColor" strokeWidth="1.5" opacity="0.7"/>
              <line x1="5" y1="10" x2="10" y2="5" stroke="currentColor" strokeWidth="1.5" opacity="0.7"/>
            </svg>
          </div>
        );
      })()}

      {/* ── Botón flotante "Guardar forma" + hints ───────────────────────── */}
      {editingPolygonId && (
        <button
          className="absolute top-3 left-1/2 -translate-x-1/2 z-10 bg-accent text-white text-xs font-bold px-4 py-1.5 rounded-full shadow-lg hover:bg-accent/90 transition-colors"
          onClick={commitVertexEdit}
        >
          ✓ Guardar forma
        </button>
      )}

      {!editingPolygonId && (
        <div className="absolute bottom-4 left-4 text-[10px] text-textMuted pointer-events-none select-none space-y-0.5">
          <div>🖱 Rueda → zoom · Derecho+drag → pan</div>
          <div>⌨ Ctrl+drag fondo → selección múltiple</div>
        </div>
      )}
      {editingPolygonId && (
        <div className="absolute bottom-4 left-4 text-[10px] text-accent pointer-events-none select-none space-y-0.5">
          <div>✏ Click fuera o <kbd className="bg-surfaceAlt px-1 rounded text-textMuted">Guardar forma</kbd> → guardar</div>
          <div><kbd className="bg-surfaceAlt px-1 rounded text-textMuted">Esc</kbd> → descartar</div>
          <div className="mt-1 pt-1 border-t border-accent/20 space-y-0.5">
            <div>⌥ Alt+arrastrar punto medio → convierte segmento en curva</div>
            <div>⬤ Arrastrar handle (círculo) → ajusta la curvatura</div>
            <div className="flex items-center gap-1">
              <span style={{ display:'inline-block', width:8, height:8, borderRadius:'50%', border:'1.5px solid #A78BFA', marginRight:2 }} />
              hueco morado = simétrico (ambos lados se mueven juntos)
            </div>
            <div className="flex items-center gap-1">
              <span style={{ display:'inline-block', width:8, height:8, borderRadius:'50%', border:'1.5px solid #F59E0B', background:'rgba(245,158,11,0.15)', marginRight:2 }} />
              relleno ámbar = independiente (cada lado se mueve solo)
            </div>
            <div>↔ Click derecho en handle → alterna simétrico / independiente</div>
            <div>✕ Shift+click derecho en handle → elimina la curva (vuelve a recto)</div>
            <div>✕ Click derecho en vértice ancla → elimina vértice</div>
            <div>＋ Click en punto medio → inserta vértice</div>
          </div>
        </div>
      )}
    </div>
  );
}
