package com.capysoft.tuevento.modules.storage.application.usecase;

import com.capysoft.tuevento.modules.storage.application.port.in.CheckFileAccessPort;
import com.capysoft.tuevento.modules.storage.domain.model.StoredFile;
import com.capysoft.tuevento.modules.storage.domain.repository.StoredFileRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckFileAccessUseCase implements CheckFileAccessPort {

    private final StoredFileRepository storedFileRepository;

    @Override
    @Transactional(readOnly = true)
    public void assertCanRead(Integer storedFileId, Integer requesterId, boolean requesterIsAdmin) {
        if (!load(storedFileId).isReadableBy(requesterId, requesterIsAdmin)) {
            throw new AccessDeniedException("You do not have access to this file");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanDelete(Integer storedFileId, Integer requesterId, boolean requesterIsAdmin) {
        if (!load(storedFileId).isDeletableBy(requesterId, requesterIsAdmin)) {
            throw new AccessDeniedException("You cannot delete this file");
        }
    }

    private StoredFile load(Integer storedFileId) {
        return storedFileRepository.findById(storedFileId)
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new NotFoundException("FILE_NOT_FOUND", "File not found or has been deleted"));
    }
}
