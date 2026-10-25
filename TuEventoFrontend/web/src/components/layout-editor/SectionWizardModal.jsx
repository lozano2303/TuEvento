import { useEffect, useState } from 'react';
import { LayoutGrid } from 'lucide-react';
import BaseModal from '../common/BaseModal';
import { ShapeGrid } from './ShapeIcon';
import { SECTION_SHAPES, MAX_SECTION_SEATS, SEAT_SIZES, suggestedSeatSize } from './sectionShapes';

const DEFAULT_SEATS = 50;
const formatInt = (n) => n.toLocaleString('es-CO');

export const seatsErrorFor = (raw) => {
  if (raw.trim() === '') return 'Ingresa cuántas sillas tendrá la sección';
  const n = Number(raw);
  if (!Number.isInteger(n)) return 'La cantidad de sillas debe ser un número entero';
  if (n < 1) return 'La sección debe tener al menos 1 silla';
  if (n > MAX_SECTION_SEATS) return `Una sección puede tener como máximo ${formatInt(MAX_SECTION_SEATS)} sillas`;
  return null;
};

export const priceErrorFor = (raw) => {
  if (raw.trim() === '') return 'Ingresa el precio por silla (puede ser 0 si es gratis)';
  const n = Number(raw);
  if (!Number.isFinite(n)) return 'El precio debe ser un número';
  if (n < 0) return 'El precio no puede ser negativo';
  return null;
};

/**
 * Asistente que aparece al añadir un tipo de sección al mapa: cuántas sillas,
 * qué forma base, de qué tamaño son las sillas y qué precio tiene cada una.
 * Con eso se crea la sección ya dimensionada para que quepan todas sus sillas.
 *
 * Props:
 *   template        — tipo de sección elegido ({ label, color, ... }) o null si está cerrado
 *   remainingSeats  — sillas que aún permite el aforo del evento (null si no hay tope)
 *   onCancel        — () => void
 *   onConfirm       — ({ targetSeats, shapeId, seatSize, price }) => void
 */
export default function SectionWizardModal({ template, remainingSeats, onCancel, onConfirm }) {
  const [seats,     setSeats]     = useState(String(DEFAULT_SEATS));
  const [shapeId,   setShapeId]   = useState('rect');
  const [seatSize,  setSeatSize]  = useState('normal');
  const [sizeTouched, setSizeTouched] = useState(false);
  const [price,     setPrice]     = useState('');
  const [touched,   setTouched]   = useState({ seats: false, price: false });
  const [submitted, setSubmitted] = useState(false);

  // Cada vez que se abre para un tipo nuevo, empezar de cero
  useEffect(() => {
    if (!template) return;
    setSeats(String(DEFAULT_SEATS));
    setShapeId('rect');
    setSeatSize('normal');
    setSizeTouched(false);
    setPrice('');
    setTouched({ seats: false, price: false });
    setSubmitted(false);
  }, [template]);

  const seatsError = seatsErrorFor(seats);
  const priceError = priceErrorFor(price);
  const showSeatsError = seatsError && (touched.seats || submitted);
  const showPriceError = priceError && (touched.price || submitted);
  const seatCount    = Number(seats);
  const overCapacity = !seatsError && remainingSeats != null && seatCount > remainingSeats;

  const handleSeatsChange = (value) => {
    // No se puede escribir más del máximo por sección: se recorta al tope
    const next = Number(value) > MAX_SECTION_SEATS ? String(MAX_SECTION_SEATS) : value;
    setSeats(next);
    // Mientras el usuario no elija un tamaño, se sugiere uno según la cantidad
    if (!sizeTouched && !seatsErrorFor(next)) setSeatSize(suggestedSeatSize(Number(next)));
  };

  const inputClass = (hasError) =>
    'w-full bg-background border rounded-lg px-3 py-2 text-sm text-textPrimary ' +
    'focus:outline-none transition-colors ' +
    (hasError ? 'border-error focus:border-error' : 'border-surfaceAlt focus:border-accent');
  const questionClass = 'text-sm font-semibold text-textPrimary block mb-1';
  const hintClass     = 'text-[11px] text-textMuted mt-1 leading-snug';
  const errorClass    = 'text-[11px] text-error mt-1 leading-snug';

  const confirm = () => {
    setSubmitted(true);
    if (seatsError || priceError) return;
    onConfirm({ targetSeats: seatCount, shapeId, seatSize, price: Number(price) });
  };

  return (
    <BaseModal
      isOpen={template != null}
      onClose={onCancel}
      variant="info"
      icon={<LayoutGrid className="w-8 h-8" />}
      title={`Nueva sección: ${template?.label ?? ''}`}
      subtitle="Configura la sección antes de ubicarla en el mapa"
      maxWidth="max-w-xl"
      scrollableBody
      hideOverlayClose
      actions={[
        { label: 'Cancelar', variant: 'secondary', onClick: onCancel },
        { label: 'Crear sección', variant: 'primary', onClick: confirm },
      ]}
    >
      <div className="space-y-5 text-left">
        {submitted && (seatsError || priceError) && (
          <p role="alert" className="text-xs text-error bg-error/10 border border-error/30 rounded-lg px-3 py-2">
            Revisa los campos marcados para crear la sección.
          </p>
        )}

        <div>
          <label htmlFor="wizard-seats" className={questionClass}>
            ¿Cuántas sillas quieres que tenga esta sección?
          </label>
          <input
            id="wizard-seats"
            type="number"
            min={1}
            max={MAX_SECTION_SEATS}
            step={1}
            aria-invalid={Boolean(showSeatsError)}
            className={inputClass(showSeatsError)}
            value={seats}
            onChange={(e) => handleSeatsChange(e.target.value)}
            onBlur={() => setTouched((t) => ({ ...t, seats: true }))}
          />
          {showSeatsError
            ? <p className={errorClass}>{seatsError}</p>
            : (
              <p className={hintClass}>
                Máximo {formatInt(MAX_SECTION_SEATS)} por sección. Podrás cambiarlo después en el panel Propiedades,
                apartado Sillas.
                {remainingSeats != null && ` Quedan ${formatInt(remainingSeats)} sillas disponibles en el aforo del evento.`}
              </p>
            )}
          {overCapacity && (
            <p className="text-[11px] text-yellow-400 mt-1">
              Supera el aforo restante del evento. Podrás crearla, pero no guardar el layout hasta ajustarla.
            </p>
          )}
        </div>

        <div>
          <span className={questionClass}>¿De qué tamaño son las sillas en el mapa?</span>
          <div className="grid grid-cols-3 gap-1.5" role="radiogroup" aria-label="Tamaño de las sillas">
            {SEAT_SIZES.map((size) => (
              <button
                key={size.id}
                type="button"
                role="radio"
                aria-checked={seatSize === size.id}
                onClick={() => { setSeatSize(size.id); setSizeTouched(true); }}
                className={[
                  'rounded-md px-2 py-1.5 text-xs border transition-colors',
                  seatSize === size.id
                    ? 'bg-accent/20 border-accent/60 text-textPrimary'
                    : 'bg-surfaceAlt border-transparent text-textSecondary hover:border-accent/30',
                ].join(' ')}
              >
                {size.label}
              </button>
            ))}
          </div>
          <p className={hintClass}>
            Con muchas sillas conviene un tamaño menor para que la sección no ocupe todo el mapa.
            Las páginas de 10 × 10 del selector de asientos no cambian.
          </p>
        </div>

        <div>
          <span className={questionClass}>¿Qué forma quieres como base?</span>
          <ShapeGrid
            shapes={SECTION_SHAPES}
            selectedId={shapeId}
            onSelect={setShapeId}
            color={template?.color}
            columns={5}
          />
          <p className={hintClass}>
            La sección se crea con el tamaño justo para sus sillas. Después puedes ajustar sus vértices con Editar forma.
          </p>
        </div>

        <div>
          <label htmlFor="wizard-price" className={questionClass}>
            ¿Cuál es el precio por silla de esta sección?
          </label>
          <input
            id="wizard-price"
            type="number"
            min={0}
            step={1000}
            placeholder="0"
            aria-invalid={Boolean(showPriceError)}
            className={inputClass(showPriceError)}
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            onBlur={() => setTouched((t) => ({ ...t, price: true }))}
          />
          <p className={showPriceError ? errorClass : hintClass}>
            {showPriceError ? priceError : 'Todas las sub-secciones de esta sección tendrán el mismo precio.'}
          </p>
        </div>
      </div>
    </BaseModal>
  );
}
