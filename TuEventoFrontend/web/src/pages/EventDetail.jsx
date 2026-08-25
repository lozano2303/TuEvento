import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Calendar, MapPin, Users, ImageOff } from 'lucide-react';
import { getEventById } from '../services/EventService';
import { getEventMedia } from '../services/EventMediaService';
import BackButton from '../components/common/BackButton';

export default function EventDetail() {
  const { eventId } = useParams();
  const [event,       setEvent]       = useState(null);
  const [media,       setMedia]       = useState([]);
  const [activeImage, setActiveImage] = useState(0);
  const [isLoading,   setIsLoading]   = useState(true);
  const [error,       setError]       = useState(null);

  useEffect(() => {
    Promise.all([
      getEventById(eventId),
      getEventMedia(eventId),
    ])
      .then(([eventRes, mediaRes]) => {
        setEvent(eventRes.data);
        setMedia(mediaRes.data ?? []);
      })
      .catch((err) => setError(err.message))
      .finally(() => setIsLoading(false));
  }, [eventId]);

  const prev = () => setActiveImage((i) => (i - 1 + media.length) % media.length);
  const next = () => setActiveImage((i) => (i + 1) % media.length);

  // ── Loading ────────────────────────────────────────────────────────────────
  if (isLoading) {
    return (
      <div
        className="min-h-screen flex items-center justify-center"
        style={{ background: 'linear-gradient(160deg, var(--color-background) 0%, var(--color-surface) 50%, var(--color-background) 100%)' }}
      >
        <p className="text-sm text-textMuted">Cargando evento…</p>
      </div>
    );
  }

  if (error || !event) {
    return (
      <div
        className="min-h-screen flex flex-col items-center justify-center gap-4"
        style={{ background: 'linear-gradient(160deg, var(--color-background) 0%, var(--color-surface) 50%, var(--color-background) 100%)' }}
      >
        <p className="text-sm text-error">{error ?? 'Evento no encontrado'}</p>
        <BackButton fallback="/events" label="Volver a eventos" />
      </div>
    );
  }

  return (
    <div
      className="min-h-screen text-textPrimary"
      style={{ background: 'linear-gradient(160deg, var(--color-background) 0%, var(--color-surface) 50%, var(--color-background) 100%)' }}
    >
      <div className="max-w-5xl mx-auto px-4 py-8">

        {/* Back */}
        <div className="mb-6">
          <BackButton fallback="/events" label="Volver a eventos" />
        </div>

        <div className="grid md:grid-cols-2 gap-8">

          {/* ── Galería ────────────────────────────────────────────────────── */}
          <div className="flex flex-col gap-3">

            {/* Imagen principal */}
            <div
              className="relative rounded-2xl overflow-hidden"
              style={{ border: '0.5px solid color-mix(in srgb, var(--color-accent) 25%, transparent)', aspectRatio: '16/9' }}
            >
              {media.length > 0 ? (
                <>
                  <img
                    src={media[activeImage].imgUrl}
                    alt={`${event.eventName} — imagen ${activeImage + 1}`}
                    className="w-full h-full object-cover"
                  />
                  {media.length > 1 && (
                    <>
                      <button
                        onClick={prev}
                        className="absolute left-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full flex items-center justify-center transition-all"
                        style={{ background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)' }}
                        aria-label="Imagen anterior"
                      >
                        <ChevronLeft className="w-4 h-4 text-white" />
                      </button>
                      <button
                        onClick={next}
                        className="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full flex items-center justify-center transition-all"
                        style={{ background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)' }}
                        aria-label="Imagen siguiente"
                      >
                        <ChevronRight className="w-4 h-4 text-white" />
                      </button>
                      <div
                        className="absolute bottom-2 left-1/2 -translate-x-1/2 text-[10px] px-2 py-0.5 rounded-full text-textPrimary"
                        style={{ background: 'rgba(0,0,0,0.5)' }}
                      >
                        {activeImage + 1} / {media.length}
                      </div>
                    </>
                  )}
                </>
              ) : (
                <div
                  className="w-full h-full flex flex-col items-center justify-center gap-2"
                  style={{ background: 'color-mix(in srgb, var(--color-primary) 15%, transparent)' }}
                >
                  <ImageOff className="w-8 h-8 text-textMuted opacity-40" />
                  <span className="text-xs text-textMuted opacity-50">
                    Este evento aún no tiene imágenes
                  </span>
                </div>
              )}
            </div>

            {/* Thumbnails */}
            {media.length > 1 && (
              <div className="flex gap-2 overflow-x-auto pb-1">
                {media.map((m, i) => (
                  <button
                    key={m.mediaId}
                    onClick={() => setActiveImage(i)}
                    className="shrink-0 w-16 h-16 rounded-lg overflow-hidden transition-all"
                    style={{
                      border: i === activeImage
                        ? '2px solid var(--color-accent)'
                        : '2px solid transparent',
                      opacity: i === activeImage ? 1 : 0.6,
                    }}
                  >
                    <img src={m.imgUrl} alt={`Thumbnail ${i + 1}`} className="w-full h-full object-cover" />
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* ── Info del evento ────────────────────────────────────────────── */}
          <div className="flex flex-col gap-5">
            <div>
              <h1 className="text-2xl font-bold text-textPrimary">
                {event.eventName}
              </h1>
              {event.description && (
                <p className="mt-2 text-sm leading-relaxed text-textSecondary">
                  {event.description}
                </p>
              )}
            </div>

            <div className="space-y-3">
              {/* Fechas */}
              {event.startDate && (
                <div className="flex items-center gap-2 text-sm text-textSecondary">
                  <Calendar className="w-4 h-4 shrink-0 text-accent opacity-70" />
                  <span>
                    {event.startDate}
                    {event.finishDate && event.finishDate !== event.startDate && ` → ${event.finishDate}`}
                  </span>
                </div>
              )}

              {/* Sede */}
              {event.siteName && (
                <div className="flex items-center gap-2 text-sm text-textSecondary">
                  <MapPin className="w-4 h-4 shrink-0 text-accent opacity-70" />
                  <span>{event.siteName}</span>
                </div>
              )}

              {/* Sillas */}
              {event.availableSeats > 0 && (
                <div className="flex items-center gap-2 text-sm text-textSecondary">
                  <Users className="w-4 h-4 shrink-0 text-accent opacity-70" />
                  <span>{event.availableSeats.toLocaleString()} sillas disponibles</span>
                </div>
              )}
            </div>

            {/* Badge estado */}
            <div>
              <span
                className="inline-block text-xs font-semibold px-3 py-1 rounded-full"
                style={
                  event.status === 'PUBLISHED'
                    ? {
                        background: 'color-mix(in srgb, var(--color-success) 20%, transparent)',
                        color:      'var(--color-success)',
                        border:     '1px solid color-mix(in srgb, var(--color-success) 35%, transparent)',
                      }
                    : {
                        background: 'color-mix(in srgb, var(--color-textMuted) 20%, transparent)',
                        color:      'var(--color-textMuted)',
                        border:     '1px solid color-mix(in srgb, var(--color-textMuted) 30%, transparent)',
                      }
                }
              >
                {event.status === 'PUBLISHED' ? 'Publicado' : event.status}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
