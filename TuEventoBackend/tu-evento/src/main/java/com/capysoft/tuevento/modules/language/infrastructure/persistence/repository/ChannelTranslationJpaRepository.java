package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.ChannelTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelTranslationJpaRepository extends JpaRepository<ChannelTranslationEntity, Integer> {
    
    List<ChannelTranslationEntity> findByChannelId(Integer channelId);
    
    Optional<ChannelTranslationEntity> findByChannelIdAndLanguageId(Integer channelId, Integer languageId);
    
    List<ChannelTranslationEntity> findByLanguageId(Integer languageId);
}