package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.SeatBlockTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.SeatBlockTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.SeatBlockTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SeatBlockTranslationRepositoryImpl implements SeatBlockTranslationRepository {

    private final SeatBlockTranslationJpaRepository jpaRepository;
    private final SeatBlockTranslationMapper mapper;

    @Override
    public Optional<SeatBlockTranslation> findBySeatBlockAndLanguage(Long seatBlockId, Integer languageId) {
        return jpaRepository.findBySeatBlockIdAndLanguageId(seatBlockId.intValue(), languageId)
                .map(mapper::toDomain);
    }

    @Override
    public List<SeatBlockTranslation> findBySeatBlock(Long seatBlockId) {
        return jpaRepository.findBySeatBlockId(seatBlockId.intValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SeatBlockTranslation> findByLanguage(Integer languageId) {
        return jpaRepository.findByLanguageId(languageId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SeatBlockTranslation> findByStatus(TranslationStatus status) {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .filter(translation -> status.equals(translation.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SeatBlockTranslation> findBySeatBlockAndStatus(Long seatBlockId, TranslationStatus status) {
        return findBySeatBlock(seatBlockId)
                .stream()
                .filter(translation -> status.equals(translation.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public SeatBlockTranslation save(SeatBlockTranslation translation) {
        var entity = mapper.toEntity(translation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<SeatBlockTranslation> saveAll(List<SeatBlockTranslation> translations) {
        var entities = translations.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        var savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(SeatBlockTranslation translation) {
        var entity = mapper.toEntity(translation);
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteBySeatBlock(Long seatBlockId) {
        var entities = jpaRepository.findBySeatBlockId(seatBlockId.intValue());
        jpaRepository.deleteAll(entities);
    }

    @Override
    public boolean existsBySeatBlockAndLanguage(Long seatBlockId, Integer languageId) {
        return jpaRepository.findBySeatBlockIdAndLanguageId(seatBlockId.intValue(), languageId).isPresent();
    }
}