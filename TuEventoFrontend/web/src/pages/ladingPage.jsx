import {
  Smartphone, Globe, Download, ArrowRight, Star,
  Ticket, ChevronRight, Map, Bell, CreditCard,
  BarChart2, Navigation, Tag, MessageCircle,
  Headphones,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import Footer from '../layouts/Footer';
import BaseModal from '../components/common/BaseModal';
import { getAllEvents } from '../services/EventService';
import { getEventComments } from '../services/EventCommentService';
import { getAccessToken } from '../services/tokenStore.js';

/* ── Pasos "Cómo funciona" ─────────────────────────────────────────────── */
const STEPS = [
  { Icon: ChevronRight, label: '01', title: 'Crea tu cuenta',    desc: 'Regístrate en segundos. Sin tarjeta de crédito, sin pasos innecesarios.' },
  { Icon: Map,          label: '02', title: 'Diseña el espacio', desc: 'Usa el editor de planos para armar secciones, sillas y zonas personalizadas.' },
  { Icon: Ticket,       label: '03', title: 'Vende entradas',    desc: 'Activa la venta de tickets y gestiona pagos de forma segura.' },
  { Icon: Star,         label: '04', title: 'Vive el evento',    desc: 'Administra asistentes en tiempo real y recibe feedback de tu audiencia.' },
];

/* ── Features ──────────────────────────────────────────────────────────── */
const FEATURES = [
  { Icon: Map,         title: 'Editor de planos',       desc: 'Crea la distribución de tu evento con un editor visual drag & drop en tiempo real.' },
  { Icon: Bell,        title: 'Notificaciones',          desc: 'Recordatorios, cambios y confirmaciones llegan automáticamente a tus asistentes.' },
  { Icon: CreditCard,  title: 'Pagos integrados',        desc: 'Pasarela de pago segura. Cobra entradas y recibe tu dinero sin intermediarios.' },
  { Icon: BarChart2,   title: 'Analíticas',              desc: 'Métricas de ventas y asistencia en un panel claro para tomar mejores decisiones.' },
  { Icon: Navigation,  title: 'Geolocalización',         desc: 'Los asistentes descubren eventos cerca de ellos filtrando por ciudad y categoría.' },
  { Icon: Tag,         title: 'Categorías',              desc: 'Clasifica tu evento entre música, cultura, deporte, gastronomía y mucho más.' },
];

/* ═══════════════════════════════════════════════════════════════════════ */
export default function LadingPage() {
  const navigate = useNavigate();

  const [userData,       setUserData]       = useState(null);
  const [topReviews,     setTopReviews]     = useState([]);
  const [reviewsLoading, setReviewsLoading] = useState(true);
  const [showApkModal,   setShowApkModal]   = useState(false);

  /* Auth */
  useEffect(() => {
    const token = getAccessToken();
    const uid   = localStorage.getItem('userID');
    if (token && uid) {
      setUserData({
        userId: uid,
        alias: localStorage.getItem('alias'),
        fullName: localStorage.getItem('name'),
        role: localStorage.getItem('role'),
      });
    }
  }, []);

  // Re-read role when admin revokes organizer via WS
  useEffect(() => {
    const handleRoleChanged = () => {
      setUserData(prev => prev ? { ...prev, role: localStorage.getItem('role') } : prev);
    };
    window.addEventListener('roleChanged', handleRoleChanged);
    return () => window.removeEventListener('roleChanged', handleRoleChanged);
  }, []);

  /* Ratings reales */
  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const eventsRes = await getAllEvents();
        const events    = eventsRes?.data ?? eventsRes ?? [];
        const candidates = Array.isArray(events) ? events.filter(ev => ev.eventId).slice(0, 8) : [];
        const results = await Promise.all(
          candidates.map(ev =>
            getEventComments(ev.eventId)
              .then(r => ({ event: ev, comments: r?.data ?? r ?? [] }))
              .catch(() => ({ event: ev, comments: [] })),
          ),
        );
        if (cancelled) return;
        const good = results
          .flatMap(({ event, comments }) =>
            (Array.isArray(comments) ? comments : [])
              .filter(c =>
                c.parentRatingId == null && c.rating != null && c.rating >= 4 &&
                c.isVisible !== false && typeof c.comment === 'string' && c.comment.trim().length >= 40,
              )
              .map(c => ({ ...c, eventName: event.name ?? event.title ?? 'TuEvento' })),
          )
          .sort((a, b) => b.rating - a.rating);
        setTopReviews(good.slice(0, 3));
      } catch { /* sin datos, sección oculta */ }
      finally { if (!cancelled) setReviewsLoading(false); }
    }
    void load();
    return () => { cancelled = true; };
  }, []);

  const firstName = (() => {
    if (!userData) return null;
    const parts = (userData.fullName || '').split(' ').filter(Boolean);
    if (!parts.length) return userData.alias;
    return parts[0].length <= 3 && parts.length > 1 ? `${parts[0]} ${parts[1]}` : parts[0];
  })();

  return (
    <div className="min-h-screen bg-background text-textPrimary">

      {/* ══ HERO ══════════════════════════════════════════════════════════ */}
      <section className="lp2-hero pt-16">
        <div className="lp2-orb lp2-orb-3" />

        {/* ── Formas decorativas del hero ───────────────────────────────── */}

        {/* Blob grande superior-izquierda — más opaco, con color accent */}
        <svg className="absolute -top-40 -left-40 pointer-events-none" style={{ opacity: 0.35, animation: 'lp2-float-slow 10s ease-in-out infinite' }} width="560" height="560" viewBox="0 0 560 560" fill="none">
          <circle cx="280" cy="280" r="270" fill="url(#hg1)"/>
          <circle cx="280" cy="280" r="200" stroke="rgba(200,180,255,0.4)" strokeWidth="1.5" strokeDasharray="14 9"/>
          <circle cx="280" cy="280" r="140" stroke="rgba(200,180,255,0.25)" strokeWidth="1" strokeDasharray="7 12"/>
          <defs>
            <radialGradient id="hg1" cx="35%" cy="35%" r="65%">
              <stop offset="0%" stopColor="#c4b5fd" stopOpacity="0.45"/>
              <stop offset="60%" stopColor="#7c3aed" stopOpacity="0.12"/>
              <stop offset="100%" stopColor="#7c3aed" stopOpacity="0"/>
            </radialGradient>
          </defs>
        </svg>

        {/* Anillo giratorio top-left — más grueso y visible */}
        <svg className="absolute top-10 left-14 pointer-events-none" style={{ opacity: 0.35, animation: 'lp2-spin-slow 26s linear infinite' }} width="180" height="180" viewBox="0 0 180 180" fill="none">
          <circle cx="90" cy="90" r="84" stroke="rgba(221,214,254,0.9)" strokeWidth="2" strokeDasharray="12 7"/>
          <circle cx="90" cy="90" r="58" stroke="rgba(221,214,254,0.5)" strokeWidth="1.2" strokeDasharray="6 11"/>
          <circle cx="90" cy="90" r="32" fill="rgba(167,139,250,0.08)" stroke="rgba(221,214,254,0.3)" strokeWidth="0.8"/>
        </svg>

        {/* Rombo flotante derecha — más grande y con fill visible */}
        <svg className="absolute top-16 right-8 pointer-events-none" style={{ opacity: 0.32, animation: 'lp2-float 7s 1s ease-in-out infinite' }} width="150" height="150" viewBox="0 0 150 150" fill="none">
          <polygon points="75,6 144,75 75,144 6,75" fill="url(#hg2)"/>
          <polygon points="75,6 144,75 75,144 6,75" stroke="rgba(221,214,254,0.7)" strokeWidth="1.8" fill="none"/>
          <polygon points="75,30 120,75 75,120 30,75" stroke="rgba(221,214,254,0.35)" strokeWidth="1" fill="none" strokeDasharray="5 6"/>
          <defs>
            <linearGradient id="hg2" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stopColor="#a78bfa" stopOpacity="0.35"/>
              <stop offset="100%" stopColor="#7c3aed" stopOpacity="0.05"/>
            </linearGradient>
          </defs>
        </svg>

        {/* Triángulo bottom-derecha — más grande */}
        <svg className="absolute bottom-20 right-16 pointer-events-none" style={{ opacity: 0.28, animation: 'lp2-float-slow 11s 2s ease-in-out infinite' }} width="110" height="110" viewBox="0 0 110 110" fill="none">
          <polygon points="55,5 105,98 5,98" fill="url(#hg3)" stroke="rgba(221,214,254,0.6)" strokeWidth="1.8"/>
          <defs>
            <linearGradient id="hg3" x1="0.5" y1="0" x2="0.5" y2="1">
              <stop offset="0%" stopColor="#c4b5fd" stopOpacity="0.38"/>
              <stop offset="100%" stopColor="#7c3aed" stopOpacity="0.04"/>
            </linearGradient>
          </defs>
        </svg>

        {/* Cuadrado rotado bottom-izquierda */}
        <svg className="absolute bottom-28 left-1/4 pointer-events-none" style={{ opacity: 0.25, transform: 'rotate(18deg)', animation: 'lp2-float 9s 0.5s ease-in-out infinite' }} width="72" height="72" viewBox="0 0 72 72" fill="none">
          <rect x="4" y="4" width="64" height="64" rx="10" fill="rgba(167,139,250,0.18)" stroke="rgba(221,214,254,0.6)" strokeWidth="1.8" strokeDasharray="6 5"/>
        </svg>

        {/* Cruz / plus centro-top */}
        <svg className="absolute top-14 left-1/2 pointer-events-none" style={{ opacity: 0.28, animation: 'lp2-float-slow 13s 3s ease-in-out infinite' }} width="32" height="32" viewBox="0 0 32 32" fill="none">
          <line x1="16" y1="2" x2="16" y2="30" stroke="rgba(221,214,254,0.9)" strokeWidth="2.5" strokeLinecap="round"/>
          <line x1="2"  y1="16" x2="30" y2="16" stroke="rgba(221,214,254,0.9)" strokeWidth="2.5" strokeLinecap="round"/>
        </svg>

        {/* Hexágono — forma nueva más interesante */}
        <svg className="absolute top-1/3 right-1/3 pointer-events-none" style={{ opacity: 0.18, animation: 'lp2-float-slow 15s 1s ease-in-out infinite' }} width="58" height="58" viewBox="0 0 58 58" fill="none">
          <polygon points="29,2 54,16 54,42 29,56 4,42 4,16" fill="rgba(167,139,250,0.20)" stroke="rgba(221,214,254,0.65)" strokeWidth="1.5"/>
        </svg>

        {/* Puntos de luz — más brillantes */}
        <div className="absolute top-24 right-24 pointer-events-none" style={{ width: 16, height: 16, borderRadius: '50%', background: 'rgba(221,214,254,0.85)', boxShadow: '0 0 28px 10px rgba(167,139,250,0.35)', animation: 'lp2-pulse-ring 2.6s ease-out infinite' }}/>
        <div className="absolute top-1/2 left-20 pointer-events-none"  style={{ width: 10, height: 10, borderRadius: '50%', background: 'rgba(196,181,253,0.7)',  boxShadow: '0 0 18px 6px rgba(167,139,250,0.28)', animation: 'lp2-pulse-ring 3.2s 1.1s ease-out infinite' }}/>
        <div className="absolute bottom-32 right-1/3 pointer-events-none" style={{ width: 7, height: 7, borderRadius: '50%', background: 'rgba(196,181,253,0.6)', boxShadow: '0 0 12px 4px rgba(167,139,250,0.22)', animation: 'lp2-pulse-ring 4s 0.7s ease-out infinite' }}/>

        {/* Estrellas / destellos pequeños */}
        {[
          { top: '18%', left: '38%', size: 4, delay: '0s',   dur: '2.8s' },
          { top: '72%', left: '55%', size: 3, delay: '1.2s', dur: '3.4s' },
          { top: '40%', left: '82%', size: 5, delay: '0.6s', dur: '2.2s' },
          { top: '85%', left: '15%', size: 3, delay: '1.8s', dur: '3.8s' },
          { top: '12%', left: '65%', size: 4, delay: '0.3s', dur: '2.5s' },
          { top: '58%', left: '8%',  size: 3, delay: '2.1s', dur: '3.1s' },
        ].map(({ top, left, size, delay, dur }, i) => (
          <div
            key={i}
            className="absolute pointer-events-none"
            style={{
              top, left,
              width: size, height: size,
              borderRadius: '50%',
              background: 'rgba(255,255,255,0.95)',
              boxShadow: `0 0 ${size * 3}px ${size}px rgba(255,255,255,0.4)`,
              animation: `lp2-star-twinkle ${dur} ${delay} ease-in-out infinite`,
            }}
          />
        ))}

        {/* Blob morfante — esquina inferior derecha */}
        <div
          className="lp2-morph-blob"
          style={{
            width: 280, height: 280,
            bottom: '-60px', right: '-60px',
            background: 'radial-gradient(circle, rgba(167,139,250,0.18), rgba(124,58,237,0.06) 70%)',
          }}
        />

        {/* Líneas de cuadrícula diagonal — fondo sutil */}
        <svg className="absolute inset-0 w-full h-full pointer-events-none" style={{ opacity: 0.04 }} preserveAspectRatio="none">
          <defs>
            <pattern id="lp-grid" x="0" y="0" width="60" height="60" patternUnits="userSpaceOnUse" patternTransform="rotate(15)">
              <line x1="0" y1="0" x2="0" y2="60" stroke="white" strokeWidth="0.8"/>
              <line x1="0" y1="0" x2="60" y2="0" stroke="white" strokeWidth="0.8"/>
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#lp-grid)"/>
        </svg>

        <div className="relative max-w-6xl mx-auto px-4 py-20 md:py-28">
          <div className="grid md:grid-cols-2 gap-14 items-center">

            {/* Texto */}
            <div className="space-y-7 z-10 relative">
              {/* Eyebrow */}
              <div>
                <span className="lp2-eyebrow">
                  <span className="lp2-eyebrow-dot" />
                  {userData ? `Bienvenido de nuevo, ${firstName}` : 'Gestión de eventos · Simple · Potente'}
                </span>
              </div>
              <h1 className="lp2-hero-title">
                {userData ? (
                  <>Tu próximo<br /><span className="lp2-hero-title-accent">evento</span><br />te espera.</>
                ) : (
                  <>Diseña eventos<br /><span className="lp2-hero-title-accent">que la gente</span><br />recuerda.</>
                )}
              </h1>
              <p className="lp2-hero-subtitle">
                {userData
                  ? 'Explora los eventos de tu ciudad, organiza el tuyo propio o gestiona tus entradas. Todo en un solo lugar.'
                  : 'Diseña, planifica y vive experiencias únicas que marquen la diferencia. Cada detalle cuenta y nuestro kit de asistencia lo hace realidad.'}
              </p>

              <div className="flex flex-wrap gap-4">
                {userData ? (
                  <>
                    <button className="lp2-btn-primary lp2-btn-primary-glow" onClick={() => navigate('/events')}>
                      Ver eventos <ArrowRight className="w-4 h-4" />
                    </button>
                    {userData.role === 'ORGANIZER' ? (
                      <button className="lp2-btn-secondary" onClick={() => navigate('/events/create')}>
                        Crear evento <ChevronRight className="w-4 h-4" />
                      </button>
                    ) : userData.role !== 'ADMIN' && (
                      <button className="lp2-btn-secondary" onClick={() => navigate('/organizer-petition-form')}>
                        Ser organizador <ChevronRight className="w-4 h-4" />
                      </button>
                    )}
                  </>
                ) : (
                  <>
                    <button className="lp2-btn-primary lp2-btn-primary-glow" onClick={() => navigate('/login')}>
                      Empezar gratis <ArrowRight className="w-4 h-4" />
                    </button>
                    <button className="lp2-btn-secondary" onClick={() => navigate('/events')}>
                      Ver eventos <ChevronRight className="w-4 h-4" />
                    </button>
                  </>
                )}
              </div>

              {/* Trust bar — solo si no hay sesión */}
              {!userData && (
                <div className="space-y-4 pt-1">
                  <div className="flex flex-wrap items-center gap-5">
                    {[
                      { Icon: Ticket,     label: 'Gratis para empezar' },
                      { Icon: CreditCard, label: 'Sin tarjeta de crédito' },
                      { Icon: Headphones, label: 'Soporte en español' },
                    ].map(({ Icon, label }) => (
                      <span key={label} className="flex items-center gap-2 text-xs" style={{ color: 'rgba(255,255,255,0.65)' }}>
                        <Icon className="w-3.5 h-3.5" style={{ color: 'rgba(255,255,255,0.5)' }} />
                        {label}
                      </span>
                    ))}
                  </div>
                  {/* Mini stats strip */}
                  <div className="flex flex-wrap gap-3 pt-1">
                    {[
                      { val: '2D',   label: 'Editor visual' },
                      { val: '100%', label: 'Seguro' },
                      { val: '24/7', label: 'Disponible' },
                    ].map(({ val, label }) => (
                      <div
                        key={val}
                        className="flex items-center gap-2 px-4 py-2 rounded-xl"
                        style={{
                          background: 'rgba(255,255,255,0.22)',
                          border: '1px solid rgba(255,255,255,0.45)',
                          backdropFilter: 'blur(12px)',
                        }}
                      >
                        <span style={{ color: '#fff', fontWeight: 900, fontSize: '0.95rem', lineHeight: 1 }}>{val}</span>
                        <span style={{ color: 'rgba(255,255,255,0.9)', fontSize: '0.78rem', fontWeight: 500 }}>{label}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Tarjeta visual hero */}
            <div className="relative z-10 flex justify-center md:justify-end">
              <div className="w-full max-w-sm">
                <div className="lp2-hero-card">
                  <div className="rounded-xl overflow-hidden mb-4" style={{ height: '200px' }}>
                    <img src="/src/assets/images/ladingpage.png" alt="TuEvento" className="w-full h-full object-cover" />
                  </div>
                  <div className="space-y-3">
                    <div className="lp2-stat-pill">
                      <div className="lp2-stat-pill-icon">
                        <Map className="w-5 h-5 text-white opacity-80" />
                      </div>
                      <div>
                        <div className="lp2-stat-pill-val">Editor de planos</div>
                        <div className="lp2-stat-pill-label">Diseña tu espacio en tiempo real</div>
                      </div>
                    </div>
                    <div className="lp2-stat-pill">
                      <div className="lp2-stat-pill-icon">
                        <Ticket className="w-5 h-5 text-white opacity-80" />
                      </div>
                      <div>
                        <div className="lp2-stat-pill-val">Venta de entradas</div>
                        <div className="lp2-stat-pill-label">Pagos seguros y sin complicaciones</div>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Badge flotante */}
                <div
                  className="absolute -bottom-4 -left-6 flex items-center gap-3 px-4 py-2.5 rounded-2xl shadow-xl"
                  style={{
                    background: 'rgba(255,255,255,0.16)',
                    backdropFilter: 'blur(14px)',
                    border: '1px solid rgba(255,255,255,0.25)',
                    animation: 'lp2-float 5s 1s ease-in-out infinite',
                  }}
                >
                  <div className="w-8 h-8 rounded-full flex items-center justify-center shrink-0" style={{ background: 'rgba(255,255,255,0.28)' }}>
                    <Ticket className="w-4 h-4 text-white" />
                  </div>
                  <div>
                    <p style={{ color: '#fff', fontSize: '0.75rem', fontWeight: 700 }}>Entrada confirmada</p>
                    <p style={{ color: 'rgba(255,255,255,0.60)', fontSize: '0.65rem' }}>VIP · Fila A</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* ── Olas animadas (montaña que sube y baja) ──────────────────── */}
        <div className="lp2-wave-wrap">
          {/* Ola trasera — más lenta */}
          <svg className="lp2-wave-svg lp2-wave-svg-2" viewBox="0 0 2880 110" preserveAspectRatio="none">
            <path
              d="M0,55 C240,95 480,20 720,55 C960,90 1200,15 1440,55 C1680,95 1920,20 2160,55 C2400,90 2640,15 2880,55 L2880,110 L0,110 Z"
              fill="color-mix(in srgb, var(--color-background) 55%, var(--color-primaryDark))"
            />
          </svg>
          {/* Ola delantera — más rápida y sólida */}
          <svg className="lp2-wave-svg lp2-wave-svg-1" viewBox="0 0 2880 110" preserveAspectRatio="none">
            <path
              d="M0,65 C180,20 360,95 540,55 C720,15 900,90 1080,55 C1260,20 1440,95 1620,55 C1800,15 1980,90 2160,55 C2340,20 2520,90 2700,60 C2790,45 2840,70 2880,65 L2880,110 L0,110 Z"
              fill="var(--color-background)"
            />
          </svg>
        </div>
      </section>

      {/* ══ PROPUESTA DE VALOR ════════════════════════════════════════════ */}
      <section className="py-24 relative overflow-hidden">
        {/* Forma geométrica de fondo */}
        <svg className="absolute -bottom-16 -left-16 opacity-5 pointer-events-none" width="360" height="360" viewBox="0 0 360 360" fill="none">
          <circle cx="180" cy="180" r="170" stroke="var(--color-primary)" strokeWidth="2"/>
          <circle cx="180" cy="180" r="120" stroke="var(--color-primary)" strokeWidth="1.5"/>
          <circle cx="180" cy="180" r="70"  stroke="var(--color-primary)" strokeWidth="1"/>
        </svg>

        <div className="max-w-6xl mx-auto px-4 space-y-28">

          {/* Bloque 1 */}
          <div className="grid md:grid-cols-2 gap-14 items-center">
            <div className="space-y-6">
              <p className="text-sm font-semibold tracking-widest uppercase" style={{ color: 'var(--color-accent)' }}>
                Todo en una plataforma
              </p>
              <h2 className="text-3xl md:text-4xl font-bold text-textPrimary leading-tight">
                Organiza, diseña y vive<br />
                <span style={{ background: 'linear-gradient(90deg, var(--color-accent), var(--color-primary))', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text' }}>
                  eventos sin límites.
                </span>
              </h2>
              <p className="text-textSecondary text-lg leading-relaxed">
                Construye eventos memorables con nuestro creador de planos interactivos en línea.
                Diseña espacios personalizados, gestiona asistentes y visualiza cada detalle de tu
                evento en tiempo real, todo en una sola plataforma.
              </p>
              <ul className="space-y-4">
                {[
                  { Icon: Map,       text: 'Editor de planos drag & drop en tiempo real' },
                  { Icon: BarChart2, text: 'Panel de analíticas para cada evento' },
                  { Icon: Bell,      text: 'Notificaciones automáticas a tus asistentes' },
                ].map(({ Icon, text }) => (
                  <li key={text} className="flex items-center gap-3 text-sm text-textSecondary">
                    <span
                      className="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
                      style={{
                        background: 'color-mix(in srgb, var(--color-primary) 12%, var(--color-surface))',
                        border: '1px solid color-mix(in srgb, var(--color-primary) 25%, transparent)',
                      }}
                    >
                      <Icon className="w-4 h-4" style={{ color: 'var(--color-accent)' }} />
                    </span>
                    {text}
                  </li>
                ))}
              </ul>
            </div>
            <div className="rounded-2xl shadow-xl overflow-hidden" style={{ background: 'linear-gradient(135deg, var(--color-surface), var(--color-background))', padding: '1.25rem' }}>
              <div className="rounded-xl overflow-hidden" style={{ height: '280px' }}>
                <img src="/src/assets/images/ladingpage1.png" alt="Organiza tu evento" className="w-full h-full object-cover" />
              </div>
              <div className="flex flex-wrap gap-2 mt-4">
                {['Plano interactivo', 'Venta de tickets', 'Analíticas'].map(tag => (
                  <span key={tag} className="text-xs px-3 py-1 rounded-full font-medium"
                    style={{ background: 'color-mix(in srgb, var(--color-primary) 12%, transparent)', color: 'var(--color-accent)', border: '1px solid color-mix(in srgb, var(--color-primary) 28%, transparent)' }}>
                    {tag}
                  </span>
                ))}
              </div>
            </div>
          </div>

          {/* Separador */}
          <div className="flex items-center gap-6">
            <div className="flex-1 h-px" style={{ background: 'linear-gradient(to right, transparent, var(--color-primary))' }} />
            <div className="w-2 h-2 rounded-full bg-primary opacity-60" />
            <div className="w-3 h-3 rounded-full bg-accent opacity-80" />
            <div className="w-2 h-2 rounded-full bg-primary opacity-60" />
            <div className="flex-1 h-px" style={{ background: 'linear-gradient(to left, transparent, var(--color-primary))' }} />
          </div>

          {/* Bloque 2 */}
          <div className="grid md:grid-cols-2 gap-14 items-center">
            <div className="rounded-2xl shadow-xl overflow-hidden order-2 md:order-1" style={{ background: 'linear-gradient(135deg, var(--color-surface), var(--color-background))', padding: '1.25rem' }}>
              <div className="rounded-xl overflow-hidden aspect-video">
                <img src="/src/assets/images/ladingpage2.png" alt="Eventos disponibles" className="w-full h-full object-cover" />
              </div>
              <div className="flex flex-wrap gap-2 mt-4">
                {['Búsqueda avanzada', 'Por ubicación', 'Por categoría'].map(tag => (
                  <span key={tag} className="text-xs px-3 py-1 rounded-full font-medium"
                    style={{ background: 'color-mix(in srgb, var(--color-accent) 10%, transparent)', color: 'var(--color-accent)', border: '1px solid color-mix(in srgb, var(--color-accent) 28%, transparent)' }}>
                    {tag}
                  </span>
                ))}
              </div>
            </div>
            <div className="space-y-6 order-1 md:order-2">
              <p className="text-sm font-semibold tracking-widest uppercase" style={{ color: 'var(--color-accent)' }}>
                Descubre experiencias
              </p>
              <h2 className="text-3xl md:text-4xl font-bold text-textPrimary leading-tight">
                Solicitud de<br />
                <span style={{ background: 'linear-gradient(90deg, var(--color-accent), var(--color-primary))', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text' }}>
                  Eventos excepcionales.
                </span>
              </h2>
              <p className="text-textSecondary text-lg leading-relaxed">
                Si buscas nuevas experiencias, revisa nuestra lista de eventos disponibles.
                Explora todas las opciones y encuentra el plan perfecto para ti.
              </p>
              <ul className="space-y-4">
                {[
                  { Icon: Navigation, text: 'Filtra por ciudad, categoría y fecha' },
                  { Icon: Ticket,     text: 'Compra entradas de forma rápida y segura' },
                  { Icon: Bell,       text: 'Recibe alertas de eventos que te interesan' },
                ].map(({ Icon, text }) => (
                  <li key={text} className="flex items-center gap-3 text-sm text-textSecondary">
                    <span className="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
                      style={{ background: 'color-mix(in srgb, var(--color-accent) 10%, var(--color-surface))', border: '1px solid color-mix(in srgb, var(--color-accent) 22%, transparent)' }}>
                      <Icon className="w-4 h-4" style={{ color: 'var(--color-accent)' }} />
                    </span>
                    {text}
                  </li>
                ))}
              </ul>
              <button onClick={() => navigate('/events')} className="lp2-btn-primary"
                style={{ background: 'linear-gradient(135deg, var(--color-accent), var(--color-primary))', color: '#fff' }}>
                Ver eventos disponibles <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* ══ CÓMO FUNCIONA ════════════════════════════════════════════════ */}
      <section className="py-20 bg-surface relative overflow-hidden">
        {/* Forma de fondo */}
        <svg className="absolute top-0 right-0 opacity-5 pointer-events-none" width="300" height="300" viewBox="0 0 300 300" fill="none">
          <rect x="20" y="20" width="260" height="260" rx="12" stroke="var(--color-primary)" strokeWidth="1.5"/>
          <rect x="60" y="60" width="180" height="180" rx="8"  stroke="var(--color-primary)" strokeWidth="1"/>
          <rect x="100" y="100" width="100" height="100" rx="6" stroke="var(--color-primary)" strokeWidth="0.8"/>
        </svg>

        <div className="max-w-6xl mx-auto px-4">
          <div className="text-center mb-16">
            <p className="text-sm font-semibold tracking-widest uppercase mb-2" style={{ color: 'var(--color-accent)' }}>
              Simple por diseño
            </p>
            <h2 className="text-3xl md:text-4xl font-bold text-textPrimary">
              Tu evento listo en<br />cuatro pasos
            </h2>
            <p className="text-textSecondary mt-3 max-w-xl mx-auto">
              De la idea al evento real, sin que necesites saber nada técnico.
            </p>
          </div>

          <div className="grid sm:grid-cols-2 md:grid-cols-4 gap-8">
            {STEPS.map(({ Icon, label, title, desc }) => (
              <div key={title} className="lp2-step-card">
                <div className="lp2-step-number">{label}</div>
                <div className="lp2-step-icon">
                  <Icon className="w-7 h-7" style={{ color: 'var(--color-accent)' }} />
                </div>
                <h3 className="text-base font-bold text-textPrimary mb-2">{title}</h3>
                <p className="text-sm text-textSecondary leading-relaxed">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ══ FEATURES ═════════════════════════════════════════════════════ */}
      <section className="py-20">
        <div className="max-w-6xl mx-auto px-4">
          <div className="text-center mb-14">
            <p className="text-sm font-semibold tracking-widest uppercase mb-2" style={{ color: 'var(--color-accent)' }}>
              Herramientas incluidas
            </p>
            <h2 className="text-3xl md:text-4xl font-bold text-textPrimary">
              Todo lo que necesitas,<br />
              <span style={{ background: 'linear-gradient(90deg, var(--color-accent), var(--color-primary))', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text' }}>
                listo desde el primer día
              </span>
            </h2>
            <p className="text-textSecondary mt-3 max-w-lg mx-auto">
              Sin plugins extra, sin integraciones complicadas. Cada herramienta está pensada para que te concentres en tu evento, no en la tecnología.
            </p>
          </div>

          <div className="grid sm:grid-cols-2 md:grid-cols-3 gap-6">
            {FEATURES.map(({ Icon, title, desc }) => (
              <div key={title} className="lp2-feature-card group">
                <div
                  className="lp2-feature-icon"
                  style={{
                    background: 'color-mix(in srgb, var(--color-primary) 10%, var(--color-surface))',
                    border: '1px solid color-mix(in srgb, var(--color-primary) 22%, transparent)',
                  }}
                >
                  <Icon className="w-6 h-6" style={{ color: 'var(--color-accent)' }} />
                </div>
                <h3 className="font-bold text-textPrimary mb-2">{title}</h3>
                <p className="text-sm text-textSecondary leading-relaxed">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ══ RESEÑAS REALES (condicional) ═════════════════════════════════ */}
      {!reviewsLoading && topReviews.length > 0 && (
        <section className="py-20 bg-surface">
          <div className="max-w-6xl mx-auto px-4">
            <div className="text-center mb-14">
              <p className="text-sm font-semibold tracking-widest uppercase mb-2" style={{ color: 'var(--color-accent)' }}>
                Usuarios reales
              </p>
              <h2 className="text-3xl md:text-4xl font-bold text-textPrimary">
                Lo que dicen sobre<br />los eventos en TuEvento
              </h2>
              <p className="text-textSecondary mt-2 text-sm">
                Reseñas verificadas de asistentes en eventos publicados.
              </p>
            </div>

            <div className={`grid gap-6 ${topReviews.length === 1 ? 'md:grid-cols-1 max-w-md mx-auto' : topReviews.length === 2 ? 'md:grid-cols-2 max-w-2xl mx-auto' : 'md:grid-cols-3'}`}>
              {topReviews.map(review => (
                <div key={review.ratingId} className="lp2-testimonial-card">
                  <div className="lp2-testimonial-quote">"</div>
                  <div className="lp2-stars mb-3">
                    {Array.from({ length: 5 }).map((_, i) => (
                      <Star key={i} className={`w-4 h-4 ${i < (review.rating ?? 0) ? 'fill-yellow-400 text-yellow-400' : 'text-surfaceAlt'}`} />
                    ))}
                  </div>
                  <p className="text-textSecondary text-sm leading-relaxed mb-4 relative z-10">
                    {review.comment.length > 180 ? review.comment.slice(0, 177) + '...' : review.comment}
                  </p>
                  <p className="text-xs mb-4 font-semibold" style={{ color: 'var(--color-accent)' }}>
                    {review.eventName}
                  </p>
                  <div className="flex items-center gap-3">
                    <div className="lp2-avatar">{(review.authorName ?? 'U').charAt(0).toUpperCase()}</div>
                    <div>
                      <p className="font-bold text-textPrimary text-sm">{review.authorName ?? 'Usuario'}</p>
                      {review.isOrganizer && (
                        <span className="text-xs px-2 py-0.5 rounded-full font-semibold"
                          style={{ background: 'color-mix(in srgb, var(--color-primary) 15%, transparent)', color: 'var(--color-accent)' }}>
                          Organizador
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>

            <div className="text-center mt-10">
              <button onClick={() => navigate('/events')}
                className="inline-flex items-center gap-2 text-sm font-semibold"
                style={{ color: 'var(--color-accent)' }}>
                <MessageCircle className="w-4 h-4" />
                Ver todos los eventos y sus reseñas
              </button>
            </div>
          </div>
        </section>
      )}

      {/* ══ PLATAFORMAS ══════════════════════════════════════════════════ */}
      <section className="py-20">
        <div className="max-w-3xl mx-auto px-4">
          <div className="text-center mb-14">
            <p className="text-sm font-semibold tracking-widest uppercase mb-2" style={{ color: 'var(--color-accent)' }}>
              Disponible en
            </p>
            <h2 className="text-3xl md:text-4xl font-bold text-textPrimary">
              Donde estés,<br />TuEvento va contigo
            </h2>
            <p className="text-textSecondary mt-3">
              Accede desde el navegador o descarga la app Android. La misma experiencia en cualquier dispositivo.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div className="lp2-platform-card">
              <div className="lp2-platform-icon-wrap">
                <Globe className="w-10 h-10" style={{ color: 'var(--color-accent)' }} />
              </div>
              <h3 className="text-xl font-bold text-textPrimary mb-2">Versión Web</h3>
              <p className="text-textSecondary text-sm mb-5">
                Funciona en cualquier navegador moderno. Sin instalar nada, solo abre la página y listo.
              </p>
              <button onClick={() => navigate('/events')} className="w-full py-2.5 rounded-xl font-semibold text-sm transition-all"
                style={{ background: 'color-mix(in srgb, var(--color-primary) 12%, transparent)', color: 'var(--color-accent)', border: '1px solid color-mix(in srgb, var(--color-primary) 30%, transparent)' }}>
                Abrir app web
              </button>
            </div>

            <div className="lp2-platform-card relative overflow-hidden">
              <div className="absolute top-4 right-4 text-xs font-bold px-2.5 py-0.5 rounded-full"
                style={{ background: 'linear-gradient(135deg, var(--color-accent), var(--color-primary))', color: '#fff' }}>
                Popular
              </div>
              <div className="lp2-platform-icon-wrap">
                <Smartphone className="w-10 h-10" style={{ color: 'var(--color-accent)' }} />
              </div>
              <h3 className="text-xl font-bold text-textPrimary mb-2">Android App</h3>
              <p className="text-textSecondary text-sm mb-5">
                Descarga la APK y accede a notificaciones push, scanner de tickets y funciones nativas.
              </p>
              <button onClick={() => setShowApkModal(true)}
                className="w-full py-2.5 rounded-xl font-semibold text-sm flex items-center justify-center gap-2"
                style={{ background: 'linear-gradient(135deg, var(--color-accent), var(--color-primary))', color: '#fff' }}>
                <Download className="w-4 h-4" /> Descargar APK
              </button>
            </div>
          </div>
        </div>
      </section>

      <Footer />

      {/* ── Modal APK ──────────────────────────────────────────────────── */}
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
              window.open('https://drive.google.com/file/d/1VeECC-bj0F9i-5uczP3Si0FP5c6N8LE8/view?usp=drivesdk', '_blank', 'noopener,noreferrer');
              setShowApkModal(false);
            },
          },
          { label: 'Cancelar', variant: 'secondary', onClick: () => setShowApkModal(false) },
        ]}
      >
        <p className="bm-hint text-center text-sm leading-relaxed">
          Descarga la aplicación móvil para acceder a todas las funciones de TuEvento
          desde tu dispositivo Android. Notificaciones push, scanner de tickets y más.
        </p>
      </BaseModal>
    </div>
  );
}