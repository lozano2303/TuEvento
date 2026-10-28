package com.capysoft.storage.application.usecase;

import com.capysoft.storage.application.dto.request.StorageRequester;
import com.capysoft.storage.application.dto.response.StoredFileResponse;
import com.capysoft.storage.application.port.in.GetFilePort;
import com.capysoft.storage.application.service.StorageAccessPolicy;
import com.capysoft.storage.application.service.StoredFileResponseFactory;
import com.capysoft.storage.domain.model.StoredFile;
import com.capysoft.storage.domain.repository.StoredFileRepository;
import com.capysoft.common.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetFileUseCase implements GetFilePort {

    private final StoredFileRepository      storedFileRepository;
    private final StorageAccessPolicy       accessPolicy;
    private final StoredFileResponseFactory responseFactory;

    @Override
    @Transactional(readOnly = true)
    public StoredFileResponse getById(Integer storedFileId, StorageRequester requester) {
        StoredFile file = storedFileRepository.findById(storedFileId)
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new NotFoundException("FILE_NOT_FOUND", "File not found or has been deleted"));

        accessPolicy.assertCanRead(file, requester);

        return responseFactory.toResponse(file);
    }
}
