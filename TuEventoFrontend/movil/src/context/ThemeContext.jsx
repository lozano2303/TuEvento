import { createContext, useContext, useState, useEffect, useRef } from "react";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { colors as baseColors } from "../theme/colors";
import { THEMES, DEFAULT_THEME_ID } from "../theme/themes";
import { activateTheme, getActivePalette, getThemes } from "../services/themeService";
import { useAuth } from "./AuthContext";

const STORAGE_KEY = "activeThemeId";

const ThemeContext = createContext(null);

export function ThemeProvider({ children }) {
  const { user } = useAuth();
  const [activeThemeId, setActiveThemeId] = useState(DEFAULT_THEME_ID);
  // Estado inicial siempre PRINCIPAL
  const principalPalette = THEMES.find((t) => t.id === "PRINCIPAL")?.palette ?? baseColors;
  const [colors, setColors] = useState({ ...baseColors, ...principalPalette });
  // Mapa nombre → id numérico del backend, ej: { DARK: 1, LIGHT: 2, ... }
  const backendIdMap = useRef({});

  // ── Reacciona a cambios de sesión: resetea a PRINCIPAL en logout / sin sesión ──
  useEffect(() => {
    if (!user) {
      // Sin sesión — resetear al tema PRINCIPAL
      const principalTheme = THEMES.find((t) => t.id === "PRINCIPAL");
      if (principalTheme) {
        setActiveThemeId("PRINCIPAL");
        setColors({ ...baseColors, ...principalTheme.palette });
      }
      return;
    }

    // Con sesión — cargar tema del usuario desde backend
    const loadTheme = async () => {
      try {
        let token = await AsyncStorage.getItem("accessToken");
        if (!token) {
          await new Promise((resolve) => setTimeout(resolve, 500));
          token = await AsyncStorage.getItem("accessToken");
        }
        if (!token) return;
        const data = await getActivePalette(token);
        if (data) {
          const found = THEMES.find((t) => t.id === data.themeName);
          if (found) {
            setActiveThemeId(found.id);
            await AsyncStorage.setItem(STORAGE_KEY, found.id);
          }
          if (data.palette) {
            applyPalette(data.palette);
          } else {
            applyPalette(data);
          }
        }
      } catch (e) {
        if (e?.message?.includes("403")) return;
        // Fallback offline: usar el tema guardado localmente
        const savedId = await AsyncStorage.getItem(STORAGE_KEY);
        if (savedId) {
          const found = THEMES.find((t) => t.id === savedId);
          if (found) {
            setActiveThemeId(found.id);
            setColors({ ...baseColors, ...found.palette });
          }
        }
      }
    };
    loadTheme();
  }, [user]);

  // ── Al arrancar: cargar mapa nombre → id numérico del backend para applyTheme ──
  useEffect(() => {
    const loadBackendMap = async () => {
      try {
        const backendThemes = await getThemes();
        if (Array.isArray(backendThemes)) {
          const map = {};
          backendThemes.forEach((t) => { map[t.name] = t.id; });
          backendIdMap.current = map;
        }
      } catch (e) {
        // silencioso
      }
    };
    loadBackendMap();
  }, []);

  // ── Polling cada 30 segundos — sincroniza paleta resuelta con el backend ──
  useEffect(() => {
    if (!user) return; // sin sesión no hacer polling
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
  }, [user]);

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

  // ── syncTheme: fuerza sincronización con el backend (para usar post-login) ──
  const syncTheme = async () => {
    try {
      const token = await AsyncStorage.getItem("accessToken");
      if (!token) return;
      const data = await getActivePalette();
      if (!data) return;
      const found = THEMES.find((t) => t.id === data.themeName);
      if (found) {
        setActiveThemeId(found.id);
        await AsyncStorage.setItem(STORAGE_KEY, found.id);
      }
      if (data.palette) {
        setColors({ ...baseColors, ...data.palette });
      }
    } catch (e) {
      if (e?.message?.includes("403")) return;
    }
  };

  return (
    <ThemeContext.Provider value={{ colors, palette: colors, activeThemeId, applyTheme, applyPalette, syncTheme, themes: THEMES }}>
      {children}
    </ThemeContext.Provider>
  );
}

export const useTheme = () => {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error("useTheme must be used inside ThemeProvider");
  return ctx;
};
