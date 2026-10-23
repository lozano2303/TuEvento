import { createContext, useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import AsyncStorage from '@react-native-async-storage/async-storage';
import i18n, { bootstrapLanguage, changeUILanguage } from '../i18n';
import { getActiveLanguages } from '../services/languageService';
import { profileService } from '../services/profileService';
import { useAuth } from './AuthContext';

/**
 * LanguageContext — equivalente móvil del sistema de idioma de la web
 * (i18n.js + useLanguageSync + persistencia en perfil).
 *
 * Responsabilidades:
 *   1. Al arrancar: aplicar el idioma persistido en AsyncStorage (bootstrapLanguage).
 *   2. Con sesión: sincronizar con el idioma preferido del perfil del backend
 *      (preferredLanguageId -> code via getActiveLanguages -> changeUILanguage).
 *   3. changeLanguage(code): cambia i18next + persiste local + persiste en el
 *      perfil del backend si hay sesión.
 *
 * Expone: { language, isSpanish, changeLanguage, languages, ready }
 */
const LanguageContext = createContext(null);

export function LanguageProvider({ children }) {
  const { i18n: i18nInstance } = useTranslation();
  const { user } = useAuth();
  const [language, setLanguage] = useState(i18n.language || 'es');
  const [languages, setLanguages] = useState([]); // [{ languageId, code }]
  const [ready, setReady] = useState(false);

  // Mantener el estado `language` en sync con los cambios de i18next.
  useEffect(() => {
    const onChange = (lng) => setLanguage(lng);
    i18nInstance.on('languageChanged', onChange);
    return () => i18nInstance.off('languageChanged', onChange);
  }, [i18nInstance]);

  // 1. Aplicar idioma persistido al arrancar.
  useEffect(() => {
    void (async () => {
      await bootstrapLanguage();
      setLanguage(i18n.language);
      setReady(true);
    })();
  }, []);

  // Cargar catálogo de idiomas activos (para mapear id <-> code).
  useEffect(() => {
    void (async () => {
      try {
        setLanguages(await getActiveLanguages());
      } catch {
        // sin catálogo: el selector podrá usar los códigos soportados por defecto
      }
    })();
  }, []);

  // 2. Sincronizar con el idioma preferido del perfil cuando hay sesión.
  useEffect(() => {
    if (!user?.userId) return;
    void (async () => {
      try {
        const token = await AsyncStorage.getItem('accessToken');
        if (!token) return;
        const profile = await profileService.getByUserId(user.userId, token);
        if (!profile?.preferredLanguageId) return;
        const list = languages.length ? languages : await getActiveLanguages();
        const preferred = list.find((l) => l.languageId === profile.preferredLanguageId);
        if (preferred && preferred.code !== i18n.language) {
          await changeUILanguage(preferred.code);
        }
      } catch {
        // no crítico — mantener idioma actual
      }
    })();
  }, [user?.userId, languages]);

  /**
   * Cambia el idioma de la UI. Persiste local siempre; en el perfil si hay sesión.
   * @param {string} code - es | en | fr | pt
   */
  const changeLanguage = async (code) => {
    await changeUILanguage(code);
    if (user?.profileId) {
      try {
        const token = await AsyncStorage.getItem('accessToken');
        const list = languages.length ? languages : await getActiveLanguages();
        const match = list.find((l) => l.code === code);
        if (token && match) {
          await profileService.updateProfile(
            user.profileId,
            { preferredLanguageId: match.languageId },
            token,
          );
        }
      } catch {
        // la persistencia en perfil es best-effort; el cambio local ya ocurrió
      }
    }
  };

  return (
    <LanguageContext.Provider
      value={{ language, isSpanish: language === 'es', changeLanguage, languages, ready }}
    >
      {children}
    </LanguageContext.Provider>
  );
}

export const useLanguage = () => {
  const ctx = useContext(LanguageContext);
  if (!ctx) throw new Error('useLanguage must be used inside LanguageProvider');
  return ctx;
};
