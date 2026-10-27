package com.capysoft.identity.theme.infrastructure.persistence.repository;

import com.capysoft.identity.theme.infrastructure.persistence.entity.ThemeTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ThemeTranslationJpaRepository extends JpaRepository<ThemeTranslationEntity, Integer> {

    @Query("SELECT tt FROM ThemeTranslationEntity tt WHERE tt.themeId = :themeId AND tt.languageId = :languageId")
    Optional<ThemeTranslationEntity> findByThemeIdAndLanguageId(@Param("themeId") Integer themeId, 
                                                               @Param("languageId") Integer languageId);
}
