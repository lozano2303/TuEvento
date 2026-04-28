import { Calendar, User, LogOut, Key, Plus, Wallet } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import ChangePassword from "../pages/ChangePassword.jsx";
import { useTranslation } from "../context/TranslationContext";

export default function Navbar() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [userData, setUserData] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [showChangePasswordModal, setShowChangePasswordModal] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const storedUserID = localStorage.getItem('userID');
    const storedAlias = localStorage.getItem('alias');
    const storedName = localStorage.getItem('name');
    const storedRole = localStorage.getItem('role');
    const storedEmail = localStorage.getItem('userEmail');
    if (token && storedUserID) {
      setUserData({ userId: storedUserID, alias: storedAlias || storedName, fullName: storedName, role: storedRole, email: storedEmail });
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
    const isOrganizerOrAdmin = userData.organizer || userData.role === 'ADMIN' || userData.email === 'tuevento.capysoft@gmail.com';
    navigate(isOrganizerOrAdmin ? '/event-management' : '/organizer-petition');
  };

  const isAdmin = userData?.role === 'ADMIN' || userData?.email === 'tuevento.capysoft@gmail.com';
  const isOrganizer = userData?.organizer;
  const isOrganizerOrAdmin = isOrganizer || isAdmin;

  const roleBadge = isAdmin
    ? { label: t('roleAdmin'), bg: 'bg-red-500' }
    : isOrganizer
    ? { label: t('roleOrganizer'), bg: 'bg-blue-500' }
    : { label: t('roleUser'), bg: 'bg-gray-500' };

  return (
    <>
      <header className="bg-background border-b border-surfaceAlt shadow-lg sticky top-0 z-40">
        <nav className="max-w-6xl mx-auto px-6 py-3 flex items-center justify-between">

          {/* Logo / Nombre */}
          <Link to="/" className="flex items-center gap-2 group">
            <span className="text-xl font-bold text-textPrimary group-hover:text-accent transition-colors">
              Tu <span className="text-accent group-hover:text-textPrimary transition-colors">Evento</span>
            </span>
          </Link>

          {/* Links centrales */}
          <div className="hidden md:flex items-center gap-12 text-sm font-medium flex-1 justify-center">
            <Link to="/" className="text-textSecondary hover:text-accent transition-colors">
              {t('home')}
            </Link>
            <Link to="/nosotros" className="text-textSecondary hover:text-accent transition-colors">
              {t('aboutUs')}
            </Link>
            <Link to="/events" className="text-textSecondary hover:text-accent transition-colors">
              {t('events')}
            </Link>
            {userData && isOrganizer && (
              <button
                onClick={() => navigate('/event-management')}
                className="flex items-center gap-1 text-textSecondary hover:text-accent transition-colors bg-transparent border-none cursor-pointer text-sm font-medium"
              >
                <Plus className="w-4 h-4" />
                {t('createEvent')}
              </button>
            )}
            {userData && !isAdmin && !isOrganizer && (
              <button
                onClick={() => navigate('/organizer-petition')}
                className="flex items-center gap-1 text-textSecondary hover:text-accent transition-colors bg-transparent border-none cursor-pointer text-sm font-medium"
              >
                <Plus className="w-4 h-4" />
                {t('organizerPetition')}
              </button>
            )}
          </div>

          {/* Lado derecho */}
          <div className="flex items-center gap-3">

            {/* Usuario logueado */}
            {userData ? (
              <div className="relative user-modal">
                <button
                  onClick={() => setIsModalOpen(!isModalOpen)}
                  className="user-pill text-textPrimary transition-colors"
                >
                  <span className="name">
                    {(() => {
                      const name = userData.fullName || userData.alias || '';
                      const parts = name.split(' ');
                      return parts.length >= 2 ? parts.slice(0, 2).join(' ') : (parts[0] || userData.alias || name);
                    })()}
                  </span>
                  <span className={`role ${roleBadge.bg}`}>
                    {roleBadge.label}
                  </span>
                </button>

                {/* Dropdown */}
                {isModalOpen && (
                  <div className="user-modal absolute right-0 mt-2 w-64 bg-surface border border-surfaceAlt rounded-2xl shadow-2xl z-50 overflow-hidden">
                    
                    {/* Opciones */}
                    <div className="p-3 space-y-1.5">
                      

                      {isOrganizer && (
                        <Link
                          to="/event-management"
                          onClick={() => setIsModalOpen(false)}
                          className="w-full bg-blue-600 hover:bg-blue-700 text-white text-sm py-2 px-4 rounded-xl transition-colors flex items-center gap-2"
                        >
                          <Calendar className="w-4 h-4" />
                          {t('myEvents')}
                        </Link>
                        
                      )}

                      <Link
                        to="/profile"
                        onClick={() => setIsModalOpen(false)}
                        className="w-full bg-primary hover:bg-primaryDark text-textPrimary text-sm py-2 px-4 rounded-xl transition-colors flex items-center gap-2"
                      >
                        <User className="w-4 h-4" />
                        {t('profile')}
                      </Link>

                      {isAdmin && (
                        <Link
                          to="/admin-dashboard"
                          onClick={() => setIsModalOpen(false)}
                          className="w-full bg-error hover:bg-error text-textPrimary text-sm py-2 px-4 rounded-xl transition-colors flex items-center gap-2"
                        >
                          <Calendar className="w-4 h-4" />
                          Panel de Gestión
                        </Link>
                      )}

                      {!isAdmin && (
                        <Link
                          to="/wallet"
                          onClick={() => setIsModalOpen(false)}
                          className="w-full bg-success hover:bg-success text-textPrimary text-sm py-2 px-4 rounded-xl transition-colors flex items-center gap-2"
                        >
                          <Wallet className="w-4 h-4" />
                          {t('wallet')}
                        </Link>
                      )}
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <Link
                to="/login"
                className="bg-primary hover:bg-primaryDark text-textPrimary text-sm font-medium px-5 py-2 rounded-xl transition-colors"
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