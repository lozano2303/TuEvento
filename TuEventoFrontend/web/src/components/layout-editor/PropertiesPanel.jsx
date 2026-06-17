import { computeSectionSize, polyBoundingBox } from './layoutEditorUtils';

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

export default function PropertiesPanel({
  element,
  onChange,
  onDelete,
  isEditingVertices,   // Fase 1.3
  onStartVertexEdit,   // Fase 1.3
  onEndVertexEdit,     // Fase 1.3
}) {
  if (!element) {
    return (
      <aside className="w-[240px] flex-shrink-0 bg-surface border-l border-surfaceAlt flex items-center justify-center p-4">
        <p className="text-textMuted text-xs text-center leading-relaxed">
          Selecciona o arrastra un elemento al canvas para editar sus propiedades.
        </p>
      </aside>
    );
  }

  const update  = (patch) => onChange({ ...element, ...patch });
  const colors  = element.type === 'section' ? SECTION_COLORS : INFRA_COLORS;
  const shapeMode = element.type === 'section' ? (element.shapeMode ?? 'rect') : null;

  const inputClass =
    'w-full bg-background border border-surfaceAlt rounded px-2 py-1.5 text-xs text-textPrimary ' +
    'focus:outline-none focus:border-accent transition-colors';
  const labelClass = 'text-[10px] font-semibold text-textMuted uppercase tracking-wider mb-1 block';

  // ── Fase 1.3: convertir a polígono ────────────────────────────────────────
  const handleConvertToPolygon = () => {
    update({
      shapeMode:     'polygon',
      polygonPoints: [
        [0,             0],
        [element.width, 0],
        [element.width, element.height],
        [0,             element.height],
      ],
    });
  };

  // ── Fase 1.3: volver a rectángulo ─────────────────────────────────────────
  const handleRevertToRect = () => {
    if (!window.confirm(
      '¿Volver a rectángulo? Si el polígono no era ya un rectángulo, la forma se simplificará.'
    )) return;
    const bb = polyBoundingBox(element.polygonPoints || []);
    update({
      shapeMode:     'rect',
      polygonPoints: null,
      width:  Math.max(60, Math.round(bb.width  || element.width)),
      height: Math.max(40, Math.round(bb.height || element.height)),
    });
    // Si estaba en modo edición, salir también
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

        {/* ── Fase 1.3: controles de forma ─────────────────────────────── */}
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
                  ✓ Listo (o Esc)
                </button>
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

        {/* Label */}
        <div>
          <label className={labelClass}>Etiqueta</label>
          <input
            type="text"
            className={inputClass}
            value={element.label}
            onChange={(e) => update({ label: e.target.value })}
            disabled={isEditingVertices}
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
                  type="number"
                  className={inputClass}
                  value={Math.round(element[key])}
                  disabled={isEditingVertices}
                  onChange={(e) => update({ [key]: Number(e.target.value) })}
                />
              </div>
            ))}
          </div>
        </div>

        {/* Tamaño — solo editable en modo rect o cuando no editamos vértices */}
        {shapeMode !== 'polygon' && (
          <div>
            <label className={labelClass}>Tamaño</label>
            <div className="grid grid-cols-2 gap-2">
              <div>
                <span className="text-[9px] text-textMuted mb-0.5 block">Ancho</span>
                <input
                  type="number" className={inputClass} min={40}
                  value={Math.round(element.width)}
                  onChange={(e) => update({ width: Math.max(40, Number(e.target.value)) })}
                />
              </div>
              <div>
                <span className="text-[9px] text-textMuted mb-0.5 block">Alto</span>
                <input
                  type="number" className={inputClass} min={30}
                  value={Math.round(element.height)}
                  onChange={(e) => update({ height: Math.max(30, Number(e.target.value)) })}
                />
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
                key={c.value}
                title={c.label}
                onClick={() => update({ color: c.value })}
                className="w-7 h-7 rounded-full border-2 transition-transform hover:scale-110"
                style={{
                  background:  c.value,
                  borderColor: element.color === c.value ? '#ffffff' : 'transparent',
                }}
              />
            ))}
            <input
              type="color"
              value={element.color}
              onChange={(e) => update({ color: e.target.value })}
              className="w-7 h-7 rounded-full cursor-pointer border-2 border-surfaceAlt"
              title="Color personalizado"
            />
          </div>
        </div>

        {/* Grilla de sillas */}
        {element.type === 'section' && element.seatLayout && !isEditingVertices && (
          <div>
            <label className={labelClass}>
              Grilla de sillas
              {shapeMode === 'polygon' && (
                <span className="ml-1 text-accent normal-case font-normal">(adaptada al contorno)</span>
              )}
            </label>
            <div className="grid grid-cols-2 gap-2">
              <div>
                <span className="text-[9px] text-textMuted mb-0.5 block">Filas</span>
                <input
                  type="number" className={inputClass} min={1} max={20}
                  value={element.seatLayout.rows}
                  onChange={(e) => {
                    const rows      = Math.max(1, Number(e.target.value));
                    const newLayout = { ...element.seatLayout, rows };
                    const newSize   = computeSectionSize(newLayout);
                    // Para polígonos, no forzar el resize (el contorno ya define el espacio)
                    update(shapeMode === 'polygon'
                      ? { seatLayout: newLayout }
                      : { seatLayout: newLayout, ...(newSize ?? {}) });
                  }}
                />
              </div>
              <div>
                <span className="text-[9px] text-textMuted mb-0.5 block">Columnas</span>
                <input
                  type="number" className={inputClass} min={1} max={30}
                  value={element.seatLayout.cols}
                  onChange={(e) => {
                    const cols      = Math.max(1, Number(e.target.value));
                    const newLayout = { ...element.seatLayout, cols };
                    const newSize   = computeSectionSize(newLayout);
                    update(shapeMode === 'polygon'
                      ? { seatLayout: newLayout }
                      : { seatLayout: newLayout, ...(newSize ?? {}) });
                  }}
                />
              </div>
            </div>
            <p className="text-[10px] text-textMuted mt-1">
              Máx {element.seatLayout.rows * element.seatLayout.cols} asientos
              {shapeMode === 'polygon' && ' (según forma)'}
            </p>
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
