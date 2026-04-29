import { createContext, useContext, useState, useEffect } from "react";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { colors } from "../theme/colors";
import { getActivePalette } from "../services/themeService";
import { useAuth } from "./AuthContext";

export const ThemeContext = createContext(null);

export function ThemeProvider({ children }) {
  const { user } = useAuth();
  const [palette, setPalette] = useState(colors);
  const [isLoadingTheme, setIsLoadingTheme] = useState(false);

  useEffect(() => {
    if (!user) {
      // Sin sesión — volver al fallback
      setPalette(colors);
      return;
    }

    fetchPalette();
  }, [user]);

  // Polling cada 30 segundos
  useEffect(() => {
    if (!user) return;
    
    const interval = setInterval(async () => {
      try {
        const token = await AsyncStorage.getItem('accessToken');
        if (!token) return;
        const data = await getActivePalette(token);
        setPalette({ ...colors, ...data });
      } catch (e) {
        console.log('[ThemeContext] polling error:', e.message);
      }
    }, 30000);
    
    return () => clearInterval(interval);
  }, [user]);

  const fetchPalette = async () => {
    setIsLoadingTheme(true);
    try {
      const accessToken = await AsyncStorage.getItem("accessToken");
      if (!accessToken) return;
      const remotePalette = await getActivePalette(accessToken);
      setPalette({ ...colors, ...remotePalette });
    } catch (e) {
      console.warn("[ThemeContext] No se pudo cargar la paleta remota, usando fallback:", e.message);
      // Mantiene el fallback — la app no se rompe
    } finally {
      setIsLoadingTheme(false);
    }
  };

  const refreshPalette = async () => {
    await fetchPalette();
  };

  return (
    <ThemeContext.Provider value={{ palette, isLoadingTheme, refreshPalette }}>
      {children}
    </ThemeContext.Provider>
  );
}

export const useTheme = () => useContext(ThemeContext);
