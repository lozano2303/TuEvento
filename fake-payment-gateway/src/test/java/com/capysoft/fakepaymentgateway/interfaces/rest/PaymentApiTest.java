package com.capysoft.fakepaymentgateway.interfaces.rest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.capysoft.fakepaymentgateway.application.dto.response.PaymentResponse;
import com.capysoft.fakepaymentgateway.application.usecase.ApprovePaymentUseCase;
import com.capysoft.fakepaymentgateway.application.usecase.CancelPaymentUseCase;
import com.capysoft.fakepaymentgateway.application.usecase.CreatePaymentUseCase;
import com.capysoft.fakepaymentgateway.application.usecase.DeclinePaymentUseCase;
import com.capysoft.fakepaymentgateway.application.usecase.FailPaymentUseCase;
import com.capysoft.fakepaymentgateway.application.usecase.GetPaymentUseCase;
import com.capysoft.fakepaymentgateway.application.usecase.RefundPaymentUseCase;
import com.capysoft.fakepaymentgateway.domain.model.InvalidStatusTransitionException;
import com.capysoft.fakepaymentgateway.domain.model.PaymentMethod;
import com.capysoft.fakepaymentgateway.domain.model.PaymentStatus;
import com.capysoft.fakepaymentgateway.domain.repository.PaymentRepository;

/**
 * Contrato HTTP: controladores, manejo de errores (L-02) y protección de /admin (L-03).
 */
@WebMvcTest(properties = "gateway.admin.api-key=test-admin-key")
class PaymentApiTest {

    private static final String KEY_HEADER = "X-Api-Key";
    private static final String KEY = "test-admin-key";
    private static final String ID = "fake_3f2b7a0e-9c1d-4e8f-a6b5-1d2c3e4f5a6b";

    @Autowired
    private MockMvc mvc;

    @MockitoBean private CreatePaymentUseCase createPaymentUseCase;
    @MockitoBean private GetPaymentUseCase getPaymentUseCase;
    @MockitoBean private ApprovePaymentUseCase approvePaymentUseCase;
    @MockitoBean private DeclinePaymentUseCase declinePaymentUseCase;
    @MockitoBean private FailPaymentUseCase failPaymentUseCase;
    @MockitoBean private CancelPaymentUseCase cancelPaymentUseCase;
    @MockitoBean private RefundPaymentUseCase refundPaymentUseCase;
    @MockitoBean private PaymentRepository paymentRepository;

    private PaymentResponse pending() {
        Instant now = Instant.parse("2026-10-24T15:04:05Z");
        return new PaymentResponse(ID, "ORDER-1", PaymentStatus.PENDING, new BigDecimal("150000"), "COP",
            PaymentMethod.CARD, now, now);
    }

    // ── API pública ─────────────────────────────────────────────────────────

    @Test
    void crearPagoValidoDevuelve201() throws Exception {
        when(createPaymentUseCase.execute(any())).thenReturn(pending());

        mvc.perform(post("/public/payments").contentType(MediaType.APPLICATION_JSON).content("""
                {"externalReference":"ORDER-1","amount":150000,"currency":"cop","paymentMethod":"CARD"}
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentId").value(ID))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void crearPagoConCamposInvalidosDevuelve400ConDetallePorCampo() throws Exception {
        mvc.perform(post("/public/payments").contentType(MediaType.APPLICATION_JSON).content("""
                {"externalReference":"","amount":0,"currency":"COPX"}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.fields.externalReference").exists())
            .andExpect(jsonPath("$.fields.amount").value("Amount must be positive"))
            .andExpect(jsonPath("$.fields.currency").value("Currency must be a 3-letter ISO 4217 code"))
            .andExpect(jsonPath("$.fields.paymentMethod").value("Payment method is required"));
        verifyNoInteractions(createPaymentUseCase);
    }

    @Test
    void metodoDePagoDesconocidoDevuelve400() throws Exception {
        mvc.perform(post("/public/payments").contentType(MediaType.APPLICATION_JSON).content("""
                {"externalReference":"ORDER-1","amount":10,"currency":"COP","paymentMethod":"BITCOIN"}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Malformed request"))
            .andExpect(jsonPath("$.message").value("Invalid value for field 'paymentMethod'"));
    }

    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        mvc.perform(post("/public/payments").contentType(MediaType.APPLICATION_JSON).content("{nope"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Malformed request"));
    }

    @Test
    void contentTypeIncorrectoDevuelve415() throws Exception {
        mvc.perform(post("/public/payments").contentType(MediaType.TEXT_PLAIN).content("x"))
            .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void consultarPagoInexistenteDevuelve404() throws Exception {
        when(getPaymentUseCase.execute("fake_x")).thenThrow(new GetPaymentUseCase.PaymentNotFoundException("fake_x"));

        mvc.perform(get("/public/payments/fake_x"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Payment not found"));
    }

    @Test
    void errorInternoDevuelve500SinFiltrarDetalles() throws Exception {
        when(getPaymentUseCase.execute(ID)).thenThrow(new IllegalStateException("password=secreto"));

        mvc.perform(get("/public/payments/" + ID))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Unexpected error"));
    }

    // ── Checkout público ────────────────────────────────────────────────────

    @Test
    void checkoutPuedeAprobarSinApiKey() throws Exception {
        mvc.perform(post("/public/checkout/" + ID + "/approve")).andExpect(status().isOk());
        verify(approvePaymentUseCase).execute(ID);
    }

    @Test
    void checkoutPuedeRechazarFallarYCancelar() throws Exception {
        mvc.perform(post("/public/checkout/" + ID + "/decline")).andExpect(status().isOk());
        mvc.perform(post("/public/checkout/" + ID + "/fail")).andExpect(status().isOk());
        mvc.perform(post("/public/checkout/" + ID + "/cancel")).andExpect(status().isOk());
        verify(declinePaymentUseCase).execute(ID);
        verify(failPaymentUseCase).execute(ID);
        verify(cancelPaymentUseCase).execute(ID);
    }

    @Test
    void checkoutNoPermiteReembolsar() throws Exception {
        mvc.perform(post("/public/checkout/" + ID + "/refund")).andExpect(status().is4xxClientError());
        verifyNoInteractions(refundPaymentUseCase);
    }

    @Test
    void transicionInvalidaDevuelve400() throws Exception {
        doThrow(new InvalidStatusTransitionException("Cannot transition from DECLINED to PROCESSING"))
            .when(approvePaymentUseCase).execute(ID);

        mvc.perform(post("/public/checkout/" + ID + "/approve"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid status transition"));
    }

    // ── API administrativa ──────────────────────────────────────────────────

    @Test
    void adminSinApiKeyDevuelve401() throws Exception {
        mvc.perform(post("/admin/payments/" + ID + "/refund"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Unauthorized"));
        mvc.perform(get("/admin/payments")).andExpect(status().isUnauthorized());
        verifyNoInteractions(refundPaymentUseCase, paymentRepository);
    }

    @Test
    void adminConApiKeyIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/admin/payments/" + ID + "/approve").header(KEY_HEADER, "wrong"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(approvePaymentUseCase);
    }

    @Test
    void adminNoSePuedeEvadirConRutasCodificadasOMatrixParams() throws Exception {
        for (String raw : List.of(
                "/%61dmin/payments/" + ID + "/refund",
                "/admin;x=1/payments/" + ID + "/refund",
                "/admin/payments;x=1/" + ID + "/refund",
                "//admin/payments/" + ID + "/refund")) {
            mvc.perform(post(URI.create(raw))).andExpect(status().is4xxClientError());
        }
        verifyNoInteractions(refundPaymentUseCase);
    }

    @Test
    void adminConApiKeyValidaReembolsa() throws Exception {
        mvc.perform(post("/admin/payments/" + ID + "/refund").header(KEY_HEADER, KEY))
            .andExpect(status().isOk());
        verify(refundPaymentUseCase).execute(ID);
    }

    @Test
    void adminConApiKeyValidaLista() throws Exception {
        when(paymentRepository.findAll()).thenReturn(List.of());

        mvc.perform(get("/admin/payments").header(KEY_HEADER, KEY))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void preflightCorsDeAdminNoExigeApiKey() throws Exception {
        mvc.perform(options("/admin/payments")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", KEY_HEADER))
            .andExpect(status().isOk());
    }

    @Test
    void rutaInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/no-existe"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message", containsString("no-existe")));
    }
}
