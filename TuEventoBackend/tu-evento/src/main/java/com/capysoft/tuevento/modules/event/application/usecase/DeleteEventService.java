package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.port.in.DeleteEventUseCase;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteEventService implements DeleteEventUseCase {

    private final EventRepository eventRepository;

    @Override
    @Transactional
    public void execute(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("EVENT_NOT_FOUND",
                        "Event not found with id: " + eventId));

        if (!event.getUserId().equals(userId)) {
            throw new BusinessException("EVENT_ACCESS_DENIED",
                    "User " + userId + " does not own event " + eventId);
        }

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessException("EVENT_DELETE_NOT_ALLOWED",
                    "Only DRAFT events can be deleted");
        }

        eventRepository.delete(eventId);
    }
}
