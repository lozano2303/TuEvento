import { SeatDataStore, SeatStatus } from "../../src/utils/seatDataStore";

const seat = (seatId, eventSectionId, code, extra = {}) => ({
  seatId,
  eventSectionId,
  code,
  status: "AVAILABLE",
  reservedBy: null,
  reservedUntil: null,
  ...extra,
});

describe("SeatDataStore", () => {
  it("agrega secciones por separado sin perder las anteriores", () => {
    const store = new SeatDataStore(10);
    store.upsertSeats([seat(1, 10, "A1"), seat(2, 10, "A2")]);
    store.upsertSeats([seat(3, 20, "B1")]);

    expect(store.count).toBe(3);
    expect(store.hasSection(10)).toBe(true);
    expect(store.hasSection(20)).toBe(true);
    expect(store.getSeat(3).eventSectionId).toBe(20);
  });

  it("al repetir una silla la actualiza en lugar de duplicarla", () => {
    const store = new SeatDataStore(10);
    store.upsertSeats([seat(1, 10, "A1")]);
    store.upsertSeats([seat(1, 10, "A1", { status: "SOLD" })]);

    expect(store.count).toBe(1);
    expect(store.getSeat(1).status).toBe("SOLD");
  });

  it("ordena las sillas de la sección por código igual que la web y cachea el orden", () => {
    const store = new SeatDataStore(10);
    store.upsertSeats([seat(1, 10, "B1"), seat(2, 10, "A2"), seat(3, 10, "A1")]);

    const sorted = store.getSortedSectionIndices(10);
    expect(sorted.map((idx) => store.codes[idx])).toEqual(["A1", "A2", "B1"]);
    // Cambiar el estado no altera el orden ni invalida la cache
    store.updateSeat(2, { status: "RESERVED", reservedBy: 7 }, 7);
    expect(store.getSortedSectionIndices(10)).toBe(sorted);
  });

  it("invalida el orden cacheado cuando llegan sillas nuevas de la sección", () => {
    const store = new SeatDataStore(10);
    store.upsertSeats([seat(1, 10, "A2")]);
    const before = store.getSortedSectionIndices(10);
    store.upsertSeats([seat(2, 10, "A1")]);

    const after = store.getSortedSectionIndices(10);
    expect(after).not.toBe(before);
    expect(after.map((idx) => store.codes[idx])).toEqual(["A1", "A2"]);
  });

  it("crece por encima de la capacidad inicial y admite ids de sección mayores que 65535", () => {
    const store = new SeatDataStore(2);
    store.upsertSeats([seat(1, 70000, "A1"), seat(2, 70000, "A2"), seat(3, 70000, "A3")]);

    expect(store.count).toBe(3);
    expect(store.capacity).toBeGreaterThanOrEqual(3);
    expect(store.getSeat(3).eventSectionId).toBe(70000);
  });

  it("colorea y detecta las reservas del usuario actual", () => {
    const store = new SeatDataStore(10);
    store.upsertSeats(
      [
        seat(1, 10, "A1", { status: "RESERVED", reservedBy: 7 }),
        seat(2, 10, "A2", { status: "RESERVED", reservedBy: 8 }),
      ],
      7,
    );

    const mine = store.seatIdToIndex.get(1);
    const other = store.seatIdToIndex.get(2);
    expect(store.colorHexAt(mine)).toBe("#3B82F6");
    expect(store.colorHexAt(other)).toBe("#FBBF24");
    expect(store.isReservedBy(mine, 7)).toBe(true);
    expect(store.isReservedBy(other, 7)).toBe(false);
    expect(store.getUserReservations(7).map((s) => s.seatId)).toEqual([1]);
    expect(store.statuses[mine]).toBe(SeatStatus.RESERVED);
  });

  it("loadSeats reemplaza todo el contenido", () => {
    const store = new SeatDataStore(10);
    store.upsertSeats([seat(1, 10, "A1")]);
    store.loadSeats([seat(5, 20, "Z1")]);

    expect(store.count).toBe(1);
    expect(store.getSeat(1)).toBeNull();
    expect(store.hasSection(10)).toBe(false);
  });
});
