package com.capysoft.tuevento.modules.storage.domain.repository;

import com.capysoft.tuevento.modules.storage.domain.model.FileCategory;

import java.util.List;
import java.util.Optional;

public interface FileCategoryRepository {

    FileCategory save(FileCategory fileCategory);
    Optional<FileCategory> findById(Integer fileCategoryId);
    Optional<FileCategory> findByCode(String code);
    List<FileCategory> findAll();
    List<FileCategory> findByStorageProviderId(Integer storageProviderId);
}
