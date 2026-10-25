/**
 * AdminUsers.jsx
 * Gestión de usuarios del panel admin: búsqueda, filtro por rol y revocación del
 * rol de organizador (el usuario vuelve a USER y recibe el motivo por notificación).
 */

import { useState, useEffect, useCallback } from 'react';
import { adminText as t } from '../constants/adminText';
import { Search, Loader2, AlertCircle, UserMinus, Users, X } from 'lucide-react';
import AdminSidebar from '../components/admin/AdminSidebar';
import Toast from '../components/Toast';
import { useToast } from '../hooks/useToast';
import { getAdminUsers, revokeOrganizerRole } from '../services/AdminUserService';

const ROLE_FILTERS = ['', 'ORGANIZER', 'USER', 'ADMIN'];
const REASON_MIN = 10;
const REASON_MAX = 500;

const ROLE_BADGE = {
  ADMIN:     'bg-amber-500/15 text-amber-300 border-amber-500/25',
  ORGANIZER: 'bg-primary/20 text-accent border-primary/30',
  USER:      'bg-surfaceAlt/60 text-textSecondary border-surfaceAlt',
};

export default function AdminUsers() {
  const { toast, showToast, hideToast } = useToast();

  const [users,   setUsers]   = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState(null);
  const [query,   setQuery]   = useState('');
  const [role,    setRole]    = useState('');

  // Modal de revocación
  const [target,     setTarget]     = useState(null); // usuario a revocar
  const [reason,     setReason]     = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadUsers = useCallback(async (q, r) => {
    setLoading(true);
    setError(null);
    try {
      setUsers(await getAdminUsers({ q, role: r }));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  // Búsqueda con debounce para no pedir en cada tecla
  useEffect(() => {
    const id = setTimeout(() => { void loadUsers(query, role); }, 300);
    return () => clearTimeout(id);
  }, [query, role, loadUsers]);

  const openRevoke = (user) => {
    setTarget(user);
    setReason('');
  };

  const closeRevoke = () => {
    if (submitting) return;
    setTarget(null);
  };

  const trimmedReason = reason.trim();
  const reasonValid = trimmedReason.length >= REASON_MIN && trimmedReason.length <= REASON_MAX;

  const confirmRevoke = async () => {
    if (!target || !reasonValid) return;
    setSubmitting(true);
    try {
      await revokeOrganizerRole(target.userId, trimmedReason);
      showToast('revoke-ok', t('adminUsers.revokeSuccess', { name: target.fullName || target.alias }), 'success');
      setTarget(null);
      await loadUsers(query, role);
    } catch (err) {
      showToast('revoke-error', err.message || t('adminUsers.revokeError'), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const fmtDate = (iso) => iso
    ? new Date(iso).toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' })
    : '—';

  return (
    <div className="min-h-screen flex bg-background text-textPrimary font-sans">
      <AdminSidebar activePage="users" />
      <main className="flex-1 overflow-y-auto bg-background">
        <div className="p-8 max-w-6xl mx-auto w-full">

          {/* Header */}
          <div className="mb-6">
            <h1 className="text-2xl font-black uppercase tracking-wide text-textPrimary mb-1">
              {t('adminUsers.title')}
            </h1>
            <p className="text-textSecondary text-sm">{t('adminUsers.subtitle')}</p>
          </div>

          {/* Filtros */}
          <div className="flex flex-wrap items-center gap-3 mb-6">
            <div className="flex items-center gap-2 flex-1 min-w-64 px-4 py-2.5 rounded-xl bg-surface border border-surfaceAlt">
              <Search className="w-4 h-4 text-textMuted shrink-0" />
              <input
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder={t('adminUsers.searchPlaceholder')}
                className="bg-transparent text-sm outline-none w-full text-textPrimary placeholder:text-textMuted"
              />
            </div>
            <div className="flex gap-1.5">
              {ROLE_FILTERS.map((r) => (
                <button
                  key={r || 'all'}
                  onClick={() => setRole(r)}
                  className={`px-3.5 py-2 rounded-full text-xs font-semibold border transition-colors
                    ${role === r
                      ? 'bg-primary text-white border-primary'
                      : 'text-textSecondary border-surfaceAlt hover:text-textPrimary hover:bg-surfaceAlt/40'}`}
                >
                  {t(`adminUsers.roles.${r || 'ALL'}`)}
                </button>
              ))}
            </div>
          </div>

          {error && (
            <div className="mb-6 px-4 py-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              {error}
            </div>
          )}

          {/* Tabla */}
          <div className="rounded-2xl border border-white/[0.07] overflow-hidden" style={{ background: 'rgba(255,255,255,0.02)' }}>
            {loading ? (
              <div className="flex items-center justify-center py-20 text-textMuted gap-2 text-sm">
                <Loader2 className="w-4 h-4 animate-spin" /> {t('adminUsers.loading')}
              </div>
            ) : users.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-20 gap-3 text-center">
                <Users className="w-8 h-8 text-textMuted" />
                <p className="text-sm text-textSecondary">{t('adminUsers.empty')}</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left text-[11px] uppercase tracking-[0.1em] text-textMuted border-b border-white/[0.06]">
                      <th className="px-5 py-3 font-bold">{t('adminUsers.columns.user')}</th>
                      <th className="px-5 py-3 font-bold">{t('adminUsers.columns.email')}</th>
                      <th className="px-5 py-3 font-bold">{t('adminUsers.columns.role')}</th>
                      <th className="px-5 py-3 font-bold">{t('adminUsers.columns.status')}</th>
                      <th className="px-5 py-3 font-bold">{t('adminUsers.columns.joined')}</th>
                      <th className="px-5 py-3 font-bold text-right">{t('adminUsers.columns.actions')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((u) => (
                      <tr key={u.userId} className="border-b border-white/[0.04] last:border-0 hover:bg-white/[0.02]">
                        <td className="px-5 py-3">
                          <p className="font-semibold text-textPrimary">{u.fullName || u.alias}</p>
                          <p className="text-xs text-textMuted">@{u.alias}</p>
                        </td>
                        <td className="px-5 py-3 text-textSecondary">{u.email || '—'}</td>
                        <td className="px-5 py-3">
                          <span className={`inline-block px-2.5 py-0.5 rounded-full text-[11px] font-bold border ${ROLE_BADGE[u.role] ?? ROLE_BADGE.USER}`}>
                            {t(`adminUsers.roles.${u.role}`, { defaultValue: u.role })}
                          </span>
                        </td>
                        <td className="px-5 py-3 text-textSecondary">
                          {t(`adminUsers.statuses.${u.status}`, { defaultValue: u.status })}
                        </td>
                        <td className="px-5 py-3 text-textSecondary">{fmtDate(u.createdAt)}</td>
                        <td className="px-5 py-3 text-right">
                          {u.role === 'ORGANIZER' && (
                            <button
                              onClick={() => openRevoke(u)}
                              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold
                                bg-rose-500/10 text-rose-400 border border-rose-500/20
                                hover:bg-rose-500 hover:text-white hover:border-rose-500 transition-all"
                            >
                              <UserMinus className="w-3.5 h-3.5" />
                              {t('adminUsers.revokeButton')}
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </main>

      {/* Modal: quitar rol de organizador */}
      {target && (
        <div
          role="presentation"
          className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-[80] p-4"
          onClick={(e) => { if (e.target === e.currentTarget) closeRevoke(); }}
        >
          <div className="bg-surface rounded-2xl border border-surfaceAlt shadow-2xl shadow-black/60 w-full max-w-md p-6 flex flex-col gap-4">
            <div className="flex items-start justify-between gap-3">
              <h3 className="text-base font-black text-textPrimary leading-snug">
                {t('adminUsers.revokeTitle', { name: target.fullName || target.alias })}
              </h3>
              <button onClick={closeRevoke} className="text-textMuted hover:text-textPrimary">
                <X className="w-5 h-5" />
              </button>
            </div>
            <p className="text-sm text-textSecondary leading-relaxed">{t('adminUsers.revokeMessage')}</p>
            <div>
              <label className="block text-xs font-bold uppercase tracking-wide text-textMuted mb-1.5">
                {t('adminUsers.reasonLabel')}
              </label>
              <textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                maxLength={REASON_MAX}
                rows={4}
                placeholder={t('adminUsers.reasonPlaceholder')}
                className="w-full rounded-xl bg-background border border-surfaceAlt px-3 py-2.5 text-sm text-textPrimary outline-none focus:border-primary resize-none"
              />
              <div className="flex justify-between mt-1 text-[11px]">
                <span className={trimmedReason.length > 0 && trimmedReason.length < REASON_MIN ? 'text-rose-400' : 'text-textMuted'}>
                  {t('adminUsers.reasonMin', { count: REASON_MIN })}
                </span>
                <span className="text-textMuted">{reason.length}/{REASON_MAX}</span>
              </div>
            </div>
            <div className="flex gap-3 pt-1">
              <button
                onClick={closeRevoke}
                disabled={submitting}
                className="flex-1 py-2.5 rounded-full text-sm font-bold bg-surfaceAlt text-textSecondary border border-surfaceAlt hover:bg-surfaceAlt/70 transition-all disabled:opacity-50"
              >
                {t('adminUsers.cancel')}
              </button>
              <button
                onClick={confirmRevoke}
                disabled={!reasonValid || submitting}
                className="flex-1 py-2.5 rounded-full text-sm font-bold flex items-center justify-center gap-2
                  bg-rose-500 text-white hover:brightness-110 transition-all disabled:opacity-50"
              >
                {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
                {t('adminUsers.revokeConfirm')}
              </button>
            </div>
          </div>
        </div>
      )}

      <Toast toast={toast} onHide={hideToast} />
    </div>
  );
}
