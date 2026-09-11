import { useState, useEffect, useCallback } from 'react';
import {
  Eye, LayoutDashboard, CreditCard, Calendar, BarChart2, RefreshCcw,
  Users, LogOut, User, Settings, ChevronDown, X, CheckCircle, XCircle,
  AlertTriangle, Globe, Lock, Loader2, ChevronLeft, ChevronRight,
  UserCircle, Tag, MapPin, Ticket, ZoomIn, Rocket, Clock, Check,
} from 'lucide-react';
import EventImagePlaceholder from '../components/common/EventImagePlaceholder';
import { useNavigate } from 'react-router-dom';
import { performLogout } from '../services/httpClient';
import { getAdminEvents, adminChangeEventStatus, getEventById } from '../services/EventService';
import { getEventMedia } from '../services/EventMediaService';

const PAGE_SIZE = 10;

// ── Helpers de formato ─────────────────────────────────────────────────────────
const fmtDate = (d) =>
  d ? new Date(d + 'T00:00:00').toLocaleDateString('es-CO', {
    day: '2-digit', month: 'short', year: 'numeric',
  }) : '—';

const fmtDateTime = (dt) =>
  dt ? new Date(dt).toLocaleString('es-CO', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  }) : '—';

// ── Status helpers ─────────────────────────────────────────────────────────────
const statusStyle = (status) => {
  if (status === 'DRAFT')      return { bg: 'bg-slate-600',   label: 'Borrador'   };
  if (status === 'PUBLISHED')  return { bg: 'bg-emerald-500', label: 'Activo'     };
  if (status === 'CANCELLED')  return { bg: 'bg-rose-500',    label: 'Cancelado'  };
  if (status === 'COMPLETED')  return { bg: 'bg-violet-600',  label: 'Finalizado' };
  return { bg: 'bg-slate-700', label: status };
};

// Transiciones permitidas según las reglas de negocio del backend:
//   DRAFT      → PUBLISHED
//   PUBLISHED  → CANCELLED | COMPLETED
//   CANCELLED  → (ninguna — estado final)
//   COMPLETED  → (ninguna — estado final)
const allowedTransitions = (status) => {
  if (status === 'DRAFT')     return [{ value: 'PUBLISHED', label: 'Publicar'  }];
  if (status === 'PUBLISHED') return [
    { value: 'CANCELLED', label: 'Cancelar'  },
    { value: 'COMPLETED', label: 'Finalizar' },
  ];
  return [];
};

// ── Tarjeta de campo con ícono — patrón: caja ícono + label + valor ──────────
const InfoCard = ({ icon: Icon, iconBg = 'from-[#7f13ec] to-[#5a189a]', label, children, wide = false }) => (
  <div className={`flex items-start gap-3 bg-white/[0.04] rounded-xl p-3.5 border border-white/[0.06]
      hover:border-white/10 transition-colors ${wide ? 'col-span-2' : ''}`}>
    {/* Caja del ícono */}
    <div className={`w-9 h-9 rounded-lg bg-gradient-to-br ${iconBg} flex items-center justify-center flex-shrink-0 shadow-lg`}>
      <Icon className="w-4 h-4 text-white" />
    </div>
    {/* Label + valor */}
    <div className="min-w-0 flex-1">
      <p className="text-[9px] uppercase tracking-widest text-slate-500 font-bold mb-0.5">{label}</p>
      {children}
    </div>
  </div>
);

// ── Componente principal ───────────────────────────────────────────────────────
export default function AdminEventManagement() {
  const [events,        setEvents]        = useState([]);
  const [loading,       setLoading]       = useState(true);
  const [filter,        setFilter]        = useState('all');
  const [error,         setError]         = useState(null);
  const [page,          setPage]          = useState(1);
  const [showMenu,      setShowMenu]      = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  // Modal de detalle
  const [selected,       setSelected]       = useState(null);   // AdminEventSummaryResponse (tabla)
  const [fullDetail,     setFullDetail]     = useState(null);   // EventResponse (GET /events/{id})
  const [detailLoading,  setDetailLoading]  = useState(false);
  const [showModal,      setShowModal]      = useState(false);
  // Carrusel de imágenes del modal
  const [mediaUrls,      setMediaUrls]      = useState([]);     // string[]
  const [carouselIdx,    setCarouselIdx]    = useState(0);
  // Lightbox de pantalla completa
  const [lightboxOpen,   setLightboxOpen]   = useState(false);
  const [lightboxIdx,    setLightboxIdx]    = useState(0);

  // Modal de confirmación de cancelación
  const [pendingCancel,  setPendingCancel]  = useState(null);   // { eventId, eventName } | null
  const [cancelFromModal, setCancelFromModal] = useState(false); // si se disparó desde el modal de detalle

  const navigate = useNavigate();

  const userData = {
    name: localStorage.getItem('name') || localStorage.getItem('alias') || 'Admin',
    role: 'Super Admin',
  };

  const navItems = [
    { icon: LayoutDashboard, label: 'Dashboard',          href: '#'             },
    { icon: CreditCard,      label: 'Control Financiero', href: '#'             },
    { icon: Calendar,        label: 'Eventos',            href: '/admin-events', active: true },
    { icon: BarChart2,       label: 'Reportes',           href: '#'             },
    { icon: RefreshCcw,      label: 'Reembolsos',         href: '#'             },
    { icon: Users,           label: 'Solicitudes',        href: '/admin-panel'  },
  ];

  const filters = [
    { key: 'all',       label: 'Todos'       },
    { key: 'DRAFT',     label: 'Borradores'  },
    { key: 'PUBLISHED', label: 'Activos'     },
    { key: 'CANCELLED', label: 'Cancelados'  },
    { key: 'COMPLETED', label: 'Finalizados' },
  ];

  // ── Fetch lista ───────────────────────────────────────────────────────────
  const fetchEvents = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const statusParam = filter === 'all' ? null : filter;
      const data = await getAdminEvents(statusParam);
      setEvents(data.data || []);
      setPage(1);
    } catch (err) {
      setError('Error al cargar eventos: ' + err.message);
    } finally {
      setLoading(false);
    }
  }, [filter]);

  useEffect(() => { fetchEvents(); }, [fetchEvents]);

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (showMenu && !e.target.closest('.user-menu-container')) setShowMenu(false);
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [showMenu]);

  // ── Paginación ────────────────────────────────────────────────────────────
  const totalPages  = Math.max(1, Math.ceil(events.length / PAGE_SIZE));
  const pageStart   = (page - 1) * PAGE_SIZE;
  const pageEnd     = Math.min(page * PAGE_SIZE, events.length);
  const pageEvents  = events.slice(pageStart, pageEnd);
  const pageNumbers = Array.from({ length: totalPages }, (_, i) => i + 1)
    .filter(n => n === 1 || n === totalPages || Math.abs(n - page) <= 1);

  // ── Modal de detalle ──────────────────────────────────────────────────────
  const openModal = async (ev) => {
    setSelected(ev);
    setFullDetail(null);
    setMediaUrls([]);
    setCarouselIdx(0);
    setShowModal(true);
    setDetailLoading(true);
    try {
      // Carga EventResponse + lista de imágenes en paralelo
      const [detailRes, mediaRes] = await Promise.allSettled([
        getEventById(ev.eventId),
        getEventMedia(ev.eventId),
      ]);

      if (detailRes.status === 'fulfilled') {
        setFullDetail(detailRes.value?.data ?? null);
      }
      if (mediaRes.status === 'fulfilled') {
        const list = mediaRes.value?.data ?? [];
        // Ordenar por mediaId ascendente (la portada es la primera)
        const urls = list
          .sort((a, b) => a.mediaId - b.mediaId)
          .map((m) => m.imgUrl)
          .filter(Boolean);
        setMediaUrls(urls);
      }
    } catch {
      // fallback silencioso — el modal sigue mostrando datos del summary
    } finally {
      setDetailLoading(false);
    }
  };

  const closeModal = () => {
    setShowModal(false);
    setSelected(null);
    setFullDetail(null);
    setMediaUrls([]);
    setCarouselIdx(0);
    setLightboxOpen(false);
    setLightboxIdx(0);
  };

  // ── Cambio de estado (no-cancelar) ────────────────────────────────────────
  const handleStatusChange = async (eventId, newStatus, eventName) => {
    // Para CANCELLED usamos nuestro propio modal de confirmación
    if (newStatus === 'CANCELLED') {
      setPendingCancel({ eventId, eventName });
      return;
    }

    const labels = { PUBLISHED: 'publicar', COMPLETED: 'finalizar' };
    const confirmed = window.confirm(
      `¿${(labels[newStatus] || newStatus).charAt(0).toUpperCase() + (labels[newStatus] || newStatus).slice(1)} el evento "${eventName}"?`
    );
    if (!confirmed) return;

    await executeStatusChange(eventId, newStatus);
  };

  const executeStatusChange = async (eventId, newStatus) => {
    setActionLoading(true);
    setError(null);
    try {
      await adminChangeEventStatus(eventId, newStatus);
      closeModal();
      setPendingCancel(null);
      setCancelFromModal(false);
      await fetchEvents();
    } catch (err) {
      setError(err.message || 'Error al cambiar el estado del evento');
    } finally {
      setActionLoading(false);
    }
  };

  // ── Flujo de cancelación ──────────────────────────────────────────────────
  // Llamado tanto desde la tabla como desde el modal de detalle
  const requestCancel = (eventId, eventName, fromModal = false) => {
    setCancelFromModal(fromModal);
    setPendingCancel({ eventId, eventName });
  };

  const confirmCancel = () => executeStatusChange(pendingCancel.eventId, 'CANCELLED');

  const dismissCancel = () => {
    setPendingCancel(null);
    setCancelFromModal(false);
  };

  // ── Logout / navegación ───────────────────────────────────────────────────
  const handleLogout = async () => {
    await performLogout();
    window.location.href = '/login';
  };

  const handleMenuClick = (action) => {
    setShowMenu(false);
    if (action === 'profile')  navigate('/profile');
    if (action === 'settings') navigate('/settings');
    if (action === 'logout')   handleLogout();
  };

  // ── Datos combinados para el modal (summary + detail) ────────────────────
  // `selected` viene del summary (tabla); `fullDetail` viene de EventResponse.
  // Si fullDetail cargó, usamos sus campos; si no, usamos el summary como fallback.
  const modalData = selected ? {
    ...selected,
    description:  fullDetail?.description  ?? null,
    isPublic:     fullDetail?.isPublic     ?? selected.isPublic,
    createdAt:    fullDetail?.createdAt    ?? null,
    updatedAt:    fullDetail?.updatedAt    ?? null,
    createdBy:    fullDetail?.createdBy    ?? null,
    // coverUrl viene del summary; EventResponse no lo incluye
    coverUrl:     selected.coverUrl        ?? null,
    // organizerName, organizerEmail y organizerUserId vienen del summary
    organizerName:    selected.organizerName    ?? null,
    organizerEmail:   selected.organizerEmail   ?? null,
    organizerUserId:  selected.organizerUserId  ?? null,
  } : null;

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div className="min-h-screen flex bg-[#12091b] text-white font-sans">

      {/* ── Sidebar ── */}
      <aside className="w-52 flex-shrink-0 flex flex-col justify-between px-3 py-6 bg-[#12091b] border-r border-white/5 h-screen sticky top-0">
        <nav className="flex flex-col gap-0.5">
          {navItems.map((item, idx) => (
            <a
              key={idx}
              href={item.href}
              className={`flex items-center gap-2.5 px-3 py-2.5 rounded-full text-sm font-medium transition-colors
                ${item.active
                  ? 'bg-[#7f13ec] text-white'
                  : 'text-slate-400 hover:text-white hover:bg-white/5'
                }`}
            >
              <item.icon className="w-4 h-4 flex-shrink-0" />
              {item.label}
            </a>
          ))}
        </nav>

        {/* User card */}
        <div className="pt-4 border-t border-white/5 relative user-menu-container flex-shrink-0">
          <button
            onClick={() => setShowMenu(!showMenu)}
            className="w-full flex items-center gap-3 p-2 rounded-xl hover:bg-white/5 transition-all group"
          >
            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-[#7f13ec] to-[#5a189a] flex items-center justify-center text-white font-bold text-sm flex-shrink-0">
              {userData.name.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 text-left min-w-0">
              <p className="text-sm font-semibold text-white truncate">{userData.name}</p>
              <p className="text-xs text-slate-400 truncate">{userData.role}</p>
            </div>
            <ChevronDown className={`w-4 h-4 text-slate-400 transition-transform ${showMenu ? 'rotate-180' : ''}`} />
          </button>

          {showMenu && (
            <div className="absolute bottom-full left-0 right-0 mb-2 mx-2 bg-[#1a0d28] border border-white/10 rounded-xl overflow-hidden shadow-xl z-50 user-menu-container">
              <button onClick={() => handleMenuClick('profile')}
                className="w-full flex items-center gap-3 px-4 py-3 text-sm text-slate-300 hover:bg-white/5 hover:text-white transition-colors">
                <User className="w-4 h-4" /> Perfil
              </button>
              <button onClick={() => handleMenuClick('settings')}
                className="w-full flex items-center gap-3 px-4 py-3 text-sm text-slate-300 hover:bg-white/5 hover:text-white transition-colors">
                <Settings className="w-4 h-4" /> Configuración
              </button>
              <div className="border-t border-white/5">
                <button onClick={() => handleMenuClick('logout')}
                  className="w-full flex items-center gap-3 px-4 py-3 text-sm text-red-400 hover:bg-red-500/10 transition-colors">
                  <LogOut className="w-4 h-4" /> Cerrar sesión
                </button>
              </div>
            </div>
          )}
        </div>
      </aside>

      {/* ── Main content ── */}
      <main className="flex-1 overflow-y-auto bg-[#16091f]">
        <div className="p-8 max-w-6xl mx-auto w-full">

          {/* Page header */}
          <div className="mb-6">
            <h1 className="text-2xl font-black uppercase tracking-wide text-white mb-1">
              Gestión de Eventos
            </h1>
            <p className="text-slate-400 text-sm leading-relaxed max-w-xl">
              Administra y supervisa todos los eventos publicados en la plataforma de manera
              eficiente y en tiempo real.
            </p>
          </div>

          {/* Filter pills */}
          <div className="flex items-center gap-2 mb-6 flex-wrap">
            {filters.map(({ key, label }) => (
              <button
                key={key}
                onClick={() => setFilter(key)}
                className={`px-5 py-1.5 rounded-full text-sm font-semibold transition-all
                  ${filter === key
                    ? 'bg-[#7f13ec] text-white'
                    : 'bg-white/5 text-slate-300 hover:bg-white/10'
                  }`}
              >
                {label}
              </button>
            ))}
          </div>

          {/* Error banner */}
          {error && (
            <div className="mb-4 px-4 py-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-start gap-2">
              <AlertTriangle className="w-4 h-4 flex-shrink-0 mt-0.5" />
              <span>{error}</span>
              <button onClick={() => setError(null)} className="ml-auto text-rose-400/60 hover:text-rose-400">
                <X className="w-4 h-4" />
              </button>
            </div>
          )}

          {/* Table card */}
          <div className="rounded-xl overflow-hidden border border-white/5 bg-[#1a0d28]">
            {loading ? (
              <div className="p-10 text-center text-slate-500 text-sm flex items-center justify-center gap-2">
                <Loader2 className="w-4 h-4 animate-spin" /> Cargando eventos…
              </div>
            ) : events.length === 0 ? (
              <div className="p-10 text-center text-slate-500 text-sm">
                No hay eventos para mostrar
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className="border-b border-white/5">
                      {['Nombre del Evento', 'Organizador', 'Fechas', 'Categoría', 'Boletas', 'Estado', 'Acciones'].map((h, i) => (
                        <th key={i}
                          className={`px-5 py-3.5 text-xs font-bold uppercase tracking-widest text-[#7f13ec]
                            ${i === 5 ? 'text-center' : i === 6 ? 'text-right' : ''}`}
                        >
                          {h}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {pageEvents.map((ev, idx) => {
                      const { bg, label } = statusStyle(ev.status);
                      const transitions   = allowedTransitions(ev.status);
                      return (
                        <tr key={ev.eventId}
                          className={`border-b border-white/5 hover:bg-white/[0.03] transition-colors
                            ${idx % 2 !== 0 ? 'bg-white/[0.02]' : ''}`}
                        >
                          {/* Nombre + avatar del organizador */}
                          <td className="px-5 py-3.5">
                            <div className="flex items-center gap-3">
                              <div className="w-9 h-9 rounded-full bg-slate-700 overflow-hidden flex-shrink-0 border border-white/5">
                                {ev.organizerProfilePicture
                                  ? <img src={ev.organizerProfilePicture} alt={ev.organizerName} className="w-full h-full object-cover" />
                                  : <div className="w-full h-full flex items-center justify-center text-xs font-bold text-slate-400">
                                      {ev.organizerName?.charAt(0)?.toUpperCase() || '?'}
                                    </div>
                                }
                              </div>
                              <div className="min-w-0">
                                <p className="text-sm font-semibold text-white truncate max-w-[180px]">{ev.eventName}</p>
                                <p className="text-xs text-slate-500">#{ev.eventId}</p>
                              </div>
                            </div>
                          </td>

                          {/* Organizador */}
                          <td className="px-5 py-3.5 text-sm text-slate-400">
                            {ev.organizerName || <span className="text-slate-600">—</span>}
                          </td>

                          {/* Fechas */}
                          <td className="px-5 py-3.5 text-sm text-slate-400 whitespace-nowrap">
                            {fmtDate(ev.startDate)}
                            <span className="text-slate-600 mx-1">→</span>
                            {fmtDate(ev.finishDate)}
                          </td>

                          {/* Categoría */}
                          <td className="px-5 py-3.5">
                            {ev.categoryName
                              ? <span className="px-2.5 py-1 bg-slate-700/60 text-slate-300 rounded-full text-xs font-medium">
                                  {ev.categoryName}
                                </span>
                              : <span className="text-slate-600 text-xs">—</span>
                            }
                          </td>

                          {/* Boletas */}
                          <td className="px-5 py-3.5 text-sm text-slate-400 whitespace-nowrap">
                            {ev.availableSeats.toLocaleString('es-CO')}
                          </td>

                          {/* Estado */}
                          <td className="px-5 py-3.5 text-center">
                            <span className={`inline-block px-3 py-1 rounded-full text-xs font-bold text-white ${bg}`}>
                              {label}
                            </span>
                          </td>

                          {/* Acciones */}
                          <td className="px-5 py-3.5 text-right">
                            <div className="flex items-center justify-end gap-1">
                              {/* Ver detalle */}
                              <button
                                onClick={() => openModal(ev)}
                                className="p-1.5 hover:bg-[#7f13ec]/20 rounded-full transition-colors text-[#7f13ec]"
                                title="Ver detalle"
                              >
                                <Eye className="w-4 h-4" />
                              </button>

                              {/* Botones de transición rápida */}
                              {transitions.map(({ value, label: tLabel }) => (
                                <button
                                  key={value}
                                  onClick={() =>
                                    value === 'CANCELLED'
                                      ? requestCancel(ev.eventId, ev.eventName, false)
                                      : handleStatusChange(ev.eventId, value, ev.eventName)
                                  }
                                  disabled={actionLoading}
                                  title={tLabel}
                                  className={`p-1.5 rounded-full transition-colors disabled:opacity-40
                                    ${value === 'PUBLISHED' ? 'hover:bg-emerald-500/20 text-emerald-400' : ''}
                                    ${value === 'CANCELLED' ? 'hover:bg-rose-500/20    text-rose-400'    : ''}
                                    ${value === 'COMPLETED' ? 'hover:bg-violet-500/20  text-violet-400'  : ''}
                                  `}
                                >
                                  {value === 'PUBLISHED' && <CheckCircle className="w-4 h-4" />}
                                  {value === 'CANCELLED' && <XCircle     className="w-4 h-4" />}
                                  {value === 'COMPLETED' && <CheckCircle className="w-4 h-4" />}
                                </button>
                              ))}
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}

            {/* Paginación */}
            {!loading && events.length > 0 && (
              <div className="px-5 py-4 flex flex-col sm:flex-row items-center justify-between gap-3 border-t border-white/5">
                <p className="text-xs text-slate-500">
                  Mostrando {pageStart + 1} a {pageEnd} de {events.length} eventos
                </p>
                <div className="flex items-center gap-1.5">
                  <button
                    onClick={() => setPage(p => Math.max(1, p - 1))}
                    disabled={page === 1}
                    className="h-8 w-8 flex items-center justify-center rounded-full bg-white/5 text-slate-400 text-sm
                      hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-40 transition-colors"
                  >
                    ‹
                  </button>

                  {pageNumbers.map((n, i) => {
                    const prev = pageNumbers[i - 1];
                    return (
                      <div key={n} className="flex items-center gap-1.5">
                        {prev && n - prev > 1 && (
                          <span className="text-slate-600 text-xs px-1">…</span>
                        )}
                        <button
                          onClick={() => setPage(n)}
                          className={`h-8 w-8 flex items-center justify-center rounded-full text-xs font-bold transition-colors
                            ${page === n
                              ? 'bg-[#7f13ec] text-white'
                              : 'bg-white/5 hover:bg-white/10 text-slate-300'
                            }`}
                        >
                          {n}
                        </button>
                      </div>
                    );
                  })}

                  <button
                    onClick={() => setPage(p => Math.min(totalPages, p + 1))}
                    disabled={page === totalPages}
                    className="h-8 px-3 flex items-center justify-center rounded-full bg-white/5
                      hover:bg-white/10 text-slate-300 text-xs transition-colors
                      disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    Siguiente ›
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </main>

      {/* ═══════════════════════════════════════════════════════════════════════
          Modal de detalle completo
      ═══════════════════════════════════════════════════════════════════════ */}
      {showModal && selected && (
        <div
          className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4"
          onClick={(e) => { if (e.target === e.currentTarget) closeModal(); }}
        >
          <div className="bg-[#1a0d28] rounded-2xl border border-white/10 shadow-2xl shadow-black/60 w-full max-w-3xl flex flex-col max-h-[90vh]">

            {/* ── Header: ícono grande + nombre + badges + cerrar ── */}
            <div className="flex items-center gap-4 px-6 pt-5 pb-4 border-b border-white/5 flex-shrink-0">
              {/* Ícono del evento — caja grande rounded-2xl con inicial */}
              <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-[#7f13ec] to-[#3b0764] flex items-center justify-center flex-shrink-0 shadow-lg shadow-[#7f13ec]/30 border border-white/10">
                <span className="text-2xl font-black text-white select-none">
                  {modalData.eventName?.charAt(0).toUpperCase() ?? '?'}
                </span>
              </div>

              {/* Título + ID */}
              <div className="flex-1 min-w-0">
                <h2 className="text-2xl font-black text-white leading-tight truncate">
                  {modalData.eventName}
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">ID #{modalData.eventId}</p>
              </div>

              {/* Badges de estado y visibilidad + cerrar */}
              <div className="flex items-center gap-2 flex-shrink-0">
                {/* Badge estado con ícono */}
                <span className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold text-white
                  ${statusStyle(modalData.status).bg}`}>
                  {modalData.status === 'DRAFT'     && <Clock        className="w-3.5 h-3.5" />}
                  {modalData.status === 'PUBLISHED'  && <CheckCircle  className="w-3.5 h-3.5" />}
                  {modalData.status === 'CANCELLED'  && <XCircle      className="w-3.5 h-3.5" />}
                  {modalData.status === 'COMPLETED'  && <CheckCircle  className="w-3.5 h-3.5" />}
                  {statusStyle(modalData.status).label}
                </span>

                {/* Badge visibilidad con ícono */}
                {modalData.isPublic != null && (
                  <span className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold
                    ${modalData.isPublic
                      ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/25'
                      : 'bg-slate-700/60   text-slate-400   border border-white/10'
                    }`}
                  >
                    {modalData.isPublic ? <Globe className="w-3.5 h-3.5" /> : <Lock className="w-3.5 h-3.5" />}
                    {modalData.isPublic ? 'Público' : 'Privado'}
                  </span>
                )}

                {/* Botón cerrar */}
                <button
                  onClick={closeModal}
                  className="ml-1 p-2 hover:bg-white/10 rounded-full transition-colors text-slate-400 hover:text-white"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>

            {/* ── Cuerpo scrollable ── */}
            <div className="overflow-y-auto flex-1 px-6 py-4 space-y-5">

              {detailLoading && (
                <div className="flex items-center gap-2 text-xs text-slate-500">
                  <Loader2 className="w-3 h-3 animate-spin" /> Cargando información completa…
                </div>
              )}

              {/* ── Galería de imágenes ── */}
              <div>
                {/* Imagen principal grande */}
                <div
                  className={`relative w-full h-64 rounded-xl overflow-hidden bg-[#12091b] border border-white/5
                    ${mediaUrls.length > 0 ? 'cursor-zoom-in group' : ''}`}
                  onClick={() => {
                    if (mediaUrls.length > 0) {
                      setLightboxIdx(carouselIdx);
                      setLightboxOpen(true);
                    }
                  }}
                >
                  {mediaUrls.length > 0 ? (
                    <img
                      key={carouselIdx}
                      src={mediaUrls[carouselIdx]}
                      alt={`${modalData.eventName} — imagen ${carouselIdx + 1}`}
                      className="w-full h-full object-cover object-center transition-opacity duration-200"
                    />
                  ) : (
                    <EventImagePlaceholder size="lg" />
                  )}

                  {mediaUrls.length > 0 && (
                    <div className="absolute inset-x-0 bottom-0 h-20 bg-gradient-to-t from-black/60 to-transparent pointer-events-none" />
                  )}
                  {mediaUrls.length > 0 && (
                    <div className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none">
                      <div className="bg-black/50 backdrop-blur-sm rounded-full p-3">
                        <ZoomIn className="w-6 h-6 text-white" />
                      </div>
                    </div>
                  )}
                  {mediaUrls.length > 1 && (
                    <div className="absolute bottom-3 right-3 bg-black/60 backdrop-blur-sm text-white text-xs font-semibold px-2.5 py-1 rounded-full pointer-events-none">
                      {carouselIdx + 1} / {mediaUrls.length}
                    </div>
                  )}
                </div>

                {/* Tira de miniaturas — solo si hay más de una imagen */}
                {mediaUrls.length > 1 && (
                  <div className="overflow-x-auto overflow-y-visible mt-4 py-1.5 px-0.5">
                    <div className="flex justify-center gap-2 min-w-min mx-auto">
                      {mediaUrls.map((url, i) => (
                      /*
                       * Estructura de dos capas para que el ring no quede tapado:
                       *   - <button> exterior: lleva ring + rounded + transition (sin overflow-hidden)
                       *   - <div> interior:    lleva overflow-hidden + rounded (mismo radio)
                       * Si overflow-hidden y ring estuvieran en el mismo elemento, el ring
                       * (implementado como box-shadow externo) quedaría recortado.
                       */
                      <button
                        key={i}
                        onClick={() => setCarouselIdx(i)}
                        className={`flex-shrink-0 w-16 h-16 rounded-lg transition-all focus:outline-none
                          ${i === carouselIdx
                            ? 'ring-2 ring-[#7f13ec] ring-offset-2 ring-offset-[#1a0d28] opacity-100'
                            : 'opacity-50 hover:opacity-80'
                          }`}
                        aria-label={`Imagen ${i + 1}`}
                      >
                        <div className="w-full h-full rounded-lg overflow-hidden">
                          <img
                            src={url}
                            alt={`Miniatura ${i + 1}`}
                            className="w-full h-full object-cover object-center"
                          />
                        </div>
                      </button>
                    ))}
                    </div>
                  </div>
                )}
              </div>

              {/* ── Descripción ── */}
              {modalData.description && (
                <div className="bg-white/[0.04] rounded-xl p-4 border border-white/[0.06]">
                  <p className="text-[9px] uppercase tracking-widest text-slate-500 font-bold mb-2">Descripción</p>
                  <p className="text-sm text-slate-300 leading-relaxed whitespace-pre-wrap">{modalData.description}</p>
                </div>
              )}

              {/* ── Grid de tarjetas con ícono ── */}
              <div className="grid grid-cols-2 gap-2.5">

                <InfoCard icon={UserCircle} label="Organizador">
                  <p className="text-sm font-bold text-white truncate">{modalData.organizerName || '—'}</p>
                  {modalData.organizerEmail && (
                    <p className="text-xs text-[#a78bfa] truncate mt-0.5">{modalData.organizerEmail}</p>
                  )}
                  {modalData.organizerUserId && (
                    <p className="text-[10px] text-slate-500 mt-0.5">ID: {modalData.organizerUserId}</p>
                  )}
                </InfoCard>

                <InfoCard icon={Tag} iconBg="from-violet-600 to-violet-800" label="Categoría">
                  {modalData.categoryName
                    ? <span className="inline-block px-2 py-0.5 bg-violet-500/15 text-violet-300 rounded-md text-xs font-semibold border border-violet-500/20">
                        {modalData.categoryName}
                      </span>
                    : <p className="text-sm font-bold text-white">—</p>
                  }
                </InfoCard>

                <InfoCard icon={Calendar} iconBg="from-sky-600 to-sky-800" label="Fecha de inicio">
                  <p className="text-sm font-bold text-white">{fmtDate(modalData.startDate)}</p>
                </InfoCard>

                <InfoCard icon={Calendar} iconBg="from-sky-600 to-sky-800" label="Fecha de fin">
                  <p className="text-sm font-bold text-white">{fmtDate(modalData.finishDate)}</p>
                </InfoCard>

                <InfoCard icon={MapPin} iconBg="from-rose-600 to-rose-800" label="Ubicación / Sede" wide>
                  <p className="text-sm font-bold text-white">{modalData.siteName || '—'}</p>
                </InfoCard>

                <InfoCard icon={Ticket} iconBg="from-emerald-600 to-emerald-800" label="Boletas disponibles">
                  <p className="text-sm font-bold text-white">{modalData.availableSeats?.toLocaleString('es-CO') ?? '—'}</p>
                </InfoCard>

                <InfoCard
                  icon={modalData.isPublic ? Globe : Lock}
                  iconBg={modalData.isPublic ? 'from-emerald-600 to-emerald-800' : 'from-slate-600 to-slate-700'}
                  label="Visibilidad"
                >
                  <p className={`text-sm font-bold ${modalData.isPublic ? 'text-emerald-400' : 'text-slate-400'}`}>
                    {modalData.isPublic == null ? '—' : modalData.isPublic ? 'Público' : 'Privado'}
                  </p>
                </InfoCard>

                {modalData.createdAt && (
                  <InfoCard icon={Calendar} iconBg="from-slate-600 to-slate-700" label="Creado el" wide>
                    <p className="text-sm font-bold text-white">{fmtDateTime(modalData.createdAt)}</p>
                    {modalData.createdBy && (
                      <p className="text-[10px] text-slate-500 mt-0.5">por {modalData.createdBy}</p>
                    )}
                  </InfoCard>
                )}

                {modalData.updatedAt && (
                  <InfoCard icon={Calendar} iconBg="from-slate-600 to-slate-700" label="Última actualización" wide>
                    <p className="text-sm font-bold text-white">{fmtDateTime(modalData.updatedAt)}</p>
                  </InfoCard>
                )}
              </div>
            </div>

            {/* ── Footer: botones de acción ── */}
            {allowedTransitions(modalData.status).length > 0 && (
              <div className="px-6 py-4 border-t border-white/5 flex gap-3 flex-shrink-0">
                {allowedTransitions(modalData.status).map(({ value, label: tLabel }) => (
                  <button
                    key={value}
                    onClick={() =>
                      value === 'CANCELLED'
                        ? requestCancel(modalData.eventId, modalData.eventName, true)
                        : handleStatusChange(modalData.eventId, value, modalData.eventName)
                    }
                    disabled={actionLoading}
                    className={`flex-1 py-3 rounded-full text-sm font-bold border transition-all disabled:opacity-50
                      flex items-center justify-center gap-2
                      ${value === 'PUBLISHED'
                        ? 'bg-gradient-to-r from-[#7f13ec] to-[#3b82f6] text-white border-transparent hover:brightness-110 shadow-lg shadow-[#7f13ec]/30'
                        : value === 'CANCELLED'
                          ? 'bg-rose-500/10 text-rose-400 border-rose-500/20 hover:bg-rose-500 hover:text-white hover:border-rose-500'
                          : 'bg-violet-500/10 text-violet-400 border-violet-500/20 hover:bg-violet-500 hover:text-white hover:border-violet-500'
                      }`}
                  >
                    {actionLoading ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      <>
                        {value === 'PUBLISHED' && <Rocket  className="w-4 h-4" />}
                        {value === 'CANCELLED' && <XCircle className="w-4 h-4" />}
                        {value === 'COMPLETED' && <Check   className="w-4 h-4" />}
                        {actionLoading ? 'Procesando…' : tLabel}
                      </>
                    )}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* ═══════════════════════════════════════════════════════════════════════
          Lightbox de pantalla completa
      ═══════════════════════════════════════════════════════════════════════ */}
      {lightboxOpen && mediaUrls.length > 0 && (
        <div
          className="fixed inset-0 bg-black/95 backdrop-blur-md flex items-center justify-center z-[70]"
          onClick={(e) => { if (e.target === e.currentTarget) setLightboxOpen(false); }}
        >
          {/* Imagen centrada */}
          <img
            key={lightboxIdx}
            src={mediaUrls[lightboxIdx]}
            alt={`${modalData?.eventName} — imagen ${lightboxIdx + 1}`}
            className="max-w-[90vw] max-h-[85vh] object-contain rounded-xl shadow-2xl"
          />

          {/* Botón cerrar */}
          <button
            onClick={() => setLightboxOpen(false)}
            className="absolute top-5 right-5 p-2.5 bg-white/10 hover:bg-white/20 rounded-full transition-colors text-white"
            aria-label="Cerrar visor"
          >
            <X className="w-5 h-5" />
          </button>

          {/* Flechas — solo si hay más de una imagen */}
          {mediaUrls.length > 1 && (
            <>
              <button
                onClick={() => setLightboxIdx((i) => (i - 1 + mediaUrls.length) % mediaUrls.length)}
                className="absolute left-5 top-1/2 -translate-y-1/2 w-12 h-12 rounded-full flex items-center justify-center bg-white/10 hover:bg-white/20 transition-colors text-white"
                aria-label="Imagen anterior"
              >
                <ChevronLeft className="w-6 h-6" />
              </button>
              <button
                onClick={() => setLightboxIdx((i) => (i + 1) % mediaUrls.length)}
                className="absolute right-5 top-1/2 -translate-y-1/2 w-12 h-12 rounded-full flex items-center justify-center bg-white/10 hover:bg-white/20 transition-colors text-white"
                aria-label="Imagen siguiente"
              >
                <ChevronRight className="w-6 h-6" />
              </button>
            </>
          )}

          {/* Contador */}
          {mediaUrls.length > 1 && (
            <div className="absolute bottom-6 left-1/2 -translate-x-1/2 bg-white/10 backdrop-blur-sm text-white text-sm font-semibold px-4 py-1.5 rounded-full">
              {lightboxIdx + 1} / {mediaUrls.length}
            </div>
          )}
        </div>
      )}

      {/* ═══════════════════════════════════════════════════════════════════════
          Modal de confirmación de cancelación
          Se muestra encima del modal de detalle cuando cancelFromModal=true,
          o standalone cuando viene desde la tabla.
      ═══════════════════════════════════════════════════════════════════════ */}
      {pendingCancel && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-[60] p-4">
          <div className="bg-[#1a0d28] rounded-2xl border border-white/10 shadow-2xl shadow-black/60 w-full max-w-sm p-6 flex flex-col items-center text-center gap-4">

            {/* Icono de advertencia */}
            <div className="w-14 h-14 rounded-full bg-rose-500/10 border border-rose-500/20 flex items-center justify-center">
              <AlertTriangle className="w-7 h-7 text-rose-400" />
            </div>

            <div>
              <h3 className="text-base font-black text-white mb-1">¿Cancelar este evento?</h3>
              <p className="text-sm text-slate-400 leading-relaxed">
                Estás por cancelar{' '}
                <span className="text-white font-semibold">"{pendingCancel.eventName}"</span>.
                Esta acción no se puede deshacer y dejará de ser visible para los asistentes.
              </p>
            </div>

            <div className="flex gap-3 w-full">
              <button
                onClick={dismissCancel}
                disabled={actionLoading}
                className="flex-1 py-2.5 rounded-full text-sm font-bold bg-white/5 text-slate-300
                  border border-white/10 hover:bg-white/10 transition-all disabled:opacity-50"
              >
                No, volver
              </button>
              <button
                onClick={confirmCancel}
                disabled={actionLoading}
                className="flex-1 py-2.5 rounded-full text-sm font-bold bg-rose-500/10 text-rose-400
                  border border-rose-500/20 hover:bg-rose-500 hover:text-white hover:border-rose-500
                  transition-all disabled:opacity-50"
              >
                {actionLoading ? 'Cancelando…' : 'Sí, cancelar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
