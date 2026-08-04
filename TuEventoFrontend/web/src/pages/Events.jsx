import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search } from 'lucide-react';
import { getAllEvents, cancelEvent } from '../services/EventService.js';
import { searchEvents } from '../services/searchEvents.js';
import EventCard from '../components/events/EventCard.jsx';
const TuEvento = () => {
  const navigate = useNavigate();
  const [selectedCity, setSelectedCity] = useState('Bogotá');
  const [selectedDate, setSelectedDate] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [events, setEvents] = useState([]);
  const [filteredEvents, setFilteredEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentUserId, setCurrentUserId] = useState(null);

  useEffect(() => {
    const userId = localStorage.getItem('userID');
    setCurrentUserId(userId ? parseInt(userId) : null);
  }, []);

  useEffect(() => {
    const loadEvents = async () => {
      try {
        setLoading(true);
        const result = await getAllEvents();
        if (result.success) {
          setEvents(result.data);
        } else {
          setError(result.message || 'Error al cargar eventos');
        }
      } catch (err) {
        setError('Error de conexión al cargar eventos');
        console.error('Error loading events:', err);
      } finally {
        setLoading(false);
      }
    };

    loadEvents();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (events.length > 0) {
      // Filtro simplificado: categoryId viene embebido en EventSummaryResponse,
      // igual que coverUrl — sin fetch adicional por evento.
      const filtered = events.filter(event =>
        event.status === 'PUBLISHED' && event.categoryId
      );
      setFilteredEvents(filtered);
    }
  }, [events]);

  const handleServerFilter = async () => {
    try {
      setLoading(true);
      const result = await searchEvents(
        searchTerm || null,
        selectedDate || null,
        true,
        selectedCity !== 'Bogotá' ? selectedCity : null
      );

      if (result.success) {
        const eventsList = result.data;
        setEvents(eventsList);
        // filtro se aplica en el useEffect — categoryId viene embebido
        setFilteredEvents(eventsList.filter(e => e.status === 'PUBLISHED' && e.categoryId));
      } else {
        setError(result.message || 'Error en la búsqueda');
      }
    } catch (err) {
      console.error('Error en searchEvents:', err);
      setError('No se pudieron cargar los eventos filtrados');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteEvent = async (eventId) => {
    if (window.confirm('¿Estás seguro de que quieres eliminar este evento?')) {
      try {
        const result = await cancelEvent(eventId);
        if (result.success) {
          const updatedEvents = events.filter(event => event.eventId !== eventId);
          setEvents(updatedEvents);
          setFilteredEvents(updatedEvents);
          alert('Evento eliminado exitosamente');
        } else {
          alert('Error al eliminar el evento: ' + result.message);
        }
      } catch (error) {
        console.error('Error deleting event:', error);
        alert('Error al eliminar el evento');
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
                  placeholder="Buscar eventos..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter') handleServerFilter(); }}
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
                onChange={(e) => setSelectedDate(e.target.value)}
                className="bg-transparent text-sm outline-none"
                style={{ color: selectedDate ? 'var(--color-textPrimary)' : 'var(--color-textMuted)' }}
              />

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
                Buscar
              </button>
            </div>
          </div>
        </div>

        {/* Contenido principal */}
        <div className="relative z-10 px-6 py-12">
          <div className="max-w-7xl mx-auto">
            <div className="mb-16">

              <h2
                className="text-3xl font-bold mb-12 text-center tracking-wide"
                style={{ color: 'var(--color-textPrimary)' }}
              >
                EVENTOS
              </h2>

              {loading && (
                <p className="text-center text-sm" style={{ color: 'var(--color-textMuted)' }}>
                  Cargando eventos...
                </p>
              )}

              {error && (
                <p className="text-center text-sm text-red-400">{error}</p>
              )}

              {!loading && !error && (
                <>
                  {filteredEvents.length > 0 ? (
                    <div className="grid md:grid-cols-3 gap-6">
                      {filteredEvents.map((event) => (
                        <EventCard
                          key={event.eventId}
                          event={event}
                          userId={currentUserId}
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
                          No se encontraron eventos
                        </p>
                        <p className="text-sm" style={{ color: 'var(--color-textMuted)' }}>
                          Intentá con otra búsqueda o cambiá la fecha.
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