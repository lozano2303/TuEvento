import { useEffect, useState } from 'react';
import './Toast.css';

/**
 * Componente de Toast/Snackbar para mostrar alertas temporales.
 * 
 * @param {Object} props
 * @param {Object} props.toast - { key, message, type }
 * @param {Function} props.onHide - Callback cuando se oculta el toast
 */
export default function Toast({ toast, onHide }) {
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
      case 'warning': return '⚠';
      case 'info': return 'i';
      default: return '✕';
    }
  };

  const getClassName = () => {
    return `toast toast--${toast.type} ${isVisible ? 'toast--visible' : ''}`;
  };

  return (
    <div className={getClassName()}>
      <span className="toast__icon">{getIcon()}</span>
      <span className="toast__message">{toast.message}</span>
    </div>
  );
}
