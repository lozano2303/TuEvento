import { useState, useRef, useEffect } from 'react';
import { ShieldCheck, Mail, Loader2, CheckCircle, AlertTriangle, KeyRound, RefreshCcw } from 'lucide-react';
import BaseModal from './BaseModal.jsx';
import { requestReactivation, confirmReactivation } from '../../services/Login.js';

/**
 * ReactivationModal — se muestra cuando el login falla con ACCOUNT_DEACTIVATED.
 *
 * Steps:
 *   confirm → usuario ve el email y decide solicitar el código
 *   verify  → ingresa el código de 8 chars recibido por correo
 *   done    → cuenta reactivada, cierre automático / botón de login
 *   error   → fallo al enviar el correo (reintento disponible)
 *
 * El backend NO necesita el email para validar el código (findByToken
 * resuelve directamente), por lo que solo se envía { token }.
 *
 * Usa BaseModal para heredar overlay, animación de entrada y tema activo.
 */

// ── Traducción de errores del backend → mensajes en español ─────────────────
const normalizeConfirmError = (message) => {
  const m = (message || '').toLowerCase();
  if (m.includes('expirado') || m.includes('expired'))
    return { msg: 'El código ha expirado. Solicita uno nuevo con el botón de abajo.', canResend: true };
  if (m.includes('ya fue utilizado') || m.includes('used'))
    return { msg: 'Este código ya fue utilizado. Solicita uno nuevo si necesitas reactivar tu cuenta.', canResend: true };
  if (m.includes('inválido') || m.includes('not_found') || m.includes('invalid') || m.includes('no encontrado'))
    return { msg: 'El código no es válido. Verifica que lo copiaste correctamente del correo.', canResend: false };
  return { msg: message || 'No se pudo confirmar la reactivación. Intenta de nuevo.', canResend: false };
};

export default function ReactivationModal({ isOpen, onClose, email }) {
  // steps: 'confirm' | 'verify' | 'done' | 'send_error'
  const [step, setStep]           = useState('confirm');
  const [code, setCode]           = useState('');
  const [codeError, setCodeError] = useState('');    // error inline del input
  const [canResend, setCanResend] = useState(false); // mostrar "Reenviar código"
  const [sendLoading, setSendLoading]     = useState(false);
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [sendErrorMsg, setSendErrorMsg]   = useState('');
  const [resendStatus, setResendStatus]   = useState('idle'); // 'idle'|'sending'|'sent'
  const codeInputRef = useRef(null);

  // Enfocar el input cuando entra al step verify
  useEffect(() => {
    if (step === 'verify' && isOpen) {
      setTimeout(() => codeInputRef.current?.focus(), 80);
    }
  }, [step, isOpen]);

  const resetState = () => {
    setStep('confirm');
    setCode('');
    setCodeError('');
    setCanResend(false);
    setSendLoading(false);
    setConfirmLoading(false);
    setSendErrorMsg('');
    setResendStatus('idle');
  };

  const handleClose = () => {
    // Retrasamos el reset para que la animación de salida termine antes
    setTimeout(resetState, 300);
    onClose();
  };

  // ── Solicitar código de reactivación ─────────────────────────────────────
  const handleRequestCode = async () => {
    setSendLoading(true);
    setSendErrorMsg('');
    try {
      await requestReactivation(email);
      setCode('');
      setCodeError('');
      setCanResend(false);
      setResendStatus('idle');
      setStep('verify');
    } catch (err) {
      setSendErrorMsg(err.message || 'No se pudo enviar el correo. Intenta de nuevo.');
      setStep('send_error');
    } finally {
      setSendLoading(false);
    }
  };

  // ── Reenviar código desde el step verify ─────────────────────────────────
  const handleResend = async () => {
    setResendStatus('sending');
    setCodeError('');
    setCode('');
    try {
      await requestReactivation(email);
      setResendStatus('sent');
    } catch {
      setResendStatus('idle'); // silencioso — no interrumpir el flujo
    }
  };

  // ── Confirmar código ──────────────────────────────────────────────────────
  const handleConfirm = async () => {
    const trimmed = code.trim().toUpperCase();
    if (trimmed.length !== 8) {
      setCodeError('El código debe tener exactamente 8 caracteres.');
      return;
    }
    setConfirmLoading(true);
    setCodeError('');
    setCanResend(false);
    try {
      await confirmReactivation(trimmed);
      setStep('done');
    } catch (err) {
      const { msg, canResend: showResend } = normalizeConfirmError(err.message);
      setCodeError(msg);
      setCanResend(showResend);
    } finally {
      setConfirmLoading(false);
    }
  };

  // ── Step: confirm ─────────────────────────────────────────────────────────
  if (step === 'confirm') {
    return (
      <BaseModal
        isOpen={isOpen}
        onClose={handleClose}
        variant="warning"
        icon={<ShieldCheck className="w-7 h-7" />}
        title="Cuenta desactivada"
        subtitle="Tu cuenta fue desactivada voluntariamente."
        maxWidth="max-w-[420px]"
        actions={[
          {
            label: sendLoading ? 'Enviando...' : 'Enviar código de reactivación',
            onClick: handleRequestCode,
            variant: 'primary',
            disabled: sendLoading,
            loading: sendLoading,
            loadingLabel: 'Enviando...',
            icon: sendLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Mail className="w-4 h-4" />,
          },
          {
            label: 'Cancelar',
            onClick: handleClose,
            variant: 'secondary',
            disabled: sendLoading,
          },
        ]}
      >
        <div className="space-y-3">
          <p className="text-sm bm-label leading-relaxed">
            Para volver a acceder, te enviaremos un código de reactivación a:
          </p>
          <div
            className="flex items-center gap-2 rounded-xl px-4 py-3 bm-info-card"
            style={{ border: '1px solid color-mix(in srgb, var(--color-primary) 25%, transparent)' }}
          >
            <Mail className="w-4 h-4 flex-shrink-0" style={{ color: 'var(--color-accent)' }} />
            <span className="text-sm font-medium text-textPrimary truncate">{email}</span>
          </div>
          <p className="text-xs bm-hint leading-relaxed">
            El código expira en 30 minutos y solo funciona una vez.
          </p>
        </div>
      </BaseModal>
    );
  }

  // ── Step: verify — ingreso del código ────────────────────────────────────
  if (step === 'verify') {
    return (
      <BaseModal
        isOpen={isOpen}
        onClose={handleClose}
        variant="info"
        icon={<KeyRound className="w-7 h-7" />}
        title="Ingresa tu código"
        subtitle={`Revisa el correo de ${email}`}
        maxWidth="max-w-[420px]"
        actions={[
          {
            label: confirmLoading ? 'Verificando...' : 'Confirmar reactivación',
            onClick: handleConfirm,
            variant: 'primary',
            disabled: confirmLoading || code.trim().length !== 8,
            loading: confirmLoading,
            loadingLabel: 'Verificando...',
            icon: confirmLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle className="w-4 h-4" />,
          },
          {
            label: 'Volver',
            onClick: () => { setStep('confirm'); setCode(''); setCodeError(''); setCanResend(false); },
            variant: 'secondary',
            disabled: confirmLoading,
          },
        ]}
      >
        <div className="space-y-3">
          {/* Input del código */}
          <div className="space-y-1.5">
            <label className="text-xs bm-label font-medium" htmlFor="reactivation-code">
              Código de reactivación (8 caracteres)
            </label>
            <input
              ref={codeInputRef}
              id="reactivation-code"
              type="text"
              inputMode="text"
              autoComplete="one-time-code"
              maxLength={8}
              placeholder="Ej: NHFM2N77"
              value={code}
              disabled={confirmLoading}
              onChange={(e) => {
                // Trim automático + forzar mayúsculas al escribir
                setCode(e.target.value.trimStart().toUpperCase());
                setCodeError('');
                setCanResend(false);
              }}
              onKeyDown={(e) => { if (e.key === 'Enter' && !confirmLoading) handleConfirm(); }}
              className="w-full rounded-xl px-4 py-3 text-center text-xl font-mono font-bold tracking-[0.35em] text-textPrimary disabled:opacity-50 focus:outline-none transition-all"
              style={{
                background: 'color-mix(in srgb, var(--color-surfaceAlt) 60%, transparent)',
                border: `1px solid ${codeError
                  ? 'color-mix(in srgb, var(--color-error) 60%, transparent)'
                  : 'color-mix(in srgb, var(--color-primary) 30%, transparent)'}`,
                letterSpacing: '0.35em',
              }}
              onFocus={(e) => {
                if (!codeError) e.target.style.borderColor = 'color-mix(in srgb, var(--color-primary) 65%, transparent)';
                e.target.style.boxShadow = '0 0 0 3px color-mix(in srgb, var(--color-primary) 15%, transparent)';
              }}
              onBlur={(e) => {
                if (!codeError) e.target.style.borderColor = 'color-mix(in srgb, var(--color-primary) 30%, transparent)';
                e.target.style.boxShadow = 'none';
              }}
            />
            {/* Error inline */}
            {codeError && (
              <p className="text-xs flex items-start gap-1.5" style={{ color: 'var(--color-error)' }}>
                <AlertTriangle className="w-3.5 h-3.5 flex-shrink-0 mt-0.5" />
                {codeError}
              </p>
            )}
          </div>

          {/* Reenviar código */}
          <div className="bm-divider" />
          {resendStatus === 'sent' ? (
            <p className="text-xs bm-hint flex items-center gap-1.5">
              <CheckCircle className="w-3.5 h-3.5 flex-shrink-0" style={{ color: 'var(--color-success)' }} />
              Nuevo código enviado. Revisa tu correo.
            </p>
          ) : (
            <div className="flex items-center justify-between">
              <p className="text-xs bm-hint">¿No te llegó o ya expiró?</p>
              <button
                type="button"
                onClick={handleResend}
                disabled={resendStatus === 'sending' || confirmLoading}
                className="text-xs font-semibold flex items-center gap-1 transition-colors disabled:opacity-40"
                style={{ color: 'var(--color-accent)' }}
                onMouseOver={(e) => { e.currentTarget.style.color = 'var(--color-primary)'; }}
                onMouseOut={(e)  => { e.currentTarget.style.color = 'var(--color-accent)'; }}
              >
                {resendStatus === 'sending'
                  ? <><Loader2 className="w-3 h-3 animate-spin" /> Enviando…</>
                  : <><RefreshCcw className="w-3 h-3" /> Reenviar código</>
                }
              </button>
            </div>
          )}

          {/* Mostrar "Solicitar nuevo" si el error indica que puede reenviar */}
          {canResend && (
            <p className="text-xs bm-hint">
              Usa el botón{' '}
              <span className="font-semibold" style={{ color: 'var(--color-accent)' }}>
                Reenviar código
              </span>{' '}
              de arriba para obtener un código nuevo.
            </p>
          )}
        </div>
      </BaseModal>
    );
  }

  // ── Step: done — cuenta reactivada ───────────────────────────────────────
  if (step === 'done') {
    return (
      <BaseModal
        isOpen={isOpen}
        onClose={handleClose}
        variant="success"
        icon={<CheckCircle className="w-7 h-7" />}
        title="¡Cuenta reactivada!"
        subtitle="Ya puedes iniciar sesión."
        maxWidth="max-w-[420px]"
        hideOverlayClose
        actions={[
          {
            label: 'Iniciar sesión',
            onClick: handleClose,
            variant: 'primary',
          },
        ]}
      >
        <div className="bm-info-card rounded-xl">
          <p className="text-sm bm-label leading-relaxed">
            Tu cuenta está activa de nuevo. Todos tus datos se conservaron intactos.
            Cierra este aviso para iniciar sesión con tu correo y contraseña.
          </p>
        </div>
      </BaseModal>
    );
  }

  // ── Step: send_error — fallo al enviar el correo ──────────────────────────
  return (
    <BaseModal
      isOpen={isOpen}
      onClose={handleClose}
      variant="danger"
      icon={<AlertTriangle className="w-7 h-7" />}
      title="No se pudo enviar"
      subtitle="Ocurrió un problema al enviar el correo."
      maxWidth="max-w-[420px]"
      actions={[
        {
          label: 'Reintentar',
          onClick: () => { setStep('confirm'); setSendErrorMsg(''); },
          variant: 'primary',
        },
        {
          label: 'Cerrar',
          onClick: handleClose,
          variant: 'secondary',
        },
      ]}
    >
      <p className="text-sm bm-label leading-relaxed">{sendErrorMsg}</p>
    </BaseModal>
  );
}
