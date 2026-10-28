package com.capysoft.geolocation.language.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Evento que se dispara cuando se cambia el idioma por defecto.
 */
@Getter
@AllArgsConstructor
public class DefaultLanguageChangedEvent {
    private final Integer previousDefaultId;
    private final Integer newDefaultId;
}