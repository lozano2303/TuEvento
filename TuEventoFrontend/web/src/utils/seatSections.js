/**
 * Utilidades para cargar y ordenar las sillas por sección en el mapa del evento.
 */

/**
 * Ids de las sillas de cada sección ordenados por código (mismo criterio que asocia
 * cada silla con su posición en el layout).
 *
 * @param {Object<string, {seatId, eventSectionId, code}>} seats - { seatId: SeatResponse }
 * @returns {Object<string, number[]>} { eventSectionId: [seatId, ...] }
 */
export function buildSectionOrder(seats) {
  const bySection = {};
  for (const seat of Object.values(seats)) {
    (bySection[seat.eventSectionId] ??= []).push(seat);
  }
  const order = {};
  for (const id of Object.keys(bySection)) {
    order[id] = bySection[id].sort((a, b) => a.code.localeCompare(b.code)).map((seat) => seat.seatId);
  }
  return order;
}

/**
 * Ejecuta `task` para cada elemento con como máximo `limit` en paralelo. Los errores de
 * una tarea se pasan a `onError` y no detienen a las demás.
 */
export async function runWithConcurrency(items, limit, task, onError = () => {}) {
  const queue = [...items];
  const worker = async () => {
    while (queue.length > 0) {
      const item = queue.shift();
      try {
        await task(item);
      } catch (err) {
        onError(err, item);
      }
    }
  };
  await Promise.all(Array.from({ length: Math.min(limit, queue.length) }, worker));
}
