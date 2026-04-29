import { Calendar, User, LogOut, Key, Plus, Wallet, Menu, X } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import ChangePassword from "../pages/ChangePassword.jsx";

export default function Navbar() {
  const navigate = useNavigate();
  const [userData, setUserData] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [showChangePasswordModal, setShowChangePasswordModal] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const storedUserID = localStorage.getItem('userID');
    const storedAlias = localStorage.getItem('alias');
    const storedName = localStorage.getItem('name');
    const storedRole = localStorage.getItem('role');
    const storedEmail = localStorage.getItem('userEmail');
    if (token && storedUserID) {
      setUserData({
        userId: storedUserID,
        alias: storedAlias,
        fullName: storedName || storedAlias,
        role: storedRole,
        email: storedEmail
      });
    }
  }, []);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (isModalOpen && !event.target.closest('.user-modal')) {
        setIsModalOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isModalOpen]);

  // Cerrar menú móvil al cambiar de tamaño a desktop
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth >= 768) setIsMobileMenuOpen(false);
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userID');
    localStorage.removeItem('role');
    localStorage.removeItem('pendingActivationUserID');
    localStorage.removeItem('adminLoggedIn');
    setUserData(null);
    setIsModalOpen(false);
    setIsMobileMenuOpen(false);
    window.location.reload();
  };

  const handleNavClick = (path) => {
    setIsMobileMenuOpen(false);
    navigate(path);
  };

  const isAdmin = userData?.role === 'ADMIN' || userData?.email === 'tuevento.capysoft@gmail.com';
  const isOrganizer = userData?.organizer;

  const roleBadge = isAdmin
    ? { label: 'Admin', badgeClass: 'bg-red-500 text-white', roleText: 'Administrador' }
    : isOrganizer
    ? { label: 'Org', badgeClass: 'bg-violet-500 text-white', roleText: 'Organizador' }
    : { label: 'User', badgeClass: 'bg-gray-500 text-white', roleText: 'Usuario' };

  const displayName = (() => {
    const name = userData?.fullName || userData?.alias || '';
    const parts = name.split(' ').filter(p => p.trim().length > 0);
    if (parts.length === 0) return userData?.alias || name;
    if (parts.length === 1) return parts[0];
    const firstName = parts[0];
    const lastName = parts[1];
    if (firstName.length <= 3) return lastName.length > firstName.length ? lastName : firstName;
    return firstName;
  })();

  return (
    <>
      <header className="bg-gray-900 border-b border-gray-700 shadow-lg sticky top-0 z-40">
        <nav className="max-w-6xl mx-auto px-4 md:px-6 py-3 flex items-center justify-between">
          
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2 group flex-shrink-0">
            <span className="text-xl font-bold text-white group-hover:text-purple-400 transition-colors">
              Tu <span className="text-purple-400 group-hover:text-white transition-colors">Evento</span>
            </span>
          </Link>

          {/* Links centrales — solo desktop */}
          <div className="hidden md:flex items-center gap-8 text-sm font-medium flex-1 justify-center">
            <Link to="/" className="text-gray-300 hover:text-purple-400 transition-colors">Inicio</Link>
            <Link to="/nosotros" className="text-gray-300 hover:text-purple-400 transition-colors">Nosotros</Link>
            <Link to="/events" className="text-gray-300 hover:text-purple-400 transition-colors">Eventos</Link>
            {userData && isOrganizer && (
              <button
                onClick={() => navigate('/event-management')}
                className="flex items-center gap-1 text-gray-300 hover:text-purple-400 transition-colors bg-transparent border-none cursor-pointer text-sm font-medium"
              >
                <Plus className="w-4 h-4" /> Crear Evento
              </button>
            )}
            {userData && !isAdmin && !isOrganizer && (
              <button
                onClick={() => navigate('/organizer-petition-form')}
                className="flex items-center gap-1 text-gray-300 hover:text-purple-400 transition-colors bg-transparent border-none cursor-pointer text-sm font-medium"
              >
                <Plus className="w-4 h-4" /> Solicitud Organizador
              </button>
            )}
          </div>

          {/* Derecha: usuario + hamburguesa */}
          <div className="flex items-center gap-2">
            {/* Botón usuario */}
            {userData ? (
              <div className="relative user-modal">
                <button
                  onClick={() => setIsModalOpen(!isModalOpen)}
                  className="group inline-flex items-center gap-2 pl-1.5 pr-3 py-1.5 rounded-full bg-purple-600 hover:bg-purple-700 transition-all duration-200 outline-none cursor-pointer"
                >
                  <span className="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 bg-white/20">
                    <User className="w-4 h-4 text-white" />
                  </span>
                  <span className="text-[13px] font-medium text-white leading-none">
                    {displayName}
                  </span>
                  <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold uppercase ${roleBadge.badgeClass}`}>
                    {roleBadge.label}
                  </span>
                </button>
                {isModalOpen && (
                  <div className="user-modal absolute right-0 mt-2 w-64 bg-gray-800 border border-gray-700 rounded-2xl shadow-2xl z-50 overflow-hidden">
                    <div className="p-3 space-y-1.5">
                      <Link
                        to="/profile"
                        onClick={() => setIsModalOpen(false)}
                        className="w-full bg-purple-600 hover:bg-purple-700 text-white text-sm py-2 px-4 rounded-xl transition-colors flex items-center gap-2"
                      >
                        <User className="w-4 h-4" /> Perfil
                      </Link>
                      {isAdmin && (
                        <Link
                          to="/admin-panel"
                          onClick={() => setIsModalOpen(false)}
                          className="w-full bg-red-600 hover:bg-red-700 text-white text-sm py-2 px-4 rounded-xl transition-colors flex items-center gap-2"
                        >
                          <Calendar className="w-4 h-4" /> Panel de Gestión
                        </Link>
                      )}
                      <button
                        onClick={handleLogout}
                        className="w-full bg-red-600 hover:bg-red-700 text-white text-sm py-2 px-4 rounded-xl transition-colors flex items-center gap-2"
                      >
                        <LogOut className="w-4 h-4" /> Cerrar sesión
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <Link
                to="/login"
                className="bg-purple-600 hover:bg-purple-700 text-white text-sm font-medium px-5 py-2 rounded-xl transition-colors"
              >
                Iniciar sesión
              </Link>
            )}

            {/* Hamburguesa — solo mobile */}
            <button
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              className="md:hidden flex items-center justify-center w-9 h-9 rounded-xl border border-gray-700 text-gray-300 hover:text-white hover:border-gray-500 transition-colors"
            >
              {isMobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </nav>

        {/* Menú móvil desplegable */}
        {isMobileMenuOpen && (
          <div className="md:hidden bg-gray-800 border-t border-gray-700 px-4 py-3 flex flex-col gap-1">
            <Link
              to="/"
              onClick={() => setIsMobileMenuOpen(false)}
              className="text-gray-300 hover:text-purple-400 hover:bg-gray-700 transition-colors text-sm font-medium px-3 py-2.5 rounded-xl"
            >
              Inicio
            </Link>
            <Link
              to="/nosotros"
              onClick={() => setIsMobileMenuOpen(false)}
              className="text-gray-300 hover:text-purple-400 hover:bg-gray-700 transition-colors text-sm font-medium px-3 py-2.5 rounded-xl"
            >
              Nosotros
            </Link>
            <Link
              to="/events"
              onClick={() => setIsMobileMenuOpen(false)}
              className="text-gray-300 hover:text-purple-400 hover:bg-gray-700 transition-colors text-sm font-medium px-3 py-2.5 rounded-xl"
            >
              Eventos
            </Link>
            {userData && isOrganizer && (
              <button
                onClick={() => handleNavClick('/event-management')}
                className="flex items-center gap-2 text-gray-300 hover:text-purple-400 hover:bg-gray-700 transition-colors text-sm font-medium px-3 py-2.5 rounded-xl w-full text-left bg-transparent border-none cursor-pointer"
              >
                <Plus className="w-4 h-4" /> Crear Evento
              </button>
            )}
            {userData && !isAdmin && !isOrganizer && (
              <button
                onClick={() => handleNavClick('/organizer-petition-form')}
                className="flex items-center gap-2 text-gray-300 hover:text-purple-400 hover:bg-gray-700 transition-colors text-sm font-medium px-3 py-2.5 rounded-xl w-full text-left bg-transparent border-none cursor-pointer"
              >
                <Plus className="w-4 h-4" /> Solicitud Organizador
              </button>
            )}
          </div>
        )}
      </header>

      {showChangePasswordModal && (
        <ChangePassword onClose={() => setShowChangePasswordModal(false)} />
      )}
    </>
  );
}