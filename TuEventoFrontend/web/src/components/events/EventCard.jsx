import { useNavigate } from 'react-router-dom';

/**
 * EventCard — tarjeta de evento para el listado público /events.
 *
 * Props:
 *   event  — EventSummaryResponse: { eventId, eventName, status, coverUrl, ... }
 *   userId — number | null — userId del usuario logueado (para badge de borrador)
 *
 * coverUrl viene embebida en el objeto event desde el backend (no N+1).
 */
export default function EventCard({ event, userId }) {
  const navigate = useNavigate();
  const imageUrl = event.coverUrl ?? null;

  return (
    <div
      className="relative rounded-xl overflow-hidden cursor-pointer"
      style={{
        background: 'rgba(88, 28, 135, 0.2)',
        border: '0.5px solid rgba(167, 139, 250, 0.2)',
      }}
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
        <div
          className="w-full h-48 flex items-center justify-center"
          style={{ background: 'rgba(109, 40, 217, 0.25)' }}
        >
          <span className="text-sm" style={{ color: 'rgba(196,181,253,0.4)' }}>
            Sin imagen
          </span>
        </div>
      )}

      {/* Badge borrador — solo visible al propio organizador */}
      {userId === event.userId && event.status === 'DRAFT' && (
        <div className="absolute top-2 left-2 bg-yellow-500 text-black px-2 py-1 text-xs font-bold rounded">
          Borrador
        </div>
      )}

      {/* Footer */}
      <div
        className="p-4 flex items-center justify-between"
        style={{ borderTop: '0.5px solid rgba(167, 139, 250, 0.15)' }}
      >
        <span
          className="text-sm truncate mr-3"
          style={{ color: 'rgba(233, 213, 255, 0.8)' }}
        >
          {event.eventName}
        </span>
        <button
          className="shrink-0 text-sm px-4 py-1.5 rounded-lg font-medium transition-all hover:brightness-110"
          style={{
            background: 'linear-gradient(135deg, #7c3aed 0%, #9333ea 100%)',
            color: '#fff',
            boxShadow: '0 0 10px rgba(124, 58, 237, 0.35)',
          }}
          onClick={(e) => { e.stopPropagation(); navigate(`/events/${event.eventId}`); }}
        >
          Ver detalles
        </button>
      </div>
    </div>
  );
}
