/**
 * BaseModal.jsx — sistema de modal reutilizable extraído del WelcomeModal.
 *
 * Estructura visual:
 *   Overlay oscuro
 *   └─ Card con glow exterior primary
 *        ├─ Header  → gradiente primary→primaryDark + círculos decorativos (via CSS)
 *        │             ícono de estado (ring blanco) + título + subtítulo
 *        │             variante "compact" para modales sin ícono central (ej. Términos)
 *        ├─ Body    → slot libre (children) con fondo surface
 *        └─ Footer  → botones bm-btn-primary / bm-btn-secondary
 *
 * Props:
 *   isOpen      boolean              — controla visibilidad
 *   onClose     () => void           — cierre por overlay, Escape o botón X
 *   variant     'success' | 'warning' | 'danger' | 'info' | 'neutral'
 *                                    — color del ícono de estado (semántico)
 *   icon        ReactNode            — ícono JSX que se coloca dentro del ring blanco
 *                                      (si null, se omite el ring)
 *   title       string               — título bold del header
 *   subtitle    string               — subtítulo atenuado del header
 *   children    ReactNode            — contenido libre del body
 *   actions     Array<ActionDef>     — botones del footer (ver shape abajo)
 *   maxWidth    string               — clase CSS max-w-* de Tailwind (default 'max-w-md')
 *   compact     boolean              — header compacto sin ícono central (para Términos,
 *                                      confirmaciones simples, etc.)
 *   hideOverlayClose boolean         — desactiva el cierre al hacer click en el overlay
 *   scrollableBody boolean           — añade max-h + scroll al body (para contenido largo)
 *   decorIcons  ReactNode            — íconos festivos opcionales (PartyPopper, Sparkles…)
 *                                      se renderizan dentro del header con position:relative
 *
 * ActionDef shape:
 *   { label: string, onClick: () => void, variant?: 'primary' | 'secondary',
 *     icon?: ReactNode, disabled?: boolean, loading?: boolean, loadingLabel?: string }
 *
 * Comportamiento:
 *   - Cierre por Escape
 *   - Cierre por click en overlay (salvo hideOverlayClose=true)
 *   - Bloqueo de scroll en body mientras está abierto
 *   - Focus trap básico: el card recibe foco al abrir
 *   - role="dialog", aria-modal, aria-labelledby
 */

import { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';

// ── Mapa de clases de color por variant ──────────────────────────────────────
const ICON_COLOR_CLASS = {
  success: 'bm-icon-success',
  warning: 'bm-icon-warning',
  danger:  'bm-icon-danger',
  info:    'bm-icon-info',
  neutral: 'bm-icon-neutral',
};

export default function BaseModal({
  isOpen,
  onClose,
  variant       = 'success',
  icon          = null,
  title         = '',
  subtitle      = '',
  children,
  actions       = [],
  maxWidth      = 'max-w-[420px]',
  compact       = false,
  hideOverlayClose = false,
  scrollableBody   = false,
  decorIcons    = null,
}) {
  const cardRef = useRef(null);
  const titleId = useRef(`bm-title-${Math.random().toString(36).slice(2)}`).current;

  // Cierre por Escape + bloqueo de scroll
  useEffect(() => {
    if (!isOpen) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const handler = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handler);
    cardRef.current?.focus();

    return () => {
      window.removeEventListener('keydown', handler);
      document.body.style.overflow = prev;
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleOverlayClick = () => { if (!hideOverlayClose) onClose(); };

  const iconColorClass = ICON_COLOR_CLASS[variant] ?? 'bm-icon-neutral';

  const modal = (
    /* El overlay usa estilos inline además de la clase CSS para garantizar
       que fixed+inset+flex funcionen incluso si el CSS de bm-overlay no se
       ha aplicado todavía al momento del primer render del portal.
       La clase bm-overlay sigue presente para el backdrop-color y z-index. */
    <div
      className="bm-overlay"
      style={{
        position: 'fixed',
        inset: 0,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '1rem',
        zIndex: 9999,
        backgroundColor: 'rgba(0, 0, 0, 0.92)',
        backdropFilter: 'blur(20px)',
        WebkitBackdropFilter: 'blur(20px)',
      }}
      onClick={handleOverlayClick}
      role="presentation"
    >
      <div
        ref={cardRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        onClick={(e) => e.stopPropagation()}
        className={`bm-card w-full ${maxWidth} outline-none`}
      >
        {/* ── Header ──────────────────────────────────────────────────────── */}
        {compact ? (
          /* Variante compacta: título a la izquierda + botón X a la derecha */
          <div className="bm-header bm-header--compact">
            <h2 id={titleId} className="bm-title">{title}</h2>
            {onClose && (
              <button
                onClick={onClose}
                className="bm-close-btn"
                aria-label="Cerrar"
              >
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                  <path d="M2 2l12 12M14 2L2 14" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                </svg>
              </button>
            )}
          </div>
        ) : (
          /* Variante estándar: ícono centrado + título + subtítulo */
          <div className="bm-header">
            {/* decorIcons necesita position:relative + z-index para quedar
                sobre los pseudo-elementos ::before/::after del header */}
            {decorIcons && (
              <div style={{ position: 'relative', zIndex: 1 }}>
                {decorIcons}
              </div>
            )}

            {icon && (
              <div className="flex justify-center mb-3">
                <div className={`bm-icon-ring ${iconColorClass}`}>
                  {icon}
                </div>
              </div>
            )}

            <h2 id={titleId} className="bm-title">{title}</h2>
            {subtitle && <p className="bm-subtitle">{subtitle}</p>}
          </div>
        )}

        {/* ── Body ────────────────────────────────────────────────────────── */}
        <div className={`bm-body${scrollableBody ? ' bm-body--scroll' : ''}`}>
          {children}
        </div>

        {/* ── Footer ──────────────────────────────────────────────────────── */}
        {actions.length > 0 && (
          <div className="bm-footer">
            {actions.map((action, i) => {
              const isPrimary = (action.variant ?? (i === 0 ? 'primary' : 'secondary')) === 'primary';
              // Destructive modals (danger variant) use red button instead of primary gradient
              const btnClass = isPrimary
                ? (variant === 'danger' ? 'bm-btn-danger' : 'bm-btn-primary')
                : 'bm-btn-secondary';
              return (
                <button
                  key={i}
                  onClick={action.onClick}
                  disabled={action.disabled || action.loading}
                  className={btnClass}
                >
                  {action.loading
                    ? (action.loadingLabel ?? action.label)
                    : (
                      <>
                        {action.icon}
                        {action.label}
                      </>
                    )
                  }
                </button>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );

  // Portal: monta el modal directo en document.body para escapar de cualquier
  // stacking context del árbol padre (position:fixed, transform, filter, etc.).
  // Esto previene el bug donde bm-overlay queda confinado al contenedor padre
  // en lugar de cubrir el viewport completo.
  return createPortal(modal, document.body);
}
