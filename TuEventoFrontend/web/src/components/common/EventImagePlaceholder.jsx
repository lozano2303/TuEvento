import { Calendar } from 'lucide-react';

/**
 * EventImagePlaceholder — placeholder neutro para eventos sin imagen de portada.
 *
 * Props:
 *   size    — 'sm' | 'md' | 'lg'
 *             sm: thumbnail de tabla (40×40px) — solo ícono, sin texto
 *             md: tarjeta pública (h-48)       — ícono + texto opcional
 *             lg: modal de detalle (h-52)       — ícono más grande + texto
 *   label   — string opcional (por defecto "Sin imagen de portada")
 *   className — clases extra para el contenedor
 *
 * Usa colores fijos del tema oscuro (#1a0d28 / #7f13ec) en el admin,
 * y CSS custom properties (--color-primary) donde se aplica el sistema de temas.
 * El prop `themed` activa las variables CSS en lugar de los colores hardcodeados.
 */
export default function EventImagePlaceholder({
  size = 'md',
  label,
  className = '',
  themed = false,
}) {
  const iconSizes = { sm: 'w-4 h-4', md: 'w-7 h-7', lg: 'w-10 h-10' };
  const showLabel  = size !== 'sm';
  const defaultLabel = label ?? (size === 'lg' ? 'Sin imagen de portada' : 'Sin imagen');

  const bgStyle = themed
    ? {
        background: 'linear-gradient(135deg, color-mix(in srgb, var(--color-primary) 18%, transparent) 0%, color-mix(in srgb, var(--color-primaryDark) 28%, transparent) 100%)',
        borderColor: 'color-mix(in srgb, var(--color-primary) 20%, transparent)',
      }
    : {};

  const bgClass = themed
    ? 'border'
    : 'bg-gradient-to-br from-[#2a1040] to-[#1a0d28]';

  const iconColor = themed ? 'text-primary opacity-50' : 'text-[#7f13ec] opacity-60';
  const textColor = themed ? 'text-textMuted opacity-50' : 'text-slate-500';

  return (
    <div
      className={`w-full h-full flex flex-col items-center justify-center gap-1.5 ${bgClass} ${className}`}
      style={themed ? bgStyle : {}}
      aria-label={defaultLabel}
      role="img"
    >
      <Calendar className={`${iconSizes[size]} ${iconColor}`} strokeWidth={1.5} />
      {showLabel && (
        <span className={`text-[10px] font-medium tracking-wide ${textColor} leading-tight text-center px-2`}>
          {defaultLabel}
        </span>
      )}
    </div>
  );
}
