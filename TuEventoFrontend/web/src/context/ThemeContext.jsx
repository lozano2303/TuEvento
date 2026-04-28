import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { getActivePalette } from '../services/themeService';

const DEFAULT_PALETTE = {
  background: "#1E0A3C",
  surface: "#2D1B4E",
  surfaceAlt: "#3D2B5E",
  primary: "#7C3AED",
  primaryDark: "#6D28D9",
  accent: "#A78BFA",
  textPrimary: "#FFFFFF",
  textSecondary: "#9CA3AF",
  textMuted: "#6B7280",
  error: "#EF4444",
  errorBg: "#EF444422",
  success: "#059669",
  successBg: "#05966922",
  borderRadius: "8px",
  fontFamily: "Inter",
};

const applyPalette = (palette) => {
  const root = document.documentElement;
  Object.entries(palette).forEach(([key, value]) => {
    if (!Array.isArray(value)) {
      root.style.setProperty(`--color-${key}`, value);
    }
  });
};

export const ThemeContext = createContext(null);

export function ThemeProvider({ children }) {
  const [palette, setPalette] = useState(DEFAULT_PALETTE);
  const [isLoadingTheme, setIsLoadingTheme] = useState(false);

  const refreshPalette = useCallback(async () => {
    // Aplica el fallback inmediatamente para evitar flash
    applyPalette(DEFAULT_PALETTE);

    setIsLoadingTheme(true);
    try {
      const remotePalette = await getActivePalette();
      if (remotePalette) {
        const merged = { ...DEFAULT_PALETTE, ...remotePalette };
        applyPalette(merged);
        setPalette(merged);
      }
    } catch (e) {
      console.warn('[ThemeContext] No se pudo cargar la paleta remota, usando fallback:', e.message);
      // Fallback ya aplicado arriba — la app no se rompe
    } finally {
      setIsLoadingTheme(false);
    }
  }, []);

  // Al montar: aplica fallback de inmediato y luego intenta el fetch
  useEffect(() => {
    refreshPalette();
  }, [refreshPalette]);

  return (
    <ThemeContext.Provider value={{ palette, isLoadingTheme, refreshPalette }}>
      {children}
    </ThemeContext.Provider>
  );
}

export const useTheme = () => useContext(ThemeContext);
