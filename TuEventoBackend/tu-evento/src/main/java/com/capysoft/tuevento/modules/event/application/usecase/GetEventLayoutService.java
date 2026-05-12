package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.response.EventLayoutResponse;
import com.capysoft.tuevento.modules.event.application.port.in.GetEventLayoutUseCase;
import com.capysoft.tuevento.modules.event.domain.repository.EventLayoutRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetEventLayoutService implements GetEventLayoutUseCase {

    private final EventLayoutRepository eventLayoutRepository;

    @Override
    public EventLayoutResponse findByEventId(Long eventId) {
        return eventLayoutRepository.findByEventId(eventId)
                .map(l -> EventLayoutResponse.builder()
                        .eventLayoutId(l.getEventLayoutId())
                        .eventId(l.getEventId())
                        .layoutData(l.getLayoutData())
                        .build())
                .orElseThrow(() -> new NotFoundException("EVENT_LAYOUT_NOT_FOUND",
                        "Layout not found for event id: " + eventId));
    }
}
