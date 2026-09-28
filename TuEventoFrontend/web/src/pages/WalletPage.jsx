import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Wallet, ShoppingCart, RefreshCcw, ShieldCheck, HelpCircle,
  ChevronRight, CheckCircle, Zap, XCircle, Loader2,
} from 'lucide-react';

import Footer from '../layouts/Footer';
import { getMyWallet, getMyWalletTransactions } from '../services/WalletService';
import { useTheme } from '../context/ThemeContext';

// ── Helpers ────────────────────────────────────────────────────────────────────
const fmtCOP = (n) =>
  new Intl.NumberFormat('es-CO', {
    style: 'currency', currency: 'COP', maximumFractionDigits: 0,
  }).format(Math.abs(n));

const fmtDate = (isoStr) => {
  if (!isoStr) return '';
  return new Date(isoStr).toLocaleDateString('es-CO', {
    day: '2-digit', month: 'short', year: 'numeric',
  });
};

// ── Mapeo de WalletTransactionType a iconType y texto ──────────────────────────
const mapTransactionType = (type, isAdditive) => {
  switch (type) {
    case 'CREDIT':
    case 'REVERSAL':
      return {
        iconType: 'credit',
        concept: type === 'REVERSAL' ? 'Reversión' : 'Crédito por cancelación',
      };
    case 'PAYMENT':
      return {
        iconType: 'purchase',
        concept: 'Aplicación en compra',
      };
    case 'ADJUSTMENT':
      return {
        iconType: isAdditive ? 'credit' : 'purchase',
        concept: isAdditive ? 'Ajuste administrativo' : 'Ajuste administrativo',
      };
    default:
      return { iconType: 'credit', concept: 'Movimiento' };
  }
};

// ── Mapeo de WalletTransactionStatus a badge ───────────────────────────────────
const mapStatus = (status) => {
  switch (status) {
    case 'COMPLETED':
      return 'Completado';
    case 'PENDING':
      return 'Pendiente';
    case 'FAILED':
      return 'Fallido';
    default:
      return status;
  }
};

// ── Mapa de íconos temáticos por tipo de movimiento ───────────────────────────
const MOVEMENT_ICON = {
  cancel:   { Icon: XCircle },
  credit:   { Icon: Wallet },
  purchase: { Icon: ShoppingCart },
};

// ── Ilustración: dos tarjetas superpuestas ─────────────────────────────────────
function WalletIllustration({ palette }) {
  return (
    <div className="relative w-72 h-48 select-none" aria-hidden="true">
      {/* Tarjeta trasera — desplazada y rotada */}
      <div
        className="absolute top-6 right-0 w-56 h-36 rounded-2xl"
        style={{
          background: `linear-gradient(135deg, ${palette.primaryDark}, ${palette.primary})`,
          transform: 'rotate(-6deg)',
          boxShadow: `0 8px 32px ${palette.primary}40`,
          opacity: 0.75,
        }}
      />

      {/* Tarjeta delantera */}
      <div
        className="absolute top-0 right-4 w-60 h-38 rounded-2xl overflow-hidden"
        style={{
          background: `linear-gradient(135deg, ${palette.primary}, ${palette.accent})`,
          boxShadow: `0 12px 40px ${palette.primary}50`,
          width: '240px',
          height: '152px',
        }}
      >
        {/* Brillo superior */}
        <div
          className="absolute inset-x-0 top-0 h-16 rounded-t-2xl"
          style={{ background: 'linear-gradient(180deg, rgba(255,255,255,0.12) 0%, transparent 100%)' }}
        />

        {/* Chip */}
        <div
          className="absolute top-5 left-5 w-9 h-7 rounded-md"
          style={{ background: `linear-gradient(135deg, ${palette.accent}, ${palette.primary})`, opacity: 0.9 }}
        />

        {/* Ícono contactless — esquina superior derecha */}
        <div className="absolute top-5 right-5 opacity-80">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2z"
              stroke="white" strokeWidth="1.5" strokeDasharray="3 3" opacity="0.4"/>
            <path d="M8 12c0-2.2 1.8-4 4-4" stroke="white" strokeWidth="1.8"
              strokeLinecap="round" />
            <path d="M10.5 12c0-.8.7-1.5 1.5-1.5" stroke="white" strokeWidth="1.8"
              strokeLinecap="round" />
            <circle cx="12" cy="12" r="1" fill="white" />
          </svg>
        </div>

        {/* Texto "Tu Evento" abajo a la izquierda */}
        <div className="absolute bottom-4 left-5 flex items-center gap-1.5">
          <span
            className="text-sm font-black tracking-wide"
            style={{ color: 'rgba(255,255,255,0.9)', fontFamily: 'sans-serif' }}
          >
            Tu{' '}
            <span style={{ color: 'rgba(255,255,255,0.75)' }}>Evento</span>
          </span>
        </div>

        {/* Toggle/switch decorativo abajo a la derecha */}
        <div className="absolute bottom-4 right-5 flex items-center gap-1">
          <div className="w-7 h-4 rounded-full bg-white/20 border border-white/30 flex items-center px-0.5">
            <div className="w-3 h-3 rounded-full bg-white/80 ml-auto" />
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Componente principal ───────────────────────────────────────────────────────
export default function WalletPage() {
  const navigate = useNavigate();
  const { palette } = useTheme();
  const [showAll, setShowAll] = useState(false);
  
  // Estado
  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);

        // Cargar wallet y transacciones en paralelo
        const [walletRes, txRes] = await Promise.all([
          getMyWallet().catch(() => null), // Si falla, retornar null
          getMyWalletTransactions().catch(() => ({ success: true, data: [] })),
        ]);

        if (walletRes?.success) {
          setWallet(walletRes.data);
        }

        if (txRes?.success) {
          setTransactions(txRes.data || []);
        }
      } catch (err) {
        console.error('Error loading wallet data:', err);
        setError(err.message || 'Error al cargar datos de cartera');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  // Procesamiento de transacciones para UI
  const processedTransactions = transactions.map(tx => {
    const isAdditive = ['CREDIT', 'REVERSAL', 'ADJUSTMENT'].includes(tx.type);
    const { iconType, concept } = mapTransactionType(tx.type, isAdditive);
    
    // Determinar si el monto es positivo o negativo según el tipo
    let displayAmount = tx.amount;
    if (tx.type === 'PAYMENT') {
      displayAmount = -Math.abs(tx.amount);
    } else {
      displayAmount = Math.abs(tx.amount);
    }

    return {
      id: tx.transactionId,
      concept,
      event: tx.eventName || `ID: ${tx.entityId || 'N/A'}`,
      date: tx.createdAt,
      status: mapStatus(tx.status),
      amount: displayAmount,
      iconType,
    };
  });

  const visibleMovements = showAll ? processedTransactions : processedTransactions.slice(0, 10);
  const availableBalance = wallet?.availableBalance || 0;
  const reservedBalance = wallet?.reservedBalance || 0;

  // Estado vacío o sin wallet
  if (!loading && !wallet) {
    return (
      <div
        className="min-h-screen w-full flex items-center justify-center"
        style={{ backgroundColor: palette.background, color: palette.textPrimary }}
      >
        <div className="text-center max-w-md px-4">
          <Wallet className="w-16 h-16 mx-auto mb-4" style={{ color: palette.textMuted }} />
          <h2 className="text-2xl font-bold mb-2">Aún no tenés crédito acumulado</h2>
          <p className="text-sm" style={{ color: palette.textSecondary }}>
            Tu saldo de cartera se generará cuando canceles una compra o recibas un reembolso.
          </p>
        </div>
      </div>
    );
  }

  // Loading skeleton
  if (loading) {
    return (
      <div
        className="min-h-screen w-full flex items-center justify-center"
        style={{ backgroundColor: palette.background, color: palette.textPrimary }}
      >
        <Loader2 className="w-8 h-8 animate-spin" style={{ color: palette.primary }} />
      </div>
    );
  }

  return (
    <div
      className="min-h-screen w-full"
      style={{ backgroundColor: palette.background, color: palette.textPrimary }}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">

        {/* ══════════════════════════════════════════════════════════════════════
            HERO
        ══════════════════════════════════════════════════════════════════════ */}
        <section className="flex flex-col md:flex-row items-center gap-8 py-2">
          {/* Texto */}
          <div className="flex-1 space-y-4">
            {/* Badge pill */}
            <span 
              className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold"
              style={{ 
                color: palette.accent,
                backgroundColor: `${palette.primary}20`,
                borderWidth: '1px',
                borderColor: `${palette.primary}30`
              }}
            >
              <Wallet className="w-3.5 h-3.5" />
              Mi Saldo
            </span>

            {/* Título */}
            <h1 className="text-3xl md:text-4xl font-black leading-tight">
              <span>Tu saldo </span>
              <span style={{ color: palette.primary }}>disponible</span>
            </h1>

            {/* Descripción */}
            <p className="text-sm leading-relaxed max-w-sm" style={{ color: palette.textSecondary }}>
              Llevá el control de tus fondos y disfrutá de una experiencia
              segura, rápida y sin complicaciones para tus compras en eventos.
            </p>
          </div>

          {/* Ilustración */}
          <div className="flex-shrink-0 flex items-center justify-center md:justify-end">
            <WalletIllustration palette={palette} />
          </div>
        </section>

        {/* ══════════════════════════════════════════════════════════════════════
            CUERPO — tarjeta saldo + historial
        ══════════════════════════════════════════════════════════════════════ */}
        <div className="grid lg:grid-cols-12 gap-5 items-start">

          {/* ── Tarjeta SALDO ACTUAL ────────────────────────────────────────── */}
          <div 
            className="lg:col-span-4 rounded-2xl shadow-lg overflow-hidden relative"
            style={{ 
              backgroundColor: palette.surface,
              borderWidth: '1px',
              borderColor: `${palette.textPrimary}15`
            }}
          >

            {/* Brillo decorativo esquina superior derecha */}
            <div
              className="absolute top-0 right-0 w-40 h-40 pointer-events-none"
              style={{
                background: `radial-gradient(circle at top right, ${palette.primary}30 0%, transparent 70%)`,
                filter: 'blur(16px)',
              }}
            />

            {/* Label */}
            <div className="px-6 pt-5 pb-1 relative z-10">
              <span 
                className="text-[10px] uppercase tracking-widest font-bold"
                style={{ color: palette.textMuted }}
              >
                SALDO ACTUAL
              </span>
            </div>

            <div className="px-6 pb-6 space-y-5 relative z-10">
              {/* Monto */}
              <div>
                <p className="text-4xl font-black tracking-tight">
                  $ {availableBalance.toLocaleString('es-CO')}
                </p>
                <p className="text-xs mt-0.5" style={{ color: palette.textMuted }}>COP</p>
              </div>

              {/* Subtítulo con saldo reservado si existe */}
              {reservedBalance > 0 ? (
                <p className="text-sm" style={{ color: palette.textSecondary }}>
                  ${reservedBalance.toLocaleString('es-CO')} en compras pendientes
                </p>
              ) : (
                <p className="text-sm" style={{ color: palette.textSecondary }}>
                  Saldo disponible para usar en tus compras
                </p>
              )}

              {/* Badge protegido */}
              <span 
                className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-bold"
                style={{
                  color: palette.success,
                  backgroundColor: palette.successBg,
                  borderWidth: '1px',
                  borderColor: `${palette.success}40`
                }}
              >
                <ShieldCheck className="w-3 h-3" />
                Crédito interno protegido
              </span>

              {/* Divider */}
              <div style={{ borderTop: `1px solid ${palette.textPrimary}10` }} />

              {/* Botón único */}
              <div className="space-y-2.5">
                <button
                  onClick={() => navigate('/events')}
                  className="w-full flex items-center justify-between px-5 py-3 rounded-xl
                    text-sm font-bold transition-all hover:brightness-110 active:scale-[0.98]"
                  style={{
                    background: `linear-gradient(135deg, ${palette.primary}, ${palette.primaryDark})`,
                    color: palette.textPrimary,
                    boxShadow: `0 4px 20px ${palette.primary}50`,
                  }}
                >
                  <div className="flex items-center gap-2">
                    <Wallet className="w-4 h-4" />
                    Usar saldo en una compra
                  </div>
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>

          {/* ── Historial de movimientos ────────────────────────────────────── */}
          <div 
            className="lg:col-span-8 rounded-2xl shadow-lg overflow-hidden"
            style={{
              backgroundColor: palette.surface,
              borderWidth: '1px',
              borderColor: `${palette.textPrimary}15`
            }}
          >

            {/* Header */}
            <div 
              className="px-6 py-4 flex items-center justify-between"
              style={{ borderBottom: `1px solid ${palette.textPrimary}10` }}
            >
              <h2 className="text-sm font-bold">Historial de movimientos</h2>
              <button
                onClick={() => setShowAll(!showAll)}
                className="text-xs font-semibold flex items-center gap-1 transition-colors hover:brightness-110"
                style={{ color: palette.primary }}
              >
                {showAll ? 'Ver menos' : 'Ver todo'}
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>

            {/* Tabla */}
            <div className="overflow-x-auto">
              <table className="w-full text-left table-auto">
                <thead>
                  <tr style={{ borderBottom: `1px solid ${palette.textPrimary}10` }}>
                    {[
                      { label: 'CONCEPTO', right: false, width: 'w-2/5' },
                      { label: 'FECHA',    right: false, width: 'w-1/5' },
                      { label: 'ESTADO',   right: false, width: 'w-1/5' },
                      { label: 'MONTO',    right: true,  width: 'w-1/5' },
                    ].map(({ label, right, width }) => (
                      <th
                        key={label}
                        className={`px-5 py-3 text-[10px] font-bold uppercase tracking-widest ${width}
                          ${right ? 'text-right' : ''}`}
                        style={{ color: palette.textMuted }}
                      >
                        {label}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {visibleMovements.map((m, idx) => {
                    const isCredit = m.amount > 0;
                    const { Icon } = MOVEMENT_ICON[m.iconType] ?? MOVEMENT_ICON.credit;
                    
                    // Colores dinámicos para el ícono basados en palette
                    const iconBg = m.iconType === 'credit' 
                      ? `${palette.primary}30` 
                      : m.iconType === 'purchase' 
                      ? `${palette.accent}30` 
                      : `${palette.success}30`;
                    const iconColor = m.iconType === 'credit' 
                      ? palette.primary 
                      : m.iconType === 'purchase' 
                      ? palette.accent 
                      : palette.success;

                    return (
                      <tr
                        key={m.id}
                        className="transition-colors"
                        style={{
                          borderBottom: `1px solid ${palette.textPrimary}10`,
                          backgroundColor: idx % 2 !== 0 ? `${palette.textPrimary}03` : 'transparent'
                        }}
                      >
                        {/* Concepto */}
                        <td className="px-5 py-4 w-2/5">
                          <div className="flex items-center gap-3">
                            <div 
                              className="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0"
                              style={{ 
                                backgroundColor: iconBg,
                                borderWidth: '1px',
                                borderColor: `${iconColor}30`
                              }}
                            >
                              <Icon className="w-3.5 h-3.5" style={{ color: iconColor }} />
                            </div>
                            <div className="min-w-0 flex-1">
                              <p className="text-sm font-semibold truncate">
                                {m.concept}
                              </p>
                              <p className="text-[10px] truncate" style={{ color: palette.textMuted }}>
                                {m.event}
                              </p>
                            </div>
                          </div>
                        </td>

                        {/* Fecha */}
                        <td className="px-5 py-4 text-xs whitespace-nowrap w-1/5" style={{ color: palette.textSecondary }}>
                          {fmtDate(m.date)}
                        </td>

                        {/* Estado */}
                        <td className="px-5 py-4 w-1/5">
                          <span 
                            className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold"
                            style={{
                              backgroundColor: m.status === 'Completado' 
                                ? palette.successBg 
                                : m.status === 'Pendiente' 
                                ? `${palette.primary}20` 
                                : palette.errorBg,
                              color: m.status === 'Completado' 
                                ? palette.success 
                                : m.status === 'Pendiente' 
                                ? palette.primary 
                                : palette.error,
                              borderWidth: '1px',
                              borderColor: m.status === 'Completado' 
                                ? `${palette.success}40` 
                                : m.status === 'Pendiente' 
                                ? `${palette.primary}40` 
                                : `${palette.error}40`
                            }}
                          >
                            <CheckCircle className="w-2.5 h-2.5" />
                            {m.status}
                          </span>
                        </td>

                        {/* Monto */}
                        <td 
                          className="px-5 py-4 text-sm font-bold text-right whitespace-nowrap w-1/5"
                          style={{ color: isCredit ? palette.success : palette.error }}
                        >
                          {isCredit ? '+ ' : '- '}{fmtCOP(m.amount)}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>

              {processedTransactions.length === 0 && (
                <div className="p-10 text-center text-sm" style={{ color: palette.textMuted }}>
                  No hay movimientos registrados
                </div>
              )}
            </div>
          </div>
        </div>

        {/* ══════════════════════════════════════════════════════════════════════
            TARJETAS INFORMATIVAS
        ══════════════════════════════════════════════════════════════════════ */}
        <div className="grid sm:grid-cols-3 gap-4">
          {[
            {
              icon: Zap,
              title: '¿Cómo usarlo?',
              text:  'Utilizá tu saldo disponible para comprar tickets, servicios y más en nuestros eventos.',
              arrow: true,
            },
            {
              icon: ShieldCheck,
              title: 'Saldo Protegido',
              text:  'Tu dinero está seguro con nuestra tecnología de encriptación y procesos verificados.',
              arrow: true,
            },
            {
              icon: HelpCircle,
              title: '¿Dudas?',
              text:  'Si tenés preguntas sobre tu saldo o movimientos, contactá a nuestro equipo de soporte.',
              arrow: true,
            },
          ].map(({ icon: Icon, title, text, arrow }) => (
            <div
              key={title}
              className="rounded-2xl p-5 flex items-start gap-4 shadow-lg transition-all cursor-pointer hover:brightness-110"
              style={{
                backgroundColor: palette.surface,
                borderWidth: '1px',
                borderColor: `${palette.textPrimary}15`
              }}
            >
              {/* Ícono circular */}
              <div 
                className="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0"
                style={{
                  backgroundColor: `${palette.primary}30`,
                  borderWidth: '1px',
                  borderColor: `${palette.primary}40`
                }}
              >
                <Icon className="w-5 h-5" style={{ color: palette.primary }} />
              </div>

              {/* Texto + flecha */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between mb-1">
                  <p className="text-sm font-bold">{title}</p>
                  {arrow && <ChevronRight className="w-4 h-4 flex-shrink-0" style={{ color: palette.textMuted }} />}
                </div>
                <p className="text-xs leading-relaxed" style={{ color: palette.textSecondary }}>{text}</p>
              </div>
            </div>
          ))}
        </div>

      </div>

      <Footer />
    </div>
  );
}
