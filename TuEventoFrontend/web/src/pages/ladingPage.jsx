import { Users, Gift, Smartphone, Globe, CheckCircle, Download } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import Footer from '../layouts/Footer';
import BaseModal from '../components/common/BaseModal';


export default function LadingPage() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [userData, setUserData] = useState(null);
  const [showApkModal, setShowApkModal] = useState(false);

  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const oauthToken = urlParams.get('token');
    const oauthUserID = urlParams.get('userID');
    const oauthRole = urlParams.get('role');
    const isOAuth = urlParams.get('oauth');

    const token = localStorage.getItem('token');
    const storedUserID = localStorage.getItem('userID');
    const storedAlias = localStorage.getItem('alias');
    const storedFullName = localStorage.getItem('name');
    if (token && storedUserID) {
      setUserData({ userId: storedUserID, alias: storedAlias, fullName: storedFullName });
    }
  }, []);

  return (
    <div className="min-h-screen bg-background text-textPrimary">

      {/* Hero Section */}
      <section className="relative overflow-hidden pt-16 md:pt-20">

        {/* Fondo: gradiente primary → primaryDark, idéntico al welcome-modal-header */}
        <div className="hero-section-bg" />

        {/* Capa de profundidad sutil (da la misma sensación de los pseudo-elementos del modal) */}
        <div className="hero-section-depth" />

        {/* Círculos decorativos con blur — misma lógica que ::before/::after del modal */}
        <div className="hero-section-circle-tr" />
        <div className="hero-section-circle-bl" />
        <div className="hero-section-circle-ml" />

        {/* Contenido principal */}
        <div className="relative max-w-6xl mx-auto px-4 pb-25">
          <div className="grid md:grid-cols-2 gap-12 items-center">

            {/* Texto */}
            <div className="space-y-6 pt-8">
              <h1 className="text-4xl md:text-6xl font-bold leading-tight hero-section-title">
                {userData ? (() => {
                  if (!userData?.fullName) return t('landing.hero.greetingFallback', { alias: userData.alias });
                  const name = userData.fullName;
                  const parts = name.split(' ').filter(part => part.trim().length > 0);
                  if (parts.length === 0) return t('landing.hero.greetingFallback', { alias: userData.alias });
                  if (parts.length === 1) return t('landing.hero.greeting', { name: parts[0] });
                  const firstName = parts[0];
                  const lastName = parts[1];
                  if (firstName.length <= 3) return t('landing.hero.greeting', { name: `${firstName} ${lastName}` });
                  return t('landing.hero.greeting', { name: firstName });
                })() : t('landing.hero.mainTitle')}
                {/* Antes: text-accent → lavanda #A78BFA sobre primary #7C3AED ≈ 2.4:1 (falla WCAG AA).
                    Ahora: hero-section-accent-text → onPrimary 90% ≥ 4.5:1 en todos los temas. */}
                <span className="block hero-section-accent-text">
                  {userData ? t('landing.hero.welcomeBack') : t('landing.hero.subtitle')}
                </span>
              </h1>
              {/* Párrafo: onPrimary 78% — mismo ratio que welcome-modal-header-sub */}
              <p className="text-lg hero-section-subtitle">
                {t('landing.hero.description')}
              </p>
              {/* Botón: gradiente accent→primary idéntico a welcome-modal-btn / modal-btn-cta */}
              {!userData && (
                <button
                  className="hero-section-btn"
                  onClick={() => navigate('/login')}
                >
                  {t('landing.hero.startNow')}
                </button>
              )}
            </div>

            {/* Imagen */}
            <div className="relative">
              <div
                className="w-60 h-60 rounded-full opacity-20 absolute -top-10 -right-10"
                style={{ background: 'linear-gradient(to right, var(--color-accent), var(--color-primary))' }}
              />
              <div
                className="relative rounded-2xl p-6"
                style={{
                  background: 'rgba(255,255,255,0.1)',
                  backdropFilter: 'blur(8px)',
                  border: '1px solid rgba(255,255,255,0.2)',
                }}
              >
                {/* Contenedor fijo */}
                <div className="w-full h-55 mb-4">
                  <img
                    src="/src/assets/images/ladingpage.png"
                    alt="ladingpage"
                    className="w-120 h-70 relative -top-8 rounded-xl object-contain"
                  />
                </div>
                <div className="space-y-2">
                  <div className="h-4 rounded w-3/4" style={{ background: 'rgba(255,255,255,0.3)' }} />
                  <div className="h-4 rounded w-1/2" style={{ background: 'rgba(255,255,255,0.3)' }} />
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Curva inferior */}
        <svg
          viewBox="0 0 1200 120"
          className="absolute bottom-0 left-0 w-full h-16 fill-background"
          preserveAspectRatio="none"
        >
          <path d="M0,60 Q150,0 300,60 T600,60 Q750,120 900,60 T1200,60 L1200,120 L0,120 Z" />
        </svg>
      </section>

      {/* Features Section */}
      <section className="py-20 relative overflow-hidden">
        <div
          className="absolute bottom-0 left-0 w-72 h-72 rounded-full opacity-20 translate-y-1/2 -translate-x-1/4"
          style={{ background: 'linear-gradient(to right, var(--color-primary), transparent)' }}
        />

        <div className="max-w-6xl mx-auto px-4 relative z-10">
          {/* Primera sección */}
          <div className="grid md:grid-cols-2 gap-12 items-center mb-20">
            <div className="space-y-6">
              <h2 className="text-3xl md:text-4xl font-bold text-textPrimary leading-tight">
                {t('landing.features.section1.title')}
                <span className="block">{t('landing.features.section1.titleAccent')}</span>
              </h2>
              <p className="text-lg text-textSecondary">
                {t('landing.features.section1.description')}
              </p>
            </div>

            <div className="relative">
              <div
                className="rounded-2xl p-6 shadow-2xl"
                style={{ background: 'linear-gradient(135deg, var(--color-surface), var(--color-background))' }}
              >
                <div className="rounded-xl mb-4 overflow-hidden h-56">
                  <img
                    src="/src/assets/images/ladingpage1.png"
                    alt="evento"
                    className="w-full h-full object-cover"
                  />
                </div>
                <div className="space-y-2">
                  <div className="h-3 bg-surfaceAlt rounded w-3/4" />
                  <div className="h-3 bg-surfaceAlt rounded w-1/2" />
                </div>
              </div>
            </div>
          </div>

          {/* Separador */}
          <div className="flex items-center gap-6 mb-16 px-4">
            <div className="flex-1 h-px" style={{ background: 'linear-gradient(to right, transparent, var(--color-primary), var(--color-accent))' }} />
            <div className="flex items-center gap-2 shrink-0">
              <div className="w-2 h-2 rounded-full bg-accent animate-pulse" />
              <div className="w-3 h-3 rounded-full bg-primary animate-pulse" />
              <div className="w-2 h-2 rounded-full bg-accent animate-pulse" />
            </div>
            <div className="flex-1 h-px" style={{ background: 'linear-gradient(to left, transparent, var(--color-primary), var(--color-accent))' }} />
          </div>

          {/* Segunda sección */}
          <div className="grid md:grid-cols-2 gap-12 items-center">
            <div className="relative order-2 md:order-1">
              <div
                className="rounded-2xl p-6 shadow-2xl"
                style={{ background: 'linear-gradient(135deg, var(--color-surface), var(--color-background))' }}
              >
                <div className="aspect-video overflow-hidden rounded-xl">
                  <img
                    src="/src/assets/images/ladingpage2.png"
                    alt="Búsqueda de eventos"
                    className="w-full h-full object-cover"
                  />
                </div>
                <div className="space-y-2 mt-4">
                  <div className="h-3 bg-surfaceAlt rounded w-2/3" />
                  <div className="h-3 bg-surfaceAlt rounded w-1/2" />
                </div>
              </div>
            </div>

            <div className="space-y-6 order-1 md:order-2">
              <h3 className="text-3xl md:text-4xl font-bold text-textPrimary leading-tight">
                {t('landing.features.section2.title')}
                <span className="block">{t('landing.features.section2.titleAccent')}</span>
              </h3>
              <p className="text-textSecondary text-lg">
                {t('landing.features.section2.description')}
              </p>
              <button
                onClick={() => navigate('/events')}
                className="px-8 py-3 rounded-full font-semibold transition-all duration-300 shadow-lg hover:shadow-xl text-textPrimary"
                style={{ background: 'linear-gradient(135deg, var(--color-accent), var(--color-primary))' }}
              >
                {t('landing.features.section2.button')}
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* Benefits Section */}
      <section className="py-20 bg-background">
        <div className="max-w-6xl mx-auto px-4">
          <div className="text-center mb-16">
            <h2 className="text-3xl md:text-4xl font-bold text-textPrimary mb-8">
              {t('landing.benefits.title')}
            </h2>
          </div>

          <div className="grid md:grid-cols-3 gap-8">
            {[
              { 
                icon: CheckCircle, 
                titleKey: 'landing.benefits.items.timesSaving.title', 
                textKey: 'landing.benefits.items.timesSaving.description'
              },
              { 
                icon: Users, 
                titleKey: 'landing.benefits.items.findEvents.title', 
                textKey: 'landing.benefits.items.findEvents.description'
              },
              { 
                icon: Gift, 
                titleKey: 'landing.benefits.items.free.title', 
                textKey: 'landing.benefits.items.free.description'
              },
            ].map(({ icon: Icon, titleKey, textKey }) => (
              <div key={titleKey} className="text-center space-y-4">
                <div className="w-16 h-16 bg-primary rounded-full flex items-center justify-center mx-auto">
                  <Icon className="w-8 h-8 text-textPrimary" />
                </div>
                <h3 className="text-xl font-semibold text-textPrimary">{t(titleKey)}</h3>
                <p className="text-textMuted text-sm">{t(textKey)}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Compatibility Section */}
      <section className="py-20 bg-surface">
        <div className="max-w-2xl mx-auto px-4 text-center">
          <h2 className="text-3xl md:text-4xl font-bold text-textPrimary mb-12">
            {t('landing.compatibility.title')}
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 justify-items-center">
            <div className="space-y-4">
              <div className="w-20 h-20 bg-surfaceAlt rounded-2xl flex items-center justify-center mx-auto">
                <Globe className="w-10 h-10 text-textPrimary" />
              </div>
              <h3 className="text-xl font-semibold text-textPrimary">{t('landing.compatibility.web')}</h3>
            </div>

            <div className="space-y-4">
              <div className="w-20 h-20 bg-surfaceAlt rounded-2xl flex items-center justify-center mx-auto">
                <Smartphone className="w-10 h-10 text-textPrimary" />
              </div>
              <h3 className="text-xl font-semibold text-textPrimary">{t('landing.compatibility.android')}</h3>
              <button
                onClick={() => setShowApkModal(true)}
                className="bg-primary hover:bg-primaryDark text-textPrimary px-6 py-2 rounded-lg font-semibold transition-colors"
              >
                {t('landing.compatibility.downloadApk')}
              </button>
            </div>
          </div>
        </div>
      </section>

      <Footer />

      {/* ── BaseModal: descarga APK ──────────────────────────────────── */}
      <BaseModal
        isOpen={showApkModal}
        onClose={() => setShowApkModal(false)}
        variant="info"
        icon={<Download className="w-8 h-8" />}
        title={t('landing.apkModal.title')}
        subtitle={t('landing.apkModal.subtitle')}
        actions={[
          {
            label: t('landing.apkModal.download'),
            icon: <Download className="w-4 h-4" />,
            variant: 'primary',
            onClick: () => {
              window.open(
                'https://drive.google.com/file/d/1VeECC-bj0F9i-5uczP3Si0FP5c6N8LE8/view?usp=drivesdk',
                '_blank',
                'noopener,noreferrer'
              );
              setShowApkModal(false);
            },
          },
          {
            label: t('landing.apkModal.cancel'),
            variant: 'secondary',
            onClick: () => setShowApkModal(false),
          },
        ]}
      >
        <p className="bm-hint text-center text-sm leading-relaxed">
          {t('landing.apkModal.description')}
        </p>
      </BaseModal>

    </div>
  );
}
