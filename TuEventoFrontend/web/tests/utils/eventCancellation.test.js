import { describe, expect, it, vi } from 'vitest';
import { emitEventCancelled, onEventCancelled, withoutEvent } from '../../src/utils/eventCancellation';

describe('onEventCancelled / emitEventCancelled', () => {
  it('avisa a los listeners con el eventId y deja de avisar al desuscribirse', () => {
    const listener = vi.fn();
    const unsubscribe = onEventCancelled(listener);

    emitEventCancelled(42);
    unsubscribe();
    emitEventCancelled(43);

    expect(listener).toHaveBeenCalledTimes(1);
    expect(listener).toHaveBeenCalledWith(42);
  });
});

describe('withoutEvent', () => {
  it('quita el evento comparando ids como texto', () => {
    const list = [{ eventId: 1 }, { eventId: 2 }];
    expect(withoutEvent(list, '2')).toEqual([{ eventId: 1 }]);
  });

  it('devuelve tal cual lo que no es una lista', () => {
    expect(withoutEvent(null, 1)).toBeNull();
  });
});
