import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../../src/services/ProfileService.js', () => ({ getProfileByUserId: vi.fn() }));
vi.mock('../../src/services/LanguageService.js', () => ({ getActiveLanguages: vi.fn() }));
vi.mock('../../src/i18n', () => ({ changeUILanguage: vi.fn() }));

const { applySessionLanguage, SESSION_LANGUAGE_WAIT_MS } = await import('../../src/services/sessionLanguage.js');
const { getProfileByUserId } = await import('../../src/services/ProfileService.js');
const { getActiveLanguages } = await import('../../src/services/LanguageService.js');
const { changeUILanguage } = await import('../../src/i18n');
const { setAccessToken, clearAccessToken } = await import('../../src/services/tokenStore.js');

const LANGUAGES = [
  { languageId: 1, code: 'es' },
  { languageId: 2, code: 'en' },
  { languageId: 3, code: 'fr' },
];

describe('applySessionLanguage', () => {
  let storage;

  beforeEach(() => {
    storage = { userID: '7' };
    vi.stubGlobal('localStorage', { getItem: (key) => storage[key] ?? null });
    getActiveLanguages.mockResolvedValue(LANGUAGES);
  });

  afterEach(() => {
    clearAccessToken();
    vi.unstubAllGlobals();
    vi.clearAllMocks();
    vi.useRealTimers();
  });

  it('con sesión aplica el idioma preferido del perfil', async () => {
    setAccessToken('token');
    getProfileByUserId.mockResolvedValue({ success: true, data: { preferredLanguageId: 3 } });

    await applySessionLanguage();

    expect(getProfileByUserId).toHaveBeenCalledWith('7');
    expect(changeUILanguage).toHaveBeenCalledWith('fr');
  });

  it('sin sesión no consulta nada y se queda en español', async () => {
    await applySessionLanguage();

    expect(getProfileByUserId).not.toHaveBeenCalled();
    expect(changeUILanguage).not.toHaveBeenCalled();
  });

  it('sin userID guardado no consulta el perfil aunque haya token', async () => {
    storage = {};
    setAccessToken('token');

    await applySessionLanguage();

    expect(getProfileByUserId).not.toHaveBeenCalled();
  });

  it('un perfil sin idioma preferido deja el español', async () => {
    setAccessToken('token');
    getProfileByUserId.mockResolvedValue({ success: true, data: { preferredLanguageId: null } });

    await applySessionLanguage();

    expect(changeUILanguage).not.toHaveBeenCalled();
  });

  it('si el perfil falla no lanza y deja el español', async () => {
    setAccessToken('token');
    getProfileByUserId.mockRejectedValue(new Error('network'));

    await expect(applySessionLanguage()).resolves.toBeUndefined();
    expect(changeUILanguage).not.toHaveBeenCalled();
  });

  it('no retrasa el primer render más del límite, y aplica el idioma cuando llega', async () => {
    vi.useFakeTimers();
    setAccessToken('token');
    let resolveProfile;
    getProfileByUserId.mockReturnValue(new Promise((resolve) => { resolveProfile = resolve; }));

    const pending = applySessionLanguage();
    await vi.advanceTimersByTimeAsync(SESSION_LANGUAGE_WAIT_MS);
    await expect(pending).resolves.toBeUndefined();
    expect(changeUILanguage).not.toHaveBeenCalled();

    resolveProfile({ success: true, data: { preferredLanguageId: 2 } });
    await vi.waitFor(() => expect(changeUILanguage).toHaveBeenCalledWith('en'));
  });
});
