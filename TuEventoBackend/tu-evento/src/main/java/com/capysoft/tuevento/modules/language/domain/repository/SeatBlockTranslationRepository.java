package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.SeatBlockTranslation;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para SeatBlockTranslation.
 */
public interface SeatBlockTranslationRepository {

    /**
     * Busca una traducción específica por seat block y idioma.
     */
    Optional<SeatBlockTranslation> findBySeatBlockAndLanguage(Long seatBlockId, Integer languageId);

    /**
     * Busca todas las traducciones de un seat block.
     */
    List<SeatBlockTranslation> findBySeatBlock(Long seatBlockId);

    /**
     * Busca todas las traducciones en un idioma específico.
     */
    List<SeatBlockTranslation> findByLanguage(Integer languageId);

    /**
     * Busca traducciones por estado.
     */
    List<SeatBlockTranslation> findByStatus(TranslationStatus status);

    /**
     * Busca traducciones por seat block y estado.
     */
    List<SeatBlockTranslation> findBySeatBlockAndStatus(Long seatBlockId, TranslationStatus status);

    /**
     * Guarda una traducción.
     */
    SeatBlockTranslation save(SeatBlockTranslation translation);

    /**
     * Guarda múltiples traducciones.
     */
    List<SeatBlockTranslation> saveAll(List<SeatBlockTranslation> translations);

    /**
     * Elimina una traducción.
     */
    void delete(SeatBlockTranslation translation);

    /**
     * Elimina todas las traducciones de un seat block.
     */
    void deleteBySeatBlock(Long seatBlockId);

    /**
     * Verifica si existe una traducción para seat block y idioma específicos.
     */
    boolean existsBySeatBlockAndLanguage(Long seatBlockId, Integer languageId);
}