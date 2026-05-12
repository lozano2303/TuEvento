package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.SaveEventLayoutRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventLayoutResponse;
import com.capysoft.tuevento.modules.event.application.port.in.SaveEventLayoutUseCase;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventLayout;
import com.capysoft.tuevento.modules.event.domain.repository.EventLayoutRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SaveEventLayoutService implements SaveEventLayoutUseCase {

    private final EventRepository eventRepository;
    private final EventLayoutRepository eventLayoutRepository;

    @Override
    @Transactional
    public EventLayoutResponse execute(Long eventId, SaveEventLayoutRequest request, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("EVENT_NOT_FOUND",
                        "Event not found with id: " + eventId));

        if (!event.getUserId().equals(userId)) {
            throw new BusinessException("EVENT_ACCESS_DENIED",
                    "User " + userId + " does not own event " + eventId);
        }

        Optional<EventLayout> existing = eventLayoutRepository.findByEventId(eventId);

        EventLayout layout = existing
                .map(l -> EventLayout.builder()
                        .eventLayoutId(l.getEventLayoutId())
                        .eventId(l.getEventId())
                        .layoutData(request.getLayoutData())
                        .build())
                .orElseGet(() -> EventLayout.builder()
                        .eventId(eventId)
                        .layoutData(request.getLayoutData())
                        .build());

        EventLayout saved = eventLayoutRepository.save(layout);

        return EventLayoutResponse.builder()
                .eventLayoutId(saved.getEventLayoutId())
                .eventId(saved.getEventId())
                .layoutData(saved.getLayoutData())
                .build();
    }
}
