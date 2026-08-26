import { useState } from 'react';
import { AlertTriangle, Eye, EyeOff, Loader2, ShieldOff } from 'lucide-react';
import { deactivateAccount } from '../../services/ProfileService';
import { performLogout } from '../../services/httpClient';
import BaseModal from './BaseModal';

/**
 * DeactivateAccountModal
 *
 * Two-step confirmation modal built on top of BaseModal for visual consistency.
 *   Step 1 — Explain consequences and ask for explicit confirmation.
 *   Step 2 — Require password verification before executing.
 *
 * On success: calls performLogout() and redirects to /login.
 */
export default function DeactivateAccountModal({ isOpen, onClose }) {
  const [step, setStep]                 = useState(1);
  const [password, setPassword]         = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading]           = useState(false);
  const [error, setError]               = useState('');

  const handleClose = () => {
    setStep(1);
    setPassword('');
    setShowPassword(false);
    setError('');
    setLoading(false);
    onClose();
  };

  const handleDeactivate = async (e) => {
    e.preventDefault();
    if (!password.trim()) { setError('Ingresa tu contraseña para continuar.'); return; }
    setLoading(true);
    setError('');
    try {
      await deactivateAccount(password);
      await performLogout();
      window.location.href = '/login';
    } catch (err) {
      setError(err.message || 'No se pudo desactivar la cuenta. Intenta de nuevo.');
      setLoading(false);
    }
  };

  // ── Step 1 actions ──────────────────────────────────────────────────────────
  const step1Actions = [
    {
      label: 'Cancelar',
      variant: 'secondary',
      onClick: handleClose,
    },
    {
      label: 'Sí, continuar',
      variant: 'primary',
      onClick: () => { setStep(2); setError(''); },
    },
  ];

  // ── Step 2 actions ──────────────────────────────────────────────────────────
  const step2Actions = [
    {
      label: 'Volver',
      variant: 'secondary',
      disabled: loading,
      onClick: () => { setStep(1); setError(''); setPassword(''); },
    },
    {
      label: loading ? 'Desactivando...' : 'Confirmar desactivación',
      variant: 'primary',
      disabled: loading || !password.trim(),
      loading,
      loadingLabel: 'Desactivando...',
      // The form submit is handled separately; this button triggers it via form id
      onClick: () => document.getElementById('deactivate-form')?.requestSubmit(),
    },
  ];

  return (
    <BaseModal
      isOpen={isOpen}
      onClose={handleClose}
      variant="danger"
      icon={<ShieldOff className="w-7 h-7" />}
      title="Desactivar Cuenta"
      subtitle={step === 1 ? 'Esta acción cerrará tu sesión de inmediato' : 'Confirma tu identidad para continuar'}
      actions={step === 1 ? step1Actions : step2Actions}
      maxWidth="max-w-[440px]"
    >
      {/* ── Step 1: consequences ── */}
      {step === 1 && (
        <div className="space-y-4">
          <div className="flex gap-3 p-4 rounded-xl bg-error/8 border border-error/20">
            <AlertTriangle className="w-5 h-5 text-error flex-shrink-0 mt-0.5" />
            <div className="space-y-2">
              <p className="text-sm font-semibold text-error">¿Estás seguro?</p>
              <ul className="text-sm text-textSecondary space-y-1.5">
                <li className="flex items-start gap-2">
                  <span className="text-error leading-5">•</span>
                  Tu sesión cerrará inmediatamente.
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-error leading-5">•</span>
                  No podrás iniciar sesión hasta contactar soporte.
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-error leading-5">•</span>
                  Tus datos se conservan, no se elimina nada permanentemente.
                </li>
              </ul>
            </div>
          </div>
          <p className="text-sm text-textMuted">
            Para reactivar tu cuenta deberás contactar al equipo de soporte de Tu Evento.
          </p>
        </div>
      )}

      {/* ── Step 2: password ── */}
      {step === 2 && (
        <form id="deactivate-form" onSubmit={handleDeactivate} className="space-y-4">
          <p className="text-sm text-textSecondary leading-relaxed">
            Ingresa tu contraseña actual para confirmar la desactivación.
          </p>

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
                border: `1px solid ${error ? 'var(--color-error)' : 'color-mix(in srgb, var(--color-surfaceAlt) 80%, transparent)'}`,
                outline: 'none',
              }}
              onFocus={(e)  => { if (!error) e.target.style.borderColor = 'var(--color-primary)'; }}
              onBlur={(e)   => { if (!error) e.target.style.borderColor = 'color-mix(in srgb, var(--color-surfaceAlt) 80%, transparent)'; }}
            />
            <button
              type="button"
              tabIndex={-1}
              onClick={() => setShowPassword((v) => !v)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-textMuted hover:text-textSecondary transition-colors"
            >
              {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            </button>
          </div>

          {error && (
            <p className="text-xs text-error flex items-center gap-1.5">
              <AlertTriangle className="w-3.5 h-3.5 flex-shrink-0" />
              {error}
            </p>
          )}
        </form>
      )}
    </BaseModal>
  );
}
