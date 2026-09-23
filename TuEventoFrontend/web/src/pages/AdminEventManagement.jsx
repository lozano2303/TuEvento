import { useState, useEffect, useCallback } from 'react';
import {
  Eye, LayoutDashboard, CreditCard, Calendar, BarChart2, RefreshCcw,
  Users, LogOut, User, Settings, ChevronDown, X, CheckCircle, XCircle,
  AlertTriangle, Globe, Lock, Loader2, ChevronLeft, ChevronRight,
  UserCircle, Tag, MapPin, Ticket, ZoomIn, Clock, Check, Send,
  FileText, Shield,
} from 'lucide-react';
import EventImagePlaceholder from '../components/common/EventImagePlaceholder';
import ConfirmModal from '../components/common/ConfirmModal';
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

// Extrae solo la hora de una fecha ISO o de un campo date (YYYY-MM-DD → sin hora → '—')
const fmtTime = (d) => {
  if (!d) return '—';
  // Si incluye 'T' es ISO con hora; si no, es solo fecha → sin hora conocida
  if (!d.includes('T')) return '—';
  return new Date(d).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
};

// ── Status helpers ─────────────────────────────────────────────────────────────
// Estilo "outline" desaturado — fondo muy sutil + borde + texto, sin pill sólido.
// Valores hardcodeados aquí porque este componente vive fuera del sistema de temas
// (AdminPanel usa su propio fondo oscuro fijo #12091b/#1a0d28).
const statusStyle = (status) => {
  if (status === 'DRAFT')     return {
    label: 'Borrador',
    cls: 'bg-slate-500/10 text-slate-400 border border-slate-500/30',
  };
  if (status === 'PUBLISHED') return {
    label: 'Activo',
    cls: 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30',
  };
  if (status === 'CANCELLED') return {
    label: 'Cancelado',
    cls: 'bg-rose-500/10 text-rose-400 border border-rose-500/30',
  };
  if (status === 'COMPLETED') return {
    label: 'Finalizado',
    cls: 'bg-primary/10 text-primary border border-primary/30',
  };
  return { label: status, cls: 'bg-slate-700/30 text-slate-500 border border-white/10' };
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

// ── Tarjeta de campo con ícono ────────────────────────────────────────────────
// iconBg: clase bg-* con opacidad ~15-18% del color semántico — visible pero no sólido.
// iconColor: clase text-* del color semántico.
// wide: ocupa las 2 columnas del grid.
const InfoCard = ({
  icon: Icon,
  iconColor = 'text-textMuted',
  iconBg    = 'bg-violet-900/30',
  label,
  children,
  wide = false,
}) => (
  <div className={`admin-info-card flex items-start gap-3 rounded-xl p-3.5 border border-white/[0.07]
      hover:border-white/[0.14] ${wide ? 'col-span-2' : ''}`}
    style={{ background: 'rgba(255,255,255,0.04)' }}
  >
    {/* Ícono — caja cuadrada con fondo de color bajo-saturado, claramente visible */}
    <div className={`w-8 h-8 rounded-lg ${iconBg} flex items-center justify-center flex-shrink-0`}>
      <Icon className={`w-[15px] h-[15px] ${iconColor}`} />
    </div>
    {/* Label + valor */}
    <div className="min-w-0 flex-1">
      <p className="text-[10px] uppercase tracking-[0.10em] text-textMuted font-bold mb-1 leading-none">
        {label}
      </p>
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

  // Modal de confirmación genérico (para PUBLISHED, COMPLETED — no-destructivos)
  const [pendingConfirm, setPendingConfirm] = useState(null);   // { eventId, newStatus, eventName } | null

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
    // Abre el ConfirmModal estilizado en lugar de window.confirm
    setPendingConfirm({ eventId, newStatus, eventName,
      label: (labels[newStatus] || newStatus).charAt(0).toUpperCase()
             + (labels[newStatus] || newStatus).slice(1),
    });
  };

  const executeStatusChange = async (eventId, newStatus) => {
    setActionLoading(true);
    setError(null);
    try {
      await adminChangeEventStatus(eventId, newStatus);
      closeModal();
      setPendingCancel(null);
      setPendingConfirm(null);
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
    <div className="min-h-screen flex bg-background text-textPrimary font-sans">

      {/* ── Sidebar ── */}
      <aside className="w-52 flex-shrink-0 flex flex-col justify-between px-3 py-6 bg-background border-r border-surfaceAlt/30 h-screen sticky top-0">
        <nav className="flex flex-col gap-0.5">
          {navItems.map((item, idx) => (
            <a
              key={idx}
              href={item.href}
              className={`flex items-center gap-2.5 px-3 py-2.5 rounded-full text-sm font-medium transition-colors
                ${item.active
                  ? 'bg-primary text-textPrimary'
                  : 'text-textSecondary hover:text-textPrimary hover:bg-surfaceAlt/50'
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
            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary to-primaryDark flex items-center justify-center text-textPrimary font-bold text-sm flex-shrink-0">
              {userData.name.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 text-left min-w-0">
              <p className="text-sm font-semibold text-textPrimary truncate">{userData.name}</p>
              <p className="text-xs text-textMuted truncate">{userData.role}</p>
            </div>
            <ChevronDown className={`w-4 h-4 text-slate-400 transition-transform ${showMenu ? 'rotate-180' : ''}`} />
          </button>

          {showMenu && (
            <div className="absolute bottom-full left-0 right-0 mb-2 mx-2 bg-surface border border-surfaceAlt rounded-xl overflow-hidden shadow-xl z-50 user-menu-container">
              <button onClick={() => handleMenuClick('profile')}
                className="w-full flex items-center gap-3 px-4 py-3 text-sm text-textSecondary hover:bg-surfaceAlt/40 hover:text-textPrimary transition-colors">
                <User className="w-4 h-4" /> Perfil
              </button>
              <button onClick={() => handleMenuClick('settings')}
                className="w-full flex items-center gap-3 px-4 py-3 text-sm text-textSecondary hover:bg-surfaceAlt/40 hover:text-textPrimary transition-colors">
                <Settings className="w-4 h-4" /> Configuración
              </button>
              <div className="border-t border-surfaceAlt/40">
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
      <main className="flex-1 overflow-y-auto bg-background">
        <div className="p-8 max-w-6xl mx-auto w-full">

          {/* Page header */}
          <div className="mb-6">
            <h1 className="text-2xl font-black uppercase tracking-wide text-textPrimary mb-1">
              Gestión de Eventos
            </h1>
            <p className="text-textSecondary text-sm leading-relaxed max-w-xl">
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
                    ? 'bg-primary text-textPrimary'
                    : 'bg-surfaceAlt/60 text-textMuted hover:bg-surfaceAlt'
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
          <div className="rounded-xl overflow-hidden border border-surfaceAlt bg-surface">
            {loading ? (
              <div className="p-10 text-center text-textMuted text-sm flex items-center justify-center gap-2">
                <Loader2 className="w-4 h-4 animate-spin" /> Cargando eventos…
              </div>
            ) : events.length === 0 ? (
              <div className="p-10 text-center text-textMuted text-sm">
                No hay eventos para mostrar
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className="border-b border-white/5">
                      {['Nombre del Evento', 'Organizador', 'Fechas', 'Categoría', 'Boletas', 'Estado', 'Acciones'].map((h, i) => (
                        <th key={i}
                          className={`px-5 py-3.5 text-xs font-bold uppercase tracking-widest text-primary
                            ${i === 5 ? 'text-center' : i === 6 ? 'text-right' : ''}`}
                        >
                          {h}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {pageEvents.map((ev, idx) => {
                      const transitions   = allowedTransitions(ev.status);
                      return (
                        <tr key={ev.eventId}
                          className={`border-b border-white/5 hover:bg-white/[0.03] transition-colors
                            ${idx % 2 !== 0 ? 'bg-white/[0.02]' : ''}`}
                        >
                          {/* Nombre + avatar del organizador */}
                          <td className="px-5 py-3.5">
                            <div className="flex items-center gap-3">
                              <div className="w-9 h-9 rounded-full bg-surfaceAlt overflow-hidden flex-shrink-0 border border-surfaceAlt">
                                {ev.organizerProfilePicture
                                  ? <img src={ev.organizerProfilePicture} alt={ev.organizerName} className="w-full h-full object-cover" />
                                  : <div className="w-full h-full flex items-center justify-center text-xs font-bold text-textMuted">
                                      {ev.organizerName?.charAt(0)?.toUpperCase() || '?'}
                                    </div>
                                }
                              </div>
                              <div className="min-w-0">
                                <p className="text-sm font-semibold text-textPrimary truncate max-w-[180px]">{ev.eventName}</p>
                                <p className="text-xs text-textMuted">#{ev.eventId}</p>
                              </div>
                            </div>
                          </td>

                          {/* Organizador */}
                          <td className="px-5 py-3.5 text-sm text-textSecondary">
                            {ev.organizerName || <span className="text-textMuted">—</span>}
                          </td>

                          {/* Fechas */}
                          <td className="px-5 py-3.5 text-sm text-textSecondary whitespace-nowrap">
                            {fmtDate(ev.startDate)}
                            <span className="text-textMuted mx-1">→</span>
                            {fmtDate(ev.finishDate)}
                          </td>

                          {/* Categoría */}
                          <td className="px-5 py-3.5">
                            {ev.categoryName
                              ? <span className="px-2.5 py-1 bg-surfaceAlt text-textSecondary rounded-full text-xs font-medium">
                                  {ev.categoryName}
                                </span>
                              : <span className="text-textMuted text-xs">—</span>
                            }
                          </td>

                          {/* Boletas */}
                          <td className="px-5 py-3.5 text-sm text-textSecondary whitespace-nowrap">
                            {ev.availableSeats.toLocaleString('es-CO')}
                          </td>

                          {/* Estado */}
                          <td className="px-5 py-3.5 text-center">
                            <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${statusStyle(ev.status).cls}`}>
                              {statusStyle(ev.status).label}
                            </span>
                          </td>

                          {/* Acciones */}
                          <td className="px-5 py-3.5 text-right">
                            <div className="flex items-center justify-end gap-1">
                              {/* Ver detalle */}
                              <button
                                onClick={() => openModal(ev)}
                                className="p-1.5 hover:bg-primary/20 rounded-full transition-colors text-primary"
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
                                    ${value === 'COMPLETED' ? 'hover:bg-primary/20  text-primary'  : ''}
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
                <p className="text-xs text-textMuted">
                  Mostrando {pageStart + 1} a {pageEnd} de {events.length} eventos
                </p>
                <div className="flex items-center gap-1.5">
                  <button
                    onClick={() => setPage(p => Math.max(1, p - 1))}
                    disabled={page === 1}
                    className="h-8 w-8 flex items-center justify-center rounded-full bg-surfaceAlt/50 text-textSecondary text-sm
                      hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-40 transition-colors"
                  >
                    ‹
                  </button>

                  {pageNumbers.map((n, i) => {
                    const prev = pageNumbers[i - 1];
                    return (
                      <div key={n} className="flex items-center gap-1.5">
                        {prev && n - prev > 1 && (
                          <span className="text-textMuted text-xs px-1">…</span>
                        )}
                        <button
                          onClick={() => setPage(n)}
                          className={`h-8 w-8 flex items-center justify-center rounded-full text-xs font-bold transition-colors
                            ${page === n
                              ? 'bg-primary text-textPrimary'
                              : 'bg-surfaceAlt/50 hover:bg-surfaceAlt text-textSecondary'
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
          {/* Panel principal — animación de entrada */}
          <div
            className="admin-modal-panel w-full max-w-3xl flex flex-col max-h-[90vh] rounded-2xl overflow-hidden"
            style={{ background: '#1a0d28', border: '1px solid rgba(255,255,255,0.08)', boxShadow: '0 32px 80px rgba(0,0,0,0.7), 0 0 0 1px rgba(255,255,255,0.04)' }}
          >

            {/* ══ HERO HEADER — imagen de fondo + corte diagonal SVG ══════════ */}
            <div className="relative flex-shrink-0">
              {/* Bloque de imagen — altura fija, sin clip-path */}
              <div
                className="relative w-full"
                style={{ height: '190px', background: '#1a0d28' }}
              >
                {/* Imagen de fondo — cuando existe */}
                {mediaUrls.length > 0 && (
                  <img
                    src={mediaUrls[carouselIdx]}
                    alt=""
                    className="absolute inset-0 w-full h-full object-cover"
                    aria-hidden="true"
                  />
                )}

                {/* Fallback sin imagen — patrón de líneas diagonales moradas,
                    sin ningún blanco ni gris */}
                {mediaUrls.length === 0 && (
                  <div
                    className="absolute inset-0"
                    style={{
                      backgroundImage: `repeating-linear-gradient(
                        -45deg,
                        transparent 0px,
                        transparent 18px,
                        rgba(124,58,237,0.09) 18px,
                        rgba(124,58,237,0.09) 19px
                      )`,
                    }}
                  />
                )}

                {/* Scrim — siempre presente, más pronunciado en la base */}
                <div
                  className="absolute inset-0"
                  style={{
                    background: mediaUrls.length > 0
                      ? 'linear-gradient(to top, rgba(15,5,32,0.97) 0%, rgba(18,6,36,0.70) 38%, rgba(22,8,42,0.35) 65%, rgba(26,10,44,0.12) 100%)'
                      : 'linear-gradient(to top, rgba(15,5,32,0.85) 0%, rgba(15,5,32,0.45) 60%, rgba(15,5,32,0.15) 100%)',
                  }}
                />

                {/* Scrim radial esquina superior derecha — garantiza legibilidad de badges */}
                <div
                  className="absolute top-0 right-0 pointer-events-none"
                  style={{
                    width: '220px', height: '110px',
                    background: 'radial-gradient(ellipse at 100% 0%, rgba(15,5,32,0.80) 0%, transparent 65%)',
                  }}
                />

                {/* Badges + cerrar */}
                <div className="absolute top-3.5 right-4 flex items-center gap-1.5 z-10">
                  {(() => {
                    const s = statusStyle(modalData.status);
                    const dotCls =
                      modalData.status === 'PUBLISHED' ? 'bg-emerald-400' :
                      modalData.status === 'CANCELLED' ? 'bg-rose-400'    :
                      modalData.status === 'COMPLETED' ? 'bg-violet-400'  :
                      'bg-slate-400';
                    return (
                      <span className={`admin-badge-angular flex items-center gap-1.5 px-2.5 py-1 text-[11px] font-semibold ${s.cls}`}>
                        <span className={`w-1.5 h-1.5 rounded-full flex-shrink-0 ${dotCls}`} />
                        {s.label}
                      </span>
                    );
                  })()}
                  {modalData.isPublic != null && (
                    <span className={`admin-badge-angular flex items-center gap-1.5 px-2.5 py-1 text-[11px] font-semibold
                      ${modalData.isPublic
                        ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/35'
                        : 'bg-violet-900/40 text-violet-300 border border-violet-700/40'}`}>
                      <span className={`w-1.5 h-1.5 rounded-full flex-shrink-0 ${modalData.isPublic ? 'bg-emerald-400' : 'bg-violet-400'}`} />
                      {modalData.isPublic ? 'Activo' : 'Privado'}
                    </span>
                  )}
                  <button
                    onClick={closeModal}
                    className="p-1.5 rounded-lg text-white/55 hover:text-white hover:bg-white/10"
                    style={{ transition: 'color 150ms, background 150ms, transform 200ms' }}
                    aria-label="Cerrar"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>

                {/* Nombre + ID */}
                <div className="absolute bottom-3 left-0 right-0 px-6 z-10">
                  <h2
                    className="text-2xl font-black text-white leading-tight tracking-wider uppercase"
                    style={{ textShadow: '0 2px 16px rgba(0,0,0,0.7)' }}
                  >
                    {modalData.eventName}
                  </h2>
                  <p className="text-[11px] font-bold uppercase tracking-[0.12em] mt-0.5"
                    style={{ color: 'rgba(196,181,253,0.62)' }}>
                    ID #{modalData.eventId}
                  </p>
                </div>
              </div>

              {/* Corte diagonal SVG — único método fiable sin artefactos cross-browser.
                  Dibuja un path que tapa la esquina inferior-derecha con el mismo fondo
                  del panel (#1a0d28), creando la ilusión del ángulo ~8°. */}
              <svg
                aria-hidden="true"
                className="absolute bottom-0 left-0 w-full pointer-events-none"
                style={{ height: '22px', display: 'block' }}
                preserveAspectRatio="none"
                viewBox="0 0 100 22"
              >
                <path d="M0 22 L100 0 L100 22 Z" fill="#1a0d28" />
              </svg>

              {/* Tira de miniaturas */}
              {mediaUrls.length > 1 && (
                <div
                  className="flex gap-2 px-6 py-2.5"
                  style={{ background: 'rgba(15,5,32,0.80)', borderBottom: '1px solid rgba(124,58,237,0.15)' }}
                >
                  {mediaUrls.map((url, i) => (
                    <button
                      key={i}
                      onClick={() => setCarouselIdx(i)}
                      className={`admin-thumbnail flex-shrink-0 rounded-md overflow-hidden focus:outline-none
                        ${i === carouselIdx ? 'opacity-100' : 'opacity-40 hover:opacity-70'}`}
                      style={{
                        width: '48px', height: '34px',
                        ...(i === carouselIdx
                          ? { outline: '2px solid rgba(167,139,250,0.9)', outlineOffset: '2px' }
                          : {}),
                      }}
                      aria-label={`Imagen ${i + 1}`}
                      aria-pressed={i === carouselIdx}
                    >
                      <img src={url} alt="" className="w-full h-full object-cover" />
                    </button>
                  ))}

                  {/* Contador */}
                  <span className="ml-auto self-center text-[11px] font-semibold tabular-nums" style={{ color: 'rgba(196,181,253,0.60)' }}>
                    {carouselIdx + 1} / {mediaUrls.length}
                  </span>

                  {/* Botón abrir lightbox */}
                  <button
                    onClick={() => { setLightboxIdx(carouselIdx); setLightboxOpen(true); }}
                    className="self-center p-1 rounded hover:text-white/80 transition-colors" style={{ color: 'rgba(196,181,253,0.55)' }}
                    aria-label="Ver en pantalla completa"
                  >
                    <ZoomIn className="w-3.5 h-3.5" />
                  </button>
                </div>
              )}

              {/* Separador — solo dentro del cuerpo, no aquí */}
            </div>

            {/* ══ CUERPO scrollable ════════════════════════════════════════════ */}
            <div className="overflow-y-auto flex-1 px-6 pt-5 pb-8 space-y-5">

              {detailLoading && (
                <div className="flex items-center gap-2 text-xs" style={{ color: 'rgba(196,181,253,0.55)' }}>
                  <Loader2 className="w-3 h-3 animate-spin" /> Cargando información completa…
                </div>
              )}

              {/* ── Meta inline: fecha · hora · ubicación — chips ────────────── */}
              {(modalData.startDate || modalData.siteName) && (
                <div className="flex flex-wrap items-center gap-2">
                  {modalData.startDate && (
                    <span
                      className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium"
                      style={{
                        background: 'rgba(124,58,237,0.18)',
                        border: '1px solid rgba(124,58,237,0.30)',
                        color: 'rgba(220,210,255,0.92)',
                      }}
                    >
                      <Calendar className="w-3 h-3 flex-shrink-0" />
                      {fmtDate(modalData.startDate)}
                      {fmtTime(modalData.startDate) !== '—' && (
                        <span style={{ color: 'rgba(196,181,253,0.65)' }}>· {fmtTime(modalData.startDate)}</span>
                      )}
                    </span>
                  )}
                  {modalData.siteName && (
                    <span
                      className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium"
                      style={{
                        background: 'rgba(255,255,255,0.07)',
                        border: '1px solid rgba(255,255,255,0.12)',
                        color: 'rgba(255,255,255,0.72)',
                      }}
                    >
                      <MapPin className="w-3 h-3 flex-shrink-0" />
                      {modalData.siteName}
                    </span>
                  )}
                </div>
              )}

              {/* ── Descripción del evento ────────────────────────────────────── */}
              {modalData.description && (
                <>
                  <div className="admin-divider-angled" />
                  <div
                    className="flex items-start gap-3 rounded-xl p-4"
                    style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}
                  >
                    <div
                      className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0"
                      style={{ background: 'rgba(124,58,237,0.18)' }}
                    >
                      <FileText className="w-[15px] h-[15px]" style={{ color: 'rgba(167,139,250,0.85)' }} />
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="text-[10px] font-bold uppercase tracking-[0.12em] mb-1.5" style={{ color: 'rgba(196,181,253,0.78)' }}>
                        Descripción del evento
                      </p>
                      <p className="text-sm leading-relaxed whitespace-pre-wrap" style={{ color: 'rgba(220,210,255,0.75)' }}>
                        {modalData.description}
                      </p>
                    </div>
                  </div>
                </>
              )}

              {/* Separador */}
              <div className="admin-divider-angled" />

              {/* ── Grid 2×N de InfoCards ─────────────────────────────────────── */}
              <div>
                {/* Grupo temporal — fecha inicio + fin */}
                <p className="text-[10px] font-bold uppercase tracking-[0.14em] mb-2" style={{ color: 'rgba(196,181,253,0.82)' }}>
                  Fechas
                </p>
                <div className="grid grid-cols-2 gap-2 mb-4">
                  <InfoCard icon={Calendar} iconColor="text-sky-400" iconBg="bg-sky-500/15" label="Fecha y hora">
                    <p className="text-sm font-bold text-white">{fmtDate(modalData.startDate)}</p>
                    {fmtTime(modalData.startDate) !== '—' && (
                      <p className="text-xs mt-0.5" style={{ color: 'rgba(196,181,253,0.65)' }}>{fmtTime(modalData.startDate)}</p>
                    )}
                  </InfoCard>
                  <InfoCard icon={Calendar} iconColor="text-sky-400" iconBg="bg-sky-500/15" label="Fecha de fin">
                    <p className="text-sm font-bold text-white">{fmtDate(modalData.finishDate)}</p>
                  </InfoCard>
                </div>

                {/* Grupo de contexto — tipo, ciudad, modalidad, estado */}
                <p className="text-[10px] font-bold uppercase tracking-[0.14em] mb-2" style={{ color: 'rgba(196,181,253,0.82)' }}>
                  Detalles
                </p>
                <div className="grid grid-cols-2 gap-2 mb-4">
                  <InfoCard icon={Tag} iconColor="text-violet-400" iconBg="bg-violet-500/15" label="Tipo de evento">
                    {modalData.categoryName
                      ? <span
                          className="admin-badge-angular inline-block mt-0.5 px-2 py-0.5 text-xs font-semibold"
                          style={{ background: 'rgba(124,58,237,0.15)', color: 'rgba(196,181,253,0.9)', border: '1px solid rgba(124,58,237,0.25)' }}
                        >
                          {modalData.categoryName}
                        </span>
                      : <p className="text-sm font-bold text-white">—</p>
                    }
                  </InfoCard>
                  <InfoCard icon={MapPin} iconColor="text-rose-400" iconBg="bg-rose-500/15" label="Ciudad">
                    <p className="text-sm font-bold text-white">{modalData.siteName || '—'}</p>
                  </InfoCard>
                  <InfoCard icon={Users} iconColor="text-amber-400" iconBg="bg-amber-500/15" label="Modalidad">
                    <p className="text-sm font-bold text-white">
                      {modalData.isPublic == null ? '—' : modalData.isPublic ? 'Presencial' : 'Virtual'}
                    </p>
                  </InfoCard>
                  <InfoCard
                    icon={Shield}
                    iconColor={modalData.isPublic ? 'text-emerald-400' : 'text-violet-300'}
                    iconBg={modalData.isPublic ? 'bg-emerald-500/15' : 'bg-violet-900/30'}
                    label="Estado"
                  >
                    <p className={`text-sm font-bold ${modalData.isPublic ? 'text-emerald-400' : 'text-violet-300'}`}>
                      {modalData.isPublic == null ? '—' : modalData.isPublic ? 'Público' : 'Privado'}
                    </p>
                  </InfoCard>
                </div>

                {/* Organizador + boletas */}
                <p className="text-[10px] font-bold uppercase tracking-[0.14em] mb-2" style={{ color: 'rgba(196,181,253,0.82)' }}>
                  Organización
                </p>
                <div className="grid grid-cols-2 gap-2">
                  <InfoCard icon={UserCircle} iconColor="text-violet-300" iconBg="bg-violet-900/30" label="Organizador" wide>
                    <p className="text-sm font-bold text-white truncate">{modalData.organizerName || '—'}</p>
                    {modalData.organizerEmail && (
                      <p className="text-xs truncate mt-0.5" style={{ color: 'rgba(167,139,250,0.75)' }}>
                        {modalData.organizerEmail}
                      </p>
                    )}
                  </InfoCard>
                  <InfoCard icon={Ticket} iconColor="text-emerald-400" iconBg="bg-emerald-500/15" label="Boletas disponibles">
                    <p className="text-sm font-bold text-white">{modalData.availableSeats?.toLocaleString('es-CO') ?? '—'}</p>
                  </InfoCard>
                  {modalData.createdAt && (
                    <InfoCard icon={Clock} iconColor="text-violet-300" iconBg="bg-violet-900/30" label="Creado el">
                      <p className="text-sm font-bold text-white">{fmtDateTime(modalData.createdAt)}</p>
                      {modalData.createdBy && (
                        <p className="text-[10px] mt-0.5" style={{ color: 'rgba(196,181,253,0.55)' }}>por {modalData.createdBy}</p>
                      )}
                    </InfoCard>
                  )}
                  {modalData.updatedAt && (
                    <InfoCard icon={Clock} iconColor="text-violet-300" iconBg="bg-violet-900/30" label="Última actualización">
                      <p className="text-sm font-bold text-white">{fmtDateTime(modalData.updatedAt)}</p>
                    </InfoCard>
                  )}
                </div>
              </div>
            </div>

            {/* ══ FOOTER: botones de acción ════════════════════════════════════ */}
            {allowedTransitions(modalData.status).length > 0 && (
              <div
                className="px-6 py-4 flex gap-3 flex-shrink-0"
                style={{ borderTop: '1px solid rgba(255,255,255,0.06)' }}
              >
                {allowedTransitions(modalData.status).map(({ value, label: tLabel }) => (
                  <button
                    key={value}
                    onClick={() =>
                      value === 'CANCELLED'
                        ? requestCancel(modalData.eventId, modalData.eventName, true)
                        : handleStatusChange(modalData.eventId, value, modalData.eventName)
                    }
                    disabled={actionLoading}
                    className={`flex-1 py-3.5 rounded-xl text-sm font-bold border transition-all disabled:opacity-50
                      flex items-center justify-center gap-2 tracking-wide
                      ${value === 'PUBLISHED'
                        ? 'admin-publish-btn text-white border-transparent hover:brightness-110'
                        : value === 'CANCELLED'
                          ? 'bg-rose-500/10 text-rose-400 border-rose-500/20 hover:bg-rose-500 hover:text-white hover:border-rose-500'
                          : 'bg-white/5 text-white/70 border-white/10 hover:bg-white/10 hover:text-white hover:border-white/20'
                      }`}
                    style={value === 'PUBLISHED' ? {
                      background: 'linear-gradient(90deg, #7c3aed 0%, #a78bfa 100%)',
                      boxShadow: 'rgba(124,58,237,0.45) 0 12px 32px, rgba(124,58,237,0.15) 0 0 0 1px inset',
                    } : undefined}
                  >
                    {actionLoading ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      <>
                        {value === 'PUBLISHED' && <Send    className="w-4 h-4" />}
                        {value === 'CANCELLED' && <XCircle className="w-4 h-4" />}
                        {value === 'COMPLETED' && <Check   className="w-4 h-4" />}
                        {tLabel}
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
          Modal de confirmación genérico (Publicar / Finalizar)
      ═══════════════════════════════════════════════════════════════════════ */}
      <ConfirmModal
        isOpen={!!pendingConfirm}
        title={`¿${pendingConfirm?.label ?? ''} este evento?`}
        message={
          <>
            Estás por{' '}
            <span className="text-white font-semibold">
              {pendingConfirm?.label?.toLowerCase() ?? ''}
            </span>{' '}
            el evento{' '}
            <span className="text-white font-semibold">"{pendingConfirm?.eventName}"</span>.
            {pendingConfirm?.newStatus === 'PUBLISHED' &&
              ' Una vez publicado será visible para todos los usuarios.'}
          </>
        }
        confirmLabel={pendingConfirm?.label ?? 'Confirmar'}
        cancelLabel="No, volver"
        confirmStyle={pendingConfirm?.newStatus === 'COMPLETED' ? 'warning' : 'primary'}
        loading={actionLoading}
        onConfirm={() => executeStatusChange(pendingConfirm.eventId, pendingConfirm.newStatus)}
        onCancel={() => setPendingConfirm(null)}
      />

      {/* ═══════════════════════════════════════════════════════════════════════
          Modal de confirmación de cancelación
          Se muestra encima del modal de detalle cuando cancelFromModal=true,
          o standalone cuando viene desde la tabla.
      ═══════════════════════════════════════════════════════════════════════ */}
      {pendingCancel && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-[60] p-4">
          <div className="bg-surface rounded-2xl border border-surfaceAlt shadow-2xl shadow-black/60 w-full max-w-sm p-6 flex flex-col items-center text-center gap-4">

            {/* Icono de advertencia */}
            <div className="w-14 h-14 rounded-full bg-rose-500/10 border border-rose-500/20 flex items-center justify-center">
              <AlertTriangle className="w-7 h-7 text-rose-400" />
            </div>

            <div>
              <h3 className="text-base font-black text-textPrimary mb-1">¿Cancelar este evento?</h3>
              <p className="text-sm text-textSecondary leading-relaxed">
                Estás por cancelar{' '}
                <span className="text-white font-semibold">"{pendingCancel.eventName}"</span>.
                Esta acción no se puede deshacer y dejará de ser visible para los asistentes.
              </p>
            </div>

            <div className="flex gap-3 w-full">
              <button
                onClick={dismissCancel}
                disabled={actionLoading}
                className="flex-1 py-2.5 rounded-full text-sm font-bold bg-surfaceAlt text-textSecondary
                  border border-surfaceAlt hover:bg-surfaceAlt/80 transition-all disabled:opacity-50"
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
