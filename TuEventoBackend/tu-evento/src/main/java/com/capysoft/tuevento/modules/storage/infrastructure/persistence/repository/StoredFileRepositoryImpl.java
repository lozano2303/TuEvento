package com.capysoft.tuevento.modules.storage.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.storage.application.mapper.StoredFileMapper;
import com.capysoft.tuevento.modules.storage.domain.model.StoredFile;
import com.capysoft.tuevento.modules.storage.domain.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StoredFileRepositoryImpl implements StoredFileRepository {

    private final StoredFileJpaRepository jpaRepository;
    private final StoredFileMapper mapper;

    @Override
    public StoredFile save(StoredFile storedFile) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(storedFile)));
    }

    @Override
    public Optional<StoredFile> findById(Integer storedFileId) {
        return jpaRepository.findById(storedFileId).map(mapper::toDomain);
    }

    @Override
    public Optional<StoredFile> findByS3Key(String s3Key) {
        return jpaRepository.findByS3Key(s3Key).map(mapper::toDomain);
    }

    @Override
    public List<StoredFile> findByUploadedBy(Integer uploadedBy) {
        return jpaRepository.findByUploadedBy(uploadedBy).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<StoredFile> findByFileCategoryId(Integer fileCategoryId) {
        return jpaRepository.findByFileCategoryId(fileCategoryId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<StoredFile> findByOwnerEntity(Integer ownerEntityId, String ownerEntityType) {
        return jpaRepository.findByOwnerEntity(ownerEntityId, ownerEntityType)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void softDelete(Integer storedFileId) {
        jpaRepository.softDelete(storedFileId);
    }

    @Override
    public void deleteById(Integer storedFileId) {
        jpaRepository.deleteById(storedFileId);
    }
}
