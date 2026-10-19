/**
 * AdminReports.jsx
 * Página de Reportes del panel de administración.
 * Consume GET /admin/stats y visualiza métricas clave de la plataforma.
 */

import { useState, useEffect } from 'react';
import {
  LayoutDashboard, CreditCard, Calendar, BarChart2, RefreshCcw,
  Users, LogOut, User, Settings, ChevronDown,
  CalendarCheck, AlertCircle, Loader2, Ticket,
  TrendingUp, UserCheck, Eye, XCircle, CheckCircle2,
  UsersRound,
} from 'lucide-react';
import { performLogout, httpRequest } from '../services/httpClient';
import { useNavigate } from 'react-router-dom';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

const fmtCOP = (n) =>
  new Intl.NumberFormat('es-CO', {
    style: 'currency', currency: 'COP', maximumFractionDigits: 0,
  }).format(Number(n) || 0);

const fmtNum = (n) =>
  new Intl.NumberFormat('es-CO').format(Number(n) || 0);

const pct = (part, total) =>
  total > 0 ? ((Number(part) / Number(total)) * 100).toFixed(1) : '0.0';

// ── Tarjeta de métrica ────────────────────────────────────────────────────────
function MetricCard({ icon: Icon, iconBg, iconColor, label, value, sub, loading }) {
  return (
    <div
      className="rounded-xl p-5 border border-white/[0.07] flex flex-col gap-3 transition-all hover:border-white/[0.14]"
      style={{ background: 'rgba(255,255,255,0.04)' }}
    >
      <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${iconBg}`}>
        <Icon className={`w-5 h-5 ${iconColor}`} />
      </div>
      <div>
        <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-textMuted mb-1">{label}</p>
        {loading
          ? <div className="h-7 w-24 rounded-lg bg-surfaceAlt animate-pulse" />
          : <p className="text-2xl font-black text-textPrimary leading-none" style={{ letterSpacing: '-0.02em' }}>{value}</p>
        }
        {sub && !loading && (
          <p className="text-[13px] mt-1.5" style={{ color: 'rgba(255,255,255,0.55)' }}>{sub}</p>
        )}
      </div>
    </div>
  );
}

// ── Fila de desglose de eventos ───────────────────────────────────────────────
function EventRow({ icon: Icon, iconColor, iconBg, label, count, total, loading }) {
  const share = pct(count, total);
  return (
    <div className="flex items-center gap-3 py-3 border-b border-white/[0.05] last:border-0">
      <div className={`w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 ${iconBg}`}>
        <Icon className={`w-3.5 h-3.5 ${iconColor}`} />
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between mb-1.5">
          <span className="text-sm font-medium text-textPrimary">{label}</span>
          {loading
            ? <div className="h-4 w-10 rounded bg-surfaceAlt animate-pulse" />
            : <span className="text-sm font-bold text-textPrimary">{fmtNum(count)}</span>
          }
        </div>
        {/* Barra de progreso */}
        <div className="h-1.5 rounded-full overflow-hidden" style={{ background: 'rgba(255,255,255,0.06)' }}>
          {!loading && (
            <div
              className={`h-full rounded-full transition-all duration-500 ${iconBg.replace('/15', '/60')}`}
              style={{ width: `${share}%` }}
            />
          )}
        </div>
      </div>
      {!loading && (
        <span className="text-[11px] text-textMuted flex-shrink-0 w-10 text-right">{share}%</span>
      )}
    </div>
  );
}

// ── Componente principal ───────────────────────────────────────────────────────
export default function AdminReports() {
  const [stats,        setStats]        = useState(null);
  const [loadingStats, setLoadingStats] = useState(true);
  const [error,        setError]        = useState(null);
  const [showUserMenu, setShowUserMenu] = useState(false);

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

  useEffect(() => {
    const handler = (e) => {
      if (!e.target.closest('.user-menu-container')) setShowUserMenu(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  useEffect(() => {
    (async () => {
      try {
        const res  = await httpRequest(`${API_URL}/admin/stats`, { method: 'GET' });
        const data = await res.json();
        if (res.ok) setStats(data.data);
        else setError(data.message || 'Error al cargar reportes');
      } catch (err) {
        setError('Error de conexión: ' + err.message);
      } finally {
        setLoadingStats(false);
      }
    })();
  }, []);

  const navItems = [
    { icon: LayoutDashboard, label: 'Dashboard',          href: '/admin-dashboard' },
    { icon: CreditCard,      label: 'Control Financiero', href: '/admin-financial' },
    { icon: Calendar,        label: 'Eventos',            href: '/admin-events'    },
    { icon: BarChart2,       label: 'Reportes',           href: '/admin-reports', active: true },
    { icon: RefreshCcw,      label: 'Reembolsos',         href: '#'               },
    { icon: Users,           label: 'Solicitudes',        href: '/admin-panel'    },
  ];

  // ── Métricas de resumen ────────────────────────────────────────────────────
  const summaryCards = [
    {
      icon: TrendingUp, iconBg: 'bg-emerald-500/15', iconColor: 'text-emerald-400',
      label: 'Ingresos totales',
      value: fmtCOP(stats?.totalRevenue ?? 0),
      sub:   `${fmtNum(stats?.approvedPayments)} pagos aprobados`,
    },
    {
      icon: UsersRound, iconBg: 'bg-violet-500/15', iconColor: 'text-violet-400',
      label: 'Usuarios registrados',
      value: fmtNum(stats?.totalUsers),
      sub:   `${fmtNum(stats?.approvedOrganizerRequests)} organizadores`,
    },
    {
      icon: Ticket, iconBg: 'bg-sky-500/15', iconColor: 'text-sky-400',
      label: 'Boletas vendidas',
      value: fmtNum(stats?.totalTickets),
      sub:   `en ${fmtNum(stats?.publishedEvents)} eventos publicados`,
    },
    {
      icon: RefreshCcw, iconBg: 'bg-rose-500/15', iconColor: 'text-rose-400',
      label: 'Total reembolsado',
      value: fmtCOP(stats?.totalRefundedAmount ?? 0),
      sub:   `${fmtNum(stats?.processedRefunds)} reembolsos procesados`,
    },
  ];

  // ── Filas de desglose de eventos ──────────────────────────────────────────
  const eventRows = [
    { icon: CheckCircle2, iconBg: 'bg-emerald-500/15', iconColor: 'text-emerald-400', label: 'Publicados',     count: stats?.publishedEvents     },
    { icon: CalendarCheck,iconBg: 'bg-primary/15',     iconColor: 'text-accent',       label: 'Finalizados',   count: stats?.completedEvents     },
    { icon: Eye,          iconBg: 'bg-yellow-500/15',  iconColor: 'text-yellow-400',   label: 'En revisión',   count: stats?.pendingReviewEvents  },
    { icon: XCircle,      iconBg: 'bg-rose-500/15',    iconColor: 'text-rose-400',     label: 'Rechazados',    count: stats?.rejectedEvents      },
    { icon: AlertCircle,  iconBg: 'bg-orange-500/15',  iconColor: 'text-orange-400',   label: 'Cancelados',    count: stats?.cancelledEvents     },
    { icon: Calendar,     iconBg: 'bg-slate-500/15',   iconColor: 'text-slate-400',    label: 'Borradores',    count: stats?.draftEvents         },
  ];

  return (
    <div className="min-h-screen flex bg-background text-textPrimary font-sans">

      {/* ── Sidebar ─────────────────────────────────────────────────────── */}
      <aside className="w-52 flex-shrink-0 flex flex-col justify-between px-3 py-6 bg-background border-r border-surfaceAlt/30 h-screen sticky top-0">
        <div>
          <div className="flex items-center gap-2 px-3 mb-6">
            <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center flex-shrink-0">
              <span className="text-white font-black text-xs">TE</span>
            </div>
            <span className="text-sm font-bold text-textPrimary">Tu Evento</span>
          </div>
          <nav className="flex flex-col gap-0.5">
            {navItems.map((item, idx) => (
              <a key={idx} href={item.href}
                className={`flex items-center gap-2.5 px-3 py-2.5 rounded-full text-sm font-medium transition-colors
                  ${item.active
                    ? 'bg-primary text-textPrimary'
                    : 'text-textSecondary hover:text-textPrimary hover:bg-surfaceAlt/50'}`}
              >
                <item.icon className="w-4 h-4 flex-shrink-0" />
                {item.label}
              </a>
            ))}
          </nav>
        </div>

        {/* User card */}
        <div className="pt-4 border-t border-surfaceAlt/40 relative user-menu-container flex-shrink-0">
          <button
            onClick={() => setShowUserMenu(!showUserMenu)}
            className="w-full flex items-center gap-3 p-2 rounded-xl hover:bg-surfaceAlt/40 transition-all"
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

      {/* ── Main ────────────────────────────────────────────────────────── */}
      <main className="flex-1 overflow-y-auto bg-background">
        <div className="p-8 max-w-6xl mx-auto w-full">

          {/* Header */}
          <div className="mb-8">
            <h1 className="text-2xl font-black uppercase tracking-wide text-textPrimary mb-1">Reportes</h1>
            <p className="text-textSecondary text-sm">
              Resumen consolidado del rendimiento de la plataforma.
            </p>
          </div>

          {/* Error */}
          {error && (
            <div className="mb-6 px-4 py-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" /> {error}
            </div>
          )}

          {/* ── Tarjetas de resumen ─────────────────────────────────────── */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
            {summaryCards.map((card, i) => (
              <MetricCard key={i} {...card} loading={loadingStats} />
            ))}
          </div>

          {/* ── Dos columnas: desglose eventos + solicitudes ───────────── */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">

            {/* Desglose de eventos */}
            <div className="rounded-2xl border border-white/[0.07] overflow-hidden"
              style={{ background: 'rgba(255,255,255,0.03)' }}>
              <div className="px-6 py-4 border-b border-white/[0.06] flex items-center justify-between">
                <div>
                  <h2 className="text-sm font-bold text-textPrimary">Eventos por estado</h2>
                  <p className="text-xs text-textMuted mt-0.5">
                    Total: {loadingStats ? '—' : fmtNum(stats?.totalEvents)} eventos
                  </p>
                </div>
                {loadingStats && <Loader2 className="w-4 h-4 text-textMuted animate-spin" />}
              </div>
              <div className="px-6 py-2">
                {eventRows.map((row, i) => (
                  <EventRow
                    key={i}
                    {...row}
                    total={stats?.totalEvents ?? 0}
                    loading={loadingStats}
                  />
                ))}
              </div>
            </div>

            {/* Solicitudes y usuarios */}
            <div className="flex flex-col gap-6">
              {/* Solicitudes de organizadores */}
              <div className="rounded-2xl border border-white/[0.07] overflow-hidden"
                style={{ background: 'rgba(255,255,255,0.03)' }}>
                <div className="px-6 py-4 border-b border-white/[0.06]">
                  <h2 className="text-sm font-bold text-textPrimary">Solicitudes de organizadores</h2>
                  <p className="text-xs text-textMuted mt-0.5">Historial de aprobaciones</p>
                </div>
                <div className="px-6 py-4 grid grid-cols-2 gap-4">
                  <div className="rounded-xl p-4 text-center" style={{ background: 'rgba(255,255,255,0.04)' }}>
                    <div className="w-9 h-9 rounded-xl bg-emerald-500/15 flex items-center justify-center mx-auto mb-2">
                      <UserCheck className="w-4 h-4 text-emerald-400" />
                    </div>
                    {loadingStats
                      ? <div className="h-7 w-12 rounded-lg bg-surfaceAlt animate-pulse mx-auto" />
                      : <p className="text-2xl font-black text-textPrimary leading-none">{fmtNum(stats?.approvedOrganizerRequests)}</p>
                    }
                    <p className="text-xs text-textMuted mt-1">Aprobadas</p>
                  </div>
                  <div className="rounded-xl p-4 text-center" style={{ background: 'rgba(255,255,255,0.04)' }}>
                    <div className="w-9 h-9 rounded-xl bg-yellow-500/15 flex items-center justify-center mx-auto mb-2">
                      <Eye className="w-4 h-4 text-yellow-400" />
                    </div>
                    {loadingStats
                      ? <div className="h-7 w-12 rounded-lg bg-surfaceAlt animate-pulse mx-auto" />
                      : <p className="text-2xl font-black text-textPrimary leading-none">{fmtNum(stats?.pendingOrganizerRequests)}</p>
                    }
                    <p className="text-xs text-textMuted mt-1">Pendientes</p>
                  </div>
                </div>
              </div>

              {/* Reembolsos */}
              <div className="rounded-2xl border border-white/[0.07] overflow-hidden"
                style={{ background: 'rgba(255,255,255,0.03)' }}>
                <div className="px-6 py-4 border-b border-white/[0.06]">
                  <h2 className="text-sm font-bold text-textPrimary">Reembolsos</h2>
                  <p className="text-xs text-textMuted mt-0.5">Estado de solicitudes</p>
                </div>
                <div className="px-6 py-4 grid grid-cols-2 gap-4">
                  <div className="rounded-xl p-4 text-center" style={{ background: 'rgba(255,255,255,0.04)' }}>
                    <div className="w-9 h-9 rounded-xl bg-emerald-500/15 flex items-center justify-center mx-auto mb-2">
                      <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                    </div>
                    {loadingStats
                      ? <div className="h-7 w-12 rounded-lg bg-surfaceAlt animate-pulse mx-auto" />
                      : <p className="text-2xl font-black text-textPrimary leading-none">{fmtNum(stats?.processedRefunds)}</p>
                    }
                    <p className="text-xs text-textMuted mt-1">Procesados</p>
                  </div>
                  <div className="rounded-xl p-4 text-center" style={{ background: 'rgba(255,255,255,0.04)' }}>
                    <div className="w-9 h-9 rounded-xl bg-rose-500/15 flex items-center justify-center mx-auto mb-2">
                      <AlertCircle className="w-4 h-4 text-rose-400" />
                    </div>
                    {loadingStats
                      ? <div className="h-7 w-12 rounded-lg bg-surfaceAlt animate-pulse mx-auto" />
                      : <p className="text-2xl font-black text-textPrimary leading-none">{fmtNum(stats?.pendingRefunds)}</p>
                    }
                    <p className="text-xs text-textMuted mt-1">Pendientes</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* ── Tasa de conversión ──────────────────────────────────────── */}
          <div className="rounded-2xl border border-white/[0.07] overflow-hidden"
            style={{ background: 'rgba(255,255,255,0.03)' }}>
            <div className="px-6 py-4 border-b border-white/[0.06]">
              <h2 className="text-sm font-bold text-textPrimary">Indicadores clave</h2>
              <p className="text-xs text-textMuted mt-0.5">Relaciones entre métricas de la plataforma</p>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 divide-y sm:divide-y-0 sm:divide-x divide-white/[0.05]">
              {/* Tasa eventos publicados */}
              <div className="px-6 py-5 text-center">
                <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-textMuted mb-2">Tasa de publicación</p>
                {loadingStats
                  ? <div className="h-10 w-16 rounded-xl bg-surfaceAlt animate-pulse mx-auto" />
                  : <p className="text-4xl font-black text-accent leading-none">
                      {pct(stats?.publishedEvents, stats?.totalEvents)}%
                    </p>
                }
                <p className="text-xs text-textMuted mt-1.5">eventos publicados vs. totales</p>
              </div>
              {/* Tasa de aprobación de organizadores */}
              <div className="px-6 py-5 text-center">
                <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-textMuted mb-2">Aprobación organizadores</p>
                {loadingStats
                  ? <div className="h-10 w-16 rounded-xl bg-surfaceAlt animate-pulse mx-auto" />
                  : <p className="text-4xl font-black text-emerald-400 leading-none">
                      {pct(
                        stats?.approvedOrganizerRequests,
                        (stats?.approvedOrganizerRequests ?? 0) + (stats?.pendingOrganizerRequests ?? 0)
                      )}%
                    </p>
                }
                <p className="text-xs text-textMuted mt-1.5">aprobados vs. total solicitudes</p>
              </div>
              {/* Tasa de reembolso */}
              <div className="px-6 py-5 text-center">
                <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-textMuted mb-2">Tasa de reembolso</p>
                {loadingStats
                  ? <div className="h-10 w-16 rounded-xl bg-surfaceAlt animate-pulse mx-auto" />
                  : <p className="text-4xl font-black text-rose-400 leading-none">
                      {pct(
                        (stats?.processedRefunds ?? 0) + (stats?.pendingRefunds ?? 0),
                        stats?.approvedPayments
                      )}%
                    </p>
                }
                <p className="text-xs text-textMuted mt-1.5">reembolsos vs. pagos aprobados</p>
              </div>
            </div>
          </div>

        </div>
      </main>
    </div>
  );
}
