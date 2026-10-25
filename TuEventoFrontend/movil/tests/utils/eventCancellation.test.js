import { emitEventCancelled, onEventCancelled, withoutEvent } from "../../src/utils/eventCancellation";

describe("onEventCancelled / emitEventCancelled", () => {
  it("avisa a cada listener y deja de avisar al desuscribirse", () => {
    const listener = jest.fn();
    const unsubscribe = onEventCancelled(listener);

    emitEventCancelled(7);
    unsubscribe();
    emitEventCancelled(8);

    expect(listener).toHaveBeenCalledTimes(1);
    expect(listener).toHaveBeenCalledWith(7);
  });

  it("un listener que falla no impide avisar a los demás", () => {
    const broken = onEventCancelled(() => {
      throw new Error("boom");
    });
    const listener = jest.fn();
    const unsubscribe = onEventCancelled(listener);

    emitEventCancelled(9);

    expect(listener).toHaveBeenCalledWith(9);
    broken();
    unsubscribe();
  });
});

describe("withoutEvent", () => {
  it("quita el evento comparando ids como texto", () => {
    expect(withoutEvent([{ eventId: 1 }, { eventId: "2" }], 2)).toEqual([{ eventId: 1 }]);
  });

  it("devuelve tal cual lo que no es una lista", () => {
    expect(withoutEvent(undefined, 1)).toBeUndefined();
  });
});
