package com.capysoft.storage.infrastructure.persistence.repository;

import com.capysoft.storage.application.mapper.StorageProviderMapper;
import com.capysoft.storage.domain.model.StorageProvider;
import com.capysoft.storage.domain.repository.StorageProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StorageProviderRepositoryImpl implements StorageProviderRepository {

    private final StorageProviderJpaRepository jpaRepository;
    private final StorageProviderMapper mapper;

    @Override
    public StorageProvider save(StorageProvider storageProvider) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(storageProvider)));
    }

    @Override
    public Optional<StorageProvider> findById(Integer storageProviderId) {
        return jpaRepository.findById(storageProviderId).map(mapper::toDomain);
    }

    @Override
    public Optional<StorageProvider> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<StorageProvider> findAllActive() {
        return jpaRepository.findAllActive().stream().map(mapper::toDomain).toList();
    }
}
