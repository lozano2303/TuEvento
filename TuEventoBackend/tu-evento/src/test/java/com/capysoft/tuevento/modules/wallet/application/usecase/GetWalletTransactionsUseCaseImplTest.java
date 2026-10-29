package com.capysoft.tuevento.modules.wallet.application.usecase;

import com.capysoft.tuevento.modules.ticket.application.port.out.EventLookupPort;
import com.capysoft.tuevento.modules.ticket.application.port.out.EventLookupPort.EventRef;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.wallet.application.dto.WalletTransactionResponse;
import com.capysoft.tuevento.modules.wallet.domain.model.Wallet;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletReference;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletReferenceEntityType;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletTransaction;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletReferenceRepository;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletRepository;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** Un movimiento ligado a una orden muestra el nombre del evento, leído de Catálogo. */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetWalletTransactionsUseCaseImpl")
class GetWalletTransactionsUseCaseImplTest {

    private static final Long USER_ID = 7L;

    @Mock private WalletRepository walletRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private WalletReferenceRepository walletReferenceRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private EventLookupPort eventLookupPort;
    @InjectMocks private GetWalletTransactionsUseCaseImpl useCase;

    @Test
    @DisplayName("movimiento de una orden → lleva el nombre de su evento")
    void orderReference_addsEventName() {
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(Wallet.builder().walletId(1L).userId(USER_ID).build()));
        when(walletTransactionRepository.findByWalletId(1L)).thenReturn(List.of(
                WalletTransaction.builder().transactionId(10L).walletId(1L).amount(BigDecimal.TEN).build()));
        when(walletReferenceRepository.findByTransactionId(10L)).thenReturn(List.of(WalletReference.builder()
                .transactionId(10L).entityType(WalletReferenceEntityType.ORDER).entityId(40L).build()));
        when(orderRepository.findById(40L)).thenReturn(Optional.of(Order.builder().orderId(40L).eventId(300L).build()));
        when(eventLookupPort.find(300L)).thenReturn(Optional.of(new EventRef(300L, 30L, "Concierto", "PUBLISHED")));

        List<WalletTransactionResponse> transactions = useCase.execute(USER_ID);

        assertThat(transactions).singleElement().satisfies(tx -> {
            assertThat(tx.getEventName()).isEqualTo("Concierto");
            assertThat(tx.getEntityId()).isEqualTo(40L);
        });
    }
}
