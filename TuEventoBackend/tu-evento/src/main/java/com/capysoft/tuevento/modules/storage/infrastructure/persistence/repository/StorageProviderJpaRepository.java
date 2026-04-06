package com.capysoft.tuevento.modules.storage.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.storage.infrastructure.persistence.entity.StorageProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StorageProviderJpaRepository extends JpaRepository<StorageProviderEntity, Integer> {

    Optional<StorageProviderEntity> findByCode(String code);

    @Query("SELECT s FROM StorageProviderEntity s WHERE s.isActive = true")
    List<StorageProviderEntity> findAllActive();
}
