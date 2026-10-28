package com.capysoft.tuevento.modules.security.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.capysoft.tuevento.modules.profile.infrastructure.persistence.entity.ProfileEntity;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository.ProfileJpaRepository;
import com.capysoft.tuevento.modules.security.application.dto.response.ActivityItemResponse;
import com.capysoft.tuevento.modules.security.application.port.out.AdminPanelSourcesPort;
import com.capysoft.tuevento.modules.security.application.port.out.AdminPanelSourcesPort.CatalogStats;
import com.capysoft.tuevento.modules.security.application.port.out.AdminPanelSourcesPort.CommerceStats;
import com.capysoft.tuevento.modules.security.application.port.out.AdminPanelSourcesPort.RecentEvent;
import com.capysoft.tuevento.modules.security.application.port.out.AdminPanelSourcesPort.RecentPayment;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerPetitionRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserJpaRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GetAdminDashboardUseCase")
class GetAdminDashboardUseCaseTest {

    @Mock AdminPanelSourcesPort sources;
    @Mock UserJpaRepository userJpaRepository;
    @Mock ProfileJpaRepository profileJpaRepository;
    @Mock OrganizerPetitionRepository organizerPetitionRepository;
    @InjectMocks GetAdminDashboardUseCase useCase;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 27, 10, 0);

    @Test
    @DisplayName("las estadísticas combinan usuarios y solicitudes de Identidad con las cifras de Catálogo y Comercio")
    void statsComposeAllServices() {
        when(userJpaRepository.count()).thenReturn(40L);
        when(sources.catalogStats()).thenReturn(new CatalogStats(12, 5, 2, 1, 1, 2, 1));
        when(sources.commerceStats()).thenReturn(new CommerceStats(90, new BigDecimal("1500000"), 30, 3,
                new BigDecimal("80000"), 4));
        when(organizerPetitionRepository.findByStatus("PENDING")).thenReturn(List.of());
        when(organizerPetitionRepository.findByStatus("APPROVED")).thenReturn(List.of());

        var stats = useCase.getStats();

        assertThat(stats.getTotalUsers()).isEqualTo(40);
        assertThat(stats.getTotalEvents()).isEqualTo(12);
        assertThat(stats.getPendingReviewEvents()).isEqualTo(2);
        assertThat(stats.getDraftEvents()).isEqualTo(1);
        assertThat(stats.getTotalTickets()).isEqualTo(90);
        assertThat(stats.getTotalRevenue()).isEqualByComparingTo("1500000");
        assertThat(stats.getProcessedRefunds()).isEqualTo(4);
    }

    @Test
    @DisplayName("la actividad resuelve nombres de organizador, usa 'Organizador' si faltan y ordena por fecha")
    void activityResolvesNamesAndSorts() {
        when(sources.recentlyUpdatedEvents(50)).thenReturn(List.of(
                new RecentEvent("Jazz Night", "PUBLISHED", 15L, NOW),
                new RecentEvent("Orphan Fest", "REJECTED", null, NOW.minusHours(1)),
                new RecentEvent("Indie Day", "PENDING_REVIEW", 16L, NOW.minusHours(2)),
                new RecentEvent("Sin estado", null, 15L, NOW.minusHours(3))));
        when(profileJpaRepository.findAllByUserIdIn(List.of(15, 16))).thenReturn(List.of(
                ProfileEntity.builder().userId(15).fullName("Bob Marley").build(),
                ProfileEntity.builder().userId(16).fullName(null).build()));
        when(sources.recentPayments(20)).thenReturn(List.of(
                new RecentPayment(true, new BigDecimal("50000"), "COP", 9L, NOW.minusMinutes(30))));
        when(userJpaRepository.findTopByOrderByCreatedAtDesc(any())).thenReturn(List.of());

        List<ActivityItemResponse> items = useCase.getActivity(50);

        assertThat(items).extracting(ActivityItemResponse::getTitle).containsExactly(
                "Event approved and published", "Payment approved", "Event rejected",
                "Event submitted for review", "Event created as draft");
        assertThat(items).extracting(ActivityItemResponse::getActor).containsExactly(
                "Organizer: Bob Marley", "Order #9", "Organizer: Organizador",
                "Organizer: Organizador", "Organizer: Bob Marley");
        assertThat(items.get(1).getDescription()).isEqualTo("$ " + String.format("%,.0f", new BigDecimal("50000")) + " COP");
    }

    @Test
    @DisplayName("con un límite pequeño no falla (antes ?limit=2 daba 500) y no pide usuarios con página vacía")
    void smallLimitDoesNotFail() {
        when(sources.recentlyUpdatedEvents(anyInt())).thenReturn(List.of());
        when(sources.recentPayments(anyInt())).thenReturn(List.of());

        assertThat(useCase.getActivity(2)).isEmpty();
        assertThat(useCase.getActivity(0)).isEmpty();
        verify(userJpaRepository, never()).findTopByOrderByCreatedAtDesc(any());
    }
}
