import { createContext, useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import AsyncStorage from '@react-native-async-storage/async-storage';
import i18n, { changeUILanguage } from '../i18n';
import { getActiveLanguages } from '../services/languageService';
import { profileService } from '../services/profileService';
import { useAuth } from './AuthContext';
import { onSessionEvent } from '../utils/sessionEvents';

/**
 * LanguageContext — equivalente móvil del sistema de idioma de la web
 * (i18n.js + useLanguageSync + persistencia en perfil).
 *
 * La app arranca siempre en español: no se recuerda el idioma de la sesión anterior ni se
 * aplica el del perfil al abrir.
 *
 * Responsabilidades:
 *   1. Cambios en vivo: LANGUAGE_UPDATED (el usuario cambió el idioma en otro dispositivo).
 *   2. changeLanguage(code): cambia i18next y lo guarda en el perfil si hay sesión (para el
 *      idioma de las notificaciones y como aviso a sus otras sesiones abiertas).
 *
 * Expone: { language, isSpanish, changeLanguage, languages, ready }
 */
const LanguageContext = createContext(null);

export function LanguageProvider({ children }) {
  const { i18n: i18nInstance } = useTranslation();
  const { user } = useAuth();
  const [language, setLanguage] = useState(i18n.language || 'es');
  const [languages, setLanguages] = useState([]); // [{ languageId, code }]
  // Sin idioma que restaurar, el contexto está listo desde el primer render
  const ready = true;

  // Mantener el estado `language` en sync con los cambios de i18next.
  useEffect(() => {
    const onChange = (lng) => setLanguage(lng);
    i18nInstance.on('languageChanged', onChange);
    return () => i18nInstance.off('languageChanged', onChange);
  }, [i18nInstance]);

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

  // 1. Cambios en vivo: LANGUAGE_UPDATED llega por WebSocket cuando el idioma
  //    preferido cambia en otro dispositivo o en la web (solo se aplica local).
  useEffect(() => {
    if (!user?.userId) return;
    return onSessionEvent((event) => {
      if (event?.type !== 'LANGUAGE_UPDATED') return;
      void (async () => {
        try {
          const list = await getActiveLanguages();
          const lang = list.find((l) => l.languageId === event.languageId);
          if (lang && lang.code !== i18n.language) await changeUILanguage(lang.code);
        } catch {
          // no crítico — mantener idioma actual
        }
      })();
    });
  }, [user?.userId]);

  /**
   * 2. Cambia el idioma de la UI y lo guarda en el perfil si hay sesión.
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
