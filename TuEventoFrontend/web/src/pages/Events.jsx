import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Flame } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useLanguageSync } from '../hooks/useLanguageSync';
import { getAllEvents, cancelEvent } from '../services/EventService.js';
import { searchEvents } from '../services/searchEvents.js';
import { getProfileByUserId } from '../services/ProfileService';
import { getActiveLanguages } from '../services/LanguageService';
import EventCard from '../components/events/EventCard.jsx';
const TuEvento = () => {
  const { t } = useTranslation();
  const currentLanguage = useLanguageSync();
  const navigate = useNavigate();
  const [selectedCity, setSelectedCity] = useState('Bogotá');
  const [selectedDate, setSelectedDate] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [events, setEvents] = useState([]);
  const [filteredEvents, setFilteredEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentUserId, setCurrentUserId] = useState(null);
  const [eventsWithTranslations, setEventsWithTranslations] = useState(new Set()); // IDs de eventos con traducciones en progreso
  const [dateError, setDateError] = useState('');
  // "Populares": solo eventos con entradas vendidas, de más a menos vendidos
  const [popularOnly, setPopularOnly] = useState(false);

  const visibleEvents = popularOnly
    ? filteredEvents.filter(e => e.ticketsSold > 0).sort((a, b) => b.ticketsSold - a.ticketsSold)
    : filteredEvents;

  // Fecha mínima permitida: hoy (YYYY-MM-DD en hora local)
  const today = new Date().toISOString().split('T')[0];

  useEffect(() => {
    const userId = localStorage.getItem('userID');
    setCurrentUserId(userId ? parseInt(userId) : null);
  }, []);

  useEffect(() => {
    const loadEvents = async () => {
      try {
        setLoading(true);
        
        // Obtener idioma del usuario si está autenticado
        const userId = localStorage.getItem('userID');
        let userLanguageCode = null;

        if (userId) {
          try {
            const profileRes = await getProfileByUserId(userId);
            const preferredLangId = profileRes?.data?.preferredLanguageId;
            if (preferredLangId) {
              const langs = await getActiveLanguages();
              const preferredLang = langs.find(l => l.languageId === preferredLangId);
              if (preferredLang) {
                userLanguageCode = preferredLang.code;
              }
            }
          } catch (err) {
            console.warn('[Events] No se pudo cargar idioma preferido del usuario:', err);
          }
        }

        const result = await getAllEvents(userLanguageCode);
        if (result.success) {
          setEvents(result.data);
        } else {
          setError(result.message || t('events.connectionError'));
        }
      } catch (err) {
        setError(t('events.connectionError'));
        console.error('Error loading events:', err);
      } finally {
        setLoading(false);
      }
    };

    void loadEvents();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentLanguage]); // Recargar eventos cuando cambie el idioma

  useEffect(() => {
    if (events.length > 0) {
      // Filtro simplificado: categoryId viene embebido en EventSummaryResponse,
      // igual que coverUrl — sin fetch adicional por evento.
      const filtered = events.filter(event =>
        event.status === 'PUBLISHED' && event.categoryId
      );
      setFilteredEvents(filtered);

      // Identificar eventos con traducciones en progreso
      const eventsGeneratingTranslations = new Set();
      filtered.forEach(event => {
        if (event.translationGenerating) {
          eventsGeneratingTranslations.add(event.eventId);
        }
      });
      setEventsWithTranslations(eventsGeneratingTranslations);
    }
  }, [events]);

  // Polling para eventos con traducciones en progreso
  useEffect(() => {
    if (eventsWithTranslations.size === 0) return;

    let pollCount = 0;
    const maxPolls = 20; // Máximo 20 intentos (1 minuto)
    
    const checkTranslations = async () => {
      try {
        // Obtener idioma del usuario para el polling también
        const userId = localStorage.getItem('userID');
        let userLanguageCode = null;

        if (userId) {
          try {
            const profileRes = await getProfileByUserId(userId);
            const preferredLangId = profileRes?.data?.preferredLanguageId;
            if (preferredLangId) {
              const langs = await getActiveLanguages();
              const preferredLang = langs.find(l => l.languageId === preferredLangId);
              if (preferredLang) {
                userLanguageCode = preferredLang.code;
              }
            }
          } catch (err) {
            console.warn('[Events] No se pudo cargar idioma preferido del usuario para polling:', err);
          }
        }

        const result = await getAllEvents(userLanguageCode);
        if (result.success) {
          const updatedEvents = result.data;
          setEvents(updatedEvents);
          
          // Verificar si alguna traducción se completó
          const stillGenerating = new Set();
          updatedEvents.forEach(event => {
            if (event.translationGenerating && eventsWithTranslations.has(event.eventId)) {
              stillGenerating.add(event.eventId);
            }
          });
          
          // Si ya no hay traducciones en progreso, detener polling
          if (stillGenerating.size === 0) {
            setEventsWithTranslations(new Set());
            return true;
          }
          
          setEventsWithTranslations(stillGenerating);
        }
        
        pollCount++;
        if (pollCount >= maxPolls) {
          console.warn('[Events] Translation polling timeout after 1 minute');
          setEventsWithTranslations(new Set());
          return true; // Detener el polling
        }
        
        return false; // Continuar polling
      } catch (err) {
        console.warn('[Events] Error checking translation status:', err);
        return false;
      }
    };
    
    // Verificar cada 3 segundos
    const pollInterval = setInterval(async () => {
      const shouldStop = await checkTranslations();
      if (shouldStop) {
        clearInterval(pollInterval);
      }
    }, 3000);
    
    return () => clearInterval(pollInterval);
  }, [eventsWithTranslations]);

  const handleServerFilter = async () => {
    // Validación de fecha en el frontend antes de ir al servidor
    if (selectedDate && selectedDate < today) {
      setDateError('No podés buscar eventos en fechas pasadas.');
      return;
    }
    setDateError('');

    try {
      setLoading(true);
      setError(null);
      const result = await searchEvents(
        searchTerm || null,
        selectedDate || null
      );

      if (result.success) {
        const eventsList = result.data;
        setEvents(eventsList);
        setFilteredEvents(eventsList.filter(e => e.status === 'PUBLISHED' && e.categoryId));
      } else {
        setError(result.message || t('events.connectionError'));
      }
    } catch (err) {
      console.error('Error en searchEvents:', err);
      setError(t('events.connectionError'));
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteEvent = async (eventId) => {
    if (window.confirm(t('events.deleteConfirm'))) {
      try {
        const result = await cancelEvent(eventId);
        if (result.success) {
          const updatedEvents = events.filter(event => event.eventId !== eventId);
          setEvents(updatedEvents);
          setFilteredEvents(updatedEvents);
          alert(t('events.deleteSuccess'));
        } else {
          alert(t('events.deleteError') + ': ' + result.message);
        }
      } catch (error) {
        console.error('Error deleting event:', error);
        alert(t('events.deleteError'));
      }
    }
  };

  return (
    <div
      className="min-h-screen"
      style={{ color: 'var(--color-textPrimary)' }}
    >
      <section
        className="relative min-h-screen"
        style={{
          // Fondo base: gradiente entre background y surface del tema activo
          background: 'linear-gradient(160deg, var(--color-background) 0%, var(--color-surface) 60%, var(--color-background) 100%)',
        }}
      >
        {/* Acento radial — usa primary con baja opacidad, reacciona al tema */}
        <div
          className="absolute inset-0 pointer-events-none"
          style={{
            background: 'radial-gradient(ellipse 80% 50% at 50% 0%, color-mix(in srgb, var(--color-primary) 18%, transparent) 0%, transparent 70%)',
          }}
        />

        {/* Barra de búsqueda */}
        <div className="relative z-10 px-6 pt-8">
          <div className="max-w-3xl mx-auto">
            <div
              className="flex flex-wrap items-center gap-3 p-3 rounded-2xl"
              style={{
                background: 'color-mix(in srgb, var(--color-primary) 10%, transparent)',
                border: '0.5px solid color-mix(in srgb, var(--color-primary) 30%, transparent)',
                backdropFilter: 'blur(16px)',
              }}
            >
              {/* Buscador */}
              <div className="flex items-center gap-2 flex-1 min-w-44">
                <Search className="w-4 h-4 shrink-0" style={{ color: 'var(--color-textMuted)' }} />
                <input
                  type="text"
                  placeholder={t('events.searchPlaceholder')}
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter') void handleServerFilter(); }}
                  className="bg-transparent text-sm outline-none w-full"
                  style={{ color: 'var(--color-textPrimary)' }}
                />
              </div>

              {/* Separador */}
              <div style={{ width: '0.5px', height: '20px', background: 'var(--color-surfaceAlt)' }} />

              {/* Fecha */}
              <input
                type="date"
                value={selectedDate}
                min={today}
                onChange={(e) => {
                  setSelectedDate(e.target.value);
                  setDateError('');
                }}
                className="bg-transparent text-sm outline-none"
                style={{ color: selectedDate ? 'var(--color-textPrimary)' : 'var(--color-textMuted)' }}
              />

              {/* Separador */}
              <div style={{ width: '0.5px', height: '20px', background: 'var(--color-surfaceAlt)' }} />

              {/* Populares */}
              <button
                type="button"
                onClick={() => setPopularOnly(v => !v)}
                aria-pressed={popularOnly}
                className="flex items-center gap-1.5 text-sm font-medium px-3 py-2 rounded-xl transition-all"
                style={popularOnly
                  ? { background: 'var(--color-primary)', color: '#fff' }
                  : { background: 'transparent', color: 'var(--color-textSecondary)', border: '0.5px solid var(--color-surfaceAlt)' }}
              >
                <Flame className="w-4 h-4" />
                {t('events.popularFilter')}
              </button>

              {/* Separador */}
              <div style={{ width: '0.5px', height: '20px', background: 'var(--color-surfaceAlt)' }} />

              {/* Botón buscar */}
              <button
                onClick={handleServerFilter}
                className="text-sm font-medium px-5 py-2 rounded-xl transition-all hover:brightness-110"
                style={{
                  background: 'linear-gradient(135deg, #7c3aed 0%, #9333ea 100%)',
                  color: '#fff',
                  boxShadow: '0 0 16px rgba(124, 58, 237, 0.4)'
                }}
              >
                {t('events.searchButton')}
              </button>
            </div>
          </div>
          {/* Error de fecha */}
          {dateError && (
            <p className="text-center text-xs mt-2" style={{ color: '#f87171' }}>
              {dateError}
            </p>
          )}
        </div>

        {/* Contenido principal */}
        <div className="relative z-10 px-6 py-12">
          <div className="max-w-7xl mx-auto">
            <div className="mb-16">

              <h2
                className="text-3xl font-bold mb-12 text-center tracking-wide"
                style={{ color: 'var(--color-textPrimary)' }}
              >
                {t('events.pageTitle')}
              </h2>

              {loading && (
                <p className="text-center text-sm" style={{ color: 'var(--color-textMuted)' }}>
                  {t('events.loadingEvents')}
                </p>
              )}

              {error && (
                <p className="text-center text-sm text-red-400">{error}</p>
              )}

              {!loading && !error && (
                <>
                  {visibleEvents.length > 0 ? (
                    <div className="grid md:grid-cols-3 gap-6">
                      {visibleEvents.map((event) => (
                        <EventCard
                          key={event.eventId}
                          event={event}
                          userId={currentUserId}
                          isTranslating={eventsWithTranslations.has(event.eventId)}
                        />
                      ))}
                    </div>
                  ) : (
                    <div className="flex justify-center mt-10">
                      <div
                        className="rounded-2xl p-8 text-center max-w-sm w-full"
                        style={{
                          background: 'color-mix(in srgb, var(--color-primary) 12%, transparent)',
                          border: '0.5px solid color-mix(in srgb, var(--color-primary) 25%, transparent)',
                        }}
                      >
                        <div className="text-4xl mb-4" style={{ opacity: 0.4 }}>🔍</div>
                        <p className="font-medium mb-1" style={{ color: 'var(--color-textPrimary)' }}>
                          {t('events.noEventsFound')}
                        </p>
                        <p className="text-sm" style={{ color: 'var(--color-textMuted)' }}>
                          {popularOnly ? t('events.popularEmpty') : t('events.noEventsMessage')}
                        </p>
                      </div>
                    </div>
                  )}
                </>
              )}

            </div>
          </div>
        </div>
      </section>
    </div>
  );
};

export default TuEvento;