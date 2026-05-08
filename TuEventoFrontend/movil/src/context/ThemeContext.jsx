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

  // ── Al arrancar: obtener paleta resuelta del backend (incluye customizaciones) ──
  useEffect(() => {
    const init = async () => {
      const token = await AsyncStorage.getItem("accessToken");

      if (token) {
        // Con sesión: la fuente de verdad es siempre el backend.
        // getActivePalette retorna la paleta resuelta con customizaciones aplicadas.
        try {
          const data = await getActivePalette();
          if (data) {
            // Actualizar activeThemeId desde el nombre que devuelve el backend
            const found = THEMES.find((t) => t.id === data.themeName);
            if (found) {
              setActiveThemeId(found.id);
              await AsyncStorage.setItem(STORAGE_KEY, found.id);
            }
            // Aplicar la paleta resuelta — nunca usar colores locales si hay sesión
            if (data.palette) {
              setColors({ ...baseColors, ...data.palette });
            }
          }
        } catch (e) {
          // Fallback offline: usar el tema guardado localmente sin customizaciones
          const savedId = await AsyncStorage.getItem(STORAGE_KEY);
          if (savedId) {
            const found = THEMES.find((t) => t.id === savedId);
            if (found) {
              setActiveThemeId(found.id);
              setColors({ ...baseColors, ...found.palette });
            }
          }
        }
      } else {
        // Sin sesión: tema DARK por defecto (no hay customizaciones que preservar)
        const dark = THEMES.find((t) => t.id === "DARK");
        if (dark) {
          setActiveThemeId("DARK");
          setColors({ ...baseColors, ...dark.palette });
        }
      }

      // Cargar mapa nombre → id numérico del backend para applyTheme
      try {
        const backendThemes = await getThemes();
        if (Array.isArray(backendThemes)) {
          const map = {};
          backendThemes.forEach((t) => { map[t.name] = t.id; });
          backendIdMap.current = map;
        }
      } catch (e) {
        // silencioso — applyTheme solo activará sin confirmar con backend
      }
    };
    init();
  }, []);

  // ── Polling cada 30 segundos — sincroniza paleta resuelta con el backend ──
  useEffect(() => {
    const interval = setInterval(async () => {
      try {
        const token = await AsyncStorage.getItem("accessToken");
        if (!token) return;
        const data = await getActivePalette();
        if (!data) return;
        // Actualizar activeThemeId si cambió
        const found = THEMES.find((t) => t.id === data.themeName);
        if (found) {
          setActiveThemeId(found.id);
          await AsyncStorage.setItem(STORAGE_KEY, found.id);
        }
        // Aplicar la paleta resuelta — preserva customizaciones
        if (data.palette) {
          setColors({ ...baseColors, ...data.palette });
        }
      } catch (e) {
        if (e?.message?.includes("403")) return;
        // otros errores: silencioso, se reintenta en el próximo ciclo
      }
    }, 30000);
    return () => clearInterval(interval);
  }, []);

  // ── applyTheme: activa un tema diferente en el backend y obtiene la paleta resuelta ──
  const applyTheme = async (themeId) => {
    const numericId = backendIdMap.current[themeId];
    if (!numericId) return;

    try {
      // 1. Activar el tema en el backend
      await activateTheme(numericId);

      // 2. Obtener la paleta resuelta post-activación (incluye customizaciones del nuevo tema)
      const data = await getActivePalette();
      if (data) {
        const found = THEMES.find((t) => t.id === data.themeName);
        if (found) {
          setActiveThemeId(found.id);
          await AsyncStorage.setItem(STORAGE_KEY, found.id);
        }
        if (data.palette) {
          setColors({ ...baseColors, ...data.palette });
        }
      }
    } catch (e) {
      if (e?.message?.includes("403")) return;
      // Fallback optimista: aplicar colores locales si el backend falla
      const found = THEMES.find((t) => t.id === themeId);
      if (found) {
        setActiveThemeId(found.id);
        setColors({ ...baseColors, ...found.palette });
        await AsyncStorage.setItem(STORAGE_KEY, found.id);
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
