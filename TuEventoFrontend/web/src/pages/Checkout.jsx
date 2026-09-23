import { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ShoppingCart, ArrowLeft, CreditCard, AlertCircle, Loader2, Ticket } from 'lucide-react';
import { createOrder } from '../services/OrderService';
import { createPayment } from '../services/PaymentService';

/**
 * Pantalla de checkout.
 *
 * Recibe por navigate state:
 *   - eventId: number
 *   - seatIds: number[]
 *   - cartItems: { seatId, code, sectionName, price }[]
 *   - eventTitle: string
 *
 * Flujo:
 *   1. Al montar → createOrder({ eventId, seatIds })
 *   2. Muestra resumen de la orden (sillas + total)
 *   3. Botón "Pagar" → createPayment({ orderId, paymentMethod: 'QR' })
 *   4. Navega a /checkout/pending/:paymentId
 */
export default function Checkout() {
  const location = useLocation();
  const navigate = useNavigate();

  // Datos del carrito pasados desde EventDetail
  const { eventId, seatIds, cartItems = [], eventTitle = 'Evento' } = location.state || {};

  const [order, setOrder]       = useState(null);
  const [loading, setLoading]   = useState(true);  // cargando createOrder
  const [paying, setPaying]     = useState(false);  // cargando createPayment
  const [error, setError]       = useState(null);

  // Si llegamos sin datos de carrito, volver
  useEffect(() => {
    if (!eventId || !seatIds?.length) {
      navigate('/events', { replace: true });
    }
  }, [eventId, seatIds, navigate]);

  // Crear la orden al montar
  useEffect(() => {
    if (!eventId || !seatIds?.length) return;

    const doCreateOrder = async () => {
      setLoading(true);
      setError(null);
      try {
        const result = await createOrder({ eventId, seatIds });
        setOrder(result.data);
      } catch (err) {
        setError(err.message || 'No se pudo crear la orden. Puede que alguna silla ya no esté disponible.');
      } finally {
        setLoading(false);
      }
    };

    doCreateOrder();
  }, [eventId, seatIds]);

  const handlePay = async () => {
    if (!order) return;
    setPaying(true);
    setError(null);
    try {
      const result = await createPayment({ orderId: order.orderId ?? order.id, paymentMethod: 'QR' });
      const paymentId = result.data.paymentId ?? result.data.id;
      navigate(`/checkout/pending/${paymentId}`, {
        state: { orderId: order.orderId ?? order.id, eventId, cartItems, eventTitle },
      });
    } catch (err) {
      setError(err.message || 'No se pudo iniciar el pago. Intentá de nuevo.');
      setPaying(false);
    }
  };

  const handleBack = () => {
    navigate(`/events/${eventId}`);
  };

  // ── Calcular total desde cartItems si la orden aún no cargó ─────────────
  const localTotal = cartItems.reduce((sum, item) => sum + (item.price ?? 0), 0);
  const orderTotal = order?.totalAmount ?? order?.total ?? localTotal;

  return (
    <div
      className="min-h-screen flex flex-col items-center justify-start pt-12 px-4 pb-16"
      style={{ background: 'var(--color-background)' }}
    >
      {/* Header */}
      <div className="w-full max-w-lg mb-8">
        <button
          onClick={handleBack}
          className="flex items-center gap-2 text-sm mb-6 transition-opacity hover:opacity-70"
          style={{ color: 'var(--color-textSecondary)' }}
        >
          <ArrowLeft className="w-4 h-4" />
          Volver a selección
        </button>

        <h1 className="text-2xl font-bold mb-1" style={{ color: 'var(--color-textPrimary)' }}>
          Confirmar compra
        </h1>
        <p className="text-sm" style={{ color: 'var(--color-textSecondary)' }}>
          {eventTitle}
        </p>
      </div>

      {/* Contenido */}
      <div className="w-full max-w-lg space-y-4">

        {/* Estado: cargando orden */}
        {loading && (
          <div
            className="rounded-xl p-8 flex flex-col items-center gap-4"
            style={{ background: 'var(--color-surface)', border: '1px solid rgba(167,139,250,0.15)' }}
          >
            <Loader2 className="w-8 h-8 animate-spin" style={{ color: 'var(--color-primary)' }} />
            <p className="text-sm" style={{ color: 'var(--color-textSecondary)' }}>
              Preparando tu orden…
            </p>
          </div>
        )}

        {/* Estado: error en createOrder */}
        {!loading && error && !order && (
          <div
            className="rounded-xl p-6 flex flex-col gap-4"
            style={{ background: 'var(--color-errorBg)', border: '1px solid var(--color-error)' }}
          >
            <div className="flex items-start gap-3">
              <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" style={{ color: 'var(--color-error)' }} />
              <div>
                <p className="text-sm font-semibold mb-1" style={{ color: 'var(--color-error)' }}>
                  No se pudo crear la orden
                </p>
                <p className="text-xs" style={{ color: 'var(--color-textSecondary)' }}>
                  {error}
                </p>
              </div>
            </div>
            <button
              onClick={handleBack}
              className="w-full py-2 rounded-lg text-sm font-semibold transition-all"
              style={{
                background: 'var(--color-surface)',
                color: 'var(--color-textPrimary)',
                border: '1px solid var(--color-error)',
              }}
            >
              Volver a selección de sillas
            </button>
          </div>
        )}

        {/* Estado: orden creada — resumen */}
        {!loading && order && (
          <>
            {/* Tarjeta de sillas */}
            <div
              className="rounded-xl p-5"
              style={{ background: 'var(--color-surface)', border: '1px solid rgba(167,139,250,0.15)' }}
            >
              <div className="flex items-center gap-2 mb-4">
                <ShoppingCart className="w-4 h-4" style={{ color: 'var(--color-accent)' }} />
                <h2 className="text-sm font-semibold" style={{ color: 'var(--color-textPrimary)' }}>
                  Sillas seleccionadas
                </h2>
                <span
                  className="ml-auto text-xs px-2 py-0.5 rounded-full"
                  style={{ background: 'rgba(167,139,250,0.15)', color: 'var(--color-accent)' }}
                >
                  {cartItems.length} {cartItems.length === 1 ? 'silla' : 'sillas'}
                </span>
              </div>

              <div className="space-y-2">
                {cartItems.map((item) => (
                  <div key={item.seatId} className="flex items-center justify-between text-sm">
                    <div className="flex items-center gap-2">
                      <Ticket className="w-3.5 h-3.5" style={{ color: 'var(--color-textMuted)' }} />
                      <span style={{ color: 'var(--color-textPrimary)' }}>{item.code}</span>
                      <span className="text-xs" style={{ color: 'var(--color-textMuted)' }}>
                        · {item.sectionName}
                      </span>
                    </div>
                    <span className="font-medium" style={{ color: 'var(--color-accent)' }}>
                      ${(item.price ?? 0).toLocaleString('es-CO')}
                    </span>
                  </div>
                ))}
              </div>

              <div
                className="mt-4 pt-4 flex items-center justify-between"
                style={{ borderTop: '1px solid rgba(167,139,250,0.15)' }}
              >
                <span className="text-sm font-semibold" style={{ color: 'var(--color-textSecondary)' }}>
                  Total
                </span>
                <span className="text-lg font-bold" style={{ color: 'var(--color-primary)' }}>
                  ${orderTotal.toLocaleString('es-CO')}
                </span>
              </div>
            </div>

            {/* Tarjeta de método de pago */}
            <div
              className="rounded-xl p-5"
              style={{ background: 'var(--color-surface)', border: '1px solid rgba(167,139,250,0.15)' }}
            >
              <div className="flex items-center gap-2 mb-3">
                <CreditCard className="w-4 h-4" style={{ color: 'var(--color-accent)' }} />
                <h2 className="text-sm font-semibold" style={{ color: 'var(--color-textPrimary)' }}>
                  Método de pago
                </h2>
              </div>
              <div
                className="flex items-center gap-3 p-3 rounded-lg"
                style={{ background: 'rgba(167,139,250,0.08)', border: '1px solid rgba(167,139,250,0.2)' }}
              >
                <div
                  className="w-8 h-8 rounded-lg flex items-center justify-center text-lg"
                  style={{ background: 'rgba(167,139,250,0.15)' }}
                >
                  📱
                </div>
                <div>
                  <p className="text-sm font-semibold" style={{ color: 'var(--color-textPrimary)' }}>
                    Código QR
                  </p>
                  <p className="text-xs" style={{ color: 'var(--color-textMuted)' }}>
                    Escanea el QR con tu app bancaria
                  </p>
                </div>
                <div
                  className="ml-auto w-4 h-4 rounded-full border-2 flex items-center justify-center"
                  style={{ borderColor: 'var(--color-primary)' }}
                >
                  <div
                    className="w-2 h-2 rounded-full"
                    style={{ background: 'var(--color-primary)' }}
                  />
                </div>
              </div>
            </div>

            {/* Error de pago */}
            {error && (
              <div
                className="rounded-xl p-4 flex items-start gap-3"
                style={{ background: 'var(--color-errorBg)', border: '1px solid var(--color-error)' }}
              >
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" style={{ color: 'var(--color-error)' }} />
                <p className="text-xs" style={{ color: 'var(--color-error)' }}>{error}</p>
              </div>
            )}

            {/* Botón Pagar */}
            <button
              onClick={handlePay}
              disabled={paying}
              className="w-full py-3 rounded-xl font-semibold text-sm transition-all flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed"
              style={{
                background: 'linear-gradient(135deg, var(--color-primaryDark) 0%, var(--color-primary) 100%)',
                color: 'var(--color-onPrimary)',
              }}
            >
              {paying ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Procesando…
                </>
              ) : (
                <>
                  <CreditCard className="w-4 h-4" />
                  Pagar ${orderTotal.toLocaleString('es-CO')}
                </>
              )}
            </button>

            <p className="text-center text-xs" style={{ color: 'var(--color-textMuted)' }}>
              Al confirmar, tus sillas quedarán reservadas mientras se procesa el pago.
            </p>
          </>
        )}
      </div>
    </div>
  );
}
