import { useEffect, useRef } from 'react';
import { X } from 'lucide-react';

/**
 * Modal — componente genérico reutilizable.
 *
 * Props:
 *   isOpen    — boolean: controla si el modal está visible
 *   onClose   — () => void: llamado al cerrar (Escape, overlay, botón X)
 *   title     — string | ReactNode: título en el header
 *   maxWidth  — string: clase Tailwind max-w-* (default: 'max-w-lg')
 *   children  — contenido del cuerpo
 *   footer    — ReactNode: botones/acciones del footer (opcional)
 *   hideClose — boolean: oculta el botón X del header (default: false)
 *
 * Comportamiento:
 *   - Cierre por Escape
 *   - Cierre por click en el overlay (fuera del panel)
 *   - Focus trap: al abrir, el panel recibe foco para que Escape funcione
 *   - Scroll bloqueado en body mientras está abierto
 *   - Accesible: role="dialog", aria-modal, aria-labelledby
 */
export default function Modal({
  isOpen,
  onClose,
  title,
  maxWidth = 'max-w-lg',
  children,
  footer,
  hideClose = false,
}) {
  const panelRef = useRef(null);

  // Efecto 1 — bloqueo de scroll + handler de Escape (depende de isOpen y onClose)
  useEffect(() => {
    if (!isOpen) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const handler = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handler);

    return () => {
      window.removeEventListener('keydown', handler);
      document.body.style.overflow = prev;
    };
  }, [isOpen, onClose]);

  // Efecto 2 — focus al panel solo cuando el modal se abre (no en cada render)
  // Separado del efecto de Escape para que onClose no reinicie el focus en cada tecla.
  useEffect(() => {
    if (!isOpen) return;
    // Pequeño timeout para que el DOM esté pintado antes de hacer focus
    const t = setTimeout(() => panelRef.current?.focus(), 0);
    return () => clearTimeout(t);
  }, [isOpen]); // ← solo cuando isOpen cambia, NO cuando cambia onClose

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4"
      onClick={onClose}
      role="presentation"
    >
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        tabIndex={-1}
        onClick={(e) => e.stopPropagation()}
        className={`bg-surface border border-surfaceAlt rounded-2xl w-full ${maxWidth} shadow-2xl
                    flex flex-col max-h-[90vh] outline-none`}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-surfaceAlt flex-shrink-0">
          <h3
            id="modal-title"
            className="text-sm font-bold text-textPrimary"
          >
            {title}
          </h3>
          {!hideClose && (
            <button
              onClick={onClose}
              className="text-textMuted hover:text-textPrimary transition-colors p-1 rounded hover:bg-surfaceAlt"
              aria-label="Cerrar"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* Body — scrollable */}
        <div className="flex-1 overflow-y-auto">
          {children}
        </div>

        {/* Footer — solo si se pasa */}
        {footer && (
          <div className="px-5 py-4 border-t border-surfaceAlt flex justify-end gap-2 flex-shrink-0">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
}
