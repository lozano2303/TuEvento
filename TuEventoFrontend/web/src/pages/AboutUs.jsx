import { Calendar, Users, Gift, Smartphone, Globe, CheckCircle, Heart, Target, Award, Sparkles, Zap, Clock, ChevronLeft, ChevronRight } from 'lucide-react';
import { useState, useEffect } from 'react';
import Footer from '../layouts/Footer';


export default function AboutUs() {
  const [currentMember, setCurrentMember] = useState(0);

  const teamMembers = [
    {
      id: 1,
      name: "Cristofer David Lozano Contreras",
      email: "cristoferlozano233@gmail.com",
      phone: "+57 313 460  5214",
      initials: "CG",
      role: "Desarollador de Software",
      description: "Encargado de vigilar el seguimiento del equipo, desarrollador del Backend, diseñador de la base de datos, apoyo en la visualización móvil y documentación.",
      gradient: "from-green-500 to-emerald-500"
    },
    {
      id: 2,
      name: "Jhampier Santos Ortiz",
      email: "ortizjhampier@gmail.com",
      phone: "+57 302 770 0760",
      initials: "DP",
      role: "Desarollador de Software",
      description: "Responsable de implementar la lógica del sistema, creación de interfaces gráficas web y móviles, además de la elaboración de la documentación del proyecto.",
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
          <div className="absolute inset-0 opacity-20">
            <div className="absolute top-20 left-20 w-2 h-2 bg-accent rounded-full animate-pulse" />
            <div className="absolute top-40 right-32 w-3 h-3 bg-accent rounded-full animate-bounce" />
            <div className="absolute bottom-32 left-1/4 w-1 h-1 bg-textPrimary rounded-full animate-ping" />
            <div className="absolute top-60 left-3/4 w-2 h-2 bg-accent rounded-full animate-pulse" />
          </div>
          <div
            className="absolute inset-0"
            style={{ background: 'linear-gradient(135deg, color-mix(in srgb, var(--color-primary) 80%, transparent), color-mix(in srgb, var(--color-primaryDark) 60%, transparent), color-mix(in srgb, var(--color-background) 40%, transparent))' }}
          />
        </div>

        <div className="relative max-w-6xl mx-auto px-4 py-17">
          <div className="text-center space-y-8">
            <div
              className="inline-flex items-center px-6 py-2 rounded-full border mb-6"
              style={{ background: 'rgba(255,255,255,0.1)', backdropFilter: 'blur(8px)', borderColor: 'rgba(255,255,255,0.2)' }}
            >
              <Sparkles className="w-5 h-5 text-accent mr-2" />
              <span className="text-sm font-medium text-textPrimary">Plataforma de Eventos Innovadora</span>
            </div>

            <h1 className="text-5xl md:text-7xl font-bold leading-tight text-textPrimary">
              Sobre <span className="text-accent drop-shadow-lg">Nosotros</span>
            </h1>

            <p className="text-xl max-w-4xl mx-auto leading-relaxed" style={{ color: 'color-mix(in srgb, var(--color-textPrimary) 85%, transparent)' }}>
              En <span className="font-semibold text-accent">"Tu Evento"</span> transformamos ideas en experiencias inolvidables. Somos una plataforma especializada en la
              <span className="font-medium text-textPrimary"> maquetación virtual de espacios</span> para eventos, permitiendo a organizadores planificar visualmente la disposición
              de sus lugares: mesas, escenarios, pistas de baile, áreas VIP y más.
            </p>

            <div className="flex flex-wrap justify-center gap-4 mt-8">
              {[
                { icon: Zap,    text: 'Visualización 2D',     color: 'text-accent' },
                { icon: Target, text: 'Planificación Precisa', color: 'text-success' },
                { icon: Heart,  text: 'Experiencias Únicas',  color: 'text-error'  },
              ].map(({ icon: Icon, text, color }) => (
                <div
                  key={text}
                  className="flex items-center px-4 py-2 rounded-lg border"
                  style={{ background: 'rgba(255,255,255,0.1)', backdropFilter: 'blur(8px)', borderColor: 'rgba(255,255,255,0.2)' }}
                >
                  <Icon className={`w-5 h-5 ${color} mr-2`} />
                  <span className="text-sm text-textPrimary">{text}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Onda decorativa */}
        <div className="relative w-full overflow-hidden mt-16">
          <svg viewBox="0 0 1200 120" className="w-full h-32 fill-background" preserveAspectRatio="none">
            <path d="M0,60 Q150,20 300,60 T600,60 Q750,100 900,60 T1200,60 L1200,120 L0,120 Z" />
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
              <span className="text-sm font-medium text-accent">Nuestra Trayectoria</span>
            </div>

            <h2 className="text-4xl md:text-5xl font-bold mb-8 text-textPrimary">
              Nuestra Historia
            </h2>
            <p className="text-textSecondary text-xl max-w-4xl mx-auto leading-relaxed">
              Todo comenzó con una simple idea: hacer que la organización de eventos sea más fácil,
              eficiente y accesible para todos. Desde nuestros inicios, hemos trabajado incansablemente
              para crear una plataforma que combine tecnología innovadora con una experiencia de usuario excepcional.
            </p>
          </div>

          <div className="grid md:grid-cols-2 gap-16 items-center">
            <div className="space-y-8">
              <div
                className="p-8 rounded-2xl hover:border-primary/30 transition-all duration-300"
                style={{ background: 'color-mix(in srgb, var(--color-surface) 50%, transparent)', backdropFilter: 'blur(8px)', border: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 50%, transparent)' }}
              >
                <h3 className="text-3xl font-bold mb-4 text-textPrimary">Nuestra Visión</h3>
                <p className="text-textSecondary leading-relaxed mb-6">
                  Ser la plataforma líder en gestión de eventos, facilitando conexiones significativas
                  entre organizadores y asistentes, impulsando la industria del entretenimiento y
                  fortaleciendo comunidades a través de experiencias inolvidables.
                </p>
                <div className="flex items-center space-x-4">
                  <div className="w-12 h-12 bg-primary/20 rounded-full flex items-center justify-center">
                    <Target className="w-6 h-6 text-accent" />
                  </div>
                  <span className="text-lg font-semibold text-accent">Innovación Constante</span>
                </div>
              </div>
            </div>

            <div className="space-y-8">
              <div
                className="p-8 rounded-2xl transition-all duration-300"
                style={{ background: 'color-mix(in srgb, var(--color-surface) 50%, transparent)', backdropFilter: 'blur(8px)', border: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 50%, transparent)' }}
              >
                <h3 className="text-3xl font-bold mb-4 text-textPrimary">Nuestra Misión</h3>
                <p className="text-textSecondary leading-relaxed mb-6">
                  Proporcionar herramientas intuitivas y poderosas que permitan a organizadores
                  crear eventos excepcionales y a asistentes descubrir experiencias que enriquezcan
                  sus vidas, todo mientras mantenemos los más altos estándares de calidad y seguridad.
                </p>
                <div className="flex items-center space-x-4">
                  <div className="w-12 h-12 bg-error/20 rounded-full flex items-center justify-center">
                    <Heart className="w-6 h-6 text-error" />
                  </div>
                  <span className="text-lg font-semibold text-error">Pasión por Eventos</span>
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
              <span className="text-sm font-medium text-accent">Lo que nos define</span>
            </div>
            <h2 className="text-4xl md:text-5xl font-bold mb-8 text-textPrimary">
              Nuestros Valores
            </h2>
          </div>

          <div className="grid md:grid-cols-3 gap-8">
            {[
              { icon: Award,       title: 'Excelencia',   text: 'Nos comprometemos con la más alta calidad en todo lo que hacemos, desde el desarrollo de software hasta el soporte al cliente.',        hoverBorder: 'hover:border-primary/50',  gradient: 'from-primary to-primaryDark' },
              { icon: Users,       title: 'Colaboración', text: 'Creemos en el poder del trabajo en equipo y la colaboración para lograr objetivos comunes y crear valor para todos.',                   hoverBorder: 'hover:border-accent/50',   gradient: 'from-accent to-primary'     },
              { icon: CheckCircle, title: 'Integridad',   text: 'Operamos con honestidad, transparencia y ética en todas nuestras interacciones y decisiones.',                                          hoverBorder: 'hover:border-success/50',  gradient: 'from-success to-primaryDark' },
            ].map(({ icon: Icon, title, text, hoverBorder, gradient }) => (
              <div
                key={title}
                className={`group text-center space-y-6 p-8 rounded-2xl backdrop-blur-sm transition-all duration-300 hover:scale-105 ${hoverBorder}`}
                style={{ background: 'color-mix(in srgb, var(--color-surfaceAlt) 30%, transparent)', border: '1px solid color-mix(in srgb, var(--color-surfaceAlt) 30%, transparent)' }}
              >
                <div className={`w-20 h-20 bg-gradient-to-br ${gradient} rounded-2xl flex items-center justify-center mx-auto group-hover:scale-110 transition-transform duration-300 shadow-lg`}>
                  <Icon className="w-10 h-10 text-textPrimary" />
                </div>
                <h3 className="text-2xl font-bold text-textPrimary">{title}</h3>
                <p className="text-textSecondary leading-relaxed">{text}</p>
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
            {/* Ilustración */}
            <div className="relative">
              <div
                className="p-8 rounded-3xl"
                style={{ background: 'color-mix(in srgb, var(--color-primary) 20%, transparent)', backdropFilter: 'blur(8px)', border: '1px solid color-mix(in srgb, var(--color-accent) 30%, transparent)' }}
              >
                <div
                  className="relative h-80 rounded-2xl overflow-hidden"
                  style={{ background: 'color-mix(in srgb, var(--color-primary) 30%, transparent)' }}
                >
                  <div className="absolute top-8 left-8 w-16 h-12 rounded-lg flex items-center justify-center" style={{ background: 'rgba(255,255,255,0.2)' }}>
                    <Calendar className="w-6 h-6 text-textPrimary" />
                  </div>
                  <div className="absolute top-8 right-8 w-12 h-12 rounded-full flex items-center justify-center bg-accent/80">
                    <Clock className="w-6 h-6 text-background" />
                  </div>
                  <div className="absolute bottom-8 left-8 w-20 h-16 rounded-lg" style={{ background: 'rgba(255,255,255,0.2)' }} />
                  <div className="absolute bottom-8 right-8 w-14 h-14 bg-primary/80 rounded-full" />
                  <div className="absolute bottom-16 left-1/2 transform -translate-x-1/2">
                    <div className="w-16 h-16 rounded-full flex items-center justify-center" style={{ background: 'rgba(255,255,255,0.3)' }}>
                      <Users className="w-8 h-8 text-textPrimary" />
                    </div>
                  </div>
                  <div className="absolute top-20 left-1/4 w-2 h-2 bg-accent rounded-full animate-pulse" />
                  <div className="absolute top-32 right-1/4 w-3 h-3 bg-textPrimary/60 rounded-full animate-bounce" />
                  <div className="absolute bottom-32 left-1/3 w-1 h-1 bg-textPrimary rounded-full animate-ping" />
                </div>
              </div>
            </div>

            {/* Contenido de texto */}
            <div className="space-y-8">
              <div>
                <h2 className="text-4xl md:text-5xl font-bold mb-6 text-textPrimary">
                  Organiza tus eventos
                </h2>
                <p className="text-xl leading-relaxed mb-8" style={{ color: 'color-mix(in srgb, var(--color-textPrimary) 85%, transparent)' }}>
                  Organiza tus eventos en minutos, no en horas. Con nuestras herramientas virtuales,
                  simplifica la planificación y enfócate en lo importante.
                </p>
              </div>

              <div className="space-y-4">
                {[
                  { icon: Clock,  title: 'Ahorra tiempo',          desc: 'Planifica en minutos con nuestras herramientas intuitivas',  accentBg: 'bg-accent/80',   accentText: 'text-background' },
                  { icon: Zap,    title: 'Herramientas virtuales',  desc: 'Maquetación 2D y visualización en tiempo real',              accentBg: 'bg-primary/80',  accentText: 'text-textPrimary' },
                  { icon: Target, title: 'Enfoque en lo importante', desc: 'Dedica más tiempo a crear experiencias memorables',          accentBg: 'bg-success/80',  accentText: 'text-textPrimary' },
                ].map(({ icon: Icon, title, desc, accentBg, accentText }) => (
                  <div
                    key={title}
                    className="flex items-center space-x-4 p-4 rounded-lg border"
                    style={{ background: 'rgba(255,255,255,0.1)', backdropFilter: 'blur(8px)', borderColor: 'rgba(255,255,255,0.2)' }}
                  >
                    <div className={`w-10 h-10 ${accentBg} rounded-full flex items-center justify-center flex-shrink-0`}>
                      <Icon className={`w-5 h-5 ${accentText}`} />
                    </div>
                    <div>
                      <h3 className="font-semibold text-textPrimary">{title}</h3>
                      <p className="text-sm" style={{ color: 'color-mix(in srgb, var(--color-textPrimary) 70%, transparent)' }}>{desc}</p>
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
              <span className="text-sm font-medium text-accent">Conoce al equipo</span>
            </div>
            <h2 className="text-4xl md:text-5xl font-bold mb-8 text-textPrimary">
              Nuestro Equipo
            </h2>
            <p className="text-textSecondary text-xl max-w-3xl mx-auto leading-relaxed">
              Un equipo apasionado de 5 profesionales dedicados a hacer que cada evento sea memorable.
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
                {currentMember + 1} de {teamMembers.length}
              </span>
            </div>
          </div>
        </div>
      </section>

      <Footer />
    </div>
  );
}
