import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Loader2, Wallet } from 'lucide-react';
import { getPayment } from '../services/PaymentService';
import { getOrderTickets } from '../services/OrderService';
import { getMyWallet } from '../services/WalletService';
import PaymentApprovedSummary from '../components/PaymentApprovedSummary';

/**
 * Pantalla de confirmación para pagos 100% con wallet (wallet-only).
 *
 * Flujo:
 *   1. Al montar: carga el payment, tickets, y saldo restante de wallet
 *   2. Muestra el resumen de tickets aprobados (igual que PaymentPending APPROVED)
 *   3. Además muestra el saldo restante de wallet después del pago
 *   4. Botón para volver al evento
 */
export default function PaymentConfirmation() {
  const { paymentId } = useParams();
  const navigate      = useNavigate();

  const [loading, setLoading]             = useState(true);
  const [error, setError]                 = useState(null);
  const [payment, setPayment]             = useState(null);
  const [tickets, setTickets]             = useState([]);
  const [walletBalance, setWalletBalance] = useState(null);

  useEffect(() => {
    if (!paymentId) {
      navigate('/events', { replace: true });
      return;
    }

    const loadData = async () => {
      try {
        // 1. Obtener el payment para verificar que es APPROVED y obtener orderId
        const paymentRes = await getPayment(paymentId);
        const paymentData = paymentRes.data;
        setPayment(paymentData);

        if (paymentData.status !== 'APPROVED') {
          setError('El pago no está aprobado');
          setLoading(false);
          return;
        }

        // 2. Obtener tickets de la orden
        const ticketsRes = await getOrderTickets(paymentData.orderId);
        setTickets(ticketsRes.data ?? []);

        // 3. Obtener saldo restante de wallet
        try {
          const walletRes = await getMyWallet();
          setWalletBalance(walletRes.data);
        } catch (err) {
          console.warn('No se pudo cargar el saldo de wallet:', err.message);
          // No es crítico, continuar sin mostrar saldo
        }

        setLoading(false);
      } catch (err) {
        console.error('[PaymentConfirmation] error:', err);
        setError(err.message || 'Error al cargar los datos');
        setLoading(false);
      }
    };

    loadData();
  }, [paymentId, navigate]);

  const handleGoToEvent = () => {
    if (payment?.orderId) {
      // Buscar el eventId desde el payment o hacer otra llamada si es necesario
      // Por ahora navegamos a eventos generales
      navigate('/events');
    }
  };

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div
      className="min-h-screen flex flex-col items-center justify-center px-4 pb-16"
      style={{ background: 'var(--color-background)' }}
    >
      <div className="w-full max-w-md">

        {/* ── LOADING ──────────────────────────────────────────────────── */}
        {loading && (
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
            <p className="text-sm" style={{ color: 'var(--color-textSecondary)' }}>
              Cargando tu confirmación…
            </p>
          </div>
        )}

        {/* ── ERROR ────────────────────────────────────────────────────── */}
        {error && !loading && (
          <div
            className="rounded-2xl p-8 flex flex-col items-center gap-6 text-center"
            style={{ background: 'var(--color-surface)', border: '1px solid var(--color-error)' }}
          >
            <p className="text-sm" style={{ color: 'var(--color-error)' }}>
              {error}
            </p>
            <button
              onClick={() => navigate('/events')}
              className="w-full py-3 rounded-xl font-semibold text-sm"
              style={{
                background: 'linear-gradient(135deg, var(--color-primaryDark) 0%, var(--color-primary) 100%)',
                color: 'var(--color-onPrimary)',
              }}
            >
              Volver a eventos
            </button>
          </div>
        )}

        {/* ── SUCCESS ──────────────────────────────────────────────────── */}
        {!loading && !error && payment && (
          <PaymentApprovedSummary
            tickets={tickets}
            onGoEvents={handleGoToEvent}
            additionalInfo={
              walletBalance && (
                <div
                  className="w-full flex items-center justify-between p-4 rounded-xl"
                  style={{
                    background: 'rgba(167,139,250,0.08)',
                    border: '1px solid rgba(167,139,250,0.15)',
                  }}
                >
                  <div className="flex items-center gap-2">
                    <Wallet className="w-4 h-4" style={{ color: 'var(--color-primary)' }} />
                    <span className="text-sm font-semibold" style={{ color: 'var(--color-textPrimary)' }}>
                      Tu saldo restante
                    </span>
                  </div>
                  <span className="text-sm font-bold" style={{ color: 'var(--color-primary)' }}>
                    ${(walletBalance.availableBalance ?? 0).toLocaleString('es-CO')}
                  </span>
                </div>
              )
            }
          />
        )}

      </div>
    </div>
  );
}
