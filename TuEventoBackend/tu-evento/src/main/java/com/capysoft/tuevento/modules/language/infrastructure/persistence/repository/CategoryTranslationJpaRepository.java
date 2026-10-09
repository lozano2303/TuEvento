package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.CategoryTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryTranslationJpaRepository extends JpaRepository<CategoryTranslationEntity, Integer> {

    @Query("SELECT ct FROM CategoryTranslationEntity ct WHERE ct.categoryId = :categoryId")
    List<CategoryTranslationEntity> findByCategoryId(@Param("categoryId") Integer categoryId);

    @Query("SELECT ct FROM CategoryTranslationEntity ct WHERE ct.categoryId = :categoryId AND ct.languageId = :languageId")
    Optional<CategoryTranslationEntity> findByCategoryIdAndLanguageId(@Param("categoryId") Integer categoryId, 
                                                                     @Param("languageId") Integer languageId);

    @Query("SELECT ct FROM CategoryTranslationEntity ct WHERE ct.languageId = :languageId")
    List<CategoryTranslationEntity> findByLanguageId(@Param("languageId") Integer languageId);

    @Query("SELECT ct FROM CategoryTranslationEntity ct WHERE ct.status = :status")
    List<CategoryTranslationEntity> findByStatus(@Param("status") String status);

    @Query("SELECT ct FROM CategoryTranslationEntity ct WHERE ct.source = :source")
    List<CategoryTranslationEntity> findBySource(@Param("source") String source);

    @Query("SELECT ct FROM CategoryTranslationEntity ct WHERE ct.categoryId = :categoryId AND ct.status = :status")
    List<CategoryTranslationEntity> findByCategoryIdAndStatus(@Param("categoryId") Integer categoryId, @Param("status") String status);

    @Modifying
    @Query("DELETE FROM CategoryTranslationEntity ct WHERE ct.categoryId = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Integer categoryId);

    @Query("SELECT COUNT(ct) > 0 FROM CategoryTranslationEntity ct WHERE ct.categoryId = :categoryId AND ct.languageId = :languageId")
    boolean existsByCategoryIdAndLanguageId(@Param("categoryId") Integer categoryId, @Param("languageId") Integer languageId);
}