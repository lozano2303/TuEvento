import { useState, useEffect } from 'react';
import Footer from '../layouts/Footer';
import { useTheme } from '../context/ThemeContext';
import { getThemes, activateTheme } from '../services/themeService';

const THEME_PREVIEWS = {
  DARK:       { background: "#1E0A3C", primary: "#7C3AED", accent: "#A78BFA" },
  LIGHT:      { background: "#FFFFFF", primary: "#7C3AED", accent: "#8B5CF6" },
  VIBRANT:    { background: "#0D0D0D", primary: "#FF4081", accent: "#FFEB3B" },
  ACCESSIBLE: { background: "#FFFFFF", primary: "#005FCC", accent: "#E65100" }
};

const ProfilePage = () => {
  const [selectedLanguage, setSelectedLanguage] = useState('es');
  const [loading, setLoading] = useState(false);
  const [themes, setThemes] = useState([]);
  const [loadingTheme, setLoadingTheme] = useState(false);
  const { refreshPalette, activeThemeId } = useTheme();

  const userEmail = localStorage.getItem('userEmail') || 'francisco@tuevento.com';
  const storedName = localStorage.getItem('name') || localStorage.getItem('fullName') || 'Francisco';
  const userRole = localStorage.getItem('role') || 'USER';

  const getDisplayName = (name) => {
    if (!name) return 'Usuario';
    
    const parts = name.split(' ').filter(part => part.trim().length > 0);
    
    if (parts.length === 0) return 'Usuario';
    if (parts.length === 1) return parts[0];
    
    const firstName = parts[0];
    const lastName = parts[1];
    
    if (firstName.length <= 3) {
      if (lastName.length > firstName.length) {
        return lastName;
      }
      return firstName;
    }
    
    return firstName;
  };
  
  const displayName = getDisplayName(storedName);
  const firstLetter = displayName ? displayName.charAt(0).toUpperCase() : 'F';

  const roleLabel = userRole === 'ADMIN' ? 'Administrador' : userRole === 'ORGANIZER' ? 'Organizador Premium' : 'Usuario';

  const [formData, setFormData] = useState({
    nombreCompleto: storedName || 'Francisco Rodríguez',
    telefono: '+34 600 000 000',
    fechaNacimiento: '',
    direccion: 'Calle Mayor, 1 Madrid'
  });

  useEffect(() => {
    loadThemes();
  }, []);

  const loadThemes = async () => {
    const token = localStorage.getItem('token');
    if (!token) return;
    
    try {
      const themesData = await getThemes();
      setThemes(themesData);
    } catch (error) {
      console.error('Error loading themes:', error);
    }
  };

  const handleThemeChange = async (themeId) => {
    setLoadingTheme(true);
    try {
      await activateTheme(themeId);
      await refreshPalette();
    } catch (error) {
      console.error('Error activating theme:', error);
      alert('Error al cambiar el tema');
    } finally {
      setLoadingTheme(false);
    }
  };


  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSave = async () => {
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      alert('Cambios guardados correctamente');
    }, 1000);
  };

  const handleLogout = () => {
    localStorage.clear();
    window.location.href = '/login';
  };

  const languages = [
    { id: 'es', label: 'Español (ES)' },
    { id: 'en', label: 'English (EN)' },
    { id: 'fr', label: 'Français (FR)' },
    { id: 'pt', label: 'Português (PT)' }
  ];

  return (
    <div className="min-h-screen bg-background text-slate-100 font-sans">
      {/* Hero Section */}
      <div className="w-full h-80 relative overflow-hidden bg-background">
        <div className="absolute inset-0 z-0" style={{ background: 'radial-gradient(circle at 30% 20%, #4c1d95 0%, transparent 60%), radial-gradient(circle at 70% 30%, #1e3a8a 0%, transparent 60%), radial-gradient(circle at 50% 0%, #7c3aed 0%, transparent 40%)' }}></div>
        <div className="absolute top-10 left-0 w-[150%] h-px bg-gradient-to-r from-transparent via-[rgba(124,23,211,0.3)] to-transparent -rotate-45"></div>
        <div className="absolute top-40 left-0 w-[150%] h-px bg-gradient-to-r from-transparent via-[rgba(0,212,255,0.3)] to-transparent -rotate-45"></div>
        <div className="absolute top-60 left-0 w-[150%] h-px bg-gradient-to-r from-transparent via-[rgba(124,23,211,0.3)] to-transparent -rotate-45"></div>
        <div className="absolute inset-0 z-10 opacity-40" style={{ backgroundImage: 'linear-gradient(to right, rgba(255,255,255,0.03) 1px, transparent 1px), linear-gradient(to bottom, rgba(255,255,255,0.03) 1px, transparent 1px)', backgroundSize: '40px 40px' }}></div>
        <div className="absolute bottom-0 left-0 w-full h-48 bg-gradient-to-t from-background via-background/80 to-transparent"></div>
        <div className="absolute bottom-0 left-0 w-full h-24 bg-background blur-xl scale-y-150 origin-bottom opacity-90"></div>
      </div>

      {/* Main Content */}
      <main className="max-w-[1100px] mx-auto px-6 -mt-70 pb-20 relative z-10">
        {/* Premium Header Card */}
        <div className="relative rounded-2xl p-8 mb-8" style={{ background: 'rgba(26, 17, 33, 0.5)', backdropFilter: 'blur(25px)', border: '1px solid rgba(124, 23, 211, 0.45)', boxShadow: '0 0 30px rgba(124, 23, 211, 0.2), inset 0 0 20px rgba(255,255,255,0.05)' }}>
          <div className="absolute inset-0 z-0 rounded-2xl" style={{ background: 'radial-gradient(at 0% 0%, hsla(268,77%,35%,1) 0%, transparent 50%), radial-gradient(at 100% 0%, hsla(195,100%,45%,0.3) 0%, transparent 50%), radial-gradient(at 50% 100%, hsla(262,80%,20%,1) 0%, transparent 50%)', opacity: 0.8 }}></div>
          <div className="absolute inset-0 z-0 rounded-2xl" style={{ backgroundImage: 'url("data:image/svg+xml,%3Csvg viewBox=\'0 0 200 200\' xmlns=\'http://www.w3.org/2000/svg\'%3E%3Cfilter id=\'noiseFilter\'%3E%3CfeTurbulence type=\'fractalNoise\' baseFrequency=\'0.65\' numOctaves=\'3\' stitchTiles=\'stitch\'/%3E%3C/filter%3E%3Crect width=\'100%25\' height=\'100%25\' filter=\'url(%23noiseFilter)\'/%3E%3C/svg%3E")', opacity: 0.04 }}></div>
          <div className="flex flex-col md:flex-row items-center md:items-end gap-8 relative z-10">
            <div className="relative">
              <div className="absolute -inset-4 rounded-full blur-3xl" style={{ background: 'rgba(124, 23, 211, 0.2)' }}></div>
              <div className="absolute -inset-1 rounded-2xl blur-md opacity-60" style={{ background: 'linear-gradient(to top right, #7c17d3, #60a5fa, #a855f7)' }}></div>
              <div className="relative w-32 h-32 rounded-xl bg-gradient-to-br from-[#7c17d3] to-[#5a189a] flex items-center justify-center text-5xl font-bold text-white border-2 border-white/30 shadow-2xl" style={{ boxShadow: '0 0 50px rgba(124, 23, 211, 0.6), 0 0 20px rgba(0, 212, 255, 0.4)' }}>
                {firstLetter}
              </div>
              <button className="absolute -bottom-2 -right-2 w-10 h-10 rounded-full bg-[#7c17d3] flex items-center justify-center text-white shadow-xl hover:scale-110 transition-transform border-2 border-background z-20">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/>
                </svg>
              </button>
            </div>
            <div className="flex-1 text-center md:text-left">
              <h2 className="text-4xl font-bold text-white tracking-tight" style={{ textShadow: '0 2px 10px rgba(0,0,0,0.5)' }}>{formData.nombreCompleto}</h2>
              <div className="flex flex-wrap justify-center md:justify-start items-center gap-3 mt-2">
                <p className="text-[#9d4edd] font-medium flex items-center gap-1">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/>
                  </svg>
                  {userEmail}
                </p>
                <span className="w-1 h-1 bg-slate-500 rounded-full hidden md:block"></span>
                <span className="px-3 py-1 rounded-full bg-[rgba(124,23,211,0.2)] border border-[rgba(124,23,211,0.4)] text-[#9d4edd] text-xs font-bold uppercase tracking-wider backdrop-blur-sm">{roleLabel}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Left Column */}
          <div className="lg:col-span-2 space-y-8">
            {/* Información Personal */}
            <section className="rounded-2xl p-8" style={{ background: 'rgba(26, 17, 33, 0.7)', backdropFilter: 'blur(12px)', border: '1px solid rgba(124, 23, 211, 0.2)' }}>
              <h3 className="text-xl font-bold text-white mb-6 flex items-center gap-2">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="#7c17d3">
                  <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
                </svg>
                Información Personal
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label className="text-sm font-medium text-slate-400 block mb-2">Nombre Completo</label>
                  <input 
                    className="w-full rounded-xl px-4 py-3 text-white bg-[rgba(15,15,26,0.5)] border border-[#372447] focus:outline-none focus:border-[#7c17d3] focus:ring-1 focus:ring-[#7c17d3] transition-all" 
                    type="text" 
                    name="nombreCompleto"
                    value={formData.nombreCompleto}
                    onChange={handleInputChange}
                  />
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-400 block mb-2">Teléfono</label>
                  <input 
                    className="w-full rounded-xl px-4 py-3 text-white bg-[rgba(15,15,26,0.5)] border border-[#372447] focus:outline-none focus:border-[#7c17d3] focus:ring-1 focus:ring-[#7c17d3] transition-all" 
                    type="tel" 
                    name="telefono"
                    value={formData.telefono}
                    onChange={handleInputChange}
                    placeholder="+34 600 000 000"
                  />
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-400 block mb-2">Fecha de Nacimiento</label>
                  <input 
                    className="w-full rounded-xl px-4 py-3 text-white bg-[rgba(15,15,26,0.5)] border border-[#372447] focus:outline-none focus:border-[#7c17d3] focus:ring-1 focus:ring-[#7c17d3] transition-all" 
                    type="date" 
                    name="fechaNacimiento"
                    value={formData.fechaNacimiento}
                    onChange={handleInputChange}
                  />
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-400 block mb-2">Dirección</label>
                  <input 
                    className="w-full rounded-xl px-4 py-3 text-white bg-[rgba(15,15,26,0.5)] border border-[#372447] focus:outline-none focus:border-[#7c17d3] focus:ring-1 focus:ring-[#7c17d3] transition-all" 
                    type="text" 
                    name="direccion"
                    value={formData.direccion}
                    onChange={handleInputChange}
                    placeholder="Calle Mayor, 1 Madrid"
                  />
                </div>
              </div>
            </section>

            {/* Seguridad */}
            <section className="rounded-2xl p-8" style={{ background: 'rgba(26, 17, 33, 0.7)', backdropFilter: 'blur(12px)', border: '1px solid rgba(124, 23, 211, 0.2)' }}>
              <h3 className="text-xl font-bold text-white mb-6 flex items-center gap-2">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="#7c17d3">
                  <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z"/>
                </svg>
                Seguridad de la Cuenta
              </h3>
              <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 p-4 rounded-xl bg-red-500/5 border border-red-500/20">
                <div>
                  <p className="font-bold text-error">Zona de Peligro</p>
                  <p className="text-sm text-slate-400">Una vez que desactives tu cuenta, no podrás revertir esta acción.</p>
                </div>
                <div className="flex gap-3">
                  <button className="px-4 py-2 rounded-lg border border-red-500/50 text-red-500 text-sm font-bold hover:bg-red-500 hover:text-white transition-all">Desactivar Cuenta</button>
                  <button 
                    onClick={handleLogout}
                    className="px-4 py-2 rounded-lg bg-error text-white text-sm font-bold hover:bg-red-700 transition-all"
                  >
                    Cerrar Sesión
                  </button>
                </div>
              </div>
            </section>
          </div>

          {/* Right Column */}
          <div className="space-y-8">
            {/* Idioma */}
            <section className="rounded-2xl p-6 border-l-4" style={{ background: 'rgba(26, 17, 33, 0.7)', backdropFilter: 'blur(12px)', borderLeftColor: '#7c17d3', border: '1px solid rgba(124, 23, 211, 0.2)' }}>
              <h3 className="text-lg font-bold text-white mb-4">Idioma</h3>
              <div className="space-y-3">
                {languages.map((lang) => (
                  <button 
                    key={lang.id}
                    onClick={() => setSelectedLanguage(lang.id)}
                    className={`flex items-center justify-between p-3 rounded-xl w-full transition-all ${
                      selectedLanguage === lang.id 
                        ? 'bg-[rgba(124,23,211,0.1)] border border-[rgba(124,23,211,0.4)]' 
                        : 'bg-[#1a1121] border border-[#372447] hover:border-[rgba(124,23,211,0.4)]'
                    }`}
                  >
                    <span className={`text-sm font-medium ${selectedLanguage === lang.id ? 'text-white' : 'text-slate-300'}`}>{lang.label}</span>
                    <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${
                      selectedLanguage === lang.id ? 'border-[#7c17d3]' : 'border-[#372447]'
                    }`}>
                      {selectedLanguage === lang.id && (
                        <div className="w-2.5 h-2.5 rounded-full bg-[#7c17d3]"></div>
                      )}
                    </div>
                  </button>
                ))}
              </div>
            </section>

            {/* Tema Visual */}
            <section className="rounded-2xl p-6 border-l-4" style={{ background: 'rgba(26, 17, 33, 0.7)', backdropFilter: 'blur(12px)', borderLeftColor: '#7c17d3', border: '1px solid rgba(124, 23, 211, 0.2)' }}>
              <h3 className="text-lg font-bold text-white mb-4">Tema Visual</h3>
              <div className="space-y-3">
                {themes.map((theme) => {
                  const preview = THEME_PREVIEWS[theme.name] || THEME_PREVIEWS.DARK;
                  const isActive = activeThemeId === theme.id;
                  
                  return (
                    <button
                      key={theme.id}
                      onClick={() => handleThemeChange(theme.id)}
                      disabled={loadingTheme}
                      className={`relative rounded-xl p-4 text-left transition-all w-full ${
                        isActive 
                          ? 'bg-[rgba(124,23,211,0.15)] border-2 border-[#7c17d3]' 
                          : 'bg-[#1a1121] border-2 border-[#372447] hover:border-[rgba(124,23,211,0.4)]'
                      } ${loadingTheme ? 'opacity-50 cursor-not-allowed' : ''}`}
                    >
                      <div className="flex items-center justify-between mb-3">
                        <span className={`text-sm font-bold ${isActive ? 'text-white' : 'text-slate-300'}`}>
                          {theme.name}
                        </span>
                        {isActive && (
                          <div className="px-2 py-0.5 rounded-full text-xs font-bold" style={{ background: 'rgba(124, 23, 211, 0.3)', color: '#c4b5fd' }}>
                            ACTIVO
                          </div>
                        )}
                      </div>
                      <div className="flex items-center gap-2">
                        <div 
                          className="w-8 h-8 rounded-full border-2 border-white/20" 
                          style={{ background: preview.background }}
                        />
                        <div 
                          className="w-8 h-8 rounded-full border-2 border-white/20" 
                          style={{ background: preview.primary }}
                        />
                        <div 
                          className="w-8 h-8 rounded-full border-2 border-white/20" 
                          style={{ background: preview.accent }}
                        />
                      </div>
                      {isActive && (
                        <div className="absolute inset-0 pointer-events-none rounded-xl" style={{ boxShadow: 'inset 0 0 20px rgba(124,23,211,0.2)' }}></div>
                      )}
                    </button>
                  );
                })}
              </div>
            </section>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
};

export default ProfilePage;