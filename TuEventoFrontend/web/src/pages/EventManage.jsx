import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, LayoutDashboard, Pencil, Trash2, AlertTriangle } from 'lucide-react';
import * as EventService from '../services/EventService';
import * as CategoryService from '../services/CategoryService';
import StatusDropdown from '../components/event-manage/StatusDropdown';
import Modal from '../components/common/Modal';

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
  const [editTarget,       setEditTarget]       = useState(null);  // event summary | null (para saber qué fila actualizar)
  const [editModalOpen,    setEditModalOpen]    = useState(false);
  const [isLoadingEditForm, setIsLoadingEditForm] = useState(false);
  const [editForm,         setEditForm]         = useState({});
  const [categories,       setCategories]       = useState([]);
  const [isSubmitting,     setIsSubmitting]     = useState(false);
  const [editError,        setEditError]        = useState(null);

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

  // ── Editar info general — carga EventResponse completo (tiene description) ──
  const openEdit = async (eventSummary) => {
    setEditTarget(eventSummary);
    setEditModalOpen(true);
    setIsLoadingEditForm(true);
    setEditError(null);
    try {
      const res  = await EventService.getEventById(eventSummary.eventId);
      const full = res.data; // EventResponse — sí incluye description
      setEditForm({
        eventName:      full.eventName      ?? '',
        description:    full.description    ?? '',
        startDate:      full.startDate      ?? '',
        finishDate:     full.finishDate     ?? '',
        isPublic:       full.isPublic       ?? true,
        availableSeats: full.availableSeats ?? '',
        categoryId:     full.categoryId     ?? '',
      });
    } catch (err) {
      setEditError('No se pudo cargar la información completa del evento');
      setEditModalOpen(false);
    } finally {
      setIsLoadingEditForm(false);
    }
  };

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
      // Payload completo — no usar && para no omitir campos vacíos intencionales
      // (p.ej. description borrada a propósito debe llegar al backend como "")
      const payload = {
        eventName:      editForm.eventName,
        description:    editForm.description,
        startDate:      editForm.startDate      || undefined,
        finishDate:     editForm.finishDate     || undefined,
        isPublic:       editForm.isPublic,
        availableSeats: editForm.availableSeats ? Number(editForm.availableSeats) : undefined,
        categoryId:     editForm.categoryId     ? Number(editForm.categoryId)     : undefined,
      };
      const res = await EventService.updateEvent(editTarget.eventId, payload);
      const updated = res.data;
      setEvents((prev) =>
        prev.map((e) => e.eventId === editTarget.eventId ? { ...e, ...updated } : e)
      );
      setEditModalOpen(false);
      setEditTarget(null);
    } catch (err) {
      setEditError(err.message);
    } finally {
      setIsSubmitting(false);
    }
  }, [editTarget, editForm]);

  // El cierre por click fuera lo maneja StatusDropdown internamente vía su propio useEffect.

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

                    {/* Estado — portal dropdown */}
                    <td className="px-4 py-3">
                      <StatusDropdown
                        event={event}
                        badge={badge}
                        busy={busy}
                        isOpen={openStatusMenu === event.eventId}
                        onToggle={setOpenStatusMenu}
                        onSelect={handleStatusChange}
                        statusOptions={STATUS_OPTIONS}
                        statusLabel={STATUS_LABEL}
                      />
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
                        {/* Editar layout — solo disponible en DRAFT */}
                        <button
                          onClick={() => event.status === 'DRAFT' && navigate(`/events/${event.eventId}/layout`)}
                          disabled={event.status !== 'DRAFT'}
                          className={`w-7 h-7 flex items-center justify-center rounded-lg transition-colors
                            ${event.status === 'DRAFT'
                              ? 'text-textMuted hover:text-accent hover:bg-accent/10 cursor-pointer'
                              : 'text-textMuted/30 cursor-not-allowed'}`}
                          title={event.status === 'DRAFT'
                            ? 'Editar layout del evento'
                            : 'El layout solo se puede editar en estado Borrador'}
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

                        {/* Eliminar — solo disponible en DRAFT (backend lo exige también) */}
                        <button
                          onClick={() => event.status === 'DRAFT' && setDeleteTarget(event)}
                          disabled={event.status !== 'DRAFT'}
                          className={`w-7 h-7 flex items-center justify-center rounded-lg transition-colors
                            ${event.status === 'DRAFT'
                              ? 'text-textMuted hover:text-error hover:bg-error/10 cursor-pointer'
                              : 'text-textMuted/30 cursor-not-allowed'}`}
                          title={event.status === 'DRAFT'
                            ? 'Eliminar evento'
                            : 'Solo se pueden eliminar eventos en estado Borrador'}
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
      <Modal
        isOpen={!!deleteTarget}
        onClose={() => !isDeleting && setDeleteTarget(null)}
        title="¿Eliminar este evento?"
        maxWidth="max-w-sm"
        hideClose={isDeleting}
        footer={
          <>
            <button
              onClick={() => setDeleteTarget(null)}
              disabled={isDeleting}
              className="px-4 py-2 rounded-xl text-sm font-medium text-textSecondary bg-surfaceAlt hover:bg-surfaceAlt/80 transition-colors disabled:opacity-60"
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
          </>
        }
      >
        <div className="flex items-start gap-3 px-5 py-4">
          <div className="w-10 h-10 rounded-xl bg-error/15 border border-error/30 flex items-center justify-center flex-shrink-0">
            <Trash2 className="w-5 h-5 text-error" />
          </div>
          <p className="text-sm text-textMuted mt-1">
            <span className="font-medium text-textSecondary">{deleteTarget?.eventName}</span>{' '}
            será eliminado de forma permanente. Esta acción no se puede deshacer.
          </p>
        </div>
      </Modal>

      {/* ── Modal: editar info general ──────────────────────────────────── */}
      <Modal
        isOpen={editModalOpen}
        onClose={() => { if (!isSubmitting) { setEditModalOpen(false); setEditTarget(null); } }}
        title="Editar evento"
        hideClose={isSubmitting}
        footer={
          <>
            <button
              onClick={() => { setEditModalOpen(false); setEditTarget(null); }}
              disabled={isSubmitting}
              className="px-4 py-2 rounded-xl text-sm font-medium text-textSecondary bg-surfaceAlt hover:bg-surfaceAlt/80 transition-colors disabled:opacity-60"
            >
              Cancelar
            </button>
            <button
              onClick={handleEditSubmit}
              disabled={isSubmitting || isLoadingEditForm}
              className="px-4 py-2 rounded-xl text-sm font-medium bg-primary text-white hover:bg-primaryDark transition-colors disabled:opacity-60"
            >
              {isSubmitting ? 'Guardando…' : 'Guardar cambios'}
            </button>
          </>
        }
      >
        <div className="p-5 space-y-4">
          {/* Loading skeleton mientras carga EventResponse completo */}
          {isLoadingEditForm ? (
            <div className="flex flex-col gap-3 animate-pulse">
              {[100, 60, 80].map((w, i) => (
                <div key={i} className="h-9 rounded-lg bg-surfaceAlt" style={{ width: `${w}%` }} />
              ))}
            </div>
          ) : (
            <>
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
            </>
          )}
        </div>
      </Modal>
    </div>
  );
}
