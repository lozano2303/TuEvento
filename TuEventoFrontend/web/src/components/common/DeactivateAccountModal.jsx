import { useState } from 'react';
import { AlertTriangle, Eye, EyeOff, Loader2, ShieldOff } from 'lucide-react';
import { deactivateAccount } from '../../services/ProfileService';
import { performLogout } from '../../services/httpClient';

/**
 * DeactivateAccountModal
 *
 * Two-step confirmation modal for account deactivation:
 *   Step 1 — Explain consequences and ask for explicit confirmation.
 *   Step 2 — Require password verification before executing.
 *
 * On success: calls performLogout() and redirects to /login.
 * Uses the project's CSS vars (var(--color-*)) for full theme support.
 */
export default function DeactivateAccountModal({ isOpen, onClose }) {
  const [step, setStep]               = useState(1); // 1 = warning, 2 = password
  const [password, setPassword]       = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading]         = useState(false);
  const [error, setError]             = useState('');

  if (!isOpen) return null;

  const handleClose = () => {
    // Reset internal state when dismissed
    setStep(1);
    setPassword('');
    setShowPassword(false);
    setError('');
    setLoading(false);
    onClose();
  };

  const handleConfirmWarning = () => {
    setStep(2);
    setError('');
  };

  const handleDeactivate = async (e) => {
    e.preventDefault();
    if (!password.trim()) {
      setError('Ingresa tu contraseña para continuar.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await deactivateAccount(password);
      // Session cleanup + redirect — same pattern as handleLogout in ProfilePage
      await performLogout();
      window.location.href = '/login';
    } catch (err) {
      setError(err.message || 'No se pudo desactivar la cuenta. Intenta de nuevo.');
      setLoading(false);
    }
  };

  return (
    /* Overlay */
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: 'rgba(0,0,0,0.60)' }}
      onClick={(e) => { if (e.target === e.currentTarget) handleClose(); }}
    >
      <div
        className="w-full max-w-md rounded-2xl overflow-hidden shadow-2xl"
        style={{
          background: 'var(--color-surface)',
          border: '1px solid color-mix(in srgb, var(--color-error) 30%, transparent)',
        }}
      >
        {/* ── Header ── */}
        <div
          className="px-6 py-5 flex items-center gap-3"
          style={{
            background: 'color-mix(in srgb, var(--color-error) 10%, var(--color-surface))',
            borderBottom: '1px solid color-mix(in srgb, var(--color-error) 20%, transparent)',
          }}
        >
          <div
            className="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0"
            style={{
              background: 'color-mix(in srgb, var(--color-error) 15%, transparent)',
              border: '1px solid color-mix(in srgb, var(--color-error) 35%, transparent)',
            }}
          >
            <ShieldOff className="w-5 h-5 text-error" />
          </div>
          <div>
            <p className="text-base font-bold text-error leading-tight">Desactivar Cuenta</p>
            <p className="text-xs text-textMuted mt-0.5">
              {step === 1 ? 'Lee esto antes de continuar' : 'Confirma tu identidad'}
            </p>
          </div>
        </div>

        {/* ── Step 1: Warning ── */}
        {step === 1 && (
          <div className="px-6 py-6 space-y-4">
            <div
              className="flex gap-3 p-4 rounded-xl"
              style={{
                background: 'color-mix(in srgb, var(--color-error) 8%, transparent)',
                border: '1px solid color-mix(in srgb, var(--color-error) 20%, transparent)',
              }}
            >
              <AlertTriangle className="w-5 h-5 text-error flex-shrink-0 mt-0.5" />
              <div className="space-y-1.5">
                <p className="text-sm font-semibold text-error">¿Estás seguro?</p>
                <p className="text-sm text-textSecondary leading-relaxed">
                  Al desactivar tu cuenta:
                </p>
                <ul className="text-sm text-textSecondary space-y-1 list-none">
                  <li className="flex items-start gap-2">
                    <span className="text-error mt-0.5">•</span>
                    Tu sesión cerrará inmediatamente.
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-error mt-0.5">•</span>
                    No podrás iniciar sesión hasta contactar soporte.
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-error mt-0.5">•</span>
                    Tus datos se conservan — no se elimina nada permanentemente.
                  </li>
                </ul>
              </div>
            </div>

            <p className="text-sm text-textMuted">
              Para reactivar tu cuenta necesitarás contactar al equipo de soporte de Tu Evento.
            </p>

            <div className="flex gap-3 pt-2">
              <button
                type="button"
                onClick={handleClose}
                className="flex-1 py-2.5 rounded-xl text-sm font-semibold text-textSecondary transition-all"
                style={{
                  background: 'var(--color-surfaceAlt)',
                  border: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 80%, transparent)',
                }}
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleConfirmWarning}
                className="flex-1 py-2.5 rounded-xl text-sm font-bold text-white transition-all hover:brightness-90"
                style={{ background: 'var(--color-error)' }}
              >
                Sí, quiero desactivar
              </button>
            </div>
          </div>
        )}

        {/* ── Step 2: Password verification ── */}
        {step === 2 && (
          <form onSubmit={handleDeactivate} className="px-6 py-6 space-y-4">
            <p className="text-sm text-textSecondary leading-relaxed">
              Ingresa tu contraseña actual para confirmar la desactivación de tu cuenta.
            </p>

            {/* Password input */}
            <div className="relative">
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => { setPassword(e.target.value); setError(''); }}
                placeholder="Contraseña actual"
                autoFocus
                disabled={loading}
                className="w-full rounded-xl px-4 py-3 pr-11 text-sm text-textPrimary transition-all"
                style={{
                  background: 'var(--color-background)',
                  border: `1px solid ${error
                    ? 'var(--color-error)'
                    : 'color-mix(in srgb, var(--color-surfaceAlt) 80%, transparent)'}`,
                  outline: 'none',
                }}
                onFocus={(e) => {
                  if (!error) e.target.style.borderColor = 'var(--color-primary)';
                }}
                onBlur={(e) => {
                  if (!error) e.target.style.borderColor =
                    'color-mix(in srgb, var(--color-surfaceAlt) 80%, transparent)';
                }}
              />
              <button
                type="button"
                onClick={() => setShowPassword((v) => !v)}
                tabIndex={-1}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-textMuted hover:text-textSecondary transition-colors"
              >
                {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>

            {/* Inline error */}
            {error && (
              <p className="text-xs text-error flex items-center gap-1.5">
                <AlertTriangle className="w-3.5 h-3.5 flex-shrink-0" />
                {error}
              </p>
            )}

            <div className="flex gap-3 pt-1">
              <button
                type="button"
                onClick={() => { setStep(1); setError(''); setPassword(''); }}
                disabled={loading}
                className="flex-1 py-2.5 rounded-xl text-sm font-semibold text-textSecondary transition-all disabled:opacity-50"
                style={{ background: 'var(--color-surfaceAlt)' }}
              >
                Volver
              </button>
              <button
                type="submit"
                disabled={loading || !password.trim()}
                className="flex-1 py-2.5 rounded-xl text-sm font-bold text-white transition-all hover:brightness-90 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                style={{ background: 'var(--color-error)' }}
              >
                {loading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Desactivando...
                  </>
                ) : (
                  'Confirmar desactivación'
                )}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
