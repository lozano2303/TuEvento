import { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import {
  AlertTriangle, Eye, EyeOff, Loader2,
  ShieldCheck, X,
} from 'lucide-react';
import { deactivateAccount } from '../../services/ProfileService';
import { performLogout } from '../../services/httpClient';

/**
 * DeactivateAccountModal — premium redesign
 *
 * Two-step flow:
 *   Step 1 — Explain consequences + CTA to proceed
 *   Step 2 — Password confirmation
 *
 * Logic/callbacks preserved exactly. Only the visual layer changes.
 */
export default function DeactivateAccountModal({ isOpen, onClose }) {
  const [step, setStep]                 = useState(1);
  const [password, setPassword]         = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading]           = useState(false);
  const [error, setError]               = useState('');
  const cardRef                         = useRef(null);

  /* ── Reset on close ────────────────────────────────────────────────────── */
  const handleClose = () => {
    setStep(1);
    setPassword('');
    setShowPassword(false);
    setError('');
    setLoading(false);
    onClose();
  };

  /* ── Escape key + scroll lock ──────────────────────────────────────────── */
  useEffect(() => {
    if (!isOpen) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const onKey = (e) => { if (e.key === 'Escape' && !loading) handleClose(); };
    window.addEventListener('keydown', onKey);
    setTimeout(() => cardRef.current?.focus(), 50);
    return () => {
      window.removeEventListener('keydown', onKey);
      document.body.style.overflow = prev;
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen, loading]);

  /* ── Deactivation submit ───────────────────────────────────────────────── */
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

  if (!isOpen) return null;

  const modal = (
    /* ── Overlay ── */
    <div
      className="fixed inset-0 z-[9999] flex items-center justify-center p-4 sm:p-6"
      style={{
        background: 'rgba(10,4,20,0.80)',
        backdropFilter: 'blur(18px)',
        WebkitBackdropFilter: 'blur(18px)',
      }}
      onClick={(e) => { if (e.target === e.currentTarget && !loading) handleClose(); }}
      role="presentation"
    >
      {/* ── Card ── */}
      <div
        ref={cardRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="dac-title"
        tabIndex={-1}
        className="relative w-full outline-none"
        style={{
          maxWidth: '480px',
          background: 'linear-gradient(145deg, rgba(30,10,60,0.92) 0%, rgba(20,6,42,0.96) 100%)',
          backdropFilter: 'blur(24px)',
          WebkitBackdropFilter: 'blur(24px)',
          border: '1px solid rgba(139,92,246,0.25)',
          borderRadius: '22px',
          boxShadow: '0 0 0 1px rgba(139,92,246,0.08), 0 32px 64px rgba(0,0,0,0.55), 0 0 80px rgba(109,40,217,0.12)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* ── Top glow bar ── */}
        <div
          className="absolute top-0 left-1/2 -translate-x-1/2 pointer-events-none"
          style={{
            width: '60%',
            height: '1px',
            background: 'linear-gradient(90deg, transparent, rgba(167,139,250,0.6), transparent)',
            borderRadius: '50%',
          }}
        />

        {/* ── Close button ── */}
        <button
          type="button"
          onClick={handleClose}
          disabled={loading}
          aria-label="Cerrar"
          className="absolute top-4 right-4 w-8 h-8 rounded-full flex items-center justify-center transition-all disabled:opacity-40"
          style={{
            background: 'rgba(139,92,246,0.08)',
            border: '1px solid rgba(139,92,246,0.15)',
            color: 'rgba(196,181,253,0.7)',
          }}
          onMouseOver={(e) => { e.currentTarget.style.background = 'rgba(139,92,246,0.18)'; }}
          onMouseOut={(e)  => { e.currentTarget.style.background = 'rgba(139,92,246,0.08)'; }}
        >
          <X className="w-4 h-4" />
        </button>

        {/* ── Inner content ── */}
        <div className="px-6 pt-8 pb-6 sm:px-8 sm:pt-9 sm:pb-7">

          {/* Title */}
          <h2
            id="dac-title"
            className="text-center text-xl sm:text-2xl font-bold mb-2"
            style={{ color: '#f5f3ff', letterSpacing: '-0.01em' }}
          >
            ¿Desactivar tu cuenta?
          </h2>

          {/* Subtitle */}
          <p
            className="text-center text-sm leading-relaxed mb-6"
            style={{ color: 'rgba(196,181,253,0.75)' }}
          >
            {step === 1
              ? 'Esta acción cerrará tu sesión de inmediato y desactivará tu cuenta temporalmente.'
              : 'Ingresa tu contraseña para confirmar la desactivación de tu cuenta.'}
          </p>

          {/* ── STEP 1 ── */}
          {step === 1 && (
            <div className="space-y-3">

              {/* Consequences card */}
              <div
                className="rounded-xl p-4"
                style={{
                  background: 'rgba(109,40,217,0.12)',
                  border: '1px solid rgba(139,92,246,0.20)',
                }}
              >
                <p
                  className="text-xs font-semibold uppercase tracking-widest mb-3"
                  style={{ color: 'rgba(167,139,250,0.80)' }}
                >
                  Ten en cuenta que:
                </p>
                <ul className="space-y-2.5">
                  {[
                    'Tu sesión cerrará inmediatamente.',
                    'No podrás iniciar sesión hasta contactar soporte.',
                    'Tus datos se conservan, no se elimina nada permanentemente.',
                  ].map((text) => (
                    <li key={text} className="flex items-start gap-2.5">
                      <div
                        className="w-4 h-4 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5"
                        style={{
                          background: 'rgba(139,92,246,0.20)',
                          border: '1px solid rgba(139,92,246,0.35)',
                        }}
                      >
                        <div
                          className="w-1.5 h-1.5 rounded-full"
                          style={{ background: '#a78bfa' }}
                        />
                      </div>
                      <span className="text-sm leading-5" style={{ color: 'rgba(221,214,254,0.85)' }}>
                        {text}
                      </span>
                    </li>
                  ))}
                </ul>
              </div>

              {/* Actions */}
              <div className="flex flex-col sm:flex-row gap-3 pt-2">
                <button
                  type="button"
                  onClick={handleClose}
                  className="flex-1 py-3 rounded-xl text-sm font-semibold transition-all"
                  style={{
                    background: 'rgba(255,255,255,0.05)',
                    border: '1px solid rgba(255,255,255,0.10)',
                    color: 'rgba(196,181,253,0.80)',
                  }}
                  onMouseOver={(e) => { e.currentTarget.style.background = 'rgba(255,255,255,0.09)'; }}
                  onMouseOut={(e)  => { e.currentTarget.style.background = 'rgba(255,255,255,0.05)'; }}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  onClick={() => { setStep(2); setError(''); }}
                  className="flex-1 py-3 rounded-xl text-sm font-bold transition-all"
                  style={{
                    background: 'linear-gradient(135deg, #7c3aed 0%, #9333ea 60%, #a21caf 100%)',
                    color: '#ffffff',
                    boxShadow: '0 4px 20px rgba(109,40,217,0.40)',
                  }}
                  onMouseOver={(e) => { e.currentTarget.style.filter = 'brightness(1.10)'; }}
                  onMouseOut={(e)  => { e.currentTarget.style.filter = 'none'; }}
                >
                  Sí, desactivar cuenta
                </button>
              </div>

              {/* Security note */}
              <p className="text-center text-xs pt-1" style={{ color: 'rgba(167,139,250,0.45)' }}>
                <ShieldCheck className="w-3 h-3 inline mr-1 -mt-px" />
                Tu información está segura con nosotros.
              </p>
            </div>
          )}

          {/* ── STEP 2 ── */}
          {step === 2 && (
            <form id="deactivate-form" onSubmit={handleDeactivate} className="space-y-4">

              {/* Password input */}
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => { setPassword(e.target.value); setError(''); }}
                  placeholder="Contraseña actual"
                  autoFocus
                  disabled={loading}
                  className="w-full rounded-xl px-4 py-3 pr-11 text-sm transition-all"
                  style={{
                    background: 'rgba(255,255,255,0.05)',
                    border: `1px solid ${error ? 'rgba(248,113,113,0.60)' : 'rgba(139,92,246,0.25)'}`,
                    color: '#f5f3ff',
                    outline: 'none',
                  }}
                  onFocus={(e) => {
                    if (!error) e.target.style.borderColor = 'rgba(139,92,246,0.60)';
                    e.target.style.boxShadow = '0 0 0 3px rgba(109,40,217,0.15)';
                  }}
                  onBlur={(e) => {
                    if (!error) e.target.style.borderColor = 'rgba(139,92,246,0.25)';
                    e.target.style.boxShadow = 'none';
                  }}
                />
                <button
                  type="button"
                  tabIndex={-1}
                  onClick={() => setShowPassword((v) => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 transition-colors"
                  style={{ color: 'rgba(167,139,250,0.55)' }}
                  onMouseOver={(e) => { e.currentTarget.style.color = 'rgba(196,181,253,0.90)'; }}
                  onMouseOut={(e)  => { e.currentTarget.style.color = 'rgba(167,139,250,0.55)'; }}
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>

              {/* Inline error */}
              {error && (
                <p className="text-xs flex items-center gap-1.5" style={{ color: '#f87171' }}>
                  <AlertTriangle className="w-3.5 h-3.5 flex-shrink-0" />
                  {error}
                </p>
              )}

              {/* Actions */}
              <div className="flex flex-col sm:flex-row gap-3 pt-1">
                <button
                  type="button"
                  onClick={() => { setStep(1); setError(''); setPassword(''); }}
                  disabled={loading}
                  className="flex-1 py-3 rounded-xl text-sm font-semibold transition-all disabled:opacity-40"
                  style={{
                    background: 'rgba(255,255,255,0.05)',
                    border: '1px solid rgba(255,255,255,0.10)',
                    color: 'rgba(196,181,253,0.80)',
                  }}
                  onMouseOver={(e) => { if (!loading) e.currentTarget.style.background = 'rgba(255,255,255,0.09)'; }}
                  onMouseOut={(e)  => { e.currentTarget.style.background = 'rgba(255,255,255,0.05)'; }}
                >
                  Volver
                </button>
                <button
                  type="submit"
                  disabled={loading || !password.trim()}
                  className="flex-1 py-3 rounded-xl text-sm font-bold transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                  style={{
                    background: 'linear-gradient(135deg, #7c3aed 0%, #9333ea 60%, #a21caf 100%)',
                    color: '#ffffff',
                    boxShadow: loading || !password.trim() ? 'none' : '0 4px 20px rgba(109,40,217,0.40)',
                  }}
                  onMouseOver={(e) => { if (!loading && password.trim()) e.currentTarget.style.filter = 'brightness(1.10)'; }}
                  onMouseOut={(e)  => { e.currentTarget.style.filter = 'none'; }}
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

              {/* Security note */}
              <p className="text-center text-xs pt-1" style={{ color: 'rgba(167,139,250,0.45)' }}>
                <ShieldCheck className="w-3 h-3 inline mr-1 -mt-px" />
                Tu información está segura con nosotros.
              </p>
            </form>
          )}
        </div>
      </div>
    </div>
  );

  return createPortal(modal, document.body);
}
