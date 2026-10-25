import { useEffect, useMemo, useState } from 'react';
import { Gift } from 'lucide-react';
import BaseModal from '../common/BaseModal';
import { estimateEarnings, formatCOP } from './layoutEarnings';

/**
 * Menú de sillas de cortesía: cuántas cortesías tendrá cada sección lógica.
 * Las cortesías no se venden, así que se descuentan de la ganancia estimada.
 * Al guardar el layout, el backend las marca con estado COURTESY.
 *
 * Props:
 *   isOpen   — boolean
 *   groups   — secciones lógicas (getSectionGroups)
 *   onClose  — () => void
 *   onApply  — ({ [eventSectionId]: number }) => void
 */
export default function CourtesySeatsModal({ isOpen, groups, onClose, onApply }) {
  const [draft, setDraft] = useState({});

  // Al abrir, partir de las cortesías actuales de cada sección
  useEffect(() => {
    if (isOpen) setDraft(Object.fromEntries(groups.map((g) => [g.eventSectionId, String(g.courtesySeats)])));
  }, [isOpen, groups]);

  const parsed = useMemo(() => groups.map((g) => {
    const raw   = draft[g.eventSectionId] ?? '0';
    const value = raw === '' ? 0 : Number(raw);
    const valid = Number.isInteger(value) && value >= 0 && value <= g.totalSeats;
    return { ...g, courtesySeats: valid ? value : g.courtesySeats, valid };
  }), [groups, draft]);

  const allValid  = parsed.every((g) => g.valid);
  const estimate  = estimateEarnings(parsed);
  const withoutCourtesy = estimateEarnings(parsed.map((g) => ({ ...g, courtesySeats: 0 })));
  const totalCourtesy   = parsed.reduce((sum, g) => sum + g.courtesySeats, 0);

  const apply = () => {
    if (!allValid) return;
    onApply(Object.fromEntries(parsed.map((g) => [g.eventSectionId, g.courtesySeats])));
  };

  const inputClass =
    'w-24 bg-background border rounded-lg px-2 py-1.5 text-sm text-right text-textPrimary ' +
    'focus:outline-none focus:border-accent transition-colors';

  return (
    <BaseModal
      isOpen={isOpen}
      onClose={onClose}
      variant="info"
      icon={<Gift className="w-8 h-8" />}
      title="Sillas de cortesía"
      subtitle="¿Cuántas sillas de cortesía agregarás en cada sección?"
      maxWidth="max-w-xl"
      scrollableBody
      actions={[
        { label: 'Cancelar', variant: 'secondary', onClick: onClose },
        { label: 'Aplicar', variant: 'primary', onClick: apply, disabled: !allValid || groups.length === 0 },
      ]}
    >
      <div className="space-y-4 text-left">
        <p className="text-[11px] text-textMuted leading-snug">
          Las cortesías no se ponen a la venta y no generan ingresos. Al guardar el layout quedan reservadas
          en las últimas sillas de cada sección.
        </p>

        {groups.length === 0 ? (
          <p className="text-sm text-textMuted italic">Agrega al menos una sección al mapa.</p>
        ) : (
          <ul className="divide-y divide-surfaceAlt">
            {parsed.map((g) => (
              <li key={g.eventSectionId} className="flex items-center gap-3 py-2">
                <span className="w-2.5 h-2.5 rounded-sm flex-shrink-0" style={{ background: g.color }} />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-textPrimary truncate">{g.label}</p>
                  <p className="text-[11px] text-textMuted">
                    {g.totalSeats.toLocaleString('es-CO')} sillas · {formatCOP(g.price)} c/u
                  </p>
                </div>
                <div className="text-right">
                  <input
                    type="number"
                    min={0}
                    max={g.totalSeats}
                    step={1}
                    aria-label={`Cortesías de ${g.label}`}
                    className={`${inputClass} ${g.valid ? 'border-surfaceAlt' : 'border-error'}`}
                    value={draft[g.eventSectionId] ?? ''}
                    onChange={(e) => setDraft((d) => ({ ...d, [g.eventSectionId]: e.target.value }))}
                  />
                  {!g.valid && (
                    <p className="text-[10px] text-error mt-0.5">Entre 0 y {g.totalSeats.toLocaleString('es-CO')}</p>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}

        <div className="rounded-xl bg-surfaceAlt/60 px-4 py-3 space-y-1">
          <div className="flex justify-between text-sm">
            <span className="text-textSecondary">Ganancia estimada</span>
            <span className="font-bold text-textPrimary">{formatCOP(estimate)}</span>
          </div>
          {totalCourtesy > 0 && (
            <p className="text-[11px] text-textMuted">
              {totalCourtesy.toLocaleString('es-CO')} cortesías: {formatCOP(withoutCourtesy - estimate)} menos que
              vendiendo todas las sillas ({formatCOP(withoutCourtesy)}).
            </p>
          )}
          <p className="text-[10px] text-textMuted">Estimado si se venden todas las sillas que no son de cortesía.</p>
        </div>
      </div>
    </BaseModal>
  );
}
