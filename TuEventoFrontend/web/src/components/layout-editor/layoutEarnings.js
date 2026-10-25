// ── Secciones lógicas, cortesías y ganancia estimada del layout ──────────────
// Una sección lógica agrupa todos los elementos con el mismo eventSectionId
// (la sección y sus sub-secciones): comparten precio y cortesías.
// La fórmula es la misma que usa el backend para la ganancia potencial:
//   Σ precio × (sillas − cortesías)

import { groupBy } from './layoutEditorUtils';

const sectionSeats = (el) => el.seatLayout?.targetSeats ?? 0;

/**
 * Agrupa las secciones del layout por eventSectionId.
 * @returns {{ eventSectionId, label, color, price, totalSeats, courtesySeats }[]}
 */
export function getSectionGroups(elements) {
  const groups = groupBy(elements.filter((el) => el.type === 'section'), 'eventSectionId');
  return Object.entries(groups).map(([eventSectionId, group]) => {
    const totalSeats = group.reduce((sum, el) => sum + sectionSeats(el), 0);
    return {
      eventSectionId,
      label:         group[0].sectionType ?? group[0].label,
      color:         group[0].color,
      price:         group[0].price ?? 0,
      totalSeats,
      // Nunca más cortesías que sillas: si se redujo la sección, se recortan
      courtesySeats: Math.min(totalSeats, Math.max(0, group[0].courtesySeats ?? 0)),
    };
  });
}

/** Ganancia si se venden todas las sillas que no son de cortesía. */
export const estimateEarnings = (groups) =>
  groups.reduce((sum, g) => sum + g.price * (g.totalSeats - g.courtesySeats), 0);

export const countCourtesySeats = (groups) =>
  groups.reduce((sum, g) => sum + g.courtesySeats, 0);

/**
 * Aplica a cada elemento las cortesías de su sección lógica.
 * @param {{ [eventSectionId]: number }} courtesyBySection
 */
export const applyCourtesySeats = (elements, courtesyBySection) =>
  elements.map((el) => (
    el.type === 'section' && el.eventSectionId in courtesyBySection
      ? { ...el, courtesySeats: courtesyBySection[el.eventSectionId] }
      : el
  ));

export const formatCOP = (value) =>
  new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(value ?? 0);
