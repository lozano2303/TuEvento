import { useNavigate } from 'react-router-dom';
import { Calendar, MapPin, Users, ArrowRight } from 'lucide-react';
import EventImagePlaceholder from '../common/EventImagePlaceholder';

/**
 * EventCard — tarjeta de evento rediseñada para el listado público /events.
 *
 * Campos usados de EventSummaryResponse:
 *   eventId, eventName, coverUrl, startDate, siteName, availableSeats, status
 */
export default function EventCard({ event, userId }) {
  const navigate  = useNavigate();
  const imageUrl  = event.coverUrl ?? null;

  /* Fecha formateada */
  const dateLabel = event.startDate
    ? new Date(event.startDate + 'T00:00:00').toLocaleDateString('es-CO', {
        day: 'numeric', month: 'short', year: 'numeric',
      })
    : null;

  /* Disponibilidad */
  const seats       = event.availableSeats ?? 0;
  const soldOut     = seats === 0;
  const almostGone  = seats > 0 && seats <= 10;

  return (
    <div
      className="ec-card group"
      onClick={() => navigate(`/events/${event.eventId}`)}
      role="button"
      tabIndex={0}
      onKeyDown={e => e.key === 'Enter' && navigate(`/events/${event.eventId}`)}
      aria-label={`Ver detalles de ${event.eventName}`}
    >
      {/* ── Imagen ──────────────────────────────────────────────────── */}
      <div className="ec-img-wrap">
        {imageUrl ? (
          <img src={imageUrl} alt={event.eventName} className="ec-img" loading="lazy" decoding="async" />
        ) : (
          <div className="ec-img-placeholder">
            <EventImagePlaceholder size="md" themed />
          </div>
        )}

        {/* Overlay degradado sobre la imagen */}
        <div className="ec-img-overlay" />

        {/* Badge de estado — esquina superior izquierda */}
        <div className="ec-badges">
          {userId === event.userId && event.status === 'DRAFT' && (
            <span className="ec-badge ec-badge-draft">Borrador</span>
          )}
          {soldOut && (
            <span className="ec-badge ec-badge-soldout">Agotado</span>
          )}
          {almostGone && !soldOut && (
            <span className="ec-badge ec-badge-almost">Últimos lugares</span>
          )}
        </div>

        {/* Fecha flotante — esquina superior derecha */}
        {dateLabel && (
          <div className="ec-date-chip">
            <Calendar className="w-3 h-3" />
            {dateLabel}
          </div>
        )}
      </div>

      {/* ── Cuerpo ──────────────────────────────────────────────────── */}
      <div className="ec-body">
        <h3 className="ec-title">{event.eventName}</h3>

        <div className="ec-meta">
          {event.siteName && (
            <span className="ec-meta-item">
              <MapPin className="w-3.5 h-3.5 shrink-0" />
              <span className="truncate">{event.siteName}</span>
            </span>
          )}
          {seats > 0 && (
            <span className="ec-meta-item">
              <Users className="w-3.5 h-3.5 shrink-0" />
              {seats} lugares disponibles
            </span>
          )}
        </div>
      </div>

      {/* ── Footer ──────────────────────────────────────────────────── */}
      <div className="ec-footer">
        <button
          className="ec-btn"
          onClick={e => { e.stopPropagation(); navigate(`/events/${event.eventId}`); }}
        >
          Ver detalles <ArrowRight className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
}
