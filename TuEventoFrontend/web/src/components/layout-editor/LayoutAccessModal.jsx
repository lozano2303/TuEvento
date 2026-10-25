import { useTranslation } from 'react-i18next';
import { ShieldAlert } from 'lucide-react';
import BaseModal from '../common/BaseModal';

/**
 * Modal que bloquea el editor de layout cuando el usuario no puede usarlo.
 * Se muestra en el idioma del usuario: va antes de la restricción de español.
 *
 * Props:
 *   reason  'notOrganizer' — el usuario no tiene rol de organizador
 *           'notOwner'     — es organizador, pero el evento es de otra persona
 *   onBack  () => void     — vuelve a una ruta segura
 */
export default function LayoutAccessModal({ reason, onBack }) {
  const { t } = useTranslation();
  return (
    <BaseModal
      isOpen
      onClose={onBack}
      hideOverlayClose
      variant="danger"
      icon={<ShieldAlert className="w-8 h-8" />}
      title={t(`layoutGuard.${reason}.title`)}
      subtitle={t(`layoutGuard.${reason}.subtitle`)}
      actions={[{ label: t(`layoutGuard.${reason}.action`), variant: 'primary', onClick: onBack }]}
    >
      <p className="bm-hint text-center text-sm leading-relaxed">
        {t(`layoutGuard.${reason}.description`)}
      </p>
    </BaseModal>
  );
}
