/**
 * Mapa de transiciones de estado válidas por estado actual.
 * Refleja la lógica de ChangeEventStatusService.validateTransition() en el backend.
 */
export const VALID_TRANSITIONS = {
  DRAFT:     ['PUBLISHED'],
  PUBLISHED: ['CANCELLED'], // COMPLETED es automático — lo gestiona el scheduler del backend
  CANCELLED: [],
  COMPLETED: [],
};

/** Badges por estado — clase Tailwind para el pill de color */
export const STATUS_BADGE = {
  DRAFT:     { label: 'Borrador',   cls: 'bg-gray-500/20 text-gray-400 border-gray-500/30' },
  PUBLISHED: { label: 'Publicado',  cls: 'bg-green-500/20 text-green-400 border-green-500/30' },
  CANCELLED: { label: 'Cancelado',  cls: 'bg-red-500/20 text-red-400 border-red-500/30' },
  COMPLETED: { label: 'Finalizado', cls: 'bg-blue-500/20 text-blue-400 border-blue-500/30' },
};

/** Texto del ítem en el dropdown para cada estado destino */
export const STATUS_LABEL = {
  DRAFT:     'Borrador',
  PUBLISHED: 'Publicar',
  CANCELLED: 'Cancelar',
  COMPLETED: 'Marcar finalizado',
};

/**
 * Textos del modal de confirmación para cada transición.
 * Solo las transiciones válidas tienen entrada aquí.
 */
export const TRANSITION_INFO = {
  PUBLISHED: {
    title:        '¿Publicar este evento?',
    body:         'Una vez publicado, el evento será visible públicamente y ya no podrás modificar el layout ni volver a marcarlo como borrador. El día después de la fecha de finalización, el evento se marcará automáticamente como Completado.',
    confirmLabel: 'Sí, publicar',
    confirmClass: 'bg-green-600 hover:bg-green-500 text-white',
  },
  CANCELLED: {
    title:        '¿Cancelar este evento?',
    body:         'El evento dejará de estar disponible públicamente. Esta acción no se puede revertir. Si ya existen tickets vendidos, el proceso de reembolso deberá gestionarse por separado.',
    confirmLabel: 'Sí, cancelar evento',
    confirmClass: 'bg-red-600 hover:bg-red-500 text-white',
  },
  COMPLETED: {
    title:        '¿Marcar este evento como completado?',
    body:         'Esto indica que el evento ya finalizó. No podrás modificarlo ni cambiar su estado después de esto.',
    confirmLabel: 'Sí, marcar como completado',
    confirmClass: 'bg-blue-600 hover:bg-blue-500 text-white',
  },
};
