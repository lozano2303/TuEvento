package com.capysoft.tuevento.modules.language.infrastructure.config;

import com.capysoft.tuevento.modules.category.infrastructure.translation.CategoryTranslatableContentHandler;
import com.capysoft.tuevento.modules.event.infrastructure.translation.EventTranslatableContentHandler;
import com.capysoft.tuevento.modules.language.application.port.out.TranslatableContentHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración para el sistema de traducción.
 * Registra todos los handlers de contenido traducible disponibles.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class TranslationConfig {

    private final EventTranslatableContentHandler eventHandler;
    private final CategoryTranslatableContentHandler categoryHandler;

    /**
     * Registry de handlers por tipo de entidad.
     * Cada módulo registra su handler aquí.
     */
    @Bean("translatableContentHandlerRegistry")
    public Map<String, TranslatableContentHandler> translatableContentHandlerRegistry() {
        Map<String, TranslatableContentHandler> registry = new HashMap<>();
        
        // Registrar handler de eventos
        registry.put("event", eventHandler);
        
        // Registrar handler de categorías
        registry.put("category", categoryHandler);
        
        // TODO: Agregar handlers para otras entidades (sites, etc.)
        // registry.put("site", siteHandler);
        
        log.info("Registered {} translatable content handlers: {}", 
                registry.size(), registry.keySet());
        
        return registry;
    }
}