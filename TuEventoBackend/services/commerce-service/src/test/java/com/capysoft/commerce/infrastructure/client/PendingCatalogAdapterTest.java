package com.capysoft.commerce.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.commerce.shared.domain.exception.ServiceUnavailableException;

@DisplayName("PendingCatalogAdapter — sin Catálogo no hay datos inventados")
class PendingCatalogAdapterTest {

    private final PendingCatalogAdapter adapter = new PendingCatalogAdapter();

    @Test
    @DisplayName("toda operación responde que Catálogo no está disponible")
    void everyOperationIsUnavailable() {
        List<ThrowingCallable> calls = List.of(
                () -> adapter.holdForOrder(1L, 2L, 3L, List.of(4)),
                () -> adapter.sell(1L, 2L, List.of(4), "r"),
                () -> adapter.release(1L, List.of(4), 2, "r"),
                () -> adapter.find(3L),
                () -> adapter.findAll(Set.of(3L)),
                () -> adapter.findByOwner(2L),
                () -> adapter.summarizeByOrganizer(2L));

        calls.forEach(call -> assertThatThrownBy(call).isInstanceOf(ServiceUnavailableException.class));
    }
}
