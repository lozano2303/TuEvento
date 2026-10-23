import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { getProfileByUserId } from '../services/ProfileService';
import { getActiveLanguages } from '../services/LanguageService';
import { changeUILanguage } from '../i18n';

/**
 * Hook que sincroniza automáticamente el idioma de i18next con el idioma preferido del usuario del backend.
 * Se ejecuta una vez al montar el componente que lo use.
 */
export const useLanguageSync = () => {
  const { i18n } = useTranslation();

  useEffect(() => {
    const syncLanguage = async () => {
      const userId = localStorage.getItem('userID');
      if (!userId) {
        // Usuario no autenticado - usar idioma del navegador o español por defecto
        return;
      }

      try {
        // Obtener perfil del usuario
        const profileRes = await getProfileByUserId(userId);
        const profile = profileRes?.data;
        
        if (!profile?.preferredLanguageId) {
          // No tiene idioma preferido - mantener el actual
          return;
        }

        // Obtener los idiomas activos
        const languages = await getActiveLanguages();
        const preferredLang = languages.find(l => l.languageId === profile.preferredLanguageId);
        
        if (preferredLang && preferredLang.code !== i18n.language) {
          // Sincronizar i18next con el idioma del backend
          changeUILanguage(preferredLang.code);
          console.log(`[useLanguageSync] UI language synced to: ${preferredLang.code}`);
        }
      } catch (err) {
        console.warn('[useLanguageSync] Could not sync language:', err.message);
      }
    };

    syncLanguage();
  }, []); // Solo ejecutar una vez al montar

  return i18n.language;
};
