import { useState, useRef, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { ChevronDown } from 'lucide-react';

/**
 * StatusDropdown — botón + panel flotante vía createPortal.
 * El panel se renderiza en document.body para escapar cualquier
 * overflow-hidden ancestral (p.ej. el contenedor rounded de la tabla).
 */
export default function StatusDropdown({
  event,
  badge,
  busy,
  isOpen,
  onToggle,
  onSelect,
  statusOptions,
  statusLabel,
}) {
  const buttonRef = useRef(null);
  const menuRef   = useRef(null);
  const [coords, setCoords] = useState({ top: 0, left: 0 });

  // Calcular posición cuando se abre
  useEffect(() => {
    if (isOpen && buttonRef.current) {
      const rect = buttonRef.current.getBoundingClientRect();
      setCoords({
        top:  rect.bottom + window.scrollY + 4,
        left: rect.left   + window.scrollX,
      });
    }
  }, [isOpen]);

  // Reposicionar en scroll/resize mientras está abierto
  useEffect(() => {
    if (!isOpen) return;
    const reposition = () => {
      if (buttonRef.current) {
        const rect = buttonRef.current.getBoundingClientRect();
        setCoords({
          top:  rect.bottom + window.scrollY + 4,
          left: rect.left   + window.scrollX,
        });
      }
    };
    window.addEventListener('scroll', reposition, true);
    window.addEventListener('resize', reposition);
    return () => {
      window.removeEventListener('scroll', reposition, true);
      window.removeEventListener('resize', reposition);
    };
  }, [isOpen]);

  // Cerrar al hacer click fuera (botón O menú)
  useEffect(() => {
    if (!isOpen) return;
    const handleClickOutside = (e) => {
      if (
        buttonRef.current && !buttonRef.current.contains(e.target) &&
        menuRef.current   && !menuRef.current.contains(e.target)
      ) {
        onToggle(null);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen, onToggle]);

  return (
    <>
      <button
        ref={buttonRef}
        disabled={busy}
        onClick={() => onToggle(isOpen ? null : event.eventId)}
        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold border transition-colors
                    ${badge.cls} ${busy ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer hover:opacity-80'}`}
      >
        {badge.label}
        {!busy && <ChevronDown className="w-3 h-3" />}
        {busy && (
          <span className="w-3 h-3 inline-block animate-spin border border-current border-t-transparent rounded-full" />
        )}
      </button>

      {isOpen && createPortal(
        <div
          ref={menuRef}
          style={{ position: 'absolute', top: coords.top, left: coords.left, zIndex: 9999 }}
          className="bg-surface border border-surfaceAlt rounded-xl shadow-2xl shadow-primary/20 overflow-hidden min-w-[160px]"
        >
          {statusOptions.map((s) => (
            <button
              key={s}
              onClick={() => onSelect(event, s)}
              className={`w-full text-left px-3 py-2 text-xs transition-colors hover:bg-surfaceAlt
                          ${event.status === s ? 'font-bold text-accent' : 'text-textSecondary'}`}
            >
              {statusLabel[s] ?? s}
            </button>
          ))}
        </div>,
        document.body
      )}
    </>
  );
}
