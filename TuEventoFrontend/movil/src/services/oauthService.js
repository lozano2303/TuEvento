import * as AuthSession from "expo-auth-session";
import * as WebBrowser from "expo-web-browser";
import * as Linking from "expo-linking";

const API_URL = process.env.EXPO_PUBLIC_API_URL;
const GOOGLE_CLIENT_ID = process.env.EXPO_PUBLIC_GOOGLE_CLIENT_ID;
const FACEBOOK_APP_ID = process.env.EXPO_PUBLIC_FACEBOOK_APP_ID;

// Esquema de redirección
const redirectUrl = Linking.createURL("oauth-callback");

WebBrowser.maybeCompleteAuthSession();

export const oauthService = {
  /**
   * Inicia el flujo OAuth2 con Google
   */
  async loginWithGoogle() {
    try {
      const result = await AuthSession.startAsync({
        authUrl: `https://accounts.google.com/o/oauth2/v2/auth?${new URLSearchParams({
          client_id: GOOGLE_CLIENT_ID,
          redirect_uri: redirectUrl,
          response_type: "code",
          scope: "openid profile email",
          prompt: "consent",
        }).toString()}`,
        returnUrl: redirectUrl,
      });

      if (result.type === "success") {
        return result.params.code;
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
   * Inicia el flujo OAuth2 con Facebook
   */
  async loginWithFacebook() {
    try {
      const result = await AuthSession.startAsync({
        authUrl: `https://www.facebook.com/v18.0/dialog/oauth?${new URLSearchParams({
          client_id: FACEBOOK_APP_ID,
          redirect_uri: redirectUrl,
          response_type: "code",
          scope: "public_profile,email",
          display: "popup",
        }).toString()}`,
        returnUrl: redirectUrl,
      });

      if (result.type === "success") {
        return result.params.code;
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
