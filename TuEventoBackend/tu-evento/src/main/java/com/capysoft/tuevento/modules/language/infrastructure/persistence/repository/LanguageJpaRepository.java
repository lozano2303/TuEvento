package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.LanguageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para LanguageEntity.
 */
@Repository
public interface LanguageJpaRepository extends JpaRepository<LanguageEntity, Long> {

    /**
     * Busca un idioma por su código.
     */
    Optional<LanguageEntity> findByCode(String code);

    /**
     * Busca el idioma por defecto.
     */
    Optional<LanguageEntity> findByIsDefaultTrue();

    /**
     * Busca todos los idiomas activos.
     */
    List<LanguageEntity> findByIsActiveTrueOrderByName();

    /**
     * Busca todos los idiomas ordenados por nombre.
     */
    @Query("SELECT l FROM LanguageEntity l ORDER BY l.name")
    List<LanguageEntity> findAllOrderByName();

    /**
     * Verifica si existe un idioma con el código dado.
     */
    boolean existsByCode(String code);

    /**
     * Verifica si existe un idioma con el código dado (case-insensitive).
     */
    boolean existsByCodeIgnoreCase(String code);
}