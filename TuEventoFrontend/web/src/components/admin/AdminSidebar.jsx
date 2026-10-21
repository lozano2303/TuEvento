import { useState, useEffect } from 'react';
import { 
  LayoutDashboard, CreditCard, Calendar, BarChart2, RefreshCcw, 
  Users, LogOut, User, Settings, ChevronDown, Layers, Languages,
  FileText, Globe
} from 'lucide-react';
import { performLogout } from '../../services/httpClient';
import { useNavigate } from 'react-router-dom';

/**
 * AdminSidebar - Componente estandarizado del sidebar para todas las páginas del admin
 * 
 * @param {string} activePage - Identificador de la página activa ('dashboard', 'financial', 'events', 'requests', 'categories', 'section-types')
 */
export default function AdminSidebar({ activePage }) {
  const [showUserMenu, setShowUserMenu] = useState(false);
  // Persistir el estado del dropdown de traducciones en localStorage
  const [showTranslations, setShowTranslations] = useState(() => {
    const saved = localStorage.getItem('adminSidebar_showTranslations');
    // Si la página activa es de traducciones, siempre mostrar el dropdown
    if (activePage?.startsWith('translation')) return true;
    // Si no, usar el valor guardado (por defecto false)
    return saved === 'true';
  });
  const navigate = useNavigate();

  const userData = {
    name: localStorage.getItem('name') || localStorage.getItem('fullName') || 'Usuario',
    email: localStorage.getItem('userEmail') || '',
    role: localStorage.getItem('role') === 'ADMIN' ? 'Super Admin' : 'Admin',
  };

  // Navegación estandarizada para todas las páginas admin
  const navItems = [
    { 
      id: 'dashboard', 
      icon: LayoutDashboard, 
      label: 'Dashboard', 
      href: '/admin-dashboard' 
    },
    { 
      id: 'financial', 
      icon: CreditCard, 
      label: 'Control Financiero', 
      href: '/admin-financial' 
    },
    { 
      id: 'events', 
      icon: Calendar, 
      label: 'Eventos', 
      href: '/admin-events' 
    },
    { 
      id: 'reports', 
      icon: BarChart2, 
      label: 'Reportes',
      href: '#',
      disabled: true
    },
    { 
      id: 'refunds', 
      icon: RefreshCcw, 
      label: 'Reembolsos',
      href: '#',
      disabled: true
    },
    { 
      id: 'requests', 
      icon: Users, 
      label: 'Solicitudes', 
      href: '/admin-panel' 
    },
    { 
      id: 'categories', 
      icon: Settings, 
      label: 'Categorías', 
      href: '/admin-categories' 
    },
    { 
      id: 'section-types', 
      icon: Layers, 
      label: 'Tipos de Sección', 
      href: '/admin-section-types' 
    },
  ];

  // Módulos de traducciones (sin idiomas anidados)
  // Las rutas apuntan a inglés por defecto (español es el idioma fuente, no se traduce)
  const translationModules = [
    { id: 'translation-events', label: 'Eventos', href: '/admin-translations/en/events', icon: Calendar },
    { id: 'translation-categories', label: 'Categorías', href: '/admin-translations/en/categories', icon: Settings },
    { id: 'translation-section-types', label: 'Tipos de Sección', href: '/admin-translations/en/section-types', icon: Layers },
    { id: 'translation-comments', label: 'Comentarios', href: '/admin-translations/en/comments', icon: FileText },
    { id: 'translation-profiles', label: 'Perfiles', href: '/admin-translations/en/profiles', icon: Users },
  ];

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
    const handleClickOutside = (e) => {
      if (showUserMenu && !e.target.closest('.user-menu-container')) {
        setShowUserMenu(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [showUserMenu]);

  // Persistir el estado del dropdown en localStorage cuando cambie
  useEffect(() => {
    localStorage.setItem('adminSidebar_showTranslations', showTranslations.toString());
  }, [showTranslations]);

  // Asegurarse de que el dropdown esté abierto si estamos en una página de traducciones
  useEffect(() => {
    if (activePage?.startsWith('translation') && !showTranslations) {
      setShowTranslations(true);
    }
  }, [activePage]);

  return (
    <aside className="w-52 flex-shrink-0 flex flex-col justify-between px-3 py-6 bg-background border-r border-surfaceAlt/30 h-screen sticky top-0 overflow-y-auto">
      {/* Logo + Navigation */}
      <div>
        {/* Logo */}
        <div className="flex items-center gap-2 px-3 mb-6">
          <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center flex-shrink-0">
            <span className="text-white font-black text-xs">TE</span>
          </div>
          <span className="text-sm font-bold text-textPrimary">Tu Evento</span>
        </div>

        {/* Navigation */}
        <nav className="flex flex-col gap-0.5">
          {navItems.map((item) => (
            <a
              key={item.id}
              href={item.disabled ? undefined : item.href}
              onClick={(e) => {
                if (item.disabled) {
                  e.preventDefault();
                }
              }}
              className={`flex items-center gap-2.5 px-3 py-2.5 rounded-full text-sm font-medium transition-colors
                ${item.id === activePage
                  ? 'bg-primary text-textPrimary'
                  : item.disabled
                    ? 'text-textMuted cursor-not-allowed opacity-50'
                    : 'text-textSecondary hover:text-textPrimary hover:bg-surfaceAlt/50'
                }`}
            >
              <item.icon className="w-4 h-4 flex-shrink-0" />
              {item.label}
            </a>
          ))}

          {/* Sección de Traducciones con dropdown simple */}
          <div className="mt-2 border-t border-surfaceAlt/40 pt-2">
            <button
              onClick={() => setShowTranslations(!showTranslations)}
              className={`w-full flex items-center gap-2.5 px-3 py-2.5 rounded-full text-sm font-medium transition-colors
                ${activePage?.startsWith('translation')
                  ? 'bg-primary text-textPrimary'
                  : 'text-textSecondary hover:text-textPrimary hover:bg-surfaceAlt/50'
                }`}
            >
              <Languages className="w-4 h-4 flex-shrink-0" />
              <span className="flex-1 text-left">Traducciones</span>
              <ChevronDown className={`w-3.5 h-3.5 transition-transform ${showTranslations ? 'rotate-180' : ''}`} />
            </button>

            {/* Lista de módulos traducibles */}
            {showTranslations && (
              <div className="mt-1 ml-2 space-y-0.5">
                {translationModules.map((module) => (
                  <a
                    key={module.id}
                    href={module.disabled ? undefined : module.href}
                    onClick={(e) => {
                      if (module.disabled) {
                        e.preventDefault();
                      }
                    }}
                    className={`flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs font-medium transition-colors
                      ${module.id === activePage
                        ? 'bg-primary/20 text-primary'
                        : module.disabled
                          ? 'text-textMuted cursor-not-allowed opacity-50'
                          : 'text-textSecondary hover:text-textPrimary hover:bg-surfaceAlt/30'
                      }`}
                  >
                    <module.icon className="w-3.5 h-3.5 flex-shrink-0" />
                    {module.label}
                  </a>
                ))}
              </div>
            )}
          </div>
        </nav>
      </div>

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

        {/* Dropdown Menu */}
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
  );
}
