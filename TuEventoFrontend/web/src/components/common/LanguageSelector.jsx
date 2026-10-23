import { useState, useRef, useEffect } from 'react';
import { Globe } from 'lucide-react';
import { useTranslation } from 'react-i18next';

/**
 * LanguageSelector - Selector de idioma con banderas
 * 
 * Para páginas públicas/sin sesión donde el usuario puede cambiar el idioma
 * de la UI sin afectar su perfil (porque no hay sesión activa).
 * 
 * Props:
 *   - position: 'fixed-top-right' | 'fixed-top-left' | 'inline' (default: 'fixed-top-right')
 *   - className: clases CSS adicionales
 *   - compact: boolean - versión más compacta para navbar
 */
export default function LanguageSelector({ position = 'fixed-top-right', className = '', compact = false }) {
  const { i18n } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);

  const languages = [
    { code: 'es', name: 'Español', flag: '🇪🇸' },
    { code: 'en', name: 'English', flag: '🇺🇸' },
    { code: 'fr', name: 'Français', flag: '🇫🇷' },
    { code: 'pt', name: 'Português', flag: '🇧🇷' },
  ];

  const currentLanguage = languages.find(lang => lang.code === i18n.language) || languages[0];

  const handleLanguageChange = (code) => {
    i18n.changeLanguage(code);
    setIsOpen(false);
  };

  // Cerrar dropdown al hacer clic fuera
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  const positionClasses = {
    'fixed-top-right': 'fixed top-4 right-4 z-50',
    'fixed-top-left': 'fixed top-4 left-4 z-50',
    'inline': 'relative',
  };

  const buttonSize = compact ? 'px-2.5 py-1.5' : 'px-3 py-2';
  const flagSize = compact ? 'text-lg' : 'text-xl';
  const iconSize = compact ? 14 : 16;

  return (
    <div 
      ref={dropdownRef}
      className={`${positionClasses[position]} ${className}`}
    >
      {/* Botón del selector */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className={`flex items-center gap-2 ${buttonSize} rounded-lg transition-all duration-200 hover:scale-105`}
        style={{
          background: 'var(--color-surface)',
          border: '1px solid var(--color-surfaceAlt)',
          color: 'var(--color-textPrimary)',
        }}
        aria-label="Seleccionar idioma"
      >
        <span className={flagSize}>{currentLanguage.flag}</span>
        <Globe size={iconSize} className="opacity-70" />
      </button>

      {/* Dropdown */}
      {isOpen && (
        <div
          className="absolute mt-2 rounded-lg shadow-xl overflow-hidden min-w-[160px]"
          style={{
            background: 'var(--color-surface)',
            border: '1px solid var(--color-surfaceAlt)',
            right: position.includes('right') || position === 'inline' ? 0 : 'auto',
            left: position.includes('left') ? 0 : 'auto',
          }}
        >
          {languages.map((lang) => (
            <button
              key={lang.code}
              onClick={() => handleLanguageChange(lang.code)}
              className="w-full flex items-center gap-3 px-4 py-3 transition-colors"
              style={{
                background: lang.code === currentLanguage.code 
                  ? 'var(--color-surfaceAlt)' 
                  : 'transparent',
                color: 'var(--color-textPrimary)',
              }}
              onMouseEnter={(e) => {
                if (lang.code !== currentLanguage.code) {
                  e.currentTarget.style.background = 'var(--color-surfaceAlt)';
                }
              }}
              onMouseLeave={(e) => {
                if (lang.code !== currentLanguage.code) {
                  e.currentTarget.style.background = 'transparent';
                }
              }}
            >
              <span className="text-2xl">{lang.flag}</span>
              <span className="text-sm font-medium">{lang.name}</span>
              {lang.code === currentLanguage.code && (
                <span 
                  className="ml-auto text-xs font-semibold"
                  style={{ color: 'var(--color-accent)' }}
                >
                  ✓
                </span>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
