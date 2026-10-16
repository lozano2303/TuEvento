import { useState, useEffect } from 'react';
import {
  LayoutDashboard, CreditCard, Calendar, BarChart2, RefreshCcw,
  Users, LogOut, User, Settings, ChevronDown,
  UsersRound, CalendarCheck, Zap, Eye, Ticket,
  UserCheck, TrendingUp, AlertCircle, Loader2,
} from 'lucide-react';
import { performLogout, httpRequest } from '../services/httpClient';
import { useNavigate } from 'react-router-dom';
import { formatDistanceToNow } from 'date-fns';
import { es } from 'date-fns/locale';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

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

// ── Activity item ──────────────────────────────────────────────────────────────
function ActivityItem({ icon: Icon, iconBg, iconColor, title, desc, time }) {
  return (
    <div className="flex items-start gap-3 py-3 border-b border-white/[0.05] last:border-0">
      <div className={`w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 mt-0.5 ${iconBg}`}>
        <Icon className={`w-3.5 h-3.5 ${iconColor}`} />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-textPrimary leading-snug">{title}</p>
        {desc && <p className="text-xs text-textMuted mt-0.5 leading-snug truncate">{desc}</p>}
      </div>
      <span className="text-[10px] text-textMuted flex-shrink-0 mt-1">{time}</span>
    </div>
  );
}

// ── Componente principal ───────────────────────────────────────────────────────
export default function AdminDashboard() {
  const [stats,         setStats]         = useState(null);
  const [recentEvents,  setRecentEvents]  = useState([]);
  const [loadingStats,  setLoadingStats]  = useState(true);
  const [loadingEvents, setLoadingEvents] = useState(true);
  const [error,         setError]         = useState(null);
  const [showUserMenu,  setShowUserMenu]  = useState(false);

  const navigate = useNavigate();

  const userData = {
    name:  localStorage.getItem('name') || localStorage.getItem('fullName') || 'Admin',
    email: localStorage.getItem('userEmail') || '',
    role:  localStorage.getItem('role') === 'ADMIN' ? 'Super Admin' : 'Admin',
  };

  const handleLogout = async () => {
    await performLogout();
    window.location.href = '/login';
  };

  const handleMenuClick = (action) => {
    setShowUserMenu(false);
    if (action === 'profile')  navigate('/profile');
    if (action === 'settings') navigate('/settings');
    if (action === 'logout')   handleLogout();
  };

  // Cerrar menú al click fuera
  useEffect(() => {
    const handler = (e) => {
      if (!e.target.closest('.user-menu-container')) setShowUserMenu(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  // Cargar stats
  useEffect(() => {
    (async () => {
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

  // Cargar eventos recientes (todos, sin filtro — tomamos los últimos por ID desc)
  useEffect(() => {
    (async () => {
      try {
        const res  = await httpRequest(`${API_URL}/admin/events`, { method: 'GET' });
        const data = await res.json();
        if (res.ok) {
          const sorted = [...(data.data || [])].sort((a, b) => b.eventId - a.eventId);
          setRecentEvents(sorted.slice(0, 7));
        }
      } catch {
        // actividad reciente no crítica — silenciar
      } finally {
        setLoadingEvents(false);
      }
    })();
  }, []);

  const navItems = [
    { icon: LayoutDashboard, label: 'Dashboard',          href: '/admin-dashboard', active: true },
    { icon: CreditCard,      label: 'Control Financiero', href: '#'             },
    { icon: Calendar,        label: 'Eventos',            href: '/admin-events' },
    { icon: BarChart2,       label: 'Reportes',           href: '#'             },
    { icon: RefreshCcw,      label: 'Reembolsos',         href: '#'             },
    { icon: Users,           label: 'Solicitudes',        href: '/admin-panel'  },
  ];

  // ── Derivar actividad reciente de los eventos ─────────────────────────────
  const activityFromEvents = recentEvents.map((ev) => {
    const statusMap = {
      PENDING_REVIEW: {
        icon: Eye,
        iconBg: 'bg-yellow-500/15',
        iconColor: 'text-yellow-400',
        title: 'Evento enviado a revisión',
        desc: `"${ev.eventName}" requiere aprobación administrativa.`,
      },
      PUBLISHED: {
        icon: CalendarCheck,
        iconBg: 'bg-emerald-500/15',
        iconColor: 'text-emerald-400',
        title: 'Evento aprobado y publicado',
        desc: `"${ev.eventName}" ya está visible para el público.`,
      },
      REJECTED: {
        icon: AlertCircle,
        iconBg: 'bg-rose-500/15',
        iconColor: 'text-rose-400',
        title: 'Evento rechazado',
        desc: `"${ev.eventName}" fue rechazado por el administrador.`,
      },
      CANCELLED: {
        icon: AlertCircle,
        iconBg: 'bg-rose-500/15',
        iconColor: 'text-rose-400',
        title: 'Evento cancelado',
        desc: `"${ev.eventName}" fue cancelado.`,
      },
      COMPLETED: {
        icon: CalendarCheck,
        iconBg: 'bg-primary/15',
        iconColor: 'text-accent',
        title: 'Evento finalizado',
        desc: `"${ev.eventName}" concluyó exitosamente.`,
      },
      DRAFT: {
        icon: Calendar,
        iconBg: 'bg-slate-500/15',
        iconColor: 'text-slate-400',
        title: 'Evento creado como borrador',
        desc: `"${ev.eventName}" fue creado por ${ev.organizerName || 'un organizador'}.`,
      },
    };
    const meta = statusMap[ev.status] || statusMap.DRAFT;
    return { ...meta, eventId: ev.eventId };
  });

  // ── Cards de stats ─────────────────────────────────────────────────────────
  const statCards = [
    {
      icon:       UsersRound,
      iconBg:     'bg-violet-500/15',
      iconColor:  'text-violet-400',
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

      {/* ── Sidebar ────────────────────────────────────────────────────────── */}
      <aside className="w-52 flex-shrink-0 flex flex-col justify-between px-3 py-6 bg-background border-r border-surfaceAlt/30 h-screen sticky top-0">
        <nav className="flex flex-col gap-0.5">
          {navItems.map((item, idx) => (
            <a
              key={idx}
              href={item.href}
              className={`flex items-center gap-2.5 px-3 py-2.5 rounded-full text-sm font-medium transition-colors
                ${item.active
                  ? 'bg-primary text-textPrimary'
                  : 'text-textSecondary hover:text-textPrimary hover:bg-surfaceAlt/50'
                }`}
            >
              <item.icon className="w-4 h-4 flex-shrink-0" />
              {item.label}
            </a>
          ))}
        </nav>

        {/* User card */}
        <div className="pt-4 border-t border-surfaceAlt/40 relative user-menu-container flex-shrink-0">
          <button
            onClick={() => setShowUserMenu(!showUserMenu)}
            className="w-full flex items-center gap-3 p-2 rounded-xl hover:bg-surfaceAlt/40 transition-all group"
          >
            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary to-primaryDark flex items-center justify-center text-textPrimary font-bold text-sm flex-shrink-0">
              {userData.name.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 text-left min-w-0">
              <p className="text-sm font-semibold text-textPrimary truncate">{userData.name}</p>
              <p className="text-xs text-textMuted truncate">{userData.role}</p>
            </div>
            <ChevronDown className={`w-4 h-4 text-textMuted transition-transform ${showUserMenu ? 'rotate-180' : ''}`} />
          </button>

          {showUserMenu && (
            <div className="absolute bottom-full left-0 right-0 mb-2 mx-2 bg-surface border border-surfaceAlt rounded-xl overflow-hidden shadow-xl z-50">
              <button onClick={() => handleMenuClick('profile')}
                className="w-full flex items-center gap-3 px-4 py-3 text-sm text-textSecondary hover:bg-surfaceAlt/40 hover:text-textPrimary transition-colors">
                <User className="w-4 h-4" /> Perfil
              </button>
              <button onClick={() => handleMenuClick('settings')}
                className="w-full flex items-center gap-3 px-4 py-3 text-sm text-textSecondary hover:bg-surfaceAlt/40 hover:text-textPrimary transition-colors">
                <Settings className="w-4 h-4" /> Configuración
              </button>
              <div className="border-t border-surfaceAlt/40">
                <button onClick={() => handleMenuClick('logout')}
                  className="w-full flex items-center gap-3 px-4 py-3 text-sm text-red-400 hover:bg-red-500/10 transition-colors">
                  <LogOut className="w-4 h-4" /> Cerrar sesión
                </button>
              </div>
            </div>
          )}
        </div>
      </aside>

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
          <div
            className="rounded-2xl p-6 mb-8 border border-primary/20"
            style={{ background: 'linear-gradient(135deg, rgba(124,58,237,0.12) 0%, rgba(167,139,250,0.06) 100%)' }}
          >
            <p className="text-[10px] font-black uppercase tracking-[0.18em] text-accent mb-2">
              Control Financiero
            </p>
            <p className="text-sm text-textMuted mb-1">Total de ingresos generados</p>
            {loadingStats
              ? <div className="h-10 w-56 rounded-xl bg-surfaceAlt/60 animate-pulse" />
              : (
                <div className="flex items-end gap-3">
                  <p
                    className="text-4xl font-black text-textPrimary leading-none"
                    style={{ letterSpacing: '-0.02em' }}
                  >
                    {fmtCOP(stats?.totalRevenue ?? 0)}
                  </p>
                  <span className="text-sm text-textMuted mb-1">COP</span>
                </div>
              )
            }
          </div>

          {/* ── Actividad reciente ────────────────────────────────────────── */}
          <div className="rounded-2xl border border-white/[0.07] overflow-hidden"
            style={{ background: 'rgba(255,255,255,0.03)' }}>

            <div className="px-6 py-4 border-b border-white/[0.06] flex items-center justify-between">
              <h2 className="text-sm font-bold text-textPrimary">Actividad reciente</h2>
              <a
                href="/admin-events"
                className="text-xs font-semibold text-accent hover:text-textPrimary transition-colors"
              >
                Ver todo
              </a>
            </div>

            <div className="px-6 py-2">
              {loadingEvents
                ? (
                  <div className="py-6 flex items-center justify-center gap-2 text-textMuted text-sm">
                    <Loader2 className="w-4 h-4 animate-spin" /> Cargando actividad…
                  </div>
                )
                : activityFromEvents.length === 0
                  ? (
                    <p className="py-8 text-center text-sm text-textMuted">
                      No hay actividad reciente.
                    </p>
                  )
                  : activityFromEvents.map((item) => (
                    <ActivityItem key={item.eventId} {...item} time="reciente" />
                  ))
              }
            </div>
          </div>

        </div>
      </main>
    </div>
  );
}
