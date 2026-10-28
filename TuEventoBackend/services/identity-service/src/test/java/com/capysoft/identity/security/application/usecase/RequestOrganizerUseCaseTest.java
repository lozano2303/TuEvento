package com.capysoft.identity.security.application.usecase;

import com.capysoft.identity.security.application.dto.request.RequestOrganizerRequest;
import com.capysoft.identity.security.application.dto.response.RequestOrganizerResponse;
import com.capysoft.identity.security.application.port.out.OrganizerDocumentPort;
import com.capysoft.identity.security.application.port.out.OrganizerDocumentPort.DocumentUpload;
import com.capysoft.identity.security.domain.event.OrganizerPetitionCreatedEvent;
import com.capysoft.identity.security.domain.model.OrganizerPetition;
import com.capysoft.identity.security.domain.model.Role;
import com.capysoft.identity.security.domain.model.User;
import com.capysoft.identity.security.domain.repository.OrganizerPetitionRepository;
import com.capysoft.identity.security.domain.repository.UserRepository;
import com.capysoft.common.domain.exception.BusinessException;
import com.capysoft.common.infrastructure.security.SecurityUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestOrganizerUseCase")
class RequestOrganizerUseCaseTest {

    private static final Integer USER_ID = 21;
    private static final Integer STORED_FILE_ID = 99;

    @Mock private OrganizerPetitionRepository organizerPetitionRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrganizerDocumentPort organizerDocumentPort;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RequestOrganizerUseCase useCase;

    private final MockMultipartFile document =
            new MockMultipartFile("document", "id.pdf", "application/pdf", new byte[] {1, 2, 3});

    @BeforeEach
    void authenticate() {
        SecurityUser principal = mock(SecurityUser.class);
        when(principal.getAlias()).thenReturn("ana");
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void userWithRole(String roleCode) {
        User user = User.builder().userId(USER_ID).alias("ana").role(Role.builder().code(roleCode).build()).build();
        when(userRepository.findByAlias("ana")).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("uploads the document to storage and keeps only its storedFileId on the petition")
    void uploadsDocumentAndStoresFileId() {
        userWithRole("USER");
        when(organizerPetitionRepository.findPendingByUserId(USER_ID)).thenReturn(Optional.empty());
        when(organizerDocumentPort.upload(any())).thenReturn(STORED_FILE_ID);
        when(organizerPetitionRepository.save(any())).thenAnswer(invocation -> {
            OrganizerPetition petition = invocation.getArgument(0);
            petition.setOrganizerPetitionId(5);
            return petition;
        });

        RequestOrganizerResponse response = useCase.request(
                RequestOrganizerRequest.builder().document(document).documentType("CC").build());

        ArgumentCaptor<DocumentUpload> upload = ArgumentCaptor.forClass(DocumentUpload.class);
        verify(organizerDocumentPort).upload(upload.capture());
        assertThat(upload.getValue()).isEqualTo(new DocumentUpload("ORGANIZER_DOCUMENT", "organizer_petition",
                USER_ID, "id.pdf", "application/pdf", new byte[] {1, 2, 3}));

        ArgumentCaptor<OrganizerPetition> saved = ArgumentCaptor.forClass(OrganizerPetition.class);
        verify(organizerPetitionRepository).save(saved.capture());
        assertThat(saved.getValue().getStoredFileId()).isEqualTo(STORED_FILE_ID);
        assertThat(saved.getValue().getStatus()).isEqualTo("PENDING");

        assertThat(response.getOrganizerPetitionId()).isEqualTo(5);
        assertThat(response.getStoredFileId()).isEqualTo(STORED_FILE_ID);
        verify(eventPublisher).publishEvent(any(OrganizerPetitionCreatedEvent.class));
    }

    @Test
    @DisplayName("an organizer cannot request the role again and nothing is uploaded")
    void rejectsExistingOrganizer() {
        userWithRole("ORGANIZER");

        assertThatThrownBy(() -> useCase.request(RequestOrganizerRequest.builder().document(document).build()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "ALREADY_ORGANIZER");
        verify(organizerDocumentPort, never()).upload(any());
    }
}
