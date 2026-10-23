import * as AuthSession from "expo-auth-session";
import * as WebBrowser from "expo-web-browser";
import * as Linking from "expo-linking";
import * as Crypto from "expo-crypto";
import Constants, { ExecutionEnvironment } from "expo-constants";

/**
 * Carga perezosa del SDK nativo de Google Sign-In.
 * Si el binario instalado (dev client) se compiló antes de añadir la librería,
 * el import estático tumba toda la app al arrancar; así solo falla el botón de Google.
 */
function loadGoogleSignin() {
  // Expo Go no incluye módulos nativos de terceros: ni siquiera se intenta cargar.
  if (Constants.executionEnvironment === ExecutionEnvironment.StoreClient) {
    throw new Error("GOOGLE_SIGNIN_EXPO_GO");
  }
  try {
    return require("@react-native-google-signin/google-signin");
  } catch (_) {
    throw new Error("Google Sign-In no está disponible en este build. Recompila el dev client.");
  }
}

const API_URL = process.env.EXPO_PUBLIC_API_URL;
const GOOGLE_CLIENT_ID = process.env.EXPO_PUBLIC_GOOGLE_CLIENT_ID;
const FACEBOOK_APP_ID = process.env.EXPO_PUBLIC_FACEBOOK_APP_ID;

// Deep-link scheme registered in app.json (scheme field).
// Facebook's authorization endpoint returns the code to this URL after the user
// approves the app; Google does the same.  Expo's AuthSession intercepts the
// redirect and hands back result.params to the calling code.
const redirectUrl = Linking.createURL("oauth-callback");

WebBrowser.maybeCompleteAuthSession();

/**
 * Generates a cryptographically random state string for CSRF protection.
 * Uses expo-crypto so it works in both development and production builds
 * without relying on the web-only crypto.randomUUID().
 */
async function generateState() {
  const bytes = await Crypto.getRandomBytesAsync(16);
  return Array.from(bytes)
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

export const oauthService = {
  /**
   * Inicia sesión con Google usando el SDK nativo (Google Sign-In / Credential Manager).
   *
   * Mismo contrato que la web (GSI): se obtiene un id_token cuyo audience es el
   * Web client ID (webClientId) y se envía a POST /auth/google, donde el backend
   * lo verifica contra GOOGLE_CLIENT_ID.
   *
   * Requiere un cliente OAuth tipo Android en Google Cloud con el package
   * com.tuevento.movil y el SHA-1 del keystore con el que se firma el build.
   *
   * Devuelve { idToken }.
   */
  async loginWithGoogle() {
    // Fuera del try para que el mensaje llegue intacto a mapErrorMessage.
    const sdk = loadGoogleSignin();
    let isErrorWithCode = (_error) => false; // se reemplaza por el del SDK si carga
    let statusCodes = {};
    try {
      const { GoogleSignin, isCancelledResponse, isSuccessResponse } = sdk;
      ({ isErrorWithCode, statusCodes } = sdk);

      GoogleSignin.configure({ webClientId: GOOGLE_CLIENT_ID });
      await GoogleSignin.hasPlayServices({ showPlayServicesUpdateDialog: true });
      // Cierra la sesión local previa para que siempre se muestre el selector de cuenta.
      await GoogleSignin.signOut();

      const response = await GoogleSignin.signIn();
      if (isCancelledResponse(response)) {
        throw new Error("Usuario canceló el inicio de sesión");
      }
      if (!isSuccessResponse(response) || !response.data.idToken) {
        throw new Error("Error en el flujo OAuth de Google");
      }
      return { idToken: response.data.idToken };
    } catch (error) {
      if (isErrorWithCode(error) && error.code === statusCodes.IN_PROGRESS) {
        throw new Error("Error en el flujo OAuth de Google: ya hay un inicio de sesión en curso");
      }
      throw new Error(`Error OAuth Google: ${error.message}`);
    }
  },

  /**
   * Initiates the OAuth2 Authorization Code flow with Facebook.
   *
   * Returns { code, state } so the caller can pass both to authService.oauthLogin()
   * and verify the state against the value included in the server response.
   */
  async loginWithFacebook() {
    try {
      const state = await generateState();

      const result = await AuthSession.startAsync({
        authUrl: `https://www.facebook.com/v18.0/dialog/oauth?${new URLSearchParams({
          client_id: FACEBOOK_APP_ID,
          redirect_uri: redirectUrl,
          response_type: "code",
          scope: "public_profile,email",
          display: "popup",
          state,
        }).toString()}`,
        returnUrl: redirectUrl,
      });

      if (result.type === "success") {
        // Facebook always returns state in the callback when it was sent.
        if (result.params.state && result.params.state !== state) {
          throw new Error("OAuth state mismatch — posible ataque CSRF");
        }
        return { code: result.params.code, state };
      } else if (result.type === "dismiss") {
        throw new Error("Usuario canceló el inicio de sesión");
      } else {
        throw new Error("Error en el flujo OAuth de Facebook");
      }
    } catch (error) {
      throw new Error(`Error OAuth Facebook: ${error.message}`);
    }
  },
};
