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
    }, 4000); // Cambia cada 4 segundos

    return () => clearInterval(interval);
  }, [teamMembers.length]);

  const nextMember = () => {
    setCurrentMember((prev) => (prev + 1) % teamMembers.length);
  };

  const prevMember = () => {
    setCurrentMember((prev) => (prev - 1 + teamMembers.length) % teamMembers.length);
  };

  const member = teamMembers[currentMember];
  return (
    <div className="min-h-screen bg-background text-textPrimary">
      {/* Hero Section */}
      <section className="relative overflow-hidden py-0">
        {/* Fondo con partículas animadas */}
        <div className="absolute inset-0 bg-purple-700">
          <div className="absolute inset-0 opacity-20">
            <div className="absolute top-20 left-20 w-2 h-2 bg-yellow-300 rounded-full animate-pulse"></div>
            <div className="absolute top-40 right-32 w-3 h-3 bg-pink-300 rounded-full animate-bounce"></div>
            <div className="absolute bottom-32 left-1/4 w-1 h-1 bg-blue-300 rounded-full animate-ping"></div>
            <div className="absolute top-60 left-3/4 w-2 h-2 bg-green-300 rounded-full animate-pulse"></div>
          </div>
          {/* Gradiente superpuesto */}
          <div className="absolute inset-0 bg-gradient-to-br from-purple-700/80 via-purple-800/60 to-purple-900/40"></div>
        </div>

        <div className="relative max-w-6xl mx-auto px-4 py-17">
          <div className="text-center space-y-8">
            <div className="inline-flex items-center px-6 py-2 bg-white/10 backdrop-blur-sm rounded-full border border-white/20 mb-6">
              <Sparkles className="w-5 h-5 text-yellow-300 mr-2" />
              <span className="text-sm font-medium">Plataforma de Eventos Innovadora</span>
            </div>
            
            <h1 className="text-5xl md:text-7xl font-bold leading-tight">
              Sobre <span className="text-yellow-300 drop-shadow-lg">Nosotros</span>
            </h1>
            
            <p className="text-xl text-purple-100 max-w-4xl mx-auto leading-relaxed">
              En <span className="font-semibold text-yellow-300">"Tu Evento"</span> transformamos ideas en experiencias inolvidables. Somos una plataforma especializada en la
              <span className="font-medium text-white"> maquetación virtual de espacios</span> para eventos, permitiendo a organizadores planificar visualmente la disposición
              de sus lugares: mesas, escenarios, pistas de baile, áreas VIP y más.
            </p>
            
            <div className="flex flex-wrap justify-center gap-4 mt-8">
              <div className="flex items-center px-4 py-2 bg-white/10 backdrop-blur-sm rounded-lg border border-white/20">
                <Zap className="w-5 h-5 text-yellow-300 mr-2" />
                <span className="text-sm">Visualización 2D</span>
              </div>
              <div className="flex items-center px-4 py-2 bg-white/10 backdrop-blur-sm rounded-lg border border-white/20">
                <Target className="w-5 h-5 text-green-300 mr-2" />
                <span className="text-sm">Planificación Precisa</span>
              </div>
              <div className="flex items-center px-4 py-2 bg-white/10 backdrop-blur-sm rounded-lg border border-white/20">
                <Heart className="w-5 h-5 text-pink-300 mr-2" />
                <span className="text-sm">Experiencias Únicas</span>
              </div>
            </div>
          </div>
        </div>

        {/* Onda decorativa mejorada */}
        <div className="relative w-full overflow-hidden mt-16">
          <svg viewBox="0 0 1200 120" className="w-full h-32 fill-background" preserveAspectRatio="none">
            <path d="M0,60 Q150,20 300,60 T600,60 Q750,100 900,60 T1200,60 L1200,120 L0,120 Z"></path>
          </svg>
        </div>
      </section>

      {/* Nuestra Historia */}
      <section className="py-24 bg-background relative overflow-hidden">
        {/* Elementos decorativos de fondo */}
        <div className="absolute inset-0 opacity-5">
          <div className="absolute top-20 left-10 w-32 h-32 bg-purple-500 rounded-full blur-3xl"></div>
          <div className="absolute bottom-20 right-10 w-40 h-40 bg-blue-500 rounded-full blur-3xl"></div>
        </div>
        
        <div className="max-w-6xl mx-auto px-4 relative z-10">
          <div className="text-center mb-20">
            <div className="inline-flex items-center px-6 py-3 bg-purple-600/20 backdrop-blur-sm rounded-full border border-purple-400/30 mb-8">
              <Calendar className="w-5 h-5 text-purple-300 mr-2" />
              <span className="text-sm font-medium text-purple-200">Nuestra Trayectoria</span>
            </div>
            
            <h2 className="text-4xl md:text-5xl font-bold mb-8 bg-gradient-to-r from-white to-gray-300 bg-clip-text text-transparent">
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
              <div className="bg-gradient-to-br from-gray-800/50 to-gray-900/50 backdrop-blur-sm p-8 rounded-2xl border border-gray-700/50 hover:border-purple-500/30 transition-all duration-300">
                <h3 className="text-3xl font-bold mb-4 text-textPrimary">Nuestra Visión</h3>
                <p className="text-textSecondary leading-relaxed mb-6">
                  Ser la plataforma líder en gestión de eventos, facilitando conexiones significativas
                  entre organizadores y asistentes, impulsando la industria del entretenimiento y
                  fortaleciendo comunidades a través de experiencias inolvidables.
                </p>
                <div className="flex items-center space-x-4">
                  <div className="w-12 h-12 bg-purple-600/20 rounded-full flex items-center justify-center">
                    <Target className="w-6 h-6 text-purple-400" />
                  </div>
                  <span className="text-lg font-semibold text-purple-200">Innovación Constante</span>
                </div>
              </div>
            </div>

            <div className="space-y-8">
              <div className="bg-gradient-to-br from-gray-800/50 to-gray-900/50 backdrop-blur-sm p-8 rounded-2xl border border-gray-700/50 hover:border-red-500/30 transition-all duration-300">
                <h3 className="text-3xl font-bold mb-4 text-textPrimary">Nuestra Misión</h3>
                <p className="text-textSecondary leading-relaxed mb-6">
                  Proporcionar herramientas intuitivas y poderosas que permitan a organizadores
                  crear eventos excepcionales y a asistentes descubrir experiencias que enriquezcan
                  sus vidas, todo mientras mantenemos los más altos estándares de calidad y seguridad.
                </p>
                <div className="flex items-center space-x-4">
                  <div className="w-12 h-12 bg-red-600/20 rounded-full flex items-center justify-center">
                    <Heart className="w-6 h-6 text-red-400" />
                  </div>
                  <span className="text-lg font-semibold text-red-200">Pasión por Eventos</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Nuestros Valores */}
      <section className="py-24 bg-surface relative overflow-hidden">
        {/* Elementos decorativos de fondo */}
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-1/4 left-1/4 w-64 h-64 bg-purple-500 rounded-full blur-3xl"></div>
          <div className="absolute bottom-1/4 right-1/4 w-48 h-48 bg-blue-500 rounded-full blur-3xl"></div>
        </div>
        
        <div className="max-w-6xl mx-auto px-4 relative z-10">
          <div className="text-center mb-20">
            <div className="inline-flex items-center px-6 py-3 bg-purple-600/20 backdrop-blur-sm rounded-full border border-purple-400/30 mb-8">
              <Award className="w-5 h-5 text-purple-300 mr-2" />
              <span className="text-sm font-medium text-purple-200">Lo que nos define</span>
            </div>
            
            <h2 className="text-4xl md:text-5xl font-bold mb-8 bg-gradient-to-r from-white to-gray-300 bg-clip-text text-transparent">
              Nuestros Valores
            </h2>
          </div>

          <div className="grid md:grid-cols-3 gap-8">
            <div className="group text-center space-y-6 p-8 bg-gradient-to-br from-gray-700/30 to-gray-800/30 backdrop-blur-sm rounded-2xl border border-gray-600/30 hover:border-purple-500/50 transition-all duration-300 hover:transform hover:scale-105">
              <div className="w-20 h-20 bg-gradient-to-br from-purple-600 to-purple-700 rounded-2xl flex items-center justify-center mx-auto group-hover:scale-110 transition-transform duration-300 shadow-lg">
                <Award className="w-10 h-10 text-white" />
              </div>
              <h3 className="text-2xl font-bold text-textPrimary">Excelencia</h3>
              <p className="text-textSecondary leading-relaxed">
                Nos comprometemos con la más alta calidad en todo lo que hacemos,
                desde el desarrollo de software hasta el soporte al cliente.
              </p>
            </div>

            <div className="group text-center space-y-6 p-8 bg-gradient-to-br from-gray-700/30 to-gray-800/30 backdrop-blur-sm rounded-2xl border border-gray-600/30 hover:border-blue-500/50 transition-all duration-300 hover:transform hover:scale-105">
              <div className="w-20 h-20 bg-gradient-to-br from-blue-600 to-blue-700 rounded-2xl flex items-center justify-center mx-auto group-hover:scale-110 transition-transform duration-300 shadow-lg">
                <Users className="w-10 h-10 text-white" />
              </div>
              <h3 className="text-2xl font-bold text-textPrimary">Colaboración</h3>
              <p className="text-textSecondary leading-relaxed">
                Creemos en el poder del trabajo en equipo y la colaboración
                para lograr objetivos comunes y crear valor para todos.
              </p>
            </div>

            <div className="group text-center space-y-6 p-8 bg-gradient-to-br from-gray-700/30 to-gray-800/30 backdrop-blur-sm rounded-2xl border border-gray-600/30 hover:border-green-500/50 transition-all duration-300 hover:transform hover:scale-105">
              <div className="w-20 h-20 bg-gradient-to-br from-green-600 to-green-700 rounded-2xl flex items-center justify-center mx-auto group-hover:scale-110 transition-transform duration-300 shadow-lg">
                <CheckCircle className="w-10 h-10 text-white" />
              </div>
              <h3 className="text-2xl font-bold text-textPrimary">Integridad</h3>
              <p className="text-textSecondary leading-relaxed">
                Operamos con honestidad, transparencia y ética en todas
                nuestras interacciones y decisiones.
              </p>
            </div>
          </div>
        </div>
      </section>

     
      {/* Organiza tus eventos */}
      <section className="py-20 bg-gradient-to-r from-purple-900 to-purple-800 relative overflow-hidden">
        {/* Elementos decorativos de fondo */}
        <div className="absolute inset-0 opacity-20">
          <div className="absolute top-10 left-10 w-32 h-32 bg-white rounded-full blur-3xl"></div>
          <div className="absolute bottom-10 right-10 w-24 h-24 bg-yellow-300 rounded-full blur-2xl"></div>
        </div>
        
        <div className="max-w-6xl mx-auto px-4 relative z-10">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            {/* Ilustración */}
            <div className="relative">
              <div className="bg-gradient-to-br from-purple-600/20 to-purple-800/20 backdrop-blur-sm p-8 rounded-3xl border border-purple-400/30">
                {/* Representación de la ilustración de planificación */}
                <div className="relative h-80 bg-gradient-to-br from-purple-500/30 to-pink-500/30 rounded-2xl overflow-hidden">
                  {/* Elementos decorativos que representan la planificación */}
                  <div className="absolute top-8 left-8 w-16 h-12 bg-white/20 rounded-lg flex items-center justify-center">
                    <Calendar className="w-6 h-6 text-white" />
                  </div>
                  <div className="absolute top-8 right-8 w-12 h-12 bg-yellow-300/80 rounded-full flex items-center justify-center">
                    <Clock className="w-6 h-6 text-purple-800" />
                  </div>
                  <div className="absolute bottom-8 left-8 w-20 h-16 bg-white/20 rounded-lg"></div>
                  <div className="absolute bottom-8 right-8 w-14 h-14 bg-pink-300/80 rounded-full"></div>
                  
                  {/* Figura central representando una persona planificando */}
                  <div className="absolute bottom-16 left-1/2 transform -translate-x-1/2">
                    <div className="w-16 h-16 bg-white/30 rounded-full flex items-center justify-center">
                      <Users className="w-8 h-8 text-white" />
                    </div>
                  </div>
                  
                  {/* Partículas decorativas */}
                  <div className="absolute top-20 left-1/4 w-2 h-2 bg-yellow-300 rounded-full animate-pulse"></div>
                  <div className="absolute top-32 right-1/4 w-3 h-3 bg-pink-300 rounded-full animate-bounce"></div>
                  <div className="absolute bottom-32 left-1/3 w-1 h-1 bg-white rounded-full animate-ping"></div>
                </div>
              </div>
            </div>

            {/* Contenido de texto */}
            <div className="space-y-8">
              <div>
                <h2 className="text-4xl md:text-5xl font-bold mb-6 text-textPrimary">
                  Organiza tus eventos
                </h2>
                <p className="text-xl text-purple-100 leading-relaxed mb-8">
                  Organiza tus eventos en minutos, no en horas. Con nuestras herramientas virtuales, 
                  simplifica la planificación y enfócate en lo importante.
                </p>
              </div>

              {/* Características destacadas */}
              <div className="space-y-4">
                <div className="flex items-center space-x-4 p-4 bg-white/10 backdrop-blur-sm rounded-lg border border-white/20">
                  <div className="w-10 h-10 bg-yellow-300 rounded-full flex items-center justify-center">
                    <Clock className="w-5 h-5 text-purple-800" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-textPrimary">Ahorra tiempo</h3>
                    <p className="text-purple-200 text-sm">Planifica en minutos con nuestras herramientas intuitivas</p>
                  </div>
                </div>
                
                <div className="flex items-center space-x-4 p-4 bg-white/10 backdrop-blur-sm rounded-lg border border-white/20">
                  <div className="w-10 h-10 bg-pink-300 rounded-full flex items-center justify-center">
                    <Zap className="w-5 h-5 text-purple-800" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-textPrimary">Herramientas virtuales</h3>
                    <p className="text-purple-200 text-sm">Maquetación 2D y visualización en tiempo real</p>
                  </div>
                </div>
                
                <div className="flex items-center space-x-4 p-4 bg-white/10 backdrop-blur-sm rounded-lg border border-white/20">
                  <div className="w-10 h-10 bg-green-300 rounded-full flex items-center justify-center">
                    <Target className="w-5 h-5 text-purple-800" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-textPrimary">Enfoque en lo importante</h3>
                    <p className="text-purple-200 text-sm">Dedica más tiempo a crear experiencias memorables</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

       {/* Nuestro Equipo */}
      <section className="py-24 bg-background relative overflow-hidden">
        <div className="absolute inset-0 opacity-5">
          <div className="absolute top-20 right-20 w-56 h-56 bg-green-500 rounded-full blur-3xl"></div>
          <div className="absolute bottom-20 left-20 w-40 h-40 bg-pink-500 rounded-full blur-3xl"></div>
        </div>
        
        <div className="max-w-6xl mx-auto px-4 relative z-10">
          <div className="text-center mb-16">
            <div className="inline-flex items-center px-6 py-3 bg-purple-600/20 backdrop-blur-sm rounded-full border border-purple-400/30 mb-8">
              <Users className="w-5 h-5 text-purple-300 mr-2" />
              <span className="text-sm font-medium text-purple-200">Conoce al equipo</span>
            </div>
            
            <h2 className="text-4xl md:text-5xl font-bold mb-8 bg-gradient-to-r from-white to-gray-300 bg-clip-text text-transparent">
              Nuestro Equipo
            </h2>
            <p className="text-textSecondary text-xl max-w-3xl mx-auto leading-relaxed">
              Un equipo apasionado de 5 profesionales dedicados a hacer que cada evento sea memorable.
            </p>
          </div>

          {/* Carrusel del equipo */}
          <div className="relative bg-gradient-to-br from-gray-800/40 to-gray-900/40 backdrop-blur-sm rounded-3xl border border-gray-700/30 p-8 md:p-12">
            <div className="grid md:grid-cols-2 gap-12 items-center min-h-[400px]">
              {/* Foto del miembro */}
              <div className="relative">
                <div className="relative group">
                  <div className={`w-80 h-80 mx-auto bg-gradient-to-r ${member.gradient} rounded-3xl overflow-hidden shadow-2xl group-hover:scale-105 transition-all duration-500`}>
                    {member.id === 1 ? (
                      <img src="/src/assets/images/francisco.jpg" alt={member.name} className="w-full h-full object-cover" />
                    ) : member.id === 2 ? (
                      <img src="/src/assets/images/keiner.png" alt={member.name} className="w-full h-full object-cover" />
                    ) : member.id === 3 ? (
                      <img src="/src/assets/images/cristofer.jpg" alt={member.name} className="w-full h-full object-cover" />
                    ) : member.id === 4 ? (
                      <img src="/src/assets/images/angel.jpg" alt={member.name} className="w-full h-full object-cover" />
                    ) : member.id === 5 ? (
                      <img src="/src/assets/images/jhampier.jpg" alt={member.name} className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-white font-bold text-6xl">
                        {member.initials}
                      </div>
                    )}
                  </div>
                  {/* Elementos decorativos alrededor de la foto */}
                  <div className="absolute -top-4 -left-4 w-8 h-8 bg-yellow-400 rounded-full animate-pulse"></div>
                  <div className="absolute -bottom-4 -right-4 w-6 h-6 bg-pink-400 rounded-full animate-bounce"></div>
                  <div className="absolute top-8 -right-2 w-4 h-4 bg-blue-400 rounded-full animate-ping"></div>
                </div>
              </div>

              {/* Información del miembro */}
              <div className="space-y-6 text-center md:text-left">
                <div>
                  <h3 className="text-3xl md:text-4xl font-bold text-textPrimary mb-2">
                    {member.name}
                  </h3>
                  <p className="text-xl text-textSecondary font-semibold mb-4">
                    {member.role}
                  </p>
                  <p className="text-textSecondary text-lg leading-relaxed">
                    {member.description}
                  </p>
                </div>

                {/* Información de contacto */}
                <div className="space-y-4">
                  <div className="flex items-center justify-center md:justify-start space-x-3 p-3 bg-white/5 backdrop-blur-sm rounded-lg border border-white/10">
                    <div className="w-3 h-3 bg-purple-400 rounded-full"></div>
                    <span className="text-textSecondary">{member.email}</span>
                  </div>
                  <div className="flex items-center justify-center md:justify-start space-x-3 p-3 bg-white/5 backdrop-blur-sm rounded-lg border border-white/10">
                    <div className="w-3 h-3 bg-green-400 rounded-full"></div>
                    <span className="text-textSecondary">{member.phone}</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Controles de navegación */}
            <div className="flex items-center justify-between mt-8">
              {/* Botón anterior */}
              <button 
                onClick={prevMember}
                className="w-12 h-12 bg-white/10 hover:bg-white/20 backdrop-blur-sm border border-white/20 rounded-full flex items-center justify-center transition-all duration-300 hover:scale-110"
              >
                <ChevronLeft className="w-6 h-6 text-white" />
              </button>

              {/* Indicadores */}
              <div className="flex space-x-3">
                {teamMembers.map((_, index) => (
                  <button
                    key={index}
                    onClick={() => setCurrentMember(index)}
                    className={`w-3 h-3 rounded-full transition-all duration-300 ${
                      index === currentMember
                        ? 'bg-purple-400 scale-125'
                        : 'bg-white/30 hover:bg-white/50'
                    }`}
                  />
                ))}
              </div>

              {/* Botón siguiente */}
              <button 
                onClick={nextMember}
                className="w-12 h-12 bg-white/10 hover:bg-white/20 backdrop-blur-sm border border-white/20 rounded-full flex items-center justify-center transition-all duration-300 hover:scale-110"
              >
                <ChevronRight className="w-6 h-6 text-white" />
              </button>
            </div>

            {/* Contador del miembro actual */}
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