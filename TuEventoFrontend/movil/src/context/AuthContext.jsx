import { createContext, useContext, useState, useEffect } from "react";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { jwtDecode } from "jwt-decode";
import { authService } from "../services/authService";
import { profileService } from "../services/profileService";
import { refreshAccessToken } from "../services/httpClient";
import { mapErrorMessage } from "../utils/errorMessages";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showLogoutModal, setShowLogoutModal] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  // Inicialización: restaura la sesión guardada al arrancar. El access token dura 15 min, así
  // que al volver a abrir la app suele estar vencido: se renueva con el refresh token en vez
  // de cerrar la sesión.
  useEffect(() => {
    const restoreSession = async () => {
      try {
        let accessToken = await AsyncStorage.getItem("accessToken");
        if (!accessToken) return;
        const { exp } = jwtDecode(accessToken);
        if (exp && exp * 1000 - Date.now() < 30_000) {
          try {
            accessToken = await refreshAccessToken();
          } catch (e) {
            // El servidor rechazó el refresh (vencido o revocado) o no hay refresh token: sin sesión.
            // Un fallo de red cae al catch de abajo y conserva los tokens.
            if (e.message === "REFRESH_FAILED" || e.message === "NO_REFRESH_TOKEN") {
              await AsyncStorage.multiRemove(["accessToken", "refreshToken"]);
              return;
            }
            throw e;
          }
        }
        const decoded = jwtDecode(accessToken);
        const profile = await profileService.getByUserId(decoded.userId, accessToken);
        setUser({
          userId: decoded.userId,
          alias: decoded.sub,
          role: decoded.role,
          fullName: profile.fullName,
          profileId: profile.profileId,
          storedFileId: profile.storedFileId,
        });
      } catch {
        // Sin red u otro fallo pasajero: arranca sin sesión pero conserva los tokens para
        // restaurarla en el próximo arranque (una sesión inválida la limpia httpRequest)
      } finally {
        setIsLoading(false);
      }
    };
    void restoreSession();
  }, []);

  const setSession = async (data) => {
    await AsyncStorage.setItem("accessToken", data.accessToken);
    await AsyncStorage.setItem("refreshToken", data.refreshToken);
    const decoded = jwtDecode(data.accessToken);
    const profile = await profileService.getByUserId(data.userId, data.accessToken);
    setUser({
      userId: data.userId,
      alias: data.alias,
      role: decoded.role,
      fullName: profile.fullName,
      profileId: profile.profileId,
      storedFileId: profile.storedFileId,
    });
    return decoded.role;
  };

  const login = async (email, password) => {
    setLoading(true);
    setError(null);
    try {
      const data = await authService.login(email, password);
      const role = await setSession(data);
      return { success: true, role };
    } catch (e) {
      const mapped = mapErrorMessage(e.message);
      setError(mapped);
      return { success: false, message: mapped };
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    await AsyncStorage.removeItem("accessToken");
    await AsyncStorage.removeItem("refreshToken");
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, setUser, loading, error, login, logout, setSession, showLogoutModal, setShowLogoutModal, isLoading }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
