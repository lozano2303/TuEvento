import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Stage, Layer, Group, Rect, Circle, Text, Shape } from 'react-konva';
import { ArrowLeft, ShoppingCart, Clock, X } from 'lucide-react';
import * as LayoutService from '../services/LayoutService';
import * as SeatService from '../services/SeatService';
import * as EventService from '../services/EventService';
import { connectSeatSocket, disconnectSeatSocket } from '../services/websocketClient';
import { distributeSeats, migratePolygonPoints, polyCentroid } from '../components/layout-editor/layoutEditorUtils';

/**
 * EventSeatSelector — Vista interactiva de compra de sillas.
 * 
 * Flujo:
 * 1. Carga el layout visual del evento (geometría de secciones)
 * 2. Obtiene el estado real de cada silla vía API
 * 3. Conecta WebSocket para actualizaciones en tiempo real
 * 4. Permite seleccionar sillas AVAILABLE (reserva temporal 10 min)
 * 5. Muestra carrito con sillas reservadas y countdown del TTL
 * 
 * No incluye flujo de pago — termina en "sillas seleccionadas".
 */
export default function EventSeatSelector() {
  const { eventId } = useParams();
  const navigate = useNavigate();

  // Estado del evento y layout
  const [event, setEvent] = useState(null);
  const [layoutElements, setLayoutElements] = useState([]);
  const [seats, setSeats] = useState({}); // { seatId: SeatResponse }
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Estado de interacción
  const [reserving, setReserving] = useState(new Set()); // seatIds en proceso
  const [currentUserId, setCurrentUserId] = useState(null);

  // WebSocket
  const wsClientRef = useRef(null);

  // Canvas
  const [zoom, setZoom] = useState(0.6);
  const [canvasSize, setCanvasSize] = useState({ width: 1200, height: 800 });

  // ── Carga inicial ─────────────────────────────────────────────────────────
  useEffect(() => {
    const userId = localStorage.getItem('userID');
    setCurrentUserId(userId ? parseInt(userId) : null);

    (async () => {
      try {
        setLoading(true);

        // Cargar evento
        const eventRes = await EventService.getEventById(eventId);
        setEvent(eventRes.data);

        // Cargar layout
        const layoutRes = await LayoutService.getLayout(eventId);
        if (!layoutRes?.data?.layoutData) {
          setError('Este evento no tiene layout configurado');
          setLoading(false);
          return;
        }

        const parsed = JSON.parse(layoutRes.data.layoutData);
        const sections = (parsed.elements ?? []).filter(
          (el) => el.type === 'section' && el.backendSectionId
        );
        setLayoutElements(sections);

        if (parsed.canvasWidth && parsed.canvasHeight) {
          setCanvasSize({ width: parsed.canvasWidth, height: parsed.canvasHeight });
        }

        // Cargar sillas de todas las secciones
        const seatMap = {};
        for (const section of sections) {
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

        setLoading(false);
      } catch (err) {
        console.error('[EventSeatSelector] Error al cargar:', err);
        setError(err.message);
        setLoading(false);
      }
    })();
  }, [eventId]);

  // ── Conectar WebSocket ────────────────────────────────────────────────────
  useEffect(() => {
    if (!eventId || loading) return;

    const client = connectSeatSocket(parseInt(eventId), (event) => {
      console.log('[WebSocket] Seat update:', event);
      
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
      if (client) {
        disconnectSeatSocket(client);
      }
    };
  }, [eventId, loading]);

  // ── Carrito derivado del estado de sillas ─────────────────────────────────
  const cart = useMemo(() => {
    if (!currentUserId) return [];
    
    return Object.values(seats).filter(
      (seat) => seat.status === 'RESERVED' && seat.reservedBy === currentUserId
    );
  }, [seats, currentUserId]);

  // ── Handlers de reserva/liberación ────────────────────────────────────────
  const handleReserveSeat = useCallback(async (seatId) => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }

    setReserving((prev) => new Set(prev).add(seatId));

    try {
      const result = await SeatService.reserveSeat(seatId);
      
      // Actualizar estado local inmediatamente (WebSocket también lo hará)
      setSeats((prev) => ({
        ...prev,
        [seatId]: result.data,
      }));
    } catch (err) {
      alert(err.message);
      
      // Refrescar estado de la silla en caso de race condition
      const seat = seats[seatId];
      if (seat) {
        try {
          const seatsRes = await SeatService.getSeatsBySection(seat.eventSectionId);
          const updated = seatsRes.data.find((s) => s.seatId === seatId);
          if (updated) {
            setSeats((prev) => ({ ...prev, [seatId]: updated }));
          }
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
  }, [seats, navigate]);

  const handleReleaseSeat = useCallback(async (seatId) => {
    setReserving((prev) => new Set(prev).add(seatId));

    try {
      const result = await SeatService.releaseSeat(seatId);
      
      setSeats((prev) => ({
        ...prev,
        [seatId]: result.data,
      }));
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

  // ── Rendering ─────────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen bg-background text-textPrimary">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4" />
          <p>Cargando sillas disponibles...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center h-screen bg-background text-textPrimary gap-4">
        <p className="text-red-400">{error}</p>
        <button
          onClick={() => navigate(-1)}
          className="px-4 py-2 bg-surfaceAlt rounded-lg hover:bg-surface transition-colors"
        >
          <ArrowLeft className="w-4 h-4 inline mr-2" />
          Volver
        </button>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 flex bg-background text-textPrimary overflow-hidden">
      {/* Header */}
      <div className="absolute top-0 left-0 right-0 bg-surface border-b border-surfaceAlt px-4 py-3 flex items-center gap-4 z-10">
        <button
          onClick={() => navigate(-1)}
          className="text-textMuted hover:text-textPrimary transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <h1 className="text-lg font-bold">{event?.eventName}</h1>
          <p className="text-xs text-textMuted">Selecciona tus sillas</p>
        </div>
        <div className="ml-auto flex items-center gap-2">
          <button
            onClick={() => setZoom((z) => Math.max(0.3, z - 0.1))}
            className="px-3 py-1 bg-surfaceAlt rounded text-sm hover:bg-surface transition-colors"
          >
            -
          </button>
          <span className="text-sm text-textMuted">{Math.round(zoom * 100)}%</span>
          <button
            onClick={() => setZoom((z) => Math.min(2, z + 0.1))}
            className="px-3 py-1 bg-surfaceAlt rounded text-sm hover:bg-surface transition-colors"
          >
            +
          </button>
        </div>
      </div>

      {/* Canvas */}
      <div className="flex-1 overflow-auto pt-16 pb-4">
        <Stage
          width={window.innerWidth - 320}
          height={window.innerHeight - 80}
          scaleX={zoom}
          scaleY={zoom}
          style={{ backgroundColor: '#1a1a1a' }}
        >
          <Layer>
            {layoutElements.map((section) => (
              <SectionRenderer
                key={section.id}
                section={section}
                seats={seats}
                currentUserId={currentUserId}
                reserving={reserving}
                onReserveSeat={handleReserveSeat}
                onReleaseSeat={handleReleaseSeat}
              />
            ))}
          </Layer>
        </Stage>
      </div>

      {/* Carrito lateral */}
      <CartPanel
        cart={cart}
        onReleaseSeat={handleReleaseSeat}
        reserving={reserving}
      />
    </div>
  );
}

/**
 * Renderiza una sección con sus sillas clickeables.
 */
function SectionRenderer({
  section,
  seats,
  currentUserId,
  reserving,
  onReserveSeat,
  onReleaseSeat,
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
                  curr.handleOut.x, curr.handleOut.y,
                  next.handleIn.x, next.handleIn.y,
                  next.x, next.y
                );
              } else {
                ctx.lineTo(next.x, next.y);
              }
            }
            ctx.closePath();
            ctx.fillStrokeShape(shape);
          }}
          fill={section.color}
          opacity={0.4}
          stroke="rgba(255,255,255,0.2)"
          strokeWidth={1}
          listening={false}
        />
      ) : (
        <Rect
          width={section.width}
          height={section.height}
          fill={section.color}
          opacity={0.4}
          cornerRadius={6}
          stroke="rgba(255,255,255,0.2)"
          strokeWidth={1}
          listening={false}
        />
      )}

      {/* Label de la sección */}
      <Text
        x={labelCenter.x}
        y={labelCenter.y - 10}
        text={section.label}
        fontSize={14}
        fontStyle="bold"
        fill="#ffffff"
        align="center"
        offsetX={0}
        listening={false}
      />

      {/* Sillas individuales */}
      {seatPositions.map((pos, idx) => {
        const seat = seatList[idx];
        if (!seat) return null;

        return (
          <SeatCircle
            key={seat.seatId}
            seat={seat}
            position={pos}
            currentUserId={currentUserId}
            isReserving={reserving.has(seat.seatId)}
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
 */
function SeatCircle({ seat, position, currentUserId, isReserving, onReserve, onRelease }) {
  const isMyReservation = seat.status === 'RESERVED' && seat.reservedBy === currentUserId;
  const isOtherReservation = seat.status === 'RESERVED' && seat.reservedBy !== currentUserId;

  const getSeatColor = () => {
    if (isReserving) return '#9CA3AF'; // gris mientras procesa
    if (seat.status === 'AVAILABLE') return '#10B981'; // verde
    if (isMyReservation) return '#3B82F6'; // azul (mi reserva)
    if (isOtherReservation) return '#FBBF24'; // amarillo (reservado por otro)
    if (seat.status === 'SOLD') return '#6B7280'; // gris
    if (seat.status === 'COURTESY') return '#8B5CF6'; // morado
    return '#6B7280';
  };

  const isClickable = seat.status === 'AVAILABLE' || isMyReservation;

  const handleClick = () => {
    if (!isClickable || isReserving) return;
    if (isMyReservation) {
      onRelease();
    } else {
      onReserve();
    }
  };

  return (
    <Circle
      x={position.x}
      y={position.y}
      radius={position.r}
      fill={getSeatColor()}
      opacity={0.9}
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
function CartPanel({ cart, onReleaseSeat, reserving }) {
  if (cart.length === 0) {
    return (
      <div className="w-80 bg-surface border-l border-surfaceAlt p-4">
        <div className="flex items-center gap-2 mb-4">
          <ShoppingCart className="w-5 h-5 text-primary" />
          <h2 className="text-lg font-bold">Tus Sillas</h2>
        </div>
        <p className="text-sm text-textMuted text-center py-8">
          No has seleccionado ninguna silla
        </p>
      </div>
    );
  }

  const totalPrice = cart.reduce((sum, seat) => {
    // Aquí deberías obtener el precio de la sección correspondiente
    return sum + 0; // placeholder
  }, 0);

  return (
    <div className="w-80 bg-surface border-l border-surfaceAlt p-4 flex flex-col">
      <div className="flex items-center gap-2 mb-4">
        <ShoppingCart className="w-5 h-5 text-primary" />
        <h2 className="text-lg font-bold">Tus Sillas</h2>
        <span className="ml-auto text-sm text-textMuted">({cart.length})</span>
      </div>

      <div className="flex-1 overflow-y-auto space-y-2">
        {cart.map((seat) => (
          <CartItem
            key={seat.seatId}
            seat={seat}
            onRelease={() => onReleaseSeat(seat.seatId)}
            isReleasing={reserving.has(seat.seatId)}
          />
        ))}
      </div>

      <div className="mt-4 pt-4 border-t border-surfaceAlt">
        <button
          className="w-full py-3 bg-primary text-white rounded-lg font-semibold hover:bg-primary/90 transition-colors"
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
function CartItem({ seat, onRelease, isReleasing }) {
  const [timeLeft, setTimeLeft] = useState('');

  useEffect(() => {
    if (!seat.reservedUntil) return;

    const updateTimer = () => {
      const now = new Date();
      const until = new Date(seat.reservedUntil);
      const diffMs = until - now;

      if (diffMs <= 0) {
        setTimeLeft('Expirado');
        return;
      }

      const minutes = Math.floor(diffMs / 1000 / 60);
      const seconds = Math.floor((diffMs / 1000) % 60);
      setTimeLeft(`${minutes}:${seconds.toString().padStart(2, '0')}`);
    };

    updateTimer();
    const interval = setInterval(updateTimer, 1000);

    return () => clearInterval(interval);
  }, [seat.reservedUntil]);

  return (
    <div className="bg-surfaceAlt rounded-lg p-3 flex items-center gap-3">
      <div className="flex-1">
        <p className="font-semibold text-sm">{seat.code}</p>
        <div className="flex items-center gap-1 text-xs text-textMuted mt-1">
          <Clock className="w-3 h-3" />
          <span>{timeLeft}</span>
        </div>
      </div>
      <button
        onClick={onRelease}
        disabled={isReleasing}
        className="p-1 hover:bg-surface rounded transition-colors disabled:opacity-50"
        title="Liberar silla"
      >
        <X className="w-4 h-4 text-textMuted hover:text-red-400" />
      </button>
    </div>
  );
}
