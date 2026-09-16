import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle, AlertTriangle, Loader2, ShieldCheck, RefreshCcw } from 'lucide-react';
import { confirmReactivation, requestReactivation } from '../services/Login.js';

/**
 * ReactivateAccountPage — /reactivate-account?token=XXXXXXXX
 *
 * Al montar, lee el parámetro `token` de la URL y llama a
 * POST /api/v1/auth/reactivate-confirm.
 *
 * Estados:
 *  loading  — llamada en curso
 *  success  — cuenta reactivada, botón para ir al login
 *  error    — token inválido/expirado/ya usado, con mensaje claro y
 *             opción de solicitar uno nuevo ingresando el email
 *
 * No requiere autenticación — es una ruta pública.
 */
export default function ReactivateAccountPage() {
  const navigate    = useNavigate();
  const [status, setStatus]           = useState('loading'); // 'loading' | 'success' | 'error'
  const [errorMessage, setErrorMessage] = useState('');
  const [email, setEmail]               = useState('');
  const [resendStatus, setResendStatus] = useState('idle'); // 'idle' | 'sending' | 'sent' | 'error'
  const [resendError, setResendError]   = useState('');
  const calledRef = useRef(false);

  useEffect(() => {
    // Prevent double-invocation in React 18 Strict Mode dev double-effect
    if (calledRef.current) return;
    calledRef.current = true;

    const params = new URLSearchParams(window.location.search);
    const token  = params.get('token');

    if (!token || token.trim().length === 0) {
      setErrorMessage('No se proporcionó ningún token de reactivación. Verifica el enlace del correo.');
      setStatus('error');
      return;
    }

    confirmReactivation(token.trim())
      .then(() => setStatus('success'))
      .catch((err) => {
        const msg = err.message || '';
        // Translate known backend error codes to friendly Spanish messages
        if (msg.includes('expirado') || msg.toLowerCase().includes('expired')) {
          setErrorMessage('El código de reactivación expiró. Los códigos son válidos por 30 minutos. Solicita uno nuevo a continuación.');
        } else if (msg.includes('ya fue utilizado') || msg.toLowerCase().includes('used')) {
          setErrorMessage('Este código ya fue utilizado. Si tu cuenta sigue sin acceso, solicita un nuevo código.');
        } else if (msg.includes('inválido') || msg.toLowerCase().includes('not_found') || msg.toLowerCase().includes('invalid')) {
          setErrorMessage('El código de reactivación no es válido. Asegúrate de copiar el código completo del correo.');
        } else {
          setErrorMessage(msg || 'No se pudo reactivar la cuenta. Por favor intenta de nuevo.');
        }
        setStatus('error');
      });
  }, []);

  const handleResend = async () => {
    if (!email.trim()) return;
    setResendStatus('sending');
    setResendError('');
    try {
      await requestReactivation(email.trim());
      setResendStatus('sent');
    } catch (err) {
      setResendError(err.message || 'No se pudo enviar el correo. Intenta de nuevo.');
      setResendStatus('error');
    }
  };

  // ── Shared wrapper ────────────────────────────────────────────────────────
  const Wrapper = ({ children }) => (
    <div
      className="min-h-screen flex items-center justify-center p-6"
      style={{ background: 'var(--color-background)' }}
    >
      <div
        className="w-full max-w-md theme-modal-card rounded-2xl overflow-hidden"
        style={{ boxShadow: '0 24px 60px color-mix(in srgb, var(--color-primary) 25%, transparent)' }}
      >
        {children}
      </div>
    </div>
  );

  // ── Loading ───────────────────────────────────────────────────────────────
  if (status === 'loading') {
    return (
      <Wrapper>
        <div className="p-10 flex flex-col items-center gap-4">
          <Loader2 className="w-10 h-10 animate-spin" style={{ color: 'var(--color-primary)' }} />
          <p className="text-textSecondary text-sm">Verificando tu código de reactivación…</p>
        </div>
      </Wrapper>
    );
  }

  // ── Success ───────────────────────────────────────────────────────────────
  if (status === 'success') {
    return (
      <Wrapper>
        {/* Header con gradiente del tema */}
        <div className="bm-header">
          <div className="flex justify-center mb-3">
            <div className="bm-icon-ring bm-icon-success">
              <CheckCircle className="w-8 h-8" />
            </div>
          </div>
          <h1 className="bm-title">¡Cuenta reactivada!</h1>
          <p className="bm-subtitle">Tu cuenta está activa de nuevo.</p>
        </div>

        {/* Body */}
        <div className="bm-body">
          <div className="bm-info-card rounded-xl mb-4">
            <p className="text-sm text-textSecondary leading-relaxed">
              Ya puedes iniciar sesión con tu correo y contraseña habituales. Todos tus datos se conservaron intactos.
            </p>
          </div>
          <p className="text-xs bm-hint flex items-center gap-1.5">
            <ShieldCheck className="w-3.5 h-3.5 flex-shrink-0" style={{ color: 'var(--color-accent)' }} />
            El código de reactivación ha sido invalidado y no puede volver a usarse.
          </p>
        </div>

        {/* Footer */}
        <div className="bm-footer">
          <button
            onClick={() => navigate('/login')}
            className="bm-btn-primary"
          >
            Ir al inicio de sesión
          </button>
        </div>
      </Wrapper>
    );
  }

  // ── Error ─────────────────────────────────────────────────────────────────
  return (
    <Wrapper>
      {/* Header */}
      <div className="bm-header">
        <div className="flex justify-center mb-3">
          <div className="bm-icon-ring bm-icon-danger">
            <AlertTriangle className="w-8 h-8" />
          </div>
        </div>
        <h1 className="bm-title">Código no válido</h1>
        <p className="bm-subtitle">No se pudo reactivar la cuenta.</p>
      </div>

      {/* Body */}
      <div className="bm-body space-y-4">
        <div className="bm-info-card rounded-xl">
          <p className="text-sm text-textSecondary leading-relaxed">{errorMessage}</p>
        </div>

        {/* Solicitar nuevo código */}
        {resendStatus !== 'sent' ? (
          <div className="space-y-2">
            <p className="text-xs bm-label font-medium">¿Quieres solicitar un nuevo código?</p>
            <div className="flex gap-2">
              <input
                type="email"
                value={email}
                onChange={(e) => { setEmail(e.target.value); setResendError(''); }}
                placeholder="tu@gmail.com"
                disabled={resendStatus === 'sending'}
                className="flex-1 rounded-xl px-3 py-2.5 text-sm text-textPrimary focus:outline-none disabled:opacity-50"
                style={{
                  background: 'color-mix(in srgb, var(--color-surfaceAlt) 60%, transparent)',
                  border: '1px solid color-mix(in srgb, var(--color-primary) 30%, transparent)',
                }}
                onFocus={(e) => { e.target.style.borderColor = 'color-mix(in srgb, var(--color-primary) 65%, transparent)'; }}
                onBlur={(e)  => { e.target.style.borderColor = 'color-mix(in srgb, var(--color-primary) 30%, transparent)'; }}
              />
              <button
                onClick={handleResend}
                disabled={resendStatus === 'sending' || !email.trim()}
                className="bm-btn-primary px-4 rounded-xl text-sm whitespace-nowrap disabled:opacity-50 disabled:cursor-not-allowed"
                style={{ width: 'auto' }}
              >
                {resendStatus === 'sending'
                  ? <Loader2 className="w-4 h-4 animate-spin" />
                  : <RefreshCcw className="w-4 h-4" />}
              </button>
            </div>
            {resendStatus === 'error' && (
              <p className="text-xs text-error flex items-center gap-1.5">
                <AlertTriangle className="w-3 h-3 flex-shrink-0" />
                {resendError}
              </p>
            )}
          </div>
        ) : (
          <div className="bm-info-card rounded-xl">
            <p className="text-sm text-textSecondary leading-relaxed flex items-start gap-2">
              <CheckCircle className="w-4 h-4 flex-shrink-0 mt-0.5" style={{ color: 'var(--color-success)' }} />
              Correo enviado. Si el correo corresponde a una cuenta desactivada, recibirás el nuevo código en minutos.
            </p>
          </div>
        )}
      </div>

      {/* Footer */}
      <div className="bm-footer">
        <button onClick={() => navigate('/login')} className="bm-btn-secondary">
          Volver al inicio de sesión
        </button>
      </div>
    </Wrapper>
  );
}
