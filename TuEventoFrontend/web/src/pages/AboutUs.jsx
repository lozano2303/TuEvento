import { Calendar, Users, Gift, Smartphone, Globe, CheckCircle, Heart, Target, Award, Sparkles, Zap, Clock, ChevronLeft, ChevronRight } from 'lucide-react';
import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import Footer from '../layouts/Footer';


export default function AboutUs() {
  const { t } = useTranslation();
  const [currentMember, setCurrentMember] = useState(0);

  const teamMembers = [
    {
      id: 1,
      name: t('about.team.members.cristofer.name'),
      email: "cristoferlozano233@gmail.com",
      phone: "+57 313 460  5214",
      initials: "CG",
      role: t('about.team.members.cristofer.role'),
      description: t('about.team.members.cristofer.description'),
      gradient: "from-green-500 to-emerald-500"
    },
    {
      id: 2,
      name: t('about.team.members.jhampier.name'),
      email: "ortizjhampier@gmail.com",
      phone: "+57 302 770 0760",
      initials: "DP",
      role: t('about.team.members.jhampier.role'),
      description: t('about.team.members.jhampier.description'),
      gradient: "from-pink-500 to-rose-500"
    }
  ];

  // Auto-play del carrusel
  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentMember((prev) => (prev + 1) % teamMembers.length);
    }, 4000);
    return () => clearInterval(interval);
  }, [teamMembers.length]);

  const nextMember = () => setCurrentMember((prev) => (prev + 1) % teamMembers.length);
  const prevMember = () => setCurrentMember((prev) => (prev - 1 + teamMembers.length) % teamMembers.length);

  const member = teamMembers[currentMember];

  return (
    <div className="min-h-screen bg-background text-textPrimary">

      {/* Hero Section */}
      <section className="relative overflow-hidden py-0">
        {/* Fondo con color primario del tema */}
        <div
          className="absolute inset-0"
          style={{ background: 'linear-gradient(135deg, var(--color-primary) 0%, var(--color-primaryDark) 100%)' }}
        >
          {/* Blob superior-izquierda */}
          <svg className="absolute -top-32 -left-32 pointer-events-none" style={{ opacity: 0.3, animation: 'lp2-float-slow 10s ease-in-out infinite' }} width="500" height="500" viewBox="0 0 500 500" fill="none">
            <circle cx="250" cy="250" r="240" fill="url(#auGrad1)"/>
            <circle cx="250" cy="250" r="180" stroke="rgba(196,181,253,0.35)" strokeWidth="1.5" strokeDasharray="14 9"/>
            <circle cx="250" cy="250" r="120" stroke="rgba(196,181,253,0.2)" strokeWidth="1" strokeDasharray="7 12"/>
            <defs>
              <radialGradient id="auGrad1" cx="35%" cy="35%" r="65%">
                <stop offset="0%" stopColor="#c4b5fd" stopOpacity="0.4"/>
                <stop offset="60%" stopColor="#7c3aed" stopOpacity="0.1"/>
                <stop offset="100%" stopColor="#7c3aed" stopOpacity="0"/>
              </radialGradient>
            </defs>
          </svg>

          {/* Anillo giratorio top-left */}
          <svg className="absolute top-8 left-12 pointer-events-none" style={{ opacity: 0.28, animation: 'lp2-spin-slow 28s linear infinite' }} width="160" height="160" viewBox="0 0 160 160" fill="none">
            <circle cx="80" cy="80" r="74" stroke="rgba(221,214,254,0.85)" strokeWidth="2" strokeDasharray="12 7"/>
            <circle cx="80" cy="80" r="50" stroke="rgba(221,214,254,0.45)" strokeWidth="1" strokeDasharray="5 10"/>
          </svg>

          {/* Rombo derecha */}
          <svg className="absolute top-12 right-16 pointer-events-none" style={{ opacity: 0.28, animation: 'lp2-float 7s 1s ease-in-out infinite' }} width="140" height="140" viewBox="0 0 140 140" fill="none">
            <polygon points="70,5 135,70 70,135 5,70" fill="url(#auGrad2)" stroke="rgba(221,214,254,0.65)" strokeWidth="1.8"/>
            <polygon points="70,28 112,70 70,112 28,70" fill="none" stroke="rgba(221,214,254,0.3)" strokeWidth="1" strokeDasharray="5 6"/>
            <defs>
              <linearGradient id="auGrad2" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stopColor="#a78bfa" stopOpacity="0.32"/>
                <stop offset="100%" stopColor="#7c3aed" stopOpacity="0.04"/>
              </linearGradient>
            </defs>
          </svg>

          {/* Triángulo bottom-derecha */}
          <svg className="absolute bottom-24 right-24 pointer-events-none" style={{ opacity: 0.22, animation: 'lp2-float-slow 11s 2s ease-in-out infinite' }} width="90" height="90" viewBox="0 0 90 90" fill="none">
            <polygon points="45,5 85,80 5,80" fill="url(#auGrad3)" stroke="rgba(221,214,254,0.55)" strokeWidth="1.8"/>
            <defs>
              <linearGradient id="auGrad3" x1="0.5" y1="0" x2="0.5" y2="1">
                <stop offset="0%" stopColor="#c4b5fd" stopOpacity="0.35"/>
                <stop offset="100%" stopColor="#7c3aed" stopOpacity="0.04"/>
              </linearGradient>
            </defs>
          </svg>

          {/* Cuadrado rotado bottom-left */}
          <svg className="absolute bottom-28 left-1/4 pointer-events-none" style={{ opacity: 0.2, transform: 'rotate(18deg)', animation: 'lp2-float 9s 0.5s ease-in-out infinite' }} width="64" height="64" viewBox="0 0 64 64" fill="none">
            <rect x="4" y="4" width="56" height="56" rx="10" fill="rgba(167,139,250,0.18)" stroke="rgba(221,214,254,0.6)" strokeWidth="1.8" strokeDasharray="6 5"/>
          </svg>

          {/* Hexágono centro */}
          <svg className="absolute top-1/3 left-1/2 pointer-events-none" style={{ opacity: 0.15, animation: 'lp2-float-slow 13s 1s ease-in-out infinite' }} width="52" height="52" viewBox="0 0 52 52" fill="none">
            <polygon points="26,2 50,14 50,38 26,50 2,38 2,14" fill="rgba(167,139,250,0.2)" stroke="rgba(221,214,254,0.6)" strokeWidth="1.5"/>
          </svg>

          {/* Cruz decorativa */}
          <svg className="absolute top-12 left-1/2 pointer-events-none" style={{ opacity: 0.22, animation: 'lp2-float-slow 14s 3s ease-in-out infinite' }} width="28" height="28" viewBox="0 0 28 28" fill="none">
            <line x1="14" y1="2" x2="14" y2="26" stroke="rgba(221,214,254,0.9)" strokeWidth="2.5" strokeLinecap="round"/>
            <line x1="2" y1="14" x2="26" y2="14" stroke="rgba(221,214,254,0.9)" strokeWidth="2.5" strokeLinecap="round"/>
          </svg>

          {/* Puntos de luz */}
          <div className="absolute top-16 right-20 pointer-events-none" style={{ width: 14, height: 14, borderRadius: '50%', background: 'rgba(221,214,254,0.85)', boxShadow: '0 0 26px 10px rgba(167,139,250,0.32)', animation: 'lp2-pulse-ring 2.6s ease-out infinite' }}/>
          <div className="absolute top-1/2 left-16 pointer-events-none" style={{ width: 9, height: 9, borderRadius: '50%', background: 'rgba(196,181,253,0.7)', boxShadow: '0 0 16px 6px rgba(167,139,250,0.25)', animation: 'lp2-pulse-ring 3.2s 1.1s ease-out infinite' }}/>
          <div className="absolute bottom-20 right-1/3 pointer-events-none" style={{ width: 6, height: 6, borderRadius: '50%', background: 'rgba(196,181,253,0.6)', boxShadow: '0 0 12px 4px rgba(167,139,250,0.2)', animation: 'lp2-pulse-ring 4s 0.7s ease-out infinite' }}/>

          {/* Estrellas / destellos */}
          {[
            { top: '15%', left: '42%', size: 4, delay: '0s',   dur: '2.8s' },
            { top: '68%', left: '58%', size: 3, delay: '1.2s', dur: '3.4s' },
            { top: '38%', left: '78%', size: 5, delay: '0.6s', dur: '2.2s' },
            { top: '80%', left: '18%', size: 3, delay: '1.8s', dur: '3.8s' },
            { top: '10%', left: '62%', size: 4, delay: '0.3s', dur: '2.5s' },
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

          {/* Blob morfante */}
          <div
            className="lp2-morph-blob"
            style={{
              width: 320, height: 320,
              bottom: '-80px', right: '-80px',
              background: 'radial-gradient(circle, rgba(167,139,250,0.20), rgba(124,58,237,0.05) 70%)',
            }}
          />

          {/* Cuadrícula diagonal sutil */}
          <svg className="absolute inset-0 w-full h-full pointer-events-none" style={{ opacity: 0.035 }} preserveAspectRatio="none">
            <defs>
              <pattern id="au-grid" x="0" y="0" width="60" height="60" patternUnits="userSpaceOnUse" patternTransform="rotate(15)">
                <line x1="0" y1="0" x2="0" y2="60" stroke="white" strokeWidth="0.8"/>
                <line x1="0" y1="0" x2="60" y2="0" stroke="white" strokeWidth="0.8"/>
              </pattern>
            </defs>
            <rect width="100%" height="100%" fill="url(#au-grid)"/>
          </svg>

          <div
            className="absolute inset-0"
            style={{ background: 'linear-gradient(135deg, color-mix(in srgb, var(--color-primary) 80%, transparent), color-mix(in srgb, var(--color-primaryDark) 60%, transparent), color-mix(in srgb, var(--color-background) 40%, transparent))' }}
          />
        </div>

        <div className="relative max-w-6xl mx-auto px-4 py-24 md:py-32">
          <div className="text-center space-y-7">
            <div
              className="inline-flex items-center px-5 py-2 rounded-full border mb-2"
              style={{
                background: 'rgba(255,255,255,0.13)',
                backdropFilter: 'blur(10px)',
                borderColor: 'rgba(255,255,255,0.25)',
                animation: 'lp2-slide-up 0.5s ease both',
              }}
            >
              <Sparkles className="w-4 h-4 mr-2" style={{ color: '#c4b5fd' }} />
              <span className="text-sm font-semibold" style={{ color: 'rgba(255,255,255,0.92)' }}>{t('about.hero.badge')}</span>
              <span
                className="ml-2 w-2 h-2 rounded-full inline-block"
                style={{ background: '#4ade80', boxShadow: '0 0 6px #4ade80', animation: 'lp2-pulse-ring 1.4s ease-out infinite' }}
              />
            </div>

            <h1
              className="text-5xl md:text-7xl font-extrabold leading-tight text-white"
              style={{ animation: 'lp2-slide-up 0.7s 0.1s ease both', letterSpacing: '-0.02em' }}
            >
              {t('about.hero.title')}{' '}
              <span style={{
                background: 'linear-gradient(90deg, #e9d5ff, #a78bfa, #c4b5fd)',
                backgroundSize: '200% auto',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                backgroundClip: 'text',
                animation: 'lp2-shimmer 3s linear infinite',
              }}>
                {t('about.hero.titleAccent')}
              </span>
            </h1>

            <p
              className="text-lg md:text-xl max-w-3xl mx-auto leading-relaxed"
              style={{ color: 'rgba(255,255,255,0.78)', animation: 'lp2-slide-up 0.7s 0.2s ease both' }}
            >
              {t('about.hero.description')}
            </p>

            <div
              className="flex flex-wrap justify-center gap-3 pt-2"
              style={{ animation: 'lp2-slide-up 0.7s 0.3s ease both' }}
            >
              {[
                { icon: Zap,    textKey: 'about.hero.features.visualization' },
                { icon: Target, textKey: 'about.hero.features.planning' },
                { icon: Heart,  textKey: 'about.hero.features.experiences' },
              ].map(({ icon: Icon, textKey }) => (
                <div
                  key={textKey}
                  className="flex items-center gap-2 px-5 py-2.5 rounded-2xl transition-all duration-300 cursor-default"
                  style={{
                    background: 'rgba(255,255,255,0.12)',
                    backdropFilter: 'blur(12px)',
                    border: '1px solid rgba(255,255,255,0.22)',
                  }}
                  onMouseEnter={e => {
                    e.currentTarget.style.background = 'rgba(255,255,255,0.22)';
                    e.currentTarget.style.transform = 'translateY(-3px) scale(1.04)';
                    e.currentTarget.style.boxShadow = '0 8px 24px rgba(0,0,0,0.18)';
                  }}
                  onMouseLeave={e => {
                    e.currentTarget.style.background = 'rgba(255,255,255,0.12)';
                    e.currentTarget.style.transform = '';
                    e.currentTarget.style.boxShadow = '';
                  }}
                >
                  <Icon className="w-4 h-4" style={{ color: '#c4b5fd' }} />
                  <span className="text-sm font-semibold text-white">{t(textKey)}</span>
                </div>
              ))}
            </div>

            {/* Mini stats strip */}
            <div
              className="flex flex-wrap justify-center gap-3 pt-3"
              style={{ animation: 'lp2-slide-up 0.7s 0.4s ease both' }}
            >
              {[
                { val: '2D',   labelKey: 'about.hero.stats.editor' },
                { val: '100%', labelKey: 'about.hero.stats.secure' },
                { val: '24/7', labelKey: 'about.hero.stats.available' },
                { val: '2',    labelKey: 'about.hero.stats.platforms' },
              ].map(({ val, labelKey }) => (
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
                  <span style={{ color: 'rgba(255,255,255,0.9)', fontSize: '0.78rem', fontWeight: 500 }}>{t(labelKey)}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Onda decorativa — doble ola animada */}
        <div className="lp2-wave-wrap" style={{ height: 110, marginTop: '-2px' }}>
          {/* Ola trasera */}
          <svg className="lp2-wave-svg lp2-wave-svg-2" viewBox="0 0 2880 110" preserveAspectRatio="none">
            <path
              d="M0,55 C240,95 480,20 720,55 C960,90 1200,15 1440,55 C1680,95 1920,20 2160,55 C2400,90 2640,15 2880,55 L2880,110 L0,110 Z"
              fill="color-mix(in srgb, var(--color-background) 55%, var(--color-primaryDark))"
            />
          </svg>
          {/* Ola delantera */}
          <svg className="lp2-wave-svg lp2-wave-svg-1" viewBox="0 0 2880 110" preserveAspectRatio="none">
            <path
              d="M0,65 C180,20 360,95 540,55 C720,15 900,90 1080,55 C1260,20 1440,95 1620,55 C1800,15 1980,90 2160,55 C2340,20 2520,90 2700,60 C2790,45 2840,70 2880,65 L2880,110 L0,110 Z"
              fill="var(--color-background)"
            />
          </svg>
        </div>
      </section>

      {/* Nuestra Historia */}
      <section className="py-24 bg-background relative overflow-hidden">
        <div className="absolute inset-0 opacity-5 pointer-events-none">
          <div className="absolute top-20 left-10 w-32 h-32 bg-primary rounded-full blur-3xl" />
          <div className="absolute bottom-20 right-10 w-40 h-40 bg-accent rounded-full blur-3xl" />
        </div>

        <div className="max-w-6xl mx-auto px-4 relative z-10">
          <div className="text-center mb-20">
            <div className="inline-flex items-center px-6 py-3 rounded-full border mb-8 bg-primary/20 border-primary/30">
              <Calendar className="w-5 h-5 text-accent mr-2" />
              <span className="text-sm font-medium text-accent">{t('about.story.badge')}</span>
            </div>

            <h2 className="text-4xl md:text-5xl font-bold mb-8 text-textPrimary">
              {t('about.story.title')}
            </h2>
            <p className="text-textSecondary text-xl max-w-4xl mx-auto leading-relaxed">
              {t('about.story.description')}
            </p>
          </div>

          <div className="grid md:grid-cols-2 gap-16 items-center">
            <div className="space-y-8">
              <div
                className="p-8 rounded-2xl hover:border-primary/30 transition-all duration-300"
                style={{ background: 'color-mix(in srgb, var(--color-surface) 50%, transparent)', backdropFilter: 'blur(8px)', border: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 50%, transparent)' }}
              >
                <h3 className="text-3xl font-bold mb-4 text-textPrimary">{t('about.story.vision.title')}</h3>
                <p className="text-textSecondary leading-relaxed mb-6">
                  {t('about.story.vision.description')}
                </p>
                <div className="flex items-center space-x-4">
                  <div className="w-12 h-12 bg-primary/20 rounded-full flex items-center justify-center">
                    <Target className="w-6 h-6 text-accent" />
                  </div>
                  <span className="text-lg font-semibold text-accent">{t('about.story.vision.feature')}</span>
                </div>
              </div>
            </div>

            <div className="space-y-8">
              <div
                className="p-8 rounded-2xl transition-all duration-300"
                style={{ background: 'color-mix(in srgb, var(--color-surface) 50%, transparent)', backdropFilter: 'blur(8px)', border: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 50%, transparent)' }}
              >
                <h3 className="text-3xl font-bold mb-4 text-textPrimary">{t('about.story.mission.title')}</h3>
                <p className="text-textSecondary leading-relaxed mb-6">
                  {t('about.story.mission.description')}
                </p>
                <div className="flex items-center space-x-4">
                  <div className="w-12 h-12 bg-error/20 rounded-full flex items-center justify-center">
                    <Heart className="w-6 h-6 text-error" />
                  </div>
                  <span className="text-lg font-semibold text-error">{t('about.story.mission.feature')}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Nuestros Valores */}
      <section className="py-24 bg-surface relative overflow-hidden">
        <div className="absolute inset-0 opacity-10 pointer-events-none">
          <div className="absolute top-1/4 left-1/4 w-64 h-64 bg-primary rounded-full blur-3xl" />
          <div className="absolute bottom-1/4 right-1/4 w-48 h-48 bg-accent rounded-full blur-3xl" />
        </div>

        <div className="max-w-6xl mx-auto px-4 relative z-10">
          <div className="text-center mb-20">
            <div className="inline-flex items-center px-6 py-3 rounded-full border mb-8 bg-primary/20 border-primary/30">
              <Award className="w-5 h-5 text-accent mr-2" />
              <span className="text-sm font-medium text-accent">{t('about.values.badge')}</span>
            </div>
            <h2 className="text-4xl md:text-5xl font-bold mb-8 text-textPrimary">
              {t('about.values.title')}
            </h2>
          </div>

          <div className="grid md:grid-cols-3 gap-8">
            {[
              { icon: Award,       titleKey: 'about.values.items.excellence.title',   textKey: 'about.values.items.excellence.description',    hoverBorder: 'hover:border-primary/50',  gradient: 'from-primary to-primaryDark' },
              { icon: Users,       titleKey: 'about.values.items.collaboration.title', textKey: 'about.values.items.collaboration.description', hoverBorder: 'hover:border-accent/50',   gradient: 'from-accent to-primary'     },
              { icon: CheckCircle, titleKey: 'about.values.items.integrity.title',   textKey: 'about.values.items.integrity.description',     hoverBorder: 'hover:border-success/50',  gradient: 'from-success to-primaryDark' },
            ].map(({ icon: Icon, titleKey, textKey, hoverBorder, gradient }) => (
              <div
                key={titleKey}
                className={`group text-center space-y-6 p-8 rounded-2xl backdrop-blur-sm transition-all duration-300 hover:scale-105 ${hoverBorder}`}
                style={{ background: 'color-mix(in srgb, var(--color-surfaceAlt) 30%, transparent)', border: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 30%, transparent)' }}
              >
                <div className={`w-20 h-20 bg-gradient-to-br ${gradient} rounded-2xl flex items-center justify-center mx-auto group-hover:scale-110 transition-transform duration-300 shadow-lg`}>
                  <Icon className="w-10 h-10 text-textPrimary" />
                </div>
                <h3 className="text-2xl font-bold text-textPrimary">{t(titleKey)}</h3>
                <p className="text-textSecondary leading-relaxed">{t(textKey)}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Organiza tus eventos */}
      <section
        className="py-20 relative overflow-hidden"
        style={{ background: 'linear-gradient(135deg, var(--color-primaryDark) 0%, var(--color-primary) 100%)' }}
      >
        <div className="absolute inset-0 opacity-20 pointer-events-none">
          <div className="absolute top-10 left-10 w-32 h-32 bg-textPrimary rounded-full blur-3xl" />
          <div className="absolute bottom-10 right-10 w-24 h-24 bg-accent rounded-full blur-2xl" />
        </div>

        <div className="max-w-6xl mx-auto px-4 relative z-10">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            {/* Ilustración — mockup de interfaz */}
            <div className="relative flex items-center justify-center">
              <div
                className="relative w-full max-w-sm rounded-3xl p-6 shadow-2xl"
                style={{
                  background: 'color-mix(in srgb, var(--color-primary) 22%, transparent)',
                  backdropFilter: 'blur(12px)',
                  border: '1px solid rgba(255,255,255,0.18)',
                }}
              >
                {/* Header del mockup */}
                <div className="flex items-center justify-between mb-5">
                  <div className="flex items-center gap-2">
                    <div className="w-2.5 h-2.5 rounded-full bg-red-400" />
                    <div className="w-2.5 h-2.5 rounded-full bg-yellow-400" />
                    <div className="w-2.5 h-2.5 rounded-full bg-green-400" />
                  </div>
                  <div
                    className="text-xs font-semibold px-3 py-1 rounded-full"
                    style={{ background: 'rgba(255,255,255,0.15)', color: 'rgba(255,255,255,0.85)' }}
                  >
                    Editor de planos
                  </div>
                </div>

                {/* Grid de secciones simuladas */}
                <div className="grid grid-cols-3 gap-2 mb-4">
                  {[
                    { label: 'VIP',       w: 'col-span-2', h: 'h-14', opacity: '0.35' },
                    { label: 'General',   w: 'col-span-1', h: 'h-14', opacity: '0.25' },
                    { label: 'Escenario', w: 'col-span-3', h: 'h-10', opacity: '0.20' },
                    { label: 'Mesa A',    w: 'col-span-1', h: 'h-12', opacity: '0.30' },
                    { label: 'Mesa B',    w: 'col-span-1', h: 'h-12', opacity: '0.30' },
                    { label: 'Barra',     w: 'col-span-1', h: 'h-12', opacity: '0.22' },
                  ].map(({ label, w, h, opacity }) => (
                    <div
                      key={label}
                      className={`${w} ${h} rounded-xl flex items-center justify-center`}
                      style={{ background: `rgba(255,255,255,${opacity})`, border: '1px solid rgba(255,255,255,0.2)' }}
                    >
                      <span className="text-xs font-medium" style={{ color: 'rgba(255,255,255,0.8)' }}>{label}</span>
                    </div>
                  ))}
                </div>

                {/* Barra de herramientas simulada */}
                <div
                  className="flex items-center justify-between px-3 py-2.5 rounded-xl"
                  style={{ background: 'rgba(255,255,255,0.12)', border: '1px solid rgba(255,255,255,0.15)' }}
                >
                  {[Calendar, Users, Zap, CheckCircle].map((Icon, i) => (
                    <div
                      key={i}
                      className="w-8 h-8 rounded-lg flex items-center justify-center"
                      style={{ background: i === 0 ? 'rgba(255,255,255,0.25)' : 'transparent' }}
                    >
                      <Icon className="w-4 h-4" style={{ color: i === 0 ? '#fff' : 'rgba(255,255,255,0.5)' }} />
                    </div>
                  ))}
                  <div
                    className="text-xs font-semibold px-3 py-1 rounded-lg"
                    style={{ background: 'var(--color-accent)', color: '#fff' }}
                  >
                    Guardar
                  </div>
                </div>
              </div>

              {/* Badge flotante — asistentes */}
              <div
                className="absolute -bottom-4 -right-4 flex items-center gap-2 px-3 py-2 rounded-2xl shadow-xl"
                style={{
                  background: 'rgba(255,255,255,0.95)',
                  animation: 'lp2-float 5s ease-in-out infinite',
                }}
              >
                <div className="w-7 h-7 rounded-full flex items-center justify-center shrink-0"
                  style={{ background: 'linear-gradient(135deg, var(--color-primary), var(--color-primaryDark))' }}>
                  <Users className="w-3.5 h-3.5 text-white" />
                </div>
                <div>
                  <p className="font-bold text-gray-800" style={{ fontSize: '0.7rem' }}>248 asistentes</p>
                  <p className="text-gray-500" style={{ fontSize: '0.6rem' }}>confirmados</p>
                </div>
              </div>

              {/* Badge flotante — en vivo */}
              <div
                className="absolute -top-3 -left-3 flex items-center gap-2 px-3 py-1.5 rounded-xl shadow-lg"
                style={{
                  background: 'rgba(255,255,255,0.95)',
                  animation: 'lp2-float 6s 1.5s ease-in-out infinite',
                }}
              >
                <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse inline-block" />
                <p className="font-bold text-gray-800" style={{ fontSize: '0.7rem' }}>En vivo</p>
              </div>
            </div>

            {/* Contenido de texto */}
            <div className="space-y-8">
              <div>
                <h2 className="text-4xl md:text-5xl font-bold mb-6 text-textPrimary">
                  {t('about.organize.title')}
                </h2>
                <p className="text-xl leading-relaxed mb-8" style={{ color: 'color-mix(in srgb, var(--color-textPrimary) 85%, transparent)' }}>
                  {t('about.organize.description')}
                </p>
              </div>

              <div className="space-y-4">
                {[
                  { icon: Clock,  titleKey: 'about.organize.features.saveTime.title',      descKey: 'about.organize.features.saveTime.description',      accentBg: 'bg-accent/80',   accentText: 'text-background' },
                  { icon: Zap,    titleKey: 'about.organize.features.virtualTools.title',  descKey: 'about.organize.features.virtualTools.description',  accentBg: 'bg-primary/80',  accentText: 'text-textPrimary' },
                  { icon: Target, titleKey: 'about.organize.features.focus.title',         descKey: 'about.organize.features.focus.description',         accentBg: 'bg-success/80',  accentText: 'text-textPrimary' },
                ].map(({ icon: Icon, titleKey, descKey, accentBg, accentText }) => (
                  <div
                    key={titleKey}
                    className="flex items-center space-x-4 p-4 rounded-lg border"
                    style={{ background: 'rgba(255,255,255,0.1)', backdropFilter: 'blur(8px)', borderColor: 'rgba(255,255,255,0.2)' }}
                  >
                    <div className={`w-10 h-10 ${accentBg} rounded-full flex items-center justify-center flex-shrink-0`}>
                      <Icon className={`w-5 h-5 ${accentText}`} />
                    </div>
                    <div>
                      <h3 className="font-semibold text-textPrimary">{t(titleKey)}</h3>
                      <p className="text-sm" style={{ color: 'color-mix(in srgb, var(--color-textPrimary) 70%, transparent)' }}>{t(descKey)}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Nuestro Equipo */}
      <section className="py-24 bg-background relative overflow-hidden">
        <div className="absolute inset-0 opacity-5 pointer-events-none">
          <div className="absolute top-20 right-20 w-56 h-56 bg-success rounded-full blur-3xl" />
          <div className="absolute bottom-20 left-20 w-40 h-40 bg-accent rounded-full blur-3xl" />
        </div>

        <div className="max-w-6xl mx-auto px-4 relative z-10">
          <div className="text-center mb-16">
            <div className="inline-flex items-center px-6 py-3 rounded-full border mb-8 bg-primary/20 border-primary/30">
              <Users className="w-5 h-5 text-accent mr-2" />
              <span className="text-sm font-medium text-accent">{t('about.team.badge')}</span>
            </div>
            <h2 className="text-4xl md:text-5xl font-bold mb-8 text-textPrimary">
              {t('about.team.title')}
            </h2>
            <p className="text-textSecondary text-xl max-w-3xl mx-auto leading-relaxed">
              {t('about.team.description')}
            </p>
          </div>

          {/* Carrusel del equipo */}
          <div
            className="relative rounded-3xl p-8 md:p-12"
            style={{ background: 'color-mix(in srgb, var(--color-surface) 40%, transparent)', backdropFilter: 'blur(8px)', border: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 30%, transparent)' }}
          >
            <div className="grid md:grid-cols-2 gap-12 items-center min-h-[400px]">
              {/* Foto del miembro */}
              <div className="relative">
                <div className="relative group">
                  <div className={`w-80 h-80 mx-auto bg-gradient-to-r ${member.gradient} rounded-3xl overflow-hidden shadow-2xl group-hover:scale-105 transition-all duration-500`}>
                    {member.id === 1 ? (
                      <img src="/src/assets/images/francisco.jpg" alt={member.name} className="w-full h-full object-cover" />
                    ) : member.id === 2 ? (
                      <img src="/src/assets/images/keiner.png" alt={member.name} className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-textPrimary font-bold text-6xl">
                        {member.initials}
                      </div>
                    )}
                  </div>
                  <div className="absolute -top-4 -left-4 w-8 h-8 bg-accent rounded-full animate-pulse" />
                  <div className="absolute -bottom-4 -right-4 w-6 h-6 bg-primary rounded-full animate-bounce" />
                  <div className="absolute top-8 -right-2 w-4 h-4 bg-success rounded-full animate-ping" />
                </div>
              </div>

              {/* Información del miembro */}
              <div className="space-y-6 text-center md:text-left">
                <div>
                  <h3 className="text-3xl md:text-4xl font-bold text-textPrimary mb-2">
                    {member.name}
                  </h3>
                  <p className="text-xl text-accent font-semibold mb-4">
                    {member.role}
                  </p>
                  <p className="text-textSecondary text-lg leading-relaxed">
                    {member.description}
                  </p>
                </div>

                <div className="space-y-4">
                  {[
                    { color: 'bg-accent', value: member.email },
                    { color: 'bg-success', value: member.phone },
                  ].map(({ color, value }) => (
                    <div
                      key={value}
                      className="flex items-center justify-center md:justify-start space-x-3 p-3 rounded-lg border"
                      style={{ background: 'rgba(255,255,255,0.05)', borderColor: 'rgba(255,255,255,0.1)' }}
                    >
                      <div className={`w-3 h-3 ${color} rounded-full`} />
                      <span className="text-textSecondary">{value}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Controles de navegación */}
            <div className="flex items-center justify-between mt-8">
              <button
                onClick={prevMember}
                className="w-12 h-12 rounded-full flex items-center justify-center transition-all duration-300 hover:scale-110"
                style={{ background: 'rgba(255,255,255,0.1)', backdropFilter: 'blur(8px)', border: '1px solid rgba(255,255,255,0.2)' }}
              >
                <ChevronLeft className="w-6 h-6 text-textPrimary" />
              </button>

              <div className="flex space-x-3">
                {teamMembers.map((_, index) => (
                  <button
                    key={index}
                    onClick={() => setCurrentMember(index)}
                    className="w-3 h-3 rounded-full transition-all duration-300"
                    style={{
                      background: index === currentMember ? 'var(--color-accent)' : 'rgba(255,255,255,0.3)',
                      transform: index === currentMember ? 'scale(1.25)' : 'scale(1)',
                    }}
                  />
                ))}
              </div>

              <button
                onClick={nextMember}
                className="w-12 h-12 rounded-full flex items-center justify-center transition-all duration-300 hover:scale-110"
                style={{ background: 'rgba(255,255,255,0.1)', backdropFilter: 'blur(8px)', border: '1px solid rgba(255,255,255,0.2)' }}
              >
                <ChevronRight className="w-6 h-6 text-textPrimary" />
              </button>
            </div>

            <div className="text-center mt-6">
              <span className="text-textMuted text-sm">
                {t('about.team.counter', { current: currentMember + 1, total: teamMembers.length })}
              </span>
            </div>
          </div>
        </div>
      </section>

      <Footer />
    </div>
  );
}
