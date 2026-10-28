import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import es from './locales/es.json';
import en from './locales/en.json';
import fr from './locales/fr.json';
import pt from './locales/pt.json';

// Mapeo de códigos de idioma del backend a códigos de i18next (identidad por ahora).
const LANGUAGE_CODE_MAP = { es: 'es', en: 'en', fr: 'fr', pt: 'pt' };

const SUPPORTED = ['es', 'en', 'fr', 'pt'];

// La app arranca siempre en español: no se recuerda el idioma de la sesión anterior ni se
// aplica el del perfil al abrir. El usuario lo cambia durante la sesión desde el selector.
i18n
  .use(initReactI18next)
  .init({
    resources: {
      es: { translation: es },
      en: { translation: en },
      fr: { translation: fr },
      pt: { translation: pt },
    },
    lng: 'es',
    fallbackLng: 'es',
    debug: false,
    interpolation: {
      escapeValue: false, // React ya escapa
    },
    compatibilityJSON: 'v4',
  });

/**
 * Cambia el idioma de la UI durante la sesión.
 * @param {string} languageCode - Código del idioma (es, en, fr, pt)
 */
export const changeUILanguage = async (languageCode) => {
  const mapped = LANGUAGE_CODE_MAP[languageCode] || languageCode;
  if (!SUPPORTED.includes(mapped)) return;
  await i18n.changeLanguage(mapped);
};

/**
 * Devuelve el código de idioma actual de la UI.
 * @returns {string}
 */
export const getCurrentUILanguage = () => i18n.language || 'es';

export default i18n;
