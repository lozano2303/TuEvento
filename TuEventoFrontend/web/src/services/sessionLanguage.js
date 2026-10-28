/**
 * Idioma de la interfaz al abrir la app.
 *
 * Sin sesión (primera visita) la app arranca en español. Con la sesión iniciada arranca en el
 * idioma preferido del perfil; iniciar sesión siempre recarga la página, así que también se
 * aplica justo después del login.
 */
import { getAccessToken } from './tokenStore.js';
import { getProfileByUserId } from './ProfileService.js';
import { getActiveLanguages } from './LanguageService.js';
import { changeUILanguage } from '../i18n';

/** Lo máximo que se retrasa el primer render esperando el perfil. */
export const SESSION_LANGUAGE_WAIT_MS = 2500;

/**
 * Aplica el idioma preferido del usuario con sesión. Si el perfil no tiene idioma o no se
 * puede leer, la interfaz se queda en español. Con la red lenta no espera más de
 * SESSION_LANGUAGE_WAIT_MS: la app se muestra y cambia de idioma cuando llega el perfil.
 */
export async function applySessionLanguage() {
  const userId = localStorage.getItem('userID');
  if (!getAccessToken() || !userId) return;

  const apply = (async () => {
    const [profile, languages] = await Promise.all([getProfileByUserId(userId), getActiveLanguages()]);
    const preferredId = profile?.data?.preferredLanguageId;
    const language = languages.find((l) => l.languageId === preferredId);
    if (language) changeUILanguage(language.code);
  })().catch(() => {
    // no crítico: se mantiene el español
  });

  let timer;
  const timeout = new Promise((resolve) => { timer = setTimeout(resolve, SESSION_LANGUAGE_WAIT_MS); });
  await Promise.race([apply, timeout]);
  clearTimeout(timer);
}
