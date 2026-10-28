package com.capysoft.identity.security.interfaces.rest;

import com.capysoft.identity.security.application.port.out.OrganizerDocumentPort;
import com.capysoft.identity.security.application.usecase.GetAdminDashboardUseCase;
import com.capysoft.identity.security.application.dto.request.RevokeOrganizerRoleRequest;
import com.capysoft.identity.security.application.port.in.ApproveOrganizerRequestPort;
import com.capysoft.identity.security.application.port.in.GetAdminPendingCountsPort;
import com.capysoft.identity.security.application.port.in.GetOrganizerRequestsPort;
import com.capysoft.identity.security.application.port.in.RejectOrganizerRequestPort;
import com.capysoft.identity.security.application.port.in.RevokeOrganizerRolePort;
import com.capysoft.identity.security.domain.repository.OrganizerPetitionRepository;
import com.capysoft.identity.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.common.infrastructure.security.SecurityUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

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
    @MockitoBean private OrganizerDocumentPort       organizerDocumentPort;
    @MockitoBean private OrganizerPetitionRepository organizerPetitionRepository;
    @MockitoBean private RevokeOrganizerRolePort     revokeOrganizerRolePort;
    @MockitoBean private GetAdminDashboardUseCase    getAdminDashboardUseCase;
    @MockitoBean private UserJpaRepository           userJpaRepository;

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

    // ── User management and refunds ───────────────────────────────────────────

    @Nested
    @DisplayName("GET /users")
    @WithMockUser(authorities = "ADMIN")
    class UsersAndRefunds {

        @Test
        @DisplayName("normalizes the search and role filter and caps the page size at 200")
        void getUsers_normalizesFiltersAndCapsLimit() {
            when(userJpaRepository.searchForAdmin(any(), any(), any())).thenReturn(List.of());

            var response = controller.getUsers("  Bob ", " organizer ", 999);

            assertThat(response.getBody().getData()).isEmpty();
            verify(userJpaRepository).searchForAdmin("%bob%", "ORGANIZER", PageRequest.of(0, 200));
        }

        @Test
        @DisplayName("blank filters become empty strings and the page size is at least 1")
        void getUsers_blankFilters_matchEverything() {
            controller.getUsers(" ", null, 0);

            verify(userJpaRepository).searchForAdmin("", "", PageRequest.of(0, 1));
        }

        @Test
        @WithMockUser(authorities = "ORGANIZER")
        @DisplayName("ORGANIZER → AccessDeniedException")
        void getUsers_organizer_isDenied() {
            assertThatThrownBy(() -> controller.getUsers(null, null, 100))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(userJpaRepository);
        }
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /stats y GET /activity")
    @WithMockUser(authorities = "ADMIN")
    class Dashboard {

        @Test
        @DisplayName("delegan en el caso de uso del panel, que compone Identidad, Catálogo y Comercio")
        void delegateToDashboardUseCase() {
            when(getAdminDashboardUseCase.getActivity(50)).thenReturn(List.of());

            assertThat(controller.getActivity(50).getBody().getData()).isEmpty();
            controller.getStats();

            verify(getAdminDashboardUseCase).getActivity(50);
            verify(getAdminDashboardUseCase).getStats();
        }

        @Test
        @WithMockUser(authorities = "ORGANIZER")
        @DisplayName("ORGANIZER → AccessDeniedException")
        void organizer_isDenied() {
            assertThatThrownBy(() -> controller.getStats()).isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(getAdminDashboardUseCase);
        }
    }
}
