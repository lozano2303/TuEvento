import { Loader2 } from 'lucide-react';

/**
 * ConfirmModal — diálogo de confirmación reutilizable, estilizado con el tema
 * oscuro/morado de la app. Reemplaza window.confirm() para acciones importantes.
 *
 * Props:
 *   isOpen        boolean   — controla la visibilidad
 *   title         string    — título del diálogo (ej. "¿Publicar evento?")
 *   message       string|ReactNode — cuerpo descriptivo
 *   confirmLabel  string    — texto del botón de confirmar (default: "Confirmar")
 *   cancelLabel   string    — texto del botón de cancelar  (default: "Cancelar")
 *   confirmStyle  'primary' | 'danger' | 'warning'  (default: 'primary')
 *   loading       boolean   — muestra spinner en el botón confirmar
 *   onConfirm     () => void
 *   onCancel      () => void
 */
export default function ConfirmModal({
  isOpen,
  title,
  message,
  confirmLabel = 'Confirmar',
  cancelLabel  = 'Cancelar',
  confirmStyle = 'primary',
  loading      = false,
  onConfirm,
  onCancel,
}) {
  if (!isOpen) return null;

  // ── Estilos del botón de confirmación según la acción ─────────────────────
  const confirmBtnClass = {
    primary: [
      'bg-gradient-to-r from-[#7f13ec] to-[#3b82f6] text-white border-transparent',
      'hover:brightness-110 shadow-lg shadow-[#7f13ec]/30',
    ].join(' '),
    danger: [
      'bg-rose-500/10 text-rose-400 border border-rose-500/20',
      'hover:bg-rose-500 hover:text-white hover:border-rose-500',
    ].join(' '),
    warning: [
      'bg-amber-500/10 text-amber-400 border border-amber-500/20',
      'hover:bg-amber-500 hover:text-white hover:border-amber-500',
    ].join(' '),
  }[confirmStyle] ?? '';

  return (
    <div
      className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-[80] p-4"
      onClick={(e) => { if (e.target === e.currentTarget) onCancel?.(); }}
    >
      <div className="bg-[#1a0d28] rounded-2xl border border-white/10 shadow-2xl shadow-black/60 w-full max-w-sm p-6 flex flex-col gap-5">

        {/* Título */}
        <h3 className="text-base font-black text-white leading-snug">{title}</h3>

        {/* Mensaje */}
        {message && (
          <p className="text-sm text-slate-400 leading-relaxed">{message}</p>
        )}

        {/* Botones */}
        <div className="flex gap-3 pt-1">
          {/* Cancelar — estilo secundario */}
          <button
            onClick={onCancel}
            disabled={loading}
            className="flex-1 py-2.5 rounded-full text-sm font-bold
              bg-white/5 text-slate-300 border border-white/10
              hover:bg-white/10 transition-all disabled:opacity-50"
          >
            {cancelLabel}
          </button>

          {/* Confirmar — estilo configurable */}
          <button
            onClick={onConfirm}
            disabled={loading}
            className={`flex-1 py-2.5 rounded-full text-sm font-bold
              flex items-center justify-center gap-2
              transition-all disabled:opacity-50 ${confirmBtnClass}`}
          >
            {loading
              ? <Loader2 className="w-4 h-4 animate-spin" />
              : confirmLabel
            }
          </button>
        </div>
      </div>
    </div>
  );
}
