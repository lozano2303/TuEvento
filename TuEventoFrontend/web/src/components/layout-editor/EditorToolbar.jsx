import { useState } from 'react';
import { Code2, Trash2, Save, X } from 'lucide-react';
import { serializeLayout } from './layoutEditorUtils';

/**
 * EditorToolbar
 *
 * Props:
 *   elements      — array de elementos del canvas
 *   canvasSize    — { width, height }
 *   onClear       — callback para limpiar el canvas
 *   canUndo/canRedo / onUndo/onRedo — undo-redo
 *   onOpenHelp    — abre el HelpModal
 *   onSave        — async callback de guardado real (recibe nada, lanzado desde aquí)
 *   isSaving      — boolean, bloqueado mientras guarda
 *   maxSeats      — number | null — event.availableSeats (null en modo demo)
 */
export default function EditorToolbar({
  elements,
  canvasSize,
  onClear,
  canUndo,
  canRedo,
  onUndo,
  onRedo,
  onOpenHelp,
  onSave,
  isSaving = false,
  maxSeats = null,
}) {
  const [showJson, setShowJson] = useState(false);

  // Contador de sillas totales (solo secciones)
  const totalSeats = elements
    .filter((el) => el.type === 'section')
    .reduce((sum, el) => sum + (el.seatLayout?.targetSeats ?? 0), 0);

  const isOverCapacity = maxSeats != null && totalSeats > maxSeats;

  const layoutJson = JSON.stringify(
    serializeLayout(elements, canvasSize?.width ?? 1200, canvasSize?.height ?? 800),
    null, 2
  );

  const handleClear = () => {
    if (window.confirm('¿Limpiar el canvas? Se eliminarán todos los elementos.')) {
      onClear();
    }
  };

  const btnBase =
    'flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors';

  return (
    <>
      <div className="h-11 flex-shrink-0 bg-surface border-b border-surfaceAlt flex items-center px-3 gap-2">

        {/* Elementos count */}
        <span className="text-textMuted text-xs">
          {elements.length} elemento{elements.length !== 1 ? 's' : ''}
        </span>

        <div className="w-px h-5 bg-surfaceAlt" />

        {/* Contador de sillas vs. aforo del evento */}
        <span
          className={`text-xs font-mono px-2 py-1 rounded ${
            isOverCapacity ? 'text-red-400 bg-red-500/10' : 'text-textMuted'
          }`}
          title={isOverCapacity ? 'Excede el aforo del evento' : 'Sillas en el layout'}
        >
          {totalSeats}
          {maxSeats != null && ` / ${maxSeats}`} sillas
        </span>

        {isOverCapacity && (
          <span className="text-[10px] font-semibold text-red-400 border border-red-400/30 px-2 py-0.5 rounded-full">
            Excede aforo
          </span>
        )}

        <div className="w-px h-5 bg-surfaceAlt" />

        {/* Undo / Redo / Ayuda */}
        <div className="flex items-center gap-0.5">
          <button
            onClick={onUndo}
            disabled={!canUndo}
            title="Deshacer (Ctrl+Z)"
            className="flex items-center gap-1 px-2 py-1.5 rounded-lg text-xs font-medium transition-colors
                       text-textSecondary hover:text-textPrimary hover:bg-surfaceAlt
                       disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-transparent disabled:hover:text-textSecondary"
          >
            <span className="text-base leading-none">↩</span>
            <span className="hidden sm:inline">Deshacer</span>
          </button>
          <button
            onClick={onRedo}
            disabled={!canRedo}
            title="Rehacer (Ctrl+Shift+Z)"
            className="flex items-center gap-1 px-2 py-1.5 rounded-lg text-xs font-medium transition-colors
                       text-textSecondary hover:text-textPrimary hover:bg-surfaceAlt
                       disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-transparent disabled:hover:text-textSecondary"
          >
            <span className="text-base leading-none">↪</span>
            <span className="hidden sm:inline">Rehacer</span>
          </button>
          <button
            onClick={onOpenHelp}
            className="flex items-center gap-1 px-2 py-1.5 rounded-lg text-xs font-medium transition-colors
                       text-textSecondary hover:text-textPrimary hover:bg-surfaceAlt flex-shrink-0"
            title="Ayuda"
            aria-label="Abrir ayuda"
          >
            <span className="text-sm leading-none">?</span>
            <span className="hidden sm:inline">Cómo usar el editor</span>
          </button>
        </div>

        <div className="flex-1" />

        {/* Acciones */}
        <button
          onClick={() => setShowJson(true)}
          className={`${btnBase} bg-surfaceAlt text-textSecondary hover:text-textPrimary hover:bg-surfaceAlt`}
          title="Ver JSON del layout"
        >
          <Code2 className="w-3.5 h-3.5" />
          Ver JSON
        </button>

        <button
          onClick={handleClear}
          className={`${btnBase} bg-error/10 text-error border border-error/20 hover:bg-error/20`}
          title="Limpiar canvas"
        >
          <Trash2 className="w-3.5 h-3.5" />
          Limpiar
        </button>

        <button
          onClick={onSave}
          disabled={isSaving || isOverCapacity}
          className={`${btnBase} bg-primary text-white hover:bg-primaryDark
                      disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary`}
          title={
            isOverCapacity
              ? 'Excede el aforo del evento — ajusta las sillas antes de guardar'
              : isSaving
              ? 'Guardando…'
              : 'Guardar y sincronizar'
          }
        >
          <Save className="w-3.5 h-3.5" />
          {isSaving ? 'Guardando…' : 'Guardar'}
        </button>
      </div>

      {/* Modal JSON */}
      {showJson && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
          <div className="bg-surface border border-surfaceAlt rounded-2xl w-full max-w-2xl max-h-[80vh] flex flex-col shadow-2xl">
            <div className="flex items-center justify-between px-4 py-3 border-b border-surfaceAlt">
              <h3 className="text-sm font-bold text-textPrimary">Layout JSON</h3>
              <button
                onClick={() => setShowJson(false)}
                className="text-textMuted hover:text-textPrimary transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <pre className="flex-1 overflow-auto p-4 text-[11px] text-accent font-mono leading-relaxed">
              {layoutJson}
            </pre>
            <div className="px-4 py-3 border-t border-surfaceAlt flex justify-end gap-2">
              <button
                onClick={() => { navigator.clipboard.writeText(layoutJson); }}
                className="text-xs px-3 py-1.5 rounded-lg bg-surfaceAlt text-textSecondary hover:text-textPrimary transition-colors"
              >
                Copiar
              </button>
              <button
                onClick={() => setShowJson(false)}
                className="text-xs px-3 py-1.5 rounded-lg bg-primary text-white hover:bg-primaryDark transition-colors"
              >
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
