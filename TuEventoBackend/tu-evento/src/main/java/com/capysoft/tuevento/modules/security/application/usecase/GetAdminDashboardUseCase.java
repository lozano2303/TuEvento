package com.capysoft.tuevento.modules.security.application.usecase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository.ProfileJpaRepository;
import com.capysoft.tuevento.modules.security.application.dto.response.ActivityItemResponse;
import com.capysoft.tuevento.modules.security.application.dto.response.AdminDashboardStatsResponse;
import com.capysoft.tuevento.modules.security.application.port.out.AdminPanelSourcesPort;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerPetitionRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserJpaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Resumen del panel de administración: Identidad aporta usuarios y solicitudes de organizador y
 * compone el resto con lo que entregan Catálogo y Comercio por {@link AdminPanelSourcesPort}.
 */
@Service
@RequiredArgsConstructor
public class GetAdminDashboardUseCase {

    private static final String DEFAULT_ORGANIZER_NAME = "Organizador";

    private final AdminPanelSourcesPort       sources;
    private final UserJpaRepository           userJpaRepository;
    private final ProfileJpaRepository        profileJpaRepository;
    private final OrganizerPetitionRepository organizerPetitionRepository;

    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getStats() {
        AdminPanelSourcesPort.CatalogStats events = sources.catalogStats();
        AdminPanelSourcesPort.CommerceStats commerce = sources.commerceStats();
        return AdminDashboardStatsResponse.builder()
                // users
                .totalUsers(userJpaRepository.count())
                // events
                .totalEvents(events.total())
                .publishedEvents(events.published())
                .pendingReviewEvents(events.pendingReview())
                .rejectedEvents(events.rejected())
                .cancelledEvents(events.cancelled())
                .completedEvents(events.completed())
                .draftEvents(events.draft())
                // tickets
                .totalTickets(commerce.totalTickets())
                // organizer requests
                .pendingOrganizerRequests(organizerPetitionRepository.findByStatus("PENDING").size())
                .approvedOrganizerRequests(organizerPetitionRepository.findByStatus("APPROVED").size())
                // payments
                .totalRevenue(commerce.totalRevenue())
                .approvedPayments(commerce.approvedPayments())
                .pendingRefunds(commerce.pendingRefunds())
                .totalRefundedAmount(commerce.totalRefundedAmount())
                .processedRefunds(commerce.processedRefunds())
                .build();
    }

    /** Eventos, pagos y registros recientes mezclados, del más nuevo al más viejo, hasta {@code limit}. */
    @Transactional(readOnly = true)
    public List<ActivityItemResponse> getActivity(int limit) {
        List<ActivityItemResponse> items = new ArrayList<>();

        // ── Recent event status changes ───────────────────────────────────────
        var recentEvents = sources.recentlyUpdatedEvents(Math.min(limit, 100));
        var organizerIds = recentEvents.stream()
                .map(AdminPanelSourcesPort.RecentEvent::organizerUserId)
                .filter(Objects::nonNull)
                .map(Long::intValue)
                .distinct()
                .toList();
        Map<Integer, String> organizerNames = profileJpaRepository.findAllByUserIdIn(organizerIds).stream()
                .filter(p -> p.getUserId() != null)
                .collect(Collectors.toMap(
                        p -> p.getUserId(),
                        p -> p.getFullName() != null ? p.getFullName() : DEFAULT_ORGANIZER_NAME,
                        (a, b) -> a));

        for (var ev : recentEvents) {
            String organizerName = ev.organizerUserId() != null
                    ? organizerNames.getOrDefault(ev.organizerUserId().intValue(), DEFAULT_ORGANIZER_NAME)
                    : DEFAULT_ORGANIZER_NAME;
            items.add(ActivityItemResponse.builder()
                    .type("EVENT_STATUS_CHANGE")
                    .title(eventTitle(ev.status()))
                    .description("\"" + ev.eventName() + "\"")
                    .actor("Organizer: " + organizerName)
                    .occurredAt(ev.occurredAt())
                    .build());
        }

        // ── Recent approved payments ──────────────────────────────────────────
        for (var p : sources.recentPayments(Math.min(limit / 2, 20))) {
            String amountStr = p.amount() != null
                    ? "$ " + String.format("%,.0f", p.amount()) + " " + p.currency()
                    : "";
            items.add(ActivityItemResponse.builder()
                    .type("PAYMENT")
                    .title(p.approved() ? "Payment approved" : "Payment processed")
                    .description(amountStr)
                    .actor("Order #" + p.orderId())
                    .occurredAt(p.processedAt())
                    .build());
        }

        // ── Recent user registrations ─────────────────────────────────────────
        int usersLimit = Math.min(limit / 3, 15);
        if (usersLimit > 0) {
            for (var u : userJpaRepository.findTopByOrderByCreatedAtDesc(PageRequest.of(0, usersLimit))) {
                items.add(ActivityItemResponse.builder()
                        .type("USER_REGISTERED")
                        .title("New user registered")
                        .description("@" + u.getAlias())
                        .actor(u.getRole() != null ? u.getRole().getName() : "USER")
                        .occurredAt(u.getCreatedAt())
                        .build());
            }
        }

        // ── Sort by occurredAt desc, cap to limit ─────────────────────────────
        return items.stream()
                .filter(i -> i.getOccurredAt() != null)
                .sorted(Comparator.comparing(ActivityItemResponse::getOccurredAt).reversed())
                .limit(Math.max(limit, 0))
                .toList();
    }

    private static String eventTitle(String status) {
        if (status == null) {
            return "Event created as draft";
        }
        return switch (status) {
            case "PUBLISHED"      -> "Event approved and published";
            case "REJECTED"       -> "Event rejected";
            case "CANCELLED"      -> "Event cancelled";
            case "PENDING_REVIEW" -> "Event submitted for review";
            case "COMPLETED"      -> "Event completed";
            default               -> "Event created as draft";
        };
    }
}
