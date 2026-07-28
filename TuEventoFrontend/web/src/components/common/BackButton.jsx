import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';

/**
 * BackButton — botón inteligente de "volver".
 * Si hay historial dentro de la app (window.history.state.idx > 0), retrocede.
 * Si el usuario entró directo por URL (sin historial), navega al fallback.
 * Esto evita sacar al usuario de Tu Evento si no tiene historial previo.
 */
export default function BackButton({ fallback = '/', label = 'Volver' }) {
  const navigate = useNavigate();

  const handleBack = () => {
    if (window.history.state && window.history.state.idx > 0) {
      navigate(-1);
    } else {
      navigate(fallback);
    }
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
