import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  TrendingUp, Ticket, Calendar, ChevronRight,
  LayoutDashboard, Loader2, AlertTriangle, DollarSign,
  BarChart2, ShieldCheck,
} from 'lucide-react';

import Footer from '../layouts/Footer';
import { getOrganizerEarnings } from '../services/OrganizerService';
import { useTheme } from '../context/ThemeContext';

// ── Helpers ────────────────────────────────────────────────────────────────────
const fmtCOP = (n) =>
  new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    maximumFractionDigits: 0,
  }).format(n ?? 0);

// ── Componente principal ───────────────────────────────────────────────────────
export default function OrganizerEarnings() {
  const navigate      = useNavigate();
  const { palette }   = useTheme();

  const [earnings,  setEarnings]  = useState(null);  // OrganizerEarningsResponse
  const [loading,   setLoading]   = useState(true);
  const [error,     setError]     = useState(null);

  useEffect(() => {
    getOrganizerEarnings()
      .then((res) => setEarnings(res.data))
      .catch((err) => setError(err.message || 'No se pudieron cargar las ganancias'))
      .finally(() => setLoading(false));
  }, []);

  // ── Loading ────────────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div
        className="min-h-screen w-full flex items-center justify-center"
        style={{ backgroundColor: palette.background }}
      >
        <Loader2 className="w-8 h-8 animate-spin" style={{ color: palette.primary }} />
      </div>
    );
  }

  // ── Error ──────────────────────────────────────────────────────────────────
  if (error) {
    return (
      <div
        className="min-h-screen w-full flex items-center justify-center px-4"
        style={{ backgroundColor: palette.background, color: palette.textPrimary }}
      >
        <div className="text-center max-w-md space-y-3">
          <AlertTriangle className="w-12 h-12 mx-auto" style={{ color: palette.error }} />
          <p className="font-semibold">{error}</p>
          <button
            onClick={() => window.location.reload()}
            className="text-sm px-4 py-2 rounded-xl font-medium transition-colors hover:brightness-110"
            style={{ background: palette.primary, color: '#fff' }}
          >
            Reintentar
          </button>
        </div>
      </div>
    );
  }

  const totalEarnings  = earnings?.totalEarnings  ?? 0;
  const totalTickets   = earnings?.totalTicketsSold ?? 0;
  const byEvent        = earnings?.earningsByEvent  ?? [];

  // ── Estado vacío ───────────────────────────────────────────────────────────
  if (byEvent.length === 0) {
    return (
      <div
        className="min-h-screen w-full flex items-center justify-center px-4"
        style={{ backgroundColor: palette.background, color: palette.textPrimary }}
      >
        <div className="text-center max-w-md space-y-4">
          <div
            className="w-16 h-16 rounded-2xl flex items-center justify-center mx-auto"
            style={{ background: `${palette.primary}20` }}
          >
            <TrendingUp className="w-8 h-8" style={{ color: palette.primary }} />
          </div>
          <h2 className="text-2xl font-bold">Aún no hay ganancias</h2>
          <p className="text-sm leading-relaxed" style={{ color: palette.textSecondary }}>
            Cuando alguien compre tickets en uno de tus eventos publicados,
            las ganancias aparecerán aquí automáticamente.
          </p>
          <button
            onClick={() => navigate('/events/manage')}
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-bold transition-all hover:brightness-110"
            style={{ background: `linear-gradient(135deg, ${palette.primary}, ${palette.accent})`, color: '#fff' }}
          >
            <LayoutDashboard className="w-4 h-4" />
            Ir a mis eventos
          </button>
        </div>
      </div>
    );
  }

  // ── Vista principal ────────────────────────────────────────────────────────
  return (
    <div
      className="min-h-screen w-full"
      style={{ backgroundColor: palette.background, color: palette.textPrimary }}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">

        {/* ── Hero ──────────────────────────────────────────────────────────── */}
        <section className="space-y-2">
          <span
            className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold"
            style={{
              color: palette.accent,
              backgroundColor: `${palette.primary}20`,
              border: `1px solid ${palette.primary}30`,
            }}
          >
            <TrendingUp className="w-3.5 h-3.5" />
            Panel de ganancias
          </span>
          <h1 className="text-3xl md:text-4xl font-black leading-tight">
            Tus{' '}
            <span style={{ color: palette.primary }}>ganancias</span>
          </h1>
          <p className="text-sm leading-relaxed max-w-lg" style={{ color: palette.textSecondary }}>
            Cada vez que alguien compra un ticket en tus eventos, el monto se acredita
            automáticamente en tu cartera. Aquí ves el resumen completo.
          </p>
        </section>

        {/* ── KPI cards ─────────────────────────────────────────────────────── */}
        <div className="grid sm:grid-cols-3 gap-4">

          {/* Total recaudado */}
          <div
            className="rounded-2xl p-5 relative overflow-hidden"
            style={{
              background: `linear-gradient(135deg, ${palette.primary}, ${palette.accent})`,
              boxShadow: `0 8px 32px ${palette.primary}40`,
            }}
          >
            {/* Brillo decorativo */}
            <div
              className="absolute top-0 right-0 w-32 h-32 pointer-events-none"
              style={{
                background: 'radial-gradient(circle at top right, rgba(255,255,255,0.15) 0%, transparent 70%)',
              }}
            />
            <div className="relative z-10 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold uppercase tracking-widest text-white/70">
                  Total recaudado
                </span>
                <DollarSign className="w-4 h-4 text-white/70" />
              </div>
              <p className="text-3xl font-black text-white">{fmtCOP(totalEarnings)}</p>
              <p className="text-xs text-white/60">Acumulado en todos tus eventos</p>
            </div>
          </div>

          {/* Tickets vendidos */}
          <div
            className="rounded-2xl p-5"
            style={{
              backgroundColor: palette.surface,
              border: `1px solid ${palette.textPrimary}15`,
            }}
          >
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span
                  className="text-[10px] font-bold uppercase tracking-widest"
                  style={{ color: palette.textMuted }}
                >
                  Tickets vendidos
                </span>
                <Ticket className="w-4 h-4" style={{ color: palette.textMuted }} />
              </div>
              <p className="text-3xl font-black">{totalTickets.toLocaleString('es-CO')}</p>
              <p className="text-xs" style={{ color: palette.textSecondary }}>
                Pagos aprobados en total
              </p>
            </div>
          </div>

          {/* Eventos con ventas */}
          <div
            className="rounded-2xl p-5"
            style={{
              backgroundColor: palette.surface,
              border: `1px solid ${palette.textPrimary}15`,
            }}
          >
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span
                  className="text-[10px] font-bold uppercase tracking-widest"
                  style={{ color: palette.textMuted }}
                >
                  Eventos con ventas
                </span>
                <Calendar className="w-4 h-4" style={{ color: palette.textMuted }} />
              </div>
              <p className="text-3xl font-black">{byEvent.length}</p>
              <p className="text-xs" style={{ color: palette.textSecondary }}>
                Con al menos un ticket vendido
              </p>
            </div>
          </div>
        </div>

        {/* ── Tabla de ganancias por evento ─────────────────────────────────── */}
        <div
          className="rounded-2xl overflow-hidden"
          style={{
            backgroundColor: palette.surface,
            border: `1px solid ${palette.textPrimary}15`,
          }}
        >
          {/* Header */}
          <div
            className="px-6 py-4 flex items-center justify-between"
            style={{ borderBottom: `1px solid ${palette.textPrimary}10` }}
          >
            <div className="flex items-center gap-2">
              <BarChart2 className="w-4 h-4" style={{ color: palette.primary }} />
              <h2 className="text-sm font-bold">Desglose por evento</h2>
            </div>
            <button
              onClick={() => navigate('/events/manage')}
              className="text-xs font-semibold flex items-center gap-1 transition-colors hover:brightness-110"
              style={{ color: palette.primary }}
            >
              Gestionar eventos
              <ChevronRight className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* Tabla */}
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr style={{ borderBottom: `1px solid ${palette.textPrimary}10` }}>
                  {[
                    { label: 'Evento',          align: 'left'  },
                    { label: 'Tickets vendidos', align: 'right' },
                    { label: 'Recaudado',        align: 'right' },
                  ].map(({ label, align }) => (
                    <th
                      key={label}
                      className={`px-6 py-3 text-[10px] font-bold uppercase tracking-widest ${align === 'right' ? 'text-right' : ''}`}
                      style={{ color: palette.textMuted }}
                    >
                      {label}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {byEvent.map((ev, idx) => {
                  // Barra de progreso relativa al evento con más recaudación
                  const maxRevenue = Math.max(...byEvent.map((e) => Number(e.totalRevenue)));
                  const pct        = maxRevenue > 0
                    ? Math.round((Number(ev.totalRevenue) / maxRevenue) * 100)
                    : 0;

                  return (
                    <tr
                      key={ev.eventId}
                      style={{
                        borderBottom: `1px solid ${palette.textPrimary}08`,
                        backgroundColor: idx % 2 !== 0 ? `${palette.textPrimary}03` : 'transparent',
                      }}
                    >
                      {/* Nombre del evento */}
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3 min-w-0">
                          <div
                            className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 text-xs font-black"
                            style={{
                              background: `${palette.primary}20`,
                              color: palette.primary,
                              border: `1px solid ${palette.primary}30`,
                            }}
                          >
                            {ev.eventName?.charAt(0).toUpperCase() ?? '#'}
                          </div>
                          <div className="min-w-0 flex-1">
                            <p className="text-sm font-semibold truncate max-w-xs">
                              {ev.eventName}
                            </p>
                            {/* Mini barra de progreso */}
                            <div
                              className="mt-1 h-1 rounded-full overflow-hidden"
                              style={{ background: `${palette.textPrimary}10`, maxWidth: '160px' }}
                            >
                              <div
                                className="h-full rounded-full transition-all"
                                style={{
                                  width: `${pct}%`,
                                  background: `linear-gradient(90deg, ${palette.primary}, ${palette.accent})`,
                                }}
                              />
                            </div>
                          </div>
                        </div>
                      </td>

                      {/* Tickets vendidos */}
                      <td className="px-6 py-4 text-right">
                        <span
                          className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold"
                          style={{
                            background: `${palette.primary}15`,
                            color: palette.primary,
                            border: `1px solid ${palette.primary}25`,
                          }}
                        >
                          <Ticket className="w-3 h-3" />
                          {ev.ticketsSold}
                        </span>
                      </td>

                      {/* Recaudado */}
                      <td
                        className="px-6 py-4 text-right text-sm font-bold"
                        style={{ color: palette.success }}
                      >
                        {fmtCOP(ev.totalRevenue)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>

              {/* Totales */}
              <tfoot>
                <tr style={{ borderTop: `2px solid ${palette.textPrimary}15` }}>
                  <td className="px-6 py-4">
                    <span className="text-sm font-bold" style={{ color: palette.textMuted }}>
                      Total
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <span
                      className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold"
                      style={{
                        background: `${palette.primary}15`,
                        color: palette.primary,
                        border: `1px solid ${palette.primary}25`,
                      }}
                    >
                      <Ticket className="w-3 h-3" />
                      {totalTickets}
                    </span>
                  </td>
                  <td
                    className="px-6 py-4 text-right text-sm font-black"
                    style={{ color: palette.success }}
                  >
                    {fmtCOP(totalEarnings)}
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>

        {/* ── Info cards ────────────────────────────────────────────────────── */}
        <div className="grid sm:grid-cols-2 gap-4">
          {[
            {
              icon: ShieldCheck,
              title: '¿Cómo funciona?',
              text:  'Cada pago aprobado se acredita automáticamente en tu cartera. Podrás usarlo para comprar en cualquier evento de la plataforma.',
            },
            {
              icon: TrendingUp,
              title: 'Acreditación instantánea',
              text:  'El monto total del ticket (incluido lo que el comprador pagó con su cartera) se refleja en tu saldo en tiempo real.',
            },
          ].map(({ icon: Icon, title, text }) => (
            <div
              key={title}
              className="rounded-2xl p-5 flex items-start gap-4"
              style={{
                backgroundColor: palette.surface,
                border: `1px solid ${palette.textPrimary}15`,
              }}
            >
              <div
                className="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0"
                style={{
                  background: `${palette.primary}20`,
                  border: `1px solid ${palette.primary}30`,
                }}
              >
                <Icon className="w-5 h-5" style={{ color: palette.primary }} />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-bold mb-1">{title}</p>
                <p className="text-xs leading-relaxed" style={{ color: palette.textSecondary }}>
                  {text}
                </p>
              </div>
            </div>
          ))}
        </div>

      </div>

      <Footer />
    </div>
  );
}
