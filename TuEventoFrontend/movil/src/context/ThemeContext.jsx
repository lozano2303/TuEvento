import { createContext, useContext, useState, useEffect, useRef } from "react";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { colors as baseColors } from "../theme/colors";
import { THEMES, DEFAULT_THEME_ID } from "../theme/themes";
import { activateTheme, getActivePalette, getThemes } from "../services/themeService";

const STORAGE_KEY = "activeThemeId";

const ThemeContext = createContext(null);

export function ThemeProvider({ children }) {
  const [activeThemeId, setActiveThemeId] = useState(DEFAULT_THEME_ID);
  const [colors, setColors] = useState({ ...baseColors });
  // Mapa nombre → id numérico del backend, ej: { DARK: 1, LIGHT: 2, ... }
  const backendIdMap = useRef({});

  // ── Al arrancar: restaurar tema local + cargar mapa de ids del backend ──
  useEffect(() => {
    const init = async () => {
      // 1. Restaurar tema guardado solo si hay sesión activa
      //    Sin sesión → siempre DARK (tema por defecto para usuarios no autenticados)
      const token = await AsyncStorage.getItem("accessToken");
      if (token) {
        const savedId = await AsyncStorage.getItem(STORAGE_KEY);
        if (savedId) {
          const found = THEMES.find((t) => t.id === savedId);
          if (found) {
            setActiveThemeId(found.id);
            setColors({ ...baseColors, ...found.palette });
          }
        }
      } else {
        // Sin sesión: asegurar que el tema sea DARK
        const dark = THEMES.find((t) => t.id === "DARK");
        if (dark) {
          setActiveThemeId("DARK");
          setColors({ ...baseColors, ...dark.palette });
        }
      }

      // 2. Cargar lista de temas del backend para obtener ids numéricos
      try {
        const backendThemes = await getThemes();
        if (Array.isArray(backendThemes)) {
          const map = {};
          backendThemes.forEach((t) => {
            map[t.name] = t.id;
          });
          backendIdMap.current = map;
        }
      } catch (e) {
        // silencioso — el mapa quedará vacío y applyTheme solo aplicará localmente
      }
    };
    init();
  }, []);

  // ── Polling cada 30 segundos — sincroniza con el backend ──
  useEffect(() => {
    const interval = setInterval(async () => {
      try {
        const token = await AsyncStorage.getItem("accessToken");
        if (!token) return;
        const data = await getActivePalette();
        if (!data) return;
        const found = THEMES.find((t) => t.id === data.themeName);
        if (found) {
          setActiveThemeId(found.id);
          setColors({ ...baseColors, ...found.palette });
          await AsyncStorage.setItem(STORAGE_KEY, found.id);
        }
      } catch (e) {
        if (e.message.includes('403')) {
          return;
        }
      }
    }, 30000);
    return () => clearInterval(interval);
  }, []);

  // ── applyTheme: aplica localmente + llama al backend ──
  const applyTheme = async (themeId) => {
    const found = THEMES.find((t) => t.id === themeId);
    if (!found) return;

    // 1. Aplica localmente de inmediato
    setActiveThemeId(found.id);
    setColors({ ...baseColors, ...found.palette });
    await AsyncStorage.setItem(STORAGE_KEY, found.id);

    // 2. Llama al backend con el id numérico
    const numericId = backendIdMap.current[themeId];

    if (numericId) {
      try {
        await activateTheme(numericId);
      } catch (e) {
        if (e.message.includes('403')) {
          return;
        }
      }
    }
  };

  // ── applyPalette: aplica una paleta resuelta directamente (para customizaciones) ──
  const applyPalette = (paletteObj) => {
    if (!paletteObj || typeof paletteObj !== "object") return;
    setColors((prev) => ({ ...prev, ...paletteObj }));
  };

  return (
    <ThemeContext.Provider value={{ colors, palette: colors, activeThemeId, applyTheme, applyPalette, themes: THEMES }}>
      {children}
    </ThemeContext.Provider>
  );
}

export const useTheme = () => {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error("useTheme must be used inside ThemeProvider");
  return ctx;
};
