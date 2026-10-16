import { useState, useEffect, useCallback } from 'react';
import * as EventService from '../services/EventService';

/**
 * Hook personalizado para manejar eventos con traducción automática
 * Implementa polling para mostrar progreso de traducción en tiempo real
 * Similar al sistema implementado en ProfilePage
 */
export function useEventTranslation(initialEvents = [], currentLanguage = 'es') {
  const [events, setEvents] = useState(initialEvents);
  const [isPolling, setIsPolling] = useState(false);

  // Determinar si algún evento necesita polling
  const needsPolling = events.some(event => 
    event.translationGenerating && !event.translatedName
  );

  // Función para actualizar un evento específico
  const updateEvent = useCallback((eventId, updates) => {
    setEvents(prevEvents => 
      prevEvents.map(event => 
        event.eventId === eventId 
          ? { ...event, ...updates }
          : event
      )
    );
  }, []);

  // Función para cargar eventos con traducción
  const loadEventsWithTranslation = useCallback(async (userId) => {
    try {
      // Primero obtenemos los eventos del usuario (summary sin traducción)
      const response = await EventService.getEventsByUser(userId);
      let eventsData = response.data || [];
      
      // Si el idioma NO es español, consultar cada evento con traducción
      if (currentLanguage !== 'es' && eventsData.length > 0) {
        const translatedEvents = await Promise.all(
          eventsData.map(async (event) => {
            try {
              const fullEventResponse = await EventService.getEventByIdWithLanguage(event.eventId, currentLanguage);
              const fullEvent = fullEventResponse.data;
              
              return {
                ...event,
                translatedName: fullEvent?.translatedName || null,
                translatedDescription: fullEvent?.translatedDescription || null,
                translationGenerating: fullEvent?.translationGenerating || false
              };
            } catch (error) {
              console.error(`Error getting translation for event ${event.eventId}:`, error);
              return event; // Retornar evento original si falla
            }
          })
        );
        eventsData = translatedEvents;
      }
      
      setEvents(eventsData);
      return eventsData;
    } catch (error) {
      console.error('Error loading events with translation:', error);
      throw error;
    }
  }, [currentLanguage]);

  // Polling para eventos que están generando traducción
  useEffect(() => {
    if (!needsPolling || currentLanguage === 'es') {
      setIsPolling(false);
      return;
    }

    setIsPolling(true);

    const pollInterval = setInterval(async () => {
      try {
        // Solo hacer polling para eventos que están generando traducción
        const eventsToCheck = events.filter(event => 
          event.translationGenerating && !event.translatedName
        );

        if (eventsToCheck.length === 0) {
          setIsPolling(false);
          clearInterval(pollInterval);
          return;
        }

        // Verificar cada evento individualmente
        for (const event of eventsToCheck) {
          try {
            const response = await EventService.getEventByIdWithLanguage(event.eventId, currentLanguage);
            const updatedEvent = response.data;
            
            if (updatedEvent) {
              updateEvent(event.eventId, {
                translationGenerating: updatedEvent.translationGenerating || false,
                translatedName: updatedEvent.translatedName || null,
                translatedDescription: updatedEvent.translatedDescription || null
              });
            }
          } catch (error) {
            console.error(`Error polling event ${event.eventId}:`, error);
          }
        }
      } catch (error) {
        console.error('Error during polling:', error);
      }
    }, 3000); // Poll cada 3 segundos como ProfilePage

    return () => {
      clearInterval(pollInterval);
      setIsPolling(false);
    };
  }, [needsPolling, currentLanguage, events, updateEvent]);

  return {
    events,
    setEvents,
    updateEvent,
    loadEventsWithTranslation,
    isPolling,
    needsPolling
  };
}