package com.capysoft.commerce.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.capysoft.commerce.payment.application.dto.GatewayPayment;
import com.capysoft.commerce.payment.application.dto.request.CreatePaymentRequest;
import com.capysoft.commerce.payment.application.port.out.PaymentGatewayPort;
import com.capysoft.commerce.payment.domain.model.Payment;
import com.capysoft.commerce.payment.domain.model.PaymentGatewayException;
import com.capysoft.commerce.payment.domain.model.PaymentStatus;
import com.capysoft.commerce.payment.domain.repository.PaymentRepository;
import com.capysoft.commerce.ticket.application.port.in.FailOrderPaymentUseCase;
import com.capysoft.commerce.ticket.application.port.in.InitiateOrderPaymentUseCase;
import com.capysoft.commerce.ticket.domain.model.Money;
import com.capysoft.commerce.ticket.domain.model.Order;
import com.capysoft.commerce.ticket.domain.repository.OrderRepository;
import com.capysoft.commerce.wallet.application.dto.WalletResponse;
import com.capysoft.commerce.wallet.application.usecase.GetWalletUseCaseImpl;
import com.capysoft.commerce.wallet.application.usecase.ReleaseWalletPaymentUseCaseImpl;
import com.capysoft.commerce.wallet.application.usecase.ReserveWalletPaymentUseCaseImpl;

/**
 * Cobro por pasarela fuera de la transacción: qué se guarda cuando la pasarela responde, cuando
 * falla y cuando falla el guardado local.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Cobro por pasarela")
class GatewayPaymentFlowTest {

    private static final Long ORDER_ID = 100L;
    private static final Long BUYER_ID = 7L;

    private static Order order() {
        return Order.builder().orderId(ORDER_ID).userId(BUYER_ID).eventId(1L)
                .totalAmount(new Money(new BigDecimal("50000"), "COP")).build();
    }

    @Nested
    @DisplayName("ChargeOrderViaGatewayUseCase")
    class Charge {

        @Mock PaymentRepository paymentRepository;
        @Mock PaymentGatewayPort paymentGatewayPort;
        @InjectMocks ChargeOrderViaGatewayUseCase charge;

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(charge, "gatewayName", "fake");
        }

        @Test
        @DisplayName("guarda un Payment PENDING con el id que devuelve la pasarela")
        void savesPendingPayment() {
            when(paymentGatewayPort.createPayment(any())).thenReturn(GatewayPayment.builder().paymentId("gw-1").build());
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            charge.execute(order(), new BigDecimal("30000"), "qr", new BigDecimal("20000"), 55L);

            ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(saved.capture());
            assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(saved.getValue().getGatewayTransactionId()).isEqualTo("gw-1");
            assertThat(saved.getValue().getWalletTransactionId()).isEqualTo(55L);
        }

        @Test
        @DisplayName("si la pasarela falla no guarda nada")
        void gatewayFailure_savesNothing() {
            when(paymentGatewayPort.createPayment(any())).thenThrow(new IllegalStateException("timeout"));

            assertThatThrownBy(() -> charge.execute(order(), BigDecimal.TEN, null, BigDecimal.ZERO, null))
                    .isInstanceOf(IllegalStateException.class);
            verifyNoInteractions(paymentRepository);
        }

        @Test
        @DisplayName("si falla el guardado tras crear el cobro, propaga el error")
        void saveFailure_propagates() {
            when(paymentGatewayPort.createPayment(any())).thenReturn(GatewayPayment.builder().paymentId("gw-2").build());
            when(paymentRepository.save(any())).thenThrow(new IllegalStateException("db down"));

            assertThatThrownBy(() -> charge.execute(order(), BigDecimal.TEN, "QR", BigDecimal.ZERO, null))
                    .hasMessage("db down");
        }
    }

    @Nested
    @DisplayName("CreatePaymentUseCaseImpl — fallo de la pasarela")
    class Orchestrator {

        @Mock OrderRepository orderRepository;
        @Mock InitiateOrderPaymentUseCase initiateOrderPaymentUseCase;
        @Mock ChargeOrderViaGatewayUseCase chargeOrderViaGatewayUseCase;
        @Mock CreateWalletOnlyPaymentUseCase createWalletOnlyPaymentUseCase;
        @Mock GetWalletUseCaseImpl getWalletUseCase;
        @Mock ReserveWalletPaymentUseCaseImpl reserveWalletPaymentUseCase;
        @Mock ReleaseWalletPaymentUseCaseImpl releaseWalletPaymentUseCase;
        @Mock PaymentRepository paymentRepository;
        @Mock FailOrderPaymentUseCase failOrderPaymentUseCase;
        @InjectMocks CreatePaymentUseCaseImpl createPayment;

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(createPayment, "gatewayName", "fake");
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));
        }

        @Test
        @DisplayName("sin wallet: deja un Payment en ERROR y falla la orden para liberar las sillas")
        void gatewayOnly_failureCleansUp() {
            when(chargeOrderViaGatewayUseCase.execute(any(), any(), any(), any(), any()))
                    .thenThrow(new IllegalStateException("gateway down"));

            assertThatThrownBy(() -> createPayment.execute(new CreatePaymentRequest(ORDER_ID, "QR", false), BUYER_ID))
                    .isInstanceOf(PaymentGatewayException.class);

            verify(initiateOrderPaymentUseCase).initiateOrderPayment(ORDER_ID);
            ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(saved.capture());
            assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.ERROR);
            verify(failOrderPaymentUseCase).failOrderPayment(eq(ORDER_ID), anyString());
            verify(releaseWalletPaymentUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("pago mixto: si la pasarela falla, libera la reserva de wallet")
        void mixed_failureReleasesWallet() {
            when(getWalletUseCase.execute(BUYER_ID)).thenReturn(
                    WalletResponse.builder().availableBalance(new BigDecimal("20000")).build());
            when(reserveWalletPaymentUseCase.execute(BUYER_ID, new BigDecimal("20000"), ORDER_ID)).thenReturn(55L);
            when(chargeOrderViaGatewayUseCase.execute(any(), any(), any(), any(), any()))
                    .thenThrow(new IllegalStateException("gateway down"));

            assertThatThrownBy(() -> createPayment.execute(new CreatePaymentRequest(ORDER_ID, "QR", true), BUYER_ID))
                    .isInstanceOf(PaymentGatewayException.class);

            verify(releaseWalletPaymentUseCase).execute(55L);
            verify(failOrderPaymentUseCase).failOrderPayment(eq(ORDER_ID), anyString());
        }
    }
}
