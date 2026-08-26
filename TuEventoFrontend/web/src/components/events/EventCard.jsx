import { useNavigate } from 'react-router-dom';

/**
 * EventCard — tarjeta de evento para el listado público /events.
 *
 * Props:
 *   event  — EventSummaryResponse: { eventId, eventName, status, coverUrl, ... }
 *   userId — number | null — userId del usuario logueado (para badge de borrador)
 *
 * Todos los colores usan CSS custom properties del tema activo.
 */
export default function EventCard({ event, userId }) {
  const navigate = useNavigate();
  const imageUrl = event.coverUrl ?? null;

  return (
    <div
      className="theme-event-card relative rounded-xl overflow-hidden cursor-pointer"
      onClick={() => navigate(`/events/${event.eventId}`)}
    >
      {/* Imagen */}
      {imageUrl ? (
        <img
          src={imageUrl}
          alt={event.eventName}
          className="w-full h-48 object-cover"
        />
      ) : (
        <div className="theme-event-card-placeholder w-full h-48 flex items-center justify-center">
          <span className="text-sm text-textMuted opacity-60">
            Sin imagen
          </span>
        </div>
      )}

      {/* Badge borrador — solo visible al propio organizador */}
      {userId === event.userId && event.status === 'DRAFT' && (
        <div className="absolute top-2 left-2 badge-warning px-2 py-1 text-xs font-bold rounded-lg">
          Borrador
        </div>
      )}

      {/* Footer */}
      <div className="theme-event-card-footer p-4 flex items-center justify-between">
        <span className="text-sm text-textSecondary truncate mr-3 opacity-90">
          {event.eventName}
        </span>
        <button
          className="shrink-0 text-sm px-4 py-1.5 rounded-lg font-medium transition-all
                     bg-primary hover:bg-primaryDark text-textPrimary
                     shadow-[0_0_10px_color-mix(in_srgb,var(--color-primary)_35%,transparent)]"
          onClick={(e) => { e.stopPropagation(); navigate(`/events/${event.eventId}`); }}
        >
          Ver detalles
        </button>
      </div>
    </div>
  );
}
