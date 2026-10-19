import React, { useState, useEffect } from 'react';
import { 
  Eye, LayoutDashboard, CreditCard, Calendar, BarChart2, RefreshCcw, Users, LogOut, 
  User, Settings, ChevronDown, X, Plus, Layers, 
  AlertCircle
} from 'lucide-react';
import { performLogout } from '../services/httpClient';
import { useNavigate } from 'react-router-dom';
import { getAllSectionTypes } from '../services/SectionTypeService';
import { httpRequest } from '../services/httpClient';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export default function AdminSectionTypeManagement() {
  const navigate = useNavigate();
  
  const [sectionTypes, setSectionTypes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  const [formData, setFormData] = useState({
    name: ''
  });

  const userData = {
    name: localStorage.getItem('name') || localStorage.getItem('fullName') || 'Usuario',
    email: localStorage.getItem('userEmail') || '',
    role: localStorage.getItem('role') === 'ADMIN' ? 'Super Admin' : 'Admin',
  };

  const handleLogout = async () => {
    await performLogout();
    window.location.href = '/login';
  };

  const handleMenuClick = (action) => {
    setShowUserMenu(false);
    if (action === 'profile') navigate('/profile');
    if (action === 'settings') navigate('/settings');
    if (action === 'logout') handleLogout();
  };

  useEffect(() => {
    fetchSectionTypes();
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

  const fetchSectionTypes = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await getAllSectionTypes();
      setSectionTypes(response.data || []);
    } catch (err) {
      setError('Error al cargar tipos de sección: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateSectionType = async () => {
    if (!formData.name.trim()) return;
    
    setActionLoading(true);
    try {
      const response = await httpRequest(`${API_URL}/section-types`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: formData.name.trim()
        }),
      });
      
      if (response.ok) {
        setShowCreateModal(false);
        resetForm();
        await fetchSectionTypes();
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Error al crear tipo de sección');
      }
    } catch (err) {
      setError('Error al crear tipo de sección: ' + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const openCreateModal = () => {
    resetForm();
    setShowCreateModal(true);
  };

  const resetForm = () => {
    setFormData({
      name: ''
    });
  };

  const navItems = [
    { icon: LayoutDashboard, label: 'Dashboard',          href: '/admin-dashboard' },
    { icon: CreditCard,      label: 'Control Financiero', href: '/admin-financial' },
    { icon: Calendar,        label: 'Eventos',            href: '/admin-events' },
    { icon: BarChart2,       label: 'Reportes' },
    { icon: RefreshCcw,      label: 'Reembolsos' },
    { icon: Users,           label: 'Solicitudes',        href: '/admin' },
    { icon: Settings,        label: 'Categorías',         href: '/admin-categories' },
    { icon: Layers,          label: 'Tipos de Sección',   active: true },
  ];

  return (
    <div className="min-h-screen flex bg-background text-textPrimary font-sans">

      {/* Sidebar */}
      <aside className="w-52 flex-shrink-0 flex flex-col justify-between px-3 py-6 bg-background border-r border-surfaceAlt/30 h-screen sticky top-0">
        <nav className="flex flex-col gap-0.5">
          {navItems.map((item, idx) => (
            <a
              key={idx}
              href={item.href || '#'}
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

        {/* User Profile Card */}
        <div className="pt-4 border-t border-surfaceAlt/40 relative user-menu-container flex-shrink-0">
          <button
            onClick={() => setShowUserMenu(!showUserMenu)}
            className="w-full flex items-center gap-3 p-2 rounded-xl hover:bg-surfaceAlt/40 transition-all group"
          >
            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary to-primaryDark flex items-center justify-center text-textPrimary font-bold text-sm flex-shrink-0">
              {userData.name.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 text-left min-w-0">
              <p className="text-sm font-semibold text-textPrimary truncate">{userData.name}</p>
              <p className="text-xs text-textMuted truncate">{userData.role}</p>
            </div>
            <ChevronDown className={`w-4 h-4 text-textMuted transition-transform ${showUserMenu ? 'rotate-180' : ''}`} />
          </button>

          {showUserMenu && (
            <div className="absolute bottom-full left-0 right-0 mb-2 mx-2 bg-surface border border-surfaceAlt rounded-xl overflow-hidden shadow-xl z-50 user-menu-container">
              <button
                onClick={() => handleMenuClick('profile')}
                className="w-full flex items-center gap-3 px-4 py-3 text-sm text-textSecondary hover:bg-surfaceAlt/40 hover:text-textPrimary transition-colors"
              >
                <User className="w-4 h-4" />
                Perfil
              </button>
              <button
                onClick={() => handleMenuClick('settings')}
                className="w-full flex items-center gap-3 px-4 py-3 text-sm text-textSecondary hover:bg-surfaceAlt/40 hover:text-textPrimary transition-colors"
              >
                <Settings className="w-4 h-4" />
                Configuración
              </button>
              <div className="border-t border-surfaceAlt/40">
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

      {/* Main */}
      <main className="flex-1 overflow-y-auto bg-background">
        <div className="p-8 max-w-6xl mx-auto w-full">

          {/* Page header */}
          <div className="mb-6">
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-2xl font-black uppercase tracking-wide text-textPrimary mb-1">
                  Gestión de Tipos de Sección
                </h1>
                <p className="text-textSecondary text-sm leading-relaxed max-w-xl">
                  Gestiona los tipos de sección del sistema. Define categorías como General, VIP, Palco, Tribuna, etc.
                </p>
              </div>
              <button
                onClick={openCreateModal}
                className="flex items-center gap-2 px-4 py-2.5 bg-primary hover:bg-primaryDark text-textPrimary font-semibold rounded-xl transition-all"
              >
                <Plus className="w-4 h-4" />
                Nuevo Tipo de Sección
              </button>
            </div>
          </div>

          {/* Error Alert */}
          {error && (
            <div className="mb-6 p-4 bg-error/10 border border-error/30 text-error rounded-xl flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span className="text-sm">{error}</span>
              <button 
                onClick={() => setError(null)}
                className="ml-auto p-1 hover:bg-error/20 rounded-full"
              >
                <X className="w-3 h-3" />
              </button>
            </div>
          )}

          {/* Section Types Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {loading ? (
              Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="bg-surface border border-surfaceAlt rounded-xl p-4 animate-pulse">
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <div className="w-8 h-8 bg-surfaceAlt rounded-lg"></div>
                      <div className="w-24 h-4 bg-surfaceAlt rounded"></div>
                    </div>
                  </div>
                </div>
              ))
            ) : sectionTypes.length === 0 ? (
              <div className="col-span-full text-center py-12 text-textMuted">
                <Layers className="w-12 h-12 mx-auto mb-4 opacity-50" />
                <p>No hay tipos de sección para mostrar</p>
              </div>
            ) : (
              sectionTypes.map((sectionType) => {
                return (
                  <div
                    key={sectionType.sectionTypeId}
                    className="bg-surface border border-surfaceAlt rounded-xl p-4 hover:border-primary/40 transition-all group"
                  >
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex items-center gap-2 flex-1 min-w-0">
                        <div className="w-8 h-8 bg-primary/20 rounded-lg flex items-center justify-center flex-shrink-0">
                          <Layers className="w-4 h-4 text-primary" />
                        </div>
                        <div className="min-w-0 flex-1">
                          <h3 className="font-semibold text-textPrimary truncate">
                            {sectionType.name}
                          </h3>
                          <p className="text-xs text-textMuted mt-0.5">
                            ID: {sectionType.sectionTypeId}
                          </p>
                        </div>
                      </div>
                    </div>

                    <div className="mt-3 p-2 rounded-lg bg-emerald-500/10 border border-emerald-500/20">
                      <div className="flex items-center gap-1 text-xs text-emerald-400">
                        <span className="font-semibold">Tipo de sección activo</span>
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>

        </div>
      </main>

      {/* Create Section Type Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-surface rounded-2xl border border-surfaceAlt shadow-2xl max-w-md w-full">
            <div className="flex items-center justify-between px-6 py-4 border-b border-surfaceAlt">
              <h2 className="text-lg font-bold text-textPrimary">Crear Nuevo Tipo de Sección</h2>
              <button
                onClick={() => setShowCreateModal(false)}
                className="p-1.5 hover:bg-surfaceAlt rounded-full transition-colors text-textMuted"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-textPrimary mb-2">
                  Nombre del Tipo de Sección *
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="Ej: General, VIP, Palco, Tribuna..."
                  className="w-full px-3 py-2 bg-background border border-surfaceAlt rounded-lg text-textPrimary placeholder-textMuted focus:border-primary focus:outline-none"
                />
              </div>

              <div className="p-3 bg-amber-500/10 border border-amber-500/20 rounded-lg">
                <p className="text-xs text-amber-400">
                  <strong>Nota:</strong> Los tipos de sección se usan para categorizar las áreas de un evento (ej: VIP, General, Palco). Asegúrate de usar nombres claros y descriptivos.
                </p>
              </div>
            </div>
            
            <div className="flex gap-3 px-6 py-4 border-t border-surfaceAlt">
              <button
                onClick={() => setShowCreateModal(false)}
                className="flex-1 py-2 px-4 border border-surfaceAlt rounded-lg text-textSecondary hover:bg-surfaceAlt transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={handleCreateSectionType}
                disabled={!formData.name.trim() || actionLoading}
                className="flex-1 py-2 px-4 bg-primary hover:bg-primaryDark text-textPrimary rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {actionLoading ? 'Creando...' : 'Guardar Tipo'}
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
