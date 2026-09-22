import { useState } from 'react';
import {
  Wallet, ShoppingCart, RefreshCcw, ShieldCheck, HelpCircle,
  ChevronRight, CheckCircle, Zap, XCircle,
} from 'lucide-react';

import Footer from '../layouts/Footer';

// ── Datos mock ─────────────────────────────────────────────────────────────────
const MOCK_BALANCE = 250000;

const MOCK_MOVEMENTS = [
  {
    id: 1,
    concept: 'Cancelación de evento',
    event: 'Evento #EV-0012',
    date: '2025-08-14',
    status: 'Completado',
    amount: +100,
    iconType: 'cancel',
  },
  {
    id: 2,
    concept: 'Crédito compensat...',
    event: 'Evento #EV-0008',
    date: '2025-07-30',
    status: 'Completado',
    amount: +70,
    iconType: 'credit',
  },
  {
    id: 3,
    concept: 'Aplicación en compra',
    event: 'Evento #EV-0007',
    date: '2025-07-12',
    status: 'Aplicado',
    amount: -45,
    iconType: 'purchase',
  },
];

// ── Helpers ────────────────────────────────────────────────────────────────────
const fmtCOP = (n) =>
  new Intl.NumberFormat('es-CO', {
    style: 'currency', currency: 'COP', maximumFractionDigits: 0,
  }).format(Math.abs(n));

const fmtDate = (d) =>
  new Date(d + 'T00:00:00').toLocaleDateString('es-CO', {
    day: '2-digit', month: 'short', year: 'numeric',
  });

// ── Mapa de íconos temáticos por tipo de movimiento ───────────────────────────
const MOVEMENT_ICON = {
  cancel:   { Icon: XCircle,     bg: 'bg-emerald-500/20 border border-emerald-500/30', color: 'text-emerald-400' },
  credit:   { Icon: Wallet,      bg: 'bg-violet-500/20  border border-violet-500/30',  color: 'text-violet-400'  },
  purchase: { Icon: ShoppingCart, bg: 'bg-purple-500/20  border border-purple-500/30',  color: 'text-purple-400'  },
};

// ── Ilustración: dos tarjetas superpuestas ─────────────────────────────────────
function WalletIllustration() {
  return (
    <div className="relative w-72 h-48 select-none" aria-hidden="true">
      {/* Tarjeta trasera — desplazada y rotada */}
      <div
        className="absolute top-6 right-0 w-56 h-36 rounded-2xl"
        style={{
          background: 'linear-gradient(135deg, #5b21b6 0%, #1d4ed8 100%)',
          transform: 'rotate(-6deg)',
          boxShadow: '0 8px 32px rgba(91,33,182,0.4)',
          opacity: 0.75,
        }}
      />

      {/* Tarjeta delantera */}
      <div
        className="absolute top-0 right-4 w-60 h-38 rounded-2xl overflow-hidden"
        style={{
          background: 'linear-gradient(135deg, #7f13ec 0%, #3b82f6 100%)',
          boxShadow: '0 12px 40px rgba(127,19,236,0.5)',
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
          style={{ background: 'linear-gradient(135deg, #a78bfa, #7c3aed)', opacity: 0.9 }}
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
            <span style={{ color: '#c4b5fd' }}>Evento</span>
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
  const [showAll, setShowAll] = useState(false);
  const visibleMovements = showAll ? MOCK_MOVEMENTS : MOCK_MOVEMENTS.slice(0, 3);

  return (
    <div
      className="min-h-screen w-full text-white"
      style={{ backgroundColor: '#12091b' }}
    >
      <div className="max-w-5xl mx-auto px-4 py-10 space-y-8">

        {/* ══════════════════════════════════════════════════════════════════════
            HERO
        ══════════════════════════════════════════════════════════════════════ */}
        <section className="flex flex-col md:flex-row items-center gap-8 py-2">
          {/* Texto */}
          <div className="flex-1 space-y-4">
            {/* Badge pill */}
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold
              text-purple-300 bg-purple-500/10 border border-purple-500/20">
              <Wallet className="w-3.5 h-3.5" />
              Mi Saldo
            </span>

            {/* Título */}
            <h1 className="text-3xl md:text-4xl font-black leading-tight">
              <span className="text-white">Tu saldo </span>
              <span className="text-violet-400">disponible</span>
            </h1>

            {/* Descripción — texto de la imagen */}
            <p className="text-slate-400 text-sm leading-relaxed max-w-sm">
              Llevá el control de tus fondos y disfrutá de una experiencia
              segura, rápida y sin complicaciones para tus compras en eventos.
            </p>
          </div>

          {/* Ilustración */}
          <div className="flex-shrink-0 flex items-center justify-center md:justify-end">
            <WalletIllustration />
          </div>
        </section>

        {/* ══════════════════════════════════════════════════════════════════════
            CUERPO — tarjeta saldo + historial
        ══════════════════════════════════════════════════════════════════════ */}
        <div className="grid md:grid-cols-5 gap-5 items-start">

          {/* ── Tarjeta SALDO ACTUAL ────────────────────────────────────────── */}
          <div className="md:col-span-2 rounded-2xl border border-white/10
            bg-[#1a0d28] shadow-lg shadow-black/40 overflow-hidden relative">

            {/* Brillo decorativo esquina superior derecha */}
            <div
              className="absolute top-0 right-0 w-40 h-40 pointer-events-none"
              style={{
                background: 'radial-gradient(circle at top right, rgba(127,19,236,0.18) 0%, transparent 70%)',
                filter: 'blur(16px)',
              }}
            />

            {/* Label */}
            <div className="px-6 pt-5 pb-1 relative z-10">
              <span className="text-[10px] uppercase tracking-widest text-slate-500 font-bold">
                SALDO ACTUAL
              </span>
            </div>

            <div className="px-6 pb-6 space-y-5 relative z-10">
              {/* Monto */}
              <div>
                <p className="text-4xl font-black text-white tracking-tight">
                  $ {(MOCK_BALANCE).toLocaleString('es-CO')}
                </p>
                <p className="text-xs text-slate-500 mt-0.5">CRC</p>
              </div>

              {/* Subtítulo */}
              <p className="text-sm text-slate-400">
                Saldo correspondiente por cancelación
              </p>

              {/* Badge protegido */}
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full
                text-[10px] font-bold text-emerald-300 bg-emerald-500/10 border border-emerald-500/20">
                <ShieldCheck className="w-3 h-3" />
                Crédito interno protegido
              </span>

              {/* Divider */}
              <div className="border-t border-white/5" />

              {/* Botones */}
              <div className="space-y-2.5">
                <button
                  className="w-full flex items-center justify-between px-5 py-3 rounded-xl
                    text-sm font-bold text-white transition-all hover:brightness-110 active:scale-[0.98]"
                  style={{
                    background: 'linear-gradient(135deg, #7f13ec, #3b82f6)',
                    boxShadow: '0 4px 20px rgba(127,19,236,0.35)',
                  }}
                >
                  <div className="flex items-center gap-2">
                    <Wallet className="w-4 h-4" />
                    Usar saldo en una compra
                  </div>
                  <ChevronRight className="w-4 h-4" />
                </button>

                <button
                  className="w-full flex items-center justify-center gap-2 py-3 rounded-xl
                    text-sm font-bold text-slate-300 border border-white/10
                    hover:bg-white/5 transition-all active:scale-[0.98]"
                >
                  <RefreshCcw className="w-4 h-4" />
                  Solicitar reembolso externo
                </button>
              </div>
            </div>
          </div>

          {/* ── Historial de movimientos ────────────────────────────────────── */}
          <div className="md:col-span-3 rounded-2xl border border-white/10
            bg-[#1a0d28] shadow-lg shadow-black/40 overflow-hidden">

            {/* Header */}
            <div className="px-6 py-4 flex items-center justify-between border-b border-white/5">
              <h2 className="text-sm font-bold text-white">Historial de movimientos</h2>
              <button
                onClick={() => setShowAll(!showAll)}
                className="text-xs font-semibold flex items-center gap-1
                  text-violet-400 hover:text-violet-300 transition-colors"
              >
                {showAll ? 'Ver menos' : 'Ver todo'}
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>

            {/* Tabla */}
            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead>
                  <tr className="border-b border-white/5">
                    {[
                      { label: 'CONCEPTO', right: false },
                      { label: 'FECHA',    right: false },
                      { label: 'ESTADO',   right: false },
                      { label: 'MONTO',    right: true  },
                    ].map(({ label, right }) => (
                      <th
                        key={label}
                        className={`px-5 py-3 text-[10px] font-bold uppercase tracking-widest text-slate-500
                          ${right ? 'text-right' : ''}`}
                      >
                        {label}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {visibleMovements.map((m, idx) => {
                    const isCredit = m.amount > 0;
                    const { Icon, bg, color } = MOVEMENT_ICON[m.iconType] ?? MOVEMENT_ICON.credit;
                    return (
                      <tr
                        key={m.id}
                        className={`border-b border-white/5 hover:bg-white/[0.02] transition-colors
                          ${idx % 2 !== 0 ? 'bg-white/[0.01]' : ''}`}
                      >
                        {/* Concepto */}
                        <td className="px-5 py-4">
                          <div className="flex items-center gap-3">
                            <div className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 ${bg}`}>
                              <Icon className={`w-3.5 h-3.5 ${color}`} />
                            </div>
                            <div className="min-w-0">
                              <p className="text-sm font-semibold text-white truncate max-w-[130px]">
                                {m.concept}
                              </p>
                              <p className="text-[10px] text-slate-500 truncate max-w-[130px]">
                                {m.event}
                              </p>
                            </div>
                          </div>
                        </td>

                        {/* Fecha */}
                        <td className="px-5 py-4 text-xs text-slate-400 whitespace-nowrap">
                          {fmtDate(m.date)}
                        </td>

                        {/* Estado */}
                        <td className="px-5 py-4">
                          <span className={`inline-flex items-center gap-1 px-2.5 py-0.5
                            rounded-full text-[10px] font-bold
                            ${m.status === 'Completado'
                              ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/25'
                              : 'bg-violet-500/15  text-violet-300  border border-violet-500/25'
                            }`}>
                            <CheckCircle className="w-2.5 h-2.5" />
                            {m.status}
                          </span>
                        </td>

                        {/* Monto */}
                        <td className={`px-5 py-4 text-sm font-bold text-right whitespace-nowrap
                          ${isCredit ? 'text-emerald-400' : 'text-rose-400'}`}>
                          {isCredit ? '+ $' : '- $'}{Math.abs(m.amount).toLocaleString('es-CO')}.00
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>

              {MOCK_MOVEMENTS.length === 0 && (
                <div className="p-10 text-center text-slate-500 text-sm">
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
              circleBg: 'bg-purple-500/20 border border-purple-500/30',
              iconColor: 'text-purple-400',
              title: '¿Cómo usarlo?',
              text:  'Utilizá tu saldo disponible para comprar tickets, servicios y más en nuestros eventos.',
              arrow: true,
            },
            {
              icon: ShieldCheck,
              circleBg: 'bg-emerald-500/20 border border-emerald-500/30',
              iconColor: 'text-emerald-400',
              title: 'Saldo Protegido',
              text:  'Tu dinero está seguro con nuestra tecnología de encriptación y procesos verificados.',
              arrow: true,
            },
            {
              icon: HelpCircle,
              circleBg: 'bg-sky-500/20 border border-sky-500/30',
              iconColor: 'text-sky-400',
              title: '¿Dudas?',
              text:  'Si tenés preguntas sobre tu saldo o movimientos, contactá a nuestro equipo de soporte.',
              arrow: true,
            },
          ].map(({ icon: Icon, circleBg, iconColor, title, text, arrow }) => (
            <div
              key={title}
              className="rounded-2xl border border-white/10 bg-[#1a0d28] p-5
                flex items-start gap-4 shadow-lg shadow-black/30
                hover:border-white/20 transition-colors cursor-pointer"
            >
              {/* Ícono circular */}
              <div className={`w-10 h-10 rounded-full flex items-center justify-center
                flex-shrink-0 ${circleBg}`}>
                <Icon className={`w-5 h-5 ${iconColor}`} />
              </div>

              {/* Texto + flecha */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between mb-1">
                  <p className="text-sm font-bold text-white">{title}</p>
                  {arrow && <ChevronRight className="w-4 h-4 text-slate-500 flex-shrink-0" />}
                </div>
                <p className="text-xs text-slate-400 leading-relaxed">{text}</p>
              </div>
            </div>
          ))}
        </div>

      </div>

      <Footer />
    </div>
  );
}
