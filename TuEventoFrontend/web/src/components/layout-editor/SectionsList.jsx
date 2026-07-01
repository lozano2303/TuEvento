import { useMemo, useState } from 'react';

export default function SectionsList({ elements, selectedIds, onSelect }) {
  // Índice de navegación por grupo: { [eventSectionId]: number }
  const [groupCursor, setGroupCursor] = useState({});

  const sections = useMemo(
    () => elements.filter((el) => el.type === 'section'),
    [elements],
  );

  // Agrupar por eventSectionId preservando orden de aparición
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

  if (groups.length === 0) {
    return (
      <aside className="w-[200px] flex-shrink-0 bg-surface border-l border-surfaceAlt flex flex-col overflow-hidden">
        <div className="px-3 py-3 border-b border-surfaceAlt">
          <h2 className="text-xs font-bold text-textSecondary uppercase tracking-wider">Secciones</h2>
        </div>
        <p className="text-textMuted text-xs text-center p-4 leading-relaxed">
          Sin secciones en el canvas
        </p>
      </aside>
    );
  }

  return (
    <aside className="w-[200px] flex-shrink-0 bg-surface border-l border-surfaceAlt flex flex-col overflow-hidden">
      <div className="px-3 py-3 border-b border-surfaceAlt">
        <h2 className="text-xs font-bold text-textSecondary uppercase tracking-wider">Secciones</h2>
      </div>

      <div className="flex-1 overflow-y-auto p-2 space-y-1.5">
        {groups.map(({ sectionId, instances, color, baseLabel }) => {
          const cursor      = groupCursor[sectionId] ?? 0;
          const safeIdx     = Math.min(cursor, instances.length - 1);
          const activeInst  = instances[safeIdx];
          const isSelected  = selectedIds.includes(activeInst.id);

          // Si alguna instancia del grupo está seleccionada, resaltarla
          const selectedInGroup = instances.find((i) => selectedIds.includes(i.id));

          const handlePrev = (e) => {
            e.stopPropagation();
            const newIdx = (safeIdx - 1 + instances.length) % instances.length;
            setGroupCursor((c) => ({ ...c, [sectionId]: newIdx }));
            onSelect(instances[newIdx].id);
          };
          const handleNext = (e) => {
            e.stopPropagation();
            const newIdx = (safeIdx + 1) % instances.length;
            setGroupCursor((c) => ({ ...c, [sectionId]: newIdx }));
            onSelect(instances[newIdx].id);
          };
          const handleHeaderClick = () => {
            // Si ya hay una instancia del grupo seleccionada, mantenerla; si no, ir a la activa
            const target = selectedInGroup ?? activeInst;
            onSelect(target.id);
          };

          return (
            <div
              key={sectionId}
              onClick={handleHeaderClick}
              className="rounded-lg overflow-hidden cursor-pointer transition-all"
              style={{
                border: `2px solid ${selectedInGroup ? color : 'transparent'}`,
                background: selectedInGroup ? `${color}18` : 'var(--color-surfaceAlt)',
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
                    className="text-[9px] font-bold px-1 py-0.5 rounded"
                    style={{ background: `${color}30`, color }}
                  >
                    {instances.length}
                  </span>
                )}
              </div>

              {/* Instancias navegables */}
              {instances.length > 1 && (
                <div className="flex items-center gap-1 px-2 pb-2">
                  <button
                    onClick={handlePrev}
                    className="text-textMuted hover:text-textPrimary transition-colors text-xs px-1.5 py-0.5
                               rounded hover:bg-surfaceAlt"
                    title="Instancia anterior"
                  >
                    ←
                  </button>
                  <span className="flex-1 text-[10px] text-textMuted text-center truncate">
                    {activeInst.label}
                  </span>
                  <button
                    onClick={handleNext}
                    className="text-textMuted hover:text-textPrimary transition-colors text-xs px-1.5 py-0.5
                               rounded hover:bg-surfaceAlt"
                    title="Siguiente instancia"
                  >
                    →
                  </button>
                </div>
              )}

              {/* Instancia única — mostrar label */}
              {instances.length === 1 && (
                <p className="text-[10px] text-textMuted px-2.5 pb-2 truncate">
                  {activeInst.label}
                </p>
              )}
            </div>
          );
        })}
      </div>
    </aside>
  );
}
