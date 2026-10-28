package com.capysoft.tuevento.modules.event.application.usecase;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.event.application.port.in.GetEventAdminStatsPort;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.repository.EventJpaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetEventAdminStatsUseCase implements GetEventAdminStatsPort {

    private final EventJpaRepository eventJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public EventStats getStats() {
        return new EventStats(
                eventJpaRepository.count(),
                eventJpaRepository.countByStatus(EventStatus.PUBLISHED),
                eventJpaRepository.countByStatus(EventStatus.PENDING_REVIEW),
                eventJpaRepository.countByStatus(EventStatus.REJECTED),
                eventJpaRepository.countByStatus(EventStatus.CANCELLED),
                eventJpaRepository.countByStatus(EventStatus.COMPLETED),
                eventJpaRepository.countByStatus(EventStatus.DRAFT));
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingReview() {
        return eventJpaRepository.countByStatus(EventStatus.PENDING_REVIEW);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentEvent> findRecentlyUpdated(int limit) {
        if (limit < 1) {
            return List.of();
        }
        return eventJpaRepository.findTopByOrderByUpdatedAtDesc(PageRequest.of(0, limit)).stream()
                .map(e -> new RecentEvent(e.getEventName(),
                        e.getStatus() == null ? null : e.getStatus().name(),
                        e.getUserId(),
                        e.getUpdatedAt() != null ? e.getUpdatedAt() : e.getCreatedAt()))
                .toList();
    }
}
