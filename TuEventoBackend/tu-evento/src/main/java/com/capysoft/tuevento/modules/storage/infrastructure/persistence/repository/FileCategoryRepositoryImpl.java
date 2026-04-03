package com.capysoft.tuevento.modules.storage.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.storage.application.mapper.FileCategoryMapper;
import com.capysoft.tuevento.modules.storage.domain.model.FileCategory;
import com.capysoft.tuevento.modules.storage.domain.repository.FileCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FileCategoryRepositoryImpl implements FileCategoryRepository {

    private final FileCategoryJpaRepository jpaRepository;
    private final FileCategoryMapper mapper;

    @Override
    public FileCategory save(FileCategory fileCategory) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(fileCategory)));
    }

    @Override
    public Optional<FileCategory> findById(Integer fileCategoryId) {
        return jpaRepository.findById(fileCategoryId).map(mapper::toDomain);
    }

    @Override
    public Optional<FileCategory> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<FileCategory> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<FileCategory> findByStorageProviderId(Integer storageProviderId) {
        return jpaRepository.findByStorageProviderStorageProviderId(storageProviderId)
                .stream().map(mapper::toDomain).toList();
    }
}
