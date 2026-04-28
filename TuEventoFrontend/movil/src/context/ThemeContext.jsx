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

    fetchPalette();
  }, [user]);

  return (
    <ThemeContext.Provider value={{ palette, isLoadingTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

export const useTheme = () => useContext(ThemeContext);
