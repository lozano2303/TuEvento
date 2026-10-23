/**
 * colorUtils.js — pure-JS WCAG 2.1 contrast helpers, no dependencies.
 *
 * All functions are pure (no side effects) so they can be used in both
 * component logic and unit tests without a DOM.
 */

// ── Parse any supported colour string to [r, g, b] in [0, 255] ──────────────
// Supports: #RGB, #RRGGBB, #RRGGBBAA, rgb(), rgba()
export function parseColor(color) {
  if (!color || typeof color !== 'string') return null;
  const s = color.trim();

  // Hex forms
  const hex3 = s.match(/^#([0-9a-f]{3})$/i);
  if (hex3) {
    const [r, g, b] = hex3[1].split('').map(c => parseInt(c + c, 16));
    return [r, g, b];
  }
  const hex6 = s.match(/^#([0-9a-f]{6})([0-9a-f]{2})?$/i);
  if (hex6) {
    const h = hex6[1];
    return [
      parseInt(h.slice(0, 2), 16),
      parseInt(h.slice(2, 4), 16),
      parseInt(h.slice(4, 6), 16),
    ];
  }

  // rgb() / rgba()
  const rgb = s.match(/^rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)/i);
  if (rgb) return [parseInt(rgb[1]), parseInt(rgb[2]), parseInt(rgb[3])];

  return null;
}

// ── Relative luminance per WCAG 2.1 §1.4.3 ──────────────────────────────────
export function relativeLuminance([r, g, b]) {
  const channel = (v) => {
    const c = v / 255;
    return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
  };
  return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
}

// ── Contrast ratio (1–21) ─────────────────────────────────────────────────────
export function contrastRatio(colorA, colorB) {
  const a = parseColor(colorA);
  const b = parseColor(colorB);
  if (!a || !b) return null;
  const lumA = relativeLuminance(a);
  const lumB = relativeLuminance(b);
  const lighter = Math.max(lumA, lumB);
  const darker  = Math.min(lumA, lumB);
  return (lighter + 0.05) / (darker + 0.05);
}

/**
 * WCAG thresholds:
 *   AA normal text  → 4.5 : 1
 *   AA large text   → 3.0 : 1
 *   AAA normal text → 7.0 : 1
 *
 * We use 4.5 as the default minimum because accent is used for body-size
 * links and badges.
 */
export const WCAG_AA = 4.5;
export const WCAG_AA_LARGE = 3.0;

// ── Describe the contrast level ───────────────────────────────────────────────
export function contrastLevel(ratio) {
  if (ratio === null) return 'unknown';
  if (ratio >= 7.0)  return 'aaa';
  if (ratio >= 4.5)  return 'aa';
  if (ratio >= 3.0)  return 'aa-large';
  return 'fail';
}

// ── Lighten or darken a hex colour by a factor (0–1) ─────────────────────────
// factor > 0 → lighten, factor < 0 → darken
function adjustLightness(hex, factor) {
  const rgb = parseColor(hex);
  if (!rgb) return hex;
  const [r, g, b] = rgb.map(c => Math.round(Math.min(255, Math.max(0, c + 255 * factor))));
  return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
}

/**
 * suggestAccent(primary, background)
 *
 * Given the current `primary` and `background` tokens, derives an accent
 * colour that:
 *   1. Is visually related to `primary` (lightened/darkened variant)
 *   2. Achieves WCAG AA (≥4.5:1) against `background`
 *   3. Is returned as a 6-digit hex string
 *
 * Strategy: start from `primary`, progressively lighten or darken until the
 * contrast target is reached (or we give up after 40 steps).
 */
export function suggestAccent(primary, background) {
  if (!primary || !background) return null;

  const bgRgb = parseColor(background);
  if (!bgRgb) return null;

  // Determine whether background is dark or light to choose direction
  const bgLum = relativeLuminance(bgRgb);
  const lightenFirst = bgLum < 0.18; // dark background → lighten the accent

  let candidate = primary;
  const step = 0.04; // ~10 steps to go from 0% to 40% adjustment

  for (let i = 1; i <= 40; i++) {
    const factor = step * i * (lightenFirst ? 1 : -1);
    candidate = adjustLightness(primary, factor);
    const ratio = contrastRatio(candidate, background);
    if (ratio !== null && ratio >= WCAG_AA) return candidate;
  }

  // Fallback: try the opposite direction
  for (let i = 1; i <= 40; i++) {
    const factor = step * i * (lightenFirst ? -1 : 1);
    candidate = adjustLightness(primary, factor);
    const ratio = contrastRatio(candidate, background);
    if (ratio !== null && ratio >= WCAG_AA) return candidate;
  }

  return null; // cannot find a passing accent (edge case)
}
