import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Languages } from 'lucide-react';
import { useLanguageSync } from '../../hooks/useLanguageSync';
import { changeUILanguage } from '../../i18n';
import BaseModal from './BaseModal';

/**
 * SpanishOnlyRoute — guardia para rutas que sólo pueden usarse con la UI en español
 * (crear evento, editor de layout, etc.).
 *
 * Si el idioma actual no es español, bloquea el contenido y muestra un modal
 * explicando que debe usar la UI en español. El mensaje se muestra en el idioma
 * activo del usuario (vía i18n). Permite cambiar a español ahí mismo o volver a
 * la ruta indicada (por defecto la landing). Cambiar a español afecta solo a la interfaz de esta
 * sesión: la app arranca siempre en español y el idioma preferido del perfil no se toca.
 *
 * Este guardia complementa a ProtectedRoute (rol) y cubre el caso de ingreso
 * directo por URL, donde deshabilitar un botón no es suficiente.
 *
 * Props:
 *   children     ReactNode — contenido a renderizar cuando el idioma es español.
 *   redirectTo   string    — ruta a la que volver si no es español (default '/').
 */
export default function SpanishOnlyRoute({ children, redirectTo = '/' }) {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const currentLanguage = useLanguageSync();
  const isSpanishLanguage = currentLanguage === 'es';

  if (isSpanishLanguage) return children;

  const goBack = () => navigate(redirectTo, { replace: true });

  const handleSwitch = () => changeUILanguage('es');

  return (
    <BaseModal
      isOpen
      onClose={goBack}
      hideOverlayClose
      variant="warning"
      icon={<Languages className="w-8 h-8" />}
      title={t('languageGuard.title')}
      subtitle={t('languageGuard.subtitle')}
      actions={[
        { label: t('languageGuard.action'), variant: 'secondary', onClick: goBack },
        { label: t('languageGuard.switchAction'), variant: 'primary', onClick: handleSwitch },
      ]}
    >
      <p className="bm-hint text-center text-sm leading-relaxed">
        {t('languageGuard.description')}
      </p>
    </BaseModal>
  );
}
