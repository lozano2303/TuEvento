import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Languages } from 'lucide-react';
import { useLanguageSync } from '../../hooks/useLanguageSync';
import BaseModal from './BaseModal';

/**
 * SpanishOnlyRoute — guardia para rutas que sólo pueden usarse con la UI en español
 * (crear evento, editor de layout, etc.).
 *
 * Si el idioma actual no es español, bloquea el contenido y muestra un modal
 * explicando que debe cambiar el idioma a español en su perfil. El mensaje se
 * muestra en el idioma activo del usuario (vía i18n). Al cerrar/confirmar, lo
 * devuelve a la ruta indicada (por defecto la landing).
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
        { label: t('languageGuard.action'), variant: 'primary', onClick: goBack },
      ]}
    >
      <p className="bm-hint text-center text-sm leading-relaxed">
        {t('languageGuard.description')}
      </p>
    </BaseModal>
  );
}
