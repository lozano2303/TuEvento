import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Calendar, MapPin, Users, ImageOff, ShoppingCart, Clock, X, Plus, Minus } from 'lucide-react';
import { Stage, Layer, Group, Rect, Circle, Text, Shape } from 'react-konva';
import Konva from 'konva';
import { getEventById } from '../services/EventService';
import { getEventMedia } from '../services/EventMediaService';
import * as LayoutService from '../services/LayoutService';
import * as SeatService from '../services/SeatService';
import * as EventSectionService from '../services/EventSectionService';
import { connectSeatSocket, disconnectSeatSocket } from '../services/websocketClient';
import { distributeSeats, migratePolygonPoints, polyCentroid, getElementAABB } from '../components/layout-editor/layoutEditorUtils';
import BackButton from '../components/common/BackButton';

export default function EventDetail() {
  const { eventId } = useParams();
  const navigate = useNavigate();
  
  const [event, setEvent] = useState(null);
  const [media, setMedia] = useState([]);
  const [activeImage, setActiveImage] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // Estado de selección de sillas
  const [layoutElements, setLayoutElements] = useState([]);
  const [sections, setSections] = useState([]); // EventSection[] con precio
  const [seats, setSeats] = useState({}); // { seatId: SeatResponse }
  const [selectedQuantity, setSelectedQuantity] = useState(1);
  const [selectedSectionFilter, setSelectedSectionFilter] = useState(null);
  const [reserving, setReserving] = useState(new Set());
  const [currentUserId, setCurrentUserId] = useState(null);
  const [zoom, setZoom] = useState(0.5);
  const wsClientRef = useRef(null);

  useEffect(() => {
    const userId = localStorage.getItem('userID');
    setCurrentUserId(userId ? parseInt(userId) : null);

    Promise.all([
      getEventById(eventId),
      getEventMedia(eventId),
      LayoutService.getLayout(eventId),
      EventSectionService.getByEvent(eventId),
    ])
      .then(async ([eventRes, mediaRes, layoutRes, sectionsRes]) => {
        setEvent(eventRes.data);
        setMedia(mediaRes.data ?? []);
        setSections(sectionsRes.data ?? []);

        if (layoutRes?.data?.layoutData) {
          const parsed = JSON.parse(layoutRes.data.layoutData);
          const sectionElements = (parsed.elements ?? []).filter(
            (el) => el.type === 'section' && el.backendSectionId
          );
          setLayoutElements(sectionElements);

          // Cargar sillas de todas las secciones
          const seatMap = {};
          for (const section of sectionElements) {
            try {
              const seatsRes = await SeatService.getSeatsBySection(section.backendSectionId);
              for (const seat of seatsRes.data) {
                seatMap[seat.seatId] = seat;
              }
            } catch (err) {
              console.warn(`No se pudieron cargar sillas de sección ${section.backendSectionId}:`, err);
            }
          }
          setSeats(seatMap);
        }
      })
      .catch((err) => setError(err.message))
      .finally(() => setIsLoading(false));
  }, [eventId]);

  // WebSocket
  useEffect(() => {
    if (!eventId || isLoading) return;

    const client = connectSeatSocket(parseInt(eventId), (event) => {
      setSeats((prev) => {
        const seat = prev[event.seatId];
        if (!seat) return prev;

        return {
          ...prev,
          [event.seatId]: {
            ...seat,
            status: event.newStatus,
            reservedBy: event.newStatus === 'RESERVED' ? seat.reservedBy : null,
            reservedUntil: event.reservedUntil,
          },
        };
      });
    });

    wsClientRef.current = client;

    return () => {
      if (client) disconnectSeatSocket(client);
    };
  }, [eventId, isLoading]);

  const cart = useMemo(() => {
    if (!currentUserId) return [];
    return Object.values(seats).filter(
      (seat) => seat.status === 'RESERVED' && seat.reservedBy === currentUserId
    );
  }, [seats, currentUserId]);

  // Hidratar el stepper con el número de sillas ya reservadas
  useEffect(() => {
    if (cart.length > 0 && cart.length > selectedQuantity) {
      setSelectedQuantity(cart.length);
    }
  }, [cart.length, selectedQuantity]);

  const handleReserveSeat = useCallback(async (seatId) => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }

    if (cart.length >= selectedQuantity) {
      alert(`Ya seleccionaste ${selectedQuantity} silla(s). Cambia la cantidad si necesitas más.`);
      return;
    }

    setReserving((prev) => new Set(prev).add(seatId));

    try {
      const result = await SeatService.reserveSeat(seatId);
      setSeats((prev) => ({ ...prev, [seatId]: result.data }));
    } catch (err) {
      alert(err.message);
      const seat = seats[seatId];
      if (seat) {
        try {
          const seatsRes = await SeatService.getSeatsBySection(seat.eventSectionId);
          const updated = seatsRes.data.find((s) => s.seatId === seatId);
          if (updated) setSeats((prev) => ({ ...prev, [seatId]: updated }));
        } catch (refreshErr) {
          console.error('Error al refrescar estado de silla:', refreshErr);
        }
      }
    } finally {
      setReserving((prev) => {
        const next = new Set(prev);
        next.delete(seatId);
        return next;
      });
    }
  }, [seats, navigate, cart.length, selectedQuantity]);

  const handleReleaseSeat = useCallback(async (seatId) => {
    setReserving((prev) => new Set(prev).add(seatId));

    try {
      const result = await SeatService.releaseSeat(seatId);
      setSeats((prev) => ({ ...prev, [seatId]: result.data }));
    } catch (err) {
      alert(err.message);
    } finally {
      setReserving((prev) => {
        const next = new Set(prev);
        next.delete(seatId);
        return next;
      });
    }
  }, []);

  // Handler de expiración optimista de silla (solo cliente, sin llamada al backend)
  // El scheduler del backend se encargará de liberar la silla en su próximo ciclo (cada 10s)
  const handleSeatExpire = useCallback((seatId) => {
    setSeats((prev) => {
      if (!prev[seatId]) return prev;
      
      return {
        ...prev,
        [seatId]: {
          ...prev[seatId],
          status: 'AVAILABLE',
          reservedBy: null,
          reservedUntil: null,
        },
      };
    });
  }, []);

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
      <div className="max-w-6xl mx-auto px-4 py-8">

        {/* Back */}
        <div className="mb-6">
          <BackButton fallback="/events" label="Volver a eventos" />
        </div>

        <div className="grid md:grid-cols-2 gap-8 mb-12">

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

        {/* ── Selector de sillas ─────────────────────────────────────────── */}
        {layoutElements.length > 0 && (
          <SeatSelectorSection
            layoutElements={layoutElements}
            sections={sections}
            seats={seats}
            currentUserId={currentUserId}
            cart={cart}
            selectedQuantity={selectedQuantity}
            setSelectedQuantity={setSelectedQuantity}
            selectedSectionFilter={selectedSectionFilter}
            setSelectedSectionFilter={setSelectedSectionFilter}
            reserving={reserving}
            onReserveSeat={handleReserveSeat}
            onReleaseSeat={handleReleaseSeat}
            onSeatExpire={handleSeatExpire}
            zoom={zoom}
            setZoom={setZoom}
          />
        )}
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════════════════
// SEAT SELECTOR COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Sección integrada de selección de sillas dentro de EventDetail.
 * Incluye:
 * - Selector de cantidad estilo cine (stepper)
 * - Menú lateral de secciones con precios
 * - Canvas con mapa visual de sillas (Konva)
 * - Carrito lateral con countdown
 */
function SeatSelectorSection({
  layoutElements,
  sections,
  seats,
  currentUserId,
  cart,
  selectedQuantity,
  setSelectedQuantity,
  selectedSectionFilter,
  setSelectedSectionFilter,
  reserving,
  onReserveSeat,
  onReleaseSeat,
  onSeatExpire,
  zoom,
  setZoom,
}) {
  const maxQuantity = 10;
  const stageRef = useRef();
  const containerRef = useRef();
  const [containerSize, setContainerSize] = useState({ width: 800, height: 600 });

  const ZOOM_MARGIN = 80; // Margen alrededor del contenido

  // ── Observar cambios de tamaño del contenedor ──
  useEffect(() => {
    if (!containerRef.current) return;

    const updateSize = () => {
      const rect = containerRef.current.getBoundingClientRect();
      setContainerSize({ width: rect.width, height: rect.height });
    };

    // Actualizar al montar
    updateSize();

    // Observar cambios de tamaño
    const resizeObserver = new ResizeObserver(updateSize);
    resizeObserver.observe(containerRef.current);

    // Escuchar resize de ventana como fallback
    window.addEventListener('resize', updateSize);

    return () => {
      resizeObserver.disconnect();
      window.removeEventListener('resize', updateSize);
    };
  }, []);

  const handleQuantityDecrease = () => {
    // No permitir bajar por debajo de la cantidad de sillas ya reservadas
    if (selectedQuantity > 1 && selectedQuantity > cart.length) {
      setSelectedQuantity(selectedQuantity - 1);
    }
  };

  const handleQuantityIncrease = () => {
    if (selectedQuantity < maxQuantity) setSelectedQuantity(selectedQuantity + 1);
  };

  // ── Calcular encuadre (zoom + posición) para un conjunto de elementos ──
  const calculateFraming = useCallback((elements, viewportWidth, viewportHeight) => {
    if (elements.length === 0) {
      return { scale: 1, x: 0, y: 0 };
    }

    // Calcular AABB de los elementos
    const aabbs = elements.map(getElementAABB);
    const contentMinX = Math.min(...aabbs.map((b) => b.minX));
    const contentMinY = Math.min(...aabbs.map((b) => b.minY));
    const contentMaxX = Math.max(...aabbs.map((b) => b.maxX));
    const contentMaxY = Math.max(...aabbs.map((b) => b.maxY));

    const contentWidth = contentMaxX - contentMinX;
    const contentHeight = contentMaxY - contentMinY;
    const contentCenterX = (contentMinX + contentMaxX) / 2;
    const contentCenterY = (contentMinY + contentMaxY) / 2;

    // Calcular zoom necesario para que el contenido entre en viewport con margen
    const scaleX = (viewportWidth - ZOOM_MARGIN * 2) / contentWidth;
    const scaleY = (viewportHeight - ZOOM_MARGIN * 2) / contentHeight;
    const scale = Math.min(scaleX, scaleY, 2); // Máximo 2x zoom

    // Calcular offset para centrar el contenido en el viewport
    const x = viewportWidth / 2 - contentCenterX * scale;
    const y = viewportHeight / 2 - contentCenterY * scale;

    return { scale, x, y };
  }, []);

  // ── Animar transición del Stage hacia un nuevo encuadre ──
  const animateToFraming = useCallback((framing, duration = 500) => {
    if (!stageRef.current) return;

    const stage = stageRef.current;
    const tween = new Konva.Tween({
      node: stage,
      duration: duration / 1000, // Konva usa segundos
      scaleX: framing.scale,
      scaleY: framing.scale,
      x: framing.x,
      y: framing.y,
      easing: Konva.Easings.EaseInOut,
    });

    tween.play();

    // Sincronizar el estado de zoom con el valor final de la animación
    setZoom(framing.scale);
  }, [setZoom]);

  // ── Efecto: animar a vista general o sección seleccionada ──
  useEffect(() => {
    if (!stageRef.current || layoutElements.length === 0) return;

    if (selectedSectionFilter === null) {
      // Vista general: todas las secciones
      const framing = calculateFraming(layoutElements, containerSize.width, containerSize.height);
      animateToFraming(framing);
    } else {
      // Vista de sección: solo la sección seleccionada
      const selectedElement = layoutElements.find(
        (el) => el.backendSectionId === selectedSectionFilter
      );
      if (selectedElement) {
        const framing = calculateFraming([selectedElement], containerSize.width, containerSize.height);
        animateToFraming(framing);
      }
    }
  }, [selectedSectionFilter, layoutElements, containerSize, calculateFraming, animateToFraming]);

  // Filtrar layoutElements si hay un filtro de sección activo
  const visibleLayoutElements = selectedSectionFilter
    ? layoutElements.filter((el) => el.backendSectionId === selectedSectionFilter)
    : layoutElements;

  return (
    <div
      className="mt-12 rounded-2xl p-6"
      style={{
        border: '0.5px solid rgba(167,139,250,0.2)',
        background: 'rgba(109,40,217,0.05)',
      }}
    >
      <h2 className="text-xl font-bold mb-6" style={{ color: '#e9d5ff' }}>
        Selecciona tus Sillas
      </h2>

      {/* Selector de cantidad estilo cine */}
      <div className="flex items-center gap-6 mb-6">
        <span className="text-sm" style={{ color: 'rgba(196,181,253,0.75)' }}>
          ¿Cuántas sillas querés?
        </span>
        <div className="flex items-center gap-3">
          <button
            onClick={handleQuantityDecrease}
            disabled={selectedQuantity <= 1 || selectedQuantity <= cart.length}
            className="w-8 h-8 rounded-lg flex items-center justify-center transition-all disabled:opacity-30 disabled:cursor-not-allowed"
            style={{
              background: 'rgba(167,139,250,0.15)',
              border: '1px solid rgba(167,139,250,0.3)',
            }}
            aria-label="Disminuir cantidad"
            title={
              selectedQuantity <= cart.length && cart.length > 0
                ? `No puedes bajar de ${cart.length} (sillas ya seleccionadas)`
                : undefined
            }
          >
            <Minus className="w-4 h-4" style={{ color: '#c4b5fd' }} />
          </button>
          <span
            className="text-lg font-bold w-12 text-center"
            style={{ color: '#e9d5ff' }}
          >
            {selectedQuantity}
          </span>
          <button
            onClick={handleQuantityIncrease}
            disabled={selectedQuantity >= maxQuantity}
            className="w-8 h-8 rounded-lg flex items-center justify-center transition-all disabled:opacity-30 disabled:cursor-not-allowed"
            style={{
              background: 'rgba(167,139,250,0.15)',
              border: '1px solid rgba(167,139,250,0.3)',
            }}
            aria-label="Aumentar cantidad"
          >
            <Plus className="w-4 h-4" style={{ color: '#c4b5fd' }} />
          </button>
        </div>
        <span className="text-xs ml-auto" style={{ color: 'rgba(196,181,253,0.5)' }}>
          {cart.length} de {selectedQuantity} seleccionadas
        </span>
      </div>

      {/* Layout: 3 columnas - menú de secciones + canvas + carrito */}
      <div className="flex gap-4">
        {/* Menú lateral de secciones - columna izquierda (angosta) */}
        <SectionMenu
          sections={sections}
          layoutElements={layoutElements}
          selectedSectionFilter={selectedSectionFilter}
          setSelectedSectionFilter={setSelectedSectionFilter}
        />

        {/* Canvas con mapa de sillas - columna central (flexible, dominante) */}
        <div className="flex-1 rounded-xl overflow-hidden" style={{ background: 'rgba(0,0,0,0.3)', minWidth: 0 }}>
          <div className="flex items-center justify-between p-2 border-b" style={{ borderColor: 'rgba(167,139,250,0.15)' }}>
            <span className="text-xs" style={{ color: 'rgba(196,181,253,0.5)' }}>
              Mapa de Sillas
            </span>
          </div>
          <div ref={containerRef} style={{ height: '600px', overflow: 'hidden' }}>
            <Stage
              ref={stageRef}
              width={containerSize.width}
              height={containerSize.height}
            >
              <Layer>
                {visibleLayoutElements.map((section) => (
                  <SectionRenderer
                    key={section.id}
                    section={section}
                    sections={sections}
                    seats={seats}
                    currentUserId={currentUserId}
                    reserving={reserving}
                    cart={cart}
                    selectedQuantity={selectedQuantity}
                    selectedSectionFilter={selectedSectionFilter}
                    onReserveSeat={onReserveSeat}
                    onReleaseSeat={onReleaseSeat}
                    onSectionClick={() => {
                      // Solo permitir selección de sección si estamos en vista general
                      if (selectedSectionFilter === null) {
                        setSelectedSectionFilter(section.backendSectionId);
                      }
                    }}
                  />
                ))}
              </Layer>
            </Stage>
          </div>
        </div>

        {/* Carrito lateral - columna derecha (fija) */}
        <CartPanel
          cart={cart}
          sections={sections}
          onReleaseSeat={onReleaseSeat}
          onSeatExpire={onSeatExpire}
          reserving={reserving}
        />
      </div>
    </div>
  );
}

/**
 * Menú lateral de secciones con nombre, tipo y precio.
 * Click en una sección activa el filtro (solo esa sección es seleccionable).
 */
function SectionMenu({ sections, layoutElements, selectedSectionFilter, setSelectedSectionFilter }) {
  // Mapear eventSectionId de la sección backend con los elementos de layout
  const sectionsWithLayout = sections.filter((sec) =>
    layoutElements.some((el) => el.backendSectionId === sec.eventSectionId)
  );

  return (
    <div
      className="w-56 shrink-0 rounded-xl p-4 space-y-2"
      style={{
        background: 'rgba(0,0,0,0.2)',
        border: '1px solid rgba(167,139,250,0.15)',
      }}
    >
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold" style={{ color: '#e9d5ff' }}>
          Secciones
        </h3>
        {selectedSectionFilter && (
          <button
            onClick={() => setSelectedSectionFilter(null)}
            className="text-xs px-2 py-1 rounded transition-all"
            style={{
              background: 'rgba(167,139,250,0.2)',
              color: '#c4b5fd',
            }}
          >
            Ver todas
          </button>
        )}
      </div>
      {sectionsWithLayout.map((sec) => {
        const isActive = selectedSectionFilter === sec.eventSectionId;
        return (
          <button
            key={sec.eventSectionId}
            onClick={() =>
              setSelectedSectionFilter(
                isActive ? null : sec.eventSectionId
              )
            }
            className="w-full text-left p-3 rounded-lg transition-all"
            style={{
              background: isActive
                ? 'rgba(167,139,250,0.25)'
                : 'rgba(167,139,250,0.08)',
              border: `1px solid ${
                isActive ? 'rgba(167,139,250,0.5)' : 'rgba(167,139,250,0.15)'
              }`,
            }}
          >
            <div className="flex items-start justify-between gap-2">
              <div className="flex-1 min-w-0">
                <div className="text-sm font-semibold truncate" style={{ color: '#e9d5ff' }}>
                  {sec.sectionTypeName}
                </div>
                <div className="text-xs mt-1" style={{ color: 'rgba(196,181,253,0.6)' }}>
                  {sec.availableSeats}/{sec.capacity}
                </div>
              </div>
              <div className="text-sm font-bold shrink-0" style={{ color: '#a78bfa' }}>
                ${sec.price?.toFixed(2) ?? '0.00'}
              </div>
            </div>
          </button>
        );
      })}
    </div>
  );
}

/**
 * Renderiza una sección con sus sillas clickeables.
 */
function SectionRenderer({
  section,
  sections,
  seats,
  currentUserId,
  reserving,
  cart,
  selectedQuantity,
  selectedSectionFilter,
  onReserveSeat,
  onReleaseSeat,
  onSectionClick,
}) {
  const shapeMode = section.shapeMode ?? 'rect';
  const workPoints = useMemo(() => {
    if (shapeMode === 'polygon' && section.polygonPoints) {
      return migratePolygonPoints(section.polygonPoints);
    }
    return null;
  }, [shapeMode, section.polygonPoints]);

  const seatPositions = useMemo(() => {
    return distributeSeats(section);
  }, [section]);

  const labelCenter = useMemo(() => {
    if (shapeMode === 'polygon' && workPoints) {
      return polyCentroid(workPoints);
    }
    return { x: section.width / 2, y: section.height / 2 };
  }, [shapeMode, workPoints, section.width, section.height]);

  // Mapear posiciones a sillas reales
  const seatList = Object.values(seats).filter(
    (s) => s.eventSectionId === section.backendSectionId
  );

  // Encontrar el precio de esta sección
  const sectionData = sections.find((s) => s.eventSectionId === section.backendSectionId);
  const sectionPrice = sectionData?.price ?? 0;

  // En vista general (sin filtro), las sillas no son clickeables, solo la sección
  const inOverviewMode = selectedSectionFilter === null;

  return (
    <Group x={section.x} y={section.y} rotation={section.rotation ?? 0}>
      {/* Fondo de la sección */}
      {shapeMode === 'polygon' && workPoints ? (
        <Shape
          sceneFunc={(ctx, shape) => {
            ctx.beginPath();
            ctx.moveTo(workPoints[0].x, workPoints[0].y);
            for (let i = 0; i < workPoints.length; i++) {
              const curr = workPoints[i];
              const next = workPoints[(i + 1) % workPoints.length];
              if (curr.handleOut && next.handleIn) {
                ctx.bezierCurveTo(
                  curr.handleOut.x,
                  curr.handleOut.y,
                  next.handleIn.x,
                  next.handleIn.y,
                  next.x,
                  next.y
                );
              } else {
                ctx.lineTo(next.x, next.y);
              }
            }
            ctx.closePath();
            ctx.fillStrokeShape(shape);
          }}
          fill={section.color}
          opacity={inOverviewMode ? 0.5 : 0.3}
          stroke={inOverviewMode ? 'rgba(255,255,255,0.5)' : 'rgba(255,255,255,0.15)'}
          strokeWidth={inOverviewMode ? 2 : 1}
          listening={inOverviewMode}
          onClick={inOverviewMode ? onSectionClick : undefined}
          onTap={inOverviewMode ? onSectionClick : undefined}
          cursor={inOverviewMode ? 'pointer' : 'default'}
        />
      ) : (
        <Rect
          width={section.width}
          height={section.height}
          fill={section.color}
          opacity={inOverviewMode ? 0.5 : 0.3}
          cornerRadius={6}
          stroke={inOverviewMode ? 'rgba(255,255,255,0.5)' : 'rgba(255,255,255,0.15)'}
          strokeWidth={inOverviewMode ? 2 : 1}
          listening={inOverviewMode}
          onClick={inOverviewMode ? onSectionClick : undefined}
          onTap={inOverviewMode ? onSectionClick : undefined}
          cursor={inOverviewMode ? 'pointer' : 'default'}
        />
      )}

      {/* Label de la sección con precio */}
      <Text
        x={labelCenter.x}
        y={labelCenter.y - 15}
        text={section.label}
        fontSize={14}
        fontStyle="bold"
        fill="#ffffff"
        align="center"
        offsetX={0}
        listening={false}
      />
      <Text
        x={labelCenter.x}
        y={labelCenter.y + 2}
        text={`$${sectionPrice.toFixed(2)}`}
        fontSize={11}
        fill="rgba(196,181,253,0.8)"
        align="center"
        offsetX={0}
        listening={false}
      />

      {/* Sillas individuales - solo visibles y clickeables cuando hay filtro activo */}
      {!inOverviewMode && seatPositions.map((pos, idx) => {
        const seat = seatList[idx];
        if (!seat) return null;

        return (
          <SeatCircle
            key={seat.seatId}
            seat={seat}
            position={pos}
            currentUserId={currentUserId}
            isReserving={reserving.has(seat.seatId)}
            cart={cart}
            selectedQuantity={selectedQuantity}
            isSectionFiltered={false}
            onReserve={() => onReserveSeat(seat.seatId)}
            onRelease={() => onReleaseSeat(seat.seatId)}
          />
        );
      })}
    </Group>
  );
}

/**
 * Círculo individual de silla con lógica de color y click.
 * Si hay un filtro de sección activo y esta silla no pertenece a esa sección,
 * se muestra atenuada y no es clickeable.
 */
function SeatCircle({
  seat,
  position,
  currentUserId,
  isReserving,
  cart,
  selectedQuantity,
  isSectionFiltered,
  onReserve,
  onRelease,
}) {
  const isMyReservation = seat.status === 'RESERVED' && seat.reservedBy === currentUserId;
  const isOtherReservation = seat.status === 'RESERVED' && seat.reservedBy !== currentUserId;

  const getSeatColor = () => {
    if (isSectionFiltered) return '#4B5563'; // gris oscuro cuando filtrado
    if (isReserving) return '#9CA3AF'; // gris mientras procesa
    if (seat.status === 'AVAILABLE') return '#10B981'; // verde
    if (isMyReservation) return '#3B82F6'; // azul (mi reserva)
    if (isOtherReservation) return '#FBBF24'; // amarillo (reservado por otro)
    if (seat.status === 'SOLD') return '#6B7280'; // gris
    if (seat.status === 'COURTESY') return '#8B5CF6'; // morado
    return '#6B7280';
  };

  const isClickable =
    !isSectionFiltered && (seat.status === 'AVAILABLE' || isMyReservation);

  const handleClick = () => {
    if (!isClickable || isReserving) return;
    if (isMyReservation) {
      onRelease();
    } else {
      // Validar cantidad antes de reservar
      if (cart.length >= selectedQuantity) {
        // No hacer nada, el handler en el componente padre ya muestra el alert
        return;
      }
      onReserve();
    }
  };

  return (
    <Circle
      x={position.x}
      y={position.y}
      radius={position.r}
      fill={getSeatColor()}
      opacity={isSectionFiltered ? 0.3 : 0.9}
      stroke={isMyReservation ? '#ffffff' : 'rgba(255,255,255,0.3)'}
      strokeWidth={isMyReservation ? 2 : 1}
      listening={isClickable && !isReserving}
      onClick={handleClick}
      onTap={handleClick}
      cursor={isClickable ? 'pointer' : 'default'}
    />
  );
}

/**
 * Panel lateral con el carrito de sillas reservadas.
 */
function CartPanel({ cart, sections, onReleaseSeat, onSeatExpire, reserving }) {
  const totalPrice = cart.reduce((sum, seat) => {
    const section = sections.find((s) => s.eventSectionId === seat.eventSectionId);
    return sum + (section?.price ?? 0);
  }, 0);

  if (cart.length === 0) {
    return (
      <div
        className="w-72 shrink-0 rounded-xl p-4"
        style={{
          background: 'rgba(0,0,0,0.2)',
          border: '1px solid rgba(167,139,250,0.15)',
        }}
      >
        <div className="flex items-center gap-2 mb-4">
          <ShoppingCart className="w-5 h-5" style={{ color: '#a78bfa' }} />
          <h3 className="text-sm font-semibold" style={{ color: '#e9d5ff' }}>
            Tu Carrito
          </h3>
        </div>
        <p className="text-xs text-center py-8" style={{ color: 'rgba(196,181,253,0.5)' }}>
          No has seleccionado ninguna silla
        </p>
      </div>
    );
  }

  return (
    <div
      className="w-72 shrink-0 rounded-xl p-4 flex flex-col"
      style={{
        background: 'rgba(0,0,0,0.2)',
        border: '1px solid rgba(167,139,250,0.15)',
      }}
    >
      <div className="flex items-center gap-2 mb-4">
        <ShoppingCart className="w-5 h-5" style={{ color: '#a78bfa' }} />
        <h3 className="text-sm font-semibold" style={{ color: '#e9d5ff' }}>
          Tu Carrito
        </h3>
        <span className="ml-auto text-xs" style={{ color: 'rgba(196,181,253,0.5)' }}>
          ({cart.length})
        </span>
      </div>

      <div className="flex-1 overflow-y-auto space-y-2 max-h-96">
        {cart.map((seat) => {
          const section = sections.find((s) => s.eventSectionId === seat.eventSectionId);
          return (
            <CartItem
              key={seat.seatId}
              seat={seat}
              section={section}
              onRelease={() => onReleaseSeat(seat.seatId)}
              onExpire={onSeatExpire}
              isReleasing={reserving.has(seat.seatId)}
            />
          );
        })}
      </div>

      <div className="mt-4 pt-4 space-y-3" style={{ borderTop: '1px solid rgba(167,139,250,0.15)' }}>
        <div className="flex items-center justify-between text-sm">
          <span style={{ color: 'rgba(196,181,253,0.75)' }}>Total</span>
          <span className="font-bold text-lg" style={{ color: '#a78bfa' }}>
            ${totalPrice.toFixed(2)}
          </span>
        </div>
        <button
          className="w-full py-3 rounded-lg font-semibold transition-all"
          style={{
            background: 'linear-gradient(135deg, #6d28d9 0%, #a78bfa 100%)',
            color: '#ffffff',
          }}
          disabled={cart.length === 0}
        >
          Continuar al Pago
        </button>
      </div>
    </div>
  );
}

/**
 * Item individual en el carrito con countdown.
 */
function CartItem({ seat, section, onRelease, onExpire, isReleasing }) {
  const [timeLeft, setTimeLeft] = useState('');

  useEffect(() => {
    if (!seat.reservedUntil) return;

    const updateTimer = () => {
      const now = new Date();
      const until = new Date(seat.reservedUntil);
      const diffMs = until - now;

      if (diffMs <= 0) {
        setTimeLeft('Expirado');
        // Deselección optimista: liberar inmediatamente en el cliente
        // sin esperar el evento de WebSocket del scheduler
        if (onExpire) {
          onExpire(seat.seatId);
        }
        return;
      }

      const minutes = Math.floor(diffMs / 1000 / 60);
      const seconds = Math.floor((diffMs / 1000) % 60);
      setTimeLeft(`${minutes}:${seconds.toString().padStart(2, '0')}`);
    };

    updateTimer();
    const interval = setInterval(updateTimer, 1000);

    return () => clearInterval(interval);
  }, [seat.reservedUntil, seat.seatId, onExpire]);

  return (
    <div
      className="rounded-lg p-3 flex items-center gap-3"
      style={{
        background: 'rgba(167,139,250,0.1)',
        border: '1px solid rgba(167,139,250,0.2)',
      }}
    >
      <div className="flex-1 min-w-0">
        <p className="font-semibold text-sm truncate" style={{ color: '#e9d5ff' }}>
          {seat.code}
        </p>
        <p className="text-xs truncate" style={{ color: 'rgba(196,181,253,0.6)' }}>
          {section?.sectionTypeName ?? 'Sección'}
        </p>
        <div className="flex items-center justify-between mt-1">
          <div className="flex items-center gap-1 text-xs" style={{ color: 'rgba(196,181,253,0.5)' }}>
            <Clock className="w-3 h-3" />
            <span>{timeLeft}</span>
          </div>
          <span className="text-xs font-semibold" style={{ color: '#a78bfa' }}>
            ${section?.price?.toFixed(2) ?? '0.00'}
          </span>
        </div>
      </div>
      <button
        onClick={onRelease}
        disabled={isReleasing}
        className="p-1 rounded transition-all disabled:opacity-50"
        style={{
          background: 'rgba(239,68,68,0.15)',
        }}
        title="Liberar silla"
      >
        <X className="w-4 h-4" style={{ color: '#ef4444' }} />
      </button>
    </div>
  );
}
