import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { BadgeCheck, CalendarPlus, LayoutDashboard, Wallet, ScanLine } from 'lucide-react';
import BaseModal from './BaseModal.jsx';

/**
 * Clave en sessionStorage que activa el modal tras recargar la página.
 * La escribe NotificationContext al recibir ROLE_UPDATED (solicitud aprobada).
 */
export const ORGANIZER_WELCOME_KEY = 'organizerWelcome';

function readFlag() {
  try {
    return sessionStorage.getItem(ORGANIZER_WELCOME_KEY) === 'true';
  } catch {
    return false;
  }
}

/**
 * OrganizerWelcomeModal — se muestra una sola vez cuando el usuario acaba de
 * recibir el rol ORGANIZER, listando las funciones que ahora tiene disponibles.
 */
export default function OrganizerWelcomeModal() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(readFlag);

  const close = () => {
    try { sessionStorage.removeItem(ORGANIZER_WELCOME_KEY); } catch { /* sin storage */ }
    setIsOpen(false);
  };

  const features = [
    { icon: CalendarPlus,    key: 'createEvents' },
    { icon: LayoutDashboard, key: 'manageEvents' },
    { icon: Wallet,          key: 'earnings' },
    { icon: ScanLine,        key: 'scanTickets' },
  ];

  return (
    <BaseModal
      isOpen={isOpen}
      onClose={close}
      variant="success"
      icon={<BadgeCheck size={28} />}
      title={t('organizerPetition.welcome.title')}
      subtitle={t('organizerPetition.welcome.subtitle')}
      actions={[
        { label: t('organizerPetition.welcome.later'), variant: 'secondary', onClick: close },
        {
          label: t('organizerPetition.welcome.createFirst'),
          variant: 'primary',
          onClick: () => { close(); navigate('/events/create'); },
        },
      ]}
    >
      <ul className="flex flex-col gap-3">
        {features.map(({ icon: Icon, key }) => (
          <li key={key} className="flex items-start gap-3">
            <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-primary/15 text-accent">
              <Icon size={16} />
            </span>
            <div>
              <p className="text-sm font-semibold text-textPrimary">
                {t(`organizerPetition.welcome.features.${key}.title`)}
              </p>
              <p className="text-xs text-textSecondary">
                {t(`organizerPetition.welcome.features.${key}.desc`)}
              </p>
            </div>
          </li>
        ))}
      </ul>
    </BaseModal>
  );
}
