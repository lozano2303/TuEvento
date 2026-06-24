import { useState } from 'react';
import { ZoomIn, ZoomOut, RotateCcw, Code2, Trash2, Save, X } from 'lucide-react';
import { serializeLayout } from './layoutEditorUtils';

export default function EditorToolbar({ elements, canvasSize, zoom, onZoomIn, onZoomOut, onResetZoom, onClear, canUndo, canRedo, onUndo, onRedo }) {
  const [showJson, setShowJson] = useState(false);
  const [saveMsg, setSaveMsg] = useState(null);

  const layoutJson = JSON.stringify(
    serializeLayout(elements, canvasSize?.width ?? 1200, canvasSize?.height ?? 800),
    null, 2
  );

  const handleSave = () => {
    console.log('[Layout Editor] layoutData:', serializeLayout(elements, canvasSize?.width, canvasSize?.height));
    setSaveMsg('Guardado simulado — Fase 2 conectará esto al backend');
    setTimeout(() => setSaveMsg(null), 3000);
  };

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
        {/* Zoom */}
        <div className="flex items-center gap-1 bg-background rounded-lg px-2 py-1 border border-surfaceAlt">
          <button
            onClick={onZoomOut}
            className="text-textSecondary hover:text-textPrimary transition-colors"
            title="Alejar"
          >
            <ZoomOut className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={onResetZoom}
            className="text-textSecondary hover:text-accent transition-colors text-xs w-10 text-center font-mono"
            title="Resetear zoom"
          >
            {Math.round(zoom * 100)}%
          </button>
          <button
            onClick={onZoomIn}
            className="text-textSecondary hover:text-textPrimary transition-colors"
            title="Acercar"
          >
            <ZoomIn className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="w-px h-5 bg-surfaceAlt" />

        {/* Elementos count */}
        <span className="text-textMuted text-xs">
          {elements.length} elemento{elements.length !== 1 ? 's' : ''}
        </span>

        <div className="w-px h-5 bg-surfaceAlt" />

        {/* Undo / Redo */}
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
          onClick={handleSave}
          className={`${btnBase} bg-primary text-white hover:bg-primaryDark`}
          title="Guardar (simulado en Fase 1)"
        >
          <Save className="w-3.5 h-3.5" />
          Guardar
        </button>
      </div>

      {/* Toast de guardado simulado */}
      {saveMsg && (
        <div className="fixed bottom-4 left-1/2 -translate-x-1/2 z-50 bg-surface border border-accent/40
                        text-textPrimary text-xs px-4 py-2.5 rounded-xl shadow-lg shadow-primary/20
                        flex items-center gap-2 max-w-sm text-center">
          <span>💾</span>
          <span>{saveMsg}</span>
        </div>
      )}

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
