export default function Footer() {
  return (
    <footer className="bg-gray-900 border-t border-gray-800 py-6">
      <div className="max-w-6xl mx-auto px-4">

        {/* Contenido centrado */}
        <div className="flex flex-col items-center gap-3">

          {/* Logo + nombre */}
          <div className="flex items-center gap-2">
  
            <span className="text-white font-semibold text-lg tracking-wide">
              Tu <span className="text-purple-400">Evento</span>
            </span>
          </div>

          {/* Slogan */}
          <p className="text-gray-500 text-xs tracking-wide">
            Tu plataforma de eventos favorita
          </p>

          {/* Divisor */}
          <div className="w-16 h-px bg-gray-700" />

          {/* Copyright */}
          <p className="text-gray-600 text-xs">
            © 2026 Tu Evento · Todos los derechos reservados
          </p>

        </div>
      </div>
    </footer>
  );
}