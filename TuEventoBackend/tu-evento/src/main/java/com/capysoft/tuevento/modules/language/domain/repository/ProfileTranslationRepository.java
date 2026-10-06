package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.ProfileTranslation;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para ProfileTranslation.
 */
public interface ProfileTranslationRepository {

    /**
     * Busca una traducción específica por perfil y idioma.
     */
    Optional<ProfileTranslation> findByProfileAndLanguage(Long profileId, Long languageId);

    /**
     * Busca todas las traducciones de un perfil.
     */
    List<ProfileTranslation> findByProfile(Long profileId);

    /**
     * Busca todas las traducciones en un idioma específico.
     */
    List<ProfileTranslation> findByLanguage(Long languageId);

    /**
     * Busca traducciones por estado.
     */
    List<ProfileTranslation> findByStatus(TranslationStatus status);

    /**
     * Busca traducciones por perfil y estado.
     */
    List<ProfileTranslation> findByProfileAndStatus(Long profileId, TranslationStatus status);

    /**
     * Guarda una traducción.
     */
    ProfileTranslation save(ProfileTranslation translation);

    /**
     * Guarda múltiples traducciones.
     */
    List<ProfileTranslation> saveAll(List<ProfileTranslation> translations);

    /**
     * Elimina una traducción.
     */
    void delete(ProfileTranslation translation);

    /**
     * Elimina todas las traducciones de un perfil.
     */
    void deleteByProfile(Long profileId);

    /**
     * Verifica si existe una traducción para perfil y idioma específicos.
     */
    boolean existsByProfileAndLanguage(Long profileId, Long languageId);
}