import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search } from 'lucide-react';
import { getAllEvents, cancelEvent } from '../services/EventService.js';
import { getEventMedia } from '../services/EventMediaService.js';
import { getCategoriesByEvent } from '../services/CategoryService.js';
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
  const [heroImage, setHeroImage] = useState("https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?w=1400&h=900&fit=crop");
  const [eventImagesMap, setEventImagesMap] = useState({});
  const [eventCategoriesMap, setEventCategoriesMap] = useState({});
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
          await loadHeroImage(result.data);
          await loadEventImages(result.data);
          await loadEventCategories(result.data);
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
      const filtered = events.filter(event =>
        event.status === 'PUBLISHED' && eventImagesMap[event.eventId] && eventCategoriesMap[event.eventId]
      );
      setFilteredEvents(filtered);
    }
  }, [events, eventImagesMap, eventCategoriesMap]);

  const loadHeroImage = async (eventsList) => {
    try {
      for (const event of eventsList) {
        try {
          const imagesResult = await getEventMedia(event.eventId);
          if (imagesResult.success && imagesResult.data && imagesResult.data.length > 0) {
            setHeroImage(imagesResult.data[0].imgUrl);
            return;
          }
        } catch (error) {
          console.log(`No images for event ${event.eventId}`);
        }
      }
    } catch (error) {
      console.error('Error loading hero image:', error);
    }
  };

  const loadEventImages = async (eventsList) => {
    const newImagesMap = { ...eventImagesMap };
    for (const event of eventsList) {
      if (!newImagesMap[event.eventId]) {
        try {
          const imagesResult = await getEventMedia(event.eventId);
          if (imagesResult.success && imagesResult.data && imagesResult.data.length > 0) {
            newImagesMap[event.eventId] = imagesResult.data[0].imgUrl;
          }
        } catch (error) {
          console.log(`No images for event ${event.eventId}`);
        }
      }
    }
    setEventImagesMap(newImagesMap);
  };

  const loadEventCategories = async (eventsList) => {
    const newCategoriesMap = { ...eventCategoriesMap };
    for (const event of eventsList) {
      if (!newCategoriesMap[event.eventId]) {
        try {
          const categoriesResult = await getCategoriesByEvent(event.eventId);
          if (categoriesResult.success && categoriesResult.data && categoriesResult.data.length > 0) {
            newCategoriesMap[event.eventId] = categoriesResult.data.length;
          }
        } catch (error) {
          console.log(`No categories for event ${event.eventId}`);
        }
      }
    }
    setEventCategoriesMap(newCategoriesMap);
  };

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
        setFilteredEvents(eventsList);
        await loadEventImages(eventsList);
        await loadEventCategories(eventsList);
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
      className="min-h-screen text-white"
      style={{ background: 'linear-gradient(160deg, #0f0a1e 0%, #1a0f2e 50%, #120820 100%)' }}
    >
      <section className="relative min-h-screen">

        {/* Imagen de fondo */}
        <div
          className="absolute inset-0 bg-cover bg-center"
          style={{ backgroundImage: `url("${heroImage}")` }}
        />

        {/* Overlay morado */}
        <div
          className="absolute inset-0"
          style={{
            background: 'linear-gradient(to bottom, rgba(88, 28, 135, 0.55) 0%, rgba(15, 10, 30, 0.97) 100%)'
          }}
        />

        {/* Barra de búsqueda */}
        <div className="relative z-10 px-6 pt-8">
          <div className="max-w-3xl mx-auto">
            <div
              className="flex flex-wrap items-center gap-3 p-3 rounded-2xl"
              style={{
                background: 'rgba(139, 92, 246, 0.12)',
                border: '0.5px solid rgba(167, 139, 250, 0.25)',
                backdropFilter: 'blur(16px)',
              }}
            >
              {/* Buscador */}
              <div className="flex items-center gap-2 flex-1 min-w-44">
                <Search className="w-4 h-4 shrink-0" style={{ color: 'rgba(196, 181, 253, 0.6)' }} />
                <input
                  type="text"
                  placeholder="Buscar eventos..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter') handleServerFilter(); }}
                  className="bg-transparent text-white text-sm outline-none w-full"
                  style={{ '::placeholder': { color: 'rgba(196,181,253,0.4)' } }}
                />
              </div>

              {/* Separador */}
              <div style={{ width: '0.5px', height: '20px', background: 'rgba(167, 139, 250, 0.3)' }} />

              {/* Fecha */}
              <input
                type="date"
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
                className="bg-transparent text-sm outline-none"
                style={{ color: selectedDate ? '#e9d5ff' : 'rgba(196,181,253,0.45)' }}
              />

              {/* Separador */}
              <div style={{ width: '0.5px', height: '20px', background: 'rgba(167, 139, 250, 0.3)' }} />

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
                style={{ color: '#e9d5ff' }}
              >
                EVENTOS
              </h2>

              {loading && (
                <p className="text-center text-sm" style={{ color: 'rgba(196,181,253,0.6)' }}>
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
                          imageUrl={eventImagesMap[event.eventId] ?? null}
                          userId={currentUserId}
                        />
                      ))}
                    </div>
                  ) : (
                    <div className="flex justify-center mt-10">
                      <div
                        className="rounded-2xl p-8 text-center max-w-sm w-full"
                        style={{
                          background: 'rgba(88, 28, 135, 0.2)',
                          border: '0.5px solid rgba(167, 139, 250, 0.2)',
                        }}
                      >
                        <div className="text-4xl mb-4" style={{ opacity: 0.4 }}>🔍</div>
                        <p className="font-medium mb-1" style={{ color: '#e9d5ff' }}>
                          No se encontraron eventos
                        </p>
                        <p className="text-sm" style={{ color: 'rgba(196,181,253,0.45)' }}>
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