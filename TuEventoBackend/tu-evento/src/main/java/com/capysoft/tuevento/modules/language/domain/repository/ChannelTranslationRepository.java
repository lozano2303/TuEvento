package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.ChannelTranslation;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para ChannelTranslation.
 */
public interface ChannelTranslationRepository {

    /**
     * Busca una traducción específica por channel y idioma.
     */
    Optional<ChannelTranslation> findByChannelAndLanguage(Long channelId, Long languageId);

    /**
     * Busca todas las traducciones de un channel.
     */
    List<ChannelTranslation> findByChannel(Long channelId);

    /**
     * Busca todas las traducciones en un idioma específico.
     */
    List<ChannelTranslation> findByLanguage(Long languageId);

    /**
     * Busca traducciones por estado.
     */
    List<ChannelTranslation> findByStatus(TranslationStatus status);

    /**
     * Busca traducciones por channel y estado.
     */
    List<ChannelTranslation> findByChannelAndStatus(Long channelId, TranslationStatus status);

    /**
     * Guarda una traducción.
     */
    ChannelTranslation save(ChannelTranslation translation);

    /**
     * Guarda múltiples traducciones.
     */
    List<ChannelTranslation> saveAll(List<ChannelTranslation> translations);

    /**
     * Elimina una traducción.
     */
    void delete(ChannelTranslation translation);

    /**
     * Elimina todas las traducciones de un channel.
     */
    void deleteByChannel(Long channelId);

    /**
     * Verifica si existe una traducción para channel y idioma específicos.
     */
    boolean existsByChannelAndLanguage(Long channelId, Long languageId);
}