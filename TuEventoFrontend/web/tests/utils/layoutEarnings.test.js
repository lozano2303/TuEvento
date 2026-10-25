import { describe, expect, it } from 'vitest';
import {
  applyCourtesySeats,
  countCourtesySeats,
  estimateEarnings,
  formatCOP,
  getSectionGroups,
} from '../../src/components/layout-editor/layoutEarnings';
import { serializeLayout } from '../../src/components/layout-editor/layoutEditorUtils';

const section = (id, eventSectionId, seats, extra = {}) => ({
  id, type: 'section', eventSectionId, sectionType: 'Palco Oro', label: id, color: '#CA8A04',
  price: 100000, seatLayout: { targetSeats: seats, seatRadius: 8, gap: 4 }, ...extra,
});

const ELEMENTS = [
  section('palco-1', 'sec-a', 40, { courtesySeats: 5 }),
  section('palco-2', 'sec-a', 60, { courtesySeats: 5 }),
  section('general', 'sec-b', 200, { sectionType: 'General', price: 30000 }),
  { id: 'stage', type: 'stage', label: 'Escenario' },
];

describe('getSectionGroups', () => {
  it('agrupa sub-secciones y suma sus sillas', () => {
    const groups = getSectionGroups(ELEMENTS);
    expect(groups).toHaveLength(2);
    expect(groups[0]).toMatchObject({ eventSectionId: 'sec-a', label: 'Palco Oro', totalSeats: 100, courtesySeats: 5, price: 100000 });
    expect(groups[1]).toMatchObject({ eventSectionId: 'sec-b', totalSeats: 200, courtesySeats: 0 });
  });

  it('nunca da más cortesías que sillas', () => {
    const [group] = getSectionGroups([section('s', 'x', 10, { courtesySeats: 50 })]);
    expect(group.courtesySeats).toBe(10);
  });

  it('una sección sin precio cuenta como 0', () => {
    const [group] = getSectionGroups([section('s', 'x', 10, { price: null })]);
    expect(group.price).toBe(0);
  });
});

describe('estimateEarnings', () => {
  it('descuenta las cortesías de la ganancia estimada', () => {
    const groups = getSectionGroups(ELEMENTS);
    // 100.000 × (100 − 5) + 30.000 × 200
    expect(estimateEarnings(groups)).toBe(9_500_000 + 6_000_000);
    expect(countCourtesySeats(groups)).toBe(5);
  });

  it('es 0 sin secciones', () => {
    expect(estimateEarnings([])).toBe(0);
  });
});

describe('applyCourtesySeats', () => {
  it('aplica las cortesías a todas las partes de la sección y no toca lo demás', () => {
    const result = applyCourtesySeats(ELEMENTS, { 'sec-b': 12 });
    expect(result.find((el) => el.id === 'general').courtesySeats).toBe(12);
    expect(result.find((el) => el.id === 'palco-1').courtesySeats).toBe(5);
    expect(result.find((el) => el.id === 'stage')).toBe(ELEMENTS[3]);
  });

  it('las cortesías viajan en el layout guardado', () => {
    const { elements } = serializeLayout(applyCourtesySeats(ELEMENTS, { 'sec-a': 7 }));
    expect(elements[0].courtesySeats).toBe(7);
    expect(elements[2].courtesySeats).toBe(0);
    expect(elements[3]).not.toHaveProperty('courtesySeats');
  });
});

it('formatea en pesos colombianos sin decimales', () => {
  expect(formatCOP(1500000).replace(/\s/g, ' ')).toMatch(/1\.500\.000/);
  expect(formatCOP(undefined)).toMatch(/0/);
});
