import { describe, expect, it } from 'vitest';
import { contrastRatio, ensureContrast, readablePalette, MIN_TEXT_CONTRAST, TEXT_TOKENS } from '../../src/theme/contrast';
import { THEMES } from '../../src/theme/themes';

describe('contrastRatio', () => {
  it('negro sobre blanco es 21:1 y un color sobre sí mismo 1:1', () => {
    expect(contrastRatio('#000000', '#FFFFFF')).toBeCloseTo(21, 1);
    expect(contrastRatio('#7C3AED', '#7C3AED')).toBeCloseTo(1, 5);
  });

  it('un color que no es hex de 6 dígitos no se puede medir', () => {
    expect(contrastRatio('var(--x)', '#FFFFFF')).toBeNull();
  });
});

describe('ensureContrast', () => {
  it('deja igual un color que ya se lee', () => {
    expect(ensureContrast('#212121', ['#FFFFFF'], '#000000')).toBe('#212121');
  });

  it('acerca hacia textPrimary un color que no llega a 4.5:1, sin pasarse', () => {
    const fixed = ensureContrast('#9E9E9E', ['#FFFFFF', '#F5F5F5'], '#212121');
    expect(contrastRatio(fixed, '#FFFFFF')).toBeGreaterThanOrEqual(MIN_TEXT_CONTRAST);
    expect(contrastRatio(fixed, '#F5F5F5')).toBeGreaterThanOrEqual(MIN_TEXT_CONTRAST);
    expect(fixed).not.toBe('#212121');
  });

  it('con valores que no son hex (p. ej. personalizados raros) no toca nada', () => {
    expect(ensureContrast('red', ['#FFFFFF'], '#000000')).toBe('red');
    expect(ensureContrast('#9E9E9E', ['transparent'], '#000000')).toBe('#9E9E9E');
  });
});

describe('readablePalette', () => {
  it.each(THEMES.map((t) => [t.id, t.palette]))(
    'en el tema %s todos los colores de texto se leen sobre background y surface',
    (_id, palette) => {
      const readable = readablePalette(palette);
      TEXT_TOKENS.filter((token) => palette[token]).forEach((token) => {
        expect(contrastRatio(readable[token], palette.background)).toBeGreaterThanOrEqual(MIN_TEXT_CONTRAST);
        expect(contrastRatio(readable[token], palette.surface)).toBeGreaterThanOrEqual(MIN_TEXT_CONTRAST);
      });
    },
  );

  it('no toca fondos, primary ni textPrimary', () => {
    const palette = THEMES[0].palette;
    const readable = readablePalette(palette);
    ['background', 'surface', 'surfaceAlt', 'primary', 'primaryDark', 'textPrimary'].forEach((key) => {
      expect(readable[key]).toBe(palette[key]);
    });
  });
});
