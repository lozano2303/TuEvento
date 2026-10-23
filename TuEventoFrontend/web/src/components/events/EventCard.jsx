import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Calendar, MapPin, Users, ArrowRight, Clock } from 'lucide-react';
import EventImagePlaceholder from '../common/EventImagePlaceholder';

/**
 * EventCard — tarjeta de evento rediseñada para el listado público /events.
 *
 * Campos usados de EventSummaryResponse:
 *   eventId, eventName, coverUrl, startDate, siteName, availableSeats, status,
 *   translatedName, translationGenerating
 *
 * Props:
 *   event  — EventResponse
 *   userId — number | null — userId del usuario logueado (para badge de borrador)
 *   isTranslating — boolean — indica si el evento está generando traducción
 *
 * Mantiene el diseño visual de develop e integra la traducción dinámica de
 * nombres de eventos (i18n) con indicadores de progreso.
 */
export default function EventCard({ event, userId, isTranslating = false }) {
  const { t, i18n } = useTranslation();
  const navigate  = useNavigate();
  const imageUrl  = event.coverUrl ?? null;

  // Usa el nombre traducido si está disponible y el idioma no es español,
  // sino usa el nombre original
  const displayName = (i18n.language !== 'es' && event.translatedName)
    ? event.translatedName
    : event.eventName;

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

  const showTranslating = (isTranslating || event.translationGenerating) && i18n.language !== 'es';
  const showTranslated  = !isTranslating && !event.translationGenerating && event.translatedName && i18n.language !== 'es';

  return (
    <div
      className="ec-card group"
      onClick={() => navigate(`/events/${event.eventId}`)}
      role="button"
      tabIndex={0}
      onKeyDown={e => e.key === 'Enter' && navigate(`/events/${event.eventId}`)}
      aria-label={`Ver detalles de ${displayName}`}
    >
      {/* ── Imagen ──────────────────────────────────────────────────── */}
      <div className="ec-img-wrap">
        {imageUrl ? (
          <img src={imageUrl} alt={displayName} className="ec-img" loading="lazy" decoding="async" />
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
            <span className="ec-badge ec-badge-draft">{t('events.draft')}</span>
          )}
          {soldOut && (
            <span className="ec-badge ec-badge-soldout">{t('events.soldOut')}</span>
          )}
          {almostGone && !soldOut && (
            <span className="ec-badge ec-badge-almost">{t('events.almostGone')}</span>
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
        {/* Indicador de traducción en progreso */}
        {showTranslating && (
          <div className="flex items-center gap-2 text-xs px-2 py-1 rounded-lg mb-2" style={{ background: 'rgba(251,191,36,0.1)', border: '1px solid rgba(251,191,36,0.3)', color: 'rgba(251,191,36,0.9)' }}>
            <Clock className="w-3 h-3 animate-spin" />
            <span>{t('events.translation.generating')}</span>
          </div>
        )}
        {/* Indicador de traducción completada */}
        {showTranslated && (
          <div className="text-[10px] px-2 py-0.5 rounded-full mb-2 self-start" style={{ background: 'rgba(109,40,217,0.2)', color: 'rgba(167,139,250,0.8)', border: '1px solid rgba(167,139,250,0.2)' }}>
            {t('events.translation.translated', { lang: i18n.language.toUpperCase() })}
          </div>
        )}

        <h3 className="ec-title">{displayName}</h3>
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
              {t('events.availableSeats', { count: seats })}
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
          {t('events.viewDetails')} <ArrowRight className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
}
