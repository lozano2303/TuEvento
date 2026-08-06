import { useState, useRef, useEffect } from 'react';

/**
 * Tooltip — componente genérico reutilizable.
 *
 * Props:
 *   content  — string | ReactNode: texto o JSX que se muestra en el tooltip
 *   children — ReactNode: el trigger (icono, botón, etc.)
 *   position — 'top' | 'bottom' | 'left' | 'right' (default: 'top')
 *
 * Comportamiento:
 *   - Desktop: muestra en hover (onMouseEnter/Leave)
 *   - Touch / click: toggle al hacer clic en el trigger; cierra al hacer clic fuera
 *   - Accesible: el trigger recibe role="button" y aria-describedby
 *
 * Estilizado con variables CSS del tema activo:
 *   background: var(--color-surfaceAlt)
 *   color:      var(--color-textPrimary)
 *   border:     var(--color-surfaceAlt)
 */
export default function Tooltip({ content, children, position = 'top' }) {
  const [visible, setVisible] = useState(false);
  const containerRef = useRef(null);
  const tooltipId = useRef(`tooltip-${Math.random().toString(36).slice(2, 8)}`);

  // Cerrar al hacer clic fuera del contenedor
  useEffect(() => {
    if (!visible) return;
    const handler = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setVisible(false);
      }
    };
    document.addEventListener('mousedown', handler);
    document.addEventListener('touchstart', handler);
    return () => {
      document.removeEventListener('mousedown', handler);
      document.removeEventListener('touchstart', handler);
    };
  }, [visible]);

  const positionStyles = {
    top:    { bottom: 'calc(100% + 8px)', left: '50%', transform: 'translateX(-50%)' },
    bottom: { top:    'calc(100% + 8px)', left: '50%', transform: 'translateX(-50%)' },
    left:   { right:  'calc(100% + 8px)', top:  '50%', transform: 'translateY(-50%)' },
    right:  { left:   'calc(100% + 8px)', top:  '50%', transform: 'translateY(-50%)' },
  };

  return (
    <div
      ref={containerRef}
      className="relative inline-flex items-center"
      onMouseEnter={() => setVisible(true)}
      onMouseLeave={() => setVisible(false)}
      onClick={() => setVisible((v) => !v)}
    >
      {/* Trigger */}
      <span aria-describedby={visible ? tooltipId.current : undefined}>
        {children}
      </span>

      {/* Panel */}
      {visible && (
        <div
          id={tooltipId.current}
          role="tooltip"
          style={{
            position: 'absolute',
            zIndex: 50,
            minWidth: '180px',
            maxWidth: '240px',
            padding: '8px 10px',
            borderRadius: 'var(--borderRadius, 8px)',
            background: 'var(--color-surfaceAlt)',
            color: 'var(--color-textPrimary)',
            border: '1px solid rgba(255,255,255,0.08)',
            boxShadow: '0 4px 16px rgba(0,0,0,0.35)',
            fontSize: '11px',
            lineHeight: '1.5',
            pointerEvents: 'none',
            whiteSpace: 'normal',
            wordBreak: 'break-word',
            ...positionStyles[position],
          }}
        >
          {content}
          {/* Flecha apuntando al trigger */}
          <span
            style={{
              position: 'absolute',
              ...(position === 'top'    && { bottom: '-5px', left: '50%', transform: 'translateX(-50%) rotate(45deg)' }),
              ...(position === 'bottom' && { top:    '-5px', left: '50%', transform: 'translateX(-50%) rotate(45deg)' }),
              ...(position === 'left'   && { right:  '-5px', top:  '50%', transform: 'translateY(-50%) rotate(45deg)' }),
              ...(position === 'right'  && { left:   '-5px', top:  '50%', transform: 'translateY(-50%) rotate(45deg)' }),
              width: '8px',
              height: '8px',
              background: 'var(--color-surfaceAlt)',
              border: '1px solid rgba(255,255,255,0.08)',
              clipPath: position === 'top'    ? 'polygon(100% 0, 100% 100%, 0 100%)' :
                        position === 'bottom' ? 'polygon(0 0, 100% 0, 0 100%)' :
                        position === 'left'   ? 'polygon(100% 0, 100% 100%, 0 0)' :
                                               'polygon(0 0, 100% 100%, 0 100%)',
            }}
          />
        </div>
      )}
    </div>
  );
}
