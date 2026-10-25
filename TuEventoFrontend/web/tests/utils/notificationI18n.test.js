import { describe, expect, it } from 'vitest';
import {
  baseLanguage,
  isAwaitingTranslation,
  localizeNotification,
  mergeTranslations,
  TRANSLATION_WAIT_MS,
} from '../../src/utils/notificationI18n';

const notification = {
  notificationUserId: 1,
  subject: 'Evento cancelado',
  body: 'El evento fue cancelado',
  sentAt: '2026-10-25T10:00:00Z',
  translations: [
    {
      languageCode: 'en-US',
      status: 'PUBLISHED',
      translatedSubject: 'Event cancelled',
      translatedBody: 'The event was cancelled',
    },
    { languageCode: 'pt', status: 'PENDING', translatedSubject: 'Evento cancelado (pt)' },
  ],
};

describe('baseLanguage', () => {
  it('reduce el idioma detectado a su código base', () => {
    expect(baseLanguage({ resolvedLanguage: 'en-US' })).toBe('en');
    expect(baseLanguage({ language: 'PT-br' })).toBe('pt');
  });

  it('usa español si i18n no está listo', () => {
    expect(baseLanguage(undefined)).toBe('es');
  });
});

describe('localizeNotification', () => {
  it('devuelve la traducción publicada del idioma', () => {
    expect(localizeNotification(notification, 'en')).toEqual({
      subject: 'Event cancelled',
      body: 'The event was cancelled',
    });
  });

  it('ignora traducciones no publicadas y vuelve al original', () => {
    expect(localizeNotification(notification, 'pt').subject).toBe('Evento cancelado');
  });

  it('en español devuelve el original', () => {
    expect(localizeNotification(notification, 'es').body).toBe('El evento fue cancelado');
  });
});

describe('isAwaitingTranslation', () => {
  const sentAt = new Date(notification.sentAt).getTime();

  it('espera mientras la notificación es reciente y no hay traducción', () => {
    expect(isAwaitingTranslation(notification, 'fr', sentAt + 1000)).toBe(true);
  });

  it('deja de esperar al pasar la ventana', () => {
    expect(isAwaitingTranslation(notification, 'fr', sentAt + TRANSLATION_WAIT_MS + 1)).toBe(false);
  });

  it('no espera si ya hay traducción o el idioma es el original', () => {
    expect(isAwaitingTranslation(notification, 'en', sentAt)).toBe(false);
    expect(isAwaitingTranslation(notification, 'es', sentAt)).toBe(false);
  });
});

describe('mergeTranslations', () => {
  it('copia solo las traducciones nuevas y conserva el resto de campos', () => {
    const current = [{ ...notification, translations: [], readAt: 'local' }];
    const merged = mergeTranslations(current, [notification]);

    expect(merged).not.toBe(current);
    expect(merged[0].readAt).toBe('local');
    expect(merged[0].translations).toEqual(notification.translations);
  });

  it('mantiene la misma referencia si nada cambió', () => {
    const current = [notification];
    expect(mergeTranslations(current, [notification])).toBe(current);
    expect(mergeTranslations(current, undefined)).toBe(current);
  });
});
