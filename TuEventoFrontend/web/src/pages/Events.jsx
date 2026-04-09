import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search } from 'lucide-react';
import { getAllEvents, cancelEvent } from '../services/EventService.js';
import { getEventImages } from '../services/EventImgService.js';
import { getCategoriesByEvent } from '../services/CategoryService.js';
import { searchEvents } from '../services/searchEvents.js';

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
        event.status === 1 && eventImagesMap[event.id] && eventCategoriesMap[event.id]
      );
      setFilteredEvents(filtered);
    }
  }, [events, eventImagesMap, eventCategoriesMap]);

  const loadHeroImage = async (eventsList) => {
    try {
      for (const event of eventsList) {
        try {
          const imagesResult = await getEventImages(event.id);
          if (imagesResult.success && imagesResult.data && imagesResult.data.length > 0) {
            setHeroImage(imagesResult.data[0].url);
            return;
          }
        } catch (error) {
          console.log(`No images for event ${event.id}`);
        }
      }
    } catch (error) {
      console.error('Error loading hero image:', error);
    }
  };

  const loadEventImages = async (eventsList) => {
    const newImagesMap = { ...eventImagesMap };
    for (const event of eventsList) {
      if (!newImagesMap[event.id]) {
        try {
          const imagesResult = await getEventImages(event.id);
          if (imagesResult.success && imagesResult.data && imagesResult.data.length > 0) {
            newImagesMap[event.id] = imagesResult.data[0].url;
          }
        } catch (error) {
          console.log(`No images for event ${event.id}`);
        }
      }
    }
    setEventImagesMap(newImagesMap);
  };

  const loadEventCategories = async (eventsList) => {
    const newCategoriesMap = { ...eventCategoriesMap };
    for (const event of eventsList) {
      if (!newCategoriesMap[event.id]) {
        try {
          const categoriesResult = await getCategoriesByEvent(event.id);
          if (categoriesResult.success && categoriesResult.data && categoriesResult.data.length > 0) {
            newCategoriesMap[event.id] = categoriesResult.data.length;
          }
        } catch (error) {
          console.log(`No categories for event ${event.id}`);
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
          const updatedEvents = events.filter(event => event.id !== eventId);
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
                      {filteredEvents.map((event, index) => (
                        <div
                          key={`event-${event.id}-${index}`}
                          className="relative rounded-xl overflow-hidden"
                          style={{
                            background: 'rgba(88, 28, 135, 0.2)',
                            border: '0.5px solid rgba(167, 139, 250, 0.2)',
                          }}
                        >
                          {/* Imagen */}
                          {eventImagesMap[event.id] ? (
                            <img
                              src={eventImagesMap[event.id]}
                              alt={event.name}
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

                          {/* Badge de estado */}
                          {currentUserId === event.userID?.userID && event.status === 0 && (
                            <div className="absolute top-2 left-2 bg-yellow-500 text-black px-2 py-1 text-xs font-bold rounded">
                              {!eventImagesMap[event.id]
                                ? 'Falta imagen'
                                : !eventCategoriesMap[event.id]
                                ? 'Falta categoría'
                                : 'En proceso'}
                            </div>
                          )}

                          {/* Footer tarjeta */}
                          <div
                            className="p-4 flex items-center justify-between"
                            style={{ borderTop: '0.5px solid rgba(167, 139, 250, 0.15)' }}
                          >
                            <span
                              className="text-sm truncate mr-3"
                              style={{ color: 'rgba(233, 213, 255, 0.8)' }}
                            >
                              {event.name}
                            </span>
                            <button
                              className="shrink-0 text-sm px-4 py-1.5 rounded-lg font-medium transition-all hover:brightness-110"
                              style={{
                                background: 'linear-gradient(135deg, #7c3aed 0%, #9333ea 100%)',
                                color: '#fff',
                                boxShadow: '0 0 10px rgba(124, 58, 237, 0.35)'
                              }}
                              onClick={() => navigate(`/event-info?id=${event.id}`)}
                            >
                              Ver detalles
                            </button>
                          </div>
                        </div>
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