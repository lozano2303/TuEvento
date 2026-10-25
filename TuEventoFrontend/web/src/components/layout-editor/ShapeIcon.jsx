import { useMemo } from 'react';
import { getShapeIconPath } from './sectionShapes';

/** Miniatura SVG de una forma del catálogo, dibujada con sus propios vértices. */
export default function ShapeIcon({ id, color = '#7C3AED', size = 28 }) {
  const d = useMemo(() => getShapeIconPath(id), [id]);
  return (
    <svg viewBox="0 0 40 40" width={size} height={size} xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <path d={d} stroke={color} strokeWidth="2" fill={color} fillOpacity="0.15" strokeLinejoin="round" />
    </svg>
  );
}

/**
 * Cuadrícula de formas del catálogo. Marca `selectedId` y llama a `onSelect(id)`.
 */
export function ShapeGrid({ shapes, selectedId, onSelect, color, columns = 4 }) {
  return (
    <div className="grid gap-1.5" style={{ gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))` }}>
      {shapes.map((shape) => {
        const active = shape.id === selectedId;
        return (
          <button
            key={shape.id}
            type="button"
            title={shape.label}
            aria-pressed={active}
            onClick={() => onSelect(shape.id)}
            className={[
              'flex flex-col items-center justify-center gap-1 rounded-md p-1.5 border transition-colors',
              active
                ? 'bg-accent/20 border-accent/60'
                : 'bg-surfaceAlt border-transparent hover:bg-accent/10 hover:border-accent/30',
            ].join(' ')}
          >
            <ShapeIcon id={shape.id} color={color} />
            <span className="text-[9px] text-textMuted leading-tight text-center line-clamp-2">{shape.label}</span>
          </button>
        );
      })}
    </div>
  );
}
