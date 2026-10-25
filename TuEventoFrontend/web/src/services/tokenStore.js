/**
 * tokenStore — access token solo en memoria.
 *
 * El refresh token vive en una cookie httpOnly que el backend pone en
 * /api/v1/auth (JavaScript no puede leerla). El access token no se guarda en
 * localStorage: se pierde al recargar y se recupera con restoreSession()
 * (httpClient.js) antes de montar la app.
 *
 * Las pestañas abiertas comparten el token por BroadcastChannel: cuando una
 * renueva o cierra sesión, las demás se enteran sin pedir otro refresh.
 */

const CHANNEL_NAME = 'tuevento-auth';

let accessToken = null;

const channel = typeof BroadcastChannel === 'undefined' ? null : new BroadcastChannel(CHANNEL_NAME);

if (channel) {
  channel.onmessage = ({ data }) => {
    if (data?.type === 'token') accessToken = data.token || null;
    if (data?.type === 'logout') accessToken = null;
  };
}

export const getAccessToken = () => accessToken;

export function setAccessToken(token) {
  accessToken = token || null;
  channel?.postMessage({ type: 'token', token: accessToken });
}

export function clearAccessToken() {
  accessToken = null;
  channel?.postMessage({ type: 'logout' });
}
