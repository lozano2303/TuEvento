/**
 * AdminFinancial.jsx
 * Página de Control Financiero y Reembolsos del panel de administración.
 */

import { useState, useEffect } from 'react';
import {
  TrendingUp, AlertCircle, Loader2, Ticket, ArrowLeftRight,
} from 'lucide-react';
import { httpRequest } from '../services/httpClient';
import AdminSidebar from '../components/admin/AdminSidebar';

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
          <p className="text-[13px] mt-1.5" style={{ color: 'rgba(255,255,255,0.60)' }}>{sub}</p>
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
      <AdminSidebar activePage="financial" />

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
