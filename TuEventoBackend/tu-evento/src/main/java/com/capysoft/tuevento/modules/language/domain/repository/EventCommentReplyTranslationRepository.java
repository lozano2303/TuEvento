package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.EventCommentReplyTranslation;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para EventCommentReplyTranslation.
 */
public interface EventCommentReplyTranslationRepository {

    /**
     * Busca una traducción específica por reply y idioma.
     */
    Optional<EventCommentReplyTranslation> findByReplyAndLanguage(Long replyId, Long languageId);

    /**
     * Busca todas las traducciones de un reply.
     */
    List<EventCommentReplyTranslation> findByReply(Long replyId);

    /**
     * Busca todas las traducciones en un idioma específico.
     */
    List<EventCommentReplyTranslation> findByLanguage(Long languageId);

    /**
     * Busca traducciones por estado.
     */
    List<EventCommentReplyTranslation> findByStatus(TranslationStatus status);

    /**
     * Busca traducciones por reply y estado.
     */
    List<EventCommentReplyTranslation> findByReplyAndStatus(Long replyId, TranslationStatus status);

    /**
     * Guarda una traducción.
     */
    EventCommentReplyTranslation save(EventCommentReplyTranslation translation);

    /**
     * Guarda múltiples traducciones.
     */
    List<EventCommentReplyTranslation> saveAll(List<EventCommentReplyTranslation> translations);

    /**
     * Elimina una traducción.
     */
    void delete(EventCommentReplyTranslation translation);

    /**
     * Elimina todas las traducciones de un reply.
     */
    void deleteByReply(Long replyId);

    /**
     * Verifica si existe una traducción para reply y idioma específicos.
     */
    boolean existsByReplyAndLanguage(Long replyId, Long languageId);
}