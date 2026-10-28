import { findSeatAt, findSectionAt } from "../../src/utils/seatHitTesting";
import { getElementAABB, toElementLocal } from "../../src/utils/layoutUtils";

const rectSection = (id, x, y, width, height) => ({
  id: `el-${id}`,
  type: "section",
  backendSectionId: id,
  x,
  y,
  width,
  height,
});

describe("findSectionAt", () => {
  const sections = [rectSection(1, 0, 0, 100, 50), rectSection(2, 200, 0, 100, 50)];

  it("devuelve la sección rectangular tocada, aplicando escala y offset", () => {
    // escala 2 y offset (10, 20): la sección 2 ocupa x 410..610, y 20..120 en pantalla
    expect(findSectionAt(500, 60, sections, 2, 10, 20)?.backendSectionId).toBe(2);
    expect(findSectionAt(50, 40, sections, 2, 10, 20)?.backendSectionId).toBe(1);
  });

  it("devuelve null fuera de las secciones", () => {
    expect(findSectionAt(320, 300, sections, 1, 0, 0)).toBeNull();
  });

  it("prioriza la sección dibujada encima", () => {
    const overlapping = [rectSection(1, 0, 0, 100, 100), rectSection(2, 50, 50, 100, 100)];
    expect(findSectionAt(75, 75, overlapping, 1, 0, 0)?.backendSectionId).toBe(2);
  });

  it("usa la forma real de las secciones poligonales", () => {
    const triangle = {
      ...rectSection(3, 0, 0, 100, 100),
      shapeMode: "polygon",
      polygonPoints: [
        { x: 0, y: 0 },
        { x: 100, y: 0 },
        { x: 0, y: 100 },
      ],
    };
    expect(findSectionAt(10, 10, [triangle], 1, 0, 0)?.backendSectionId).toBe(3);
    // Dentro del rectángulo envolvente pero fuera del triángulo
    expect(findSectionAt(90, 90, [triangle], 1, 0, 0)).toBeNull();
  });

  it("ignora elementos sin sección del backend", () => {
    const decoration = { ...rectSection(4, 0, 0, 100, 100), backendSectionId: null };
    expect(findSectionAt(50, 50, [decoration], 1, 0, 0)).toBeNull();
  });
});

describe("findSeatAt", () => {
  const element = { x: 100, y: 100 };
  const visibleSeats = [
    { pos: { x: 10, y: 10, r: 5 }, seatId: 41 },
    { pos: { x: 40, y: 10, r: 5 }, seatId: 42 },
  ];

  it("devuelve el seatId de la silla visible tocada", () => {
    expect(findSeatAt(140, 110, visibleSeats, element, 1, 0, 0)).toBe(42);
  });

  it("acepta toques cerca del borde (tolerancia táctil)", () => {
    expect(findSeatAt(110 + 5 + 7, 110, visibleSeats, element, 1, 0, 0)).toBe(41);
  });

  it("devuelve null si no se toca ninguna silla visible", () => {
    expect(findSeatAt(300, 300, visibleSeats, element, 1, 0, 0)).toBeNull();
  });
});

describe("secciones rotadas (la vista general las dibuja como el editor)", () => {
  // Rectángulo de 100x40 rotado 90° alrededor de su esquina (200, 100): ocupa x 160–200, y 100–200
  const rotated = { id: "r", backendSectionId: 9, x: 200, y: 100, width: 100, height: 40, rotation: 90 };

  it("detecta la sección dentro de su área rotada y no en la original", () => {
    expect(findSectionAt(180, 150, [rotated], 1, 0, 0)).toBe(rotated);
    expect(findSectionAt(260, 120, [rotated], 1, 0, 0)).toBeNull();
  });

  it("findSeatAt aplica la rotación solo cuando se le pide", () => {
    const seats = [{ pos: { x: 50, y: 20, r: 5 }, seatId: 7 }];
    // Silla local (50, 20) rotada 90° → (200 - 20, 100 + 50)
    expect(findSeatAt(180, 150, seats, rotated, 1, 0, 0, true)).toBe(7);
    expect(findSeatAt(250, 120, seats, rotated, 1, 0, 0)).toBe(7);
    expect(findSeatAt(180, 150, seats, rotated, 1, 0, 0)).toBeNull();
  });
});

describe("getElementAABB", () => {
  it("rota alrededor de la esquina superior izquierda, como Konva", () => {
    const box = getElementAABB({ x: 200, y: 100, width: 100, height: 40, rotation: 90 });
    expect(box.minX).toBeCloseTo(160);
    expect(box.minY).toBeCloseTo(100);
    expect(box.maxX).toBeCloseTo(200);
    expect(box.maxY).toBeCloseTo(200);
  });

  it("toElementLocal deshace la rotación del elemento", () => {
    const local = toElementLocal(180, 150, { x: 200, y: 100, rotation: 90 });
    expect(local.x).toBeCloseTo(50);
    expect(local.y).toBeCloseTo(20);
  });
});
