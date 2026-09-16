import { useEffect, useState } from 'react';
import { Lightbulb } from 'lucide-react';
import { useTheme } from '../context/ThemeContext';
import './Toast.css';

/**
 * Componente de Toast/Snackbar para mostrar alertas temporales.
 * 
 * @param {Object} props
 * @param {Object} props.toast - { key, message, type }
 * @param {Function} props.onHide - Callback cuando se oculta el toast
 */
export default function Toast({ toast, onHide }) {
  const { palette } = useTheme();
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    if (toast) {
      setIsVisible(true);
    } else {
      setIsVisible(false);
    }
  }, [toast]);

  if (!toast) return null;

  const getIcon = () => {
    switch (toast.type) {
      case 'success': return '✓';
      case 'warning': return <Lightbulb size={16} />; // Ícono de tip/sugerencia
      case 'info': return 'i';
      default: return '✕';
    }
  };

  const getStyles = () => {
    const baseStyle = {};
    
    if (toast.type === 'warning') {
      // Warning como tip/sugerencia usando colores del theme
      baseStyle.backgroundColor = palette.accent || '#A78BFA';
      baseStyle.color = palette.background || '#1E0A3C'; // Contraste con accent
    }
    
    return baseStyle;
  };

  const getClassName = () => {
    return `toast toast--${toast.type} ${isVisible ? 'toast--visible' : ''}`;
  };

  return (
    <div className={getClassName()} style={getStyles()}>
      <span className="toast__icon">{getIcon()}</span>
      <span className="toast__message">{toast.message}</span>
    </div>
  );
}
