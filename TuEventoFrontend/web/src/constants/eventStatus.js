/**
 * Mapa de transiciones de estado válidas por estado actual (organizer view).
 * Reflects ChangeEventStatusService.validateTransition() on the backend.
 *
 * DRAFT          → PENDING_REVIEW  (submit for admin review)
 * PENDING_REVIEW → DRAFT           (withdraw submission)
 * REJECTED       → DRAFT           (correct and re-submit later)
 * PUBLISHED      → CANCELLED       (cancel)
 * PUBLISHED      → COMPLETED       (manual — normally done by the scheduler)
 */
export const VALID_TRANSITIONS = {
  DRAFT:          ['PENDING_REVIEW'],
  PENDING_REVIEW: ['DRAFT'],
  REJECTED:       ['DRAFT'],
  PUBLISHED:      ['CANCELLED'],  // COMPLETED is automatic via backend scheduler
  CANCELLED:      [],
  COMPLETED:      [],
};

/** Badge pill per status — uses theme CSS variables, no hardcoded colors. */
export const STATUS_BADGE = {
  DRAFT:          { label: 'Borrador',    cls: 'bg-gray-500/20 text-gray-400 border-gray-500/30' },
  PENDING_REVIEW: { label: 'En revisión', cls: 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30' },
  PUBLISHED:      { label: 'Publicado',   cls: 'bg-green-500/20 text-green-400 border-green-500/30' },
  REJECTED:       { label: 'Rechazado',   cls: 'bg-red-500/20 text-red-400 border-red-500/30' },
  CANCELLED:      { label: 'Cancelado',   cls: 'bg-red-900/20 text-red-300 border-red-900/30' },
  COMPLETED:      { label: 'Finalizado',  cls: 'bg-blue-500/20 text-blue-400 border-blue-500/30' },
};

/** Label shown inside the dropdown item for each target status. */
export const STATUS_LABEL = {
  DRAFT:          'Volver a borrador',
  PENDING_REVIEW: 'Enviar a revisión',
  PUBLISHED:      'Publicar',
  REJECTED:       'Rechazar',
  CANCELLED:      'Cancelar',
  COMPLETED:      'Marcar finalizado',
};

/**
 * Confirmation-modal texts for each transition.
 * Only transitions listed in VALID_TRANSITIONS need an entry here.
 */
export const TRANSITION_INFO = {
  PENDING_REVIEW: {
    title:        '¿Enviar este evento a revisión?',
    body:         'El evento quedará en espera de aprobación por un administrador. No podrás editarlo ni modificar las imágenes mientras esté en revisión. Puedes retirarlo en cualquier momento si necesitas hacer cambios.',
    confirmLabel: 'Sí, enviar a revisión',
    confirmClass: 'bg-yellow-600 hover:bg-yellow-500 text-white',
  },
  // DRAFT is shown when the organizer withdraws (PENDING_REVIEW → DRAFT)
  // or corrects a rejected event (REJECTED → DRAFT).
  // The parent component uses TRANSITION_INFO[newStatus], so we need one entry
  // for DRAFT to cover both withdrawal and correction flows.
  DRAFT: {
    title:        '¿Retirar el evento de revisión?',
    body:         'El evento volverá a estado Borrador y podrás editarlo de nuevo. Tendrás que enviarlo a revisión otra vez cuando esté listo.',
    confirmLabel: 'Sí, retirar envío',
    confirmClass: 'bg-gray-600 hover:bg-gray-500 text-white',
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
