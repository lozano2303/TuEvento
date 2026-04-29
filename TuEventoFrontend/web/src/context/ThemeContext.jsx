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
  
  // Log de verificación
  console.log('[ThemeContext] Palette applied:', {
    background: root.style.getPropertyValue('--color-background'),
    primary: root.style.getPropertyValue('--color-primary'),
    accent: root.style.getPropertyValue('--color-accent')
  });
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
    if (!token) {
      console.warn('[ThemeContext] No token available, skipping fetch');
      return;
    }

    setIsLoadingTheme(true);
    try {
      const data = await getActivePalette();
      if (data) {
        const merged = { ...DEFAULT_PALETTE, ...data.palette };
        applyPalette(merged);
        setPalette(merged);
        setActiveThemeId(data.themeId);
        localStorage.setItem('activeThemeId', data.themeId);
        localStorage.setItem('activeThemeName', data.themeName);
      }
    } catch (e) {
      console.warn('[ThemeContext] No se pudo cargar la paleta remota, usando fallback:', e.message);
      // Fallback ya aplicado arriba — la app no se rompe
    } finally {
      setIsLoadingTheme(false);
    }
  }, []);

  // Efecto 1 — carga inicial solo si hay token
  useEffect(() => {
    const token = localStorage.getItem('token');
    applyPalette(DEFAULT_PALETTE); // siempre aplica fallback primero
    if (!token) return; // sin token no hacer fetch
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

  // Efecto 3 — polling cada 30 segundos
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
