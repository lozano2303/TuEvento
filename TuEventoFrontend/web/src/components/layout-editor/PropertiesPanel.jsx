import { computeSectionSize } from './layoutEditorUtils';

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

export default function PropertiesPanel({ element, onChange, onDelete }) {
  if (!element) {
    return (
      <aside className="w-[240px] flex-shrink-0 bg-surface border-l border-surfaceAlt flex items-center justify-center p-4">
        <p className="text-textMuted text-xs text-center leading-relaxed">
          Selecciona o arrastra un elemento al canvas para editar sus propiedades.
        </p>
      </aside>
    );
  }

  const update = (patch) => onChange({ ...element, ...patch });
  const colors = element.type === 'section' ? SECTION_COLORS : INFRA_COLORS;

  const inputClass =
    'w-full bg-background border border-surfaceAlt rounded px-2 py-1.5 text-xs text-textPrimary ' +
    'focus:outline-none focus:border-accent transition-colors';
  const labelClass = 'text-[10px] font-semibold text-textMuted uppercase tracking-wider mb-1 block';

  return (
    <aside className="w-[240px] flex-shrink-0 bg-surface border-l border-surfaceAlt flex flex-col overflow-hidden">
      <div className="px-3 py-3 border-b border-surfaceAlt flex items-center justify-between">
        <h2 className="text-xs font-bold text-textSecondary uppercase tracking-wider">
          Propiedades
        </h2>
        {element.type === 'section' && (
          <span
            className="text-[10px] font-bold px-2 py-0.5 rounded-full text-white"
            style={{ background: element.color }}
          >
            {element.sectionType}
          </span>
        )}
      </div>

      <div className="flex-1 overflow-y-auto p-3 space-y-4">
        {/* Label */}
        <div>
          <label className={labelClass}>Etiqueta</label>
          <input
            type="text"
            className={inputClass}
            value={element.label}
            onChange={(e) => update({ label: e.target.value })}
          />
        </div>

        {/* Posición */}
        <div>
          <label className={labelClass}>Posición</label>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <span className="text-[9px] text-textMuted mb-0.5 block">X</span>
              <input
                type="number"
                className={inputClass}
                value={Math.round(element.x)}
                onChange={(e) => update({ x: Number(e.target.value) })}
              />
            </div>
            <div>
              <span className="text-[9px] text-textMuted mb-0.5 block">Y</span>
              <input
                type="number"
                className={inputClass}
                value={Math.round(element.y)}
                onChange={(e) => update({ y: Number(e.target.value) })}
              />
            </div>
          </div>
        </div>

        {/* Tamaño */}
        <div>
          <label className={labelClass}>Tamaño</label>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <span className="text-[9px] text-textMuted mb-0.5 block">Ancho</span>
              <input
                type="number"
                className={inputClass}
                value={Math.round(element.width)}
                min={40}
                onChange={(e) => update({ width: Math.max(40, Number(e.target.value)) })}
              />
            </div>
            <div>
              <span className="text-[9px] text-textMuted mb-0.5 block">Alto</span>
              <input
                type="number"
                className={inputClass}
                value={Math.round(element.height)}
                min={30}
                onChange={(e) => update({ height: Math.max(30, Number(e.target.value)) })}
              />
            </div>
          </div>
        </div>

        {/* Rotación */}
        <div>
          <label className={labelClass}>Rotación ({Math.round(element.rotation ?? 0)}°)</label>
          <input
            type="range"
            min={0}
            max={360}
            step={1}
            value={element.rotation ?? 0}
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
                  background: c.value,
                  borderColor: element.color === c.value ? '#ffffff' : 'transparent',
                }}
              />
            ))}
            {/* Color personalizado */}
            <input
              type="color"
              value={element.color}
              onChange={(e) => update({ color: e.target.value })}
              className="w-7 h-7 rounded-full cursor-pointer border-2 border-surfaceAlt"
              title="Color personalizado"
            />
          </div>
        </div>

        {/* Grilla de sillas — solo secciones con seatLayout */}
        {element.type === 'section' && element.seatLayout && (
          <div>
            <label className={labelClass}>Grilla de sillas</label>
            <div className="grid grid-cols-2 gap-2">
              <div>
                <span className="text-[9px] text-textMuted mb-0.5 block">Filas</span>
                <input
                  type="number"
                  className={inputClass}
                  value={element.seatLayout.rows}
                  min={1}
                  max={20}
                  onChange={(e) => {
                    const rows = Math.max(1, Number(e.target.value));
                    const newLayout = { ...element.seatLayout, rows };
                    const newSize = computeSectionSize(newLayout);
                    update({ seatLayout: newLayout, ...(newSize ?? {}) });
                  }}
                />
              </div>
              <div>
                <span className="text-[9px] text-textMuted mb-0.5 block">Columnas</span>
                <input
                  type="number"
                  className={inputClass}
                  value={element.seatLayout.cols}
                  min={1}
                  max={30}
                  onChange={(e) => {
                    const cols = Math.max(1, Number(e.target.value));
                    const newLayout = { ...element.seatLayout, cols };
                    const newSize = computeSectionSize(newLayout);
                    update({ seatLayout: newLayout, ...(newSize ?? {}) });
                  }}
                />
              </div>
            </div>
            <p className="text-[10px] text-textMuted mt-1">
              Total: {element.seatLayout.rows * element.seatLayout.cols} asientos
            </p>
          </div>
        )}
      </div>

      {/* Eliminar */}
      <div className="p-3 border-t border-surfaceAlt">
        <button
          onClick={onDelete}
          className="w-full py-2 rounded-lg text-xs font-semibold bg-error/10 text-error
                     border border-error/30 hover:bg-error/20 transition-colors"
        >
          Eliminar elemento
        </button>
      </div>
    </aside>
  );
}
