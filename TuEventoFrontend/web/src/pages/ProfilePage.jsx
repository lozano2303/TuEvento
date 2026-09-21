import { useState, useEffect, useRef } from 'react';
import Footer from '../layouts/Footer';
import { useTheme } from '../context/ThemeContext';
import { getThemes, activateTheme } from '../services/themeService';
import { getProfileByUserId, getProfilePictureUrl, updateProfile, uploadProfilePicture } from '../services/ProfileService';
import { performLogout } from '../services/httpClient';
import { AlertCircle, Camera, CheckCircle, Info, Loader2, Palette } from 'lucide-react';
import Tooltip from '../components/common/Tooltip';
import ThemeCustomizePanel from '../components/theme/ThemeCustomizePanel';
import DeactivateAccountModal from '../components/common/DeactivateAccountModal';
import { isBioClean } from '../utils/bioValidation';

const THEME_PREVIEWS = {
  DARK:       { background: "#1E0A3C", primary: "#7C3AED", accent: "#A78BFA" },
  LIGHT:      { background: "#FFFFFF", primary: "#7C3AED", accent: "#8B5CF6" },
  VIBRANT:    { background: "#0D0D0D", primary: "#FF4081", accent: "#FFEB3B" },
  ACCESSIBLE: { background: "#FFFFFF", primary: "#005FCC", accent: "#E65100" },
};

const MAX_AVATAR_SIZE_MB = 2;
const ALLOWED_AVATAR_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

const ProfilePage = () => {
  const { refreshPalette, activeThemeId, applyPalette } = useTheme();
  const [selectedLanguage, setSelectedLanguage] = useState('es');
  const [loading, setLoading] = useState(false);
  const [customizePanelOpen, setCustomizePanelOpen] = useState(false);
  const [deactivateModalOpen, setDeactivateModalOpen] = useState(false);
  const [themes, setThemes] = useState([]);
  const [loadingTheme, setLoadingTheme] = useState(false);
  const [profileId, setProfileId] = useState(null);
  const [storedFileId, setStoredFileId] = useState(null);
  const [avatarUrl, setAvatarUrl] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [avatarLoading, setAvatarLoading] = useState(false);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const [avatarMessage, setAvatarMessage] = useState(null);
  const avatarInputRef = useRef(null);

  const userEmail = localStorage.getItem('userEmail') || '';
  const storedName = localStorage.getItem('name') || localStorage.getItem('fullName') || '';
  const userRole = localStorage.getItem('role') || 'USER';

  const getDisplayName = (name) => {
    // Si el nombre es "Evento" (nombre del admin genérico), usar "Tu Evento"
    if (!name || name === 'Evento') return 'Tu Evento';
    const parts = name.split(' ').filter(part => part.trim().length > 0);
    if (parts.length === 0) return 'Tu Evento';
    // Si solo hay un nombre corto (≤3 caracteres), mostrar "Tu Evento"
    if (parts.length === 1) {
      return parts[0].length <= 3 ? 'Tu Evento' : parts[0];
    }
    const firstName = parts[0];
    const lastName = parts[1];
    // Si el nombre es corto (≤3 caracteres), mostrar nombre completo
    if (firstName.length <= 3) {
      return `${firstName} ${lastName}`;
    }
    return firstName;
  };

  const displayName = getDisplayName(storedName);
  const firstLetter = displayName ? displayName.charAt(0).toUpperCase() : 'F';
  const roleLabel = userRole === 'ADMIN' ? 'Administrador' : userRole === 'ORGANIZER' ? 'Organizador' : 'Usuario';

  const [formData, setFormData] = useState({
    nombreCompleto: storedName || '',
    bio: '',
  });
  const [saveMessage,   setSaveMessage]   = useState(null); // { type: 'success'|'error', text }
  const [nameChangedAt, setNameChangedAt] = useState(null); // ISO string | null
  const [bioError,      setBioError]      = useState(null); // string | null

  // Calcula si el usuario puede cambiar su nombre ahora mismo.
  // canChangeName = true cuando nameChangedAt es null O ya pasaron 14 días.
  const NAME_COOLDOWN_DAYS = 14;
  const nextNameChangeDate = nameChangedAt
    ? new Date(new Date(nameChangedAt).getTime() + NAME_COOLDOWN_DAYS * 24 * 60 * 60 * 1000)
    : null;
  const canChangeName = !nextNameChangeDate || new Date() >= nextNameChangeDate;

  // Cargar temas del backend al montar
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) return;
    getThemes()
      .then(data => setThemes(data || []))
      .catch(err => console.error('[ProfilePage] Error cargando temas:', err));
  }, []);

  useEffect(() => {
    const loadProfileAvatar = async () => {
      const token = localStorage.getItem('token');
      const userId = localStorage.getItem('userID');
      if (!token || !userId) return;

      setAvatarLoading(true);
      try {
        const result = await getProfileByUserId(userId);
        const profile = result?.data;
        if (!profile) return;

        setProfileId(profile.profileId);
        setStoredFileId(profile.storedFileId);
        if (profile.fullName) {
          localStorage.setItem('name', profile.fullName);
        }

        // Precargar campos editables del formulario con los datos reales del backend
        setFormData({
          nombreCompleto: profile.fullName || '',
          bio:            profile.bio      || '',
        });
        // Guardar la fecha del último cambio de nombre para calcular el cooldown
        setNameChangedAt(profile.nameChangedAt ?? null);

        if (profile.storedFileId) {
          try {
            const url = await getProfilePictureUrl(profile.storedFileId);
            if (url) {
              setAvatarUrl(url);
            } else {
              // The endpoint returned 200 but publicUrl was empty — log for debugging
              console.warn('[ProfilePage] La URL de la foto de perfil estaba vacía (storedFileId=%s)', profile.storedFileId);
              setAvatarMessage({ type: 'error', text: 'No se pudo cargar tu foto de perfil.' });
            }
          } catch (avatarErr) {
            // Separate avatar-URL errors from profile-fetch errors so we don't
            // hide the profile data just because the image failed.
            console.error('[ProfilePage] Error al obtener la URL de la foto de perfil (storedFileId=%s):', profile.storedFileId, avatarErr);
            setAvatarMessage({ type: 'error', text: 'No se pudo cargar tu foto de perfil.' });
          }
        }
      } catch (err) {
        console.error('[ProfilePage] Error cargando datos del perfil:', err);
      } finally {
        setAvatarLoading(false);
      }
    };

    loadProfileAvatar();
  }, []);

  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  const handleThemeChange = async (themeId) => {
    if (loadingTheme || themeId === activeThemeId) return;
    setLoadingTheme(true);
    try {
      await activateTheme(themeId);
      await refreshPalette();
    } catch (err) {
      console.error('[ProfilePage] Error activando tema:', err);
    } finally {
      setLoadingTheme(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const openAvatarPicker = () => {
    avatarInputRef.current?.click();
  };

  const handleAvatarChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const isAllowedType = ALLOWED_AVATAR_TYPES.includes(file.type) || /\.(jpe?g|png|webp)$/i.test(file.name);
    if (!isAllowedType) {
      setAvatarMessage({ type: 'error', text: 'Solo se permiten imágenes JPG, PNG o WEBP.' });
      e.target.value = '';
      return;
    }

    if (file.size > MAX_AVATAR_SIZE_MB * 1024 * 1024) {
      setAvatarMessage({ type: 'error', text: `La imagen debe pesar menos de ${MAX_AVATAR_SIZE_MB} MB.` });
      e.target.value = '';
      return;
    }

    const nextPreviewUrl = URL.createObjectURL(file);
    setPreviewUrl(nextPreviewUrl);
    await uploadAvatar(file);
    e.target.value = '';
  };

  const uploadAvatar = async (file) => {
    if (!profileId) {
      setAvatarMessage({ type: 'error', text: 'No se encontró tu perfil para actualizar la foto.' });
      return;
    }

    setUploadingAvatar(true);
    setAvatarMessage(null);

    try {
      const uploaded = await uploadProfilePicture(file);
      const newStoredFileId = uploaded?.storedFileId;
      if (!newStoredFileId) throw new Error('No se pudo subir la imagen.');

      await updateProfile(profileId, { storedFileId: newStoredFileId });

      const newUrl = await getProfilePictureUrl(newStoredFileId);
      setStoredFileId(newStoredFileId);
      setAvatarUrl(newUrl || null);
      // El preview ya cumplió su función — la URL permanente toma el control.
      setPreviewUrl(null);
      setAvatarMessage({ type: 'success', text: 'Foto de perfil actualizada correctamente.' });
    } catch (err) {
      // La imagen fue rechazada (NSFW, tamaño, red, etc.) — descartamos el
      // preview de la imagen rechazada para que vuelva a mostrarse la foto
      // anterior del usuario (avatarUrl). No tocamos avatarUrl ni storedFileId
      // porque en BD no se guardó nada nuevo.
      setPreviewUrl(null);
      setAvatarMessage({ type: 'error', text: err.message || 'No se pudo actualizar la foto de perfil.' });
    } finally {
      setUploadingAvatar(false);
    }
  };

  const handleSave = async () => {
    if (!profileId) {
      setSaveMessage({ type: 'error', text: 'No se encontró tu perfil. Recarga la página.' });
      return;
    }
    setLoading(true);
    setSaveMessage(null);
    setBioError(null);
    // Validación client-side de bio antes de enviar (feedback inmediato)
    if (formData.bio.trim() && !isBioClean(formData.bio)) {
      setBioError('La biografía contiene lenguaje inapropiado.');
      setLoading(false);
      return;
    }
    try {
      await updateProfile(profileId, {
        fullName: formData.nombreCompleto.trim() || undefined,
        bio:      formData.bio.trim()            || undefined,
      });
      // Actualizar localStorage para que el nombre se refleje en navbar inmediatamente
      if (formData.nombreCompleto.trim()) {
        localStorage.setItem('name', formData.nombreCompleto.trim());
      }
      // Si el nombre cambió, refrescar la fecha de cooldown para bloquear el input
      if (formData.nombreCompleto.trim()) {
        setNameChangedAt(new Date().toISOString());
      }
      setSaveMessage({ type: 'success', text: 'Cambios guardados correctamente.' });
    } catch (err) {
      // Separar el error de lenguaje inapropiado del banner general
      if (err.message?.includes('lenguaje inapropiado') ||
          err.message?.includes('BIO_INAPPROPRIATE_LANGUAGE')) {
        setBioError('La biografía contiene lenguaje inapropiado.');
      } else {
        setSaveMessage({ type: 'error', text: err.message || 'No se pudieron guardar los cambios.' });
      }
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    await performLogout();
    window.location.href = '/login';
  };

  const languages = [
    { id: 'es', label: 'Español (ES)' },
    { id: 'en', label: 'English (EN)' },
    { id: 'fr', label: 'Français (FR)' },
    { id: 'pt', label: 'Português (PT)' }
  ];

  return (
    <div className="min-h-screen bg-background text-textPrimary font-sans">

      {/* Hero Section */}
      <div className="w-full h-80 relative overflow-hidden bg-background">
        <div className="absolute inset-0 z-0" style={{ background: 'radial-gradient(circle at 30% 20%, color-mix(in srgb, var(--color-primaryDark) 80%, #000) 0%, transparent 60%), radial-gradient(circle at 70% 30%, color-mix(in srgb, var(--color-primary) 40%, #000) 0%, transparent 60%), radial-gradient(circle at 50% 0%, color-mix(in srgb, var(--color-primary) 60%, transparent) 0%, transparent 40%)' }}></div>
        <div className="absolute top-10 left-0 w-[150%] h-px bg-gradient-to-r from-transparent via-primary/30 to-transparent -rotate-45"></div>
        <div className="absolute top-40 left-0 w-[150%] h-px bg-gradient-to-r from-transparent via-accent/30 to-transparent -rotate-45"></div>
        <div className="absolute top-60 left-0 w-[150%] h-px bg-gradient-to-r from-transparent via-primary/30 to-transparent -rotate-45"></div>
        <div className="absolute inset-0 z-10 opacity-40" style={{ backgroundImage: 'linear-gradient(to right, rgba(255,255,255,0.03) 1px, transparent 1px), linear-gradient(to bottom, rgba(255,255,255,0.03) 1px, transparent 1px)', backgroundSize: '40px 40px' }}></div>
        <div className="absolute bottom-0 left-0 w-full h-48 bg-gradient-to-t from-background via-background/80 to-transparent"></div>
        <div className="absolute bottom-0 left-0 w-full h-24 bg-background blur-xl scale-y-150 origin-bottom opacity-90"></div>
      </div>

      {/* Main Content */}
      <main className="max-w-[1100px] mx-auto px-6 -mt-70 pb-20 relative z-10">

        {/* Premium Header Card */}
        <div className="theme-hero-section relative rounded-2xl p-8 mb-8">
          <div className="absolute inset-0 z-0 rounded-2xl" style={{ background: 'radial-gradient(at 0% 0%, color-mix(in srgb, var(--color-primaryDark) 80%, #000) 0%, transparent 50%), radial-gradient(at 100% 0%, color-mix(in srgb, var(--color-accent) 30%, transparent) 0%, transparent 50%), radial-gradient(at 50% 100%, color-mix(in srgb, var(--color-primary) 50%, #000) 0%, transparent 50%)', opacity: 0.8 }}></div>
          <div className="absolute inset-0 z-0 rounded-2xl" style={{ backgroundImage: 'url("data:image/svg+xml,%3Csvg viewBox=\'0 0 200 200\' xmlns=\'http://www.w3.org/2000/svg\'%3E%3Cfilter id=\'noiseFilter\'%3E%3CfeTurbulence type=\'fractalNoise\' baseFrequency=\'0.65\' numOctaves=\'3\' stitchTiles=\'stitch\'/%3E%3C/filter%3E%3Crect width=\'100%25\' height=\'100%25\' filter=\'url(%23noiseFilter)\'/%3E%3C/svg%3E")', opacity: 0.04 }}></div>
          <div className="flex flex-col md:flex-row items-center md:items-end gap-8 relative z-10">
            <div className="relative flex flex-col items-center">
              <div className="absolute -inset-4 rounded-full blur-3xl" style={{ background: 'color-mix(in srgb, var(--color-primary) 20%, transparent)' }}></div>
              <div className="absolute -inset-1 rounded-2xl blur-md opacity-60" style={{ background: 'linear-gradient(to top right, var(--color-primary), var(--color-accent), var(--color-primaryDark))' }}></div>
              {/* Avatar container — fixed 128×128, no overflow */}
              <div className="relative w-32 h-32 flex-shrink-0">
                <div className="w-32 h-32 rounded-xl bg-gradient-to-br from-primary to-primaryDark flex items-center justify-center text-5xl font-bold text-textPrimary border-2 border-white/30 shadow-2xl overflow-hidden" style={{ boxShadow: '0 0 50px color-mix(in srgb, var(--color-primary) 60%, transparent), 0 0 20px color-mix(in srgb, var(--color-accent) 40%, transparent)' }}>
                  {avatarLoading ? (
                    <Loader2 className="w-12 h-12 animate-spin text-textPrimary" />
                  ) : (avatarUrl || previewUrl) ? (
                    <img
                      src={previewUrl || avatarUrl}
                      alt="Foto de perfil"
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <img
                      src="/default-avatar.png"
                      alt="Avatar por defecto"
                      className="w-full h-full object-cover"
                    />
                  )}
                  {uploadingAvatar && (
                    <div className="absolute inset-0 bg-background/60 backdrop-blur-[2px] flex items-center justify-center">
                      <Loader2 className="w-10 h-10 animate-spin text-white" />
                    </div>
                  )}
                </div>
                {/* Camera button — anchored to avatar container, not pushed by text */}
                <button
                  type="button"
                  onClick={openAvatarPicker}
                  disabled={uploadingAvatar}
                  className="absolute -bottom-2 -right-2 w-10 h-10 rounded-full bg-primary flex items-center justify-center text-textPrimary shadow-xl hover:scale-110 transition-transform border-2 border-background z-20 disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  <Camera className="w-5 h-5" />
                </button>
                {/* Ícono de info — independiente del botón de cámara para no disparar el file picker */}
                <div className="absolute -bottom-2 -left-2 z-20">
                  <Tooltip
                    content={`Solo JPG, PNG o WEBP. Máx. ${MAX_AVATAR_SIZE_MB} MB. Sin contenido adulto, violencia o armas.`}
                    position="right"
                  >
                    <button
                      type="button"
                      aria-label="Requisitos de imagen de perfil"
                      className="w-7 h-7 rounded-full flex items-center justify-center border-2 border-background shadow-lg"
                      style={{ background: 'var(--color-surfaceAlt)', color: 'var(--color-textMuted)' }}
                      onClick={(e) => e.stopPropagation()}
                    >
                      <Info className="w-3.5 h-3.5" />
                    </button>
                  </Tooltip>
                </div>
              </div>
              <input
                ref={avatarInputRef}
                type="file"
                accept="image/png,image/jpeg,image/webp"
                onChange={handleAvatarChange}
                disabled={uploadingAvatar}
                className="hidden"
              />
            </div>
            <div className="flex-1 text-center md:text-left">
              <h2 className="text-4xl font-bold text-textPrimary tracking-tight" style={{ textShadow: '0 2px 10px rgba(0,0,0,0.5)' }}>{formData.nombreCompleto}</h2>
              <div className="flex flex-wrap justify-center md:justify-start items-center gap-3 mt-2">
                <p className="text-accent font-medium flex items-center gap-1">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/>
                  </svg>
                  {userEmail}
                </p>
                <span className="w-1 h-1 bg-surfaceAlt rounded-full hidden md:block"></span>
                <span className="px-3 py-1 rounded-full bg-primary/20 border border-primary/40 text-accent text-xs font-bold uppercase tracking-wider backdrop-blur-sm">{roleLabel}</span>
              </div>
              {avatarMessage && (
                <div className={`mt-4 inline-flex items-center gap-2 rounded-xl px-3 py-2 text-xs font-semibold border ${
                  avatarMessage.type === 'success'
                    ? 'badge-success-fixed'
                    : 'bg-error/10 text-error border-error/20'
                }`}>
                  {avatarMessage.type === 'success' ? <CheckCircle className="w-4 h-4" /> : <AlertCircle className="w-4 h-4" />}
                  {avatarMessage.text}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">

          {/* Left Column */}
          <div className="lg:col-span-2 space-y-8">

            {/* Información Personal */}
            <section className="theme-profile-section rounded-2xl p-8">
              <h3 className="text-xl font-bold text-textPrimary mb-6 flex items-center gap-2">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="var(--color-primary)">
                  <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
                </svg>
                Información Personal
              </h3>
              <div className="space-y-5">

                {/* Nombre Completo */}
                <div>
                  <label className="text-sm font-medium text-textMuted block mb-2">
                    Nombre Completo
                  </label>
                  <input
                    className={`w-full rounded-xl px-4 py-3 text-textPrimary border
                      focus:outline-none transition-all
                      ${canChangeName
                        ? 'bg-background/50 border-surfaceAlt focus:border-primary focus:ring-1 focus:ring-primary'
                        : 'bg-background/20 border-surfaceAlt opacity-60 cursor-not-allowed'
                      }`}
                    type="text"
                    name="nombreCompleto"
                    value={formData.nombreCompleto}
                    onChange={(e) => {
                      // Filtrar en tiempo real: solo letras Unicode y espacios
                      const filtered = e.target.value.replace(/[^\p{L}\s]/gu, '');
                      setFormData(prev => ({ ...prev, nombreCompleto: filtered }));
                    }}
                    placeholder="Tu nombre completo"
                    maxLength={30}
                    disabled={!canChangeName}
                    readOnly={!canChangeName}
                  />
                  {/* Contador de caracteres */}
                  <p className="text-xs text-textMuted mt-1 text-right">
                    {formData.nombreCompleto.length} / 30
                  </p>
                  {/* Mensaje de cooldown */}
                  {!canChangeName && nextNameChangeDate && (
                    <p className="mt-1.5 text-xs flex items-center gap-1.5"
                      style={{ color: 'var(--color-textMuted)' }}>
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none"
                        stroke="currentColor" strokeWidth="2">
                        <circle cx="12" cy="12" r="10"/>
                        <polyline points="12 6 12 12 16 14"/>
                      </svg>
                      Podrás cambiar tu nombre de nuevo el{' '}
                      <strong>
                        {nextNameChangeDate.toLocaleDateString('es-CO', {
                          day: '2-digit', month: 'long', year: 'numeric',
                        })}
                      </strong>
                    </p>
                  )}
                </div>

                {/* Bio */}
                <div>
                  <label className="text-sm font-medium text-textMuted block mb-2">
                    Biografía
                  </label>
                  <textarea
                    className={`w-full rounded-xl px-4 py-3 text-textPrimary bg-background/50 border
                      focus:outline-none focus:ring-1 transition-all resize-none
                      ${bioError
                        ? 'border-error focus:border-error focus:ring-error/30'
                        : 'border-surfaceAlt focus:border-primary focus:ring-primary'
                      }`}
                    name="bio"
                    value={formData.bio}
                    onChange={(e) => {
                      setBioError(null);   // limpiar error al volver a escribir
                      handleInputChange(e);
                    }}
                    placeholder="Cuéntanos algo sobre ti..."
                    rows={3}
                    maxLength={255}
                  />
                  <div className="flex items-start justify-between mt-1 gap-2">
                    {bioError
                      ? <p className="text-xs text-error flex items-center gap-1">
                          <AlertCircle className="w-3.5 h-3.5 flex-shrink-0" />
                          {bioError}
                        </p>
                      : <span />
                    }
                    <p className="text-xs text-textMuted flex-shrink-0">
                      {formData.bio.length} / 255
                    </p>
                  </div>
                </div>

                {/* Mensaje de resultado (éxito o error) */}
                {saveMessage && (
                  <div className={`flex items-center gap-2 rounded-xl px-4 py-3 text-sm font-semibold border ${
                    saveMessage.type === 'success'
                      ? 'badge-success-fixed'
                      : 'bg-error/10 text-error border-error/20'
                  }`}>
                    {saveMessage.type === 'success'
                      ? <CheckCircle className="w-4 h-4 flex-shrink-0" />
                      : <AlertCircle className="w-4 h-4 flex-shrink-0" />
                    }
                    {saveMessage.text}
                  </div>
                )}

                {/* Botón guardar */}
                <button
                  onClick={handleSave}
                  disabled={loading}
                  className="px-6 py-2.5 rounded-xl bg-primary text-textPrimary text-sm font-bold
                    hover:bg-primaryDark transition-all disabled:opacity-50 disabled:cursor-not-allowed
                    flex items-center gap-2"
                >
                  {loading && <Loader2 className="w-4 h-4 animate-spin" />}
                  {loading ? 'Guardando…' : 'Guardar cambios'}
                </button>
              </div>
            </section>

            {/* Seguridad */}
            <section className="theme-profile-section rounded-2xl p-8">
              <h3 className="text-xl font-bold text-textPrimary mb-6 flex items-center gap-2">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="var(--color-primary)">
                  <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z"/>
                </svg>
                Seguridad de la Cuenta
              </h3>
              <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 p-4 rounded-xl bg-error/5 border border-error/20">
                <div>
                  <p className="font-bold text-error">Zona de Peligro</p>
                  <p className="text-sm text-textMuted">Una vez que desactives tu cuenta, no podrás revertir esta acción.</p>
                </div>
                <div className="flex gap-3">
                  <button
                    onClick={() => setDeactivateModalOpen(true)}
                    className="px-4 py-2 rounded-lg border border-error/50 text-error text-sm font-bold hover:bg-error hover:text-textPrimary transition-all">
                    Desactivar Cuenta
                  </button>
                  <button
                    onClick={handleLogout}
                    className="px-4 py-2 rounded-lg bg-error text-textPrimary text-sm font-bold hover:bg-error/80 transition-all"
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
            <section className="theme-profile-section rounded-2xl p-6">
              <h3 className="text-lg font-bold text-textPrimary mb-4">Idioma</h3>
              <div className="space-y-3">
                {languages.map((lang) => (
                  <button
                    key={lang.id}
                    onClick={() => setSelectedLanguage(lang.id)}
                    className={`flex items-center justify-between p-3 rounded-xl w-full transition-all ${
                      selectedLanguage === lang.id
                        ? 'bg-primary/10 border border-primary/40'
                        : 'bg-surface border border-surfaceAlt hover:border-primary/40'
                    }`}
                  >
                    <span className={`text-sm font-medium ${selectedLanguage === lang.id ? 'text-textPrimary' : 'text-textSecondary'}`}>
                      {lang.label}
                    </span>
                    <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${
                      selectedLanguage === lang.id ? 'border-primary' : 'border-surfaceAlt'
                    }`}>
                      {selectedLanguage === lang.id && (
                        <div className="w-2.5 h-2.5 rounded-full bg-primary"></div>
                      )}
                    </div>
                  </button>
                ))}
              </div>
            </section>

            {/* Tema Visual — conectado al backend */}
            <section className="theme-profile-section rounded-2xl p-6">
              <h3 className="text-lg font-bold text-textPrimary mb-4">Tema Visual</h3>
              <div className="space-y-3">
                {themes.length === 0 && (
                  <p className="text-textMuted text-sm text-center py-4">
                    Cargando temas...
                  </p>
                )}
                {themes.map((theme) => {
                  const preview = THEME_PREVIEWS[theme.name] || THEME_PREVIEWS.DARK;
                  const isActive = activeThemeId === theme.id;
                  const isActivating = loadingTheme;
                  // PRINCIPAL is not customisable — mirrors mobile guard
                  const isCustomisable = isActive && theme.name !== 'PRINCIPAL';

                  return (
                    <div key={theme.id} className="space-y-1.5">
                      <button
                        onClick={() => handleThemeChange(theme.id)}
                        disabled={isActivating}
                        className={`relative rounded-xl overflow-hidden border-2 p-4 w-full transition-all text-left ${
                          isActive
                            ? 'border-primary bg-primary/10'
                            : 'border-surfaceAlt hover:border-primary/40'
                        } ${isActivating ? 'opacity-60 cursor-not-allowed' : ''}`}
                      >
                        <div className="flex items-center justify-between mb-3">
                          <span className={`text-sm font-bold ${isActive ? 'text-textPrimary' : 'text-textSecondary'}`}>
                            {theme.name}
                          </span>
                          {isActive && (
                            <span className="text-xs font-bold text-primary bg-primary/20 border border-primary/40 px-2 py-0.5 rounded-full">
                              ACTIVO
                            </span>
                          )}
                        </div>

                        {/* Preview de 3 círculos de color */}
                        <div className="flex items-center gap-2">
                          <div className="w-6 h-6 rounded-full border border-white/20" style={{ background: preview.background }} />
                          <div className="w-6 h-6 rounded-full border border-white/20" style={{ background: preview.primary }} />
                          <div className="w-6 h-6 rounded-full border border-white/20" style={{ background: preview.accent }} />
                        </div>

                        {isActive && (
                          <div className="absolute inset-0 pointer-events-none rounded-xl" style={{ boxShadow: 'inset 0 0 20px color-mix(in srgb, var(--color-primary) 20%, transparent)' }}></div>
                        )}
                      </button>

                      {/* Personalizar button — only on the active non-PRINCIPAL theme */}
                      {isCustomisable && (
                        <button
                          onClick={() => setCustomizePanelOpen(true)}
                          className="tcp-customize-btn"
                        >
                          <Palette className="w-3.5 h-3.5 flex-shrink-0" />
                          Personalizar colores
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>
            </section>
          </div>
        </div>
      </main>
      <Footer />

      {/* Theme customise panel — slide-in drawer */}
      {customizePanelOpen && (() => {
        const activeTheme = themes.find(t => t.id === activeThemeId);
        return (
          <ThemeCustomizePanel
            themeName={activeTheme?.name ?? 'DARK'}
            onClose={() => setCustomizePanelOpen(false)}
          />
        );
      })()}

      {/* Deactivate account modal */}
      <DeactivateAccountModal
        isOpen={deactivateModalOpen}
        onClose={() => setDeactivateModalOpen(false)}
      />
    </div>
  );
};

export default ProfilePage;
