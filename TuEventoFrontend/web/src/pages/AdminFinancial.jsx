/**
 * AdminFinancial.jsx
 * Página de Control Financiero y Reembolsos del panel de administración.
 */

import { useState, useEffect } from 'react';
import {
  LayoutDashboard, CreditCard, Calendar, BarChart2, RefreshCcw,
  Users, LogOut, User, Settings, ChevronDown,
  TrendingUp, AlertCircle, Loader2, Ticket, ArrowLeftRight,
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

// ── Tarjeta de stat financiero ────────────────────────────────────────────────
function FinCard({ icon: Icon, iconBg, iconColor, label, value, sub, loading, accent }) {
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
          ? <div className="h-8 w-28 rounded-lg bg-surfaceAlt animate-pulse" />
          : <p className={`text-2xl font-black leading-none ${accent ? '' : 'text-textPrimary'}`}
              style={accent ? { color: 'var(--color-accent)', letterSpacing: '-0.02em' } : { letterSpacing: '-0.02em' }}>
              {value}
            </p>
        }
        {sub && !loading && (
          <p className="text-xs text-textMuted mt-1.5">{sub}</p>
        )}
      </div>
    </div>
  );
}

// ── Componente principal ───────────────────────────────────────────────────────
export default function AdminFinancial() {
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
        else setError(data.message || 'Error al cargar estadísticas');
      } catch (err) {
        setError('Error de conexión: ' + err.message);
      } finally {
        setLoadingStats(false);
      }
    })();
  }, []);

  const navItems = [
    { icon: LayoutDashboard, label: 'Dashboard',          href: '/admin-dashboard' },
    { icon: CreditCard,      label: 'Control Financiero', href: '/admin-financial', active: true },
    { icon: Calendar,        label: 'Eventos',            href: '/admin-events'   },
    { icon: BarChart2,       label: 'Reportes',           href: '#'               },
    { icon: RefreshCcw,      label: 'Reembolsos',         href: '#'               },
    { icon: Users,           label: 'Solicitudes',        href: '/admin-panel'    },
  ];

  const finCards = [
    {
      icon:      TrendingUp,
      iconBg:    'bg-emerald-500/15',
      iconColor: 'text-emerald-400',
      label:     'Ingresos totales',
      value:     fmtCOP(stats?.totalRevenue ?? 0),
      sub:       'Suma de todos los pagos aprobados',
    },
    {
      icon:      Ticket,
      iconBg:    'bg-sky-500/15',
      iconColor: 'text-sky-400',
      label:     'Ventas activas',
      value:     fmtNum(stats?.approvedPayments),
      sub:       'Boletas vendidas en eventos activos',
    },
    {
      icon:      AlertCircle,
      iconBg:    'bg-rose-500/15',
      iconColor: 'text-rose-400',
      label:     'Reembolsos pendientes',
      value:     fmtNum(stats?.pendingRefunds),
      sub:       'Solicitudes esperando validación',
    },
    {
      icon:      ArrowLeftRight,
      iconBg:    'bg-violet-500/15',
      iconColor: 'text-violet-400',
      label:     'Saldo compensatorio total',
      value:     fmtCOP(stats?.totalRevenue ?? 0),
      sub:       'Generado por eventos cancelados',
      accent:    true,
    },
  ];

  return (
    <div className="min-h-screen flex bg-background text-textPrimary font-sans">

      {/* ── Sidebar ────────────────────────────────────────────────────────── */}
      <aside className="w-52 flex-shrink-0 flex flex-col justify-between px-3 py-6 bg-background border-r border-surfaceAlt/30 h-screen sticky top-0">
        {/* Logo */}
        <div>
          <div className="flex items-center gap-2 px-3 mb-6">
            <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center flex-shrink-0">
              <span className="text-white font-black text-xs">TE</span>
            </div>
            <span className="text-sm font-bold text-textPrimary">Tu Evento</span>
          </div>

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

      {/* ── Main ───────────────────────────────────────────────────────────── */}
      <main className="flex-1 overflow-y-auto bg-background">
        <div className="p-8 max-w-6xl mx-auto w-full">

          {/* Header */}
          <div className="mb-8">
            <h1 className="text-2xl font-black uppercase tracking-wide text-textPrimary mb-1">
              Control Financiero y Reembolsos
            </h1>
            <p className="text-textSecondary text-sm">
              Monitoreo directo de transacciones y estados de eventos del sistema en tiempo real.
            </p>
          </div>

          {/* Error */}
          {error && (
            <div className="mb-6 px-4 py-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              {error}
            </div>
          )}

          {/* ── Tarjetas financieras ──────────────────────────────────────── */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
            {finCards.map((card, i) => (
              <FinCard key={i} {...card} loading={loadingStats} />
            ))}
          </div>

          {/* ── Área de monitoreo en tiempo real ─────────────────────────── */}
          <div
            className="rounded-2xl border border-white/[0.07] overflow-hidden"
            style={{ background: 'rgba(255,255,255,0.02)' }}
          >
            {/* Header del panel */}
            <div className="px-6 py-4 border-b border-white/[0.06] flex items-center justify-between">
              <div>
                <h2 className="text-sm font-bold text-textPrimary">Monitoreo en tiempo real</h2>
                <p className="text-xs text-textMuted mt-0.5">Transacciones y actividad financiera reciente</p>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                <span className="text-xs text-emerald-400 font-medium">En vivo</span>
              </div>
            </div>

            {/* Contenido del gráfico — placeholder */}
            <div className="flex flex-col items-center justify-center py-20 gap-4">
              <div className="w-14 h-14 rounded-2xl bg-primary/15 flex items-center justify-center">
                <TrendingUp className="w-7 h-7 text-accent" />
              </div>
              <div className="text-center">
                <p className="text-sm font-semibold text-textPrimary">
                  Vista de monitoreo en tiempo real habilitada
                </p>
                <p className="text-xs text-textMuted mt-1">
                  No hay transacciones recientes para mostrar en el gráfico principal.
                </p>
              </div>
            </div>
          </div>

        </div>
      </main>
    </div>
  );
}
