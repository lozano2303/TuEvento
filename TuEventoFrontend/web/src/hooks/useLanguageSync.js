import { useTranslation } from 'react-i18next';

/**
 * Idioma actual de la interfaz, para recargar el contenido traducido cuando cambia.
 *
 * La app arranca siempre en español: ya no se aplica al abrir el idioma preferido del perfil.
 * El usuario lo cambia durante la sesión con el selector (o desde otro dispositivo, por el
 * aviso LANGUAGE_UPDATED) y al volver a abrir la app vuelve a español.
 */
export const useLanguageSync = () => {
  const { i18n } = useTranslation();
  return i18n.language;
};
