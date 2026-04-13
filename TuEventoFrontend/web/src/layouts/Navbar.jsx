import { Calendar, User, LogOut, Key, Plus } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import ChangePassword from "../pages/ChangePassword.jsx";

export default function Navbar() {
  const navigate = useNavigate();
  const [userData, setUserData] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [showChangePasswordModal, setShowChangePasswordModal] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const storedUserID = localStorage.getItem('userID');
    const storedAlias = localStorage.getItem('alias');
    const storedName = localStorage.getItem('name');
    if (token && storedUserID) {
      setUserData({ userId: storedUserID, alias: storedAlias, fullName: storedName });
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

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userID');
    localStorage.removeItem('role');
    localStorage.removeItem('pendingActivationUserID');
    localStorage.removeItem('adminLoggedIn');
    setUserData(null);
    setIsModalOpen(false);
    window.location.reload();
  };

  const handleCreateClick = () => {
    if (!userData) { navigate('/login'); return; }
    const isOrganizerOrAdmin = userData.organizer || userData.role === 'ADMIN' || userData.email === 'atuevento72@gmail.com';
    navigate(isOrganizerOrAdmin ? '/event-management' : '/organizer-petition');
  };

  const isAdmin = userData?.role === 'ADMIN' || userData?.email === 'atuevento72@gmail.com';
  const isOrganizer = userData?.organizer;
  const isOrganizerOrAdmin = isOrganizer || isAdmin;

  const roleBadge = isAdmin
    ? { label: 'Admin', bg: 'bg-red-500' }
    : isOrganizer
    ? { label: 'Org', bg: 'bg-blue-500' }
    : { label: 'User', bg: 'bg-gray-500' };

  return (
    <>
      <header className="bg-gray-900 border-b border-gray-700 shadow-lg sticky top-0 z-40">
        <nav className="max-w-6xl mx-auto px-6 py-3 flex items-center justify-between">

          {/* Logo / Nombre */}
          <Link to="/" className="flex items-center gap-2 group">
            <span className="text-xl font-bold text-white group-hover:text-purple-400 transition-colors">
              Tu <span className="text-purple-400 group-hover:text-white transition-colors">Evento</span>
            </span>
          </Link>

          {/* Links centrales */}
          <div className="hidden md:flex items-center gap-8 text-sm font-medium">
            <Link to="/" className="text-gray-300 hover:text-purple-400 transition-colors">
              Inicio
            </Link>
            <Link to="/nosotros" className="text-gray-300 hover:text-purple-400 transition-colors">
              Nosotros
            </Link>
            <Link to="/events" className="text-gray-300 hover:text-purple-400 transition-colors">
              Eventos
            </Link>
            {userData && (
              <button
                onClick={handleCreateClick}
                className="flex items-center gap-1 text-gray-300 hover:text-purple-400 transition-colors bg-transparent border-none cursor-pointer text-sm font-medium"
              >
                <Plus className="w-4 h-4" />
                {isOrganizerOrAdmin ? "Crear" : "Enviar solicitud"}
              </button>
            )}
          </div>

          {/* Lado derecho */}
          <div className="flex items-center gap-3">

            {/* Botón Admin */}
            {isAdmin && (
              <Link
                to="/admin-dashboard"
                className="bg-red-600 hover:bg-red-700 text-white text-sm font-medium px-4 py-2 rounded-xl transition-colors"
              >
                Admin
              </Link>
            )}

            {/* Usuario logueado */}
            {userData ? (
              <div className="relative user-modal">
                <button
                  onClick={() => setIsModalOpen(!isModalOpen)}
                  className="flex items-center gap-2 bg-purple-600 hover:bg-purple-700 text-white text-sm font-medium px-4 py-2 rounded-xl transition-colors"
                >
                  <User className="w-4 h-4" />
                  <span>{userData.fullName ? userData.fullName.split(' ')[0] : userData.alias}</span>
                  <span className={`text-xs px-1.5 py-0.5 rounded-full ${roleBadge.bg}`}>
                    {roleBadge.label}
                  </span>
                </button>

                {/* Dropdown */}
                {isModalOpen && (
                  <div className="user-modal absolute right-0 mt-2 w-64 bg-gray-800 border border-gray-700 rounded-2xl shadow-2xl z-50 overflow-hidden">
                    
                    {/* Header del dropdown */}
                    <div className="bg-gradient-to-r from-purple-600 to-purple-800 p-4 text-center">
                      <div className="w-12 h-12 bg-white bg-opacity-20 rounded-full flex items-center justify-center mx-auto mb-2">
                        <User className="w-6 h-6 text-white" />
                      </div>
                      <h3 className="text-white font-semibold text-sm">{userData.fullName}</h3>
                      <p className="text-purple-200 text-xs mt-0.5">{userData.email}</p>
                      <span className={`inline-block text-xs px-2 py-0.5 rounded-full font-medium mt-2 ${
                        isAdmin ? 'bg-red-500 text-white' :
                        isOrganizer ? 'bg-blue-500 text-white' :
                        'bg-gray-500 text-white'
                      }`}>
                        {isAdmin ? 'Administrador' : isOrganizer ? 'Organizador' : 'Usuario'}
                      </span>
                    </div>

                    {/* Opciones */}
                    <div className="p-3 space-y-1.5">
                      {isAdmin && (
                        <Link
                          to="/admin-dashboard"
                          onClick={() => setIsModalOpen(false)}
                          className="w-full bg-red-600 hover:bg-red-700 text-white text-sm py-2 px-4 rounded-xl transition-colors flex items-center gap-2"
                        >
                          <User className="w-4 h-4" />
                          Panel Admin
                        </Link>
                      )}

                      {isOrganizer && (
                        <Link
                          to="/event-management"
                          onClick={() => setIsModalOpen(false)}
                          className="w-full bg-blue-600 hover:bg-blue-700 text-white text-sm py-2 px-4 rounded-xl transition-colors flex items-center gap-2"
                        >
                          <Calendar className="w-4 h-4" />
                          Gestionar Eventos
                        </Link>
                      )}

                      <Link
                        to="/profile"
                        onClick={() => setIsModalOpen(false)}
                        className="w-full bg-purple-600 hover:bg-purple-700 text-white text-sm py-2 px-4 rounded-xl transition-colors flex items-center gap-2"
                      >
                        <User className="w-4 h-4" />
                        Mi Perfil
                      </Link>

                      <button
                        onClick={() => { setIsModalOpen(false); setShowChangePasswordModal(true); }}
                        className="w-full bg-gray-700 hover:bg-gray-600 text-white text-sm py-2 px-4 rounded-xl transition-colors flex items-center gap-2"
                      >
                        <Key className="w-4 h-4" />
                        Cambiar contraseña
                      </button>

                      <button
                        onClick={handleLogout}
                        className="w-full bg-orange-600 hover:bg-orange-700 text-white text-sm py-2 px-4 rounded-xl transition-colors flex items-center gap-2"
                      >
                        <LogOut className="w-4 h-4" />
                        Cerrar sesión
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
          </div>
        </nav>
      </header>

      {showChangePasswordModal && (
        <ChangePassword onClose={() => setShowChangePasswordModal(false)} />
      )}
    </>
  );
}