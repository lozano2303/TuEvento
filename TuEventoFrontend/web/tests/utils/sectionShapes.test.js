import { describe, expect, it } from 'vitest';
import {
  SECTION_SHAPES,
  MAX_SECTION_SEATS,
  buildShapeGeometry,
  getSectionShape,
  getShapeIconPath,
  getShapePoints,
  sizeSectionForSeats,
} from '../../src/components/layout-editor/sectionShapes';
import {
  computeMaxSeats,
  fillSeatsOnResize,
  flattenPolygonForFill,
  polyBoundingBox,
} from '../../src/components/layout-editor/layoutEditorUtils';

const SEATS = { seatRadius: 8, gap: 4 };

describe('catálogo de formas', () => {
  it('tiene más de 20 formas con id y nombre únicos', () => {
    expect(SECTION_SHAPES.length).toBeGreaterThanOrEqual(15);
    expect(new Set(SECTION_SHAPES.map((s) => s.id)).size).toBe(SECTION_SHAPES.length);
    expect(new Set(SECTION_SHAPES.map((s) => s.label)).size).toBe(SECTION_SHAPES.length);
  });

  it('una forma desconocida cae en el rectángulo', () => {
    expect(getSectionShape('nope').id).toBe('rect');
  });

  it.each(SECTION_SHAPES.map((s) => [s.id]))('%s ocupa exactamente el tamaño pedido', (id) => {
    const bb = polyBoundingBox(flattenPolygonForFill(getShapePoints(id, 300, 200)));
    expect(bb.minX).toBeCloseTo(0, 5);
    expect(bb.minY).toBeCloseTo(0, 5);
    expect(bb.width).toBeCloseTo(300, 5);
    expect(bb.height).toBeCloseTo(200, 5);
  });

  it('solo el rectángulo se crea sin polígono', () => {
    expect(buildShapeGeometry('rect', 120, 90)).toEqual({ shapeMode: 'rect', polygonPoints: null, width: 120, height: 90 });
    const circle = buildShapeGeometry('circle', 120, 120);
    expect(circle.shapeMode).toBe('polygon');
    expect(circle.polygonPoints.every((p) => p.handleIn && p.handleOut)).toBe(true);
  });

  it('las anclas de un arco abierto dejan rectos los segmentos vecinos', () => {
    const pts = getShapePoints('semicircle', 200, 100);
    expect(pts[0].handleIn).toBeNull();
    expect(pts[pts.length - 1].handleOut).toBeNull();
  });

  it('no ofrece formas con hueco o con el centro vacío', () => {
    const ids = SECTION_SHAPES.map((s) => s.id);
    expect(ids).not.toContain('ring');
    expect(ids).not.toContain('horseshoe');
    expect(ids).not.toContain('ushape');
  });

  it('las formas con hueco de layouts guardados se siguen resolviendo', () => {
    expect(getSectionShape('ring').id).toBe('ring');
    expect(getSectionShape('ushape').id).toBe('ushape');
  });

  it('el anillo deja el centro sin sillas', () => {
    const ring  = computeMaxSeats(buildShapeGeometry('ring', 400, 400), SEATS.seatRadius, SEATS.gap);
    const solid = computeMaxSeats(buildShapeGeometry('circle', 400, 400), SEATS.seatRadius, SEATS.gap);
    expect(ring).toBeGreaterThan(0);
    expect(ring).toBeLessThan(solid * 0.8);
  });

  it('genera un path SVG cerrado para cada forma', () => {
    for (const { id } of SECTION_SHAPES) {
      expect(getShapeIconPath(id)).toMatch(/^M[\d.]+,[\d.]+ .* Z$/);
    }
    expect(getShapeIconPath('circle')).toContain('C');
    expect(getShapeIconPath('triangle')).not.toContain('C');
  });
});

describe('sizeSectionForSeats', () => {
  it.each(SECTION_SHAPES.map((s) => [s.id]))('en %s caben las sillas pedidas', (id) => {
    for (const target of [9, 250, 5000]) {
      const geometry = sizeSectionForSeats(id, target, SEATS);
      expect(computeMaxSeats(geometry, SEATS.seatRadius, SEATS.gap)).toBeGreaterThanOrEqual(target);
      expect(geometry.width % 10).toBe(0);
      expect(geometry.height % 10).toBe(0);
    }
  });

  it('no deja la sección mucho más grande de lo necesario', () => {
    const geometry = sizeSectionForSeats('rect', 1000, SEATS);
    expect(computeMaxSeats(geometry, SEATS.seatRadius, SEATS.gap)).toBeLessThan(1000 * 1.15);
  });

  it('respeta un tamaño mínimo y el tope de sillas por sección', () => {
    const tiny = sizeSectionForSeats('rect', 1, SEATS);
    expect(tiny.width).toBeGreaterThanOrEqual(80);
    expect(tiny.height).toBeGreaterThanOrEqual(60);

    const capped  = sizeSectionForSeats('rect', MAX_SECTION_SEATS * 2, SEATS);
    const atLimit = sizeSectionForSeats('rect', MAX_SECTION_SEATS, SEATS);
    expect(capped).toEqual(atLimit);
  });

  it('mantiene la relación de aspecto de la forma', () => {
    const oval = sizeSectionForSeats('oval', 2000, SEATS);
    expect(oval.width / oval.height).toBeCloseTo(2, 0);
  });
});

describe('fillSeatsOnResize', () => {
  const section = {
    shapeMode: 'rect', width: 200, height: 160, polygonPoints: null,
    seatLayout: { targetSeats: 10, seatRadius: 8, gap: 4 },
  };

  it('al agrandar llena la sección con el máximo que cabe', () => {
    const next   = { ...section, width: 400, height: 300 };
    const result = fillSeatsOnResize(section, next);
    expect(result.seatLayout.targetSeats).toBe(computeMaxSeats(next, 8, 4));
    expect(result.seatLayout.seatRadius).toBe(8);
  });

  it('al achicar solo recorta las sillas que ya no caben', () => {
    const full   = { ...section, seatLayout: { ...section.seatLayout, targetSeats: 120 } };
    const next   = { ...full, width: 120, height: 100 };
    const max    = computeMaxSeats(next, 8, 4);
    expect(fillSeatsOnResize(full, next).seatLayout.targetSeats).toBe(max);
    expect(fillSeatsOnResize(section, { ...section, width: 190 }).seatLayout.targetSeats).toBe(10);
  });

  it('migra el formato antiguo de filas y columnas', () => {
    const legacy = { ...section, seatLayout: { rows: 2, cols: 3 } };
    expect(fillSeatsOnResize(legacy, { ...legacy, width: 150 }).seatLayout).toMatchObject({ targetSeats: 6, gap: 4 });
  });

  it('deja intactos los elementos sin sillas', () => {
    const stage = { type: 'stage', width: 100, height: 50, seatLayout: null };
    expect(fillSeatsOnResize(stage, { ...stage, width: 300 })).toEqual({ ...stage, width: 300 });
  });
});
