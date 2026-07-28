import { useMemo } from 'react';
import { PALETTE_ELEMENTS } from '../../data/mockLayoutElements';
import { generateId } from './layoutEditorUtils';

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

// Iconos por nombre de tipo de sección (fallback genérico)
const SECTION_ICONS = {
  vip:      '⭐',
  general:  '👥',
  palco:    '🎪',
  tribuna:  '🏟️',
  platinum: '💎',
  gold:     '🥇',
  silver:   '🥈',
  bronze:   '🥉',
};

const getSectionIcon = (name) =>
  SECTION_ICONS[(name ?? '').toLowerCase()] ?? '🪑';

/**
 * ElementPalette
 *
 * Props:
 *   onAddElement         — callback cuando el usuario añade un elemento
 *   occupiedSectionCounts — { [sectionType: string]: number } — grupos ya en canvas
 *   sectionTypes          — array de { sectionTypeId, name } del backend.
 *                           Si es null/undefined, usa las 4 secciones hardcodeadas.
 */
export default function ElementPalette({ onAddElement, occupiedSectionCounts = {}, sectionTypes }) {
  // Construir la lista de templates de sección a partir del backend (o fallback estático)
  const sectionTemplates = useMemo(() => {
    if (sectionTypes && sectionTypes.length > 0) {
      return sectionTypes
        .filter((st) => st.name.toLowerCase() !== 'pista') // excluir tipo legacy del seed
        .map((st, idx) => ({
          type:          'section',
          sectionType:   st.name,
          sectionTypeId: st.sectionTypeId,
          label:         st.name,
          icon:          getSectionIcon(st.name),
          defaultWidth:  100,
          defaultHeight: 100,
          color:         SECTION_COLORS[idx % SECTION_COLORS.length],
          seatLayout:    { targetSeats: 9, seatRadius: 8, gap: 4 },
        }));
    }
    // Fallback — paleta estática (modo demo sin eventId)
    return PALETTE_ELEMENTS.filter((e) => e.type === 'section').map((e) => ({
      ...e,
      sectionTypeId: null,
    }));
  }, [sectionTypes]);

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

  const SectionItem = ({ template }) => {
    const count    = occupiedSectionCounts[template.sectionType] ?? 0;
    const occupied = count > 0;
    return (
      <div className="relative">
        <div
          draggable={!occupied}
          onDragStart={occupied ? undefined : (e) => handleDragStart(e, template)}
          onClick={occupied ? undefined : () => handleClick(template)}
          className={[
            'flex items-center gap-2 px-3 py-2 rounded-lg select-none transition-colors',
            occupied
              ? 'opacity-40 cursor-not-allowed'
              : 'cursor-grab active:cursor-grabbing hover:bg-surfaceAlt',
          ].join(' ')}
          title={occupied ? 'Ya existe en el canvas — usa Ctrl+C/V para duplicar' : 'Arrastra al canvas o haz clic para añadir'}
        >
          <span className="text-lg leading-none">{template.icon}</span>
          <div className="flex-1 min-w-0">
            <p className="text-xs font-medium text-textPrimary truncate">{template.label}</p>
            {template.seatLayout && (
              <p className="text-[10px] text-textMuted">
                {template.seatLayout.targetSeats} asientos por defecto
              </p>
            )}
          </div>
          <div
            className="w-3 h-3 rounded-sm flex-shrink-0"
            style={{ background: template.color }}
          />
        </div>
        {occupied && (
          <span className="absolute top-1 right-2 text-[9px] font-bold px-1 py-0.5 rounded
                           bg-accent/20 text-accent border border-accent/30 pointer-events-none leading-none">
            En uso ({count})
          </span>
        )}
      </div>
    );
  };

  const InfraItem = ({ template }) => (
    <div
      draggable
      onDragStart={(e) => handleDragStart(e, template)}
      onClick={() => handleClick(template)}
      className="flex items-center gap-2 px-3 py-2 rounded-lg cursor-grab active:cursor-grabbing
                 hover:bg-surfaceAlt transition-colors select-none"
      title="Arrastra al canvas o haz clic para añadir"
    >
      <span className="text-lg leading-none">{template.icon}</span>
      <div className="flex-1 min-w-0">
        <p className="text-xs font-medium text-textPrimary truncate">{template.label}</p>
      </div>
      <div
        className="w-3 h-3 rounded-sm flex-shrink-0"
        style={{ background: template.color }}
      />
    </div>
  );

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
          <p className="text-[10px] font-semibold text-textMuted uppercase tracking-wider px-1 mb-1">
            Secciones
          </p>
          {sectionTemplates.length === 0 ? (
            <p className="text-[10px] text-textMuted px-2 py-1 italic">
              Cargando tipos de sección…
            </p>
          ) : (
            <div className="space-y-0.5">
              {sectionTemplates.map((t) => (
                <SectionItem key={`${t.type}-${t.sectionType}`} template={t} />
              ))}
            </div>
          )}
        </div>

        <div>
          <p className="text-[10px] font-semibold text-textMuted uppercase tracking-wider px-1 mb-1">
            Infraestructura
          </p>
          <div className="space-y-0.5">
            {INFRA.map((t) => (
              <InfraItem key={t.type} template={t} />
            ))}
          </div>
        </div>
      </div>

      <div className="px-3 py-2 border-t border-surfaceAlt">
        <p className="text-[10px] text-textMuted text-center leading-tight">
          Arrastra o clic para añadir
        </p>
      </div>
    </aside>
  );
}
