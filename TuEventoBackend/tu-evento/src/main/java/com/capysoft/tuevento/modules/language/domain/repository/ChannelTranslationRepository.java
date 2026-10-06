package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.ChannelTranslation;

import java.util.List;
import java.util.Optional;

public interface ChannelTranslationRepository {

    ChannelTranslation save(ChannelTranslation translation);

    Optional<ChannelTranslation> findById(Integer id);

    List<ChannelTranslation> findByChannelId(Integer channelId);

    Optional<ChannelTranslation> findByChannelIdAndLanguageId(Integer channelId, Integer languageId);

    List<ChannelTranslation> findByLanguageId(Integer languageId);

    void delete(ChannelTranslation translation);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    List<ChannelTranslation> findAll();
}