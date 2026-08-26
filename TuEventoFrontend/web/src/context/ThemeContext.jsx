import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { getActivePalette, activateTheme as activateThemeService, getThemes } from '../services/themeService';
import { THEMES, DEFAULT_THEME_ID } from '../theme/themes';

// ── Paleta por defecto (tema PRINCIPAL) ─────────────────────────────────────
// Debe coincidir con THEMES[0].palette y con la mobile src/theme/colors.js
export const DEFAULT_PALETTE = THEMES.find(t => t.id === DEFAULT_THEME_ID)?.palette ?? {
  background:    "#1E0A3C",
  surface:       "#2D1B4E",
  surfaceAlt:    "#3D2B5E",
  primary:       "#7C3AED",
  primaryDark:   "#6D28D9",
  accent:        "#A78BFA",
  textPrimary:   "#FFFFFF",
  textSecondary: "#9CA3AF",
  textMuted:     "#6B7280",
  error:         "#EF4444",
  errorBg:       "#EF444422",
  success:       "#059669",
  successBg:     "#05966922",
};

// ── Aplica la paleta como CSS custom properties en :root ─────────────────────
// Esto permite que Tailwind v4 (var(--color-*)) y estilos inline con
// var(--color-*) reaccionen al cambio de tema sin recargar la página.
// También deriva --color-onPrimary: el color de texto con mejor contraste
// sobre el primary del tema (blanco o negro), para usarlo en headers de
// modales y botones que usan primary como fondo.
const applyPaletteToDOM = (palette) => {
  const root = document.documentElement;
  Object.entries(palette).forEach(([key, value]) => {
    if (typeof value === 'string') {
      root.style.setProperty(`--color-${key}`, value);
    }
  });

  // Derivar --color-onPrimary: blanco o negro con mejor contraste sobre el
  // gradiente primary→primaryDark (considerando AMBOS extremos del gradiente,
  // no solo primary). Elige el color cuyo worst-case es mayor.
  // Esto garantiza que texto y botones del Hero pasen WCAG en todos los temas.
  if (palette.primary) {
    const parseLum = (hex) => {
      const h = hex.replace('#', '');
      if (h.length !== 6) return null;
      const r = parseInt(h.slice(0,2),16)/255;
      const g = parseInt(h.slice(2,4),16)/255;
      const b = parseInt(h.slice(4,6),16)/255;
      const ch = c => c <= 0.04045 ? c/12.92 : Math.pow((c+0.055)/1.055, 2.4);
      return 0.2126*ch(r) + 0.7152*ch(g) + 0.0722*ch(b);
    };
    const contrastVsLum = (textLum, bgLum) => {
      const lighter = Math.max(textLum, bgLum);
      const darker  = Math.min(textLum, bgLum);
      return (lighter + 0.05) / (darker + 0.05);
    };

    const lumPrimary     = parseLum(palette.primary);
    const lumPrimaryDark = palette.primaryDark ? parseLum(palette.primaryDark) : lumPrimary;
    const lumWhite = 1.0;
    const lumBlack = 0.0;

    if (lumPrimary !== null && lumPrimaryDark !== null) {
      // Worst-case de blanco sobre el gradiente = mínimo contraste en ambos extremos
      const worstWhite = Math.min(
        contrastVsLum(lumWhite, lumPrimary),
        contrastVsLum(lumWhite, lumPrimaryDark)
      );
      // Worst-case de negro sobre el gradiente = mínimo contraste en ambos extremos
      const worstBlack = Math.min(
        contrastVsLum(lumBlack, lumPrimary),
        contrastVsLum(lumBlack, lumPrimaryDark)
      );
      root.style.setProperty('--color-onPrimary', worstWhite >= worstBlack ? '#ffffff' : '#111111');
    }
  }

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
  // Mapa nombre → id numérico del backend, ej: { DARK: 1, LIGHT: 2, ... }
  const backendIdMap = useRef({});

  // ── Carga inicial: aplica DEFAULT inmediatamente para evitar flash ─────────
  useEffect(() => {
    applyPaletteToDOM(DEFAULT_PALETTE);
  }, []);

  // ── applyPalette: aplica una paleta resuelta directamente ─────────────────
  // Equivalente a mobile.applyPalette — útil para customizaciones en tiempo real.
  const applyPalette = useCallback((paletteObj) => {
    if (!paletteObj || typeof paletteObj !== 'object') return;
    const merged = { ...DEFAULT_PALETTE, ...paletteObj };
    applyPaletteToDOM(merged);
    setPalette(merged);
  }, []);

  // ── refreshPalette: obtiene la paleta activa del backend y la aplica ───────
  const refreshPalette = useCallback(async () => {
    const token = localStorage.getItem('token');
    if (!token) {
      // Sin sesión: resetear al tema PRINCIPAL
      applyPaletteToDOM(DEFAULT_PALETTE);
      setPalette(DEFAULT_PALETTE);
      setActiveThemeId(null);
      return;
    }

    setIsLoadingTheme(true);
    try {
      const data = await getActivePalette();
      if (!data) return;

      const merged = { ...DEFAULT_PALETTE, ...data.palette };
      applyPaletteToDOM(merged);
      setPalette(merged);

      if (data.themeId) {
        setActiveThemeId(data.themeId);
        localStorage.setItem('activeThemeId', data.themeId);
      }
      if (data.themeName) {
        localStorage.setItem('activeThemeName', data.themeName);
      }
    } catch (e) {
      // Cualquier error al obtener la paleta (incluyendo 401/403 en un token
      // recién emitido que aún no validó, red caída, etc.) → fallback al DEFAULT.
      // Esto evita que una paleta estancada de sesión anterior quede aplicada
      // y cause que el fondo/modales muestren colores incorrectos post-login.
      applyPaletteToDOM(DEFAULT_PALETTE);
      setPalette(DEFAULT_PALETTE);
    } finally {
      setIsLoadingTheme(false);
    }
  }, []);

  // ── syncTheme: fuerza sincronización con el backend (para usar post-login) ─
  // Equivalente a mobile.syncTheme.
  const syncTheme = useCallback(async () => {
    const token = localStorage.getItem('token');
    if (!token) return;
    try {
      const data = await getActivePalette();
      if (!data) return;
      const merged = { ...DEFAULT_PALETTE, ...data.palette };
      applyPaletteToDOM(merged);
      setPalette(merged);
      if (data.themeId) {
        setActiveThemeId(data.themeId);
        localStorage.setItem('activeThemeId', data.themeId);
      }
      if (data.themeName) {
        localStorage.setItem('activeThemeName', data.themeName);
      }
    } catch (e) {
      if (e?.message?.includes('403') || e?.message?.includes('401')) return;
    }
  }, []);

  // ── applyTheme: activa un tema por id numérico y refresca la paleta ────────
  // Acepta tanto el id numérico (web) como el nombre string (mobile-compat).
  const applyTheme = useCallback(async (themeIdOrName) => {
    if (!themeIdOrName) return;

    // Si es un string tipo "DARK", buscar el id numérico en el mapa
    let numericId = themeIdOrName;
    if (typeof themeIdOrName === 'string') {
      numericId = backendIdMap.current[themeIdOrName];
      if (!numericId) return;
    }

    try {
      await activateThemeService(numericId);
      await refreshPalette();
    } catch (e) {
      // Error silencioso — la UI lo maneja en ProfilePage
    }
  }, [refreshPalette]);

  // ── resetTheme: vuelve al tema PRINCIPAL (para logout) ────────────────────
  const resetTheme = useCallback(() => {
    applyPaletteToDOM(DEFAULT_PALETTE);
    setPalette(DEFAULT_PALETTE);
    setActiveThemeId(null);
    localStorage.removeItem('activeThemeId');
    localStorage.removeItem('activeThemeName');
  }, []);

  // ── Efecto 1: carga del tema al montar si hay sesión ──────────────────────
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      refreshPalette();
    }
  }, [refreshPalette]);

  // ── Efecto 2: carga el mapa nombre→id del backend (para applyTheme) ───────
  useEffect(() => {
    const loadBackendMap = async () => {
      try {
        const backendThemes = await getThemes();
        if (Array.isArray(backendThemes)) {
          const map = {};
          backendThemes.forEach(t => { map[t.name] = t.id; });
          backendIdMap.current = map;
        }
      } catch {
        // silencioso
      }
    };
    loadBackendMap();
  }, []);

  // ── Efecto 3: sincroniza cuando el token cambia (login/logout en otra pestaña) ──
  useEffect(() => {
    const handleStorage = (e) => {
      if (e.key === 'token') {
        if (e.newValue) {
          refreshPalette();
        } else {
          resetTheme();
        }
      }
    };
    window.addEventListener('storage', handleStorage);
    return () => window.removeEventListener('storage', handleStorage);
  }, [refreshPalette, resetTheme]);

  // ── Efecto 4: polling cada 30 s (sincroniza cambios multi-device) ─────────
  useEffect(() => {
    const interval = setInterval(() => {
      const token = localStorage.getItem('token');
      if (token) refreshPalette();
    }, 30000);
    return () => clearInterval(interval);
  }, [refreshPalette]);

  // `colors` es un alias de `palette` para compatibilidad con el patrón mobile
  // (mobile usa `colors`, web usaba `palette`; ahora ambos están disponibles).
  return (
    <ThemeContext.Provider value={{
      palette,
      colors: palette,        // alias mobile-compat
      isLoadingTheme,
      refreshPalette,
      applyPalette,           // nuevo — espejo de mobile
      applyTheme,
      syncTheme,              // nuevo — espejo de mobile
      resetTheme,
      activeThemeId,
      themes: THEMES,         // nuevo — lista completa de temas
    }}>
      {children}
    </ThemeContext.Provider>
  );
}

export const useTheme = () => {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be used inside ThemeProvider');
  return ctx;
};
