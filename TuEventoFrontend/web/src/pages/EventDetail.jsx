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
      <div className="min-h-screen flex items-center justify-center" style={{ background: 'linear-gradient(160deg, #0f0a1e 0%, #1a0f2e 50%, #120820 100%)' }}>
        <p className="text-sm" style={{ color: 'rgba(196,181,253,0.6)' }}>Cargando evento…</p>
      </div>
    );
  }

  if (error || !event) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center gap-4" style={{ background: 'linear-gradient(160deg, #0f0a1e 0%, #1a0f2e 50%, #120820 100%)' }}>
        <p className="text-sm text-red-400">{error ?? 'Evento no encontrado'}</p>
        <BackButton fallback="/events" label="Volver a eventos" />
      </div>
    );
  }

  return (
    <div
      className="min-h-screen text-white"
      style={{ background: 'linear-gradient(160deg, #0f0a1e 0%, #1a0f2e 50%, #120820 100%)' }}
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
              style={{ border: '0.5px solid rgba(167,139,250,0.2)', aspectRatio: '16/9' }}
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
                      <div className="absolute bottom-2 left-1/2 -translate-x-1/2 text-[10px] px-2 py-0.5 rounded-full" style={{ background: 'rgba(0,0,0,0.5)' }}>
                        {activeImage + 1} / {media.length}
                      </div>
                    </>
                  )}
                </>
              ) : (
                <div className="w-full h-full flex flex-col items-center justify-center gap-2" style={{ background: 'rgba(109,40,217,0.15)' }}>
                  <ImageOff className="w-8 h-8" style={{ color: 'rgba(196,181,253,0.3)' }} />
                  <span className="text-xs" style={{ color: 'rgba(196,181,253,0.4)' }}>
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
                        ? '2px solid rgba(167,139,250,0.8)'
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
              <h1 className="text-2xl font-bold" style={{ color: '#e9d5ff' }}>
                {event.eventName}
              </h1>
              {event.description && (
                <p className="mt-2 text-sm leading-relaxed" style={{ color: 'rgba(196,181,253,0.75)' }}>
                  {event.description}
                </p>
              )}
            </div>

            <div className="space-y-3">
              {/* Fechas */}
              {event.startDate && (
                <div className="flex items-center gap-2 text-sm" style={{ color: 'rgba(233,213,255,0.8)' }}>
                  <Calendar className="w-4 h-4 shrink-0" style={{ color: 'rgba(167,139,250,0.7)' }} />
                  <span>
                    {event.startDate}
                    {event.finishDate && event.finishDate !== event.startDate && ` → ${event.finishDate}`}
                  </span>
                </div>
              )}

              {/* Sede */}
              {event.siteName && (
                <div className="flex items-center gap-2 text-sm" style={{ color: 'rgba(233,213,255,0.8)' }}>
                  <MapPin className="w-4 h-4 shrink-0" style={{ color: 'rgba(167,139,250,0.7)' }} />
                  <span>{event.siteName}</span>
                </div>
              )}

              {/* Sillas */}
              {event.availableSeats > 0 && (
                <div className="flex items-center gap-2 text-sm" style={{ color: 'rgba(233,213,255,0.8)' }}>
                  <Users className="w-4 h-4 shrink-0" style={{ color: 'rgba(167,139,250,0.7)' }} />
                  <span>{event.availableSeats.toLocaleString()} sillas disponibles</span>
                </div>
              )}
            </div>

            {/* Badge estado */}
            <div>
              <span
                className="inline-block text-xs font-semibold px-3 py-1 rounded-full"
                style={{
                  background: event.status === 'PUBLISHED' ? 'rgba(22,163,74,0.2)' : 'rgba(107,114,128,0.2)',
                  color:      event.status === 'PUBLISHED' ? '#4ade80' : 'rgba(196,181,253,0.5)',
                  border:     `1px solid ${event.status === 'PUBLISHED' ? 'rgba(74,222,128,0.3)' : 'rgba(107,114,128,0.3)'}`,
                }}
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
