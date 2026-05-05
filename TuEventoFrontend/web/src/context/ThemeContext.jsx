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
  // Fuerza repaint en browsers que cachean CSS variables
  // eslint-disable-next-line no-unused-expressions
  root.offsetHeight;
};

export const ThemeContext = createContext(null);

export function ThemeProvider({ children }) {
  const [palette, setPalette] = useState(DEFAULT_PALETTE);
  const [isLoadingTheme, setIsLoadingTheme] = useState(false);
  const [activeThemeId, setActiveThemeId] = useState(
    parseInt(localStorage.getItem('activeThemeId')) || null
  );

  const refreshPalette = useCallback(async () => {
    const token = localStorage.getItem('token');
    if (!token) return;

    setIsLoadingTheme(true);
    try {
      const data = await getActivePalette();
      if (!data) return;

      const merged = { ...DEFAULT_PALETTE, ...data.palette };
      applyPalette(merged);
      setPalette(merged);
      setActiveThemeId(data.themeId);
      localStorage.setItem('activeThemeId', data.themeId);
      localStorage.setItem('activeThemeName', data.themeName);
    } catch (e) {
      // 403 — token expirado o sesión cerrada: no aplicar fallback, mantener paleta actual
      if (e.message.includes('403') || e.message.includes('Forbidden')) {
        return;
      }
      applyPalette(DEFAULT_PALETTE);
    } finally {
      setIsLoadingTheme(false);
    }
  }, []);

  // Efecto 1 — carga inicial solo si hay token
  useEffect(() => {
    const token = localStorage.getItem('token');
    applyPalette(DEFAULT_PALETTE); // siempre aplica fallback primero
    if (!token) return;
    refreshPalette();
  }, [refreshPalette]);

  // Efecto 2 — detecta cuando el usuario hace login (token aparece en localStorage)
  useEffect(() => {
    const handleStorage = (e) => {
      if (e.key === 'token' && e.newValue) {
        refreshPalette();
      }
    };
    window.addEventListener('storage', handleStorage);
    return () => window.removeEventListener('storage', handleStorage);
  }, [refreshPalette]);

  // Efecto 3 — polling cada 30 segundos (sincroniza cambios desde otros dispositivos)
  useEffect(() => {
    const interval = setInterval(() => {
      const token = localStorage.getItem('token');
      if (token) refreshPalette();
    }, 30000);
    return () => clearInterval(interval);
  }, [refreshPalette]);

  return (
    <ThemeContext.Provider value={{ palette, isLoadingTheme, refreshPalette, activeThemeId }}>
      {children}
    </ThemeContext.Provider>
  );
}

export const useTheme = () => useContext(ThemeContext);
