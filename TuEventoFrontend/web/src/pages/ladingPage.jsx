import { Users, Gift, Smartphone, Globe, CheckCircle, Download } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import Footer from '../layouts/Footer';
import BaseModal from '../components/common/BaseModal';


export default function LadingPage() {
  const navigate = useNavigate();
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
                  if (!userData?.fullName) return `¡Hola, ${userData.alias}!`;
                  const name = userData.fullName;
                  const parts = name.split(' ').filter(part => part.trim().length > 0);
                  if (parts.length === 0) return `¡Hola, ${userData.alias}!`;
                  if (parts.length === 1) return `¡Hola, ${parts[0]}!`;
                  const firstName = parts[0];
                  const lastName = parts[1];
                  if (firstName.length <= 3) return `¡Hola, ${firstName} ${lastName}!`;
                  return `¡Hola, ${firstName}!`;
                })() : 'Visualiza el evento'}
                {/* Antes: text-accent → lavanda #A78BFA sobre primary #7C3AED ≈ 2.4:1 (falla WCAG AA).
                    Ahora: hero-section-accent-text → onPrimary 90% ≥ 4.5:1 en todos los temas. */}
                <span className="block hero-section-accent-text">
                  {userData ? 'Bienvenido de vuelta.' : 'ideal.'}
                </span>
              </h1>
              {/* Párrafo: onPrimary 78% — mismo ratio que welcome-modal-header-sub */}
              <p className="text-lg hero-section-subtitle">
                Diseña, planifica y vive experiencias únicas que marquen la diferencia.
                Cada detalle cuenta y nuestro kit de asistencia lo hace realidad.
              </p>
              {/* Botón: gradiente accent→primary idéntico a welcome-modal-btn / modal-btn-cta */}
              <button
                className="hero-section-btn"
                onClick={() => navigate('/login')}
              >
                Comenzar ahora
              </button>
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
                Organiza, diseña y vive eventos
                <span className="block">sin límites.</span>
              </h2>
              <p className="text-lg text-textSecondary">
                Construye eventos memorables con nuestro creador de planos interactivos en línea.
                Diseña espacios personalizados, gestiona asistentes y visualiza cada detalle de tu
                evento en tiempo real, todo en una sola plataforma.
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
                Solicitud de
                <span className="block">Eventos excepcionales.</span>
              </h3>
              <p className="text-textSecondary text-lg">
                Si buscas nuevas experiencias, revisa nuestra lista de eventos disponibles.
                Explora todas las opciones y encuentra el plan perfecto para ti.
              </p>
              <button
                onClick={() => navigate('/events')}
                className="px-8 py-3 rounded-full font-semibold transition-all duration-300 shadow-lg hover:shadow-xl text-textPrimary"
                style={{ background: 'linear-gradient(135deg, var(--color-accent), var(--color-primary))' }}
              >
                Revisa los eventos disponibles
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
              ¿Por qué deberías elegirnos?
            </h2>
          </div>

          <div className="grid md:grid-cols-3 gap-8">
            {[
              { icon: CheckCircle, title: 'Ahorra tiempo en colas', text: 'Dale a tus clientes un proceso de planificación de eventos, reduciendo el procesamiento con ayudantes expertos y elimina las filas.' },
              { icon: Users,       title: 'Encuentra eventos fácilmente', text: 'Nuestro sistema te permite encontrar el asistente desde una interfaz accesible, útil que te permitirá confirmar detalles de su evento tan rápido como lo desees sin complicaciones.' },
              { icon: Gift,        title: 'Totalmente gratuito.', text: 'Acceso a las funciones básicas de nuestro plano gratis que te convierte en un agente accesible tanto para pequeños negocios como para grandes compañías.' },
            ].map(({ icon: Icon, title, text }) => (
              <div key={title} className="text-center space-y-4">
                <div className="w-16 h-16 bg-primary rounded-full flex items-center justify-center mx-auto">
                  <Icon className="w-8 h-8 text-textPrimary" />
                </div>
                <h3 className="text-xl font-semibold text-textPrimary">{title}</h3>
                <p className="text-textMuted text-sm">{text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Compatibility Section */}
      <section className="py-20 bg-surface">
        <div className="max-w-2xl mx-auto px-4 text-center">
          <h2 className="text-3xl md:text-4xl font-bold text-textPrimary mb-12">
            Dispositivos con compatibilidad.
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 justify-items-center">
            <div className="space-y-4">
              <div className="w-20 h-20 bg-surfaceAlt rounded-2xl flex items-center justify-center mx-auto">
                <Globe className="w-10 h-10 text-textPrimary" />
              </div>
              <h3 className="text-xl font-semibold text-textPrimary">WEB</h3>
            </div>

            <div className="space-y-4">
              <div className="w-20 h-20 bg-surfaceAlt rounded-2xl flex items-center justify-center mx-auto">
                <Smartphone className="w-10 h-10 text-textPrimary" />
              </div>
              <h3 className="text-xl font-semibold text-textPrimary">ANDROID</h3>
              <button
                onClick={() => setShowApkModal(true)}
                className="bg-primary hover:bg-primaryDark text-textPrimary px-6 py-2 rounded-lg font-semibold transition-colors"
              >
                Descargar APK
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
        title="Aplicación Móvil TuEvento"
        subtitle="Disponible para dispositivos Android"
        actions={[
          {
            label: 'Descargar APK',
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
            label: 'Cancelar',
            variant: 'secondary',
            onClick: () => setShowApkModal(false),
          },
        ]}
      >
        <p className="bm-hint text-center text-sm leading-relaxed">
          Descarga la aplicación móvil para acceder a todas las funciones de TuEvento
          desde tu dispositivo Android.
        </p>
      </BaseModal>

    </div>
  );
}
