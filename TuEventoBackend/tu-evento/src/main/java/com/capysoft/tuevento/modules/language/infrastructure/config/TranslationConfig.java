package com.capysoft.tuevento.modules.language.infrastructure.config;

import com.capysoft.tuevento.modules.category.infrastructure.translation.CategoryTranslatableContentHandler;
import com.capysoft.tuevento.modules.event.infrastructure.translation.EventTranslatableContentHandler;
import com.capysoft.tuevento.modules.event.infrastructure.translation.EventRatingTranslatableContentHandler;
import com.capysoft.tuevento.modules.notification.infrastructure.translation.NotificationTranslatableContentHandler;
import com.capysoft.tuevento.modules.profile.infrastructure.translation.ProfileTranslatableContentHandler;
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
    private final EventRatingTranslatableContentHandler eventRatingHandler;
    private final CategoryTranslatableContentHandler categoryHandler;
    private final ProfileTranslatableContentHandler profileHandler;
    private final NotificationTranslatableContentHandler notificationHandler;

    /**
     * Registry de handlers por tipo de entidad.
     * Cada módulo registra su handler aquí.
     */
    @Bean("translatableContentHandlerRegistry")
    public Map<String, TranslatableContentHandler> translatableContentHandlerRegistry() {
        Map<String, TranslatableContentHandler> registry = new HashMap<>();
        
        // Registrar handler de eventos
        registry.put("event", eventHandler);
        
        // Registrar handler de comentarios de eventos
        registry.put("event_rating", eventRatingHandler);
        
        // Registrar handler de categorías
        registry.put("category", categoryHandler);
        
        // Registrar handler de perfiles
        registry.put("profile", profileHandler);
        
        // Registrar handler de notificaciones
        registry.put("notification", notificationHandler);
        
        // TODO: Agregar handlers para otras entidades (sites, etc.)
        // registry.put("site", siteHandler);
        
        log.info("Registered {} translatable content handlers: {}", 
                registry.size(), registry.keySet());
        
        return registry;
    }
}