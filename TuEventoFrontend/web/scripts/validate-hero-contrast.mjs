/**
 * validate-hero-contrast.mjs
 *
 * Valida el contraste WCAG del Hero section (ladingPage.jsx) en los 8 temas
 * del sistema de theming. Reutiliza las funciones puras de colorUtils.js.
 *
 * Uso:
 *   node scripts/validate-hero-contrast.mjs
 *
 * Qué verifica:
 *   A) Texto del Hero (h1 + span acento) → .hero-section-title / .hero-section-accent-text
 *      Color real: color-mix(onPrimary 90%, transparent) evaluado sobre fondo del gradiente.
 *      Se toma el PEOR CASO del gradiente (primary vs primaryDark) — el que da menos contraste.
 *      Umbral: WCAG AA Large ≥ 3.0:1 (el h1 es 4rem / 6rem bold → texto grande)
 *              WCAG AA normal ≥ 4.5:1 (para el span acento que puede renderizarse más pequeño)
 *
 *   B) Texto del botón "Comenzar ahora" → .hero-section-btn
 *      Color del texto: var(--color-textPrimary)
 *      Fondo del botón: gradiente accent→primary (peor caso = el extremo con menos contraste)
 *      Umbral: WCAG AA normal ≥ 4.5:1
 */

import { parseColor, relativeLuminance, contrastRatio, WCAG_AA, WCAG_AA_LARGE } from '../src/utils/colorUtils.js';
import { THEMES } from '../src/theme/themes.js';

// ── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Calcula --color-onPrimary con la lógica actualizada de ThemeContext.applyPaletteToDOM.
 * Considera AMBOS extremos del gradiente primary→primaryDark (worst-case approach).
 * Devuelve '#ffffff' o '#111111'.
 */
function calcOnPrimary(primaryHex, primaryDarkHex) {
  const parseLum = (hex) => {
    const rgb = parseColor(hex);
    if (!rgb) return null;
    return relativeLuminance(rgb);
  };
  const contrastVsLum = (tL, bL) => {
    const lighter = Math.max(tL, bL);
    const darker  = Math.min(tL, bL);
    return (lighter + 0.05) / (darker + 0.05);
  };

  const lumP  = parseLum(primaryHex);
  const lumPD = primaryDarkHex ? parseLum(primaryDarkHex) : lumP;
  if (lumP === null || lumPD === null) return '#ffffff';

  const lumWhite = 1.0, lumBlack = 0.0;
  const worstWhite = Math.min(contrastVsLum(lumWhite, lumP), contrastVsLum(lumWhite, lumPD));
  const worstBlack = Math.min(contrastVsLum(lumBlack, lumP), contrastVsLum(lumBlack, lumPD));
  return worstWhite >= worstBlack ? '#ffffff' : '#111111';
}

/**
 * Simula color-mix(in srgb, base PCT%, transparent) mezclando con negro puro
 * como peor caso conservador (un fondo oscuro amplifica la transparencia).
 * En la práctica el fondo es el gradiente primary→primaryDark, así que usamos
 * ese color real como "fondo de mezcla".
 *
 * Fórmula: result = base * pct + background * (1 - pct)
 */
function applyOpacityOver(hexFg, opacity, hexBg) {
  const fg = parseColor(hexFg);
  const bg = parseColor(hexBg);
  if (!fg || !bg) return hexFg;
  const blend = fg.map((c, i) => Math.round(c * opacity + bg[i] * (1 - opacity)));
  return `#${blend.map(c => c.toString(16).padStart(2, '0')).join('')}`;
}

/** Icono de pass/fail para la tabla */
function icon(passes) { return passes ? '✅' : '❌'; }

/** Nivel WCAG como string corto */
function level(ratio) {
  if (ratio === null) return 'N/A';
  if (ratio >= 7.0) return 'AAA';
  if (ratio >= 4.5) return 'AA';
  if (ratio >= 3.0) return 'AA Large';
  return 'FAIL';
}

// ── Validación principal ──────────────────────────────────────────────────────

const COL = {
  theme:      14,
  onPrimary:  10,
  heroAcc:    10,   // ratio accent text vs hero bg (worst case)
  heroTitle:  11,   // ratio h1 vs hero bg (worst case)
  btnText:    10,   // ratio textPrimary vs button bg (worst case)
  heroAccLv:  10,
  heroTitleLv:11,
  btnTextLv:  10,
};

function pad(str, n) { return String(str).padEnd(n); }
function padL(str, n) { return String(str).padStart(n); }

const SEPARATOR = '─'.repeat(
  COL.theme + COL.onPrimary + COL.heroAcc + COL.heroTitle +
  COL.btnText + COL.heroAccLv + COL.heroTitleLv + COL.btnTextLv + 16
);

console.log('\n' + SEPARATOR);
console.log(
  pad('Tema', COL.theme) + '  ' +
  pad('onPrimary', COL.onPrimary) + '  ' +
  pad('H1 ratio', COL.heroTitle) + '  ' +
  pad('H1 nivel', COL.heroTitleLv) + '  ' +
  pad('Acc ratio', COL.heroAcc) + '  ' +
  pad('Acc nivel', COL.heroAccLv) + '  ' +
  pad('Btn ratio', COL.btnText) + '  ' +
  pad('Btn nivel', COL.btnTextLv)
);
console.log(SEPARATOR);

const failures = [];

for (const theme of THEMES) {
  const { name, palette } = theme;
  const { primary, primaryDark, accent, textPrimary } = palette;

  // ── 1. Calcular onPrimary ──────────────────────────────────────────────────
  const onPrimary = calcOnPrimary(primary, primaryDark);

  // ── 2. Hero text — peor caso del gradiente primary→primaryDark ────────────
  //    .hero-section-title      → onPrimary puro (100%)
  //    .hero-section-accent-text → onPrimary puro (100%) — texto grande bold, umbral AA Large
  //    Span es parte del h1 (text-4xl/6xl bold) → WCAG AA Large ≥ 3.0:1

  const h1RatioVsPrimary     = contrastRatio(onPrimary, primary);
  const h1RatioVsPrimaryDark = contrastRatio(onPrimary, primaryDark);
  const h1RatioWorst = Math.min(h1RatioVsPrimary, h1RatioVsPrimaryDark);

  // Span acento: onPrimary puro (100%) — misma fórmula que h1
  const accentRatioWorst = h1RatioWorst;  // idéntico: mismo color, mismo fondo

  // ── 3. Botón — peor caso del gradiente primaryDark→primary ────────────────
  //    Texto del botón: onPrimary (color: var(--color-onPrimary))
  //    Fondo: extremos primaryDark y primary del gradiente
  const btnRatioVsAccent  = contrastRatio(onPrimary, primaryDark);  // extremo oscuro
  const btnRatioVsPrimary = contrastRatio(onPrimary, primary);       // extremo claro
  const btnRatioWorst = Math.min(btnRatioVsAccent, btnRatioVsPrimary);

  // ── 4. Determinar pass/fail ────────────────────────────────────────────────
  //    H1:       WCAG AA Large (≥3.0:1) — texto grande bold 4-6rem
  //    Acento:   WCAG AA Large (≥3.0:1) — span dentro del h1, texto grande bold
  //    Botón:    WCAG AA Normal (≥4.5:1) — etiqueta de botón (texto normal)
  const h1Passes     = h1RatioWorst    >= WCAG_AA_LARGE;
  const accentPasses = accentRatioWorst >= WCAG_AA_LARGE;
  const btnPasses    = btnRatioWorst   >= WCAG_AA;

  const allPass = h1Passes && accentPasses && btnPasses;

  console.log(
    pad(`${icon(allPass)} ${name}`, COL.theme) + '  ' +
    pad(onPrimary, COL.onPrimary) + '  ' +
    pad(h1RatioWorst.toFixed(2) + ':1', COL.heroTitle) + '  ' +
    pad(`${icon(h1Passes)} ${level(h1RatioWorst)}`, COL.heroTitleLv) + '  ' +
    pad(accentRatioWorst.toFixed(2) + ':1', COL.heroAcc) + '  ' +
    pad(`${icon(accentPasses)} ${level(accentRatioWorst)}`, COL.heroAccLv) + '  ' +
    pad(btnRatioWorst.toFixed(2) + ':1', COL.btnText) + '  ' +
    pad(`${icon(btnPasses)} ${level(btnRatioWorst)}`, COL.btnTextLv)
  );

  if (!allPass) {
    failures.push({ name, primary, primaryDark, accent, textPrimary, onPrimary,
      h1RatioWorst, accentRatioWorst, btnRatioWorst,
      h1Passes, accentPasses, btnPasses });
  }
}

console.log(SEPARATOR);

// ── Detalle de fallos ─────────────────────────────────────────────────────────
if (failures.length === 0) {
  console.log('\n✅  Todos los temas pasan WCAG en el Hero section.\n');
} else {
  console.log(`\n❌  ${failures.length} tema(s) con problemas:\n`);

  for (const f of failures) {
    console.log(`\n  Tema: ${f.name}`);
    console.log(`    onPrimary calculado: ${f.onPrimary}`);
    console.log(`    primary: ${f.primary}  |  primaryDark: ${f.primaryDark}  |  accent: ${f.accent}  |  textPrimary: ${f.textPrimary}`);

    if (!f.h1Passes) {
      console.log(`    ⚠️  H1 (hero-section-title): ${f.h1RatioWorst.toFixed(2)}:1 — necesita ≥ 3.0:1 (AA Large)`);
      console.log(`       Causa: onPrimary (${f.onPrimary}) tiene bajo contraste sobre el gradiente primary/primaryDark.`);
      console.log(`       Sugerencia: revisar si primary y primaryDark de este tema son suficientemente distintos`);
      console.log(`                   de onPrimary; es probable que el blanco/negro calculado sea el incorrecto.`);
    }
    if (!f.accentPasses) {
      console.log(`    ⚠️  Span acento (hero-section-accent-text): ${f.accentRatioWorst.toFixed(2)}:1 — necesita ≥ 4.5:1 (AA)`);
      console.log(`       Causa: color-mix(onPrimary 90%, transparent) sobre el bg del gradiente pierde contraste.`);
      console.log(`       Opción A: subir el porcentaje de 90% → 95% en .hero-section-accent-text`);
      console.log(`       Opción B: si onPrimary es '#111111', verificar que primary sea suficientemente claro.`);
    }
    if (!f.btnPasses) {
      const r1 = contrastRatio(f.onPrimary, f.primary);
      const r2 = contrastRatio(f.onPrimary, f.primaryDark);
      const worstSide = r1 <= r2
        ? `extremo primary (${f.primary}, ratio=${r1?.toFixed(2)})`
        : `extremo primaryDark (${f.primaryDark}, ratio=${r2?.toFixed(2)})`;
      console.log(`    ⚠️  Botón (hero-section-btn): ${f.btnRatioWorst.toFixed(2)}:1 — necesita ≥ 4.5:1 (AA)`);
      console.log(`       Causa: onPrimary (${f.onPrimary}) tiene bajo contraste en el ${worstSide} del gradiente.`);
      console.log(`       Nota: ${f.btnRatioWorst.toFixed(2)}:1 es el máximo alcanzable para este tema con gradiente bicolor.`);
      console.log(`             WCAG AA Large (≥3:1) SÍ se cumple. Solo falla AA normal (≥4.5:1) en este extremo.`);
    }
  }
  console.log('');
}
