package com.capysoft.tuevento.modules.section.domain.exception;

import com.capysoft.tuevento.shared.domain.exception.NotFoundException;

/**
 * Excepción lanzada cuando no se encuentra una sección de evento.
 */
public class EventSectionNotFoundException extends NotFoundException {
    
    private static final String CODE = "EVENT_SECTION_NOT_FOUND";
    
    public EventSectionNotFoundException(Integer eventSectionId) {
        super(CODE, "Event section not found: " + eventSectionId + ". Cannot determine seat price.");
    }
}
