import { useEffect, useState } from 'react';
import { LayoutGrid } from 'lucide-react';
import BaseModal from '../common/BaseModal';
import { ShapeGrid } from './ShapeIcon';
import { SECTION_SHAPES, MAX_SECTION_SEATS } from './sectionShapes';

const DEFAULT_SEATS = 50;

/**
 * Asistente que aparece al añadir un tipo de sección al mapa: cuántas sillas,
 * qué forma base y qué precio por silla. Con eso se crea la sección ya
 * dimensionada para que quepan todas sus sillas.
 *
 * Props:
 *   template        — tipo de sección elegido ({ label, color, ... }) o null si está cerrado
 *   remainingSeats  — sillas que aún permite el aforo del evento (null si no hay tope)
 *   onCancel        — () => void
 *   onConfirm       — ({ targetSeats, shapeId, price }) => void
 */
export default function SectionWizardModal({ template, remainingSeats, onCancel, onConfirm }) {
  const [seats,   setSeats]   = useState(String(DEFAULT_SEATS));
  const [shapeId, setShapeId] = useState('rect');
  const [price,   setPrice]   = useState('');

  // Cada vez que se abre para un tipo nuevo, empezar de cero
  useEffect(() => {
    if (!template) return;
    setSeats(String(DEFAULT_SEATS));
    setShapeId('rect');
    setPrice('');
  }, [template]);

  const seatCount   = Number(seats);
  const priceValue  = Number(price);
  const seatsError  = !Number.isInteger(seatCount) || seatCount < 1 || seatCount > MAX_SECTION_SEATS
    ? `Ingresa un número entero entre 1 y ${MAX_SECTION_SEATS.toLocaleString('es-CO')}`
    : null;
  const overCapacity = !seatsError && remainingSeats != null && seatCount > remainingSeats;
  const priceError   = price === '' || !Number.isFinite(priceValue) || priceValue < 0
    ? 'Ingresa el precio por silla (0 o más)'
    : null;

  const inputClass =
    'w-full bg-background border border-surfaceAlt rounded-lg px-3 py-2 text-sm text-textPrimary ' +
    'focus:outline-none focus:border-accent transition-colors';
  const questionClass = 'text-sm font-semibold text-textPrimary block mb-1';
  const hintClass     = 'text-[11px] text-textMuted mt-1 leading-snug';

  const confirm = () => {
    if (seatsError || priceError) return;
    onConfirm({ targetSeats: seatCount, shapeId, price: priceValue });
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
        { label: 'Crear sección', variant: 'primary', onClick: confirm, disabled: Boolean(seatsError || priceError) },
      ]}
    >
      <div className="space-y-5 text-left">
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
            className={inputClass}
            value={seats}
            onChange={(e) => setSeats(e.target.value)}
          />
          {seatsError
            ? <p className="text-[11px] text-error mt-1">{seatsError}</p>
            : (
              <p className={hintClass}>
                Podrás cambiarlo después en el panel Propiedades, apartado Sillas.
                {remainingSeats != null && ` Quedan ${remainingSeats.toLocaleString('es-CO')} sillas disponibles en el aforo del evento.`}
              </p>
            )}
          {overCapacity && (
            <p className="text-[11px] text-yellow-400 mt-1">
              Supera el aforo restante del evento. Podrás crearla, pero no guardar el layout hasta ajustarla.
            </p>
          )}
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
            className={inputClass}
            value={price}
            onChange={(e) => setPrice(e.target.value)}
          />
          <p className={priceError && price !== '' ? 'text-[11px] text-error mt-1' : hintClass}>
            {priceError && price !== ''
              ? priceError
              : 'Todas las sub-secciones de esta sección tendrán el mismo precio.'}
          </p>
        </div>
      </div>
    </BaseModal>
  );
}
