import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const GSI_SRC = 'https://accounts.google.com/gsi/client';

// Fresh module per test: the service keeps its state at module level (once per page load)
async function freshService() {
  vi.resetModules();
  return import('../../src/services/googleIdentity.js');
}

function fakeGsi() {
  const id = { initialize: vi.fn(), renderButton: vi.fn(), disableAutoSelect: vi.fn() };
  return id;
}

/** Simulates the browser finishing the download of gsi/client. */
function finishScriptLoad(id) {
  window.google = { accounts: { id } };
  document.querySelector(`script[src="${GSI_SRC}"]`).dispatchEvent(new Event('load'));
}

describe('googleIdentity', () => {
  beforeEach(() => {
    delete window.google;
    document.head.innerHTML = '';
    document.body.innerHTML = '';
  });

  afterEach(() => {
    delete window.google;
  });

  it('loads gsi/client once and initializes once, but renders the button on every mount', async () => {
    const { renderGoogleButton, releaseGoogleButton } = await freshService();
    const id = fakeGsi();
    const first = document.createElement('div');
    const second = document.createElement('div');

    const firstRender = renderGoogleButton(first, { clientId: 'client-1', onCredential: vi.fn(), buttonOptions: {} });
    finishScriptLoad(id);
    await firstRender;
    releaseGoogleButton(() => {});

    // Login view mounted again without a reload (e.g. after logging out from the profile view)
    await renderGoogleButton(second, { clientId: 'client-1', onCredential: vi.fn(), buttonOptions: {} });

    expect(document.querySelectorAll(`script[src="${GSI_SRC}"]`)).toHaveLength(1);
    expect(id.initialize).toHaveBeenCalledTimes(1);
    expect(id.renderButton).toHaveBeenCalledTimes(2);
    expect(id.renderButton).toHaveBeenLastCalledWith(second, {});
  });

  it('routes the credential to the button mounted now, not to an unmounted one', async () => {
    const { renderGoogleButton, releaseGoogleButton } = await freshService();
    const id = fakeGsi();
    window.google = { accounts: { id } };
    const oldHandler = vi.fn();
    const newHandler = vi.fn();

    await renderGoogleButton(document.createElement('div'), { clientId: 'c', onCredential: oldHandler, buttonOptions: {} });
    releaseGoogleButton(oldHandler);
    await renderGoogleButton(document.createElement('div'), { clientId: 'c', onCredential: newHandler, buttonOptions: {} });

    const { callback } = id.initialize.mock.calls[0][0];
    callback({ credential: 'id-token' });

    expect(oldHandler).not.toHaveBeenCalled();
    expect(newHandler).toHaveBeenCalledWith({ credential: 'id-token' });
  });

  it('drops credentials after the button is released', async () => {
    const { renderGoogleButton, releaseGoogleButton } = await freshService();
    const id = fakeGsi();
    window.google = { accounts: { id } };
    const handler = vi.fn();

    await renderGoogleButton(document.createElement('div'), { clientId: 'c', onCredential: handler, buttonOptions: {} });
    releaseGoogleButton(handler);
    id.initialize.mock.calls[0][0].callback({ credential: 'late' });

    expect(handler).not.toHaveBeenCalled();
  });

  it('clears the previous button before rendering a new one in the same container', async () => {
    const { renderGoogleButton } = await freshService();
    const id = fakeGsi();
    window.google = { accounts: { id } };
    const container = document.createElement('div');
    container.innerHTML = '<iframe></iframe>';

    await renderGoogleButton(container, { clientId: 'c', onCredential: vi.fn(), buttonOptions: {} });

    expect(container.children).toHaveLength(0);
  });

  it('allows a retry when the script fails to load', async () => {
    const { loadGoogleIdentity } = await freshService();

    const failed = loadGoogleIdentity();
    document.querySelector(`script[src="${GSI_SRC}"]`).dispatchEvent(new Event('error'));
    await expect(failed).rejects.toThrow('GSI_LOAD_FAILED');
    expect(document.querySelector(`script[src="${GSI_SRC}"]`)).toBeNull();

    const retry = loadGoogleIdentity();
    expect(document.querySelectorAll(`script[src="${GSI_SRC}"]`)).toHaveLength(1);
    const id = fakeGsi();
    finishScriptLoad(id);
    await expect(retry).resolves.toBe(id);
  });

  it('disableGoogleAutoSelect calls GSI when it is loaded and is a no-op otherwise', async () => {
    const { disableGoogleAutoSelect } = await freshService();

    expect(() => disableGoogleAutoSelect()).not.toThrow();

    const id = fakeGsi();
    window.google = { accounts: { id } };
    disableGoogleAutoSelect();
    expect(id.disableAutoSelect).toHaveBeenCalledTimes(1);
  });
});
