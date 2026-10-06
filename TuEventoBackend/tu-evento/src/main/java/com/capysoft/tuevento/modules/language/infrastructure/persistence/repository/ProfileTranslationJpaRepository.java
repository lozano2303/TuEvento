package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.ProfileTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileTranslationJpaRepository extends JpaRepository<ProfileTranslationEntity, Long> {

    @Query("SELECT pt FROM ProfileTranslationEntity pt WHERE pt.profileId = :profileId")
    List<ProfileTranslationEntity> findByProfileId(@Param("profileId") Long profileId);

    @Query("SELECT pt FROM ProfileTranslationEntity pt WHERE pt.profileId = :profileId AND pt.language.languageId = :languageId")
    Optional<ProfileTranslationEntity> findByProfileIdAndLanguageId(@Param("profileId") Long profileId, 
                                                                    @Param("languageId") Long languageId);

    @Query("SELECT pt FROM ProfileTranslationEntity pt WHERE pt.language.languageId = :languageId")
    List<ProfileTranslationEntity> findByLanguageId(@Param("languageId") Long languageId);

    @Query("SELECT pt FROM ProfileTranslationEntity pt WHERE pt.status = :status")
    List<ProfileTranslationEntity> findByStatus(@Param("status") String status);

    @Query("SELECT pt FROM ProfileTranslationEntity pt WHERE pt.source = :source")
    List<ProfileTranslationEntity> findBySource(@Param("source") String source);
}