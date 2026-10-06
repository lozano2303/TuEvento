package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.ThemeTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThemeTranslationJpaRepository extends JpaRepository<ThemeTranslationEntity, Long> {

    @Query("SELECT tt FROM ThemeTranslationEntity tt WHERE tt.themeId = :themeId")
    List<ThemeTranslationEntity> findByThemeId(@Param("themeId") Integer themeId);

    @Query("SELECT tt FROM ThemeTranslationEntity tt WHERE tt.themeId = :themeId AND tt.language.languageId = :languageId")
    Optional<ThemeTranslationEntity> findByThemeIdAndLanguageId(@Param("themeId") Integer themeId, 
                                                               @Param("languageId") Long languageId);

    @Query("SELECT tt FROM ThemeTranslationEntity tt WHERE tt.language.languageId = :languageId")
    List<ThemeTranslationEntity> findByLanguageId(@Param("languageId") Long languageId);

    @Query("SELECT tt FROM ThemeTranslationEntity tt WHERE tt.status = :status")
    List<ThemeTranslationEntity> findByStatus(@Param("status") String status);

    @Query("SELECT tt FROM ThemeTranslationEntity tt WHERE tt.source = :source")
    List<ThemeTranslationEntity> findBySource(@Param("source") String source);
}