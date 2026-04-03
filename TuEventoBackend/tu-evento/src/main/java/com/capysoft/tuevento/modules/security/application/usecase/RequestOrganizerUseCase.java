package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.dto.request.RequestOrganizerRequest;
import com.capysoft.tuevento.modules.security.application.dto.response.RequestOrganizerResponse;
import com.capysoft.tuevento.modules.security.application.port.in.RequestOrganizerPort;
import com.capysoft.tuevento.modules.security.domain.event.OrganizerPetitionCreatedEvent;
import com.capysoft.tuevento.modules.security.domain.model.OrganizerPetition;
import com.capysoft.tuevento.modules.security.domain.model.User;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerPetitionRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserRepository;
import com.capysoft.tuevento.modules.storage.application.dto.request.UploadFileRequest;
import com.capysoft.tuevento.modules.storage.application.dto.response.UploadFileResponse;
import com.capysoft.tuevento.modules.storage.application.port.in.UploadFilePort;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RequestOrganizerUseCase implements RequestOrganizerPort {

    private static final String PENDING_STATUS          = "PENDING";
    private static final String ORGANIZER_DOCUMENT_CODE = "ORGANIZER_DOCUMENT";
    private static final String OWNER_ENTITY_TYPE       = "organizer_petition";

    private final OrganizerPetitionRepository organizerPetitionRepository;
    private final UserRepository              userRepository;
    private final UploadFilePort              uploadFilePort;
    private final ApplicationEventPublisher   eventPublisher;

    @Override
    @Transactional
    public RequestOrganizerResponse request(RequestOrganizerRequest request) {
        SecurityUser securityUser = (SecurityUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        User user = userRepository.findByAlias(securityUser.getAlias())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        organizerPetitionRepository.findPendingByUserId(user.getUserId())
                .ifPresent(existing -> {
                    throw new BusinessException("PETITION_ALREADY_PENDING",
                            "There is already a pending organizer petition for this user");
                });

        byte[] documentBytes;
        try {
            documentBytes = request.getDocument().getBytes();
        } catch (IOException e) {
            throw new BusinessException("DOCUMENT_READ_ERROR", "Failed to read uploaded document");
        }

        UploadFileResponse fileResponse = uploadFilePort.upload(UploadFileRequest.builder()
                .fileCategoryCode(ORGANIZER_DOCUMENT_CODE)
                .uploadedBy(user.getUserId())
                .originalFilename(request.getDocument().getOriginalFilename())
                .contentType(request.getDocument().getContentType())
                .content(documentBytes)
                .ownerEntityType(OWNER_ENTITY_TYPE)
                .build());

        LocalDateTime now = LocalDateTime.now();
        OrganizerPetition petition = organizerPetitionRepository.save(OrganizerPetition.builder()
                .user(user)
                .storedFileId(fileResponse.getStoredFileId())
                .applicationDate(now)
                .status(PENDING_STATUS)
                .build());

        eventPublisher.publishEvent(OrganizerPetitionCreatedEvent.builder()
                .userId(user.getUserId())
                .organizerPetitionId(petition.getOrganizerPetitionId())
                .occurredAt(now)
                .build());

        return RequestOrganizerResponse.builder()
                .organizerPetitionId(petition.getOrganizerPetitionId())
                .status(petition.getStatus())
                .applicationDate(petition.getApplicationDate())
                .storedFileId(fileResponse.getStoredFileId())
                .build();
    }
}
