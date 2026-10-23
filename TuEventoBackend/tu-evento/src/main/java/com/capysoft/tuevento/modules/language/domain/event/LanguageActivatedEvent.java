package com.capysoft.tuevento.modules.language.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Evento de dominio: idioma activado.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LanguageActivatedEvent {
    
    private Long languageId;
    private String languageCode;
}