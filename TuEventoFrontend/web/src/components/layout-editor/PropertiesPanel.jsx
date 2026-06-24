import { computeMaxSeats, normalizeSeatLayout, polyBoundingBox, migratePolygonPoints, getPresetPolygonPoints } from './layoutEditorUtils';

const SECTION_COLORS = [
  { label: 'Morado', value: '#7C3AED' },
  { label: 'Verde',  value: '#16A34A' },
  { label: 'Naranja',value: '#EA580C' },
  { label: 'Azul',   value: '#2563EB' },
  { label: 'Rosado', value: '#DB2777' },
];

const INFRA_COLORS = [
  { label: 'Oscuro',  value: '#1E293B' },
  { label: 'Negro',   value: '#0F172A' },
  { label: 'Azul',    value: '#1D4ED8' },
  { label: 'Verde',   value: '#15803D' },
  { label: 'Rojo',    value: '#B91C1C' },
  { label: 'Café',    value: '#92400E' },
  { label: 'Gris',    value: '#475569' },
];

// ── Formas sugeridas: íconos SVG inline ──────────────────────────────────────
const PRESET_ICONS = {
  rect: (stroke) => (
    <polyline points="4,4 36,4 36,36 4,36 4,4" stroke={stroke} strokeWidth="2" fill="none" />
  ),
  circle: (stroke) => (
    <ellipse cx="20" cy="20" rx="16" ry="16" stroke={stroke} strokeWidth="2" fill="none" />
  ),
  semicircle: (stroke) => (
    <path d="M4,36 L4,20 Q4,4 20,4 Q36,4 36,20 L36,36 Z" stroke={stroke} strokeWidth="2" fill="none" />
  ),
  trapezoid: (stroke) => (
    <polygon points="8,6 32,6 36,34 4,34" stroke={stroke} strokeWidth="2" fill="none" />
  ),
  triangle: (stroke) => (
    <polygon points="20,4 36,36 4,36" stroke={stroke} strokeWidth="2" fill="none" />
  ),
  hexagon: (stroke) => (
    <polygon points="20,4 36,14 36,26 20,36 4,26 4,14" stroke={stroke} strokeWidth="2" fill="none" />
  ),
  lshape: (stroke) => (
    <polygon points="4,4 20,4 20,20 36,20 36,36 4,36" stroke={stroke} strokeWidth="2" fill="none" />
  ),
};

const PRESET_LABELS = {
  rect:       'Rect.',
  circle:     'Círculo',
  semicircle: 'Semicírc.',
  trapezoid:  'Trapecio',
  triangle:   'Triáng.',
  hexagon:    'Hexág.',
  lshape:     'L',
};

const PRESET_ORDER = ['rect', 'circle', 'semicircle', 'trapezoid', 'triangle', 'hexagon', 'lshape'];

export default function PropertiesPanel({
  element,
  onChange,
  onDelete,
  isEditingVertices,
  onStartVertexEdit,
  onEndVertexEdit,
  canvasSize,
  onCanvasSizeChange,
  onApplyPreset,
}) {
  const inputClass =
    'w-full bg-background border border-surfaceAlt rounded px-2 py-1.5 text-xs text-textPrimary ' +
    'focus:outline-none focus:border-accent transition-colors';
  const labelClass = 'text-[10px] font-semibold text-textMuted uppercase tracking-wider mb-1 block';

  if (!element) {
    const CANVAS_PRESETS = [
      { label: 'S',  w: 800,  h: 600  },
      { label: 'M',  w: 1200, h: 800  },
      { label: 'L',  w: 1600, h: 1000 },
      { label: 'XL', w: 2000, h: 1400 },
    ];
    return (
      <aside className="w-[240px] flex-shrink-0 bg-surface border-l border-surfaceAlt flex flex-col overflow-hidden">
        <div className="px-3 py-3 border-b border-surfaceAlt">
          <h2 className="text-xs font-bold text-textSecondary uppercase tracking-wider">Canvas</h2>
        </div>
        <div className="flex-1 overflow-y-auto p-3 space-y-4">
          {onCanvasSizeChange && canvasSize ? (
            <>
              <div>
                <label className={labelClass}>Tamaño del canvas</label>
                <div className="flex items-center gap-2">
                  <input
                    type="number"
                    className={inputClass}
                    style={{ width: '72px' }}
                    value={Math.round(canvasSize.width)}
                    min={800} step={100}
                    onChange={(e) => onCanvasSizeChange({
                      width:  Math.max(800, parseInt(e.target.value) || 800),
                      height: canvasSize.height,
                    })}
                  />
                  <span className="text-[11px] text-textMuted shrink-0">×</span>
                  <input
                    type="number"
                    className={inputClass}
                    style={{ width: '72px' }}
                    value={Math.round(canvasSize.height)}
                    min={600} step={100}
                    onChange={(e) => onCanvasSizeChange({
                      width:  canvasSize.width,
                      height: Math.max(600, parseInt(e.target.value) || 600),
                    })}
                  />
                  <span className="text-[11px] text-textMuted shrink-0">px</span>
                </div>
                <p className="text-[10px] text-textMuted mt-1">Ancho × Alto del área de diseño</p>
              </div>
              <div>
                <label className={labelClass}>Tamaños rápidos</label>
                <div className="flex flex-wrap gap-1.5">
                  {CANVAS_PRESETS.map((p) => (
                    <button
                      key={p.label}
                      onClick={() => onCanvasSizeChange({ width: p.w, height: p.h })}
                      className="text-[10px] px-2 py-1 rounded border border-surfaceAlt
                                 text-textSecondary hover:text-textPrimary hover:bg-surfaceAlt
                                 transition-colors"
                    >
                      {p.label} <span className="text-textMuted">{p.w}×{p.h}</span>
                    </button>
                  ))}
                </div>
              </div>
            </>
          ) : (
            <p className="text-textMuted text-xs text-center leading-relaxed">
              Selecciona o arrastra un elemento al canvas para editar sus propiedades.
            </p>
          )}
        </div>
      </aside>
    );
  }

  // ── Variables para el render del elemento seleccionado ──────────────────
  const update    = (patch) => onChange({ ...element, ...patch });
  const colors    = element.type === 'section' ? SECTION_COLORS : INFRA_COLORS;
  const shapeMode = element.type === 'section' ? (element.shapeMode ?? 'rect') : null;

  // Fase 1.5: normalizar seatLayout (migración automática de rows/cols)
  const sl = element.type === 'section' ? normalizeSeatLayout(element.seatLayout) : null;

  // Capacidad máxima calculada con los valores actuales
  const maxSeats = sl
    ? computeMaxSeats(element, sl.seatRadius, sl.gap)
    : 0;

  const targetSeats = sl?.targetSeats ?? 0;
  const pct = maxSeats > 0 ? Math.min(100, Math.round((targetSeats / maxSeats) * 100)) : 0;

  // Estado del badge
  let badgeColor = 'bg-success/20 text-success border-success/30';
  let badgeText  = 'Caben';
  let hintText   = `Máximo estimado: ${maxSeats} sillas`;
  if (maxSeats > 0) {
    if (targetSeats > maxSeats) {
      badgeColor = 'bg-error/20 text-error border-error/30';
      badgeText  = 'No caben';
      hintText   = `Excede por ${targetSeats - maxSeats}. Agranda la forma o reduce la cantidad`;
    } else if (targetSeats > Math.floor(maxSeats * 0.8)) {
      badgeColor = 'bg-yellow-500/20 text-yellow-400 border-yellow-400/30';
      badgeText  = 'Ajustado';
      hintText   = `Quedan solo ${maxSeats - targetSeats} de margen`;
    }
  }

  // Color de la barra de progreso
  const barColor = targetSeats > maxSeats
    ? 'bg-error'
    : targetSeats > Math.floor(maxSeats * 0.8)
    ? 'bg-yellow-400'
    : 'bg-success';

  const updateSeatLayout = (patch) => {
    const updated = { ...sl, ...patch };
    // Solo actualizar la grilla en el canvas si targetSeats <= maxSeats (nuevo)
    const newMax = computeMaxSeats(element, updated.seatRadius, updated.gap);
    if (updated.targetSeats <= newMax) {
      update({ seatLayout: updated });
    } else {
      // Solo actualizar el seatLayout sin cambiar la grilla visual
      update({ seatLayout: updated });
    }
  };

  // ── Conversor a polígono ──────────────────────────────────────────────────
  const handleConvertToPolygon = () => {
    update({
      shapeMode:     'polygon',
      polygonPoints: [
        { x: 0,             y: 0,              handleIn: null, handleOut: null, symmetric: true },
        { x: element.width, y: 0,              handleIn: null, handleOut: null, symmetric: true },
        { x: element.width, y: element.height, handleIn: null, handleOut: null, symmetric: true },
        { x: 0,             y: element.height, handleIn: null, handleOut: null, symmetric: true },
      ],
    });
  };

  const handleRevertToRect = () => {
    if (!window.confirm(
      '¿Volver a rectángulo? Si el polígono no era ya un rectángulo, la forma se simplificará.'
    )) return;
    const migrated = migratePolygonPoints(element.polygonPoints || []);
    const bb = polyBoundingBox(migrated);
    update({
      shapeMode:     'rect',
      polygonPoints: null,
      width:  Math.max(60, Math.round(bb.width  || element.width)),
      height: Math.max(40, Math.round(bb.height || element.height)),
    });
    onEndVertexEdit?.();
  };

  return (
    <aside className="w-[240px] flex-shrink-0 bg-surface border-l border-surfaceAlt flex flex-col overflow-hidden">
      <div className="px-3 py-3 border-b border-surfaceAlt flex items-center justify-between">
        <h2 className="text-xs font-bold text-textSecondary uppercase tracking-wider">
          Propiedades
        </h2>
        <div className="flex items-center gap-1.5">
          {element.type === 'section' && shapeMode === 'polygon' && (
            <span className="text-[9px] font-bold px-1.5 py-0.5 rounded bg-accent/20 text-accent border border-accent/30">
              POLY
            </span>
          )}
          {element.type === 'section' && (
            <span
              className="text-[10px] font-bold px-2 py-0.5 rounded-full text-white"
              style={{ background: element.color }}
            >
              {element.sectionType}
            </span>
          )}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-3 space-y-4">

        {/* Controles de forma — solo secciones */}
        {element.type === 'section' && (
          <div>
            <label className={labelClass}>Forma</label>
            {shapeMode === 'rect' ? (
              <button
                onClick={handleConvertToPolygon}
                className="w-full py-1.5 rounded-lg text-xs font-semibold border border-accent/40
                           text-accent hover:bg-accent/10 transition-colors"
              >
                ✦ Convertir a forma libre
              </button>
            ) : isEditingVertices ? (
              <div className="space-y-1.5">
                <p className="text-[10px] text-accent">
                  Editando vértices — arrastra puntos, click en medios para añadir, click derecho para eliminar
                </p>
                <button
                  onClick={() => onEndVertexEdit?.()}
                  className="w-full py-1.5 rounded-lg text-xs font-semibold bg-accent/15
                             text-accent border border-accent/40 hover:bg-accent/25 transition-colors"
                >
                  ✓ Guardar forma
                </button>

                {/* ── Formas sugeridas ── */}
                {onApplyPreset && (
                  <div className="pt-1">
                    <span className="text-[9px] font-semibold text-textMuted uppercase tracking-wider block mb-1.5">
                      Formas sugeridas
                    </span>
                    <div className="grid grid-cols-4 gap-1">
                      {PRESET_ORDER.map((preset) => (
                        <button
                          key={preset}
                          title={PRESET_LABELS[preset]}
                          onClick={() => {
                            const pts = getPresetPolygonPoints(preset, element.width, element.height);
                            onApplyPreset(pts);
                          }}
                          className="flex flex-col items-center justify-center gap-0.5 rounded-md
                                     bg-surfaceAlt hover:bg-accent/20 border border-transparent
                                     hover:border-accent/40 transition-colors cursor-pointer"
                          style={{ width: '100%', aspectRatio: '1 / 1', padding: '4px' }}
                        >
                          <svg
                            viewBox="0 0 40 40"
                            width="28" height="28"
                            xmlns="http://www.w3.org/2000/svg"
                            style={{ overflow: 'visible' }}
                          >
                            {PRESET_ICONS[preset]?.(element.color ?? '#7C3AED')}
                          </svg>
                          <span className="text-[8px] text-textMuted leading-none truncate w-full text-center">
                            {PRESET_LABELS[preset]}
                          </span>
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <div className="space-y-1.5">
                <button
                  onClick={onStartVertexEdit}
                  className="w-full py-1.5 rounded-lg text-xs font-semibold border border-accent/40
                             text-accent hover:bg-accent/10 transition-colors"
                >
                  ✏ Editar forma
                </button>
                <button
                  onClick={handleRevertToRect}
                  className="w-full py-1.5 rounded-lg text-xs font-semibold border border-surfaceAlt
                             text-textMuted hover:text-textSecondary hover:bg-surfaceAlt transition-colors"
                >
                  ↩ Volver a rectángulo
                </button>
              </div>
            )}
          </div>
        )}

        {/* Etiqueta */}
        <div>
          <label className={labelClass}>Etiqueta</label>
          <input
            type="text" className={inputClass}
            value={element.label}
            disabled={isEditingVertices}
            onChange={(e) => update({ label: e.target.value })}
          />
        </div>

        {/* Posición */}
        <div>
          <label className={labelClass}>Posición</label>
          <div className="grid grid-cols-2 gap-2">
            {[['X', 'x'], ['Y', 'y']].map(([lab, key]) => (
              <div key={key}>
                <span className="text-[9px] text-textMuted mb-0.5 block">{lab}</span>
                <input
                  type="number" className={inputClass}
                  value={Math.round(element[key])}
                  disabled={isEditingVertices}
                  onChange={(e) => update({ [key]: Number(e.target.value) })}
                />
              </div>
            ))}
          </div>
        </div>

        {/* Tamaño (solo rect) */}
        {shapeMode !== 'polygon' && (
          <div>
            <label className={labelClass}>Tamaño</label>
            <div className="grid grid-cols-2 gap-2">
              <div>
                <span className="text-[9px] text-textMuted mb-0.5 block">Ancho</span>
                <input type="number" className={inputClass} min={40}
                  value={Math.round(element.width)}
                  onChange={(e) => update({ width: Math.max(40, Number(e.target.value)) })} />
              </div>
              <div>
                <span className="text-[9px] text-textMuted mb-0.5 block">Alto</span>
                <input type="number" className={inputClass} min={30}
                  value={Math.round(element.height)}
                  onChange={(e) => update({ height: Math.max(30, Number(e.target.value)) })} />
              </div>
            </div>
          </div>
        )}

        {/* Rotación */}
        <div>
          <label className={labelClass}>Rotación ({Math.round(element.rotation ?? 0)}°)</label>
          <input
            type="range" min={0} max={360} step={1}
            value={element.rotation ?? 0}
            disabled={isEditingVertices}
            onChange={(e) => update({ rotation: Number(e.target.value) })}
            className="w-full accent-primary"
          />
        </div>

        {/* Color */}
        <div>
          <label className={labelClass}>Color</label>
          <div className="flex flex-wrap gap-2">
            {colors.map((c) => (
              <button
                key={c.value} title={c.label}
                onClick={() => update({ color: c.value })}
                className="w-7 h-7 rounded-full border-2 transition-transform hover:scale-110"
                style={{ background: c.value, borderColor: element.color === c.value ? '#ffffff' : 'transparent' }}
              />
            ))}
            <input
              type="color" value={element.color}
              onChange={(e) => update({ color: e.target.value })}
              className="w-7 h-7 rounded-full cursor-pointer border-2 border-surfaceAlt"
              title="Color personalizado"
            />
          </div>
        </div>

        {/* ── Fase 1.5: Capacidad de sillas ─────────────────────────────── */}
        {element.type === 'section' && sl && !isEditingVertices && (
          <div className="space-y-3">
            <label className={labelClass}>
              Sillas
              {shapeMode === 'polygon' && (
                <span className="ml-1 text-accent normal-case font-normal">(contorno)</span>
              )}
            </label>

            {/* Input targetSeats + badge */}
            <div>
              <div className="flex items-center gap-2 mb-1.5">
                <input
                  type="number" className={`${inputClass} flex-1`} min={1}
                  value={targetSeats}
                  onChange={(e) => updateSeatLayout({ targetSeats: Math.max(1, Number(e.target.value)) })}
                />
                <span className={`shrink-0 text-[9px] font-bold px-1.5 py-0.5 rounded border ${badgeColor}`}>
                  {badgeText}
                </span>
              </div>

              {/* Barra de progreso */}
              <div className="h-1.5 w-full bg-surfaceAlt rounded-full overflow-hidden mb-1">
                <div
                  className={`h-full rounded-full transition-all duration-200 ${barColor}`}
                  style={{ width: `${pct}%` }}
                />
              </div>

              {/* Hint */}
              <p className={`text-[10px] leading-tight ${targetSeats > maxSeats ? 'text-error' : 'text-textMuted'}`}>
                {hintText}
              </p>
            </div>

            {/* Tamaño del radio + separación */}
            <div className="grid grid-cols-2 gap-2">
              <div>
                <span className="text-[9px] text-textMuted mb-0.5 block">Radio (px)</span>
                <input
                  type="number" className={inputClass} min={4} max={20}
                  value={sl.seatRadius}
                  onChange={(e) => updateSeatLayout({ seatRadius: Math.min(20, Math.max(4, Number(e.target.value))) })}
                />
              </div>
              <div>
                <span className="text-[9px] text-textMuted mb-0.5 block">Separación</span>
                <input
                  type="number" className={inputClass} min={2} max={20}
                  value={sl.gap}
                  onChange={(e) => updateSeatLayout({ gap: Math.min(20, Math.max(2, Number(e.target.value))) })}
                />
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Eliminar */}
      <div className="p-3 border-t border-surfaceAlt">
        <button
          onClick={onDelete}
          disabled={isEditingVertices}
          className="w-full py-2 rounded-lg text-xs font-semibold bg-error/10 text-error
                     border border-error/30 hover:bg-error/20 transition-colors
                     disabled:opacity-40 disabled:cursor-not-allowed"
        >
          Eliminar elemento
        </button>
      </div>
    </aside>
  );
}
