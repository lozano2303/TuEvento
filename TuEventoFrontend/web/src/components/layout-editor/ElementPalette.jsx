import { useMemo, useState } from 'react';
import { PALETTE_ELEMENTS } from '../../data/mockLayoutElements';
import { generateId } from './layoutEditorUtils';
import { decorIcon } from '../../utils/decorIcons';

// Infraestructura — siempre hardcodeada, sin depender del backend
const INFRA = PALETTE_ELEMENTS.filter((e) => e.type !== 'section');

// Paleta de colores round-robin para los tipos de sección dinámicos
const SECTION_COLORS = [
  '#7C3AED', // morado
  '#16A34A', // verde
  '#EA580C', // naranja
  '#2563EB', // azul
  '#DB2777', // rosado
  '#0891B2', // cyan
  '#CA8A04', // amarillo
  '#DC2626', // rojo
];

// Categorías de section_type.category, en el orden en que se muestran los filtros
const CATEGORIES = [
  { value: 'GENERAL',      label: 'General' },
  { value: 'PREFERENCIAL', label: 'Preferencial' },
  { value: 'VIP',          label: 'VIP' },
  { value: 'PALCO',        label: 'Palcos' },
  { value: 'ESPECIAL',     label: 'Especiales' },
];
const ALL = 'ALL';

// Valores iniciales: el asistente fija las sillas, la forma y el tamaño definitivos
const DEFAULT_SEAT_LAYOUT = { targetSeats: 9, seatRadius: 8, gap: 4 };

const normalize = (text) => text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

/**
 * ElementPalette
 *
 * Props:
 *   onAddElement          — callback cuando el usuario añade un elemento. Para una
 *                           sección, el editor abre primero el asistente de creación.
 *   occupiedSectionCounts — { [sectionType: string]: number } — grupos ya en canvas
 *   sectionTypes          — array de { sectionTypeId, name, category, active } del backend.
 *                           Si es null/undefined, usa las secciones de ejemplo.
 *   disabled              — true en modo solo lectura: no se puede añadir nada
 */
export default function ElementPalette({ onAddElement, occupiedSectionCounts = {}, sectionTypes, disabled = false }) {
  const [category, setCategory] = useState(ALL);
  const [query,    setQuery]    = useState('');

  // Construir la lista de templates de sección a partir del backend (o fallback estático)
  const sectionTemplates = useMemo(() => {
    if (sectionTypes && sectionTypes.length > 0) {
      return sectionTypes
        .filter((st) => st.active !== false && st.name.toLowerCase() !== 'pista') // excluir tipo legacy del seed
        .sort((a, b) => a.name.localeCompare(b.name, 'es'))
        .map((st, idx) => ({
          type:          'section',
          sectionType:   st.name,
          sectionTypeId: st.sectionTypeId,
          category:      st.category ?? 'GENERAL',
          label:         st.name,
          defaultWidth:  100,
          defaultHeight: 100,
          color:         SECTION_COLORS[idx % SECTION_COLORS.length],
          seatLayout:    DEFAULT_SEAT_LAYOUT,
        }));
    }
    // Fallback — paleta estática (modo demo sin eventId)
    return PALETTE_ELEMENTS.filter((e) => e.type === 'section').map((e) => ({
      ...e,
      sectionTypeId: null,
      category:      'GENERAL',
    }));
  }, [sectionTypes]);

  const visibleTemplates = useMemo(() => {
    const q = normalize(query.trim());
    return sectionTemplates.filter((t) =>
      (category === ALL || t.category === category) && (q === '' || normalize(t.label).includes(q)));
  }, [sectionTemplates, category, query]);

  // Solo se muestran los filtros de categorías que tienen al menos un tipo
  const availableCategories = useMemo(
    () => CATEGORIES.filter((c) => sectionTemplates.some((t) => t.category === c.value)),
    [sectionTemplates],
  );

  const handleDragStart = (e, template) => {
    e.dataTransfer.setData('template', JSON.stringify(template));
  };

  const handleClick = (template) => {
    onAddElement({
      id:            generateId(),
      type:          template.type,
      sectionType:   template.sectionType ?? null,
      sectionTypeId: template.sectionTypeId ?? null,
      backendSectionId: null,
      eventSectionId: null,
      price:         null,
      seatLayout:    template.seatLayout ?? null,
      x:             120 + Math.random() * 200,
      y:             100 + Math.random() * 200,
      width:         template.defaultWidth,
      height:        template.defaultHeight,
      rotation:      0,
      label:         template.label,
      color:         template.color,
    });
  };

  const itemClass = (blocked) => [
    'w-full flex items-center gap-2 px-3 py-1.5 rounded-lg select-none transition-colors text-left',
    blocked ? 'opacity-40 cursor-not-allowed' : 'cursor-grab active:cursor-grabbing hover:bg-surfaceAlt',
  ].join(' ');

  const PaletteItem = ({ template, blocked, title, badge }) => (
    <button
      type="button"
      draggable={!blocked}
      disabled={blocked}
      onDragStart={blocked ? undefined : (e) => handleDragStart(e, template)}
      onClick={blocked ? undefined : () => handleClick(template)}
      className={itemClass(blocked)}
      title={title}
    >
      <span className="w-2.5 h-2.5 rounded-sm flex-shrink-0" style={{ background: template.color }} />
      <span className="flex-1 min-w-0 text-xs font-medium text-textPrimary truncate">
        {template.type !== 'section' && decorIcon(template.type) && (
          <span className="mr-1" aria-hidden="true">{decorIcon(template.type)}</span>
        )}
        {template.label}
      </span>
      {badge && (
        <span className="text-[9px] font-bold px-1 py-0.5 rounded bg-accent/20 text-accent border border-accent/30 leading-none">
          {badge}
        </span>
      )}
    </button>
  );

  const chipClass = (active) => [
    'text-[10px] font-semibold px-2 py-0.5 rounded-full border transition-colors',
    active
      ? 'bg-accent/20 text-accent border-accent/50'
      : 'text-textMuted border-surfaceAlt hover:text-textSecondary hover:border-accent/30',
  ].join(' ');

  return (
    <aside
      className="w-[220px] flex-shrink-0 bg-surface border-r border-surfaceAlt flex flex-col overflow-hidden"
      style={{ height: '100%' }}
    >
      <div className="px-3 py-3 border-b border-surfaceAlt">
        <h2 className="text-xs font-bold text-textSecondary uppercase tracking-wider">
          Elementos
        </h2>
      </div>

      <div className="flex-1 overflow-y-auto p-2 space-y-3">
        <div>
          <p className="text-[10px] font-semibold text-textMuted uppercase tracking-wider px-1 mb-1.5">
            Secciones
          </p>

          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Buscar tipo de sección"
            aria-label="Buscar tipo de sección"
            className="w-full mb-2 bg-background border border-surfaceAlt rounded px-2 py-1 text-xs text-textPrimary
                       focus:outline-none focus:border-accent transition-colors"
          />

          {availableCategories.length > 1 && (
            <div className="flex flex-wrap gap-1 mb-2" role="group" aria-label="Filtrar por categoría">
              <button type="button" className={chipClass(category === ALL)} onClick={() => setCategory(ALL)}>
                Todas
              </button>
              {availableCategories.map((c) => (
                <button key={c.value} type="button" className={chipClass(category === c.value)}
                  onClick={() => setCategory(c.value)}>
                  {c.label}
                </button>
              ))}
            </div>
          )}

          {sectionTypes == null && (
            <p className="text-[10px] text-textMuted px-2 py-1 italic">Cargando tipos de sección…</p>
          )}
          {sectionTypes != null && visibleTemplates.length === 0 && (
            <p className="text-[10px] text-textMuted px-2 py-1 italic">No hay tipos de sección con ese filtro</p>
          )}
          <div className="space-y-0.5">
            {visibleTemplates.map((t) => {
              const count = occupiedSectionCounts[t.sectionType] ?? 0;
              return (
                <PaletteItem
                  key={`${t.type}-${t.sectionType}`}
                  template={t}
                  blocked={disabled || count > 0}
                  badge={count > 0 ? `En uso (${count})` : null}
                  title={count > 0
                    ? 'Ya existe en el mapa: usa Agregar sub-sección o Ctrl+C / Ctrl+V para duplicarla'
                    : 'Arrastra al mapa o haz clic para configurarla'}
                />
              );
            })}
          </div>
        </div>

        <div>
          <p className="text-[10px] font-semibold text-textMuted uppercase tracking-wider px-1 mb-1">
            Infraestructura
          </p>
          <div className="space-y-0.5">
            {INFRA.map((t) => (
              <PaletteItem key={t.type} template={t} blocked={disabled} title="Arrastra al mapa o haz clic para añadir" />
            ))}
          </div>
        </div>
      </div>

      <div className="px-3 py-2 border-t border-surfaceAlt">
        <p className="text-[10px] text-textMuted text-center leading-tight">
          Arrastra o haz clic para añadir
        </p>
      </div>
    </aside>
  );
}
