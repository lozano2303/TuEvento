import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { performLogout } from '../../src/services/httpClient.js';
import { getAccessToken, setAccessToken } from '../../src/services/tokenStore.js';

describe('performLogout', () => {
  beforeEach(() => {
    localStorage.clear();
    globalThis.fetch = vi.fn().mockResolvedValue(new Response('{}', { status: 200 }));
  });

  afterEach(() => {
    delete window.google;
    vi.restoreAllMocks();
  });

  it('revokes the session, clears the client and disables Google auto-select', async () => {
    const disableAutoSelect = vi.fn();
    window.google = { accounts: { id: { disableAutoSelect } } };
    setAccessToken('access-token');
    localStorage.setItem('userID', '5');

    await performLogout();

    expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/auth/logout'), expect.objectContaining({ method: 'POST' }));
    expect(getAccessToken()).toBeNull();
    expect(localStorage.getItem('userID')).toBeNull();
    expect(disableAutoSelect).toHaveBeenCalledTimes(1);
  });

  it('still logs out when Google Identity Services never loaded', async () => {
    setAccessToken('access-token');

    await expect(performLogout()).resolves.toBeUndefined();
    expect(getAccessToken()).toBeNull();
  });
});
