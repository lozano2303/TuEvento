/**
 * googleIdentity — Google Identity Services (GSI) cargado e inicializado una sola vez.
 *
 * @react-oauth/google volvía a inyectar el script gsi/client y a llamar a
 * google.accounts.id.initialize() cada vez que se montaba el login (al cerrar sesión
 * desde la vista de perfil o al navegar a /login sin recargar). Google avisa de que
 * inicializar más de una vez da "unexpected behavior" y en producción el botón dejaba
 * de responder hasta el F5.
 *
 * Aquí el script se carga una vez por carga de página, initialize() se llama una vez
 * por client_id y el callback reenvía la credencial al botón montado en ese momento.
 * Lo que sí se repite en cada montaje es renderButton().
 */

const GSI_SRC = 'https://accounts.google.com/gsi/client';

let scriptPromise = null;
let initializedClientId = null;
let credentialHandler = null;

const gsi = () => window.google?.accounts?.id;

/** Carga gsi/client una sola vez y resuelve con google.accounts.id. */
export function loadGoogleIdentity() {
  if (gsi()) return Promise.resolve(gsi());
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve, reject) => {
      const existing = document.querySelector(`script[src="${GSI_SRC}"]`);
      const script = existing ?? document.createElement('script');
      script.addEventListener('load', () => (gsi() ? resolve(gsi()) : reject(new Error('GSI_UNAVAILABLE'))), { once: true });
      script.addEventListener('error', () => {
        // Permite reintentar en el próximo montaje (p. ej. sin red)
        scriptPromise = null;
        script.remove();
        reject(new Error('GSI_LOAD_FAILED'));
      }, { once: true });
      if (!existing) {
        script.src = GSI_SRC;
        script.async = true;
        script.defer = true;
        document.head.appendChild(script);
      }
    });
  }
  return scriptPromise;
}

/**
 * Dibuja el botón de Google en `container`. initialize() solo se ejecuta la primera vez
 * (o si cambia el client_id); `onCredential` pasa a ser el destino de la credencial.
 */
export async function renderGoogleButton(container, { clientId, onCredential, buttonOptions }) {
  const id = await loadGoogleIdentity();
  if (initializedClientId !== clientId) {
    id.initialize({
      client_id: clientId,
      callback: (response) => credentialHandler?.(response),
    });
    initializedClientId = clientId;
  }
  credentialHandler = onCredential;
  container.replaceChildren();
  id.renderButton(container, buttonOptions);
}

/** Al desmontar el botón: deja de recibir credenciales si era el destino actual. */
export function releaseGoogleButton(onCredential) {
  if (credentialHandler === onCredential) credentialHandler = null;
}

/**
 * Al cerrar sesión: evita que Google vuelva a elegir la cuenta automáticamente.
 * Sin efecto si GSI no llegó a cargarse en esta página.
 */
export function disableGoogleAutoSelect() {
  gsi()?.disableAutoSelect?.();
}
