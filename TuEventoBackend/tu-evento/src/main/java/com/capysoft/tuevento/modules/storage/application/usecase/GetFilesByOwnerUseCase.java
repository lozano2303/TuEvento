package com.capysoft.tuevento.modules.storage.application.usecase;

import com.capysoft.tuevento.modules.storage.application.dto.response.StoredFileResponse;
import com.capysoft.tuevento.modules.storage.application.port.in.GetFilesByOwnerPort;
import com.capysoft.tuevento.modules.storage.domain.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetFilesByOwnerUseCase implements GetFilesByOwnerPort {

    private final StoredFileRepository storedFileRepository;

    @Override
    public List<StoredFileResponse> getByOwner(Integer ownerEntityId, String ownerEntityType) {
        return storedFileRepository.findByOwnerEntity(ownerEntityId, ownerEntityType)
                .stream()
                .map(GetFileUseCase::toResponse)
                .toList();
    }
}
