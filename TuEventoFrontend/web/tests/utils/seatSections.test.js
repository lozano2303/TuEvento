import { describe, expect, it, vi } from 'vitest';
import { buildSectionOrder, runWithConcurrency } from '../../src/utils/seatSections';

const seat = (seatId, eventSectionId, code) => ({ seatId, eventSectionId, code, status: 'AVAILABLE' });

describe('buildSectionOrder', () => {
  it('agrupa por sección y ordena por código', () => {
    const seats = {
      1: seat(1, 10, 'B1'),
      2: seat(2, 10, 'A2'),
      3: seat(3, 20, 'C1'),
      4: seat(4, 10, 'A1'),
    };

    expect(buildSectionOrder(seats)).toEqual({ 10: [4, 2, 1], 20: [3] });
  });

  it('devuelve un objeto vacío sin sillas', () => {
    expect(buildSectionOrder({})).toEqual({});
  });
});

describe('runWithConcurrency', () => {
  it('procesa todos los elementos sin superar el límite en paralelo', async () => {
    let running = 0;
    let maxRunning = 0;
    const done = [];
    const task = async (item) => {
      running++;
      maxRunning = Math.max(maxRunning, running);
      await new Promise((resolve) => setTimeout(resolve, 5));
      done.push(item);
      running--;
    };

    await runWithConcurrency([1, 2, 3, 4, 5, 6, 7], 3, task);

    expect(done.sort()).toEqual([1, 2, 3, 4, 5, 6, 7]);
    expect(maxRunning).toBe(3);
  });

  it('un error no detiene a las demás tareas', async () => {
    const onError = vi.fn();
    const done = [];
    const task = async (item) => {
      if (item === 2) throw new Error('falló la sección 2');
      done.push(item);
    };

    await runWithConcurrency([1, 2, 3], 2, task, onError);

    expect(done.sort()).toEqual([1, 3]);
    expect(onError).toHaveBeenCalledWith(expect.any(Error), 2);
  });

  it('no hace nada con una lista vacía', async () => {
    const task = vi.fn();
    await runWithConcurrency([], 4, task);
    expect(task).not.toHaveBeenCalled();
  });
});
