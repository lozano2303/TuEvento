// Catálogo de elementos disponibles en la paleta del editor.
// NO son datos del backend — son la configuración de qué se puede arrastrar al canvas.
//
// Fase 1.5: seatLayout usa targetSeats en vez de rows/cols.
// defaultWidth/defaultHeight calculados para que las sillas quepan con margen.

export const PALETTE_ELEMENTS = [
  // ── Infraestructura ────────────────────────────────────────────────────────
  {
    type: 'stage',
    label: 'Escenario',
    icon: '🎭',
    defaultWidth: 220,
    defaultHeight: 100,
    color: '#1E293B',
    seatLayout: null,
  },
  {
    type: 'screen',
    label: 'Pantalla',
    icon: '🖥️',
    defaultWidth: 180,
    defaultHeight: 60,
    color: '#0F172A',
    seatLayout: null,
  },
  {
    type: 'dance_floor',
    label: 'Pista de baile',
    icon: '🕺',
    defaultWidth: 160,
    defaultHeight: 160,
    color: '#1D4ED8',
    seatLayout: null,
  },
  {
    type: 'entrance',
    label: 'Entrada',
    icon: '🚪',
    defaultWidth: 80,
    defaultHeight: 50,
    color: '#15803D',
    seatLayout: null,
  },
  {
    type: 'exit',
    label: 'Salida',
    icon: '🚪',
    defaultWidth: 80,
    defaultHeight: 50,
    color: '#B91C1C',
    seatLayout: null,
  },
  {
    type: 'bar',
    label: 'Bar',
    icon: '🍺',
    defaultWidth: 120,
    defaultHeight: 60,
    color: '#92400E',
    seatLayout: null,
  },
  {
    type: 'restroom',
    label: 'Baños',
    icon: '🚻',
    defaultWidth: 80,
    defaultHeight: 80,
    color: '#475569',
    seatLayout: null,
  },

  // ── Secciones ─────────────────────────────────────────────────────────────
  // defaultWidth/defaultHeight dimensionados para que las sillas default quepan cómodamente.
  {
    type: 'section',
    sectionType: 'VIP',
    label: 'VIP',
    icon: '⭐',
    defaultWidth: 240,
    defaultHeight: 180,
    color: '#7C3AED',
    seatLayout: { targetSeats: 24, seatRadius: 8, gap: 4 },
  },
  {
    type: 'section',
    sectionType: 'General',
    label: 'General',
    icon: '👥',
    defaultWidth: 320,
    defaultHeight: 260,
    color: '#16A34A',
    seatLayout: { targetSeats: 80, seatRadius: 6, gap: 3 },
  },
  {
    type: 'section',
    sectionType: 'Palco',
    label: 'Palco',
    icon: '🎪',
    defaultWidth: 200,
    defaultHeight: 140,
    color: '#EA580C',
    seatLayout: { targetSeats: 15, seatRadius: 8, gap: 4 },
  },
  {
    type: 'section',
    sectionType: 'Tribuna',
    label: 'Tribuna',
    icon: '🏟️',
    defaultWidth: 300,
    defaultHeight: 220,
    color: '#2563EB',
    seatLayout: { targetSeats: 60, seatRadius: 6, gap: 3 },
  },
  {
    type: 'section',
    sectionType: 'Pista',
    label: 'Pista',
    icon: '🎶',
    defaultWidth: 200,
    defaultHeight: 200,
    color: '#DB2777',
    seatLayout: null,  // área libre — sin grilla
  },
];
