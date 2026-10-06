package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.ChannelTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.ChannelTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.ChannelTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ChannelTranslationRepositoryImpl implements ChannelTranslationRepository {

    private final ChannelTranslationJpaRepository jpaRepository;
    private final ChannelTranslationMapper mapper;

    @Override
    public Optional<ChannelTranslation> findByChannelAndLanguage(Long channelId, Long languageId) {
        return jpaRepository.findByChannelIdAndLanguageId(channelId.intValue(), languageId.intValue())
                .map(mapper::toDomain);
    }

    @Override
    public List<ChannelTranslation> findByChannel(Long channelId) {
        return jpaRepository.findByChannelId(channelId.intValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChannelTranslation> findByLanguage(Long languageId) {
        return jpaRepository.findByLanguageId(languageId.intValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChannelTranslation> findByStatus(TranslationStatus status) {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .filter(translation -> translation.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChannelTranslation> findByChannelAndStatus(Long channelId, TranslationStatus status) {
        return jpaRepository.findByChannelId(channelId.intValue())
                .stream()
                .map(mapper::toDomain)
                .filter(translation -> translation.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    @Override
    public ChannelTranslation save(ChannelTranslation translation) {
        var entity = mapper.toEntity(translation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<ChannelTranslation> saveAll(List<ChannelTranslation> translations) {
        var entities = translations.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        var savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(ChannelTranslation translation) {
        var entity = mapper.toEntity(translation);
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteByChannel(Long channelId) {
        var entities = jpaRepository.findByChannelId(channelId.intValue());
        jpaRepository.deleteAll(entities);
    }

    @Override
    public boolean existsByChannelAndLanguage(Long channelId, Long languageId) {
        return jpaRepository.findByChannelIdAndLanguageId(channelId.intValue(), languageId.intValue())
                .isPresent();
    }
}