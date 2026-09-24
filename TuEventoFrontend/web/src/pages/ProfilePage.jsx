import { useState, useEffect, useRef } from 'react';
import Footer from '../layouts/Footer';
import { useTheme } from '../context/ThemeContext';
import { getThemes, activateTheme } from '../services/themeService';
import { getProfileByUserId, getProfilePictureUrl, updateProfile, uploadProfilePicture } from '../services/ProfileService';
import { performLogout } from '../services/httpClient';
import {
  AlertCircle, Camera, CheckCircle, Info, Loader2,
  Palette, ChevronLeft, ChevronRight, LogOut,
} from 'lucide-react';
import Tooltip from '../components/common/Tooltip';
import ThemeCustomizePanel from '../components/theme/ThemeCustomizePanel';
import DeactivateAccountModal from '../components/common/DeactivateAccountModal';

// ── Previews de los 8 temas del sistema (colores literales solo para las
//    muestras de vista previa — no se usan en la UI de la página) ─────────────
const THEME_PREVIEWS = {
  PRINCIPAL: { background: '#1E0A3C', primary: '#7C3AED', accent: '#A78BFA' },
  DARK:      { background: '#0D0D0D', primary: '#E0E0E0', accent: '#7a7a7a' },
  LIGHT:     { background: '#FFFFFF', primary: '#424242', accent: '#757575' },
  PASTEL:    { background: '#FFF9FB', primary: '#e0165a', accent: '#ac48bd' },
  VIBRANT:   { background: '#0A0A0A', primary: '#FF1744', accent: '#FFEA00' },
  NATURE:    { background: '#F1F8E9', primary: '#2E7D32', accent: '#886a60' },
  OCEAN:     { background: '#E3F2FD', primary: '#0272b5', accent: '#007786' },
  SUNSET:    { background: '#FFF3E0', primary: '#c94116', accent: '#d33300' },
};

const MAX_AVATAR_SIZE_MB = 2;
const ALLOWED_AVATAR_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

// ── Clase base para inputs — dark mode usando tokens del sistema ──────────────
// bg-surface: fondo de card; border-surfaceAlt: borde sutil; focus→primary
const inputCls =
  'w-full rounded-xl px-4 py-3 text-sm font-medium transition-all outline-none ' +
  'bg-surface border border-surfaceAlt text-textPrimary ' +
  'placeholder:text-textMuted ' +
  'focus:border-primary focus:ring-1 focus:ring-primary/30 ' +
  '[color-scheme:dark]';

const ProfilePage = () => {
  const { refreshPalette, activeThemeId } = useTheme();

  // ── Estado ─────────────────────────────────────────────────────────────────
  const [selectedLanguage, setSelectedLanguage] = useState('es');
  const [loading, setLoading] = useState(false);
  const [customizePanelOpen, setCustomizePanelOpen] = useState(false);
  const [deactivateModalOpen, setDeactivateModalOpen] = useState(false);

  const [themes, setThemes] = useState([]);
  const [loadingTheme, setLoadingTheme] = useState(false);
  const [carouselIdx, setCarouselIdx] = useState(0);
  const [carouselFade, setCarouselFade] = useState('in');

  const [profileId, setProfileId] = useState(null);
  const [avatarUrl, setAvatarUrl] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [avatarLoading, setAvatarLoading] = useState(false);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const [avatarMessage, setAvatarMessage] = useState(null);
  const avatarInputRef = useRef(null);

  const userEmail = localStorage.getItem('userEmail') || 'francisco@tuevento.com';
  const storedName = localStorage.getItem('name') || localStorage.getItem('fullName') || 'Francisco';
  const userRole = localStorage.getItem('role') || 'USER';

  const getDisplayName = (name) => {
    if (!name || name === 'Evento') return 'Tu Evento';
    const parts = name.split(' ').filter(p => p.trim().length > 0);
    if (!parts.length) return 'Tu Evento';
    if (parts.length === 1) return parts[0].length <= 3 ? 'Tu Evento' : parts[0];
    return parts[0].length <= 3 ? `${parts[0]} ${parts[1]}` : parts[0];
  };

  const displayName = getDisplayName(storedName);
  const roleLabel =
    userRole === 'ADMIN' ? 'Administrador' :
    userRole === 'ORGANIZER' ? 'Organizador' : 'Usuario';

  const [formData, setFormData] = useState({
    nombreCompleto: storedName || '',
    bio: '',
  });

  // ── Carga temas ────────────────────────────────────────────────────────────
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) return;
    getThemes()
      .then(data => setThemes(data || []))
      .catch(err => console.error('[ProfilePage] Error cargando temas:', err));
  }, []);

  // Sincronizar carrusel con el tema activo
  useEffect(() => {
    if (!themes.length || !activeThemeId) return;
    const idx = themes.findIndex(t => t.id === activeThemeId);
    if (idx >= 0) setCarouselIdx(idx);
  }, [themes, activeThemeId]);

  // ── Carga perfil ───────────────────────────────────────────────────────────
  useEffect(() => {
    const load = async () => {
      const token = localStorage.getItem('token');
      const userId = localStorage.getItem('userID');
      if (!token || !userId) return;
      setAvatarLoading(true);
      try {
        const result = await getProfileByUserId(userId);
        const profile = result?.data;
        if (!profile) return;
        setProfileId(profile.profileId);
        if (profile.fullName) {
          localStorage.setItem('name', profile.fullName);
          setFormData(prev => ({ ...prev, nombreCompleto: profile.fullName }));
        }
        if (profile.bio) {
          setFormData(prev => ({ ...prev, bio: profile.bio }));
        }
        if (profile.storedFileId) {
          try {
            const url = await getProfilePictureUrl(profile.storedFileId);
            if (url) setAvatarUrl(url);
            else setAvatarMessage({ type: 'error', text: 'No se pudo cargar tu foto.' });
          } catch {
            setAvatarMessage({ type: 'error', text: 'No se pudo cargar tu foto.' });
          }
        }
      } catch (err) {
        console.error('[ProfilePage] Error:', err);
      } finally {
        setAvatarLoading(false);
      }
    };
    load();
  }, []);

  useEffect(() => {
    return () => { if (previewUrl) URL.revokeObjectURL(previewUrl); };
  }, [previewUrl]);

  // ── Carrusel ───────────────────────────────────────────────────────────────
  const navigateCarousel = (dir) => {
    if (!themes.length || loadingTheme) return;
    setCarouselFade('out');
    setTimeout(() => {
      setCarouselIdx(prev => (prev + dir + themes.length) % themes.length);
      setCarouselFade('in');
    }, 120);
  };

  const jumpCarousel = (i) => {
    if (i === carouselIdx) return;
    setCarouselFade('out');
    setTimeout(() => { setCarouselIdx(i); setCarouselFade('in'); }, 120);
  };

  // ── Tema ───────────────────────────────────────────────────────────────────
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

  // ── Form ───────────────────────────────────────────────────────────────────
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSave = async () => {
    if (!profileId) return;
    setLoading(true);
    try {
      await updateProfile(profileId, {
        fullName: formData.nombreCompleto,
        bio: formData.bio,
      });
      localStorage.setItem('name', formData.nombreCompleto);
    } catch (err) {
      console.error('[ProfilePage] Error guardando perfil:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    await performLogout();
    window.location.href = '/login';
  };

  // ── Avatar ─────────────────────────────────────────────────────────────────
  const openAvatarPicker = () => avatarInputRef.current?.click();

  const handleAvatarChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const ok = ALLOWED_AVATAR_TYPES.includes(file.type) || /\.(jpe?g|png|webp)$/i.test(file.name);
    if (!ok) { setAvatarMessage({ type: 'error', text: 'Solo JPG, PNG o WEBP.' }); e.target.value = ''; return; }
    if (file.size > MAX_AVATAR_SIZE_MB * 1024 * 1024) { setAvatarMessage({ type: 'error', text: `Máx. ${MAX_AVATAR_SIZE_MB} MB.` }); e.target.value = ''; return; }
    setPreviewUrl(URL.createObjectURL(file));
    await uploadAvatar(file);
    e.target.value = '';
  };

  const uploadAvatar = async (file) => {
    if (!profileId) { setAvatarMessage({ type: 'error', text: 'No se encontró tu perfil.' }); return; }
    setUploadingAvatar(true);
    setAvatarMessage(null);
    try {
      const uploaded = await uploadProfilePicture(file);
      const newId = uploaded?.storedFileId;
      if (!newId) throw new Error('No se pudo subir la imagen.');
      await updateProfile(profileId, { storedFileId: newId });
      const newUrl = await getProfilePictureUrl(newId);
      setAvatarUrl(newUrl || null);
      setPreviewUrl(null);
      setAvatarMessage({ type: 'success', text: 'Foto actualizada.' });
    } catch (err) {
      setPreviewUrl(null);
      setAvatarMessage({ type: 'error', text: err.message || 'No se pudo actualizar la foto.' });
    } finally {
      setUploadingAvatar(false);
    }
  };

  const languages = [
    { id: 'es', label: 'Español (ES)' },
    { id: 'en', label: 'English (EN)' },
    { id: 'fr', label: 'Français (FR)' },
    { id: 'pt', label: 'Português (PT)' },
  ];

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <div className="min-h-screen bg-background text-textPrimary font-sans">

      {/* ══ MAIN ══════════════════════════════════════════════════════════════ */}
      <main className="max-w-[1100px] mx-auto px-6 pt-8 pb-20 relative z-10">

        {/* ── Header Card ──────────────────────────────────────────────────── */}
        <div className="theme-hero-section relative rounded-2xl p-8 mb-8 overflow-hidden">
          {/*
            theme-hero-section ya aplica: bg-surface/backdrop, border primary,
            box-shadow — definido en global.css y respeta el tema activo.
          */}
          {/* SVG decorativo — cubre toda la card, polígonos con tokens del tema */}
          <svg
            aria-hidden="true"
            className="absolute inset-0 w-full h-full rounded-2xl pointer-events-none"
            viewBox="0 0 700 200"
            preserveAspectRatio="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <polygon
              points="0,0 320,0 200,200 0,200"
              style={{ fill: 'var(--color-primaryDark)', opacity: 0.10 }}
            />
            <polygon
              points="180,0 620,0 500,200 60,200"
              style={{ fill: 'var(--color-primary)', opacity: 0.07 }}
            />
            <polygon
              points="460,0 700,0 700,200 340,200"
              style={{ fill: 'var(--color-primary)', opacity: 0.06 }}
            />
            <polygon
              points="580,0 700,0 700,80"
              style={{ fill: 'var(--color-accent)', opacity: 0.06 }}
            />
          </svg>

          <div className="flex flex-col md:flex-row items-center md:items-end gap-8 relative z-10">

            {/* Avatar */}
            <div className="relative flex-shrink-0">
              {/* Marco del avatar — usa tokens del tema, no hex */}
              <div
                className="w-32 h-32 rounded-xl overflow-hidden border-2 flex items-center justify-center text-5xl font-bold bg-gradient-to-br from-primary to-primaryDark text-textPrimary"
                style={{ borderColor: 'var(--color-primary)', boxShadow: 'color-mix(in srgb, var(--color-primary) 30%, transparent) 0 4px 20px' }}
              >
                {avatarLoading ? (
                  <Loader2 className="w-10 h-10 animate-spin text-textPrimary" />
                ) : (avatarUrl || previewUrl) ? (
                  <img src={previewUrl || avatarUrl} alt="Foto de perfil" className="w-full h-full object-cover" />
                ) : (
                  <img src="/default-avatar.png" alt="Avatar" className="w-full h-full object-cover" />
                )}
                {uploadingAvatar && (
                  <div className="absolute inset-0 bg-background/60 flex items-center justify-center">
                    <Loader2 className="w-8 h-8 animate-spin text-textPrimary" />
                  </div>
                )}
              </div>

              {/* Botón cámara — bg-primary, token del tema */}
              <button
                type="button"
                onClick={openAvatarPicker}
                disabled={uploadingAvatar}
                className="absolute -bottom-2 -right-2 w-9 h-9 rounded-full bg-primary hover:bg-primaryDark text-textPrimary flex items-center justify-center border-2 z-20 transition-all hover:scale-110 disabled:opacity-50 disabled:cursor-not-allowed"
                style={{ borderColor: 'var(--color-background)' }}
              >
                <Camera className="w-4 h-4" />
              </button>

              {/* Botón info */}
              <div className="absolute -bottom-2 -left-2 z-20">
                <Tooltip
                  content={`Solo JPG, PNG o WEBP. Máx. ${MAX_AVATAR_SIZE_MB} MB. Sin contenido adulto.`}
                  position="right"
                >
                  <button
                    type="button"
                    aria-label="Requisitos de imagen"
                    onClick={e => e.stopPropagation()}
                    className="w-7 h-7 rounded-full bg-surfaceAlt text-textMuted flex items-center justify-center border-2"
                    style={{ borderColor: 'var(--color-background)' }}
                  >
                    <Info className="w-3.5 h-3.5" />
                  </button>
                </Tooltip>
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

            {/* Nombre + email + badge */}
            <div className="flex-1 text-center md:text-left">
              <h2 className="text-3xl font-black text-textPrimary tracking-tight leading-tight">
                {formData.nombreCompleto}
              </h2>
              <div className="flex flex-wrap justify-center md:justify-start items-center gap-3 mt-2">
                <p className="text-sm font-medium text-accent flex items-center gap-1.5">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/>
                  </svg>
                  {userEmail}
                </p>
                {/* Badge de rol — mismo estilo que los badges del navbar */}
                <span className="px-3 py-1 rounded-full bg-primary/20 border border-primary/40 text-accent text-[10px] font-bold uppercase tracking-wider">
                  {roleLabel}
                </span>
              </div>

              {avatarMessage && (
                <div className={`mt-3 inline-flex items-center gap-2 rounded-lg px-3 py-1.5 text-xs font-semibold border ${
                  avatarMessage.type === 'success'
                    ? 'badge-success-fixed'
                    : 'bg-error/10 text-error border-error/20'
                }`}>
                  {avatarMessage.type === 'success'
                    ? <CheckCircle className="w-3.5 h-3.5" />
                    : <AlertCircle className="w-3.5 h-3.5" />
                  }
                  {avatarMessage.text}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* ── Content Grid ─────────────────────────────────────────────────── */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">

          {/* ── Columna izquierda ──────────────────────────────────────────── */}
          <div className="lg:col-span-2 space-y-8">

            {/* Información Personal */}
            <section className="theme-profile-section rounded-2xl p-8">
              <h3 className="text-lg font-bold text-textPrimary mb-6 flex items-center gap-2">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="var(--color-primary)">
                  <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
                </svg>
                Información Personal
              </h3>

              <div className="space-y-5">
                {/* Nombre completo */}
                <div>
                  <label className="block text-xs font-semibold text-textMuted uppercase tracking-wider mb-2">
                    Nombre Completo
                  </label>
                  <input
                    className={inputCls}
                    type="text"
                    name="nombreCompleto"
                    value={formData.nombreCompleto}
                    onChange={handleInputChange}
                    placeholder="Tu nombre completo"
                  />
                </div>

                {/* Biografía */}
                <div>
                  <label className="block text-xs font-semibold text-textMuted uppercase tracking-wider mb-2">
                    Biografía
                  </label>
                  <textarea
                    className={`${inputCls} resize-none`}
                    name="bio"
                    rows={3}
                    value={formData.bio}
                    onChange={handleInputChange}
                    placeholder="Cuéntanos algo sobre ti…"
                    maxLength={255}
                  />
                  <p className="text-xs text-textMuted mt-1 text-right">{formData.bio.length}/255</p>
                </div>
              </div>

              <button
                onClick={handleSave}
                disabled={loading}
                className="mt-6 px-6 py-2.5 rounded-xl text-sm font-bold bg-primary hover:bg-primaryDark text-textPrimary transition-all disabled:opacity-50"
                style={{ boxShadow: 'color-mix(in srgb, var(--color-primary) 30%, transparent) 0 2px 12px' }}
              >
                {loading ? 'Guardando…' : 'Guardar cambios'}
              </button>
            </section>

            {/* Seguridad */}
            <section className="theme-profile-section rounded-2xl p-8">
              <h3 className="text-lg font-bold text-textPrimary mb-6 flex items-center gap-2">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="var(--color-primary)">
                  <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z"/>
                </svg>
                Seguridad de la Cuenta
              </h3>

              {/* Cerrar sesión — acción neutra, outline primary */}
              <div className="flex items-center justify-between py-4 border-b border-surfaceAlt">
                <div>
                  <p className="text-sm font-semibold text-textPrimary">Cerrar sesión</p>
                  <p className="text-xs text-textSecondary mt-0.5">Salir de la cuenta en este dispositivo.</p>
                </div>
                <button
                  onClick={handleLogout}
                  className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-semibold text-accent border border-primary/30 hover:bg-primary/10 hover:border-primary/50 transition-all"
                >
                  <LogOut className="w-3.5 h-3.5" />
                  Cerrar sesión
                </button>
              </div>

              {/* Zona de peligro — borde error sutil, sin gradiente rosa */}
              <div className="mt-5 rounded-xl p-5 bg-error/[0.04] border border-error/25">
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                  <div>
                    <p className="text-sm font-bold text-error">Zona de Peligro</p>
                    <p className="text-xs text-textSecondary mt-0.5">
                      Una vez que desactives tu cuenta, no podrás revertir esta acción.
                    </p>
                  </div>
                  <button
                    onClick={() => setDeactivateModalOpen(true)}
                    className="flex-shrink-0 px-4 py-2 rounded-xl text-sm font-bold text-error border border-error/35 hover:bg-error/10 transition-all"
                  >
                    Desactivar Cuenta
                  </button>
                </div>
              </div>
            </section>
          </div>

          {/* ── Columna derecha ────────────────────────────────────────────── */}
          <div className="space-y-8">

            {/* Idioma */}
            <section className="theme-profile-section rounded-2xl p-6">
              <h3 className="text-base font-bold text-textPrimary mb-4">Idioma</h3>
              <div className="space-y-2">
                {languages.map(lang => (
                  <button
                    key={lang.id}
                    onClick={() => setSelectedLanguage(lang.id)}
                    className={`flex items-center justify-between w-full px-3.5 py-2.5 rounded-xl transition-all text-left ${
                      selectedLanguage === lang.id
                        ? 'bg-primary/10 border border-primary/40'
                        : 'bg-surface border border-surfaceAlt hover:border-primary/30'
                    }`}
                  >
                    <span className={`text-sm font-medium ${
                      selectedLanguage === lang.id ? 'text-textPrimary' : 'text-textSecondary'
                    }`}>
                      {lang.label}
                    </span>

                    {/* Radio button — borde siempre visible, relleno primary cuando activo */}
                    <span
                      className="w-4 h-4 rounded-full flex items-center justify-center flex-shrink-0 transition-all"
                      style={{
                        border: `2px solid ${selectedLanguage === lang.id ? 'var(--color-primary)' : 'var(--color-surfaceAlt)'}`,
                        background: selectedLanguage === lang.id ? 'var(--color-primary)' : 'transparent',
                      }}
                    >
                      {selectedLanguage === lang.id && (
                        <span className="w-1.5 h-1.5 rounded-full bg-textPrimary block" />
                      )}
                    </span>
                  </button>
                ))}
              </div>
            </section>

            {/* Tema Visual — carrusel compacto */}
            <section className="theme-profile-section rounded-2xl p-6">
              <h3 className="text-base font-bold text-textPrimary mb-4">Tema Visual</h3>

              {themes.length === 0 ? (
                <p className="text-textMuted text-sm text-center py-6">Cargando temas…</p>
              ) : (() => {
                const theme = themes[carouselIdx];
                const preview = THEME_PREVIEWS[theme.name] ?? THEME_PREVIEWS.PRINCIPAL;
                const isActive = activeThemeId === theme.id;
                const isCustomisable = theme.name !== 'PRINCIPAL';

                return (
                  <div>
                    {/* Card del tema visible */}
                    <div
                      className={`rounded-xl p-4 transition-opacity duration-150 ${
                        isActive ? 'bg-primary/10 border border-primary/35' : 'bg-surface border border-surfaceAlt'
                      }`}
                      style={{ opacity: carouselFade === 'out' ? 0 : 1 }}
                    >
                      {/* Nombre + badge Activo */}
                      <div className="flex items-center justify-between mb-3">
                        <span className={`text-sm font-bold ${isActive ? 'text-textPrimary' : 'text-textSecondary'}`}>
                          {theme.name}
                        </span>
                        {isActive && (
                          <span className="text-[9px] font-black uppercase tracking-widest px-2 py-0.5 rounded-sm bg-primary/20 border border-primary/40 text-accent">
                            Activo
                          </span>
                        )}
                      </div>

                      {/* Preview 3 círculos — colores literales solo en los swatches */}
                      <div className="flex items-center gap-2 mb-4">
                        {[preview.background, preview.primary, preview.accent].map((color, i) => (
                          <span
                            key={i}
                            className="w-7 h-7 rounded-full border border-surfaceAlt"
                            style={{ background: color }}
                          />
                        ))}
                      </div>

                      {/* Botón activar — solo si no está activo */}
                      {!isActive && (
                        <button
                          onClick={() => handleThemeChange(theme.id)}
                          disabled={loadingTheme}
                          className="w-full py-2 rounded-lg text-xs font-bold bg-primary/10 border border-primary/30 text-accent hover:bg-primary/20 transition-all disabled:opacity-50"
                        >
                          {loadingTheme ? 'Aplicando…' : 'Aplicar tema'}
                        </button>
                      )}
                    </div>

                    {/* Controles de navegación */}
                    <div className="flex items-center justify-between mt-3">
                      <button
                        onClick={() => navigateCarousel(-1)}
                        disabled={loadingTheme}
                        className="w-8 h-8 rounded-lg bg-surface border border-surfaceAlt text-textSecondary hover:border-primary/40 hover:text-accent flex items-center justify-center transition-all disabled:opacity-40"
                        aria-label="Tema anterior"
                      >
                        <ChevronLeft className="w-4 h-4" />
                      </button>

                      {/* Carousel dots */}
                      <div className="flex gap-1.5 items-center">
                        {themes.map((_, i) => (
                          <button
                            key={i}
                            onClick={() => jumpCarousel(i)}
                            className="rounded-full transition-all"
                            style={{
                              width: i === carouselIdx ? '16px' : '6px',
                              height: '6px',
                              background: i === carouselIdx
                                ? 'var(--color-primary)'
                                : 'var(--color-surfaceAlt)',
                            }}
                            aria-label={`Ir a tema ${i + 1}`}
                          />
                        ))}
                      </div>

                      <button
                        onClick={() => navigateCarousel(1)}
                        disabled={loadingTheme}
                        className="w-8 h-8 rounded-lg bg-surface border border-surfaceAlt text-textSecondary hover:border-primary/40 hover:text-accent flex items-center justify-center transition-all disabled:opacity-40"
                        aria-label="Tema siguiente"
                      >
                        <ChevronRight className="w-4 h-4" />
                      </button>
                    </div>

                    {/* Personalizar — todos los temas excepto PRINCIPAL */}
                    {isCustomisable && (
                      isActive ? (
                        /* Tema activo → botón directo */
                        <button
                          onClick={() => setCustomizePanelOpen(true)}
                          className="tcp-customize-btn mt-3"
                        >
                          <Palette className="w-3.5 h-3.5 flex-shrink-0" />
                          Personalizar colores
                        </button>
                      ) : (
                        /* Tema no activo → indicación de que hay que activarlo primero */
                        <p className="text-xs text-textSecondary mt-3 text-center">
                          Activa este tema para personalizarlo
                        </p>
                      )
                    )}
                  </div>
                );
              })()}
            </section>

          </div>
        </div>
      </main>

      <Footer />

      {customizePanelOpen && (() => {
        const activeTheme = themes.find(t => t.id === activeThemeId);
        return (
          <ThemeCustomizePanel
            themeName={activeTheme?.name ?? 'DARK'}
            onClose={() => setCustomizePanelOpen(false)}
          />
        );
      })()}

      <DeactivateAccountModal
        isOpen={deactivateModalOpen}
        onClose={() => setDeactivateModalOpen(false)}
      />
    </div>
  );
};

export default ProfilePage;
