import { useState, useEffect } from 'react';
import { ShieldOff, Calendar, BookOpen, AlertCircle, X, Search, RefreshCw } from 'lucide-react';
import { httpRequest } from '../services/httpClient';
import ConfirmModal from '../components/common/ConfirmModal';
import AdminSidebar from '../components/admin/AdminSidebar';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export default function AdminOrganizerManagement() {
  const [organizers, setOrganizers] = useState([]);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState(null);
  const [search, setSearch]         = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [confirmModal, setConfirmModal]   = useState(null); // { organizer }

  useEffect(() => { fetchOrganizers(); }, []);

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

  const handleRevoke = (organizer) => setConfirmModal({ organizer });

  const executeRevoke = async () => {
    if (!confirmModal) return;
    const { organizer } = confirmModal;
    setActionLoading(true);
    setError(null);
    try {
      const res  = await httpRequest(`${API_URL}/admin/organizer-users/${organizer.userId}/revoke`, { method: 'DELETE' });
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
                Al revocar, el usuario recibirá una notificación in-app y por correo electrónico.
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

      {/* Confirm revoke modal */}
      <ConfirmModal
        isOpen={!!confirmModal}
        title="Revocar rol de organizador"
        message={
          confirmModal
            ? `¿Estás seguro de que deseas revocar el rol de Organizador a ` +
              `${confirmModal.organizer.fullName || confirmModal.organizer.alias}? ` +
              `El usuario pasará a tener rol de Usuario y recibirá una notificación inmediata. ` +
              `Esta acción no se puede deshacer automáticamente.`
            : ''
        }
        confirmLabel="Sí, revocar"
        cancelLabel="Cancelar"
        confirmStyle="danger"
        onConfirm={executeRevoke}
        onCancel={() => setConfirmModal(null)}
        loading={actionLoading}
      />
    </div>
  );
}
