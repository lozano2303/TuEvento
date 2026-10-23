import { useState, useCallback, useRef } from 'react';

/**
 * Hook para mostrar alertas tipo toast con temporizador automático.
 * 
 * Comportamiento:
 * - Una sola alerta visible a la vez (reemplaza la anterior)
 * - Duración: 5 segundos
 * - Si se dispara la misma alerta (mismo key) mientras está visible, reinicia el temporizador
 * 
 * @returns {Object} { toast, showToast, hideToast }
 */
export const useToast = () => {
  const [toast, setToast] = useState(null); // { key, message, type }
  const timerRef = useRef(null);

  const hideToast = useCallback(() => {
    setToast(null);
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  const showToast = useCallback((key, message, type = 'error') => {
    // Limpiar temporizador anterior si existe
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }

    // Mostrar nueva alerta
    setToast({ key, message, type });

    // Configurar auto-hide después de 5 segundos
    timerRef.current = setTimeout(() => {
      hideToast();
    }, 5000);
  }, [hideToast]);

  return { toast, showToast, hideToast };
};
