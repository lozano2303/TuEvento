package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.dto.request.RequestOrganizerRequest;
import com.capysoft.tuevento.modules.security.application.dto.response.RequestOrganizerResponse;
import com.capysoft.tuevento.modules.security.application.port.in.RequestOrganizerPort;
import com.capysoft.tuevento.modules.security.domain.event.OrganizerPetitionCreatedEvent;
import com.capysoft.tuevento.modules.security.domain.model.OrganizerPetition;
import com.capysoft.tuevento.modules.security.domain.model.User;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerPetitionRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
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

    private static final String PENDING_STATUS = "PENDING";

    private final OrganizerPetitionRepository organizerPetitionRepository;
    private final UserRepository              userRepository;
    private final ApplicationEventPublisher   eventPublisher;

    @Override
    @Transactional
    public RequestOrganizerResponse request(RequestOrganizerRequest request) {
        String alias = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByAlias(alias)
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

        LocalDateTime now = LocalDateTime.now();
        OrganizerPetition petition = organizerPetitionRepository.save(OrganizerPetition.builder()
                .user(user)
                .document(documentBytes)
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
                .build();
    }
}
