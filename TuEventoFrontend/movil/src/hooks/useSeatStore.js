import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { SeatDataStore } from '../utils/seatDataStore';

/**
 * Hook para manejo eficiente de 100k+ sillas con DOD
 * Mantiene compatibilidad con API existente basada en objetos
 */
export function useSeatStore(currentUserId) {
  const storeRef = useRef(null);
  const [version, setVersion] = useState(0);
  
  // Inicializar store solo una vez
  if (!storeRef.current) {
    storeRef.current = new SeatDataStore(150000);
  }
  
  const store = storeRef.current;

  /**
   * Carga sillas desde backend
   */
  const loadSeats = useCallback((seatsArray) => {
    store.loadSeats(seatsArray, currentUserId);
    setVersion(v => v + 1);
  }, [currentUserId]);

  /**
   * Actualiza una silla
   */
  const updateSeat = useCallback((seatId, updates) => {
    const changed = store.updateSeat(seatId, updates, currentUserId);
    if (changed) {
      setVersion(v => v + 1);
    }
    return changed;
  }, [currentUserId]);

  /**
   * Obtiene una silla como objeto
   */
  const getSeat = useCallback((seatId) => {
    return store.getSeat(seatId);
  }, [version]);

  /**
   * Obtiene todas las sillas como objeto (para compatibilidad)
   */
  const seats = useMemo(() => {
    return store.getAllSeatsAsObject();
  }, [version]);

  /**
   * Carrito (sillas reservadas por usuario)
   */
  const cart = useMemo(() => {
    return store.getUserReservations(currentUserId);
  }, [currentUserId, version]);

  /**
   * Recalcula colores cuando cambia usuario
   */
  useEffect(() => {
    if (store.count > 0) {
      store.recalculateColors(currentUserId);
      setVersion(v => v + 1);
    }
  }, [currentUserId]);

  return {
    store,        // Referencia directa al store (para renderizado optimizado)
    seats,        // Objeto de sillas (compatibilidad)
    cart,         // Array de sillas reservadas
    loadSeats,
    updateSeat,
    getSeat,
    version,      // Para detectar cambios
  };
}
