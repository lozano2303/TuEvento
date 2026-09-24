import { Link } from 'react-router-dom';

export default function Footer() {
  const year = new Date().getFullYear();

  const links = [
    { label: 'Inicio',    to: '/' },
    { label: 'Nosotros',  to: '/nosotros' },
    { label: 'Eventos',   to: '/events' },
    { label: 'Perfil',    to: '/profile' },
  ];

  const legal = [
    { label: 'Términos de uso', to: '/terminos-de-uso' },
    { label: 'Privacidad',      to: '/privacidad' },
  ];

  return (
    /*
     * bg-background — un tono más oscuro que bg-surface (usado en la sección
     * anterior "Dispositivos con compatibilidad"), sin necesidad de mt ni gap.
     * El borde superior crea la separación de forma limpia.
     */
    <footer className="bg-background border-t border-surfaceAlt">

      {/* Cuerpo principal del footer */}
      <div className="max-w-6xl mx-auto px-6 py-12">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-10">

          {/* Columna 1 — Marca */}
          <div className="space-y-3">
            <span className="text-textPrimary font-black text-xl tracking-tight">
              Tu <span className="text-accent">Evento</span>
            </span>
            <p className="text-textSecondary text-sm leading-relaxed max-w-xs">
              La plataforma para descubrir, crear y gestionar eventos en vivo
              de forma sencilla y profesional.
            </p>
          </div>

          {/* Columna 2 — Navegación */}
          <div className="space-y-3">
            <p className="text-[10px] font-bold uppercase tracking-widest text-textMuted">
              Navegación
            </p>
            <ul className="space-y-2">
              {links.map(({ label, to }) => (
                <li key={to}>
                  <Link
                    to={to}
                    className="text-textSecondary hover:text-accent text-sm transition-colors"
                  >
                    {label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          {/* Columna 3 — Legal */}
          <div className="space-y-3">
            <p className="text-[10px] font-bold uppercase tracking-widest text-textMuted">
              Legal
            </p>
            <ul className="space-y-2">
              {legal.map(({ label, to }) => (
                <li key={label}>
                  <Link
                    to={to}
                    className="text-textSecondary hover:text-accent text-sm transition-colors"
                  >
                    {label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

        </div>
      </div>

      {/* Barra de copyright */}
      <div className="border-t border-surfaceAlt">
        <div className="max-w-6xl mx-auto px-6 py-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <p className="text-textMuted text-xs">
            © {year} Tu Evento · Todos los derechos reservados
          </p>
          <p className="text-textMuted text-xs">
            Hecho con ♥ por CapySoft
          </p>
        </div>
      </div>

    </footer>
  );
}
