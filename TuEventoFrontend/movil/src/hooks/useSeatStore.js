import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { SeatDataStore } from '../utils/seatDataStore';

/**
 * Hook para manejo eficiente de 100k+ sillas con DOD.
 *
 * El componente lee las sillas directamente del store (`store`); `version` cambia en
 * cada modificación para que React vuelva a renderizar. No se expone un objeto con
 * todas las sillas: reconstruirlo en cada update de WebSocket costaba O(n).
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
   * Carga sillas desde backend (reemplaza el contenido)
   */
  const loadSeats = useCallback((seatsArray) => {
    store.loadSeats(seatsArray, currentUserId);
    setVersion(v => v + 1);
  }, [currentUserId]);

  /**
   * Agrega las sillas de una sección (o actualiza las que ya estaban)
   */
  const upsertSeats = useCallback((seatsArray) => {
    if (store.upsertSeats(seatsArray, currentUserId)) {
      setVersion(v => v + 1);
    }
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
   * Obtiene una silla como objeto (lee el estado actual del store)
   */
  const getSeat = useCallback((seatId) => store.getSeat(seatId), []);

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
    cart,         // Array de sillas reservadas
    loadSeats,
    upsertSeats,
    updateSeat,
    getSeat,
    version,      // Para detectar cambios
  };
}
