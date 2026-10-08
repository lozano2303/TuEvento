import { useState } from 'react';
import { Star } from 'lucide-react';

/**
 * StarRatingInput — selector interactivo de calificación de 1 a 5 estrellas.
 *
 * Props:
 *   value      {number}   Valor actual (0 = sin selección).
 *   onChange   {function} Callback (rating: number) => void al hacer clic.
 *   disabled   {boolean}  Deshabilita interacción (opcional).
 *
 * Usa las variables CSS del tema (--color-primary, --color-text-muted) en lugar
 * de colores fijos para respetar el sistema de diseño del proyecto.
 * Accesible: cada estrella tiene aria-label y aria-pressed.
 */
export default function StarRatingInput({ value = 0, onChange, disabled = false }) {
  const [hovered, setHovered] = useState(0);

  return (
    <fieldset className="border-0 p-0 m-0 mb-3" aria-label="Calificación del evento (obligatoria)">
      <legend className="text-xs mb-1" style={{ color: 'rgba(196,181,253,0.6)' }}>
        Calificación <span aria-hidden="true">*</span>
      </legend>
      <div
        className="flex items-center gap-1"
        role="group"
        aria-label="Selecciona entre 1 y 5 estrellas"
        onMouseLeave={() => setHovered(0)}
      >
        {[1, 2, 3, 4, 5].map((star) => {
          const active = star <= (hovered || value);
          return (
            <button
              key={star}
              type="button"
              disabled={disabled}
              onClick={() => onChange?.(star)}
              onMouseEnter={() => setHovered(star)}
              aria-label={`${star} estrella${star > 1 ? 's' : ''}`}
              aria-pressed={star <= value}
              className="transition-colors cursor-pointer disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Star
                className="w-5 h-5 pointer-events-none"
                style={{
                  color: active ? '#f59e0b' : 'rgba(196,181,253,0.3)',
                  transition: 'color 0.1s',
                }}
                fill={active ? '#f59e0b' : 'none'}
              />
            </button>
          );
        })}
      </div>
    </fieldset>
  );
}
