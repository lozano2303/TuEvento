import { useState, useEffect } from 'react';

const TABS = ['Navegación', 'Secciones', 'Formas', 'Vértices y curvas', 'Atajos'];

// ── Contenido de cada tab ─────────────────────────────────────────────────────

function TabNavegacion() {
  const items = [
    ['Zoom', 'Rueda del mouse centrada en el cursor, o botones +/− / ⊡ en la barra.'],
    ['Pan', 'Click derecho + arrastrar desplaza el canvas.'],
    ['Seleccionar', 'Click izquierdo sobre un elemento.'],
    ['Multi-seleccionar', 'Ctrl + click sobre varios elementos para seleccionarlos individualmente.'],
    ['Rubber-band', 'Click izquierdo + arrastrar sobre el fondo selecciona todos los elementos dentro del área.'],
    ['Mover grupo', 'Con varios elementos seleccionados, arrastrar uno mueve todo el grupo.'],
  ];
  return <HelpList items={items} />;
}

function TabSecciones() {
  const items = [
    ['Crear', 'Arrastra desde la paleta izquierda al canvas. También puedes hacer clic en el ítem.'],
    ['Tipos únicos', 'Cada tipo de sección (VIP, General…) es único. Una vez creado queda bloqueado en la paleta con badge "En uso".'],
    ['Copiar / Pegar', 'Ctrl+C sobre una sección la copia. Ctrl+V crea una subsección con el mismo color e id lógico, posicionada 20 px más abajo.'],
    ['Cambiar color', 'Cambiar el color en el panel de propiedades lo propaga a todas las subsecciones del mismo tipo.'],
    ['Capacidad', 'Escribe cuántas sillas quieres. El editor calcula cuántas caben físicamente y muestra el badge verde/amarillo/rojo.'],
    ['Botón "Máx"', 'Establece automáticamente el máximo de sillas posibles según el tamaño y forma actual de la sección.'],
  ];
  return <HelpList items={items} />;
}

function TabFormas() {
  const items = [
    ['Forma por defecto', 'Las secciones se crean como rectángulos.'],
    ['Convertir a forma libre', 'El botón "✦ Convertir a forma libre" en propiedades activa el modo polígono editable.'],
    ['Formas sugeridas', 'Aparecen en modo edición de vértices. Un clic aplica la forma al tamaño actual de la sección (rectángulo, círculo, semicírculo, trapecio, triángulo, hexágono, L).'],
    ['Escalado automático', 'La forma siempre se escala al bounding box actual — cambia el tamaño de la sección y la forma se adapta.'],
    ['Volver a rectángulo', 'El botón "↩ Volver a rectángulo" revierte la sección a rect con confirmación.'],
  ];
  return <HelpList items={items} />;
}

function TabVertices() {
  const items = [
    ['Entrar al modo edición', 'Doble clic en una sección poligonal, o el botón "✏ Editar forma".'],
    ['Mover vértice', 'Arrastrar el punto sólido.'],
    ['Insertar vértice', 'Clic en el punto medio semitransparente de un segmento.'],
    ['Eliminar vértice', 'Clic derecho sobre un vértice (mínimo 3 vértices).'],
    ['Descartar cambios', 'Escape descarta toda la sesión de edición y revierte.'],
    ['Confirmar cambios', 'Enter o botón "✓ Guardar forma".'],
    ['Convertir a curva Bézier', 'Alt + arrastrar el punto medio de un segmento.'],
    ['Ajustar curva', 'Arrastrar el handle (círculo hueco) que aparece en los extremos del segmento curvo.'],
    ['Simétrico / Independiente', 'Clic derecho en un handle alterna entre modos: simétrico da curva suave, independiente permite esquina con curva.'],
    ['Eliminar curva', 'Shift + clic derecho en un handle elimina la curva de ese segmento.'],
  ];
  return <HelpList items={items} />;
}

function TabAtajos() {
  const rows = [
    ['Ctrl + Z',          'Deshacer'],
    ['Ctrl + Shift + Z',  'Rehacer'],
    ['Ctrl + Y',          'Rehacer (alternativo)'],
    ['Ctrl + C',          'Copiar sección seleccionada'],
    ['Ctrl + V',          'Pegar como subsección'],
    ['Delete',            'Eliminar elemento(s) seleccionado(s)'],
    ['Escape',            'Deseleccionar todo / descartar edición de vértices'],
    ['Enter',             'Confirmar edición de vértices'],
    ['Alt + arrastrar',   'Convertir segmento a curva Bézier'],
    ['Clic derecho',      'Eliminar vértice / alternar simetría de handle'],
    ['Shift + clic der.', 'Eliminar curva del segmento'],
  ];
  return (
    <table className="w-full text-xs border-collapse">
      <thead>
        <tr className="border-b border-surfaceAlt">
          <th className="text-left py-1.5 pr-4 text-textMuted font-semibold uppercase tracking-wider text-[10px]">Atajo</th>
          <th className="text-left py-1.5 text-textMuted font-semibold uppercase tracking-wider text-[10px]">Acción</th>
        </tr>
      </thead>
      <tbody>
        {rows.map(([shortcut, action]) => (
          <tr key={shortcut} className="border-b border-surfaceAlt/50 hover:bg-surfaceAlt/40 transition-colors">
            <td className="py-2 pr-4 font-mono text-accent whitespace-nowrap">{shortcut}</td>
            <td className="py-2 text-textSecondary">{action}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

// ── Componente de lista reutilizable ─────────────────────────────────────────
function HelpList({ items }) {
  return (
    <ul className="space-y-3">
      {items.map(([title, desc]) => (
        <li key={title} className="flex gap-3">
          <span
            className="mt-0.5 w-1.5 h-1.5 rounded-full flex-shrink-0"
            style={{ background: 'var(--color-accent)', marginTop: '5px' }}
          />
          <div>
            <span className="text-xs font-semibold text-textPrimary">{title}: </span>
            <span className="text-xs text-textSecondary">{desc}</span>
          </div>
        </li>
      ))}
    </ul>
  );
}

// ── Modal principal ───────────────────────────────────────────────────────────
const TAB_CONTENT = [TabNavegacion, TabSecciones, TabFormas, TabVertices, TabAtajos];

export default function HelpModal({ isOpen, onClose }) {
  const [activeTab, setActiveTab] = useState(0);

  // Cerrar con Escape
  useEffect(() => {
    if (!isOpen) return;
    const handler = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const Content = TAB_CONTENT[activeTab];

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 overflow-hidden"
      style={{ background: 'rgba(0,0,0,0.45)' }}
      onClick={onClose}
    >
      <div
        className="modal-elevated relative w-full bg-surface border border-surfaceAlt rounded-2xl flex flex-col overflow-hidden"
        style={{ maxWidth: '600px', maxHeight: '90vh' }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-surfaceAlt flex-shrink-0">
          <h2 className="text-sm font-bold text-textPrimary">¿Cómo usar el editor?</h2>
          <button
            onClick={onClose}
            className="text-textMuted hover:text-textPrimary transition-colors p-1 rounded hover:bg-surfaceAlt"
            aria-label="Cerrar"
          >
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M1 1l12 12M13 1L1 13" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
            </svg>
          </button>
        </div>

        {/* Tabs */}
        <div className="flex border-b border-surfaceAlt flex-shrink-0 overflow-x-auto
                        [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]">
          {TABS.map((tab, i) => (
            <button
              key={tab}
              onClick={() => setActiveTab(i)}
              className={[
                'px-4 py-2.5 text-xs font-medium whitespace-nowrap transition-colors border-b-2 -mb-px',
                activeTab === i
                  ? 'border-accent text-accent'
                  : 'border-transparent text-textMuted hover:text-textSecondary hover:bg-surfaceAlt/50',
              ].join(' ')}
            >
              {tab}
            </button>
          ))}
        </div>
        {/* Content */}
        <div className="flex-1 overflow-y-auto px-5 py-3 [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]">
        <Content />
        </div>
      </div>
    </div>
  );
}
