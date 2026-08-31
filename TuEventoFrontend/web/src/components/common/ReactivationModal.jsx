import { useState } from 'react';
import { ShieldCheck, Mail, Loader2, CheckCircle, AlertTriangle } from 'lucide-react';
import BaseModal from './BaseModal.jsx';
import { requestReactivation } from '../../services/Login.js';

/**
 * ReactivationModal — se muestra cuando el login falla con ACCOUNT_DEACTIVATED.
 *
 * Step 1 — Informa al usuario que su cuenta está desactivada y le ofrece
 *          solicitar reactivación. El email viene prellenado desde el form de login.
 * Step 2 — Confirmación de que el correo fue enviado (success) o error al
 *          hacer la petición.
 *
 * Usa BaseModal para heredar el overlay, animación de entrada y el sistema
 * de temas (colores del tema activo via CSS vars).
 */
export default function ReactivationModal({ isOpen, onClose, email }) {
  const [step, setStep]       = useState('confirm');  // 'confirm' | 'sent' | 'error'
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const handleClose = () => {
    // Reset local state when the modal closes so it's fresh if reopened
    setTimeout(() => { setStep('confirm'); setErrorMsg(''); }, 300);
    onClose();
  };

  const handleRequest = async () => {
    setLoading(true);
    setErrorMsg('');
    try {
      await requestReactivation(email);
      setStep('sent');
    } catch (err) {
      setErrorMsg(err.message || 'No se pudo enviar el correo. Intenta de nuevo.');
      setStep('error');
    } finally {
      setLoading(false);
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
            label: loading ? 'Enviando...' : 'Solicitar reactivación',
            onClick: handleRequest,
            variant: 'primary',
            disabled: loading,
            loading: loading,
            loadingLabel: 'Enviando...',
            icon: loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Mail className="w-4 h-4" />,
          },
          {
            label: 'Cancelar',
            onClick: handleClose,
            variant: 'secondary',
            disabled: loading,
          },
        ]}
      >
        <div className="space-y-3">
          <p className="text-sm bm-label leading-relaxed">
            Para volver a acceder, enviaremos un código de reactivación a:
          </p>
          <div
            className="flex items-center gap-2 rounded-xl px-4 py-3 bm-info-card"
            style={{ border: '1px solid color-mix(in srgb, var(--color-primary) 25%, transparent)' }}
          >
            <Mail className="w-4 h-4 flex-shrink-0" style={{ color: 'var(--color-accent)' }} />
            <span className="text-sm font-medium text-textPrimary truncate">{email}</span>
          </div>
          <p className="text-xs bm-hint leading-relaxed">
            El código expira en 30 minutos. Solo funciona una vez.
          </p>
        </div>
      </BaseModal>
    );
  }

  // ── Step: sent ────────────────────────────────────────────────────────────
  if (step === 'sent') {
    return (
      <BaseModal
        isOpen={isOpen}
        onClose={handleClose}
        variant="success"
        icon={<CheckCircle className="w-7 h-7" />}
        title="¡Correo enviado!"
        subtitle="Revisa tu bandeja de entrada."
        maxWidth="max-w-[420px]"
        actions={[
          {
            label: 'Entendido',
            onClick: handleClose,
            variant: 'primary',
          },
        ]}
      >
        <div className="space-y-3">
          <p className="text-sm bm-label leading-relaxed">
            Si el correo <strong className="text-textPrimary">{email}</strong> corresponde a una cuenta desactivada,
            recibirás un código de 8 dígitos en los próximos minutos.
          </p>
          <div className="bm-info-card rounded-xl">
            <p className="text-xs bm-hint leading-relaxed">
              Abre el correo de Tu Evento e ingresa el código en la página de confirmación{' '}
              (<span className="font-medium" style={{ color: 'var(--color-accent)' }}>/reactivate-account</span>).
              El enlace también está incluido en el email.
            </p>
          </div>
        </div>
      </BaseModal>
    );
  }

  // ── Step: error ───────────────────────────────────────────────────────────
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
          onClick: () => { setStep('confirm'); setErrorMsg(''); },
          variant: 'primary',
        },
        {
          label: 'Cerrar',
          onClick: handleClose,
          variant: 'secondary',
        },
      ]}
    >
      <p className="text-sm bm-label leading-relaxed">
        {errorMsg}
      </p>
    </BaseModal>
  );
}
