package com.capysoft.tuevento.modules.storage.domain.repository;

import com.capysoft.tuevento.modules.storage.domain.model.StorageProvider;

import java.util.List;
import java.util.Optional;

public interface StorageProviderRepository {

    StorageProvider save(StorageProvider storageProvider);
    Optional<StorageProvider> findById(Integer storageProviderId);
    Optional<StorageProvider> findByCode(String code);
    List<StorageProvider> findAllActive();
}
