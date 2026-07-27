import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, LayoutDashboard, Pencil, Trash2, ChevronDown, X, AlertTriangle } from 'lucide-react';
import * as EventService from '../services/EventService';
import * as CategoryService from '../services/CategoryService';

// ── Constantes ────────────────────────────────────────────────────────────────
const STATUS_OPTIONS = ['DRAFT', 'PUBLISHED', 'CANCELLED', 'COMPLETED'];

const STATUS_BADGE = {
  DRAFT:     { label: 'Borrador',   cls: 'bg-gray-500/20 text-gray-400 border-gray-500/30' },
  PUBLISHED: { label: 'Publicado',  cls: 'bg-green-500/20 text-green-400 border-green-500/30' },
  CANCELLED: { label: 'Cancelado',  cls: 'bg-red-500/20 text-red-400 border-red-500/30' },
  COMPLETED: { label: 'Finalizado', cls: 'bg-blue-500/20 text-blue-400 border-blue-500/30' },
};

const STATUS_LABEL = {
  DRAFT:     'Borrador',
  PUBLISHED: 'Publicar',
  CANCELLED: 'Cancelar',
  COMPLETED: 'Marcar finalizado',
};

const inputClass =
  'w-full bg-background border border-surfaceAlt rounded-lg px-3 py-2 text-sm text-textPrimary ' +
  'focus:outline-none focus:border-accent transition-colors';
const labelClass = 'block text-xs font-semibold text-textMuted uppercase tracking-wider mb-1';

// ── Componente principal ──────────────────────────────────────────────────────
export default function EventManage() {
  const navigate  = useNavigate();
  const userId    = localStorage.getItem('userID');

  const [events,     setEvents]     = useState([]);
  const [isLoading,  setIsLoading]  = useState(true);
  const [error,      setError]      = useState(null);

  // Estado dropdown de cambio de estado por fila
  const [openStatusMenu, setOpenStatusMenu] = useState(null); // eventId | null
  const [statusBusy,     setStatusBusy]     = useState({});   // { [eventId]: bool }

  // Modal de confirmación de eliminación
  const [deleteTarget, setDeleteTarget] = useState(null); // event | null
  const [isDeleting,   setIsDeleting]   = useState(false);

  // Modal de edición de info general
  const [editTarget,  setEditTarget]  = useState(null); // event | null
  const [editForm,    setEditForm]    = useState({});
  const [categories,  setCategories]  = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [editError,   setEditError]   = useState(null);

  // ── Carga inicial ────────────────────────────────────────────────────────
  useEffect(() => {
    if (!userId) { setError('No hay sesión activa'); setIsLoading(false); return; }
    EventService.getEventsByUser(userId)
      .then((res) => setEvents(res.data ?? []))
      .catch((err) => setError(err.message))
      .finally(() => setIsLoading(false));
  }, [userId]);

  // Cargar categorías una sola vez para el modal de edición
  useEffect(() => {
    CategoryService.getActiveCategories?.()
      .then((res) => setCategories(res.data ?? []))
      .catch(() => setCategories([]));
  }, []);

  // ── Cambio de estado ─────────────────────────────────────────────────────
  const handleStatusChange = useCallback(async (event, newStatus) => {
    setOpenStatusMenu(null);
    if (event.status === newStatus) return;
    setStatusBusy((prev) => ({ ...prev, [event.eventId]: true }));
    try {
      await EventService.changeEventStatus(event.eventId, newStatus);
      setEvents((prev) =>
        prev.map((e) => e.eventId === event.eventId ? { ...e, status: newStatus } : e)
      );
    } catch (err) {
      alert(`Error al cambiar estado: ${err.message}`);
    } finally {
      setStatusBusy((prev) => ({ ...prev, [event.eventId]: false }));
    }
  }, []);

  // ── Eliminar ─────────────────────────────────────────────────────────────
  const handleDeleteConfirm = useCallback(async () => {
    if (!deleteTarget) return;
    setIsDeleting(true);
    try {
      await EventService.deleteEvent(deleteTarget.eventId);
      setEvents((prev) => prev.filter((e) => e.eventId !== deleteTarget.eventId));
      setDeleteTarget(null);
    } catch (err) {
      alert(`Error al eliminar: ${err.message}`);
    } finally {
      setIsDeleting(false);
    }
  }, [deleteTarget]);

  // ── Editar info general ───────────────────────────────────────────────────
  const openEdit = useCallback((event) => {
    setEditTarget(event);
    setEditForm({
      eventName:      event.eventName ?? '',
      description:    event.description ?? '',
      startDate:      event.startDate ?? '',
      finishDate:     event.finishDate ?? '',
      isPublic:       event.isPublic ?? true,
      availableSeats: event.availableSeats ?? '',
      categoryId:     event.categoryId ?? '',
    });
    setEditError(null);
  }, []);

  const handleEditSubmit = useCallback(async () => {
    if (!editTarget) return;
    // Validación: finishDate posterior a startDate
    if (editForm.startDate && editForm.finishDate && editForm.finishDate <= editForm.startDate) {
      setEditError('La fecha de fin debe ser posterior a la fecha de inicio');
      return;
    }
    setIsSubmitting(true);
    setEditError(null);
    try {
      const payload = {
        ...(editForm.eventName      && { eventName:      editForm.eventName }),
        ...(editForm.description    && { description:    editForm.description }),
        ...(editForm.startDate      && { startDate:      editForm.startDate }),
        ...(editForm.finishDate     && { finishDate:     editForm.finishDate }),
        ...(editForm.availableSeats && { availableSeats: Number(editForm.availableSeats) }),
        ...(editForm.categoryId     && { categoryId:     Number(editForm.categoryId) }),
        isPublic: editForm.isPublic,
      };
      const res = await EventService.updateEvent(editTarget.eventId, payload);
      const updated = res.data;
      setEvents((prev) =>
        prev.map((e) => e.eventId === editTarget.eventId ? { ...e, ...updated } : e)
      );
      setEditTarget(null);
    } catch (err) {
      setEditError(err.message);
    } finally {
      setIsSubmitting(false);
    }
  }, [editTarget, editForm]);

  // ── Cerrar dropdowns al hacer click fuera ────────────────────────────────
  useEffect(() => {
    const handler = () => setOpenStatusMenu(null);
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  // ── Render ───────────────────────────────────────────────────────────────
  const btnBase = 'flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors';

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">

      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-textPrimary">Mis Eventos</h1>
          <p className="text-sm text-textMuted mt-0.5">Gestiona y edita tus eventos</p>
        </div>
        <button
          onClick={() => navigate('/events/create')}
          className={`${btnBase} bg-primary text-white hover:bg-primaryDark`}
        >
          <Plus className="w-4 h-4" /> Crear nuevo evento
        </button>
      </div>

      {/* Loading */}
      {isLoading && (
        <div className="flex items-center justify-center py-24 text-textMuted text-sm">
          Cargando eventos…
        </div>
      )}

      {/* Error */}
      {!isLoading && error && (
        <div className="flex items-center gap-3 bg-error/10 border border-error/30 text-error rounded-xl px-4 py-3 text-sm">
          <AlertTriangle className="w-4 h-4 flex-shrink-0" />
          {error}
        </div>
      )}

      {/* Vacío */}
      {!isLoading && !error && events.length === 0 && (
        <div className="flex flex-col items-center justify-center py-24 gap-4 text-center">
          <div className="w-16 h-16 rounded-2xl bg-surfaceAlt flex items-center justify-center">
            <LayoutDashboard className="w-8 h-8 text-textMuted" />
          </div>
          <div>
            <p className="text-textPrimary font-semibold text-lg">Aún no tienes eventos</p>
            <p className="text-textMuted text-sm mt-1">Crea tu primer evento para empezar</p>
          </div>
          <button
            onClick={() => navigate('/events/create')}
            className={`${btnBase} bg-primary text-white hover:bg-primaryDark px-5 py-2.5 text-sm`}
          >
            <Plus className="w-4 h-4" /> Crear primer evento
          </button>
        </div>
      )}

      {/* Tabla */}
      {!isLoading && !error && events.length > 0 && (
        <div className="bg-surface border border-surfaceAlt rounded-2xl overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-surfaceAlt bg-surfaceAlt/50">
                <th className="px-4 py-3 text-left text-xs font-semibold text-textMuted uppercase tracking-wider">Evento</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-textMuted uppercase tracking-wider">Estado</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-textMuted uppercase tracking-wider hidden md:table-cell">Fechas</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-textMuted uppercase tracking-wider hidden lg:table-cell">Sillas</th>
                <th className="px-4 py-3 text-right text-xs font-semibold text-textMuted uppercase tracking-wider">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {events.map((event, idx) => {
                const badge  = STATUS_BADGE[event.status] ?? STATUS_BADGE.DRAFT;
                const busy   = statusBusy[event.eventId] ?? false;
                return (
                  <tr
                    key={event.eventId}
                    className={`border-b border-surfaceAlt last:border-0 ${idx % 2 === 0 ? '' : 'bg-surfaceAlt/20'}`}
                  >
                    {/* Nombre */}
                    <td className="px-4 py-3">
                      <p className="font-medium text-textPrimary truncate max-w-[200px]">
                        {event.eventName}
                      </p>
                      <p className="text-[11px] text-textMuted mt-0.5">#{event.eventId}</p>
                    </td>

                    {/* Estado — dropdown */}
                    <td className="px-4 py-3">
                      <div className="relative inline-block" onMouseDown={(e) => e.stopPropagation()}>
                        <button
                          disabled={busy}
                          onClick={() => setOpenStatusMenu(openStatusMenu === event.eventId ? null : event.eventId)}
                          className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold border transition-colors
                                      ${badge.cls} ${busy ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer hover:opacity-80'}`}
                        >
                          {badge.label}
                          {!busy && <ChevronDown className="w-3 h-3" />}
                          {busy && <span className="w-3 h-3 inline-block animate-spin border border-current border-t-transparent rounded-full" />}
                        </button>

                        {openStatusMenu === event.eventId && (
                          <div className="absolute left-0 top-full mt-1 z-20 bg-surface border border-surfaceAlt rounded-xl shadow-2xl shadow-primary/20 overflow-hidden min-w-[160px]">
                            {STATUS_OPTIONS.map((s) => (
                              <button
                                key={s}
                                onClick={() => handleStatusChange(event, s)}
                                className={`w-full text-left px-3 py-2 text-xs transition-colors hover:bg-surfaceAlt
                                            ${event.status === s ? 'font-bold text-accent' : 'text-textSecondary'}`}
                              >
                                {STATUS_LABEL[s] ?? s}
                              </button>
                            ))}
                          </div>
                        )}
                      </div>
                    </td>

                    {/* Fechas */}
                    <td className="px-4 py-3 hidden md:table-cell text-textSecondary text-xs">
                      {event.startDate
                        ? `${event.startDate} → ${event.finishDate ?? '—'}`
                        : '—'}
                    </td>

                    {/* Sillas */}
                    <td className="px-4 py-3 hidden lg:table-cell text-textSecondary text-xs">
                      {event.availableSeats ?? '—'}
                    </td>

                    {/* Acciones */}
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        {/* Editar layout */}
                        <button
                          onClick={() => navigate(`/layout-editor-demo?eventId=${event.eventId}`)}
                          className="w-7 h-7 flex items-center justify-center rounded-lg text-textMuted hover:text-accent hover:bg-accent/10 transition-colors"
                          title="Editar layout del evento"
                        >
                          <LayoutDashboard className="w-3.5 h-3.5" />
                        </button>

                        {/* Editar info */}
                        <button
                          onClick={() => openEdit(event)}
                          className="w-7 h-7 flex items-center justify-center rounded-lg text-textMuted hover:text-primary hover:bg-primary/10 transition-colors"
                          title="Editar información del evento"
                        >
                          <Pencil className="w-3.5 h-3.5" />
                        </button>

                        {/* Eliminar */}
                        <button
                          onClick={() => setDeleteTarget(event)}
                          className="w-7 h-7 flex items-center justify-center rounded-lg text-textMuted hover:text-error hover:bg-error/10 transition-colors"
                          title="Eliminar evento"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* ── Modal: confirmar eliminación ────────────────────────────────── */}
      {deleteTarget && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
          <div className="bg-surface border border-surfaceAlt rounded-2xl w-full max-w-sm shadow-2xl p-6 flex flex-col gap-4">
            <div className="flex items-start gap-3">
              <div className="w-10 h-10 rounded-xl bg-error/15 border border-error/30 flex items-center justify-center flex-shrink-0">
                <Trash2 className="w-5 h-5 text-error" />
              </div>
              <div>
                <p className="font-semibold text-textPrimary">¿Eliminar este evento?</p>
                <p className="text-sm text-textMuted mt-0.5">
                  <span className="font-medium text-textSecondary">{deleteTarget.eventName}</span> será eliminado de forma permanente. Esta acción no se puede deshacer.
                </p>
              </div>
            </div>
            <div className="flex gap-2 justify-end">
              <button
                onClick={() => setDeleteTarget(null)}
                disabled={isDeleting}
                className="px-4 py-2 rounded-xl text-sm font-medium text-textSecondary bg-surfaceAlt hover:bg-surfaceAlt/80 transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={handleDeleteConfirm}
                disabled={isDeleting}
                className="px-4 py-2 rounded-xl text-sm font-medium bg-error text-white hover:bg-red-700 transition-colors disabled:opacity-60"
              >
                {isDeleting ? 'Eliminando…' : 'Sí, eliminar'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Modal: editar info general ──────────────────────────────────── */}
      {editTarget && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
          <div className="bg-surface border border-surfaceAlt rounded-2xl w-full max-w-lg shadow-2xl flex flex-col max-h-[90vh]">
            {/* Header */}
            <div className="flex items-center justify-between px-5 py-4 border-b border-surfaceAlt">
              <h3 className="text-sm font-bold text-textPrimary">Editar evento</h3>
              <button
                onClick={() => setEditTarget(null)}
                className="text-textMuted hover:text-textPrimary transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Body */}
            <div className="flex-1 overflow-y-auto p-5 space-y-4">
              {/* Nombre */}
              <div>
                <label className={labelClass}>Nombre del evento</label>
                <input
                  type="text" className={inputClass}
                  value={editForm.eventName}
                  onChange={(e) => setEditForm((f) => ({ ...f, eventName: e.target.value }))}
                  maxLength={100}
                />
              </div>

              {/* Descripción */}
              <div>
                <label className={labelClass}>Descripción</label>
                <textarea
                  className={`${inputClass} resize-none`}
                  rows={3}
                  value={editForm.description}
                  onChange={(e) => setEditForm((f) => ({ ...f, description: e.target.value }))}
                  maxLength={255}
                />
              </div>

              {/* Fechas */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className={labelClass}>Fecha inicio</label>
                  <input
                    type="date" className={inputClass}
                    value={editForm.startDate}
                    onChange={(e) => setEditForm((f) => ({ ...f, startDate: e.target.value }))}
                  />
                </div>
                <div>
                  <label className={labelClass}>Fecha fin</label>
                  <input
                    type="date" className={inputClass}
                    value={editForm.finishDate}
                    onChange={(e) => setEditForm((f) => ({ ...f, finishDate: e.target.value }))}
                  />
                </div>
              </div>

              {/* Sillas y categoría */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className={labelClass}>Sillas disponibles</label>
                  <input
                    type="number" className={inputClass} min={1} max={100000}
                    value={editForm.availableSeats}
                    onChange={(e) => setEditForm((f) => ({ ...f, availableSeats: e.target.value }))}
                  />
                </div>
                <div>
                  <label className={labelClass}>Categoría</label>
                  <select
                    className={inputClass}
                    value={editForm.categoryId}
                    onChange={(e) => setEditForm((f) => ({ ...f, categoryId: e.target.value }))}
                  >
                    <option value="">Sin cambiar</option>
                    {categories.map((c) => (
                      <option key={c.categoryId} value={c.categoryId}>{c.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Visibilidad */}
              <div className="flex items-center gap-3">
                <input
                  type="checkbox" id="isPublic"
                  className="w-4 h-4 accent-accent"
                  checked={editForm.isPublic}
                  onChange={(e) => setEditForm((f) => ({ ...f, isPublic: e.target.checked }))}
                />
                <label htmlFor="isPublic" className="text-sm text-textSecondary cursor-pointer">
                  Evento público (visible para todos)
                </label>
              </div>

              {/* Error */}
              {editError && (
                <div className="flex items-center gap-2 bg-error/10 border border-error/30 text-error rounded-xl px-3 py-2 text-xs">
                  <AlertTriangle className="w-3.5 h-3.5 flex-shrink-0" />
                  {editError}
                </div>
              )}
            </div>

            {/* Footer */}
            <div className="px-5 py-4 border-t border-surfaceAlt flex justify-end gap-2">
              <button
                onClick={() => setEditTarget(null)}
                disabled={isSubmitting}
                className="px-4 py-2 rounded-xl text-sm font-medium text-textSecondary bg-surfaceAlt hover:bg-surfaceAlt/80 transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={handleEditSubmit}
                disabled={isSubmitting}
                className="px-4 py-2 rounded-xl text-sm font-medium bg-primary text-white hover:bg-primaryDark transition-colors disabled:opacity-60"
              >
                {isSubmitting ? 'Guardando…' : 'Guardar cambios'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
