import { describe, expect, it } from 'vitest';
import {
  distributeSeats,
  filterSeatsByPage,
  indexPolygonSeats,
  rowsInColumnWindow,
} from '../../src/components/layout-editor/layoutEditorUtils';
import { buildShapeGeometry, getSeatSize, seatSizeOf, suggestedSeatSize } from '../../src/components/layout-editor/sectionShapes';

const CELL = 20; // seatRadius 8 + gap 4 → 2·8 + 4

const horseshoe = (targetSeats = 2000) => ({
  ...buildShapeGeometry('horseshoe', 900, 900),
  seatLayout: { targetSeats, seatRadius: 8, gap: 4 },
});

describe('indexPolygonSeats', () => {
  it('numera filas por Y y columnas por posición física, sin cambiar el orden', () => {
    // Dos brazos en la misma fila separados por un hueco de 3 celdas
    const positions = [
      { x: 10, y: 10, r: 8 }, { x: 30, y: 10, r: 8 },
      { x: 110, y: 10, r: 8 },
      { x: 10, y: 30, r: 8 },
    ];
    const { positions: indexed, rowStructure } = indexPolygonSeats(positions, CELL);

    expect(indexed.map((p) => [p.rowIndex, p.colIndex])).toEqual([[0, 0], [0, 1], [0, 5], [1, 0]]);
    expect(indexed.map((p) => p.x)).toEqual([10, 30, 110, 10]);
    expect(rowStructure).toEqual([6, 1]);
  });

  it('sin sillas devuelve estructuras vacías', () => {
    expect(indexPolygonSeats([], CELL)).toEqual({ positions: [], rowStructure: [] });
  });
});

describe('paginación 10 × 10 por columnas físicas', () => {
  const { positions } = distributeSeats(horseshoe());

  it('una página nunca abarca más de 10 columnas físicas, aunque la fila tenga un hueco', () => {
    const maxCol = Math.max(...positions.map((p) => p.colIndex));
    for (let colPage = 0; colPage <= Math.floor(maxCol / 10); colPage++) {
      const page = filterSeatsByPage(positions, colPage, 0);
      if (page.length === 0) continue;
      const xs = page.map(({ pos }) => pos.x);
      expect(Math.max(...xs) - Math.min(...xs)).toBeLessThan(10 * CELL);
      expect(new Set(page.map(({ pos }) => pos.rowIndex)).size).toBeLessThanOrEqual(10);
    }
  });

  it('las páginas cubren todas las sillas exactamente una vez', () => {
    const maxCol = Math.max(...positions.map((p) => p.colIndex));
    const seen = new Map();
    for (let colPage = 0; colPage <= Math.floor(maxCol / 10); colPage++) {
      const rowPages = Math.ceil(rowsInColumnWindow(positions, colPage).length / 10);
      for (let rowPage = 0; rowPage < rowPages; rowPage++) {
        for (const { realIndex } of filterSeatsByPage(positions, colPage, rowPage)) {
          seen.set(realIndex, (seen.get(realIndex) ?? 0) + 1);
        }
      }
    }
    expect(seen.size).toBe(positions.length);
    expect([...seen.values()].every((n) => n === 1)).toBe(true);
  });

  it('realIndex es el índice de la silla en la sección', () => {
    const page = filterSeatsByPage(positions, 0, 0);
    for (const { pos, realIndex } of page) expect(positions[realIndex]).toBe(pos);
  });

  it('sin sillas no hay página', () => {
    expect(filterSeatsByPage([], 0, 0)).toEqual([]);
    expect(filterSeatsByPage(null, 0, 0)).toEqual([]);
  });

  it('en un rectángulo pagina igual que antes: 10 columnas × 10 filas', () => {
    const rect = { shapeMode: 'rect', width: 500, height: 500, seatLayout: { targetSeats: 400, seatRadius: 8, gap: 4 } };
    const { positions: grid } = distributeSeats(rect);
    expect(filterSeatsByPage(grid, 0, 0)).toHaveLength(100);
    expect(filterSeatsByPage(grid, 1, 1).every(({ pos }) => pos.colIndex >= 10 && pos.colIndex < 20)).toBe(true);
  });
});

describe('tamaños de silla', () => {
  it('sugiere sillas más pequeñas según la cantidad', () => {
    expect(suggestedSeatSize(500)).toBe('normal');
    expect(suggestedSeatSize(5000)).toBe('compact');
    expect(suggestedSeatSize(55000)).toBe('mini');
  });

  it('identifica el tamaño de un seatLayout por su radio', () => {
    expect(seatSizeOf({ seatRadius: 5, gap: 3 }).id).toBe('compact');
    expect(seatSizeOf({ seatRadius: 3 }).id).toBe('mini');
    expect(seatSizeOf(undefined).id).toBe('normal');
    expect(getSeatSize('nope').id).toBe('normal');
  });
});
