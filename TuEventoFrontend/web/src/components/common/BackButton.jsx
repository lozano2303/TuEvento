import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';

/**
 * BackButton — botón que navega siempre al fallback fijo.
 *
 * Se usa en flujos donde el destino de "volver" es predecible y fijo
 * independientemente de cómo llegó el usuario (wizard, link directo, etc.).
 * No usa navigate(-1) porque el historial del browser puede llevar a pantallas
 * inesperadas (ej. wizard → editor → atrás llevaría al wizard, no a "Mis eventos").
 */
export default function BackButton({ fallback = '/', label = 'Volver' }) {
  const navigate = useNavigate();

  const handleBack = () => {
    navigate(fallback);
  };

  return (
    <button
      onClick={handleBack}
      className="inline-flex items-center gap-1.5 text-sm text-textMuted hover:text-textPrimary transition-colors"
    >
      <ArrowLeft className="w-4 h-4" />
      {label}
    </button>
  );
}
