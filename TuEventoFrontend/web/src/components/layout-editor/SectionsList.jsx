import { useMemo, useState } from 'react';

export default function SectionsList({ elements, selectedIds, onSelect }) {
  const [groupCursor, setGroupCursor] = useState({});

  const sections = useMemo(
    () => elements.filter((el) => el.type === 'section'),
    [elements],
  );

  const groups = useMemo(() => {
    const map = new Map();
    for (const el of sections) {
      const key = el.eventSectionId ?? el.id;
      if (!map.has(key)) map.set(key, []);
      map.get(key).push(el);
    }
    return [...map.entries()].map(([sectionId, instances]) => ({
      sectionId,
      instances,
      color:     instances[0].color,
      baseLabel: instances[0].label.replace(/ \d+$/, ''),
    }));
  }, [sections]);

  const header = (
    <div className="px-3 py-3 border-b border-surfaceAlt">
      <h2 className="text-sm font-bold text-textPrimary">Secciones del mapa</h2>
    </div>
  );

  if (groups.length === 0) {
    return (
      <div className="flex flex-col overflow-hidden">
        {header}
        <p className="text-textSecondary text-xs text-center p-4 leading-relaxed">
          Sin secciones en el layout
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col overflow-hidden h-full">
      {header}
      <div className="flex-1 overflow-y-auto p-2 space-y-1.5">
        {groups.map(({ sectionId, instances, color, baseLabel }) => {
          const cursor          = groupCursor[sectionId] ?? 0;
          const safeIdx         = Math.min(cursor, instances.length - 1);
          const activeInst      = instances[safeIdx];
          const selectedInGroup = instances.find((i) => selectedIds.includes(i.id));

          // Cuando hay instancia seleccionada, mostrarla como activa en la navegación
          const displayInst = selectedInGroup ?? activeInst;
          const displayIdx  = selectedInGroup
            ? instances.indexOf(selectedInGroup)
            : safeIdx;

          const handlePrev = (e) => {
            e.stopPropagation();
            const newIdx = (displayIdx - 1 + instances.length) % instances.length;
            setGroupCursor((c) => ({ ...c, [sectionId]: newIdx }));
            onSelect(instances[newIdx].id);
          };
          const handleNext = (e) => {
            e.stopPropagation();
            const newIdx = (displayIdx + 1) % instances.length;
            setGroupCursor((c) => ({ ...c, [sectionId]: newIdx }));
            onSelect(instances[newIdx].id);
          };
          const handleHeaderClick = () => {
            onSelect((selectedInGroup ?? activeInst).id);
          };

          return (
            <div
              key={sectionId}
              onClick={handleHeaderClick}
              className="rounded-lg overflow-hidden cursor-pointer transition-all"
              style={{
                borderLeft:  `4px solid ${selectedInGroup ? color : 'transparent'}`,
                background:  selectedInGroup ? `${color}15` : 'var(--color-surfaceAlt)',
                border:      selectedInGroup ? `1px solid ${color}40` : '1px solid transparent',
                borderLeftWidth: '4px',
                borderLeftColor: selectedInGroup ? color : 'transparent',
              }}
            >
              {/* Header */}
              <div className="flex items-center gap-2 px-2.5 py-2">
                <span
                  className="w-2.5 h-2.5 rounded-full flex-shrink-0"
                  style={{ background: color }}
                />
                <span className="text-xs font-semibold text-textPrimary flex-1 truncate">
                  {baseLabel}
                </span>
                {instances.length > 1 && (
                  <span
                    className="text-[9px] font-bold px-1.5 py-0.5 rounded-full"
                    style={{ background: `${color}25`, color }}
                  >
                    {instances.length} partes
                  </span>
                )}
              </div>

              {/* Navegación entre instancias */}
              {instances.length > 1 && (
                <div className="flex items-center gap-1 px-2 pb-2">
                  <button
                    onClick={handlePrev}
                    className="text-textMuted hover:text-textPrimary transition-colors text-xs
                               px-1.5 py-0.5 rounded hover:bg-black/10"
                    title="Parte anterior"
                  >
                    ←
                  </button>
                  <span className="flex-1 text-[10px] text-textMuted text-center truncate">
                    {displayInst.label}
                  </span>
                  <button
                    onClick={handleNext}
                    className="text-textMuted hover:text-textPrimary transition-colors text-xs
                               px-1.5 py-0.5 rounded hover:bg-black/10"
                    title="Siguiente parte"
                  >
                    →
                  </button>
                </div>
              )}

              {/* Instancia única */}
              {instances.length === 1 && (
                <p className="text-[10px] text-textMuted px-2.5 pb-2 truncate">
                  {activeInst.label}
                </p>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
