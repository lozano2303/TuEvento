package com.capysoft.tuevento.modules.language.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Evento de dominio: idioma desactivado.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LanguageDeactivatedEvent {
    
    private Long languageId;
    private String languageCode;
}