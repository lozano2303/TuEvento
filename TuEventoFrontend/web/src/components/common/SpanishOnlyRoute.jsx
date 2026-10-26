import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Languages } from 'lucide-react';
import { useLanguageSync } from '../../hooks/useLanguageSync';
import { getProfileByUserId, updateProfile } from '../../services/ProfileService';
import { getActiveLanguages } from '../../services/LanguageService';
import { changeUILanguage } from '../../i18n';
import BaseModal from './BaseModal';

/**
 * Guarda español como idioma preferido del perfil (si hay sesión) y cambia la UI.
 * Sin guardarlo en el perfil, useLanguageSync volvería al idioma anterior al recargar.
 */
async function switchToSpanish() {
  const userId = localStorage.getItem('userID');
  if (userId) {
    const [profileRes, languages] = await Promise.all([getProfileByUserId(userId), getActiveLanguages()]);
    const profileId = profileRes?.data?.profileId;
    const spanish   = languages.find((l) => l.code === 'es');
    if (profileId && spanish) await updateProfile(profileId, { preferredLanguageId: spanish.languageId });
  }
  changeUILanguage('es');
}

/**
 * SpanishOnlyRoute — guardia para rutas que sólo pueden usarse con la UI en español
 * (crear evento, editor de layout, etc.).
 *
 * Si el idioma actual no es español, bloquea el contenido y muestra un modal
 * explicando que debe usar la UI en español. El mensaje se muestra en el idioma
 * activo del usuario (vía i18n). Permite cambiar a español ahí mismo o volver a
 * la ruta indicada (por defecto la landing).
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
  const [switching, setSwitching] = useState(false);
  const isSpanishLanguage = currentLanguage === 'es';

  if (isSpanishLanguage) return children;

  const goBack = () => navigate(redirectTo, { replace: true });

  const handleSwitch = async () => {
    setSwitching(true);
    try {
      await switchToSpanish();
    } catch (err) {
      // Si no se pudo guardar en el perfil, al menos cambiar la UI de esta sesión
      console.warn('[SpanishOnlyRoute] Could not save the preferred language:', err.message);
      changeUILanguage('es');
    } finally {
      setSwitching(false);
    }
  };

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
        { label: t('languageGuard.switchAction'), variant: 'primary', onClick: handleSwitch, loading: switching },
      ]}
    >
      <p className="bm-hint text-center text-sm leading-relaxed">
        {t('languageGuard.description')}
      </p>
    </BaseModal>
  );
}
