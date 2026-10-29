import { useTranslation } from 'react-i18next';

/**
 * Idioma actual de la interfaz, para recargar el contenido traducido cuando cambia.
 *
 * Con sesión la app arranca en el idioma preferido del perfil (applySessionLanguage) y sin
 * sesión en español. Durante la sesión cambia con el selector o desde otro dispositivo, por el
 * aviso LANGUAGE_UPDATED.
 */
export const useLanguageSync = () => {
  const { i18n } = useTranslation();
  return i18n.language;
};
