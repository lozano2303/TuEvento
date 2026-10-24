/**
 * AdminRefunds.jsx
 * Panel de reembolsos del admin: resumen, listado con desglose pasarela/wallet y
 * acreditación manual de saldo a la cartera de un usuario (buscador + monto + motivo).
 */

import { useState, useEffect, useMemo, useCallback, useRef } from 'react';
import { adminText as t } from '../constants/adminText';
import {
  Search, Loader2, AlertCircle, RefreshCcw, Clock, CheckCircle2, XCircle,
  Wallet, Plus, X, ChevronDown, ArrowLeftRight,
} from 'lucide-react';
import AdminSidebar from '../components/admin/AdminSidebar';
import Toast from '../components/Toast';
import { useToast } from '../hooks/useToast';
import { getAdminRefunds, getAdminStats } from '../services/AdminRefundService';
import { getAdminUsers } from '../services/AdminUserService';
import { creditWallet } from '../services/WalletService';

const STATUS_FILTERS = ['', 'PROCESSED', 'REQUESTED', 'FAILED'];
const REASON_MIN = 5;
const REASON_MAX = 255;

const STATUS_STYLE = {
  PROCESSED: { cls: 'bg-emerald-500/15 text-emerald-300 border-emerald-500/25', icon: CheckCircle2 },
  REQUESTED: { cls: 'bg-amber-500/15 text-amber-300 border-amber-500/25',       icon: Clock },
  FAILED:    { cls: 'bg-rose-500/15 text-rose-300 border-rose-500/25',          icon: XCircle },
};

const fmtCOP = (n) =>
  new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })
    .format(Number(n) || 0);

// ── Tarjeta de resumen ────────────────────────────────────────────────────────
function StatCard({ icon: Icon, iconBg, iconColor, label, value, sub, loading }) {
  return (
    <div className="rounded-xl p-5 border border-white/[0.07] flex flex-col gap-3" style={{ background: 'rgba(255,255,255,0.04)' }}>
      <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${iconBg}`}>
        <Icon className={`w-5 h-5 ${iconColor}`} />
      </div>
      <div>
        <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-textMuted mb-1">{label}</p>
        {loading
          ? <div className="h-8 w-28 rounded-lg bg-surfaceAlt animate-pulse" />
          : <p className="text-2xl font-black leading-none text-textPrimary" style={{ letterSpacing: '-0.02em' }}>{value}</p>}
        {sub && !loading && <p className="text-[13px] mt-1.5 text-textSecondary">{sub}</p>}
      </div>
    </div>
  );
}

// ── Selector de usuario con búsqueda ──────────────────────────────────────────
function UserPicker({ value, onChange, t }) {
  const [open,    setOpen]    = useState(false);
  const [query,   setQuery]   = useState('');
  const [options, setOptions] = useState([]);
  const [loading, setLoading] = useState(false);
  const boxRef = useRef(null);

  useEffect(() => {
    if (!open) return;
    setLoading(true);
    const id = setTimeout(async () => {
      try {
        setOptions(await getAdminUsers({ q: query, limit: 20 }));
      } catch {
        setOptions([]);
      } finally {
        setLoading(false);
      }
    }, 250);
    return () => clearTimeout(id);
  }, [query, open]);

  useEffect(() => {
    const onClick = (e) => { if (boxRef.current && !boxRef.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  const label = (u) => u.fullName || u.alias;

  return (
    <div className="relative" ref={boxRef}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="w-full flex items-center justify-between gap-2 rounded-xl bg-background border border-surfaceAlt px-3 py-2.5 text-sm text-left focus:border-primary"
      >
        {value
          ? <span className="truncate"><span className="text-textPrimary font-semibold">{label(value)}</span>
              <span className="text-textMuted"> · @{value.alias}</span></span>
          : <span className="text-textMuted">{t('adminRefunds.credit.userPlaceholder')}</span>}
        <ChevronDown className={`w-4 h-4 text-textMuted shrink-0 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>

      {open && (
        <div className="absolute z-10 mt-1 w-full rounded-xl bg-surface border border-surfaceAlt shadow-xl overflow-hidden">
          <div className="flex items-center gap-2 px-3 py-2 border-b border-surfaceAlt">
            <Search className="w-4 h-4 text-textMuted" />
            <input
              autoFocus
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={t('adminRefunds.credit.userSearch')}
              className="bg-transparent outline-none text-sm w-full text-textPrimary placeholder:text-textMuted"
            />
          </div>
          <div className="max-h-56 overflow-y-auto">
            {loading ? (
              <div className="flex items-center gap-2 px-3 py-3 text-xs text-textMuted">
                <Loader2 className="w-3.5 h-3.5 animate-spin" /> {t('adminUsers.loading')}
              </div>
            ) : options.length === 0 ? (
              <p className="px-3 py-3 text-xs text-textMuted">{t('adminUsers.empty')}</p>
            ) : options.map((u) => (
              <button
                key={u.userId}
                type="button"
                onClick={() => { onChange(u); setOpen(false); }}
                className={`w-full text-left px-3 py-2 hover:bg-surfaceAlt/50 ${value?.userId === u.userId ? 'bg-primary/15' : ''}`}
              >
                <p className="text-sm text-textPrimary font-semibold truncate">{label(u)}</p>
                <p className="text-xs text-textMuted truncate">@{u.alias}{u.email ? ` · ${u.email}` : ''}</p>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ── Página ────────────────────────────────────────────────────────────────────
export default function AdminRefunds() {
  const { toast, showToast, hideToast } = useToast();

  const [refunds, setRefunds] = useState([]);
  const [stats,   setStats]   = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState(null);
  const [status,  setStatus]  = useState('');
  const [query,   setQuery]   = useState('');

  // Modal "Agregar crédito"
  const [creditOpen, setCreditOpen] = useState(false);
  const [creditUser, setCreditUser] = useState(null);
  const [amount,     setAmount]     = useState('');
  const [reason,     setReason]     = useState('');
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [list, s] = await Promise.all([getAdminRefunds(), getAdminStats()]);
      setRefunds(list);
      setStats(s);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  const visible = useMemo(() => {
    const term = query.trim().toLowerCase();
    return refunds.filter((r) => {
      if (status && r.status !== status) return false;
      if (!term) return true;
      return [r.fullName, r.alias, r.email, r.eventName]
        .filter(Boolean).join(' ').toLowerCase().includes(term);
    });
  }, [refunds, status, query]);

  // Total devuelto a carteras (wallet + pasarela de los reembolsos procesados del listado)
  const toWalletTotal = useMemo(() => refunds
    .filter((r) => r.status === 'PROCESSED')
    .reduce((sum, r) => sum + Number(r.gatewayAmount || 0) + Number(r.walletAmount || 0), 0), [refunds]);

  const fmtDateTime = (iso) => iso
    ? new Date(iso).toLocaleString('es-CO', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })
    : '—';

  const openCredit = () => {
    setCreditUser(null);
    setAmount('');
    setReason('');
    setCreditOpen(true);
  };

  const amountNumber = Number(amount);
  const trimmedReason = reason.trim();
  const creditValid = !!creditUser && amountNumber > 0
    && trimmedReason.length >= REASON_MIN && trimmedReason.length <= REASON_MAX;

  const submitCredit = async () => {
    if (!creditValid) return;
    setSubmitting(true);
    try {
      await creditWallet({ userId: creditUser.userId, amount: amountNumber, reason: trimmedReason });
      showToast('credit-ok', t('adminRefunds.credit.success', {
        amount: fmtCOP(amountNumber), name: creditUser.fullName || creditUser.alias,
      }), 'success');
      setCreditOpen(false);
    } catch (err) {
      showToast('credit-error', err.message || t('adminRefunds.credit.error'), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const cards = [
    { icon: Clock,          iconBg: 'bg-amber-500/15',   iconColor: 'text-amber-400',   label: t('adminRefunds.stats.pending'),   value: stats?.pendingRefunds ?? 0,   sub: t('adminRefunds.stats.pendingSub') },
    { icon: CheckCircle2,   iconBg: 'bg-emerald-500/15', iconColor: 'text-emerald-400', label: t('adminRefunds.stats.processed'), value: stats?.processedRefunds ?? 0, sub: t('adminRefunds.stats.processedSub') },
    { icon: ArrowLeftRight, iconBg: 'bg-rose-500/15',    iconColor: 'text-rose-400',    label: t('adminRefunds.stats.total'),     value: fmtCOP(stats?.totalRefundedAmount), sub: t('adminRefunds.stats.totalSub') },
    { icon: Wallet,         iconBg: 'bg-primary/20',     iconColor: 'text-accent',      label: t('adminRefunds.stats.toWallet'),  value: fmtCOP(toWalletTotal),        sub: t('adminRefunds.stats.toWalletSub') },
  ];

  return (
    <div className="min-h-screen flex bg-background text-textPrimary font-sans">
      <AdminSidebar activePage="refunds" />
      <main className="flex-1 overflow-y-auto bg-background">
        <div className="p-8 max-w-6xl mx-auto w-full">

          {/* Header */}
          <div className="mb-8 flex flex-wrap items-start justify-between gap-4">
            <div>
              <h1 className="text-2xl font-black uppercase tracking-wide text-textPrimary mb-1">{t('adminRefunds.title')}</h1>
              <p className="text-textSecondary text-sm">{t('adminRefunds.subtitle')}</p>
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => void load()}
                className="flex items-center gap-2 px-4 py-2.5 rounded-full text-sm font-semibold border border-surfaceAlt text-textSecondary hover:text-textPrimary hover:bg-surfaceAlt/40"
              >
                <RefreshCcw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
                {t('adminRefunds.refresh')}
              </button>
              <button
                onClick={openCredit}
                className="flex items-center gap-2 px-4 py-2.5 rounded-full text-sm font-bold text-white hover:brightness-110"
                style={{ background: 'linear-gradient(135deg, var(--color-primary), var(--color-primaryDark))' }}
              >
                <Plus className="w-4 h-4" />
                {t('adminRefunds.credit.open')}
              </button>
            </div>
          </div>

          {/* Resumen */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
            {cards.map((c, i) => <StatCard key={i} {...c} loading={loading && !stats} />)}
          </div>

          {/* Filtros */}
          <div className="flex flex-wrap items-center gap-3 mb-4">
            <div className="flex items-center gap-2 flex-1 min-w-64 px-4 py-2.5 rounded-xl bg-surface border border-surfaceAlt">
              <Search className="w-4 h-4 text-textMuted shrink-0" />
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder={t('adminRefunds.searchPlaceholder')}
                className="bg-transparent text-sm outline-none w-full text-textPrimary placeholder:text-textMuted"
              />
            </div>
            <div className="flex gap-1.5">
              {STATUS_FILTERS.map((s) => (
                <button
                  key={s || 'all'}
                  onClick={() => setStatus(s)}
                  className={`px-3.5 py-2 rounded-full text-xs font-semibold border transition-colors
                    ${status === s ? 'bg-primary text-white border-primary' : 'text-textSecondary border-surfaceAlt hover:text-textPrimary hover:bg-surfaceAlt/40'}`}
                >
                  {t(`adminRefunds.status.${s || 'ALL'}`)}
                </button>
              ))}
            </div>
          </div>

          {error && (
            <div className="mb-6 px-4 py-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" /> {error}
            </div>
          )}

          {/* Listado */}
          <div className="rounded-2xl border border-white/[0.07] overflow-hidden" style={{ background: 'rgba(255,255,255,0.02)' }}>
            {loading ? (
              <div className="flex items-center justify-center py-20 text-textMuted gap-2 text-sm">
                <Loader2 className="w-4 h-4 animate-spin" /> {t('adminRefunds.loading')}
              </div>
            ) : visible.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-20 gap-3 text-center">
                <ArrowLeftRight className="w-8 h-8 text-textMuted" />
                <p className="text-sm text-textSecondary">{t('adminRefunds.empty')}</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left text-[11px] uppercase tracking-[0.1em] text-textMuted border-b border-white/[0.06]">
                      <th className="px-5 py-3 font-bold">{t('adminRefunds.columns.date')}</th>
                      <th className="px-5 py-3 font-bold">{t('adminRefunds.columns.buyer')}</th>
                      <th className="px-5 py-3 font-bold">{t('adminRefunds.columns.event')}</th>
                      <th className="px-5 py-3 font-bold">{t('adminRefunds.columns.amount')}</th>
                      <th className="px-5 py-3 font-bold">{t('adminRefunds.columns.status')}</th>
                      <th className="px-5 py-3 font-bold">{t('adminRefunds.columns.reason')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {visible.map((r) => {
                      const st = STATUS_STYLE[r.status] ?? STATUS_STYLE.REQUESTED;
                      const gateway = Number(r.gatewayAmount || 0);
                      const wallet  = Number(r.walletAmount || 0);
                      return (
                        <tr key={r.refundId} className="border-b border-white/[0.04] last:border-0 hover:bg-white/[0.02] align-top">
                          <td className="px-5 py-3 text-textSecondary whitespace-nowrap">
                            <p>{fmtDateTime(r.requestedAt)}</p>
                            {r.processedAt && (
                              <p className="text-xs text-textMuted">{t('adminRefunds.processedAt', { date: fmtDateTime(r.processedAt) })}</p>
                            )}
                          </td>
                          <td className="px-5 py-3">
                            <p className="font-semibold text-textPrimary">{r.fullName || r.alias || `#${r.userId}`}</p>
                            <p className="text-xs text-textMuted">{r.email || (r.alias ? `@${r.alias}` : '')}</p>
                          </td>
                          <td className="px-5 py-3">
                            <p className="text-textPrimary">{r.eventName || `#${r.eventId}`}</p>
                            <p className="text-xs text-textMuted">{t('adminRefunds.order', { id: r.orderId })}</p>
                          </td>
                          <td className="px-5 py-3 whitespace-nowrap">
                            <p className="font-bold text-textPrimary">{fmtCOP(gateway + wallet)}</p>
                            {gateway > 0 && <p className="text-xs text-textMuted">{t('adminRefunds.gatewayPart', { amount: fmtCOP(gateway) })}</p>}
                            {wallet > 0 && <p className="text-xs text-textMuted">{t('adminRefunds.walletPart', { amount: fmtCOP(wallet) })}</p>}
                          </td>
                          <td className="px-5 py-3">
                            <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold border ${st.cls}`}>
                              <st.icon className="w-3 h-3" />
                              {t(`adminRefunds.status.${r.status}`, { defaultValue: r.status })}
                            </span>
                          </td>
                          <td className="px-5 py-3 text-textSecondary max-w-xs">{r.reason || '—'}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </main>

      {/* Modal: agregar crédito */}
      {creditOpen && (
        <div
          className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-[80] p-4"
          onClick={(e) => { if (e.target === e.currentTarget && !submitting) setCreditOpen(false); }}
        >
          <div className="bg-surface rounded-2xl border border-surfaceAlt shadow-2xl shadow-black/60 w-full max-w-md p-6 flex flex-col gap-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <h3 className="text-base font-black text-textPrimary">{t('adminRefunds.credit.title')}</h3>
                <p className="text-sm text-textSecondary mt-1">{t('adminRefunds.credit.subtitle')}</p>
              </div>
              <button onClick={() => !submitting && setCreditOpen(false)} className="text-textMuted hover:text-textPrimary">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wide text-textMuted mb-1.5">{t('adminRefunds.credit.user')}</label>
              <UserPicker value={creditUser} onChange={setCreditUser} t={t} />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wide text-textMuted mb-1.5">{t('adminRefunds.credit.amount')}</label>
              <input
                type="number"
                min="1"
                step="1"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="50000"
                className="w-full rounded-xl bg-background border border-surfaceAlt px-3 py-2.5 text-sm text-textPrimary outline-none focus:border-primary"
              />
              {amountNumber > 0 && <p className="text-xs text-textMuted mt-1">{fmtCOP(amountNumber)}</p>}
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wide text-textMuted mb-1.5">{t('adminRefunds.credit.reason')}</label>
              <textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                maxLength={REASON_MAX}
                rows={3}
                placeholder={t('adminRefunds.credit.reasonPlaceholder')}
                className="w-full rounded-xl bg-background border border-surfaceAlt px-3 py-2.5 text-sm text-textPrimary outline-none focus:border-primary resize-none"
              />
              <p className="text-[11px] text-textMuted text-right">{reason.length}/{REASON_MAX}</p>
            </div>

            <div className="flex gap-3 pt-1">
              <button
                onClick={() => setCreditOpen(false)}
                disabled={submitting}
                className="flex-1 py-2.5 rounded-full text-sm font-bold bg-surfaceAlt text-textSecondary border border-surfaceAlt hover:bg-surfaceAlt/70 disabled:opacity-50"
              >
                {t('adminUsers.cancel')}
              </button>
              <button
                onClick={submitCredit}
                disabled={!creditValid || submitting}
                className="flex-1 py-2.5 rounded-full text-sm font-bold flex items-center justify-center gap-2 text-white hover:brightness-110 disabled:opacity-50"
                style={{ background: 'linear-gradient(135deg, var(--color-primary), var(--color-primaryDark))' }}
              >
                {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
                {t('adminRefunds.credit.submit')}
              </button>
            </div>
          </div>
        </div>
      )}

      <Toast toast={toast} onHide={hideToast} />
    </div>
  );
}
