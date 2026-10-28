import { useState, useEffect } from 'react';
import {
  UsersRound, CalendarCheck, Zap, Eye, Ticket,
  UserCheck, AlertCircle, Loader2,
  UserPlus, TrendingUp, CreditCard, Calendar
} from 'lucide-react';
import { httpRequest } from '../services/httpClient';
import { formatDistanceToNow } from 'date-fns';
import { es } from 'date-fns/locale';
import ActivityHistoryModal from '../components/admin/ActivityHistoryModal';
import AdminSidebar from '../components/admin/AdminSidebar';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8090/api/v1';

// ── Formateador COP ────────────────────────────────────────────────────────────
const fmtCOP = (n) =>
  new Intl.NumberFormat('es-CO', {
    style: 'currency', currency: 'COP', maximumFractionDigits: 0,
  }).format(Number(n) || 0);

const fmtNum = (n) =>
  new Intl.NumberFormat('es-CO').format(Number(n) || 0);

// ── Tarjeta de stat ────────────────────────────────────────────────────────────
function StatCard({ icon: Icon, iconBg, iconColor, label, value, sub, loading }) {
  return (
    <div
      className="flex items-start gap-4 rounded-2xl p-5 border border-white/[0.07] transition-all hover:border-white/[0.14]"
      style={{ background: 'rgba(255,255,255,0.04)' }}
    >
      <div className={`w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0 ${iconBg}`}>
        <Icon className={`w-5 h-5 ${iconColor}`} />
      </div>
      <div className="min-w-0">
        <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-textMuted mb-1">{label}</p>
        {loading
          ? <div className="h-7 w-20 rounded-lg bg-surfaceAlt animate-pulse" />
          : <p className="text-2xl font-black text-textPrimary leading-none">{value}</p>
        }
        {sub && !loading && (
          <p className="text-xs text-textMuted mt-1">{sub}</p>
        )}
      </div>
    </div>
  );
}

// ── Traducciones de títulos que vienen en inglés del backend ─────────────────
const ACTIVITY_TITLE_MAP = {
  'event approved and published':  'Evento aprobado y publicado',
  'event approved':                'Evento aprobado',
  'event published':               'Evento publicado',
  'event rejected':                'Evento rechazado',
  'event cancelled':               'Evento cancelado',
  'event completed':               'Evento finalizado',
  'event created as draft':        'Evento creado como borrador',
  'event sent to review':          'Evento enviado a revisión',
  'event pending review':          'Evento en revisión',
  'event updated':                 'Evento actualizado',
  'new user registered':           'Nuevo usuario registrado',
  'user registered':               'Usuario registrado',
  'payment approved':              'Pago aprobado',
  'payment rejected':              'Pago rechazado',
  'payment refunded':              'Reembolso procesado',
  'refund processed':              'Reembolso procesado',
  'event status changed':          'Estado del evento actualizado',
};

function translateActivityTitle(raw) {
  if (!raw) return raw;
  return ACTIVITY_TITLE_MAP[raw.trim().toLowerCase()] ?? raw;
}

// ── Resolver ícono/colores según tipo de actividad ────────────────────────────
function resolveActivityStyle(item) {
  const { type, title = '' } = item;
  if (type === 'USER_REGISTERED') {
    return { Icon: UserPlus, iconBg: 'bg-violet-500/15', iconColor: 'text-accent' };
  }
  if (type === 'PAYMENT') {
    return { Icon: CreditCard, iconBg: 'bg-amber-500/15', iconColor: 'text-amber-400' };
  }
  const t = title.toLowerCase();
  if (t.includes('approved') || t.includes('published') || t.includes('aprobado') || t.includes('publicado')) {
    return { Icon: CalendarCheck, iconBg: 'bg-emerald-500/15', iconColor: 'text-emerald-400' };
  }
  if (t.includes('rejected') || t.includes('cancelled') || t.includes('rechazado') || t.includes('cancelado')) {
    return { Icon: AlertCircle, iconBg: 'bg-rose-500/15', iconColor: 'text-rose-400' };
  }
  if (t.includes('review') || t.includes('revisión')) {
    return { Icon: Eye, iconBg: 'bg-yellow-500/15', iconColor: 'text-yellow-400' };
  }
  if (t.includes('completed') || t.includes('finalizado')) {
    return { Icon: CalendarCheck, iconBg: 'bg-primary/15', iconColor: 'text-accent' };
  }
  return { Icon: Calendar, iconBg: 'bg-slate-500/15', iconColor: 'text-textSecondary' };
}

function fmtRelTime(iso) {
  if (!iso) return '';
  try {
    return formatDistanceToNow(new Date(iso), { addSuffix: true, locale: es });
  } catch {
    return '';
  }
}

// ── Activity item (inline widget) ─────────────────────────────────────────────
function ActivityItem({ item }) {
  const { Icon, iconBg, iconColor } = resolveActivityStyle(item);
  return (
    <div className="flex items-center gap-3 py-3.5 border-b border-white/[0.05] last:border-0">
      <div className={`w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 ${iconBg}`}>
        <Icon className={`w-3.5 h-3.5 ${iconColor}`} />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-textPrimary leading-snug">{translateActivityTitle(item.title)}</p>
        {item.actor && (
          <p className="text-xs text-textMuted mt-0.5 leading-snug truncate flex items-center gap-1">
            <svg className="w-3 h-3 opacity-60 flex-shrink-0" viewBox="0 0 12 12" fill="currentColor">
              <circle cx="6" cy="3.5" r="2"/>
              <path d="M1 11c0-2.76 2.24-5 5-5s5 2.24 5 5"/>
            </svg>
            {item.actor}
          </p>
        )}
        {!item.actor && item.description && (
          <p className="text-xs text-textMuted mt-0.5 leading-snug truncate">{item.description}</p>
        )}
      </div>
      <span className="text-[13px] flex-shrink-0 whitespace-nowrap" style={{ color: 'rgba(255,255,255,0.55)' }}>
        {fmtRelTime(item.occurredAt)}
      </span>
    </div>
  );
}

// ── Componente principal ───────────────────────────────────────────────────────
export default function AdminDashboard() {
  const [stats,         setStats]         = useState(null);
  const [recentActivity, setRecentActivity] = useState([]);
  const [loadingStats,  setLoadingStats]  = useState(true);
  const [loadingActivity, setLoadingActivity] = useState(true);
  const [error,         setError]         = useState(null);
  const [showActivity,  setShowActivity]  = useState(false);

  // Cargar stats
  useEffect(() => {
    void (async () => {
      try {
        const res  = await httpRequest(`${API_URL}/admin/stats`, { method: 'GET' });
        const data = await res.json();
        if (res.ok) setStats(data.data);
        else setError(data.message || 'Error al cargar estadísticas');
      } catch (err) {
        setError('Error de conexión: ' + err.message);
      } finally {
        setLoadingStats(false);
      }
    })();
  }, []);

  // Cargar actividad reciente desde el endpoint dedicado
  useEffect(() => {
    void (async () => {
      try {
        const res  = await httpRequest(`${API_URL}/admin/activity?limit=5`, { method: 'GET' });
        const data = await res.json();
        if (res.ok) setRecentActivity(data.data || []);
      } catch {
        // actividad reciente no crítica — silenciar
      } finally {
        setLoadingActivity(false);
      }
    })();
  }, []);

  // ── Cards de stats ─────────────────────────────────────────────────────────
  const statCards = [
    {
      icon:       UsersRound,
      iconBg:     'bg-violet-500/15',
      iconColor:  'text-accent',
      label:      'Usuarios registrados',
      value:      fmtNum(stats?.totalUsers),
      sub:        `${fmtNum(stats?.approvedOrganizerRequests)} organizadores aprobados`,
    },
    {
      icon:       Calendar,
      iconBg:     'bg-sky-500/15',
      iconColor:  'text-sky-400',
      label:      'Total de eventos',
      value:      fmtNum(stats?.totalEvents),
      sub:        `${fmtNum(stats?.draftEvents)} borradores`,
    },
    {
      icon:       Zap,
      iconBg:     'bg-emerald-500/15',
      iconColor:  'text-emerald-400',
      label:      'Eventos activos',
      value:      fmtNum(stats?.publishedEvents),
      sub:        `${fmtNum(stats?.completedEvents)} finalizados`,
    },
    {
      icon:       Eye,
      iconBg:     'bg-yellow-500/15',
      iconColor:  'text-yellow-400',
      label:      'Eventos en revisión',
      value:      fmtNum(stats?.pendingReviewEvents),
      sub:        `${fmtNum(stats?.rejectedEvents)} rechazados`,
    },
    {
      icon:       Ticket,
      iconBg:     'bg-pink-500/15',
      iconColor:  'text-pink-400',
      label:      'Boletas vendidas',
      value:      fmtNum(stats?.totalTickets),
      sub:        `${fmtNum(stats?.approvedPayments)} pagos aprobados`,
    },
    {
      icon:       UserCheck,
      iconBg:     'bg-amber-500/15',
      iconColor:  'text-amber-400',
      label:      'Solicitudes pendientes',
      value:      fmtNum(stats?.pendingOrganizerRequests),
      sub:        'organizadores esperando aprobación',
    },
  ];

  return (
    <div className="min-h-screen flex bg-background text-textPrimary font-sans">
      <AdminSidebar activePage="dashboard" />

      {/* ── Main ───────────────────────────────────────────────────────────── */}
      <main className="flex-1 overflow-y-auto bg-background">
        <div className="p-8 max-w-6xl mx-auto w-full">

          {/* Header */}
          <div className="mb-8">
            <h1 className="text-2xl font-black uppercase tracking-wide text-textPrimary mb-1">
              Dashboard
            </h1>
            <p className="text-textSecondary text-sm">
              Resumen general del estado de la plataforma.
            </p>
          </div>

          {/* Error */}
          {error && (
            <div className="mb-6 px-4 py-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              {error}
            </div>
          )}

          {/* ── Grid de stats (2 filas × 3 cols) ────────────────────────── */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
            {statCards.map((card, i) => (
              <StatCard key={i} {...card} loading={loadingStats} />
            ))}
          </div>

          {/* ── Control Financiero ────────────────────────────────────────── */}
          <div className="rounded-2xl mb-8 overflow-hidden border border-primary/20"
            style={{ background: 'rgba(255,255,255,0.03)' }}>

            {/* Header */}
            <div className="px-6 pt-6 pb-4 border-b border-white/[0.06]">
              <h2 className="text-lg font-black text-textPrimary">Control Financiero y Reembolsos</h2>
              <p className="text-xs text-textMuted mt-0.5">Monitoreo de transacciones y estados de eventos en tiempo real.</p>
            </div>

            {/* Tarjetas de stats financieros */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 p-6 pb-4">

              {/* Ingresos totales */}
              <div className="rounded-xl p-4 border border-white/[0.06] flex flex-col gap-2"
                style={{ background: 'rgba(255,255,255,0.04)' }}>
                <div className="w-9 h-9 rounded-lg bg-emerald-500/15 flex items-center justify-center">
                  <TrendingUp className="w-4 h-4 text-emerald-400" />
                </div>
                <p className="text-[11px] font-bold uppercase tracking-[0.1em] text-textMuted">Ingresos totales</p>
                {loadingStats
                  ? <div className="h-7 w-24 rounded-lg bg-surfaceAlt animate-pulse" />
                  : <p className="text-xl font-black text-textPrimary leading-none" style={{ letterSpacing: '-0.02em' }}>
                      {fmtCOP(stats?.totalRevenue ?? 0)}
                    </p>
                }
                <p className="text-[11px] text-textMuted">Pagos aprobados</p>
              </div>

              {/* Ventas activas (tickets) */}
              <div className="rounded-xl p-4 border border-white/[0.06] flex flex-col gap-2"
                style={{ background: 'rgba(255,255,255,0.04)' }}>
                <div className="w-9 h-9 rounded-lg bg-sky-500/15 flex items-center justify-center">
                  <Ticket className="w-4 h-4 text-sky-400" />
                </div>
                <p className="text-[11px] font-bold uppercase tracking-[0.1em] text-textMuted">Ventas activas</p>
                {loadingStats
                  ? <div className="h-7 w-16 rounded-lg bg-surfaceAlt animate-pulse" />
                  : <p className="text-3xl font-black text-textPrimary leading-none">{fmtNum(stats?.approvedPayments)}</p>
                }
                <p className="text-[11px] text-textMuted">Boletas vendidas en eventos activos</p>
              </div>

              {/* Reembolsos pendientes */}
              <div className="rounded-xl p-4 border border-white/[0.06] flex flex-col gap-2"
                style={{ background: 'rgba(255,255,255,0.04)' }}>
                <div className="w-9 h-9 rounded-lg bg-rose-500/15 flex items-center justify-center">
                  <AlertCircle className="w-4 h-4 text-rose-400" />
                </div>
                <p className="text-[11px] font-bold uppercase tracking-[0.1em] text-textMuted">Reembolsos pendientes</p>
                {loadingStats
                  ? <div className="h-7 w-10 rounded-lg bg-surfaceAlt animate-pulse" />
                  : <p className="text-3xl font-black text-textPrimary leading-none">{fmtNum(stats?.pendingRefunds)}</p>
                }
                <p className="text-[11px] text-textMuted">Solicitudes esperando validación</p>
              </div>

              {/* Eventos en revisión */}
              <div className="rounded-xl p-4 border border-white/[0.06] flex flex-col gap-2"
                style={{ background: 'rgba(255,255,255,0.04)' }}>
                <div className="w-9 h-9 rounded-lg bg-violet-500/15 flex items-center justify-center">
                  <CreditCard className="w-4 h-4 text-accent" />
                </div>
                <p className="text-[11px] font-bold uppercase tracking-[0.1em] text-textMuted">Saldo compensatorio total</p>
                {loadingStats
                  ? <div className="h-7 w-24 rounded-lg bg-surfaceAlt animate-pulse" />
                  : <p className="text-xl font-black leading-none" style={{ letterSpacing: '-0.02em', color: 'var(--color-accent)' }}>
                      {fmtCOP(stats?.totalRevenue ?? 0)}
                    </p>
                }
                <p className="text-[11px] text-textMuted">Generado por eventos cancelados</p>
              </div>
            </div>

            {/* Área de monitoreo en tiempo real */}
            <div className="mx-6 mb-6 rounded-xl border border-white/[0.05] flex flex-col items-center justify-center py-10 gap-3"
              style={{ background: 'rgba(255,255,255,0.02)' }}>
              <div className="w-10 h-10 rounded-full bg-primary/15 flex items-center justify-center">
                <TrendingUp className="w-5 h-5 text-accent" />
              </div>
              <p className="text-sm font-semibold text-textPrimary">Vista de monitoreo en tiempo real habilitada</p>
              <p className="text-xs text-textMuted">No hay transacciones recientes para mostrar en el gráfico principal.</p>
            </div>
          </div>

          {/* ── Actividad reciente ────────────────────────────────────────── */}
          <div className="rounded-2xl border border-white/[0.07] overflow-hidden"
            style={{ background: 'rgba(255,255,255,0.03)' }}>

            <div className="px-6 py-4 border-b border-white/[0.06] flex items-center justify-between">
              <h2 className="text-sm font-bold text-textPrimary">Actividad reciente</h2>
              <button
                onClick={() => setShowActivity(true)}
                className="text-xs font-semibold text-accent hover:text-textPrimary transition-colors"
              >
                Ver todo
              </button>
            </div>

            <div className="px-6 py-2">
              {loadingActivity
                ? (
                  <div className="py-6 flex items-center justify-center gap-2 text-textMuted text-sm">
                    <Loader2 className="w-4 h-4 animate-spin" /> Cargando actividad…
                  </div>
                )
                : recentActivity.length === 0
                  ? (
                    <p className="py-8 text-center text-sm text-textMuted">
                      No hay actividad reciente.
                    </p>
                  )
                  : recentActivity.map((item, i) => (
                    <ActivityItem key={`${item.type}-${i}`} item={item} />
                  ))
              }
            </div>
          </div>

        </div>
      </main>

      {/* ── Activity history modal ──────────────────────────────────────── */}
      <ActivityHistoryModal
        isOpen={showActivity}
        onClose={() => setShowActivity(false)}
      />
    </div>
  );
}
