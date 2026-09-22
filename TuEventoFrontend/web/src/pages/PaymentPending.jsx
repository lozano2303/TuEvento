import { useState, useEffect, useRef } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import { CheckCircle, XCircle, Loader2, Ticket, AlertCircle, RefreshCcw } from 'lucide-react';
import { getPayment } from '../services/PaymentService';
import { getOrderTickets } from '../services/OrderService';

const POLL_INTERVAL_MS = 3000;

/**
 * Pantalla de espera de pago.
 *
 * Recibe:
 *   - paymentId por URL param (/checkout/pending/:paymentId)
 *   - state: { orderId, eventId, cartItems, eventTitle }
 *
 * Estados del pago:
 *   PENDING  → spinner + "Esperando confirmación"
 *   APPROVED → éxito + tickets
 *   REJECTED / FAILED → error + botón reintentar
 */
export default function PaymentPending() {
  const { paymentId } = useParams();
  const location      = useLocation();
  const navigate      = useNavigate();

  const { orderId, eventId, cartItems = [], eventTitle = 'Evento' } = location.state || {};

  const [status, setStatus]   = useState('PENDING');
  const [tickets, setTickets] = useState([]);
  const [error, setError]     = useState(null);
  const intervalRef           = useRef(null);

  // Detener polling
  const stopPolling = () => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  };

  useEffect(() => {
    if (!paymentId) {
      navigate('/events', { replace: true });
      return;
    }

    const poll = async () => {
      try {
        const result = await getPayment(paymentId);
        const currentStatus = result.data.status ?? result.data.paymentStatus;

        setStatus(currentStatus);

        if (currentStatus === 'APPROVED') {
          stopPolling();
          // Cargar tickets
          if (orderId) {
            try {
              const ticketResult = await getOrderTickets(orderId);
              setTickets(ticketResult.data ?? []);
            } catch {
              // No bloquear la pantalla de éxito si los tickets fallan
            }
          }
        } else if (currentStatus === 'REJECTED' || currentStatus === 'ERROR') {
          stopPolling();
        }
      } catch (err) {
        // Error de red — no cambiar estado, seguir intentando
        console.warn('[PaymentPending] poll error:', err.message);
      }
    };

    // Primer poll inmediato
    poll();
    // Polling cada 3 segundos
    intervalRef.current = setInterval(poll, POLL_INTERVAL_MS);

    return () => stopPolling();
  }, [paymentId, orderId, navigate]);

  const handleRetry = () => {
    navigate(`/events/${eventId}`, {
      state: { restoreCart: true },
    });
  };

  const handleGoToEvents = () => {
    navigate('/events');
  };

  return (
    <div
      className="min-h-screen flex flex-col items-center justify-center px-4 pb-16"
      style={{ background: 'var(--color-background)' }}
    >
      <div className="w-full max-w-md">

        {/* ── PENDING ────────────────────────────────────────────────────── */}
        {status === 'PENDING' && (
          <div
            className="rounded-2xl p-10 flex flex-col items-center gap-6 text-center"
            style={{ background: 'var(--color-surface)', border: '1px solid rgba(167,139,250,0.15)' }}
          >
            <div
              className="w-20 h-20 rounded-full flex items-center justify-center"
              style={{ background: 'rgba(167,139,250,0.12)' }}
            >
              <Loader2 className="w-10 h-10 animate-spin" style={{ color: 'var(--color-primary)' }} />
            </div>

            <div>
              <h1 className="text-xl font-bold mb-2" style={{ color: 'var(--color-textPrimary)' }}>
                Esperando confirmación
              </h1>
              <p className="text-sm" style={{ color: 'var(--color-textSecondary)' }}>
                Tu pago está siendo procesado. Esto puede tardar unos segundos.
              </p>
            </div>

            <div
              className="w-full rounded-lg p-4 text-left space-y-1"
              style={{ background: 'rgba(167,139,250,0.06)', border: '1px solid rgba(167,139,250,0.1)' }}
            >
              <p className="text-xs" style={{ color: 'var(--color-textMuted)' }}>
                Evento: <span style={{ color: 'var(--color-textSecondary)' }}>{eventTitle}</span>
              </p>
              <p className="text-xs" style={{ color: 'var(--color-textMuted)' }}>
                Sillas: <span style={{ color: 'var(--color-textSecondary)' }}>{cartItems.length}</span>
              </p>
              <p className="text-xs" style={{ color: 'var(--color-textMuted)' }}>
                ID de pago: <span className="font-mono" style={{ color: 'var(--color-textSecondary)' }}>{paymentId}</span>
              </p>
            </div>

            <p className="text-xs" style={{ color: 'var(--color-textMuted)' }}>
              No cierres esta ventana mientras se procesa el pago.
            </p>
          </div>
        )}

        {/* ── APPROVED ───────────────────────────────────────────────────── */}
        {status === 'APPROVED' && (
          <div
            className="rounded-2xl p-8 flex flex-col items-center gap-6 text-center"
            style={{ background: 'var(--color-surface)', border: '1px solid rgba(5,150,105,0.25)' }}
          >
            <div
              className="w-20 h-20 rounded-full flex items-center justify-center"
              style={{ background: 'var(--color-successBg)' }}
            >
              <CheckCircle className="w-10 h-10" style={{ color: 'var(--color-success)' }} />
            </div>

            <div>
              <h1 className="text-xl font-bold mb-2" style={{ color: 'var(--color-textPrimary)' }}>
                ¡Pago aprobado!
              </h1>
              <p className="text-sm" style={{ color: 'var(--color-textSecondary)' }}>
                Tu compra fue exitosa. Acá están tus tickets.
              </p>
            </div>

            {/* Tickets */}
            {tickets.length > 0 && (
              <div className="w-full space-y-2">
                <h2 className="text-xs font-semibold uppercase tracking-wider text-left mb-3"
                  style={{ color: 'var(--color-textMuted)' }}
                >
                  Tus tickets
                </h2>
                {tickets.map((ticket) => (
                  <div
                    key={ticket.ticketId}
                    className="flex items-center gap-3 p-3 rounded-xl"
                    style={{
                      background: 'rgba(5,150,105,0.08)',
                      border: '1px solid rgba(5,150,105,0.2)',
                    }}
                  >
                    <Ticket className="w-4 h-4 shrink-0" style={{ color: 'var(--color-success)' }} />
                    <div className="flex-1 text-left">
                      <p className="text-sm font-semibold" style={{ color: 'var(--color-textPrimary)' }}>
                        {ticket.code ?? `Ticket #${ticket.ticketId}`}
                      </p>
                      <p className="text-xs font-mono" style={{ color: 'var(--color-textMuted)' }}>
                        ${ticket.totalPrice?.toLocaleString('es-CO') ?? '0'} {ticket.currency ?? ''}
                      </p>
                    </div>
                    {ticket.qrCode && (
                      <span
                        className="text-xs font-mono px-2 py-1 rounded"
                        style={{ background: 'rgba(5,150,105,0.12)', color: 'var(--color-success)' }}
                      >
                        {ticket.qrCode}
                      </span>
                    )}
                  </div>
                ))}
              </div>
            )}

            {/* Si no hay tickets todavía, mensaje genérico */}
            {tickets.length === 0 && (
              <div
                className="w-full p-4 rounded-xl text-sm"
                style={{ background: 'rgba(5,150,105,0.08)', color: 'var(--color-textSecondary)' }}
              >
                Tus tickets serán enviados a tu correo en breve.
              </div>
            )}

            <button
              onClick={handleGoToEvents}
              className="w-full py-3 rounded-xl font-semibold text-sm transition-all"
              style={{
                background: 'linear-gradient(135deg, var(--color-primaryDark) 0%, var(--color-primary) 100%)',
                color: 'var(--color-onPrimary)',
              }}
            >
              Ver más eventos
            </button>
          </div>
        )}

        {/* ── REJECTED / FAILED ──────────────────────────────────────────── */}
        {(status === 'REJECTED' || status === 'ERROR') && (
          <div
            className="rounded-2xl p-8 flex flex-col items-center gap-6 text-center"
            style={{ background: 'var(--color-surface)', border: '1px solid var(--color-error)' }}
          >
            <div
              className="w-20 h-20 rounded-full flex items-center justify-center"
              style={{ background: 'var(--color-errorBg)' }}
            >
              <XCircle className="w-10 h-10" style={{ color: 'var(--color-error)' }} />
            </div>

            <div>
              <h1 className="text-xl font-bold mb-2" style={{ color: 'var(--color-textPrimary)' }}>
                {status === 'REJECTED' ? 'Pago rechazado' : 'Error en el pago'}
              </h1>
              <p className="text-sm" style={{ color: 'var(--color-textSecondary)' }}>
                {status === 'REJECTED'
                  ? 'El pago fue rechazado por el procesador. Verificá los datos e intentá de nuevo.'
                  : 'Ocurrió un error al procesar el pago. Podés intentarlo nuevamente.'
                }
              </p>            </div>

            {error && (
              <div
                className="w-full flex items-start gap-2 p-3 rounded-lg text-left"
                style={{ background: 'var(--color-errorBg)' }}
              >
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" style={{ color: 'var(--color-error)' }} />
                <p className="text-xs" style={{ color: 'var(--color-error)' }}>{error}</p>
              </div>
            )}

            <div className="w-full space-y-3">
              <button
                onClick={handleRetry}
                className="w-full py-3 rounded-xl font-semibold text-sm transition-all flex items-center justify-center gap-2"
                style={{
                  background: 'linear-gradient(135deg, var(--color-primaryDark) 0%, var(--color-primary) 100%)',
                  color: 'var(--color-onPrimary)',
                }}
              >
                <RefreshCcw className="w-4 h-4" />
                Reintentar
              </button>

              <button
                onClick={handleGoToEvents}
                className="w-full py-2 rounded-xl text-sm transition-opacity hover:opacity-70"
                style={{ color: 'var(--color-textSecondary)' }}
              >
                Volver a eventos
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
