package com.capysoft.tuevento.modules.storage.application.usecase;

import com.capysoft.tuevento.modules.storage.application.dto.request.StorageRequester;
import com.capysoft.tuevento.modules.storage.application.dto.response.StoredFileResponse;
import com.capysoft.tuevento.modules.storage.application.port.in.GetFilesByOwnerPort;
import com.capysoft.tuevento.modules.storage.application.service.StorageAccessPolicy;
import com.capysoft.tuevento.modules.storage.application.service.StoredFileResponseFactory;
import com.capysoft.tuevento.modules.storage.domain.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetFilesByOwnerUseCase implements GetFilesByOwnerPort {

    private final StoredFileRepository      storedFileRepository;
    private final StorageAccessPolicy       accessPolicy;
    private final StoredFileResponseFactory responseFactory;

    @Override
    @Transactional(readOnly = true)
    public List<StoredFileResponse> getByOwner(Integer ownerEntityId, String ownerEntityType,
                                               StorageRequester requester) {
        return storedFileRepository.findByOwnerEntity(ownerEntityId, ownerEntityType)
                .stream()
                .filter(file -> accessPolicy.canRead(file, requester))
                .map(responseFactory::toResponse)
                .toList();
    }
}
