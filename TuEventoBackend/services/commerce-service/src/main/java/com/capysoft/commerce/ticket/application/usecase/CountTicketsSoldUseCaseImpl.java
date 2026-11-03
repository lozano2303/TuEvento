package com.capysoft.commerce.ticket.application.usecase;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.commerce.ticket.application.port.in.CountTicketsSoldUseCase;
import com.capysoft.commerce.ticket.domain.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CountTicketsSoldUseCaseImpl implements CountTicketsSoldUseCase {

    private final TicketRepository ticketRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> countSoldByEventIds(Collection<Long> eventIds) {
        var ids = eventIds.stream().filter(Objects::nonNull).distinct().toList();
        Map<Long, Long> counts = new HashMap<>();
        if (ids.isEmpty()) {
            return counts;
        }
        ids.forEach(id -> counts.put(id, 0L));
        counts.putAll(ticketRepository.countSoldByEventIds(ids));
        return counts;
    }
}
