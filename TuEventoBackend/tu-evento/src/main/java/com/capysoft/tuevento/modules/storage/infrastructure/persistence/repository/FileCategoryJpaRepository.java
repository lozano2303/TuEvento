package com.capysoft.tuevento.modules.storage.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.storage.infrastructure.persistence.entity.FileCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileCategoryJpaRepository extends JpaRepository<FileCategoryEntity, Integer> {

    Optional<FileCategoryEntity> findByCode(String code);

    List<FileCategoryEntity> findByStorageProviderStorageProviderId(Integer storageProviderId);
}
