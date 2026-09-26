import { CheckCircle, Ticket } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

/**
 * Componente reutilizable para mostrar el resumen de un pago aprobado con tickets.
 * Usado en PaymentPending (polling) y PaymentConfirmation (wallet-only).
 *
 * @param {Object} props
 * @param {Array} props.tickets - Lista de tickets obtenidos
 * @param {Function} props.onGoEvents - Callback para navegar a eventos (opcional)
 * @param {React.ReactNode} props.additionalInfo - Contenido adicional a mostrar (ej. saldo restante)
 */
export default function PaymentApprovedSummary({ tickets = [], onGoEvents, additionalInfo }) {
  const navigate = useNavigate();

  const handleGoEvents = () => {
    if (onGoEvents) {
      onGoEvents();
    } else {
      navigate('/events');
    }
  };

  return (
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

      {/* Información adicional (ej. saldo restante de wallet) */}
      {additionalInfo}

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
  );
}
