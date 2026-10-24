package com.capysoft.tuevento.modules.security.interfaces.rest;

import com.capysoft.tuevento.modules.event.application.port.in.AdminChangeEventStatusPort;
import com.capysoft.tuevento.modules.event.application.port.in.GetAdminEventsPort;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventEntity;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.repository.EventJpaRepository;
import com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository.PaymentJpaRepository;
import com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository.RefundJpaRepository;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.entity.ProfileEntity;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository.ProfileJpaRepository;
import com.capysoft.tuevento.modules.security.application.dto.request.RevokeOrganizerRoleRequest;
import com.capysoft.tuevento.modules.security.application.dto.response.ActivityItemResponse;
import com.capysoft.tuevento.modules.security.application.port.in.ApproveOrganizerRequestPort;
import com.capysoft.tuevento.modules.security.application.port.in.GetAdminPendingCountsPort;
import com.capysoft.tuevento.modules.security.application.port.in.GetOrganizerRequestsPort;
import com.capysoft.tuevento.modules.security.application.port.in.RejectOrganizerRequestPort;
import com.capysoft.tuevento.modules.security.application.port.in.RevokeOrganizerRolePort;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerPetitionRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.tuevento.modules.storage.application.port.in.GeneratePublicUrlPort;
import com.capysoft.tuevento.modules.ticket.infrastructure.persistence.repository.TicketJpaRepository;
import com.capysoft.tuevento.modules.wallet.application.usecase.BulkCreditWalletUseCaseImpl;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link AdminController} running behind Spring method security,
 * so the class-level {@code @PreAuthorize("hasAuthority('ADMIN')")} is enforced.
 *
 * <h3>Coverage</h3>
 * <ul>
 *   <li>Organizer management endpoints: ADMIN allowed; USER and ORGANIZER denied</li>
 *   <li>DELETE /organizer-users/{id}/revoke delegates to the use case with the reason and the admin id</li>
 *   <li>Activity feed tolerates events without organizer and profiles without name/user</li>
 * </ul>
 */
@SpringJUnitConfig({AdminController.class, AdminControllerTest.MethodSecurityConfig.class})
@DisplayName("AdminController")
class AdminControllerTest {

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @MockitoBean private GetOrganizerRequestsPort    getOrganizerRequestsPort;
    @MockitoBean private ApproveOrganizerRequestPort approveOrganizerRequestPort;
    @MockitoBean private RejectOrganizerRequestPort  rejectOrganizerRequestPort;
    @MockitoBean private GetAdminPendingCountsPort   getAdminPendingCountsPort;
    @MockitoBean private GeneratePublicUrlPort       generatePublicUrlPort;
    @MockitoBean private OrganizerPetitionRepository organizerPetitionRepository;
    @MockitoBean private RevokeOrganizerRolePort     revokeOrganizerRolePort;
    @MockitoBean private GetAdminEventsPort          getAdminEventsPort;
    @MockitoBean private AdminChangeEventStatusPort  adminChangeEventStatusPort;
    @MockitoBean private UserJpaRepository           userJpaRepository;
    @MockitoBean private EventJpaRepository          eventJpaRepository;
    @MockitoBean private TicketJpaRepository         ticketJpaRepository;
    @MockitoBean private PaymentJpaRepository        paymentJpaRepository;
    @MockitoBean private RefundJpaRepository         refundJpaRepository;
    @MockitoBean private ProfileJpaRepository        profileJpaRepository;
    @MockitoBean private BulkCreditWalletUseCaseImpl bulkCreditWalletUseCase;

    @Autowired
    private AdminController controller;

    private static final Integer ORGANIZER_ID = 15;
    private static final Integer ADMIN_ID     = 1;
    private static final String  REASON       = "Incumplimiento de las normas de la plataforma";
    private static final SecurityUser ADMIN   = new SecurityUser(ADMIN_ID, "admin", "ADMIN");

    private static RevokeOrganizerRoleRequest revokeRequest() {
        RevokeOrganizerRoleRequest request = new RevokeOrganizerRoleRequest();
        request.setReason(REASON);
        return request;
    }

    // ── Role restrictions ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /organizer-users/{userId}/revoke")
    class RevokeOrganizerRole {

        @Test
        @WithMockUser(authorities = "ADMIN")
        @DisplayName("ADMIN → revokes the role through the use case")
        void revoke_admin_delegatesToUseCase() {
            var response = controller.revokeOrganizerRole(ORGANIZER_ID, revokeRequest(), ADMIN);

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody().isSuccess()).isTrue();
            verify(revokeOrganizerRolePort).revoke(ORGANIZER_ID, REASON, ADMIN_ID);
        }

        @Test
        @WithMockUser(authorities = "USER")
        @DisplayName("USER → AccessDeniedException, role is not revoked")
        void revoke_user_isDenied() {
            assertThatThrownBy(() -> controller.revokeOrganizerRole(ORGANIZER_ID, revokeRequest(), ADMIN))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(revokeOrganizerRolePort);
        }

        @Test
        @WithMockUser(authorities = "ORGANIZER")
        @DisplayName("ORGANIZER → AccessDeniedException, cannot revoke own or others' role")
        void revoke_organizer_isDenied() {
            assertThatThrownBy(() -> controller.revokeOrganizerRole(ORGANIZER_ID, revokeRequest(), ADMIN))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(revokeOrganizerRolePort);
        }
    }

    // ── Activity feed null-safety ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /activity")
    @WithMockUser(authorities = "ADMIN")
    class Activity {

        private EventEntity event(Long userId, String name, EventStatus status, LocalDateTime updatedAt) {
            var entity = EventEntity.builder().eventId(1L).userId(userId).eventName(name).status(status).build();
            ReflectionTestUtils.setField(entity, "updatedAt", updatedAt);
            return entity;
        }

        private String actorFor(List<ActivityItemResponse> items, String eventName) {
            return items.stream()
                    .filter(i -> i.getDescription().contains(eventName))
                    .findFirst()
                    .orElseThrow()
                    .getActor();
        }

        @Test
        @DisplayName("resolves organizer names and falls back to 'Organizador' for missing user or name")
        void getActivity_missingOrganizerData_usesFallbackName() {
            var now = LocalDateTime.now();
            var withName    = event(ORGANIZER_ID.longValue(), "Jazz Night", EventStatus.PUBLISHED, now);
            var noOrganizer = event(null, "Orphan Fest", EventStatus.REJECTED, now.minusHours(1));
            var noName      = event(16L, "Indie Day", EventStatus.PENDING_REVIEW, now.minusHours(2));
            when(eventJpaRepository.findTopByOrderByUpdatedAtDesc(any()))
                    .thenReturn(List.of(withName, noOrganizer, noName));
            when(profileJpaRepository.findAllByUserIdIn(List.of(ORGANIZER_ID, 16))).thenReturn(List.of(
                    ProfileEntity.builder().userId(ORGANIZER_ID).fullName("Bob Marley").build(),
                    ProfileEntity.builder().userId(16).fullName(null).build(),
                    ProfileEntity.builder().userId(null).fullName("Ghost").build()));

            var items = controller.getActivity(50).getBody().getData();

            assertThat(items).hasSize(3);
            assertThat(actorFor(items, "Jazz Night")).isEqualTo("Organizer: Bob Marley");
            assertThat(actorFor(items, "Orphan Fest")).isEqualTo("Organizer: Organizador");
            assertThat(actorFor(items, "Indie Day")).isEqualTo("Organizer: Organizador");
            assertThat(items).extracting(ActivityItemResponse::getTitle).containsExactly(
                    "Event approved and published", "Event rejected", "Event submitted for review");
        }
    }
}
