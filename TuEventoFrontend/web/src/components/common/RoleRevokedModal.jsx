import { useEffect, useState } from 'react';
import { ShieldOff, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

/**
 * RoleRevokedModal
 *
 * One-time modal shown to a user whose ORGANIZER role has just been revoked.
 *
 * Visibility contract:
 *   - Shown once when the `open` prop turns true (WS push triggers this).
 *   - On dismiss: marks `role_revoked_ack` in localStorage so it never
 *     appears again for this user session / after a page reload.
 *   - App.jsx also checks the flag on mount so late arrivals (e.g. the user
 *     was offline when the WS fired) see the modal on the next login.
 *
 * Props:
 *   open        boolean  — controlled by parent (NotificationContext / App)
 *   onClose     () => void
 */
export default function RoleRevokedModal({ open, onClose }) {
  const navigate = useNavigate();

  if (!open) return null;

  const handleClose = () => {
    onClose?.();
  };

  const handleGoHome = () => {
    handleClose();
    navigate('/events');
  };

  return (
    <div
      role="presentation"
      className="fixed inset-0 bg-black/85 backdrop-blur-sm flex items-center justify-center z-[90] p-4"
      onClick={(e) => { if (e.target === e.currentTarget) handleClose(); }}
      onKeyDown={(e) => { if (e.key === 'Escape') handleClose(); }}
    >
      <div className="bg-surface rounded-2xl border border-rose-500/30 shadow-2xl shadow-rose-900/20 w-full max-w-md flex flex-col overflow-hidden">

        {/* Header */}
        <div className="bg-gradient-to-r from-rose-600/30 to-rose-900/20 px-6 py-5 flex items-start justify-between border-b border-rose-500/20">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-rose-500/20 flex items-center justify-center flex-shrink-0">
              <ShieldOff className="w-5 h-5 text-rose-400" />
            </div>
            <div>
              <h2 className="text-base font-black text-textPrimary leading-tight">
                Cambio en tu rol
              </h2>
              <p className="text-xs text-rose-300/70 mt-0.5">Notificación importante</p>
            </div>
          </div>
          <button
            onClick={handleClose}
            className="p-1.5 hover:bg-white/10 rounded-full transition-colors text-textMuted mt-0.5"
            aria-label="Cerrar"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Body */}
        <div className="px-6 py-5 space-y-4">
          <p className="text-sm text-textSecondary leading-relaxed">
            Un administrador ha decidido <strong className="text-textPrimary">revocar tu rol de Organizador</strong>.
            Tu cuenta ahora tiene el rol de <strong className="text-textPrimary">Usuario</strong> y ya no podrás
            crear ni gestionar eventos.
          </p>

          <div className="bg-rose-500/8 border border-rose-500/20 rounded-xl p-4 text-xs text-rose-300/80 leading-relaxed">
            Si crees que esto fue un error, puedes contactar al equipo de soporte de TuEvento
            o volver a solicitar el rol de organizador desde tu perfil cuando lo desees.
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-surfaceAlt flex gap-3">
          <button
            onClick={handleClose}
            className="flex-1 py-2.5 rounded-full text-sm font-bold
              bg-surfaceAlt text-textSecondary border border-surfaceAlt
              hover:bg-surfaceAlt/70 transition-all"
          >
            Entendido
          </button>
          <button
            onClick={handleGoHome}
            className="flex-1 py-2.5 rounded-full text-sm font-bold text-white transition-all"
            style={{
              background: 'linear-gradient(90deg, var(--color-primary) 0%, var(--color-accent) 100%)',
            }}
          >
            Ir a eventos
          </button>
        </div>
      </div>
    </div>
  );
}
