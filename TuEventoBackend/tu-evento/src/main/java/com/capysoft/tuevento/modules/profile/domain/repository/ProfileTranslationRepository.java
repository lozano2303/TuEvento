package com.capysoft.tuevento.modules.profile.domain.repository;

import com.capysoft.tuevento.modules.profile.domain.model.ProfileTranslation;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para ProfileTranslation.
 */
public interface ProfileTranslationRepository {

    Optional<ProfileTranslation> findByProfileAndLanguage(Long profileId, Integer languageId);

    List<ProfileTranslation> findByLanguage(Integer languageId);

    ProfileTranslation save(ProfileTranslation translation);

    void delete(ProfileTranslation translation);

    /** Elimina todas las traducciones de un perfil. */
    void deleteByProfile(Long profileId);
}
