/**
 * Contraste de texto según WCAG 2.1. Los temas (y las paletas que personaliza cada usuario)
 * no siempre cumplen 4.5:1 en los colores de texto secundarios; readablePalette los ajusta
 * lo justo para que se lean sobre el fondo y las tarjetas de su propio tema.
 */

/** Contraste mínimo para texto normal (WCAG AA). */
export const MIN_TEXT_CONTRAST = 4.5;

/** Colores que se usan como texto y deben leerse sobre background y surface. */
export const TEXT_TOKENS = ['textSecondary', 'textMuted', 'accent', 'error', 'success'];

const parseHex = (hex) => {
  if (typeof hex !== 'string') return null;
  const h = hex.replace('#', '');
  if (!/^[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$/.test(h)) return null;
  return [0, 2, 4].map((i) => parseInt(h.slice(i, i + 2), 16));
};

const toHex = (rgb) => `#${rgb.map((c) => Math.round(c).toString(16).padStart(2, '0')).join('')}`.toUpperCase();

export const luminance = (hex) => {
  const rgb = parseHex(hex);
  if (!rgb) return null;
  const [r, g, b] = rgb.map((c) => {
    const s = c / 255;
    return s <= 0.04045 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
};

export const contrastRatio = (a, b) => {
  const la = luminance(a);
  const lb = luminance(b);
  if (la === null || lb === null) return null;
  return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
};

/** Mezcla `color` con `target` (0 = color, 1 = target). */
const mix = (color, target, amount) => {
  const a = parseHex(color);
  const b = parseHex(target);
  return toHex(a.map((c, i) => c + (b[i] - c) * amount));
};

/**
 * Devuelve `color` o, si no llega al contraste mínimo sobre todos los fondos, la mezcla más
 * cercana hacia `towards` (normalmente textPrimary, que ya contrasta con esos fondos) que sí llega.
 */
export const ensureContrast = (color, backgrounds, towards, min = MIN_TEXT_CONTRAST) => {
  if (!parseHex(color) || !parseHex(towards)) return color;
  const valid = backgrounds.filter((bg) => parseHex(bg));
  if (valid.length === 0) return color;
  const worst = (c) => Math.min(...valid.map((bg) => contrastRatio(c, bg)));
  if (worst(color) >= min) return color;
  for (let step = 1; step <= 20; step++) {
    const candidate = mix(color, towards, step / 20);
    if (worst(candidate) >= min) return candidate;
  }
  return towards;
};

/**
 * Copia de la paleta con los colores de texto ajustados para leerse sobre background y surface.
 * No toca los fondos, primary (lo cubre onPrimary) ni textPrimary.
 */
export const readablePalette = (palette) => {
  const backgrounds = [palette.background, palette.surface];
  const towards = palette.textPrimary;
  const out = { ...palette };
  TEXT_TOKENS.forEach((token) => {
    if (palette[token]) out[token] = ensureContrast(palette[token], backgrounds, towards);
  });
  return out;
};
