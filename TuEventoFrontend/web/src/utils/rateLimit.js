import i18n from '../i18n';

// El backend responde 429 cuando una IP supera el límite de los endpoints de autenticación
export const TOO_MANY_REQUESTS = 429;

/**
 * Mensaje traducido para un 429. Usa Retry-After (segundos) si el navegador lo expone.
 */
export const rateLimitMessage = (response) => {
  const seconds = Number.parseInt(response.headers?.get('Retry-After'), 10);
  return seconds > 0
    ? i18n.t('auth.tooManyRequestsWait', { seconds })
    : i18n.t('auth.tooManyRequests');
};

export const throwIfRateLimited = (response) => {
  if (response.status === TOO_MANY_REQUESTS) {
    throw new Error(rateLimitMessage(response));
  }
};
