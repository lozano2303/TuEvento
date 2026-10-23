import { useState, useEffect, useCallback } from 'react';
import { getEventDetail } from '../services/eventService';

/**
 * Hook para manejar eventos con traducción dinámica (réplica del web).
 * Hace polling cada 3s mientras algún evento siga generando su traducción,
 * para reflejar el progreso en tiempo real.
 *
 * @param {Array}  initialEvents   lista inicial de eventos (summaries)
 * @param {string} currentLanguage código de idioma activo ('es' = sin traducción)
 */
export function useEventTranslation(initialEvents = [], currentLanguage = 'es') {
  const [events, setEvents] = useState(initialEvents);
  const [isPolling, setIsPolling] = useState(false);

  // ¿Algún evento necesita polling (traducción en curso sin resultado aún)?
  const needsPolling = events.some(
    (event) => event.translationGenerating && !event.translatedName,
  );

  const updateEvent = useCallback((eventId, updates) => {
    setEvents((prev) =>
      prev.map((e) => (e.eventId === eventId ? { ...e, ...updates } : e)),
    );
  }, []);

  // Polling: re-consulta el detalle de cada evento que sigue generando traducción.
  useEffect(() => {
    if (!needsPolling || currentLanguage === 'es') {
      setIsPolling(false);
      return;
    }

    setIsPolling(true);

    const interval = setInterval(async () => {
      const pending = events.filter(
        (e) => e.translationGenerating && !e.translatedName,
      );

      if (pending.length === 0) {
        setIsPolling(false);
        clearInterval(interval);
        return;
      }

      for (const event of pending) {
        try {
          const updated = await getEventDetail(event.eventId, currentLanguage);
          if (updated) {
            updateEvent(event.eventId, {
              translationGenerating: updated.translationGenerating || false,
              translatedName: updated.translatedName || null,
              translatedDescription: updated.translatedDescription || null,
            });
          }
        } catch {
          // error puntual — se reintenta en el próximo ciclo
        }
      }
    }, 3000);

    return () => {
      clearInterval(interval);
      setIsPolling(false);
    };
  }, [needsPolling, currentLanguage, events, updateEvent]);

  return { events, setEvents, updateEvent, isPolling, needsPolling };
}
