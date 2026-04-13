import { createContext, useContext, useState, useEffect } from "react";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { jwtDecode } from "jwt-decode";
import { authService } from "../services/authService";
import { profileService } from "../services/profileService";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showLogoutModal, setShowLogoutModal] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  // Inicialización: verifica si hay sesión guardada al arrancar
  useEffect(() => {
    const restoreSession = async () => {
      try {
        const accessToken = await AsyncStorage.getItem("accessToken");
        if (accessToken) {
          const decoded = jwtDecode(accessToken);
          // Token expirado — limpiar y no restaurar
          if (decoded.exp && decoded.exp * 1000 < Date.now()) {
            await AsyncStorage.multiRemove(["accessToken", "refreshToken"]);
          } else {
            const profile = await profileService.getByUserId(decoded.userId, accessToken);
            setUser({
              userId: decoded.userId,
              alias: decoded.sub,
              role: decoded.role,
              fullName: profile.fullName,
            });
          }
        }
      } catch {
        // Si falla la restauración, simplemente arranca sin sesión
        await AsyncStorage.multiRemove(["accessToken", "refreshToken"]);
      } finally {
        setIsLoading(false);
      }
    };
    restoreSession();
  }, []);

  const login = async (email, password) => {
    setLoading(true);
    setError(null);
    try {
      const data = await authService.login(email, password);
      await AsyncStorage.setItem("accessToken", data.accessToken);
      await AsyncStorage.setItem("refreshToken", data.refreshToken);
      const decoded = jwtDecode(data.accessToken);
      const profile = await profileService.getByUserId(data.userId, data.accessToken);
      setUser({
        userId: data.userId,
        alias: data.alias,
        role: decoded.role,
        fullName: profile.fullName,
      });
      return { success: true };
    } catch (e) {
      setError(e.message);
      return { success: false, message: e.message };
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
    <AuthContext.Provider value={{ user, loading, error, login, logout, showLogoutModal, setShowLogoutModal, isLoading }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
