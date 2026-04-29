import { createContext, useContext, useState, useEffect } from "react";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { colors as baseColors } from "../theme/colors";
import { THEMES, DEFAULT_THEME_ID } from "../theme/themes";

const STORAGE_KEY = "activeThemeId";

const ThemeContext = createContext(null);

/**
 * ThemeProvider — envuelve la app y expone:
 *   colors        → objeto de colores reactivo (muta al cambiar tema)
 *   activeThemeId → id del tema activo ("DARK" | "LIGHT" | "VIBRANT" | "ACCESSIBLE")
 *   applyTheme(id)→ aplica un tema por id, persiste en AsyncStorage
 *   themes        → lista completa de THEMES para renderizar opciones
 */
export function ThemeProvider({ children }) {
  const [activeThemeId, setActiveThemeId] = useState(DEFAULT_THEME_ID);
  // Copia mutable del objeto de colores — se reemplaza al cambiar tema
  const [colors, setColors] = useState({ ...baseColors });

  // Restaurar tema guardado al arrancar
  useEffect(() => {
    AsyncStorage.getItem(STORAGE_KEY).then((savedId) => {
      if (savedId) {
        const found = THEMES.find((t) => t.id === savedId);
        if (found) {
          setActiveThemeId(found.id);
          setColors({ ...baseColors, ...found.palette });
        }
      }
    });
  }, []);

  const applyTheme = async (themeId) => {
    const found = THEMES.find((t) => t.id === themeId);
    if (!found) return;
    setActiveThemeId(found.id);
    setColors({ ...baseColors, ...found.palette });
    await AsyncStorage.setItem(STORAGE_KEY, found.id);
  };

  return (
    <ThemeContext.Provider value={{ colors, activeThemeId, applyTheme, themes: THEMES }}>
      {children}
    </ThemeContext.Provider>
  );
}

export const useTheme = () => {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error("useTheme must be used inside ThemeProvider");
  return ctx;
};
