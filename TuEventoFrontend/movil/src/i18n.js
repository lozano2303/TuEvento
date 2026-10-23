import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import AsyncStorage from '@react-native-async-storage/async-storage';

import es from './locales/es.json';
import en from './locales/en.json';
import fr from './locales/fr.json';
import pt from './locales/pt.json';

// Clave de persistencia del idioma de UI (misma convención conceptual que la web).
export const LANGUAGE_STORAGE_KEY = 'i18nextLng';

// Mapeo de códigos de idioma del backend a códigos de i18next (identidad por ahora).
const LANGUAGE_CODE_MAP = { es: 'es', en: 'en', fr: 'fr', pt: 'pt' };

const SUPPORTED = ['es', 'en', 'fr', 'pt'];

// Inicializa i18next de forma síncrona con el fallback 'es'. El idioma persistido
// en AsyncStorage se aplica justo después (asíncrono) vía bootstrapLanguage().
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
 * Lee el idioma guardado en AsyncStorage y lo aplica si difiere del actual.
 * Debe llamarse una vez al arrancar la app (p. ej. en el LanguageProvider).
 */
export const bootstrapLanguage = async () => {
  try {
    const saved = await AsyncStorage.getItem(LANGUAGE_STORAGE_KEY);
    if (saved && SUPPORTED.includes(saved) && saved !== i18n.language) {
      await i18n.changeLanguage(saved);
    }
  } catch {
    // sin persistencia disponible — mantener fallback
  }
};

/**
 * Cambia el idioma de la UI y lo persiste en AsyncStorage.
 * @param {string} languageCode - Código del idioma (es, en, fr, pt)
 */
export const changeUILanguage = async (languageCode) => {
  const mapped = LANGUAGE_CODE_MAP[languageCode] || languageCode;
  if (!SUPPORTED.includes(mapped)) return;
  await i18n.changeLanguage(mapped);
  try {
    await AsyncStorage.setItem(LANGUAGE_STORAGE_KEY, mapped);
  } catch {
    // ignorar fallo de persistencia
  }
};

/**
 * Devuelve el código de idioma actual de la UI.
 * @returns {string}
 */
export const getCurrentUILanguage = () => i18n.language || 'es';

export default i18n;
