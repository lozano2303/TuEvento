package com.capysoft.identity.security.application.usecase;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.identity.security.application.dto.request.RequestOrganizerRequest;
import com.capysoft.identity.security.application.dto.response.RequestOrganizerResponse;
import com.capysoft.identity.security.application.port.in.RequestOrganizerPort;
import com.capysoft.identity.security.domain.event.OrganizerPetitionCreatedEvent;
import com.capysoft.identity.security.domain.model.OrganizerPetition;
import com.capysoft.identity.security.domain.model.User;
import com.capysoft.identity.security.domain.repository.OrganizerPetitionRepository;
import com.capysoft.identity.security.domain.repository.UserRepository;
import com.capysoft.identity.security.application.port.out.OrganizerDocumentPort;
import com.capysoft.common.domain.exception.BusinessException;
import com.capysoft.common.domain.exception.NotFoundException;
import com.capysoft.common.infrastructure.security.SecurityUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RequestOrganizerUseCase implements RequestOrganizerPort {

    private static final String PENDING_STATUS          = "PENDING";
    private static final String ORGANIZER_ROLE_CODE     = "ORGANIZER";
    private static final String ORGANIZER_DOCUMENT_CODE = "ORGANIZER_DOCUMENT";
    private static final String OWNER_ENTITY_TYPE       = "organizer_petition";

    private final OrganizerPetitionRepository organizerPetitionRepository;
    private final UserRepository              userRepository;
    private final OrganizerDocumentPort       organizerDocumentPort;
    private final ApplicationEventPublisher   eventPublisher;

    @Override
    @Transactional
    public RequestOrganizerResponse request(RequestOrganizerRequest request) {
        SecurityUser securityUser = (SecurityUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        User user = userRepository.findByAlias(securityUser.getAlias())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        // Verify user is not already an organizer
        if (ORGANIZER_ROLE_CODE.equals(user.getRole().getCode())) {
            throw new BusinessException("ALREADY_ORGANIZER", "User is already an organizer");
        }

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

        Integer storedFileId = organizerDocumentPort.upload(new OrganizerDocumentPort.DocumentUpload(
                ORGANIZER_DOCUMENT_CODE, OWNER_ENTITY_TYPE, user.getUserId(),
                request.getDocument().getOriginalFilename(), request.getDocument().getContentType(),
                documentBytes));

        LocalDateTime now = LocalDateTime.now();
        OrganizerPetition petition = organizerPetitionRepository.save(OrganizerPetition.builder()
                .user(user)
                .storedFileId(storedFileId)
                .applicationDate(now)
                .documentType(request.getDocumentType())
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
                .storedFileId(storedFileId)
                .build();
    }
}
