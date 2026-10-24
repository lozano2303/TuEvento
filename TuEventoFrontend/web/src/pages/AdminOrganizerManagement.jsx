import { useState, useEffect } from 'react';
import { ShieldOff, Calendar, BookOpen, AlertCircle, X, Search, RefreshCw, Loader2 } from 'lucide-react';
import { httpRequest } from '../services/httpClient';
import AdminSidebar from '../components/admin/AdminSidebar';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';
const REASON_MIN = 10;
const REASON_MAX = 500;

export default function AdminOrganizerManagement() {
  const [organizers, setOrganizers] = useState([]);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState(null);
  const [search, setSearch]         = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [confirmModal, setConfirmModal]   = useState(null); // { organizer }
  const [reason, setReason]               = useState('');

  useEffect(() => { void fetchOrganizers(); }, []);

  const fetchOrganizers = async () => {
    setLoading(true);
    setError(null);
    try {
      const res  = await httpRequest(`${API_URL}/admin/organizer-users`, { method: 'GET' });
      const data = await res.json();
      if (res.ok) {
        setOrganizers(data.data || []);
      } else {
        setError(data.message || 'Error al cargar organizadores');
      }
    } catch (err) {
      setError('Error de conexión: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleRevoke = (organizer) => {
    setReason('');
    setConfirmModal({ organizer });
  };

  const trimmedReason = reason.trim();
  const reasonValid = trimmedReason.length >= REASON_MIN && trimmedReason.length <= REASON_MAX;

  const executeRevoke = async () => {
    if (!confirmModal || !reasonValid) return;
    const { organizer } = confirmModal;
    setActionLoading(true);
    setError(null);
    try {
      // El motivo es obligatorio: se notifica al usuario y queda en el historial de revocaciones
      const res  = await httpRequest(`${API_URL}/admin/organizer-users/${organizer.userId}/revoke`, {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason: trimmedReason }),
      });
      const data = await res.json();
      if (res.ok) {
        setConfirmModal(null);
        await fetchOrganizers();
      } else {
        setError(data.message || 'Error al revocar el rol');
        setConfirmModal(null);
      }
    } catch (err) {
      setError('Error al revocar: ' + err.message);
      setConfirmModal(null);
    } finally {
      setActionLoading(false);
    }
  };

  const filtered = organizers.filter(o => {
    const q = search.toLowerCase();
    return (
      o.fullName?.toLowerCase().includes(q) ||
      o.alias?.toLowerCase().includes(q) ||
      o.email?.toLowerCase().includes(q)
    );
  });

  return (
    <div className="min-h-screen flex bg-background text-textPrimary font-sans">
      <AdminSidebar activePage="organizer-management" />

      <main className="flex-1 overflow-y-auto bg-background">
        <div className="p-8 max-w-6xl mx-auto w-full">

          {/* Header */}
          <div className="mb-6 flex items-start justify-between gap-4">
            <div>
              <h1 className="text-2xl font-black uppercase tracking-wide text-textPrimary mb-1">
                Gestión de Organizadores
              </h1>
              <p className="text-textSecondary text-sm leading-relaxed max-w-xl">
                Visualiza todos los usuarios con rol de Organizador y revoca sus permisos si es necesario.
                Al revocar, el usuario recibirá el motivo por notificación in-app y por correo electrónico,
                sus eventos publicados se cancelarán (con reembolso a los compradores) y los que estén en revisión se rechazarán.
              </p>
            </div>
            <button
              onClick={fetchOrganizers}
              disabled={loading}
              className="flex items-center gap-2 px-4 py-2.5 bg-surfaceAlt hover:bg-surfaceAlt/80 text-textSecondary rounded-xl transition-all text-sm"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              Actualizar
            </button>
          </div>

          {/* Search */}
          <div className="relative mb-6">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-textMuted" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Buscar por nombre, alias o correo..."
              className="w-full pl-10 pr-4 py-2.5 bg-surface border border-surfaceAlt rounded-xl text-textPrimary placeholder-textMuted focus:border-primary focus:outline-none text-sm"
            />
          </div>

          {/* Error */}
          {error && (
            <div className="mb-6 p-4 bg-error/10 border border-error/30 text-error rounded-xl flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span className="text-sm">{error}</span>
              <button onClick={() => setError(null)} className="ml-auto p-1 hover:bg-error/20 rounded-full">
                <X className="w-3 h-3" />
              </button>
            </div>
          )}

          {/* Stats pill */}
          {!loading && (
            <div className="mb-4 flex items-center gap-2">
              <span className="px-3 py-1 rounded-full bg-primary/20 text-primary text-xs font-semibold">
                {filtered.length} organizador{filtered.length !== 1 ? 'es' : ''}
                {search ? ' encontrados' : ' en total'}
              </span>
            </div>
          )}

          {/* Table */}
          <div className="rounded-xl overflow-hidden border border-surfaceAlt bg-surface">
            {loading ? (
              <div className="p-10 text-center text-textMuted text-sm">Cargando...</div>
            ) : filtered.length === 0 ? (
              <div className="p-10 text-center text-textMuted text-sm">
                {search ? 'No se encontraron organizadores con ese criterio.' : 'No hay organizadores registrados.'}
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className="border-b border-surfaceAlt">
                      {['Organizador', 'Correo', 'Eventos', 'Publicados', 'Miembro desde', 'Acciones'].map((h, i) => (
                        <th
                          key={i}
                          className={`px-5 py-3.5 text-xs font-bold uppercase tracking-widest text-primary
                            ${i === 5 ? 'text-right' : ''}`}
                        >
                          {h}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {filtered.map((org, idx) => (
                      <tr
                        key={org.userId}
                        className={`border-b border-surfaceAlt hover:bg-surfaceAlt/20 transition-colors
                          ${idx % 2 !== 0 ? 'bg-white/[0.02]' : ''}`}
                      >
                        {/* Organizer info */}
                        <td className="px-5 py-3.5">
                          <div className="flex items-center gap-3">
                            <div className="w-9 h-9 rounded-full bg-surfaceAlt overflow-hidden flex-shrink-0">
                              {org.profilePicture ? (
                                <img src={org.profilePicture} alt={org.fullName} className="w-full h-full object-cover" />
                              ) : (
                                <div className="w-full h-full flex items-center justify-center text-xs font-bold text-textSecondary">
                                  {org.fullName?.charAt(0)?.toUpperCase() || '?'}
                                </div>
                              )}
                            </div>
                            <div>
                              <p className="text-sm font-semibold text-textPrimary leading-tight">{org.fullName || '—'}</p>
                              <p className="text-xs text-textMuted">@{org.alias}</p>
                            </div>
                          </div>
                        </td>

                        {/* Email */}
                        <td className="px-5 py-3.5 text-sm text-textSecondary">{org.email || '—'}</td>

                        {/* Total events */}
                        <td className="px-5 py-3.5">
                          <div className="flex items-center gap-1.5 text-sm text-textSecondary">
                            <BookOpen className="w-3.5 h-3.5 text-textMuted" />
                            {org.totalEvents ?? 0}
                          </div>
                        </td>

                        {/* Published events */}
                        <td className="px-5 py-3.5">
                          <div className="flex items-center gap-1.5 text-sm text-emerald-400">
                            <Calendar className="w-3.5 h-3.5" />
                            {org.publishedEvents ?? 0}
                          </div>
                        </td>

                        {/* Member since */}
                        <td className="px-5 py-3.5 text-sm text-textSecondary">
                          {org.memberSince
                            ? new Date(org.memberSince).toLocaleDateString('es-CO')
                            : '—'}
                        </td>

                        {/* Actions */}
                        <td className="px-5 py-3.5 text-right">
                          <button
                            onClick={() => handleRevoke(org)}
                            disabled={actionLoading}
                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg
                              bg-rose-500/10 hover:bg-rose-500/25 text-rose-400 text-xs font-semibold
                              transition-all border border-rose-500/20 hover:border-rose-500/40
                              disabled:opacity-50 disabled:cursor-not-allowed"
                            title="Revocar rol de organizador"
                          >
                            <ShieldOff className="w-3.5 h-3.5" />
                            Revocar
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {/* Footer */}
            {!loading && filtered.length > 0 && (
              <div className="px-5 py-4 border-t border-surfaceAlt">
                <p className="text-xs text-textMuted">
                  Mostrando {filtered.length} de {organizers.length} organizadores
                </p>
              </div>
            )}
          </div>
        </div>
      </main>

      {/* Modal de revocación: motivo obligatorio */}
      {confirmModal && (
        <div
          className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-[80] p-4"
          onClick={(e) => { if (e.target === e.currentTarget && !actionLoading) setConfirmModal(null); }}
        >
          <div className="bg-surface rounded-2xl border border-surfaceAlt shadow-2xl shadow-black/60 w-full max-w-md p-6 flex flex-col gap-4">
            <div className="flex items-start justify-between gap-3">
              <h3 className="text-base font-black text-textPrimary leading-snug">
                ¿Revocar el rol de Organizador a {confirmModal.organizer.fullName || confirmModal.organizer.alias}?
              </h3>
              <button onClick={() => !actionLoading && setConfirmModal(null)} className="text-textMuted hover:text-textPrimary">
                <X className="w-5 h-5" />
              </button>
            </div>
            <p className="text-sm text-textSecondary leading-relaxed">
              El usuario pasará a tener rol de Usuario y ya no podrá crear ni gestionar eventos.
              Sus eventos publicados se cancelarán (los compradores recibirán el reembolso en su cartera)
              y los que estén en revisión se rechazarán. Esta acción no se puede deshacer automáticamente.
            </p>
            <div>
              <label className="block text-xs font-bold uppercase tracking-wide text-textMuted mb-1.5">Motivo</label>
              <textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                maxLength={REASON_MAX}
                rows={4}
                placeholder="Explica al usuario por qué se le quita el rol..."
                className="w-full rounded-xl bg-background border border-surfaceAlt px-3 py-2.5 text-sm text-textPrimary outline-none focus:border-primary resize-none"
              />
              <div className="flex justify-between mt-1 text-[11px]">
                <span className={trimmedReason.length > 0 && trimmedReason.length < REASON_MIN ? 'text-rose-400' : 'text-textMuted'}>
                  Mínimo {REASON_MIN} caracteres
                </span>
                <span className="text-textMuted">{reason.length}/{REASON_MAX}</span>
              </div>
            </div>
            <div className="flex gap-3 pt-1">
              <button
                onClick={() => setConfirmModal(null)}
                disabled={actionLoading}
                className="flex-1 py-2.5 rounded-full text-sm font-bold bg-surfaceAlt text-textSecondary border border-surfaceAlt hover:bg-surfaceAlt/70 transition-all disabled:opacity-50"
              >
                Cancelar
              </button>
              <button
                onClick={executeRevoke}
                disabled={!reasonValid || actionLoading}
                className="flex-1 py-2.5 rounded-full text-sm font-bold flex items-center justify-center gap-2 bg-rose-500 text-white hover:brightness-110 transition-all disabled:opacity-50"
              >
                {actionLoading && <Loader2 className="w-4 h-4 animate-spin" />}
                Sí, revocar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
