// Catálogo de elementos disponibles en la paleta del editor.
// NO son datos del backend — son la configuración de qué se puede arrastrar al canvas.
//
// Fase 1.5: seatLayout usa targetSeats en vez de rows/cols.
// Todas las secciones se crean con 100×100 y 9 sillas por defecto para evitar
// que desborden el canvas antes de que el usuario las ajuste manualmente.

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

  // ── Secciones (100×100, 9 sillas por defecto) ─────────────────────────────
  {
    type: 'section',
    sectionType: 'VIP',
    label: 'VIP',
    icon: '⭐',
    defaultWidth: 100,
    defaultHeight: 100,
    color: '#7C3AED',
    seatLayout: { targetSeats: 9, seatRadius: 8, gap: 4 },
  },
  {
    type: 'section',
    sectionType: 'General',
    label: 'General',
    icon: '👥',
    defaultWidth: 100,
    defaultHeight: 100,
    color: '#16A34A',
    seatLayout: { targetSeats: 9, seatRadius: 8, gap: 4 },
  },
  {
    type: 'section',
    sectionType: 'Palco',
    label: 'Palco',
    icon: '🎪',
    defaultWidth: 100,
    defaultHeight: 100,
    color: '#EA580C',
    seatLayout: { targetSeats: 9, seatRadius: 8, gap: 4 },
  },
  {
    type: 'section',
    sectionType: 'Tribuna',
    label: 'Tribuna',
    icon: '🏟️',
    defaultWidth: 100,
    defaultHeight: 100,
    color: '#2563EB',
    seatLayout: { targetSeats: 9, seatRadius: 8, gap: 4 },
  },
];
