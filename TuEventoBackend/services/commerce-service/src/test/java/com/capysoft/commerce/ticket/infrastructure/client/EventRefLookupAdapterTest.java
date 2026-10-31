package com.capysoft.commerce.ticket.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.capysoft.commerce.ticket.application.port.out.EventLookupPort.EventRef;
import com.capysoft.commerce.ticket.infrastructure.persistence.entity.EventRefEntity;
import com.capysoft.commerce.ticket.infrastructure.persistence.repository.EventRefJpaRepository;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("EventRefLookupAdapter — eventos desde la réplica")
class EventRefLookupAdapterTest {

    @Autowired EventRefJpaRepository repository;

    private EventRefLookupAdapter adapter() {
        return new EventRefLookupAdapter(repository);
    }

    private void stored(long eventId, long ownerId, String status) {
        repository.save(EventRefEntity.builder().eventId(eventId).ownerId(ownerId).name("E" + eventId).status(status)
                .updatedAt(LocalDateTime.now()).build());
    }

    @Test
    @DisplayName("encuentra por id, por varios ids y por dueño")
    void reads() {
        stored(1, 7, "PUBLISHED");
        stored(2, 7, "DRAFT");
        stored(3, 8, "PUBLISHED");

        assertThat(adapter().find(1L)).contains(new EventRef(1L, 7L, "E1", "PUBLISHED"));
        assertThat(adapter().find(9L)).isEmpty();
        assertThat(adapter().findAll(List.of(1L, 3L, 9L))).containsOnlyKeys(1L, 3L);
        assertThat(adapter().findByOwner(7L)).extracting(EventRef::eventId).containsExactlyInAnyOrder(1L, 2L);
    }
}
