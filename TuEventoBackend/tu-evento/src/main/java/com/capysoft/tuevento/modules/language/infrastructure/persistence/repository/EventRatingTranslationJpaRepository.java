package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.EventRatingTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRatingTranslationJpaRepository extends JpaRepository<EventRatingTranslationEntity, Long> {

    @Query("SELECT ert FROM EventRatingTranslationEntity ert WHERE ert.ratingId = :ratingId")
    List<EventRatingTranslationEntity> findByRatingId(@Param("ratingId") Long ratingId);

    @Query("SELECT ert FROM EventRatingTranslationEntity ert WHERE ert.ratingId = :ratingId AND ert.language.languageId = :languageId")
    Optional<EventRatingTranslationEntity> findByRatingIdAndLanguageId(@Param("ratingId") Long ratingId, 
                                                                       @Param("languageId") Long languageId);

    @Query("SELECT ert FROM EventRatingTranslationEntity ert WHERE ert.language.languageId = :languageId")
    List<EventRatingTranslationEntity> findByLanguageId(@Param("languageId") Long languageId);

    @Query("SELECT ert FROM EventRatingTranslationEntity ert WHERE ert.status = :status")
    List<EventRatingTranslationEntity> findByStatus(@Param("status") String status);

    @Query("SELECT ert FROM EventRatingTranslationEntity ert WHERE ert.ratingId = :ratingId AND ert.status = :status")
    List<EventRatingTranslationEntity> findByRatingIdAndStatus(@Param("ratingId") Long ratingId, @Param("status") String status);

    @Modifying
    @Query("DELETE FROM EventRatingTranslationEntity ert WHERE ert.ratingId = :ratingId")
    void deleteByRatingId(@Param("ratingId") Long ratingId);

    @Query("SELECT COUNT(ert) > 0 FROM EventRatingTranslationEntity ert WHERE ert.ratingId = :ratingId AND ert.language.languageId = :languageId")
    boolean existsByRatingIdAndLanguageId(@Param("ratingId") Long ratingId, @Param("languageId") Long languageId);
}