import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import { CheckCircle, XCircle, Loader2, Ticket, AlertCircle, RefreshCcw } from 'lucide-react';
import { getPayment } from '../services/PaymentService';
import { getOrderTickets } from '../services/OrderService';

const POLL_INTERVAL_MS  = 3000;
const GATEWAY_BASE_URL  = import.meta.env.VITE_GATEWAY_URL || 'http://localhost:4001';
const POPUP_OPTIONS     = 'width=480,height=640,left=200,top=80,resizable=no,scrollbars=yes';

/**
 * Pantalla de espera de pago.
 *
 * Flujo:
 *   1. Al montar: hace GET /api/v1/payments/:paymentId para obtener gatewayTransactionId
 *   2. En cuanto tiene el gatewayTransactionId: abre el popup del gateway automáticamente
 *   3. El popup (pay.html) permite aprobar/rechazar/fallar/cancelar
 *   4. El polling detecta el cambio de estado y actualiza la pantalla
 *   5. Si el usuario cierra el popup manualmente, el polling sigue corriendo
 */
export default function PaymentPending() {
  const { paymentId } = useParams();
  const location      = useLocation();
  const navigate      = useNavigate();

  const { orderId, eventId, cartItems = [], eventTitle = 'Evento' } = location.state || {};

  const [status, setStatus]       = useState('PENDING');
  const [gatewayTxId, setGwTxId]  = useState(null);
  const [tickets, setTickets]     = useState([]);
  const [error, setError]         = useState(null);
  const [cancelledByUser, setCancelledByUser] = useState(false); // CANCELLED vs ERROR técnico
  const [popupOpen, setPopupOpen] = useState(false);

  const intervalRef   = useRef(null);
  const popupRef      = useRef(null);
  const popupOpened   = useRef(false); // evitar abrir dos veces

  // ── Detener polling ───────────────────────────────────────────────────────
  const stopPolling = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  }, []);

  // ── Cargar tickets al aprobar ─────────────────────────────────────────────
  const loadTickets = useCallback(async () => {
    if (!orderId) return;
    try {
      const r = await getOrderTickets(orderId);
      setTickets(r.data ?? []);
    } catch { /* no bloquear la pantalla de éxito */ }
  }, [orderId]);

  // ── Abrir popup del gateway ───────────────────────────────────────────────
  const openPopup = useCallback((txId) => {
    if (popupOpened.current) return;
    popupOpened.current = true;

    const url   = `${GATEWAY_BASE_URL}/pay.html?id=${txId}`;
    const popup = window.open(url, 'fake-gateway', POPUP_OPTIONS);
    popupRef.current = popup;
    setPopupOpen(true);

    // Vigilar cierre del popup para hacer poll inmediato
    const watchClose = setInterval(() => {
      if (!popup || popup.closed) {
        clearInterval(watchClose);
        setPopupOpen(false);
        // Poll inmediato al cerrar
        getPayment(paymentId)
          .then(r => {
            const s = r.data.status;
            setStatus(s);
            if (s === 'APPROVED') { stopPolling(); loadTickets(); }
            if (s === 'REJECTED' || s === 'ERROR') { stopPolling(); }
          })
          .catch(() => {});
      }
    }, 500);
  }, [paymentId, stopPolling, loadTickets]);

  // Escuchar mensajes del popup (postMessage desde pay.html)
  useEffect(() => {
    const handleMessage = (event) => {
      if (event.data?.type !== 'GATEWAY_ACTION') return;
      const action = event.data.action;
      if (action === 'cancel') {
        setCancelledByUser(true);
        // El backend tarda un momento en procesar el webhook — forzar poll en 2s
        setTimeout(() => {
          getPayment(paymentId)
            .then(r => {
              setStatus(r.data.status);
              stopPolling();
            })
            .catch(() => {});
        }, 2000);
      }
    };
    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, [paymentId, stopPolling]);

  // ── Polling principal al backend de Tu Evento ─────────────────────────────
  useEffect(() => {
    if (!paymentId) {
      navigate('/events', { replace: true });
      return;
    }

    const poll = async () => {
      try {
        const r    = await getPayment(paymentId);
        const data = r.data;

        // Abrir popup automáticamente en cuanto tengamos el gatewayTransactionId
        if (data.gatewayTransactionId && !popupOpened.current) {
          setGwTxId(data.gatewayTransactionId);
          openPopup(data.gatewayTransactionId);
        }

        const s = data.status;
        setStatus(s);

        if (s === 'APPROVED') {
          stopPolling();
          loadTickets();
          if (popupRef.current && !popupRef.current.closed) popupRef.current.close();
        } else if (s === 'REJECTED' || s === 'ERROR') {
          stopPolling();
          if (popupRef.current && !popupRef.current.closed) popupRef.current.close();
        }
      } catch (err) {
        console.warn('[PaymentPending] poll error:', err.message);
      }
    };

    poll();
    intervalRef.current = setInterval(poll, POLL_INTERVAL_MS);
    return () => stopPolling();
  }, [paymentId, navigate, stopPolling, loadTickets, openPopup]);

  const handleRetry    = () => navigate(`/events/${eventId}`, { state: { restoreCart: true } });
  const handleGoEvents = () => navigate('/events');

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div
      className="min-h-screen flex flex-col items-center justify-center px-4 pb-16"
      style={{ background: 'var(--color-background)' }}
    >
      <div className="w-full max-w-md">

        {/* ── PENDING ──────────────────────────────────────────────────── */}
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
                Procesando tu pago
              </h1>
              <p className="text-sm" style={{ color: 'var(--color-textSecondary)' }}>
                {popupOpen
                  ? 'Completá el pago en la ventana que se abrió.'
                  : 'Abriendo la ventana de pago…'}
              </p>
            </div>

            <div
              className="w-full rounded-xl p-4 text-left space-y-2"
              style={{ background: 'rgba(167,139,250,0.06)', border: '1px solid rgba(167,139,250,0.1)' }}
            >
              <div className="flex justify-between text-xs">
                <span style={{ color: 'var(--color-textMuted)' }}>Evento</span>
                <span style={{ color: 'var(--color-textSecondary)' }}>{eventTitle}</span>
              </div>
              <div className="flex justify-between text-xs">
                <span style={{ color: 'var(--color-textMuted)' }}>Sillas</span>
                <span style={{ color: 'var(--color-textSecondary)' }}>{cartItems.length}</span>
              </div>
            </div>

            {/* Si el popup fue cerrado manualmente, ofrecer reabrirlo */}
            {!popupOpen && popupOpened.current && gatewayTxId && (
              <button
                onClick={() => { popupOpened.current = false; openPopup(gatewayTxId); }}
                className="text-sm underline underline-offset-2 transition-opacity hover:opacity-70"
                style={{ color: 'var(--color-accent)' }}
              >
                Volver a abrir la ventana de pago
              </button>
            )}

            <p className="text-xs" style={{ color: 'var(--color-textMuted)' }}>
              No cierres esta pestaña mientras se procesa el pago.
            </p>
          </div>
        )}

        {/* ── APPROVED ─────────────────────────────────────────────────── */}
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

            {tickets.length > 0 ? (
              <div className="w-full space-y-2">
                <h2
                  className="text-xs font-semibold uppercase tracking-wider text-left mb-3"
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
                      <p className="text-xs" style={{ color: 'var(--color-textMuted)' }}>
                        {ticket.totalPrice?.toLocaleString('es-CO') ?? '0'} {ticket.currency ?? ''}
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
            ) : (
              <div
                className="w-full p-4 rounded-xl text-sm"
                style={{ background: 'rgba(5,150,105,0.08)', color: 'var(--color-textSecondary)' }}
              >
                Tus tickets serán enviados a tu correo en breve.
              </div>
            )}

            <button
              onClick={handleGoEvents}
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

        {/* ── REJECTED / ERROR ─────────────────────────────────────────── */}
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
                {cancelledByUser
                  ? 'Pago cancelado'
                  : status === 'REJECTED'
                  ? 'Pago rechazado'
                  : 'Error en el pago'}
              </h1>
              <p className="text-sm" style={{ color: 'var(--color-textSecondary)' }}>
                {cancelledByUser
                  ? 'Cancelaste la transacción. Podés volver a intentarlo cuando quieras.'
                  : status === 'REJECTED'
                  ? 'El pago fue rechazado. Podés volver a intentarlo.'
                  : 'Ocurrió un error al procesar el pago. Podés intentarlo nuevamente.'}
              </p>
            </div>

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
                onClick={handleGoEvents}
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
