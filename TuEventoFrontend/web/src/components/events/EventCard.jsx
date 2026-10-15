import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Clock } from 'lucide-react';
import EventImagePlaceholder from '../common/EventImagePlaceholder';

/**
 * EventCard — tarjeta de evento para el listado público /events.
 *
 * Props:
 *   event  — EventResponse: { eventId, eventName, status, coverUrl, translatedName, translationGenerating, ... }
 *   userId — number | null — userId del usuario logueado (para badge de borrador)
 *   isTranslating — boolean — indica si el evento está generando traducción (para mostrar indicador)
 *
 * Todos los colores usan CSS custom properties del tema activo.
 * Maneja traducciones dinámicas de nombres de eventos con indicador de progreso.
 */
export default function EventCard({ event, userId, isTranslating = false }) {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const imageUrl = event.coverUrl ?? null;
  
  // Usa el nombre traducido si está disponible y el idioma no es español,
  // sino usa el nombre original
  const displayName = (i18n.language !== 'es' && event.translatedName) 
    ? event.translatedName 
    : event.eventName;

  return (
    <div
      className="theme-event-card relative rounded-xl overflow-hidden cursor-pointer"
      onClick={() => navigate(`/events/${event.eventId}`)}
    >
      {/* Imagen */}
      {imageUrl ? (
        <img
          src={imageUrl}
          alt={displayName}
          className="w-full h-48 object-cover"
        />
      ) : (
        <div className="w-full h-48">
          <EventImagePlaceholder size="md" themed />
        </div>
      )}

      {/* Badge borrador — solo visible al propio organizador */}
      {userId === event.userId && event.status === 'DRAFT' && (
        <div className="absolute top-2 left-2 badge-warning px-2 py-1 text-xs font-bold rounded-lg">
          {t('events.draft')}
        </div>
      )}

      {/* Footer */}
      <div className="theme-event-card-footer p-4 flex flex-col gap-2">
        {/* Translation status indicator */}
        {(isTranslating || event.translationGenerating) && i18n.language !== 'es' && (
          <div className="flex items-center gap-2 text-xs px-2 py-1 rounded-lg" style={{ background: 'rgba(251,191,36,0.1)', border: '1px solid rgba(251,191,36,0.3)', color: 'rgba(251,191,36,0.9)' }}>
            <Clock className="w-3 h-3 animate-spin" />
            <span>{t('events.translation.generating')}</span>
          </div>
        )}
        
        {/* Translation completed indicator */}
        {!isTranslating && !event.translationGenerating && event.translatedName && i18n.language !== 'es' && (
          <div className="text-[10px] px-2 py-0.5 rounded-full" style={{ background: 'rgba(109,40,217,0.2)', color: 'rgba(167,139,250,0.8)', border: '1px solid rgba(167,139,250,0.2)' }}>
            {t('events.translation.translated', { lang: i18n.language.toUpperCase() })}
          </div>
        )}
        
        {/* Main content */}
        <div className="flex items-center justify-between">
          <span className="text-sm text-textSecondary truncate mr-3 opacity-90">
            {displayName}
          </span>
          <button
            className="shrink-0 text-sm px-4 py-1.5 rounded-lg font-medium transition-all
                       bg-primary hover:bg-primaryDark text-textPrimary
                       shadow-[0_0_10px_color-mix(in_srgb,var(--color-primary)_35%,transparent)]"
            onClick={(e) => { e.stopPropagation(); navigate(`/events/${event.eventId}`); }}
          >
            {t('events.viewDetails')}
          </button>
        </div>
      </div>
    </div>
  );
}
