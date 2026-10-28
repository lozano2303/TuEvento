import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import es from './locales/es.json';
import en from './locales/en.json';
import fr from './locales/fr.json';
import pt from './locales/pt.json';

// Mapeo de códigos de idioma del backend a códigos de i18next
const LANGUAGE_CODE_MAP = {
  'es': 'es',
  'en': 'en',
  'fr': 'fr',
  'pt': 'pt',
};

// La app arranca siempre en español (sin detectar el idioma del navegador ni recordar el de la
// sesión anterior); el usuario lo cambia durante la sesión con el selector.
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
      escapeValue: false, // React already escapes
    },
  });

/**
 * Cambia el idioma de la UI y lo guarda en localStorage
 * @param {string} languageCode - Código del idioma (es, en, fr, pt)
 */
export const changeUILanguage = (languageCode) => {
  const mappedCode = LANGUAGE_CODE_MAP[languageCode] || languageCode;
  i18n.changeLanguage(mappedCode);
};

/**
 * Obtiene el código de idioma actual de la UI
 * @returns {string} - Código del idioma (es, en, fr, pt)
 */
export const getCurrentUILanguage = () => {
  return i18n.language || 'es';
};

export default i18n;
