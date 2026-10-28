package com.capysoft.identity.profile.domain.repository;

import com.capysoft.identity.profile.domain.model.ProfileTranslation;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para ProfileTranslation.
 */
public interface ProfileTranslationRepository {

    /**
     * Busca una traducción específica por perfil y idioma.
     */
    Optional<ProfileTranslation> findByProfileAndLanguage(Long profileId, Integer languageId);

    /**
     * Busca todas las traducciones en un idioma específico.
     */
    List<ProfileTranslation> findByLanguage(Integer languageId);

    /**
     * Guarda una traducción.
     */
    ProfileTranslation save(ProfileTranslation translation);

    /**
     * Elimina una traducción.
     */
    void delete(ProfileTranslation translation);

    /**
     * Elimina todas las traducciones de un perfil.
     */
    void deleteByProfile(Long profileId);
}
