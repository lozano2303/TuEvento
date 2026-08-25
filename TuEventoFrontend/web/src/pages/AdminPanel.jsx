import { useState, useEffect } from 'react';
import { Eye, LayoutDashboard, CreditCard, Calendar, BarChart2, RefreshCcw, Users, LogOut, User, Settings, ChevronDown, X } from 'lucide-react';
import { approveOrganizerRequest, rejectOrganizerRequest } from '../services/OrganizerPetitionService';
import { useNavigate } from 'react-router-dom';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export default function AdminPanel() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all');
  const [error, setError] = useState(null);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [showDocumentModal, setShowDocumentModal] = useState(false);
  const [documentUrl, setDocumentUrl] = useState(null);
  const [documentLoading, setDocumentLoading] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const navigate = useNavigate();

  const userData = {
    name: localStorage.getItem('name') || localStorage.getItem('fullName') || 'Usuario',
    email: localStorage.getItem('userEmail') || '',
    role: localStorage.getItem('role') === 'ADMIN' ? 'Super Admin' : 'Admin',
  };

  const handleLogout = () => { localStorage.clear(); window.location.href = '/login'; };
  const handleMenuClick = (action) => {
    setShowUserMenu(false);
    if (action === 'profile') navigate('/profile');
    if (action === 'settings') navigate('/settings');
    if (action === 'logout') handleLogout();
  };

  useEffect(() => { fetchRequests(); }, []);
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (showUserMenu && !e.target.closest('.user-menu-container')) setShowUserMenu(false);
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [showUserMenu]);

  const fetchRequests = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) { setError('No hay token de autenticación'); setLoading(false); return; }
      const response = await fetch(`${API_URL}/admin/organizer-requests`, {
        headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
      });
      const data = await response.json();
      if (response.ok) setRequests(data.data || []);
      else setError(data.message || 'Error al cargar solicitudes');
    } catch (err) {
      setError('Error de conexión: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const filteredRequests = requests.filter((req) => {
    if (filter === 'all')      return true;
    if (filter === 'pending')  return req.status === 'PENDING';
    if (filter === 'approved') return req.status === 'APPROVED';
    if (filter === 'rejected') return req.status === 'REJECTED';
    return true;
  });

  const navItems = [
    { icon: LayoutDashboard, label: 'Dashboard' },
    { icon: CreditCard,      label: 'Control Financiero' },
    { icon: Calendar,        label: 'Eventos' },
    { icon: BarChart2,       label: 'Reportes' },
    { icon: RefreshCcw,      label: 'Reembolsos' },
    { icon: Users,           label: 'Solicitudes', active: true },
  ];

  // Colores semánticos de estado — no deben cambiar con el tema
  const statusStyle = (status) => {
    if (status === 'PENDING')  return { bg: 'bg-amber-500',   label: 'Pendiente' };
    if (status === 'APPROVED') return { bg: 'bg-emerald-500', label: 'Aprobado'  };
    if (status === 'REJECTED') return { bg: 'bg-rose-500',    label: 'Rechazado' };
    return { bg: 'bg-surfaceAlt', label: status };
  };

  const filters = [
    { key: 'all',      label: 'Todas'      },
    { key: 'pending',  label: 'Pendientes' },
    { key: 'approved', label: 'Aprobadas'  },
    { key: 'rejected', label: 'Rechazadas' },
  ];

  const fetchDocumentUrl = async (req) => {
    if (!req || !req.storedFileId) { setDocumentLoading(false); return; }
    setDocumentLoading(true);
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(
        `${API_URL}/admin/organizer-requests/${req.organizerPetitionId}/document`,
        { headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' } }
      );
      const data = await response.json();
      if (response.ok && data.data?.publicUrl) setDocumentUrl(data.data.publicUrl);
      else setError('No se pudo obtener la URL del documento');
    } catch (err) {
      setError('Error al obtener documento: ' + err.message);
    } finally {
      setDocumentLoading(false);
    }
  };

  const handleViewDocument = (req) => {
    setSelectedRequest(req); setDocumentUrl(null); setShowDocumentModal(true); fetchDocumentUrl(req);
  };
  const handleApprove = async (req) => {
    if (!req || req.status !== 'PENDING') return;
    if (!window.confirm(`¿Aprobar la solicitud de ${req.fullName || 'este usuario'}?`)) return;
    setActionLoading(true); setError(null);
    try { await approveOrganizerRequest(req.organizerPetitionId); closeDocumentModal(); await fetchRequests(); }
    catch (err) { setError(err.message || 'Error al aprobar solicitud'); }
    finally { setActionLoading(false); }
  };
  const handleReject = async (req) => {
    if (!req) return;
    if (!window.confirm(`¿Rechazar la solicitud de ${req.fullName || 'este usuario'}?`)) return;
    setActionLoading(true); setError(null);
    try { await rejectOrganizerRequest(req.organizerPetitionId); closeDocumentModal(); await fetchRequests(); }
    catch (err) { setError(err.message || 'Error al rechazar solicitud'); }
    finally { setActionLoading(false); }
  };
  const handleOpenDocument = () => { if (documentUrl) window.open(documentUrl, '_blank'); };
  const closeDocumentModal = () => { setShowDocumentModal(false); setDocumentUrl(null); setSelectedRequest(null); };

  return (
    <div className="min-h-screen flex bg-background text-textPrimary font-sans">

      {/* ── Sidebar ── */}
      <aside
        className="w-52 flex-shrink-0 flex flex-col justify-between px-3 py-6 h-screen sticky top-0"
        style={{ background: 'var(--color-background)', borderRight: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 40%, transparent)' }}
      >
        <nav className="flex flex-col gap-0.5">
          {navItems.map((item, idx) => (
            <a
              key={idx}
              href="#"
              className="flex items-center gap-2.5 px-3 py-2.5 rounded-full text-sm font-medium transition-colors"
              style={item.active
                ? { background: 'var(--color-primary)', color: 'var(--color-textPrimary)' }
                : { color: 'var(--color-textMuted)' }
              }
              onMouseOver={e => { if (!item.active) { e.currentTarget.style.color = 'var(--color-textPrimary)'; e.currentTarget.style.background = 'color-mix(in srgb, var(--color-surfaceAlt) 30%, transparent)'; } }}
              onMouseOut={e => { if (!item.active) { e.currentTarget.style.color = 'var(--color-textMuted)'; e.currentTarget.style.background = 'transparent'; } }}
            >
              <item.icon className="w-4 h-4 flex-shrink-0" />
              {item.label}
            </a>
          ))}
        </nav>

        {/* User Profile Card */}
        <div
          className="pt-4 relative user-menu-container flex-shrink-0"
          style={{ borderTop: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 40%, transparent)' }}
        >
          <button
            onClick={() => setShowUserMenu(!showUserMenu)}
            className="w-full flex items-center gap-3 p-2 rounded-xl transition-all group"
            onMouseOver={e => e.currentTarget.style.background = 'color-mix(in srgb, var(--color-surfaceAlt) 30%, transparent)'}
            onMouseOut={e => e.currentTarget.style.background = 'transparent'}
          >
            <div
              className="w-10 h-10 rounded-full flex items-center justify-center text-textPrimary font-bold text-sm flex-shrink-0"
              style={{ background: 'linear-gradient(135deg, var(--color-primary), var(--color-primaryDark))' }}
            >
              {userData.name.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 text-left min-w-0">
              <p className="text-sm font-semibold text-textPrimary truncate">{userData.name}</p>
              <p className="text-xs text-textMuted truncate">{userData.role}</p>
            </div>
            <ChevronDown className={`w-4 h-4 text-textMuted transition-transform ${showUserMenu ? 'rotate-180' : ''}`} />
          </button>

          {showUserMenu && (
            <div
              className="absolute bottom-full left-0 right-0 mb-2 mx-2 rounded-xl overflow-hidden shadow-xl z-50 user-menu-container"
              style={{ background: 'var(--color-surface)', border: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 60%, transparent)' }}
            >
              {[
                { action: 'profile',  icon: User,     label: 'Perfil'          },
                { action: 'settings', icon: Settings,  label: 'Configuración'   },
              ].map(({ action, icon: Icon, label }) => (
                <button key={action} onClick={() => handleMenuClick(action)}
                  className="w-full flex items-center gap-3 px-4 py-3 text-sm text-textSecondary transition-colors"
                  onMouseOver={e => { e.currentTarget.style.background = 'color-mix(in srgb, var(--color-surfaceAlt) 40%, transparent)'; e.currentTarget.style.color = 'var(--color-textPrimary)'; }}
                  onMouseOut={e => { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.color = 'var(--color-textSecondary)'; }}
                >
                  <Icon className="w-4 h-4" />{label}
                </button>
              ))}
              <div style={{ borderTop: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 40%, transparent)' }}>
                <button onClick={() => handleMenuClick('logout')}
                  className="w-full flex items-center gap-3 px-4 py-3 text-sm text-error transition-colors"
                  onMouseOver={e => e.currentTarget.style.background = 'color-mix(in srgb, var(--color-error) 10%, transparent)'}
                  onMouseOut={e => e.currentTarget.style.background = 'transparent'}
                >
                  <LogOut className="w-4 h-4" />Cerrar sesión
                </button>
              </div>
            </div>
          )}
        </div>
      </aside>

      {/* ── Main ── */}
      <main className="flex-1 overflow-y-auto" style={{ background: 'var(--color-surface)' }}>
        <div className="p-8 max-w-6xl mx-auto w-full">

          <div className="mb-6">
            <h1 className="text-2xl font-black uppercase tracking-wide text-textPrimary mb-1">
              Solicitudes de Organizadores
            </h1>
            <p className="text-textMuted text-sm leading-relaxed max-w-xl">
              Revisa y gestiona las solicitudes enviadas por usuarios que desean crear eventos en la
              plataforma. Verifica su documentación antes de aprobar su acceso.
            </p>
          </div>

          {/* Filter pills */}
          <div className="flex items-center gap-2 mb-6">
            {filters.map(({ key, label }) => (
              <button key={key} onClick={() => setFilter(key)}
                className="px-5 py-1.5 rounded-full text-sm font-semibold transition-all"
                style={filter === key
                  ? { background: 'var(--color-primary)', color: 'var(--color-textPrimary)' }
                  : { background: 'color-mix(in srgb, var(--color-surfaceAlt) 40%, transparent)', color: 'var(--color-textSecondary)' }
                }
              >
                {label}
              </button>
            ))}
          </div>

          {/* Table card */}
          <div
            className="rounded-xl overflow-hidden"
            style={{ border: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 40%, transparent)', background: 'var(--color-background)' }}
          >
            {error ? (
              <div className="p-10 text-center text-error text-sm">{error}</div>
            ) : loading ? (
              <div className="p-10 text-center text-textMuted text-sm">Cargando...</div>
            ) : filteredRequests.length === 0 ? (
              <div className="p-10 text-center text-textMuted text-sm">No hay solicitudes para mostrar</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr style={{ borderBottom: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 40%, transparent)' }}>
                      {['Usuario','Correo','Documento','Fecha','Estado','Acciones'].map((h, i) => (
                        <th key={i}
                          className={`px-5 py-3.5 text-xs font-bold uppercase tracking-widest text-primary
                            ${i === 4 ? 'text-center' : i === 5 ? 'text-right' : ''}`}
                        >{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {filteredRequests.map((req, idx) => {
                      const { bg, label } = statusStyle(req.status);
                      return (
                        <tr key={req.organizerPetitionId}
                          className="transition-colors"
                          style={{
                            borderBottom: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 30%, transparent)',
                            background: idx % 2 !== 0 ? 'color-mix(in srgb, var(--color-surfaceAlt) 10%, transparent)' : 'transparent',
                          }}
                          onMouseOver={e => e.currentTarget.style.background = 'color-mix(in srgb, var(--color-surfaceAlt) 20%, transparent)'}
                          onMouseOut={e => e.currentTarget.style.background = idx % 2 !== 0 ? 'color-mix(in srgb, var(--color-surfaceAlt) 10%, transparent)' : 'transparent'}
                        >
                          <td className="px-5 py-3.5">
                            <div className="flex items-center gap-3">
                              <div className="w-9 h-9 rounded-full overflow-hidden flex-shrink-0"
                                style={{ background: 'var(--color-surfaceAlt)' }}>
                                {req.profilePicture
                                  ? <img src={req.profilePicture} alt={req.fullName} className="w-full h-full object-cover" />
                                  : <div className="w-full h-full flex items-center justify-center text-xs font-bold text-textMuted">{req.fullName?.charAt(0) || '?'}</div>
                                }
                              </div>
                              <span className="text-sm font-semibold text-textPrimary">{req.fullName}</span>
                            </div>
                          </td>
                          <td className="px-5 py-3.5 text-sm text-textSecondary">{req.email}</td>
                          <td className="px-5 py-3.5">
                            <span className="px-2.5 py-1 rounded-full text-xs font-medium text-textSecondary"
                              style={{ background: 'color-mix(in srgb, var(--color-surfaceAlt) 60%, transparent)' }}>
                              {req.documentType || 'Cédula'}
                            </span>
                          </td>
                          <td className="px-5 py-3.5 text-sm text-textSecondary">
                            {req.applicationDate ? new Date(req.applicationDate).toLocaleDateString('es-CO') : '-'}
                          </td>
                          <td className="px-5 py-3.5 text-center">
                            <span className={`inline-block px-3 py-1 rounded-full text-xs font-bold text-white ${bg}`}>{label}</span>
                          </td>
                          <td className="px-5 py-3.5 text-right">
                            <button onClick={() => handleViewDocument(req)}
                              className="p-1.5 rounded-full transition-colors text-primary"
                              onMouseOver={e => e.currentTarget.style.background = 'color-mix(in srgb, var(--color-primary) 20%, transparent)'}
                              onMouseOut={e => e.currentTarget.style.background = 'transparent'}
                              title="Ver solicitud"
                            >
                              <Eye className="w-4 h-4" />
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}

            {/* Pagination */}
            <div className="px-5 py-4 flex flex-col sm:flex-row items-center justify-between gap-3"
              style={{ borderTop: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 40%, transparent)' }}>
              <p className="text-xs text-textMuted">
                Mostrando 1 a {filteredRequests.length} de {requests.length} solicitudes
              </p>
              <div className="flex items-center gap-1.5">
                <button className="h-8 w-8 flex items-center justify-center rounded-full text-textMuted text-sm cursor-not-allowed"
                  style={{ background: 'color-mix(in srgb, var(--color-surfaceAlt) 30%, transparent)' }}>‹</button>
                <button className="h-8 w-8 flex items-center justify-center rounded-full text-textPrimary font-bold text-xs"
                  style={{ background: 'var(--color-primary)' }}>1</button>
                {[2, 3].map(n => (
                  <button key={n} className="h-8 w-8 flex items-center justify-center rounded-full text-textSecondary text-xs transition-colors"
                    style={{ background: 'color-mix(in srgb, var(--color-surfaceAlt) 30%, transparent)' }}>{n}</button>
                ))}
                <button className="h-8 px-3 flex items-center justify-center rounded-full text-textSecondary text-xs transition-colors"
                  style={{ background: 'color-mix(in srgb, var(--color-surfaceAlt) 30%, transparent)' }}>Siguiente ›</button>
              </div>
            </div>
          </div>

          {/* Document Modal */}
          {showDocumentModal && selectedRequest && (
            <div className="fixed inset-0 theme-overlay flex items-center justify-center z-50 p-4">
              <div className="modal-elevated rounded-2xl max-w-lg w-full flex flex-col"
                style={{ background: 'var(--color-background)', border: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 60%, transparent)' }}>

                {/* Modal Header */}
                <div className="flex items-center justify-between px-6 py-5"
                  style={{ borderBottom: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 40%, transparent)' }}>
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full flex items-center justify-center text-textPrimary font-bold text-sm flex-shrink-0 overflow-hidden"
                      style={{ background: 'linear-gradient(135deg, var(--color-primary), var(--color-primaryDark))' }}>
                      {selectedRequest.profilePicture
                        ? <img src={selectedRequest.profilePicture} alt={selectedRequest.fullName} className="w-full h-full object-cover" />
                        : selectedRequest.fullName?.charAt(0).toUpperCase() || '?'
                      }
                    </div>
                    <div>
                      <h2 className="text-base font-bold text-textPrimary leading-tight">{selectedRequest.fullName}</h2>
                      <p className="text-xs text-textMuted">{selectedRequest.email}</p>
                    </div>
                  </div>
                  <button onClick={closeDocumentModal}
                    className="p-1.5 rounded-full transition-colors text-textMuted hover:text-textPrimary"
                    onMouseOver={e => e.currentTarget.style.background = 'color-mix(in srgb, var(--color-surfaceAlt) 40%, transparent)'}
                    onMouseOut={e => e.currentTarget.style.background = 'transparent'}>
                    <X className="w-5 h-5" />
                  </button>
                </div>

                {/* Info Grid */}
                <div className="px-6 py-4 grid grid-cols-2 gap-3">
                  {[
                    { label: 'Tipo de documento', value: selectedRequest.documentType || 'Cédula' },
                    { label: 'Fecha de solicitud', value: selectedRequest.applicationDate ? new Date(selectedRequest.applicationDate).toLocaleDateString('es-CO') : '-' },
                  ].map(({ label, value }) => (
                    <div key={label} className="rounded-xl p-3"
                      style={{ background: 'color-mix(in srgb, var(--color-surfaceAlt) 20%, transparent)', border: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 40%, transparent)' }}>
                      <p className="text-[10px] uppercase tracking-widest text-textMuted font-semibold mb-1">{label}</p>
                      <p className="text-sm font-semibold text-textPrimary">{value}</p>
                    </div>
                  ))}
                  <div className="rounded-xl p-3 col-span-2"
                    style={{ background: 'color-mix(in srgb, var(--color-surfaceAlt) 20%, transparent)', border: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 40%, transparent)' }}>
                    <p className="text-[10px] uppercase tracking-widest text-textMuted font-semibold mb-1">Estado actual</p>
                    <span className={`inline-block px-3 py-1 rounded-full text-xs font-bold text-white ${statusStyle(selectedRequest.status).bg}`}>
                      {statusStyle(selectedRequest.status).label}
                    </span>
                  </div>
                </div>

                {/* Document Button */}
                <div className="px-6 pb-2">
                  <button onClick={handleOpenDocument} disabled={!documentUrl || documentLoading}
                    className="w-full flex items-center justify-between gap-3 px-4 py-3.5 rounded-xl border transition-all group"
                    style={documentUrl
                      ? { background: 'color-mix(in srgb, var(--color-primary) 10%, transparent)', borderColor: 'color-mix(in srgb, var(--color-primary) 30%, transparent)', cursor: 'pointer' }
                      : { background: 'color-mix(in srgb, var(--color-surfaceAlt) 15%, transparent)', borderColor: 'color-mix(in srgb, var(--color-surfaceAlt) 30%, transparent)', cursor: 'not-allowed', opacity: 0.5 }
                    }>
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0"
                        style={{ background: documentUrl ? 'color-mix(in srgb, var(--color-primary) 20%, transparent)' : 'color-mix(in srgb, var(--color-surfaceAlt) 30%, transparent)' }}>
                        <svg className="w-4 h-4" style={{ color: documentUrl ? 'var(--color-primary)' : 'var(--color-textMuted)' }}
                          fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                          <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                      </div>
                      <div className="text-left">
                        <p className="text-sm font-semibold" style={{ color: documentUrl ? 'var(--color-textPrimary)' : 'var(--color-textMuted)' }}>
                          {documentLoading ? 'Cargando documento...' : documentUrl ? 'Ver documento adjunto' : 'Sin documento disponible'}
                        </p>
                        <p className="text-xs text-textMuted">{documentUrl ? 'Se abrirá en una nueva pestaña' : ''}</p>
                      </div>
                    </div>
                    {!documentLoading && documentUrl && (
                      <svg className="w-4 h-4 group-hover:translate-x-0.5 transition-transform flex-shrink-0"
                        style={{ color: 'var(--color-primary)' }} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                      </svg>
                    )}
                  </button>
                </div>

                {/* Actions */}
                <div className="px-6 py-5 flex gap-3">
                  <button onClick={() => handleApprove(selectedRequest)}
                    disabled={selectedRequest.status !== 'PENDING' || actionLoading}
                    className="flex-1 py-2.5 rounded-full text-sm font-bold border transition-all bg-emerald-500/10 text-emerald-400 border-emerald-500/20 hover:bg-emerald-500 hover:text-white hover:border-emerald-500 disabled:opacity-50 disabled:cursor-not-allowed">
                    {actionLoading ? 'Procesando...' : 'Aceptar'}
                  </button>
                  <button onClick={() => handleReject(selectedRequest)}
                    disabled={selectedRequest.status !== 'PENDING' || actionLoading}
                    className="flex-1 py-2.5 rounded-full text-sm font-bold border transition-all bg-rose-500/10 text-rose-400 border-rose-500/20 hover:bg-rose-500 hover:text-white hover:border-rose-500 disabled:opacity-50 disabled:cursor-not-allowed">
                    Rechazar
                  </button>
                </div>
              </div>
            </div>
          )}

        </div>
      </main>
    </div>
  );
}
