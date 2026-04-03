package com.capysoft.tuevento.modules.storage.application.usecase;

import com.capysoft.tuevento.modules.storage.application.dto.response.StoredFileResponse;
import com.capysoft.tuevento.modules.storage.application.port.in.GetFilePort;
import com.capysoft.tuevento.modules.storage.domain.model.StoredFile;
import com.capysoft.tuevento.modules.storage.domain.repository.StoredFileRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetFileUseCase implements GetFilePort {

    private final StoredFileRepository storedFileRepository;

    @Override
    public StoredFileResponse getById(Integer storedFileId) {
        StoredFile file = storedFileRepository.findById(storedFileId)
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new NotFoundException("FILE_NOT_FOUND", "File not found or has been deleted"));

        return toResponse(file);
    }

    static StoredFileResponse toResponse(StoredFile file) {
        return StoredFileResponse.builder()
                .storedFileId(file.getStoredFileId())
                .fileCategoryCode(file.getFileCategory().getCode())
                .uploadedBy(file.getUploadedBy())
                .s3Key(file.getS3Key())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSizeBytes())
                .publicUrl(file.getPublicUrl())
                .ownerEntityId(file.getOwnerEntityId())
                .ownerEntityType(file.getOwnerEntityType())
                .uploadedAt(file.getUploadedAt())
                .build();
    }
}
