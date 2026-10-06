package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.ChannelTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.ChannelTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.ChannelTranslationMapper;
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
    public ChannelTranslation save(ChannelTranslation translation) {
        var entity = mapper.toEntity(translation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ChannelTranslation> findById(Integer id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<ChannelTranslation> findByChannelId(Integer channelId) {
        return jpaRepository.findByChannelId(channelId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ChannelTranslation> findByChannelIdAndLanguageId(Integer channelId, Integer languageId) {
        return jpaRepository.findByChannelIdAndLanguageId(channelId, languageId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ChannelTranslation> findByLanguageId(Integer languageId) {
        return jpaRepository.findByLanguageId(languageId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(ChannelTranslation translation) {
        var entity = mapper.toEntity(translation);
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteById(Integer id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public List<ChannelTranslation> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}