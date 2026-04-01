
import { Calendar, User, LogOut, Key, Plus } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import { getUserById } from "../services/Login.js";
import ChangePassword from "../pages/ChangePassword.jsx";

export default function Navbar() {
  const navigate = useNavigate();
  const [userData, setUserData] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [showChangePasswordModal, setShowChangePasswordModal] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const storedUserID = localStorage.getItem('userID');
    if (token && storedUserID) {
      getUserById(storedUserID).then(result => {
        if (result.success) {
          console.log('UserData from backend:', result.data);
          console.log('Role from backend:', result.data.role);
          console.log('Email from backend:', result.data.email);
          setUserData(result.data);
        } else {
          // Limpiar todos los datos de autenticación del localStorage
          localStorage.removeItem('token');
          localStorage.removeItem('userID');
          localStorage.removeItem('role');
          localStorage.removeItem('pendingActivationUserID');
          localStorage.removeItem('adminLoggedIn');
        }
      }).catch(() => {
        // Limpiar todos los datos de autenticación del localStorage
        localStorage.removeItem('token');
        localStorage.removeItem('userID');
        localStorage.removeItem('role');
        localStorage.removeItem('pendingActivationUserID');
        localStorage.removeItem('adminLoggedIn');
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

  const handleLogout = () => {
    // Limpiar todos los datos de autenticación del localStorage
    localStorage.removeItem('token');
    localStorage.removeItem('userID');
    localStorage.removeItem('role');
    localStorage.removeItem('pendingActivationUserID');
    localStorage.removeItem('adminLoggedIn');
    setUserData(null);
    setIsModalOpen(false);
    window.location.reload(); // Recargar para actualizar estado
  };

  const handleCreateClick = () => {
    if (!userData) {
      // Usuario no logueado
      navigate('/login');
      return;
    }

    // Verificar si es organizador o admin
    const isOrganizerOrAdmin = userData.organizer || userData.role === 'ADMIN' || userData.email === 'atuevento72@gmail.com';
    if (isOrganizerOrAdmin) {
      // Es organizador o admin, ir a gestión de eventos
      navigate('/event-management');
    } else {
      // No es organizador ni admin, ir a solicitud
      navigate('/organizer-petition');
    }
  };

  return (
    <header className="bg-gray-800 p-4">
      <nav className="max-w-6xl mx-auto flex items-center justify-between">
        <div className="flex items-center space-x-2 -translate-x-4">
          <img src="/src/assets/images/logo2.jpg" alt="Logo" className="w-8 h-8" />
          <span className="text-xl font-bold text-white">Tu Evento</span>
        </div>

        <div className="hidden md:flex space-x-6 text-white">
          <Link to="/landingPage" className="hover:text-purple-400 transition-colors">Inicio</Link>
          <Link to="/nosotros" className="hover:text-purple-400 transition-colors">Nosotros</Link>
          {userData && (
            <button
              onClick={handleCreateClick}
              className="hover:text-purple-400 transition-colors bg-transparent border-none cursor-pointer"
            >
              <Plus className="w-4 h-4 inline mr-1" />
              {(userData.organizer || userData.role === 'ADMIN' || userData.email === 'atuevento72@gmail.com') ? "Crear" : "Enviar solicitud"}
            </button>
          )}
          <Link to="/events" className="hover:text-purple-400 transition-colors">Eventos</Link>
        </div>

        <div className="flex space-x-2">
          {userData && (userData.role === 'ADMIN' || userData.email === 'atuevento72@gmail.com') && (
            <button className="bg-red-600 hover:bg-red-700 px-4 py-2 rounded-lg transition-colors text-white">
              <Link to="/admin-dashboard">Admin</Link>
            </button>
          )}
          {userData ? (
            <div className="relative">
              <button
                onClick={() => setIsModalOpen(!isModalOpen)}
                className="bg-purple-600 hover:bg-purple-700 px-4 py-2 rounded-lg transition-colors text-white flex items-center space-x-2"
              >
                <User className="w-4 h-4" />
                <span>{userData.fullName.split(' ')[0]}</span>
                <span className={`text-xs px-2 py-1 rounded-full ${
                  (userData.role === 'ADMIN' || userData.email === 'atuevento72@gmail.com') ? 'bg-red-500' :
                  userData.organizer ? 'bg-blue-500' : 'bg-gray-500'
                }`}>
                  {(userData.role === 'ADMIN' || userData.email === 'atuevento72@gmail.com') ? 'Admin' :
                   userData.organizer ? 'Org' : 'User'}
                </span>
              </button>
              {isModalOpen && (
                <div className="user-modal absolute right-0 mt-2 w-64 bg-gray-800 border border-gray-700 rounded-lg shadow-lg z-50">
                  <div className="p-4">
                    <div className="text-center mb-4">
                      <User className="w-12 h-12 mx-auto text-purple-400 mb-2" />
                      <h3 className="text-white font-semibold">{userData.fullName}</h3>
                      <p className="text-gray-400 text-sm">{userData.email}</p>
                      <div className="flex items-center justify-center space-x-2">
                        <span className="text-gray-400 text-sm">Rol:</span>
                        <span className={`text-xs px-2 py-1 rounded-full font-medium ${
                          (userData.role === 'ADMIN' || userData.email === 'atuevento72@gmail.com') ? 'bg-red-500 text-white' :
                          userData.organizer ? 'bg-blue-500 text-white' :
                          'bg-gray-500 text-white'
                        }`}>
                          {(userData.role === 'ADMIN' || userData.email === 'atuevento72@gmail.com') ? 'Administrador' :
                           userData.organizer ? 'Organizador' :
                           'Usuario'}
                        </span>
                      </div>
                    </div>
                    <div className="space-y-2">
                      {/* Opciones específicas por rol */}
                      {(userData.role === 'ADMIN' || userData.email === 'atuevento72@gmail.com') && (
                        <Link
                          to="/admin-dashboard"
                          onClick={() => setIsModalOpen(false)}
                          className="w-full bg-red-600 hover:bg-red-700 text-white py-2 px-4 rounded-lg transition-colors flex items-center justify-center space-x-2"
                        >
                          <User className="w-4 h-4" />
                          <span>Panel Admin</span>
                        </Link>
                      )}

                      {userData.organizer && (
                        <Link
                          to="/event-management"
                          onClick={() => setIsModalOpen(false)}
                          className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2 px-4 rounded-lg transition-colors flex items-center justify-center space-x-2"
                        >
                          <Calendar className="w-4 h-4" />
                          <span>Gestionar Eventos</span>
                        </Link>
                      )}

                      <Link
                        to="/profile"
                        onClick={() => setIsModalOpen(false)}
                        className="w-full bg-purple-600 hover:bg-purple-700 text-white py-2 px-4 rounded-lg transition-colors flex items-center justify-center space-x-2"
                      >
                        <User className="w-4 h-4" />
                        <span>Mi Perfil</span>
                      </Link>

                      <button
                        onClick={() => {
                          setIsModalOpen(false);
                          setShowChangePasswordModal(true);
                        }}
                        className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2 px-4 rounded-lg transition-colors flex items-center justify-center space-x-2"
                      >
                        <Key className="w-4 h-4" />
                        <span>Cambiar contraseña</span>
                      </button>

                      <button
                        onClick={handleLogout}
                        className="w-full bg-orange-600 hover:bg-orange-700 text-white py-2 px-4 rounded-lg transition-colors flex items-center justify-center space-x-2"
                      >
                        <LogOut className="w-4 h-4" />
                        <span>Cerrar sesión</span>
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <button className="bg-purple-600 hover:bg-purple-700 px-4 py-2 rounded-lg transition-colors text-white">
              <Link to="/login">Iniciar sesión</Link>
            </button>
          )}
        </div>
      </nav>

      {showChangePasswordModal && (
        <ChangePassword
          onClose={() => setShowChangePasswordModal(false)}
        />
      )}
    </header>
  );
}