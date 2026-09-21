import * as AuthSession from "expo-auth-session";
import * as WebBrowser from "expo-web-browser";
import * as Linking from "expo-linking";
import * as Crypto from "expo-crypto";

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
   * Initiates the OAuth2 Authorization Code flow with Google.
   *
   * Returns { code, state } so the caller can pass both to authService.oauthLogin()
   * and verify the state against the value included in the server response.
   */
  async loginWithGoogle() {
    try {
      const state = await generateState();

      const result = await AuthSession.startAsync({
        authUrl: `https://accounts.google.com/o/oauth2/v2/auth?${new URLSearchParams({
          client_id: GOOGLE_CLIENT_ID,
          redirect_uri: redirectUrl,
          response_type: "code",
          scope: "openid profile email",
          prompt: "consent",
          state,
        }).toString()}`,
        returnUrl: redirectUrl,
      });

      if (result.type === "success") {
        // Verify that the state returned by Google matches what we sent.
        // A mismatch indicates a possible CSRF / redirect injection attack.
        if (result.params.state && result.params.state !== state) {
          throw new Error("OAuth state mismatch — posible ataque CSRF");
        }
        return { code: result.params.code, state };
      } else if (result.type === "dismiss") {
        throw new Error("Usuario canceló el inicio de sesión");
      } else {
        throw new Error("Error en el flujo OAuth de Google");
      }
    } catch (error) {
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
