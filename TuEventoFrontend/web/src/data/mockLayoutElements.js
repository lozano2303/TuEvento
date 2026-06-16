// Catálogo de elementos disponibles en la paleta del editor.
// NO son datos del backend — son la configuración de qué se puede arrastrar al canvas.

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
  {
    type: 'section',
    sectionType: 'VIP',
    label: 'VIP',
    icon: '⭐',
    defaultWidth: 200,
    defaultHeight: 150,
    color: '#7C3AED',            // morado
    seatLayout: { rows: 4, cols: 6, seatRadius: 7, gap: 4 },
  },
  {
    type: 'section',
    sectionType: 'General',
    label: 'General',
    icon: '👥',
    defaultWidth: 240,
    defaultHeight: 180,
    color: '#16A34A',            // verde
    seatLayout: { rows: 6, cols: 8, seatRadius: 7, gap: 4 },
  },
  {
    type: 'section',
    sectionType: 'Palco',
    label: 'Palco',
    icon: '🎪',
    defaultWidth: 180,
    defaultHeight: 120,
    color: '#EA580C',            // naranja
    seatLayout: { rows: 3, cols: 5, seatRadius: 7, gap: 4 },
  },
  {
    type: 'section',
    sectionType: 'Tribuna',
    label: 'Tribuna',
    icon: '🏟️',
    defaultWidth: 260,
    defaultHeight: 160,
    color: '#2563EB',            // azul
    seatLayout: { rows: 5, cols: 8, seatRadius: 7, gap: 4 },
  },
  {
    type: 'section',
    sectionType: 'Pista',
    label: 'Pista',
    icon: '🎶',
    defaultWidth: 200,
    defaultHeight: 200,
    color: '#DB2777',            // rosado
    seatLayout: null,            // área libre — sin grilla de sillas
  },
];
