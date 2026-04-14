import { useState, useEffect } from 'react';
import { Eye, LayoutDashboard, CreditCard, Calendar, BarChart2, RefreshCcw, Users, LogOut, User, Settings, ChevronDown } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export default function AdminPanel() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all');
  const [error, setError] = useState(null);
  const [showUserMenu, setShowUserMenu] = useState(false);

  const navigate = useNavigate();

  const userData = {
    name: localStorage.getItem('name') || localStorage.getItem('fullName') || 'Usuario',
    email: localStorage.getItem('userEmail') || '',
    role: localStorage.getItem('role') === 'ADMIN' ? 'Super Admin' : 'Admin',
  };

  const handleLogout = () => {
    localStorage.clear();
    window.location.href = '/login';
  };

  const handleMenuClick = (action) => {
    setShowUserMenu(false);
    if (action === 'profile') navigate('/profile');
    if (action === 'settings') navigate('/settings');
    if (action === 'logout') handleLogout();
  };

  useEffect(() => {
    fetchRequests();
  }, []);

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (showUserMenu && !e.target.closest('.user-menu-container')) {
        setShowUserMenu(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [showUserMenu]);

  const fetchRequests = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        setError('No hay token de autenticación');
        setLoading(false);
        return;
      }
      const response = await fetch(`${API_URL}/admin/organizer-requests`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });
      const data = await response.json();
      if (response.ok) {
        setRequests(data.data || []);
      } else {
        setError(data.message || 'Error al cargar solicitudes');
      }
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

  const statusStyle = (status) => {
    if (status === 'PENDING')  return { bg: 'bg-amber-500',   label: 'Pendiente' };
    if (status === 'APPROVED') return { bg: 'bg-emerald-500', label: 'Aprobado'  };
    if (status === 'REJECTED') return { bg: 'bg-rose-500',    label: 'Rechazado' };
    return { bg: 'bg-slate-600', label: status };
  };

  const filters = [
    { key: 'all',      label: 'Todas'     },
    { key: 'pending',  label: 'Pendientes'},
    { key: 'approved', label: 'Aprobadas' },
    { key: 'rejected', label: 'Rechazadas'},
  ];

  return (
    <div className="min-h-screen flex bg-[#12091b] text-white font-sans">

      {/* ── Sidebar ── */}
      <aside className="w-52 flex-shrink-0 flex flex-col justify-between px-3 py-6 bg-[#12091b] border-r border-white/5 h-screen sticky top-0">
      

        <nav className="flex flex-col gap-0.5">
          {navItems.map((item, idx) => (
            <a
              key={idx}
              href="#"
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

        {/* User Profile Card - Fixed at bottom left of sidebar */}
        <div className="pt-4 border-t border-white/5 relative user-menu-container flex-shrink-0">
          <button
            onClick={() => setShowUserMenu(!showUserMenu)}
            className="w-full flex items-center gap-3 p-2 rounded-xl hover:bg-white/5 transition-all group"
          >
            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-[#7f13ec] to-[#5a189a] flex items-center justify-center text-white font-bold text-sm flex-shrink-0">
              {userData.name.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 text-left min-w-0">
              <p className="text-sm font-semibold text-white truncate">{userData.name}</p>
              <p className="text-xs text-slate-400 truncate">{userData.role}</p>
            </div>
            <ChevronDown className={`w-4 h-4 text-slate-400 transition-transform ${showUserMenu ? 'rotate-180' : ''}`} />
          </button>

          {/* Dropdown Menu */}
          {showUserMenu && (
            <div className="absolute bottom-full left-0 right-0 mb-2 mx-2 bg-[#1a0d28] border border-white/10 rounded-xl overflow-hidden shadow-xl z-50 user-menu-container">
              <button
                onClick={() => handleMenuClick('profile')}
                className="w-full flex items-center gap-3 px-4 py-3 text-sm text-slate-300 hover:bg-white/5 hover:text-white transition-colors"
              >
                <User className="w-4 h-4" />
                Perfil
              </button>
              <button
                onClick={() => handleMenuClick('settings')}
                className="w-full flex items-center gap-3 px-4 py-3 text-sm text-slate-300 hover:bg-white/5 hover:text-white transition-colors"
              >
                <Settings className="w-4 h-4" />
                Configuración
              </button>
              <div className="border-t border-white/5">
                <button
                  onClick={() => handleMenuClick('logout')}
                  className="w-full flex items-center gap-3 px-4 py-3 text-sm text-red-400 hover:bg-red-500/10 transition-colors"
                >
                  <LogOut className="w-4 h-4" />
                  Cerrar sesión
                </button>
              </div>
            </div>
          )}
        </div>
      </aside>

      {/* ── Main ── */}
      <main className="flex-1 overflow-y-auto bg-[#16091f]">
        <div className="p-8 max-w-6xl mx-auto w-full">

          {/* Page header */}
          <div className="mb-6">
            <h1 className="text-2xl font-black uppercase tracking-wide text-white mb-1">
              Solicitudes de Organizadores
            </h1>
            <p className="text-slate-400 text-sm leading-relaxed max-w-xl">
              Revisa y gestiona las solicitudes enviadas por usuarios que desean crear eventos en la
              plataforma. Verifica su documentación antes de aprobar su acceso.
            </p>
          </div>

          {/* Filter pills */}
          <div className="flex items-center gap-2 mb-6">
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

          {/* Table card */}
          <div className="rounded-xl overflow-hidden border border-white/5 bg-[#1a0d28]">
            {error ? (
              <div className="p-10 text-center text-red-400 text-sm">{error}</div>
            ) : loading ? (
              <div className="p-10 text-center text-slate-500 text-sm">Cargando...</div>
            ) : filteredRequests.length === 0 ? (
              <div className="p-10 text-center text-slate-500 text-sm">
                No hay solicitudes para mostrar
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className="border-b border-white/5">
                      {['Usuario','Correo','Documento','Fecha','Estado','Acciones'].map((h, i) => (
                        <th
                          key={i}
                          className={`px-5 py-3.5 text-xs font-bold uppercase tracking-widest text-[#7f13ec]
                            ${i === 4 ? 'text-center' : i === 5 ? 'text-right' : ''}`}
                        >
                          {h}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {filteredRequests.map((req, idx) => {
                      const { bg, label } = statusStyle(req.status);
                      return (
                        <tr
                          key={req.id}
                          className={`border-b border-white/5 hover:bg-white/[0.03] transition-colors
                            ${idx % 2 !== 0 ? 'bg-white/[0.02]' : ''}`}
                        >
                          <td className="px-5 py-3.5">
                            <div className="flex items-center gap-3">
                              <div className="w-9 h-9 rounded-full bg-slate-700 overflow-hidden flex-shrink-0">
                                {req.profilePicture
                                  ? <img src={req.profilePicture} alt={req.fullName} className="w-full h-full object-cover" />
                                  : <div className="w-full h-full flex items-center justify-center text-xs font-bold text-slate-400">
                                      {req.fullName?.charAt(0) || '?'}
                                    </div>
                                }
                              </div>
                              <span className="text-sm font-semibold text-white">{req.fullName}</span>
                            </div>
                          </td>
                          <td className="px-5 py-3.5 text-sm text-slate-400">{req.email}</td>
                          <td className="px-5 py-3.5">
                            <span className="px-2.5 py-1 bg-slate-700/60 text-slate-300 rounded-full text-xs font-medium">
                              {req.documentType || 'Cédula'}
                            </span>
                          </td>
                          <td className="px-5 py-3.5 text-sm text-slate-400">
                            {req.createdAt ? new Date(req.createdAt).toLocaleDateString('es-CO') : '-'}
                          </td>
                          <td className="px-5 py-3.5 text-center">
                            <span className={`inline-block px-3 py-1 rounded-full text-xs font-bold text-white ${bg}`}>
                              {label}
                            </span>
                          </td>
                          <td className="px-5 py-3.5 text-right">
                            <button
                              className="p-1.5 hover:bg-[#7f13ec]/20 rounded-full transition-colors text-[#7f13ec]"
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
            <div className="px-5 py-4 flex flex-col sm:flex-row items-center justify-between gap-3 border-t border-white/5">
              <p className="text-xs text-slate-500">
                Mostrando 1 a {filteredRequests.length} de {requests.length} solicitudes
              </p>
              <div className="flex items-center gap-1.5">
                <button className="h-8 w-8 flex items-center justify-center rounded-full bg-white/5 text-slate-400 cursor-not-allowed text-sm">‹</button>
                <button className="h-8 w-8 flex items-center justify-center rounded-full bg-[#7f13ec] text-white font-bold text-xs">1</button>
                <button className="h-8 w-8 flex items-center justify-center rounded-full bg-white/5 hover:bg-white/10 text-slate-300 text-xs transition-colors">2</button>
                <button className="h-8 w-8 flex items-center justify-center rounded-full bg-white/5 hover:bg-white/10 text-slate-300 text-xs transition-colors">3</button>
                <button className="h-8 px-3 flex items-center justify-center rounded-full bg-white/5 hover:bg-white/10 text-slate-300 text-xs transition-colors">
                  Siguiente ›
                </button>
              </div>
            </div>
          </div>

        </div>
      </main>
    </div>
  );
}