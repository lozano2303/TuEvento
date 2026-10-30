package com.capysoft.tuevento.modules.ticket.interfaces.rest.advice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capysoft.tuevento.modules.payment.domain.model.InvalidPaymentStatusTransitionException;
import com.capysoft.tuevento.modules.ticket.domain.model.InvalidOrderStatusTransitionException;
import com.capysoft.tuevento.modules.wallet.domain.model.InsufficientWalletBalanceException;
import com.capysoft.tuevento.shared.interfaces.advice.GlobalExceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Con los dos advices registrados (como en la app), los errores de Comercio conservan su
 * código HTTP: el handler de {@code Exception} del global no se los come.
 */
@DisplayName("CommerceExceptionHandler — junto al GlobalExceptionHandler")
class CommerceExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class ThrowingController {

        @GetMapping("/payment-transition")
        void paymentTransition() {
            throw new InvalidPaymentStatusTransitionException("APPROVED -> PENDING");
        }

        @GetMapping("/order-transition")
        void orderTransition() {
            throw new InvalidOrderStatusTransitionException("PAID -> PENDING");
        }

        @GetMapping("/wallet-balance")
        void walletBalance() {
            throw new InsufficientWalletBalanceException("balance 0");
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new UnsupportedOperationException("boom");
        }
    }

    @BeforeEach
    void setUp() {
        // El global va primero a propósito: el orden lo decide @Order, no el registro
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler(), new CommerceExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("transición de pago inválida → 409 con el mensaje")
    void paymentTransition_409() throws Exception {
        mockMvc.perform(get("/payment-transition"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("APPROVED -> PENDING"));
    }

    @Test
    @DisplayName("transición de orden inválida → 409")
    void orderTransition_409() throws Exception {
        mockMvc.perform(get("/order-transition"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("PAID -> PENDING"));
    }

    @Test
    @DisplayName("saldo de wallet insuficiente → 422")
    void walletBalance_422() throws Exception {
        mockMvc.perform(get("/wallet-balance"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("balance 0"));
    }

    @Test
    @DisplayName("cualquier otra excepción sigue en el global → 500")
    void otherExceptions_stillGlobal() throws Exception {
        mockMvc.perform(get("/unexpected"))
                .andExpect(status().isInternalServerError());
    }
}
