import { PALETTE_ELEMENTS } from '../../data/mockLayoutElements';
import { generateId } from './layoutEditorUtils';

const INFRA    = PALETTE_ELEMENTS.filter((e) => e.type !== 'section');
const SECTIONS = PALETTE_ELEMENTS.filter((e) => e.type === 'section');

export default function ElementPalette({ onAddElement, occupiedSectionCounts = {} }) {
  // occupiedSectionCounts: { [sectionType]: number } — 0 means not occupied
  const handleDragStart = (e, template) => {
    e.dataTransfer.setData('template', JSON.stringify(template));
  };

  const handleClick = (template) => {
    onAddElement({
      id: generateId(),
      type: template.type,
      sectionType: template.sectionType ?? null,
      eventSectionId: null,
      seatLayout: template.seatLayout ?? null,
      x: 120 + Math.random() * 200,
      y: 100 + Math.random() * 200,
      width: template.defaultWidth,
      height: template.defaultHeight,
      rotation: 0,
      label: template.label,
      color: template.color,
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
                {template.seatLayout.rows}×{template.seatLayout.cols} asientos
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
          <div className="space-y-0.5">
            {SECTIONS.map((t) => (
              <SectionItem key={`${t.type}-${t.sectionType}`} template={t} />
            ))}
          </div>
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
